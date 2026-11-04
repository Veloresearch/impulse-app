package com.velocity.impulse.data

import com.velocity.impulse.ui.Lang
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * The tools one answer may use, the same ones the desktop panel offers: web search and page
 * reading through the service, and the plugins (MCP) running on the computer. It keeps what the
 * answer did, for the message: the pages it can cite, numbered in the order they were found, and
 * its steps.
 */
class Tools(private val api: Impulse, private val web: Boolean, private val plugins: List<JSONObject>) {
    /** Pages found or read so far, as (title, url): [n] in the answer is the n-th. */
    val seen = mutableListOf<Pair<String, String>>()
    /** Pages a search listed, for showing while it reads. */
    val links = mutableListOf<Pair<String, String>>()
    val steps = mutableListOf<Step>()
    private var searches = 0

    val defs: JSONArray = JSONArray().apply {
        if (web) {
            put(def(
                "web_search",
                "Search the web and read the top pages. Use for anything recent, or facts you are not sure of. Today is ${LocalDate.now()}.",
                JSONObject().put("query", JSONObject().put("type", "string").put("description", "A short search query, 3 to 10 words")),
                "query",
            ))
            put(def(
                "read_page",
                "Open one web page and read its main text: a link the user gave, or a search result whose snippet is not enough.",
                JSONObject().put("url", JSONObject().put("type", "string").put("description", "The full address, starting with https://")),
                "url",
            ))
        }
        for (t in plugins) {
            put(JSONObject().put("type", "function").put("function", JSONObject()
                .put("name", t.optString("name"))
                .put("description", t.optString("description").ifBlank { t.optString("title").ifBlank { t.optString("tool") } })
                .put("parameters", t.optJSONObject("input_schema") ?: JSONObject().put("type", "object").put("properties", JSONObject()))))
        }
    }

    val any get() = defs.length() > 0

    /** The plugins offered, by server name. */
    val servers: List<String> get() = plugins.map { it.optString("server_name") }.filter { it.isNotBlank() }.distinct()

    /** What the answer was offered, for its `meta.tools`: "web" and the plugin servers. */
    val offered: List<String> get() = (if (web) listOf("web") else emptyList()) + servers

    /** Runs one call and returns what the model reads. [show] says what is happening, for the person watching. */
    suspend fun call(name: String, args: JSONObject, show: (String) -> Unit): String = when (name) {
        "web_search" -> search(args.optString("query").trim(), show)
        "read_page" -> read(args.optString("url").trim(), show)
        else -> plugin(name, args, show)
    }

    private suspend fun search(query: String, show: (String) -> Unit): String {
        if (query.isEmpty()) return "web_search needs a query."
        if (searches >= MAX_SEARCHES) return "You have used all $MAX_SEARCHES searches for this answer. Answer from what you found."
        searches++
        show(Lang.tr("Searching: $query", "Szukam: $query"))
        val done = Step("web_search", Lang.tr("Searched the web", "Przeszukano internet"), query, true)
        return try {
            runCatching { api.webSearch(query, 0) }.getOrNull()?.let { listed ->
                found(listed)
                show(Lang.tr("Reading pages", "Czytam strony"))
            }
            val results = api.webSearch(query, READ).map {
                val text = it.optString("text").takeUnless { t -> JUNK.containsMatchIn(t) }.orEmpty()
                Result(it.optString("title"), it.optString("url"), it.optString("snippet"), text)
            }.filter { it.url.isNotBlank() && (it.text.isNotBlank() || (it.snippet.isNotBlank() && !JUNK.containsMatchIn(it.snippet))) }
            found(results.map { JSONObject().put("title", it.title).put("url", it.url) })
            steps += done
            if (results.isEmpty()) return "Nothing useful was found for \"$query\". Search again with other words."
            val thin = if (results.none { it.text.isNotBlank() } || results.size < 2) " These results are thin: search again with other words, or read_page the most promising one." else ""
            "${compact(results, query)}\n\n$GROUNDING If they do not answer it, search again with a better query.$thin"
        } catch (e: ImpulseError) {
            steps += done.copy(ok = false)
            "The search failed: ${e.message}"
        }
    }

    private suspend fun read(url: String, show: (String) -> Unit): String {
        if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) return "read_page needs a full address starting with https://"
        show(Lang.tr("Reading: ${host(url)}", "Czytam: ${host(url)}"))
        val step = Step("read_page", Lang.tr("Read a page", "Przeczytano stronę"), url, true)
        return try {
            val p = api.webPage(url, PAGE_CHARS)
            val text = p.optString("text").replace(WHITESPACE, " ").trim()
            if (text.length < 80) {
                steps += step.copy(ok = false)
                return "$url gave no readable text (it may need JavaScript or block robots). Say so; never guess what it says. web_search may find the same content elsewhere."
            }
            val title = p.optString("title").ifBlank { url }
            found(listOf(JSONObject().put("title", title).put("url", url)))
            steps += step
            val n = number(title, url)
            "[$n] $title\n$text\n\nAnswer from this page and cite it as [$n]."
        } catch (e: ImpulseError) {
            steps += step.copy(ok = false)
            "Could not open $url: ${e.message}. Tell the user the page could not be opened; never guess what it says. web_search may find the same content elsewhere."
        }
    }

    private suspend fun plugin(name: String, args: JSONObject, show: (String) -> Unit): String {
        val t = plugins.firstOrNull { it.optString("name") == name } ?: return "There is no tool called $name."
        val label = "${t.optString("server_name")}: ${t.optString("title").ifBlank { t.optString("tool") }}"
        show(Lang.tr("Using $label", "Używam: $label"))
        return try {
            val r = api.mcpCall(name, args)
            val failed = r.optBoolean("isError")
            steps += Step(name, label, null, !failed)
            resultText(r, failed)
        } catch (e: ImpulseError) {
            steps += Step(name, label, null, false)
            e.message ?: "The tool failed."
        }
    }

    private fun found(list: List<JSONObject>) {
        for (o in list) {
            val url = o.optString("url")
            if (url.isNotBlank() && links.none { it.second == url }) links += o.optString("title").ifBlank { url } to url
        }
    }

    /** A page keeps its number for the whole answer; a new one goes on the end. */
    private fun number(title: String, url: String): Int {
        val at = seen.indexOfFirst { it.second == url }
        if (at >= 0) return at + 1
        seen += title to url
        return seen.size
    }

    /** Results as the model reads them: numbered, short, with the sentences that carry figures pulled out of each page. */
    private fun compact(results: List<Result>, query: String) = results.joinToString("\n\n") { r ->
        buildString {
            append("[${number(r.title, r.url)}] ${r.title} (${host(r.url)})")
            if (r.snippet.isNotBlank()) append("\nSnippet: ${r.snippet.replace(WHITESPACE, " ").trim()}")
            if (r.text.isNotBlank()) append("\nKey facts: ${keyFacts(r.text, query)}")
        }
    }

    private data class Result(val title: String, val url: String, val snippet: String, val text: String)

    companion object {
        const val MAX_SEARCHES = 3
        private const val READ = 3
        /** About 900 tokens of a page: enough to answer from, small enough for an 8K window. */
        private const val PAGE_CHARS = 3500
        private val WHITESPACE = Regex("\\s+")
        private val JUNK = Regex(
            "abuse detection|just a moment|checking your browser|enable javascript|access denied|verify you are (a )?human|captcha|attention required|are you a robot|sign in to continue|cookie (policy|settings|preferences)|subscribe with a free trial|share on facebook",
            RegexOption.IGNORE_CASE,
        )

        const val GROUNDING =
            "Answer only from these results. Quote scores, prices, dates and numbers exactly as they are written there, with their [n], right after each claim they support. " +
                "If results disagree, say so and give each figure with its source. If they do not contain the answer, say plainly that you did not find it - do not guess. " +
                "Name only products, models and versions the results name. Anything you add from your own knowledge gets \"(no source)\" after it. " +
                "Do not add a sources list at the end; the app shows the sources under your answer."

        private fun def(name: String, description: String, properties: JSONObject, required: String) =
            JSONObject().put("type", "function").put("function", JSONObject()
                .put("name", name)
                .put("description", description)
                .put("parameters", JSONObject().put("type", "object").put("properties", properties).put("required", JSONArray().put(required))))

        fun host(url: String): String = runCatching { java.net.URI(url).host?.removePrefix("www.") }.getOrNull() ?: url

        /** The sentences of a page that carry figures or the query's words, about 800 characters of them. */
        fun keyFacts(text: String, query: String, max: Int = 800): String {
            val flat = text.replace(WHITESPACE, " ").trim()
            val terms = Regex("\\p{L}{4,}").findAll(query.lowercase()).map { it.value.take(5) }.toSet()
            val facts = mutableListOf<String>()
            var size = 0
            for (s in flat.split(Regex("(?<=[.!?])\\s+")).flatMap { if (it.length < 400) listOf(it) else it.chunked(240) }.map { it.trim() }) {
                if (s.length <= 24 || s in facts) continue
                if (!s.any(Char::isDigit) && terms.none { s.lowercase().contains(it) }) continue
                if (size + s.length > max) break
                facts += s
                size += s.length
            }
            return if (facts.isEmpty()) flat.take(300) else facts.joinToString(" ")
        }

        /** An MCP result in the words a model reads: its text parts joined, other parts described. */
        fun resultText(r: JSONObject, failed: Boolean): String {
            val parts = mutableListOf<String>()
            var images = 0
            for (c in r.optJSONArray("content").objects()) {
                when (c.optString("type")) {
                    "text" -> c.optString("text").ifBlank { null }?.let(parts::add)
                    "image" -> images++
                    "resource" -> c.optJSONObject("resource")?.let { parts += it.optString("text").ifBlank { "[resource ${it.optString("uri")}]" } }
                    "resource_link" -> parts += "[link ${c.optString("uri")}]"
                    "audio" -> parts += "[audio]"
                }
            }
            if (parts.isEmpty()) r.opt("structuredContent")?.takeIf { it != JSONObject.NULL }?.let { parts += it.toString() }
            if (parts.isEmpty() && images > 0) parts += "[$images image${if (images == 1) "" else "s"}]"
            return parts.joinToString("\n\n").ifEmpty { if (failed) "The tool failed and gave no reason." else "The tool finished and returned nothing." }
        }

        /** A call's arguments, written as an object or as JSON text. */
        fun args(call: JSONObject): JSONObject = when (val a = call.optJSONObject("function")?.opt("arguments")) {
            is JSONObject -> a
            is String -> runCatching { JSONObject(a.trim().removePrefix("```json").removePrefix("```").removeSuffix("```")) }.getOrDefault(JSONObject())
            else -> JSONObject()
        }
    }
}

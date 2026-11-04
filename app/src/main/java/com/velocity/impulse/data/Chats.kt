package com.velocity.impulse.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.random.Random

/** One message, over the panel's own record so fields this app does not know survive a round trip. */
class Message(val raw: JSONObject) {
    val id: String get() = raw.optString("id")
    val role: String get() = raw.optString("role")
    val text: String get() = raw.optString("text")
    val thought: String get() = raw.optString("thought")
    val at: Long get() = raw.optLong("at")
    val error: String? get() = raw.optString("error").ifBlank { null }

    /** The pages the answer cites, as (title, url); its [n] is the n-th. */
    val sources: List<Pair<String, String>> get() = raw.optJSONArray("sources").objects().map { it.optString("title") to it.optString("url") }
    val steps: List<Step> get() = raw.optJSONArray("appSteps").objects().map {
        Step(it.optString("tool"), it.optString("label"), it.optString("detail").ifBlank { null }.takeIf { d -> d != "null" }, it.optBoolean("ok", true))
    }
    val receipt: JSONObject? get() = raw.optJSONObject("receipt")

    /** The answer stopped at its length limit, not by itself: it can be continued. */
    val cut: Boolean get() = receipt?.optBoolean("hit_limit") == true

    /** Names of the files sent with a question. */
    val files: List<String> get() = raw.optJSONArray("files").objects().map { it.optString("name") }

    /** How the answer was asked for: model, quant, window, effort, tools, files, web, done. */
    val meta: JSONObject? get() = raw.optJSONObject("meta")

    /** How many answers this turn has had (1 when it was never asked again), and which one is on screen. */
    val versions: Int get() = raw.optJSONArray("versions")?.length()?.coerceAtLeast(1) ?: 1
    val shown: Int get() = raw.optInt("shown", 0)

    /**
     * This answer with [next], a new answer to the same question, added as its newest version and
     * shown, as the panel keeps them (data/answerVersions.ts): the shown one lives on the message
     * itself, the others in `versions`, and the message keeps its id.
     */
    fun withVersion(next: Message): Message {
        val list = raw.optJSONArray("versions") ?: JSONArray().put(blank(at))
        val versions = JSONArray()
        for (i in 0 until list.length()) versions.put(if (i == shown) take(raw) else list.optJSONObject(i) ?: blank(at))
        versions.put(blank(next.at))
        return Message(JSONObject(next.raw.toString()).put("id", id).put("versions", versions).put("shown", versions.length() - 1))
    }

    /** This answer showing version [j]: the one on screen goes back into its slot and [j] comes out of its. */
    fun showing(j: Int): Message {
        val list = raw.optJSONArray("versions") ?: return this
        if (j == shown || j < 0 || j >= list.length() || raw.optBoolean("streaming")) return this
        val o = JSONObject(raw.toString())
        for (k in VERSION_KEYS) o.remove(k)
        list.optJSONObject(j)?.let { v -> v.keys().forEach { o.put(it, v.get(it)) } }
        val versions = JSONArray()
        for (i in 0 until list.length()) versions.put(if (i == shown) take(raw) else if (i == j) blank(list.optJSONObject(i)?.optLong("at") ?: at) else list.opt(i))
        return Message(o.put("versions", versions).put("shown", j))
    }

    companion object {
        /** What makes one version of an answer; the id and the role are the turn's. */
        private val VERSION_KEYS = listOf("text", "thought", "thoughtMs", "receipt", "error", "sources", "earlier", "steps", "appSteps", "meta", "at")

        private fun take(m: JSONObject) = JSONObject().apply { for (k in VERSION_KEYS) if (m.has(k)) put(k, m.get(k)) }

        private fun blank(at: Long) = JSONObject().put("text", "").put("at", at)

        fun of(role: String, text: String) = Message(
            JSONObject().put("id", newId("m")).put("role", role).put("text", text).put("at", System.currentTimeMillis()),
        )
    }
}

/** One chat in the panel's `session` shape, kept whole so a sync never strips what the panel wrote. */
class Chat(val raw: JSONObject) {
    val id: String get() = raw.optString("id")
    val title: String get() = raw.optString("title")
    val updatedAt: Long get() = raw.optLong("updatedAt")
    val pinned: Boolean get() = raw.optBoolean("pinned")
    val contextName: String? get() = raw.optString("contextName").ifBlank { null }.takeIf { it != "null" }

    /** The personality this chat answers in; none unless chosen in its details. */
    val persona: String? get() = raw.optString("persona").ifBlank { null }.takeIf { it != "null" }
    val messages: List<Message> get() = raw.optJSONArray("messages").objects().map(::Message)

    val systemPrompt: String? get() = (raw.opt("systemPrompt") as? String)?.ifBlank { null }

    /** This chat's own sampler settings (the panel's `options`), over the app's defaults. */
    val options: JSONObject get() = raw.optJSONObject("options") ?: JSONObject()
    val temperature: Double? get() = (options.opt("temperature") as? Number)?.toDouble()
    val maxTokens: Int? get() = (options.opt("max_tokens") as? Number)?.toInt()

    /** Not saved and not synced, and the computer keeps nothing of it. */
    val temporary: Boolean get() = raw.optBoolean("temporary")

    /** The memory this chat writes into, as the service names it after the actor `chat:<id>`. */
    val memoryName: String get() = memoryOf(id)

    /** The last answer's receipt, for the window meter. */
    val lastReceipt: JSONObject? get() = messages.lastOrNull { it.role == "assistant" && it.receipt != null }?.receipt

    /** Web search in this chat; null follows the app's default. */
    val web: Boolean? get() = raw.optJSONObject("appTools")?.opt("web") as? Boolean
    val plugins: Boolean? get() = raw.optJSONObject("appTools")?.opt("plugins") as? Boolean
    val titled: Boolean get() = raw.optBoolean("titled")

    fun edit(block: JSONObject.() -> Unit): Chat = Chat(JSONObject(raw.toString()).apply(block).put("updatedAt", System.currentTimeMillis()))

    fun withMessages(list: List<Message>): Chat = edit { put("messages", JSONArray(list.map { it.raw })) }

    companion object {
        fun start(first: String, model: String?, context: String?): Chat {
            val now = System.currentTimeMillis()
            return Chat(
                JSONObject()
                    .put("id", newId("cs"))
                    .put("title", first.lineSequence().first().take(60))
                    .put("titled", false)
                    .put("createdAt", now)
                    .put("updatedAt", now)
                    .put("modelName", model ?: JSONObject.NULL)
                    .put("contextName", context ?: JSONObject.NULL)
                    .put("pinned", false)
                    .put("messages", JSONArray()),
            )
        }
    }
}

/** One thing the model did with a tool on the way to an answer, e.g. ("web_search", "Searched the web", query, true). */
data class Step(val tool: String, val label: String, val detail: String?, val ok: Boolean) {
    fun json(): JSONObject = JSONObject().put("tool", tool).put("label", label).put("detail", detail ?: JSONObject.NULL).put("ok", ok)
}

/** The service's name for the memory of actor `chat:<id>` (runtime.rs default_context_name). */
fun memoryOf(chatId: String): String {
    val cleaned = "chat:$chatId".trim().lowercase().replace(Regex("[^a-z0-9]"), "-").trim('-')
    return if (cleaned.length > 40) cleaned.take(40).trimEnd('-') else cleaned
}

/**
 * How answers are written, set in the panel's Settings › Chat and shared through the profile's
 * `vls.chat.style`. Kept whole, so fields this app does not know survive a round trip.
 */
class ChatStyle(val raw: JSONObject = JSONObject()) {
    private fun pick(key: String, ok: List<String>, d: String) = raw.optString(key).takeIf { it in ok } ?: d

    /** sarcastic, friendly, pro, nerd, chill or off. */
    val persona: String get() = raw.optString("persona").let { if (it == "plain") "pro" else it }.takeIf { it in PERSONAS } ?: "sarcastic"
    val personaSet: Boolean get() = raw.optBoolean("personaSet") || (raw.has("persona") && persona != "sarcastic")
    /** concise, balanced or detailed. */
    val tone: String get() = pick("tone", listOf("concise", "balanced", "detailed"), "balanced")
    /** none, some or lots. */
    val emoji: String get() = pick("emoji", listOf("none", "some", "lots"), "some")
    /** asked, auto or often. */
    val visuals: String get() = pick("visuals", listOf("asked", "auto", "often"), "often")
    val visualsSet: Boolean get() = raw.optBoolean("visualsSet") || visuals == "asked" || visuals == "auto"
    /** rich or plain. */
    val layout: String get() = pick("layout", listOf("rich", "plain"), "rich")
    /** auto (the language of each message) or a code from [LANGUAGES]. */
    val lang: String get() = pick("lang", listOf("auto") + LANGUAGES.keys, "auto")
    val byName: Boolean get() = raw.optBoolean("byName", false)
    val custom: String get() = raw.optString("custom").take(CUSTOM_MAX_CHARS)
    val thinkOpen: Boolean get() = raw.optBoolean("thinkOpen", false)
    val autoTitle: Boolean get() = raw.optBoolean("autoTitle", true)
    /** Longest answer in tokens, thinking included; 0 is as much as the window allows. */
    val maxTokens: Int get() = raw.optInt("maxTokens", 8192).takeIf { it in LIMITS } ?: 8192
    /** Whether a new chat starts with web search on: auto, on or off. */
    val search: String get() = pick("search", listOf("auto", "on", "off"), "auto")

    fun with(key: String, value: Any?): ChatStyle = ChatStyle(JSONObject(raw.toString()).apply {
        if (value == null) remove(key) else put(key, value)
        if (key == "persona") put("personaSet", true)
        if (key == "visuals") put("visualsSet", true)
    })

    /** The style with what the loaded model gets until the person picks: chill and fewer pictures on a small model. */
    fun forModel(small: Boolean): ChatStyle = if (!small) this else ChatStyle(JSONObject(raw.toString()).apply {
        if (!personaSet) put("persona", "chill")
        if (!visualsSet) put("visuals", "auto")
    })

    /** The lines the style adds to the system prompt, the personality ([persona], the chat's or this one's) first. */
    fun lines(persona: String, name: String): String {
        val list = mutableListOf(
            PERSONA_TEXT[persona].orEmpty(),
            mapOf("concise" to "Keep answers short: the answer first, a few sentences, no preamble.", "detailed" to "Give thorough, detailed answers with examples and the reasoning behind them.")[tone].orEmpty(),
            mapOf("none" to "Never use emoji.", "lots" to "Use emoji generously.")[emoji].orEmpty(),
            mapOf("asked" to "Add a chart, diagram or table only when the user asks for one.", "auto" to "Draw only when the message asks for a chart/diagram/table or names data to compare; otherwise answer in words.")[visuals].orEmpty(),
            if (layout == "plain") "Write in plain paragraphs: no headings or bullet lists unless asked." else "",
            if (byName && name.isNotBlank()) "The user's name is ${JSONObject.quote(name)}; address them by it now and then." else "",
        ).filter { it.isNotBlank() }.toMutableList()
        custom.trim().takeIf { it.isNotEmpty() }?.let { list += "The user's own instructions for how to answer:\n$it" }
        LANGUAGES[lang]?.let { list += "Always answer in $it, whatever language the user writes in or the conversation was in. This overrides any rule about replying in the user's language." }
        return if (list.isEmpty()) "" else "# The user's preferences (these win over the rules above)\n- " + list.joinToString("\n- ")
    }

    /** The answer language, sent with the latest message every turn: a system-prompt rule fades under long history. */
    fun langNote(text: String): String = LANGUAGES[lang]?.let { "Answer in $it, whatever language the user writes in." } ?: when {
        Speech.isPolish(text) -> "Answer in Polish, the language of the user's message, even if attached files or earlier messages are in another language: every sentence and every diagram, chart and table label in Polish, no English sentences. Plain, correct Polish a native speaker would say. Direct, the way a smart friend talks: the answer first, no intro or wrap-up."
        Speech.isEnglish(text) -> "Answer in English, the language of the user's message, even if the chat, attached files or earlier messages are in another language: every sentence and every label in English. Direct, the way a smart friend talks: the answer first, no intro or wrap-up."
        else -> "Answer in the language of the user's message, even if attached files or earlier messages are in another language."
    }

    companion object {
        const val KEY = "vls.chat.style"
        private const val CUSTOM_MAX_CHARS = 1600
        val LIMITS = listOf(1024, 2048, 4096, 8192, 16384, 32768, 0)
        val PERSONAS = listOf("sarcastic", "friendly", "pro", "nerd", "chill", "off")
        /** Languages answers can be fixed to: code and the English name told to the model. */
        val LANGUAGES = linkedMapOf(
            "pl" to "Polish", "en" to "English", "de" to "German", "es" to "Spanish", "fr" to "French", "it" to "Italian", "pt" to "Portuguese",
            "nl" to "Dutch", "cs" to "Czech", "uk" to "Ukrainian", "ru" to "Russian", "tr" to "Turkish", "ja" to "Japanese", "zh" to "Simplified Chinese", "ko" to "Korean",
        )
        private const val PERSONA_RULES = "Never start two answers the same way; vary the first words. The personality never shortens or drops what the user asked for. Facts first and correct; never insult the user; on a serious or upsetting topic (health, safety, grief, a crisis) drop the act and be kind and plain."
        private val PERSONA_TEXT: Map<String, String> = Personas.associate { it.id to if (it.id == "sarcastic") it.prompt else it.prompt.trimEnd() + " " + PERSONA_RULES }
    }
}

/** Telling the reply language from a message, the way the panel does (data/lang.ts). */
object Speech {
    private val ENGLISH = Regex("(?:^|[^\\p{L}])(?:the|and|is|are|what|how|why|please|can|you|this|that|with|make|write|it|of|for|in|my|me|do|does|an|be|from|your|about|create|show|give|find|tell|which|who|there|have|has|was|will|would|could|should|i'm|it's|many|much|until|when|where|did|need|want|know|today|tomorrow|days|weather|price|cost)(?=$|[^\\p{L}])", RegexOption.IGNORE_CASE)
    private val POLISH_WORDS = Regex("(?:^|[^\\p{L}])(?:mozesz|umiesz|szukac|internecie|necie|jestes|narysuj|napisz|zrob|policz|oblicz|pokaz|opowiedz|wyjasnij|stworz|przygotuj|sprawdz|znajdz|porownaj|przetlumacz|stresc|podsumuj|wygeneruj|zapisz|prosze|dzieki|czesc|hejka|siema|\\p{L}{3,}(?:acji|owania|owaniu|nosci))(?=$|[^\\p{L}])", RegexOption.IGNORE_CASE)
    private val COMMON_PL = Regex("(?:^|[^\\p{L}])(?:jest|czy|nie|jak|co|ile|się|mój|moja|moje|to|w|na|dla|jaki|jaka|kto|gdzie|kiedy|tam|mi|mnie|już|też|ale|bo|mam|masz|potrafisz|dzis|dzisiaj|ktor\\p{L}*|sie|juz|tez|mamy|jestem|bylo|bedzie|zeby|teraz|wczoraj|jutro)(?=$|[^\\p{L}])")
    private val SHORT_EN = Regex("^\\s*(?:thanks?|thank you|thx|ty|whats|what's|hey|hello|hi|yo|ok(?:ay)?|cool|nice|great|sure|yes|yeah|nope|please|sorry|good (?:morning|night|evening))(?!\\p{L})[^\\p{L}]*(?:\\p{L}+[^\\p{L}]*){0,2}$", RegexOption.IGNORE_CASE)

    fun isPolish(raw: String): Boolean {
        val text = raw.replace(Regex("(?<!\\p{L})z[łl](?!\\p{L})", RegexOption.IGNORE_CASE), " ")
        return Regex("[ąćęłńśźż]", RegexOption.IGNORE_CASE).containsMatchIn(text) ||
            Regex("\\s(?:to|jest)\\?\\s*$", RegexOption.IGNORE_CASE).containsMatchIn(text) ||
            POLISH_WORDS.containsMatchIn(text) ||
            (Regex("ó", RegexOption.IGNORE_CASE).containsMatchIn(text) && !Regex("[áéíúñãõç]", RegexOption.IGNORE_CASE).containsMatchIn(text) &&
                Regex("(?:^|[^\\p{L}])(?:i|z|w|na|do|nie|się|jak|co|czy|mi)(?=$|[^\\p{L}])", RegexOption.IGNORE_CASE).containsMatchIn(text)) ||
            COMMON_PL.findAll(text.lowercase()).count() >= maxOf(2, ENGLISH.findAll(text).count() + 1)
    }

    fun isEnglish(text: String): Boolean {
        if (isPolish(text)) return false
        val n = ENGLISH.findAll(text).count()
        return n >= 2 || SHORT_EN.matches(text) || (text.trim().split(Regex("\\s+")).size <= 4 && n >= 1)
    }
}

/**
 * One turn as it goes to the model, built the way the panel builds it (data/turnWire.ts): the
 * system prompt holds only what is the same every turn of a chat, so the service's prompt cache
 * keeps the whole history; what belongs to this turn rides in the latest message.
 */
object Wire {
    const val CONTEXT_HEAD = "(Note from the app for this message, not from the user:)"
    const val CONTEXT_END = "(End of the app's note.)"
    private const val NOTE_HEAD = "[Guidance for this reply; the user never wrote or sees it, so never mention it:"
    private const val CLOSED = "(No answer was given to this; that request is closed. Answer only the newest message.)"
    const val CONTINUE = "Continue exactly where your last answer stopped, in the same language. Do not repeat what you already wrote."
    const val TITLE = "You name conversations. Reply with a title of 2 to 6 words in the language of the conversation. No quotes, no punctuation at the end, nothing else."

    /** The assistant's standing instructions, unless the chat sets its own. */
    fun rules(vision: Boolean) = listOf(
        "Today is ${java.time.LocalDate.now()}.${if (vision) " You can see pictures the user attaches and read text in them." else ""}",
        "",
        "# How to answer",
        "- Reply in the language of the user's latest message (unless the preferences below fix it): every sentence, heading and table label; English only for names and technical terms. If you think before answering, think in the language of the user's latest message.",
        "- Answer only the user's latest message: earlier requests are finished or dropped. Lead with the answer; go deeper only when asked. Use what the user told you earlier.",
        "- Talk like a smart friend: plain, natural, direct words in short sentences. Start with the answer itself and stop when it is said.",
        "- Never invent facts, numbers, quotes, sources or links. If unsure, say so briefly.",
        "- A word with several meanings keeps the meaning it has in this conversation.",
        "- When the user corrects a word or says you misunderstood, accept it at once and redo the answer with their meaning. Never argue.",
        "- Small talk: one or two friendly sentences. Never introduce yourself, name the app or address the user by name unless asked.",
        "- Asked about internet access, your tools or what you can do: answer from the Environment block in plain words. Never say you have no internet access: with web search on, say yes; with it off, say it is off now.",
        "- Markdown: headings, lists, **bold** and | tables | only when they help (never in a code block); code in fenced blocks with a language.",
    ).joinToString("\n")

    /** The environment as standing facts: the date, the person, the window, what is switched on. */
    fun environment(name: String, lang: String?, window: Int, web: Boolean, plugins: List<String>, vision: Boolean): String {
        val on = mutableListOf<String>()
        if (web) on += "web search and reading web pages"
        if (plugins.isNotEmpty()) on += "plugins on the computer: ${plugins.joinToString(", ")}"
        on += "reading attached files"
        if (vision) on += "looking at attached pictures"
        val language = lang ?: "the one each message is written in"
        return listOfNotNull(
            "# Environment (this wins over anything said earlier in the chat)",
            "Date: ${java.time.LocalDate.now()}",
            if (name.isNotBlank()) "User: $name; language: $language" else "User's language: $language",
            if (window > 0) "Model window: ${window / 1024}K tokens" else null,
            "Switched on:",
            on.joinToString("\n") { "- $it" },
            if (web) "Web search: ON." else "Web search: OFF.",
        ).joinToString("\n")
    }

    /** This turn's time and files, for the app's note in front of the latest message. */
    fun now(files: List<String>): String {
        val t = java.time.ZonedDateTime.now()
        val time = "Time: %02d:%02d (%s)".format(t.hour, t.minute, t.zone.id)
        val kinds = files.joinToString(", ") { "$it (${it.substringAfterLast('.', "file").uppercase()})" }
        return if (files.isEmpty()) time else "$time\nAttached files: $kinds"
    }

    /** The conversation as the model reads it: failed answers left out, files named, a request that got no answer closed. */
    fun history(messages: List<Message>): MutableList<JSONObject> {
        val out = mutableListOf<JSONObject>()
        for (m in messages) {
            if (m.role != "user" && m.role != "assistant") continue
            if (m.role == "assistant" && m.error != null) continue
            val attached = if (m.role == "user" && m.files.isNotEmpty()) "(attached: ${m.files.joinToString(", ")})" else ""
            val said = m.text.trim()
            val content = if (said.isNotEmpty() && attached.isNotEmpty()) "$attached\n$said" else said.ifEmpty { attached }
            if (content.isEmpty()) continue
            var last = out.lastOrNull()
            if (m.role == "user" && last?.optString("role") == "user") {
                out += JSONObject().put("role", "assistant").put("content", CLOSED)
                last = out.last()
            }
            if (last != null && last.optString("role") == m.role) last.put("content", last.optString("content") + "\n\n" + content)
            else out += JSONObject().put("role", m.role).put("content", content)
        }
        return out
    }

    /** [content] with [block] added to the app's part in front of it. */
    fun addContext(content: String, block: String): String {
        if (block.isBlank()) return content
        val end = content.indexOf(CONTEXT_END)
        if (end >= 0) return "${content.substring(0, end).trimEnd()}\n\n${block.trim()}\n${content.substring(end)}"
        return "$CONTEXT_HEAD\n${block.trim()}\n$CONTEXT_END\n\n$content"
    }

    /** The note that closes the latest message: the answer language, last, marked as the app's. */
    fun note(style: ChatStyle, text: String) = "$NOTE_HEAD ${style.langNote(text)}]"
}

/** Ids the way the panel makes them: a prefix, the time in base 36, and five random characters. */
fun newId(prefix: String): String {
    val tail = (1..5).map { "abcdefghijklmnopqrstuvwxyz0123456789"[Random.nextInt(36)] }.joinToString("")
    return "${prefix}_${System.currentTimeMillis().toString(36)}_$tail"
}

/**
 * The phone's copy of the profile's chats and where it stands against the service: the newest
 * revision seen ([head]), each chat's revision, and the chats changed here and not yet sent.
 */
class ChatCache(private val file: File) {
    var head = 0L
    val revs = HashMap<String, Long>()
    val dirty = HashSet<String>()
    val gone = HashSet<String>()

    fun load(): List<Chat> = runCatching {
        val o = JSONObject(file.readText())
        head = o.optLong("head")
        o.optJSONObject("revs")?.let { r -> r.keys().forEach { revs[it] = r.optLong(it) } }
        o.optJSONArray("dirty")?.let { a -> (0 until a.length()).forEach { dirty += a.getString(it) } }
        o.optJSONArray("gone")?.let { a -> (0 until a.length()).forEach { gone += a.getString(it) } }
        o.optJSONArray("chats").objects().map(::Chat)
    }.getOrDefault(emptyList())

    /**
     * Copies where the cache stands now, on the thread that changes it; the returned write is
     * safe to run elsewhere. Chats are never changed in place, so the list itself is not copied.
     */
    fun snapshot(chats: List<Chat>): () -> Unit {
        val h = head
        val r = HashMap(revs)
        val d = dirty.toList()
        val g = gone.toList()
        return {
            val o = JSONObject()
                .put("head", h)
                .put("revs", JSONObject(r as Map<*, *>))
                .put("dirty", JSONArray(d))
                .put("gone", JSONArray(g))
                .put("chats", JSONArray(chats.map { it.raw }))
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(o.toString())
            tmp.renameTo(file)
        }
    }

    fun forget() {
        head = 0
        revs.clear()
        dirty.clear()
        gone.clear()
        file.delete()
    }
}

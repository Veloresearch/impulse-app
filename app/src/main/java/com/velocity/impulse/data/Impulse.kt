package com.velocity.impulse.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ImpulseError(message: String, val code: String? = null, val status: Int = 0) : Exception(message)

data class Profile(val id: String, val name: String, val avatar: String?, val photo: String?, val hasPassword: Boolean, val remote: Boolean)
data class Context(val name: String, val tokens: Long?, val documents: Int)

/** A model file in the library, as `/api/v2/models` describes it. */
data class Probe(val name: String, val path: String, val format: String, val params: Long, val bytes: Long, val trainContext: Int, val family: String = "")

data class Loaded(val id: String, val name: String, val path: String, val context: Int, val thinks: Boolean, val family: String = "", val vision: Boolean = false)

data class Status(val model: Loaded?, val busy: Boolean)

sealed interface Delta {
    data class Thinking(val text: String) : Delta
    data class Text(val text: String) : Delta
    data class Queued(val ahead: Int) : Delta
    data class Done(val receipt: JSONObject) : Delta

    /** A tool call as the model writes it, raw. */
    data class ToolText(val text: String) : Delta

    /** The calls the reply made, parsed by the service (OpenAI shape), just before [Done]. */
    data class ToolCalls(val calls: JSONArray) : Delta

    /** A call the service could not read, whole. */
    data class ToolUnparsed(val raw: String) : Delta
}

/**
 * Where Impulse is. [accessId] and [accessSecret] are a Cloudflare Access service token, for a
 * tunnel that sits behind Zero Trust.
 */
data class Server(val base: String, val accessId: String? = null, val accessSecret: String? = null) {
    companion object {
        /** A scanned or typed address: a plain URL or `impulse://connect?u=…&cid=…&csec=…`. */
        fun parse(raw: String): Server {
            val s = raw.trim()
            if (!s.startsWith("impulse://")) return Server(Impulse.normalize(s))
            val q = android.net.Uri.parse(s)
            val u = q.getQueryParameter("u") ?: throw ImpulseError("This code has no address in it.")
            return Server(Impulse.normalize(u), q.getQueryParameter("cid"), q.getQueryParameter("csec"))
        }
    }
}

/** The parts of Impulse's HTTP API the panel uses, spoken the way the panel speaks them. */
class Impulse(private val server: Server, private val token: String? = null) {
    val base get() = server.base

    private fun request(path: String) = Request.Builder().url(base + path).apply {
        header("User-Agent", AGENT)
        if (token != null) header("Authorization", "Bearer $token")
        if (server.accessId != null && server.accessSecret != null) {
            header("CF-Access-Client-Id", server.accessId)
            header("CF-Access-Client-Secret", server.accessSecret)
        }
    }

    /** Cancelling the caller cancels the request, so Stop never waits on a slow search or plugin. */
    private suspend fun call(path: String, body: JSONObject? = null, client: OkHttpClient = quick): JSONObject {
        val r = request(path).apply { if (body != null) post(body.toString().toRequestBody(JSON)) }.build()
        val c = client.newCall(r)
        val resp = try {
            suspendCancellableCoroutine<Response> { cont ->
                cont.invokeOnCancellation { c.cancel() }
                c.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) = cont.resumeWithException(e)
                    override fun onResponse(call: Call, response: Response) = cont.resume(response)
                })
            }
        } catch (e: IOException) {
            throw ImpulseError("Cannot reach Impulse at ${base.substringAfter("://")}.")
        }
        return withContext(Dispatchers.IO) { read(resp) }
    }

    private fun read(response: Response): JSONObject =
        try {
            response.use { resp ->
                val text = resp.body?.string().orEmpty()
                val json = runCatching { JSONObject(text) }.getOrNull()
                if (!resp.isSuccessful) {
                    val e = json?.opt("error")
                    val message = (e as? JSONObject)?.optString("message") ?: (e as? String)
                    throw ImpulseError(message?.takeIf { it.isNotBlank() } ?: "Impulse answered ${resp.code}.", (e as? JSONObject)?.optString("code"), resp.code)
                }
                json ?: JSONObject()
            }
        } catch (e: IOException) {
            throw ImpulseError("Cannot reach Impulse at ${base.substringAfter("://")}.")
        }

    suspend fun profiles(): List<Profile> = call("/api/v2/profiles").optJSONArray("profiles").objects().map {
        Profile(
            it.getString("id"), it.optString("name"), it.optString("avatar").takeIf { a -> a.isNotBlank() && a != "null" },
            it.optString("photo").takeIf { p -> p.startsWith("data:image/") },
            it.optBoolean("has_password"), it.optBoolean("remote"),
        )
    }

    /** Signs in and returns the session token every later call carries. */
    suspend fun login(id: String, password: String): String =
        call("/api/v2/profiles/login", JSONObject().put("id", id).put("password", password)).optString("session")
            .ifBlank { throw ImpulseError("Impulse did not start a session.") }

    /** The signed-in profile: its name, its tofu or emoji, and its photo. */
    suspend fun me(): Profile? = call("/api/v2/profile").takeIf { it.optBoolean("exists") }?.let {
        Profile(
            it.optString("id"), it.optString("name"), it.optString("avatar").takeIf { a -> a.isNotBlank() && a != "null" },
            it.optString("photo").takeIf { p -> p.startsWith("data:image/") }, it.optBoolean("has_password"), it.optBoolean("remote_allowed"),
        )
    }

    suspend fun logout() {
        runCatching { call("/api/v2/profiles/logout", JSONObject()) }
    }

    suspend fun status(): Status {
        val s = call("/api/v2/status")
        val m = s.optJSONObject("model")
        return Status(
            m?.let {
                val probe = it.optJSONObject("probe")
                Loaded(
                    it.optString("id"), probe?.optString("name")?.ifBlank { null } ?: it.optString("id"), probe?.optString("path").orEmpty(),
                    it.optInt("context"), it.optBoolean("thinking_template"), probe?.optString("family").orEmpty(), it.optBoolean("vision"),
                )
            },
            s.optBoolean("busy"),
        )
    }

    suspend fun library(): List<Probe> = call("/api/v2/models").optJSONArray("models").objects()
        .filter { it.optString("format") == "gguf" || it.optString("format") == "mfy" }
        .map { Probe(it.optString("name"), it.optString("path"), it.optString("format"), it.optLong("params"), it.optLong("file_bytes"), it.optInt("train_context"), it.optString("family")) }
        .sortedBy { it.name.lowercase() }

    /** Loads a model the way the panel's picker does: its trained window, capped at 8K. */
    suspend fun load(p: Probe) {
        val context = if (p.format == "mfy") 33_554_432 else minOf(p.trainContext.takeIf { it > 0 } ?: 8192, 8192)
        call("/api/v2/load", JSONObject().put("path", p.path).put("context", context).put("gpu_layers", JSONObject.NULL), slow)
    }

    /** A publisher's picture from Hugging Face, kept by the service, as a data: URL. */
    suspend fun hubAvatar(author: String): String? =
        call("/api/v2/hub/avatar", JSONObject().put("author", author)).optString("avatar").takeIf { it.startsWith("data:image/") }

    suspend fun unload() {
        call("/api/v2/unload", JSONObject())
    }

    /** The profile's panel settings: each value is the JSON the panel stored. */
    suspend fun prefs(): JSONObject = call("/api/v2/prefs").optJSONObject("prefs") ?: JSONObject()

    suspend fun contexts(): List<Context> = call("/v1/velocity/contexts").optJSONArray("data").objects().map {
        Context(it.getString("name"), it.optLong("addressable_tokens").takeIf { n -> n > 0 }, it.optInt("documents"))
    }

    suspend fun pull(since: Long): JSONObject = call("/api/v2/chats/pull", JSONObject().put("since", since))

    suspend fun push(chats: JSONArray): JSONObject = call("/api/v2/chats/push", JSONObject().put("chats", chats))

    /** The service's web search: up to six results, the top [read] of them read in full. */
    suspend fun webSearch(q: String, read: Int): List<JSONObject> =
        call("/api/v2/web/search", JSONObject().put("q", q).put("max", 6).put("read", read), long).optJSONArray("results").objects()

    /** One public page as readable text: `{url, title, text}`. */
    suspend fun webPage(url: String, max: Int = 3500): JSONObject = call("/api/v2/web/page", JSONObject().put("url", url).put("max", max), long)

    /** Every tool of every switched-on plugin; [refresh] wakes a plugin that never said what it offers. */
    suspend fun mcpTools(refresh: Boolean): List<JSONObject> = call("/api/v2/mcp/tools", JSONObject().put("refresh", refresh), long).optJSONArray("tools").objects()

    suspend fun mcpCall(name: String, args: JSONObject, timeoutMs: Int = 60_000): JSONObject =
        call("/api/v2/mcp/call", JSONObject().put("name", name).put("arguments", args).put("timeout_ms", timeoutMs), long)

    /**
     * One turn on the panel's own endpoint: words, the model's thinking, the queue, the calls it
     * made when [tools] were offered, and the receipt at the end. [actor] is the chat's id, so each
     * chat keeps its own memory; [store] answers from a named context. Cancelling the collector
     * cancels the request.
     */
    fun turn(
        messages: List<JSONObject>,
        actor: String,
        store: String?,
        effort: String?,
        images: List<String> = emptyList(),
        tools: JSONArray? = null,
        temperature: Double? = null,
        maxTokens: Int? = null,
    ): Flow<Delta> = callbackFlow {
        val body = JSONObject()
            .put("messages", JSONArray(messages))
            .put("actor", actor)
        if (store != null) body.put("store", store)
        if (images.isNotEmpty()) body.put("images", JSONArray(images))
        if (tools != null && tools.length() > 0) body.put("tools", tools)
        if (temperature != null) body.put("temperature", temperature)
        if (maxTokens != null) body.put("max_tokens", maxTokens)
        when (effort) {
            null -> {}
            "off" -> body.put("thinking", false)
            else -> body.put("thinking", true).put("reasoning_effort", effort)
        }
        val c: Call = slow.newCall(request("/api/v2/chat").post(body.toString().toRequestBody(JSON)).build())
        try {
            c.execute().use { resp ->
                if (!resp.isSuccessful) {
                    val text = resp.body?.string().orEmpty()
                    val e = runCatching { JSONObject(text).opt("error") }.getOrNull()
                    val message = (e as? JSONObject)?.optString("message") ?: (e as? String)
                    throw ImpulseError(message?.ifBlank { null } ?: "Impulse answered ${resp.code}.", status = resp.code)
                }
                val source = resp.body!!.source()
                var done = false
                while (!done && !source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data.isEmpty() || data == "[DONE]") continue
                    val ev = runCatching { JSONObject(data) }.getOrNull() ?: continue
                    when {
                        ev.opt("delta") is String -> trySend(Delta.Text(ev.getString("delta")))
                        ev.opt("reasoning") is String -> trySend(Delta.Thinking(ev.getString("reasoning")))
                        ev.opt("tool_delta") is String -> trySend(Delta.ToolText(ev.getString("tool_delta")))
                        ev.opt("tool_calls") is JSONArray -> trySend(Delta.ToolCalls(ev.getJSONArray("tool_calls")))
                        ev.opt("tool_unparsed") is String -> trySend(Delta.ToolUnparsed(ev.getString("tool_unparsed")))
                        ev.has("queued") -> trySend(Delta.Queued(ev.optInt("queued")))
                        ev.has("done") -> {
                            trySend(Delta.Done(ev.optJSONObject("done") ?: JSONObject()))
                            done = true
                        }
                        ev.has("error") -> throw ImpulseError(ev.optString("error"))
                    }
                }
                if (!done) throw ImpulseError("The connection to Impulse closed before the answer finished.")
            }
            close()
        } catch (e: IOException) {
            close(if (c.isCanceled()) null else ImpulseError("The connection to Impulse dropped mid-answer."))
        } catch (e: Exception) {
            close(e)
        }
        awaitClose { c.cancel() }
    }.flowOn(Dispatchers.IO)

    companion object {
        private val AGENT = "ImpulseApp/${com.velocity.impulse.BuildConfig.VERSION_NAME} (${android.os.Build.MODEL})"
        private val JSON = "application/json".toMediaType()
        private val quick = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        private val slow = quick.newBuilder().readTimeout(0, TimeUnit.SECONDS).build()
        private val long = quick.newBuilder().readTimeout(120, TimeUnit.SECONDS).build()

        /** "192.168.1.20" → http://192.168.1.20:11500. A scheme or a port someone typed is kept. */
        fun normalize(raw: String): String {
            var s = raw.trim().trimEnd('/')
            if (!s.contains("://")) s = "http://$s"
            val host = s.substringAfter("://").substringBefore('/')
            if (s.startsWith("http://") && !host.contains(':')) s = s.replaceFirst(host, "$host:11500")
            return s.substringBefore("/#").trimEnd('/')
        }
    }
}

fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

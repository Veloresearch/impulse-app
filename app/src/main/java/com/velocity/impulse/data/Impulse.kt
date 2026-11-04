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

/**
 * A memory as `/api/v2/contexts` lists it. [tokens] is in the loaded model's tokens, null until
 * that model has read it; [capacity] is how much it may hold.
 */
data class Context(val name: String, val tokens: Long?, val documents: Int, val pages: Int = 0, val capacity: Long? = null)

/** What a load would do on this computer (`/api/v2/probe`): where the layers go and what it costs. */
data class Plan(
    val backend: String,
    val gpuLayers: Int?,
    val context: Int,
    val vramWeightsMb: Long,
    val vramKvMb: Long,
    val reasons: List<String>,
    val warnings: List<String>,
    /** The model as the service read it: name, params, train_context, layers… */
    val probe: JSONObject,
)

/** A Hugging Face repository in search results. */
data class HubRepo(val id: String, val author: String, val name: String, val downloads: Long, val likes: Long, val updated: String, val gated: Boolean, val builds: Int, val quants: List<String>, val task: String?)

/** One GGUF in a repository with what it costs here; [verdict] is the service's words, e.g. "Best fit". */
data class HubFile(val path: String, val quant: String, val kind: String, val loadable: Boolean, val bytes: Long, val vramNeeded: Long, val ramNeeded: Long, val verdict: String, val recommended: Boolean)

data class HubModel(val id: String, val ctx: Int, val vramTotalMb: Long, val ramTotalMb: Long, val gpu: Boolean, val files: List<HubFile>)

/** A model transfer the service runs. [state]: queued, running, verifying, paused, failed or done. */
data class Download(val name: String, val repo: String, val have: Long, val total: Long, val bytesPerSecond: Long, val etaSeconds: Long, val state: String, val error: String, val percent: Double)

/** A phone or browser signed in from outside the computer. [via]: lan, tailscale or cloudflare; [seenAgo] in seconds. */
data class Device(val id: String, val name: String, val app: Boolean, val via: String, val ip: String, val profileName: String?, val seenAgo: Long, val signedInAgo: Long)

/** Keep the computer awake: [mode] off, working or always; [holding] it is held awake now. */
data class KeepAwake(val mode: String, val holding: Boolean, val reason: String)

/** One thing written into a memory, newest first. [tokens] is null until the loaded model has read it. */
data class Entry(val at: Long, val actor: String, val source: String, val bytes: Long, val tokens: Long?, val position: Long, val preview: String, val truncated: Boolean)

data class Hit(val position: Long, val text: String, val actor: String, val at: Long, val source: String)

/** A memory being built. [phase]: ingest or tokens; [etaSeconds] null while unknown. */
data class Building(val name: String, val stage: String, val done: Long, val total: Long, val phase: String, val etaSeconds: Long?)

/** What `/api/v2/contexts/write` did: the memory written into and its size after, in the loaded model's tokens when there is one. */
data class Written(val context: String, val added: Long, val total: Long, val tokens: Long?)

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
    suspend fun me(): Profile? = profile(call("/api/v2/profile"))

    private fun profile(o: JSONObject): Profile? = o.takeIf { it.optBoolean("exists") }?.let {
        Profile(
            it.optString("id"), it.optString("name"), it.optString("avatar").takeIf { a -> a.isNotBlank() && a != "null" },
            it.optString("photo").takeIf { p -> p.startsWith("data:image/") }, it.optBoolean("has_password"), it.optBoolean("remote_allowed"),
        )
    }

    /** Changes the signed-in profile's [name], [avatar] (one emoji, or "" for none) or [photo] (a data:image URL, "" to remove). */
    suspend fun setProfile(name: String? = null, avatar: String? = null, photo: String? = null): Profile? =
        profile(call("/api/v2/profile", JSONObject().apply {
            name?.let { put("name", it) }
            avatar?.let { put("avatar", it.ifEmpty { JSONObject.NULL }) }
            photo?.let { put("photo", it.ifEmpty { JSONObject.NULL }) }
        }))

    /** Sets or changes the password ([next]), or takes it off (null); [current] is asked when there is one. */
    suspend fun setPassword(current: String?, next: String?): Profile? =
        profile(call("/api/v2/profile/password", JSONObject().put("current", current ?: JSONObject.NULL).put("password", next ?: JSONObject.NULL)))

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

    /** What loading [path] with a [context] window (and [backend]) would do here, before it is loaded. */
    suspend fun probe(path: String, context: Int? = null, backend: String? = null): Plan {
        val r = call("/api/v2/probe", JSONObject().put("path", path).apply {
            context?.let { put("context", it) }
            backend?.let { put("backend", it) }
        }, long)
        val p = r.optJSONObject("plan") ?: JSONObject()
        return Plan(
            p.optString("backend"), p.optInt("gpu_layers", -1).takeIf { it >= 0 && !p.isNull("gpu_layers") }, p.optInt("context"),
            p.optLong("vram_weights_mb"), p.optLong("vram_kv_mb"), p.optJSONArray("reasons").strings(), p.optJSONArray("warnings").strings(),
            r.optJSONObject("probe") ?: JSONObject(),
        )
    }

    /**
     * Loads a model with the load dialog's choices: [gpuLayers] null lets the service decide,
     * [tuning] is the panel's LoadTuning (batch, threads, flash_attn, n_cpu_moe…), [remember] saves
     * these as the model's own settings. Returns the service's status of the loaded model.
     */
    suspend fun load(path: String, context: Int, gpuLayers: Int? = null, backend: String? = null, tuning: JSONObject? = null, remember: Boolean = false): JSONObject =
        call("/api/v2/load", JSONObject().put("path", path).put("context", context).put("gpu_layers", gpuLayers ?: JSONObject.NULL).put("remember", remember).apply {
            backend?.let { put("backend", it) }
            tuning?.let { put("tuning", it) }
        }, slow)

    /** Deletes a model file from the library; the service refuses the loaded one. */
    suspend fun removeModel(path: String) {
        call("/api/v2/models/remove", JSONObject().put("path", path))
    }

    /** The load settings saved for a model, or null; [reset] forgets them, [thinking] notes the thinking choice for it. */
    suspend fun modelSettings(path: String, reset: Boolean? = null, thinking: Boolean? = null): JSONObject? =
        call("/api/v2/models/settings", JSONObject().put("path", path).apply {
            reset?.let { put("reset", it) }
            thinking?.let { put("thinking", it) }
        }).optJSONObject("settings")

    /** Hugging Face search. [sort]: downloads, likes, trending or updated. Returns the repos and the cursor of the next page. */
    suspend fun hubSearch(q: String, sort: String = "downloads", author: String = "", limit: Int = 24, cursor: String = ""): Pair<List<HubRepo>, String?> {
        val r = call("/api/v2/hub/search", JSONObject().put("q", q).put("sort", sort).put("author", author).put("limit", limit).put("cursor", cursor), long)
        return r.optJSONArray("models").objects().map {
            HubRepo(
                it.optString("id"), it.optString("author"), it.optString("name"), it.optLong("downloads"), it.optLong("likes"), it.optString("updated"),
                it.optBoolean("gated"), it.optInt("builds"), it.optJSONArray("quants").strings(), it.optString("task").takeIf { t -> t.isNotBlank() && t != "null" },
            )
        } to r.optString("next").takeIf { it.isNotBlank() && it != "null" }
    }

    /** A repository's GGUF files, each with what it would cost on this computer at a [ctx] window. */
    suspend fun hubModel(id: String, ctx: Int = 8192): HubModel {
        val r = call("/api/v2/hub/model", JSONObject().put("id", id).put("ctx", ctx), long)
        val hw = r.optJSONObject("hardware") ?: JSONObject()
        return HubModel(
            r.optString("id"), r.optInt("ctx"), hw.optLong("vram_total_mb"), hw.optLong("ram_total_mb"), hw.optBoolean("gpu"),
            r.optJSONArray("files").objects().map {
                HubFile(
                    it.optString("path"), it.optString("quant"), it.optString("kind"), it.optBoolean("loadable"), it.optLong("download"),
                    it.optLong("vram_needed"), it.optLong("ram_needed"), it.optString("verdict"), it.optBoolean("recommended"),
                )
            },
        )
    }

    /**
     * Starts a download on the computer. The service owns the transfer: this returns once it has
     * started, and [downloads] follows it from then on.
     */
    suspend fun hubDownload(id: String, path: String, bytes: Long) = withContext(Dispatchers.IO) {
        val c = long.newCall(request("/api/v2/hub/download").post(JSONObject().put("id", id).put("path", path).put("bytes", bytes).toString().toRequestBody(JSON)).build())
        try {
            c.execute().use { resp ->
                if (!resp.isSuccessful) read(resp)
                val source = resp.body?.source() ?: return@use
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data:")) continue
                    val ev = runCatching { JSONObject(line.removePrefix("data:").trim()) }.getOrNull() ?: continue
                    if (ev.has("error")) throw ImpulseError(ev.optString("error"))
                    break
                }
            }
        } catch (e: IOException) {
            throw ImpulseError("Cannot reach Impulse at ${base.substringAfter("://")}.")
        } finally {
            c.cancel()
        }
    }

    suspend fun downloads(): List<Download> = downloadList(call("/api/v2/hub/downloads"))

    /** [action]: pause, resume, retry or cancel; [keepPart] keeps a cancelled one's partial file. */
    suspend fun controlDownload(name: String, action: String, keepPart: Boolean = false): List<Download> =
        downloadList(call("/api/v2/hub/downloads/control", JSONObject().put("name", name).put("action", action).put("keep_part", keepPart)))

    private fun downloadList(o: JSONObject) = o.optJSONArray("downloads").objects().map {
        Download(
            it.optString("name"), it.optString("repo"), it.optLong("have"), it.optLong("total"), it.optLong("bytes_per_second"),
            it.optLong("eta_seconds"), it.optString("state"), it.optString("error"), it.optDouble("percent", 0.0),
        )
    }

    /** The computer's GPUs, memory and processor, as the panel's Hardware type describes them. */
    suspend fun hardware(): JSONObject = call("/api/v2/hardware")

    /** The profile's panel settings: each value is the JSON text the panel stored. */
    suspend fun prefs(): JSONObject = call("/api/v2/prefs").optJSONObject("prefs") ?: JSONObject()

    /** Merges settings into the profile's: each value is JSON text, as the panel keeps it. */
    suspend fun setPrefs(set: JSONObject) {
        call("/api/v2/prefs", JSONObject().put("set", set))
    }

    /** Every memory, the chats' own (`chat-…`) included. */
    suspend fun contexts(): List<Context> = call("/api/v2/contexts").optJSONArray("contexts").objects().map {
        Context(
            it.getString("name"), it.optLong("tokens", -1).takeIf { n -> n >= 0 && !it.isNull("tokens") }, it.optInt("documents"),
            it.optInt("pages"), it.optLong("capacity", -1).takeIf { n -> n >= 0 },
        )
    }

    /** Makes an empty memory of [maxTokens], or builds one from a [source] path on the computer. */
    suspend fun createContext(name: String, maxTokens: Long? = null, source: String? = null): Boolean =
        call("/api/v2/contexts/create", JSONObject().put("name", name).apply {
            maxTokens?.let { put("max_tokens", it) }
            source?.let { put("source", it) }
        }).optBoolean("started")

    /** Writes text into a memory: [actor]'s own (e.g. "chat:<id>") or the named [context]; made when new. */
    suspend fun writeContext(text: String, label: String, actor: String? = null, context: String? = null): Written {
        val r = call("/api/v2/contexts/write", JSONObject().put("text", text).put("label", label).apply {
            actor?.let { put("actor", it) }
            context?.let { put("context", it) }
        }, long)
        return Written(r.optString("context"), r.optLong("added"), r.optLong("total"), r.optLong("tokens", -1).takeIf { it >= 0 && !r.isNull("tokens") })
    }

    /** Gives a memory more room; returns the capacity the service set (capped at what it offers). */
    suspend fun growContext(name: String, capacity: Long): Long =
        call("/api/v2/contexts/grow", JSONObject().put("name", name).put("capacity", capacity)).optLong("capacity")

    suspend fun removeContext(name: String) {
        call("/api/v2/contexts/remove", JSONObject().put("name", name))
    }

    /** What a memory holds, newest first, and how many documents it has. */
    suspend fun contextEntries(name: String, offset: Int = 0, limit: Int = 50): Pair<List<Entry>, Int> {
        val r = call("/api/v2/contexts/entries", JSONObject().put("name", name).put("offset", offset).put("limit", limit))
        return r.optJSONArray("entries").objects().map {
            Entry(
                it.optLong("at"), it.optString("actor"), it.optString("source"), it.optLong("bytes"), it.optLong("tokens", -1).takeIf { n -> n >= 0 && !it.isNull("tokens") },
                it.optLong("position"), it.optString("preview"), it.optBoolean("truncated"),
            )
        } to r.optInt("documents")
    }

    /** Finds a phrase in a memory, with text around each place it occurs. */
    suspend fun contextFind(name: String, query: String, limit: Int = 30): List<Hit> =
        call("/api/v2/contexts/find", JSONObject().put("name", name).put("query", query).put("limit", limit), long).optJSONArray("hits").objects().map {
            Hit(it.optLong("position"), it.optString("text"), it.optString("actor"), it.optLong("at"), it.optString("source"))
        }

    /** The memory being built now, or null. */
    suspend fun building(): Building? = call("/api/v2/contexts/building").takeIf { it.has("name") }?.let {
        Building(it.optString("name"), it.optString("stage"), it.optLong("done"), it.optLong("total"), it.optString("phase"), it.optLong("eta_seconds", -1).takeIf { n -> n >= 0 && !it.isNull("eta_seconds") })
    }

    suspend fun keepAwake(): KeepAwake = awake(call("/api/v2/keep-awake"))

    /** [mode]: off, working or always. */
    suspend fun setKeepAwake(mode: String): KeepAwake = awake(call("/api/v2/keep-awake", JSONObject().put("mode", mode)))

    private fun awake(o: JSONObject) = KeepAwake(o.optString("mode", "off"), o.optBoolean("holding"), o.optString("reason"))

    /** Phones and browsers signed in from outside the computer. */
    suspend fun devices(): List<Device> = deviceList(call("/api/v2/phone"))

    /** Signs out one device by [id], or every one with null. Returns the devices left. */
    suspend fun signOutDevice(id: String?): List<Device> = deviceList(call("/api/v2/phone/devices/signout", JSONObject().apply { id?.let { put("id", it) } }))

    private fun deviceList(o: JSONObject) = o.optJSONArray("devices").objects().map {
        Device(
            it.optString("id"), it.optString("name"), it.optBoolean("app"), it.optString("via"), it.optString("ip"),
            it.optString("profile_name").takeIf { n -> n.isNotBlank() && n != "null" }, it.optLong("seen_ago"), it.optLong("signed_in_ago"),
        )
    }

    /** Automatic backups on the computer: enabled, every (day/week), folder, keep, last_unix, last_file, last_error, next_unix, running. */
    suspend fun backup(): JSONObject = call("/api/v2/backup/auto")

    /** Changes the automatic backup: any of enabled, every, folder, keep. */
    suspend fun setBackup(patch: JSONObject): JSONObject = call("/api/v2/backup/auto", patch)

    /** Makes a backup on the computer now, into its backup folder. */
    suspend fun backupNow(): Boolean = call("/api/v2/backup/auto/run", JSONObject()).optBoolean("started")

    /** This build against the published one: current, update_available, latest {version, notes…}. */
    suspend fun version(): JSONObject = call("/api/v2/version", client = long)

    /** Restarts Impulse; it answers, then exits, so the next calls fail for a while. */
    suspend fun restart() {
        call("/api/v2/restart", JSONObject())
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
     * chat keeps its own memory; [store] answers from a named context; [options] carries the
     * sampler (temperature, top_p, top_k, min_p, repeat_penalty, seed, stop, max_tokens); with
     * [remember] false nothing of the turn is kept. Cancelling the collector cancels the request.
     */
    fun turn(
        messages: List<JSONObject>,
        actor: String?,
        store: String?,
        effort: String?,
        images: List<String> = emptyList(),
        tools: JSONArray? = null,
        options: JSONObject? = null,
        remember: Boolean = true,
    ): Flow<Delta> = callbackFlow {
        val body = JSONObject().put("messages", JSONArray(messages))
        options?.keys()?.forEach { body.put(it, options.get(it)) }
        if (actor != null) body.put("actor", actor)
        if (store != null) body.put("store", store)
        if (images.isNotEmpty()) body.put("images", JSONArray(images))
        if (tools != null && tools.length() > 0) body.put("tools", tools)
        if (!remember) body.put("remember", false)
        when (effort) {
            null -> {}
            "off" -> body.put("thinking", false).put("reasoning_effort", "none")
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

fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else (0 until length()).mapNotNull { opt(it) as? String }

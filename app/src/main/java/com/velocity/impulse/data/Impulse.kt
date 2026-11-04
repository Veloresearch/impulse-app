package com.velocity.impulse.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ImpulseError(message: String, val code: String? = null) : Exception(message)

data class Profile(val id: String, val name: String, val hasPassword: Boolean, val remote: Boolean)
data class Model(val id: String, val window: Int?)
data class Context(val name: String, val tokens: Long?, val documents: Int)

sealed interface Delta {
    data class Thinking(val text: String) : Delta
    data class Text(val text: String) : Delta
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

/** The parts of Impulse's HTTP API this app speaks: profiles, models, contexts and the chat. */
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

    private suspend fun call(path: String, body: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
        val r = request(path).apply { if (body != null) post(body.toString().toRequestBody(JSON)) }.build()
        try {
            quick.newCall(r).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                val json = runCatching { JSONObject(text) }.getOrNull()
                if (!resp.isSuccessful) {
                    val e = json?.optJSONObject("error")
                    throw ImpulseError(e?.optString("message")?.takeIf { it.isNotBlank() } ?: "Impulse answered ${resp.code}.", e?.optString("code"))
                }
                json ?: JSONObject()
            }
        } catch (e: IOException) {
            throw ImpulseError("Cannot reach Impulse at ${base.substringAfter("://")}. Check that it is running and that this phone can see it.")
        }
    }

    suspend fun profiles(): List<Profile> = call("/api/v2/profiles").optJSONArray("profiles").objects().map {
        Profile(it.getString("id"), it.optString("name"), it.optBoolean("has_password"), it.optBoolean("remote"))
    }

    /** Signs in and returns the session token that every later call carries. */
    suspend fun login(id: String, password: String): String =
        call("/api/v2/profiles/login", JSONObject().put("id", id).put("password", password)).optString("session")
            .ifBlank { throw ImpulseError("Impulse did not start a session.") }

    suspend fun logout() {
        runCatching { call("/api/v2/profiles/logout", JSONObject()) }
    }

    suspend fun models(): List<Model> = call("/v1/models").optJSONArray("data").objects().map {
        Model(it.getString("id"), it.optInt("context_length").takeIf { n -> n > 0 })
    }

    suspend fun contexts(): List<Context> = call("/v1/velocity/contexts").optJSONArray("data").objects().map {
        Context(it.getString("name"), it.optLong("addressable_tokens").takeIf { n -> n > 0 }, it.optInt("documents"))
    }

    /** Streams one answer. Cancelling the collector cancels the request. */
    fun chat(model: String, messages: List<Message>, context: String?): Flow<Delta> = callbackFlow {
        val body = JSONObject()
            .put("model", model)
            .put("stream", true)
            .put("messages", JSONArray(messages.map { JSONObject().put("role", it.role).put("content", it.content) }))
        if (context != null) body.put("velocity_context", context)
        val c: Call = stream.newCall(request("/v1/chat/completions").post(body.toString().toRequestBody(JSON)).build())
        try {
            c.execute().use { resp ->
                if (!resp.isSuccessful) {
                    val e = runCatching { JSONObject(resp.body!!.string()).getJSONObject("error").getString("message") }.getOrNull()
                    throw ImpulseError(e ?: "Impulse answered ${resp.code}.")
                }
                val source = resp.body!!.source()
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data == "[DONE]") break
                    val delta = runCatching { JSONObject(data).getJSONArray("choices").getJSONObject(0).getJSONObject("delta") }.getOrNull() ?: continue
                    delta.optString("reasoning_content").takeIf { it.isNotEmpty() && it != "null" }?.let { trySend(Delta.Thinking(it)) }
                    delta.optString("content").takeIf { it.isNotEmpty() && it != "null" }?.let { trySend(Delta.Text(it)) }
                }
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
        private val stream = quick.newBuilder().readTimeout(0, TimeUnit.SECONDS).build()

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

private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

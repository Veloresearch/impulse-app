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

    companion object {
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
    val messages: List<Message> get() = raw.optJSONArray("messages").objects().map(::Message)

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

    fun save(chats: List<Chat>) {
        val o = JSONObject()
            .put("head", head)
            .put("revs", JSONObject(revs as Map<*, *>))
            .put("dirty", JSONArray(dirty.toList()))
            .put("gone", JSONArray(gone.toList()))
            .put("chats", JSONArray(chats.map { it.raw }))
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(o.toString())
        tmp.renameTo(file)
    }

    fun forget() {
        head = 0
        revs.clear()
        dirty.clear()
        gone.clear()
        file.delete()
    }
}

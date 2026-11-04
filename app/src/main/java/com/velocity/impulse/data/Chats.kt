package com.velocity.impulse.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Message(val role: String, val content: String, val thinking: String = "")

data class Chat(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val updated: Long = System.currentTimeMillis(),
    val messages: List<Message> = emptyList(),
)

/** Chats live on the phone, in one JSON file, written whole after every finished answer. */
class ChatStore(private val file: File) {

    fun load(): List<Chat> = runCatching {
        val a = JSONArray(file.readText())
        (0 until a.length()).map { i ->
            val c = a.getJSONObject(i)
            val m = c.getJSONArray("messages")
            Chat(
                c.getString("id"),
                c.optString("title"),
                c.optLong("updated"),
                (0 until m.length()).map { j ->
                    val o = m.getJSONObject(j)
                    Message(o.getString("role"), o.optString("content"), o.optString("thinking"))
                },
            )
        }
    }.getOrDefault(emptyList())

    fun save(chats: List<Chat>) {
        val a = JSONArray(chats.filter { it.messages.isNotEmpty() }.map { c ->
            JSONObject()
                .put("id", c.id)
                .put("title", c.title)
                .put("updated", c.updated)
                .put("messages", JSONArray(c.messages.map { JSONObject().put("role", it.role).put("content", it.content).put("thinking", it.thinking) }))
        })
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(a.toString())
        tmp.renameTo(file)
    }
}

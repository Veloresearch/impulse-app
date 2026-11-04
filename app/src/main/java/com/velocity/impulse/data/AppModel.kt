package com.velocity.impulse.data

import android.app.Application
import android.content.Context.MODE_PRIVATE
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.velocity.impulse.ui.Lang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class Stage { Connect, SignIn, Ready }

/** The answer being written right now, kept apart from the chat so each token is not a whole copy. */
data class Live(
    val chatId: String,
    val messageId: String,
    val text: String = "",
    val thought: String = "",
    val queued: Int = 0,
    val thoughtAt: Long = 0L,
    val textAt: Long = 0L,
    /** What a tool is doing now, e.g. "Searching: best pizza in Krakow". */
    val step: String? = null,
    /** Pages found or read so far, as (title, url). */
    val links: List<Pair<String, String>> = emptyList(),
)

class AppModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("impulse", MODE_PRIVATE)
    private val cache = ChatCache(File(app.filesDir, "chats.json"))
    private val syncing = Mutex()

    var server by mutableStateOf(loadServer())
        private set
    private var token by mutableStateOf(prefs.getString("token", null))
    var me by mutableStateOf(loadMe())
        private set
    var stage by mutableStateOf(
        when {
            server == null -> Stage.Connect
            token == null -> Stage.SignIn
            else -> Stage.Ready
        },
    )
        private set

    var accent by mutableStateOf(prefs.getString("accent_name", "velocity") ?: "velocity")
        private set
    var effort by mutableStateOf(prefs.getString("effort", null))
        private set
    var showThinking by mutableStateOf(prefs.getBoolean("thinking", true))
        private set
    var reduceMotion by mutableStateOf(prefs.getBoolean("reduce_motion", false))
        private set
    var textScale by mutableStateOf(prefs.getFloat("text_scale", 1f).coerceIn(0.85f, 1.3f))
        private set
    var haptics by mutableStateOf(prefs.getBoolean("haptics", true))
        private set
    /** The screen stays on while an answer streams. */
    var keepAwake by mutableStateOf(prefs.getBoolean("keep_awake", true))
        private set
    var enterSends by mutableStateOf(prefs.getBoolean("enter_sends", false))
        private set
    var webDefault by mutableStateOf(prefs.getBoolean("web_default", true))
        private set
    var pluginsDefault by mutableStateOf(prefs.getBoolean("plugins_default", true))
        private set

    /** Plugin tools the computer offers, by name. */
    var plugins by mutableStateOf<List<String>>(emptyList())
        private set

    var profiles by mutableStateOf<List<Profile>>(emptyList())
        private set
    var status by mutableStateOf<Status?>(null)
        private set
    var library by mutableStateOf<List<Probe>>(emptyList())
        private set
    var loading by mutableStateOf<String?>(null)
        private set
    var contexts by mutableStateOf<List<Context>>(emptyList())
        private set
    var defaultContext by mutableStateOf(prefs.getString("context", null))
        private set
    var online by mutableStateOf<Boolean?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    var chats by mutableStateOf(cache.load().sortedByDescending { it.updatedAt })
        private set
    var current by mutableStateOf<String?>(null)
        private set
    var live by mutableStateOf<Live?>(null)
        private set
    private var answer: Job? = null
    private var pulse: Job? = null

    val chat: Chat? get() = chats.firstOrNull { it.id == current }
    val link: Link? get() = server?.let { Link.of(it.base) }
    val streaming get() = live != null
    private fun api() = Impulse(server!!, token)

    init {
        if (stage == Stage.SignIn) loadProfiles()
    }

    private fun loadServer(): Server? = prefs.getString("server", null)?.let {
        Server(it, prefs.getString("access_id", null), prefs.getString("access_secret", null))
    }

    private fun loadMe(): Profile? = prefs.getString("me", null)?.let {
        runCatching {
            val o = JSONObject(it)
            Profile(o.getString("id"), o.optString("name"), o.optString("avatar").ifBlank { null }, o.optString("photo").ifBlank { null }, true, true)
        }.getOrNull()
    }

    private fun work(block: suspend () -> Unit) = viewModelScope.launch {
        busy = true
        error = null
        try {
            block()
        } catch (e: ImpulseError) {
            error = e.message
        } finally {
            busy = false
        }
    }

    // ── connecting and signing in ───────────────────────────────────────────────────────────

    fun connect(raw: String) = work {
        val s = try {
            Server.parse(raw)
        } catch (e: Exception) {
            throw e as? ImpulseError ?: ImpulseError(Lang.tr("That does not look like an address.", "To nie wygląda na adres."))
        }
        val found = Impulse(s).profiles()
        server = s
        prefs.edit()
            .putString("server", s.base)
            .putString("access_id", s.accessId)
            .putString("access_secret", s.accessSecret)
            .remove("token")
            .apply()
        token = null
        profiles = found
        stage = Stage.SignIn
    }

    fun loadProfiles() = work { profiles = Impulse(server!!).profiles() }

    fun signIn(profile: Profile, password: String) = work {
        val t = Impulse(server!!).login(profile.id, password)
        if (me?.id != profile.id) {
            cache.forget()
            chats = emptyList()
        }
        token = t
        prefs.edit().putString("token", t).apply()
        remember(profile)
        stage = Stage.Ready
        resume()
    }

    fun signOut() {
        val old = api()
        viewModelScope.launch { old.logout() }
        pause()
        token = null
        prefs.edit().remove("token").apply()
        stage = Stage.SignIn
        loadProfiles()
    }

    fun forgetServer() {
        signOut()
        server = null
        me = null
        cache.forget()
        chats = emptyList()
        prefs.edit().remove("server").remove("access_id").remove("access_secret").remove("me").apply()
        stage = Stage.Connect
    }

    // ── staying in step with the computer ───────────────────────────────────────────────────

    /** While the app is in front: chats every few seconds, the model and the settings with them. */
    fun resume() {
        if (stage != Stage.Ready || pulse?.isActive == true) return
        pulse = viewModelScope.launch {
            launch { refreshAll() }
            while (isActive) {
                sync()
                delay(4000)
                refreshStatus()
            }
        }
    }

    fun pause() {
        pulse?.cancel()
        pulse = null
    }

    fun refreshAll() = viewModelScope.launch {
        if (stage != Stage.Ready) return@launch
        refreshStatus()
        runCatching { library = api().library() }
        // Chat memories (chat-…) and the title helper are the service's own; the panel hides them too.
        runCatching { contexts = api().contexts().filterNot { it.name.startsWith("chat-") || it.name == "vls-titles" } }
        runCatching { readPrefs(api().prefs()) }
        runCatching { api().me()?.let { remember(it) } }
        runCatching { plugins = api().mcpTools(false).map { it.optString("name") }.filter { it.isNotBlank() } }
    }

    private fun remember(p: Profile) {
        me = p
        prefs.edit().putString("me", JSONObject().put("id", p.id).put("name", p.name).put("avatar", p.avatar ?: "").put("photo", p.photo ?: "").toString()).apply()
    }

    private suspend fun refreshStatus() {
        try {
            status = api().status()
            online = true
        } catch (e: ImpulseError) {
            online = false
            if (e.status == 401) signOut()
        }
    }

    private fun readPrefs(p: JSONObject) {
        fun value(key: String): String? = p.optString(key).ifBlank { null }?.let { runCatching { JSONObject("{\"v\":$it}").opt("v") as? String }.getOrNull() }
        value("vls.accent")?.let {
            accent = it
            prefs.edit().putString("accent_name", it).apply()
        }
        value("vls.chat.effort")?.let {
            effort = it
            prefs.edit().putString("effort", it).apply()
        }
    }

    private suspend fun sync() = syncing.withLock {
        if (stage != Stage.Ready) return@withLock
        try {
            val a = api()
            val out = JSONArray()
            val holding = live?.chatId
            for (id in cache.dirty.toList()) {
                if (id == holding) continue
                val c = chats.firstOrNull { it.id == id } ?: continue
                out.put(JSONObject().put("id", id).put("base", cache.revs[id] ?: 0).put("session", c.raw))
            }
            for (id in cache.gone) out.put(JSONObject().put("id", id).put("deleted", true).put("base", cache.revs[id] ?: 0))
            if (out.length() > 0) {
                val r = a.push(out)
                var next = chats
                for (rec in r.optJSONArray("chats").objects()) {
                    val id = rec.optString("id")
                    cache.revs[id] = rec.optLong("rev")
                    cache.dirty -= id
                    cache.gone -= id
                    rec.optJSONObject("session")?.let { s -> next = next.map { if (it.id == id) Chat(s) else it } }
                }
                chats = next
            }
            val p = a.pull(cache.head)
            var next = chats
            for (rec in p.optJSONArray("chats").objects()) {
                val id = rec.optString("id")
                cache.revs[id] = rec.optLong("rev")
                if (rec.optBoolean("deleted")) {
                    next = next.filterNot { it.id == id }
                    if (current == id && live == null) current = null
                } else if (id !in cache.dirty && id != live?.chatId) {
                    val s = rec.optJSONObject("session") ?: continue
                    next = next.filterNot { it.id == id } + Chat(s)
                }
            }
            cache.head = p.optLong("head", cache.head)
            chats = next.sortedByDescending { it.updatedAt }
            online = true
            persist()
        } catch (e: ImpulseError) {
            if (e.status == 401) signOut()
        }
    }

    private fun persist() {
        val snapshot = chats
        viewModelScope.launch(Dispatchers.IO) { synchronized(cache) { cache.save(snapshot) } }
    }

    private fun changed(c: Chat) {
        chats = (chats.filterNot { it.id == c.id } + c).sortedByDescending { it.updatedAt }
        cache.dirty += c.id
        persist()
    }

    private fun pushSoon() = viewModelScope.launch { sync() }

    // ── models and contexts ─────────────────────────────────────────────────────────────────

    fun load(p: Probe) {
        if (loading != null) return
        loading = p.path
        viewModelScope.launch {
            try {
                api().load(p)
            } catch (e: ImpulseError) {
                error = Lang.tr("Could not load ${p.name}: ${e.message}", "Nie udało się wczytać ${p.name}: ${e.message}")
            }
            refreshStatus()
            loading = null
        }
    }

    /** Publisher pictures by Hugging Face owner; an owner is asked for once per run. */
    val publishers = mutableStateMapOf<String, String?>()

    fun publisher(org: String) {
        if (org in publishers || stage != Stage.Ready) return
        publishers[org] = null
        viewModelScope.launch { runCatching { api().hubAvatar(org) }.getOrNull()?.let { publishers[org] = it } }
    }

    fun unload() = viewModelScope.launch {
        runCatching { api().unload() }
        refreshStatus()
    }

    fun useContext(name: String?) {
        defaultContext = name
        prefs.edit().putString("context", name).apply()
        chat?.let { changed(it.edit { put("contextName", name ?: JSONObject.NULL) }) }
        pushSoon()
    }

    fun setThinking(on: Boolean) {
        showThinking = on
        prefs.edit().putBoolean("thinking", on).apply()
    }

    fun calmMotion(on: Boolean) {
        reduceMotion = on
        prefs.edit().putBoolean("reduce_motion", on).apply()
    }

    fun pickAccent(name: String) {
        accent = name
        prefs.edit().putString("accent_name", name).apply()
    }

    fun pickEffort(e: String) {
        effort = e
        prefs.edit().putString("effort", e).apply()
    }

    fun pickTextScale(v: Float) {
        textScale = v.coerceIn(0.85f, 1.3f)
        prefs.edit().putFloat("text_scale", textScale).apply()
    }

    fun pickHaptics(on: Boolean) {
        haptics = on
        prefs.edit().putBoolean("haptics", on).apply()
    }

    fun pickKeepAwake(on: Boolean) {
        keepAwake = on
        prefs.edit().putBoolean("keep_awake", on).apply()
    }

    fun pickEnterSends(on: Boolean) {
        enterSends = on
        prefs.edit().putBoolean("enter_sends", on).apply()
    }

    fun pickWebDefault(on: Boolean) {
        webDefault = on
        prefs.edit().putBoolean("web_default", on).apply()
    }

    fun pickPluginsDefault(on: Boolean) {
        pluginsDefault = on
        prefs.edit().putBoolean("plugins_default", on).apply()
    }

    /** Drops the phone's copy of the chats and pulls them all again; the sign-in stays. */
    fun clearCache() {
        stop()
        current = null
        viewModelScope.launch {
            syncing.withLock {
                synchronized(cache) { cache.forget() }
                chats = emptyList()
            }
            sync()
        }
    }

    // ── chats ───────────────────────────────────────────────────────────────────────────────

    fun newChat() {
        stop()
        current = null
    }

    fun open(id: String) {
        if (live?.chatId != id) stop()
        current = id
    }

    fun delete(id: String) {
        if (current == id) newChat()
        chats = chats.filterNot { it.id == id }
        cache.dirty -= id
        cache.gone += id
        persist()
        pushSoon()
    }

    fun setPersona(id: String?) {
        chat?.let { changed(it.edit { put("persona", id ?: JSONObject.NULL) }) }
        pushSoon()
    }

    fun pin(id: String) {
        chats.firstOrNull { it.id == id }?.let { c -> changed(c.edit { put("pinned", !c.pinned) }) }
        pushSoon()
    }

    /** Edits a chat's session record, keeps it and sends it to the computer. */
    fun editChat(id: String, block: JSONObject.() -> Unit) {
        chats.firstOrNull { it.id == id }?.let { changed(it.edit(block)) }
        pushSoon()
    }

    fun rename(id: String, title: String) {
        val t = title.trim().ifEmpty { return }
        editChat(id) { put("title", t).put("titled", true) }
    }

    fun duplicate(id: String): String? {
        val c = chats.firstOrNull { it.id == id } ?: return null
        val now = System.currentTimeMillis()
        val copy = Chat(JSONObject(c.raw.toString()).put("id", newId("cs")).put("createdAt", now).put("updatedAt", now).put("pinned", false))
        changed(copy)
        pushSoon()
        return copy.id
    }

    private fun editCurrent(block: JSONObject.() -> Unit) {
        current?.let { editChat(it, block) }
    }

    private fun JSONObject.setIn(group: String, key: String, value: Any?) {
        val o = optJSONObject(group) ?: JSONObject()
        if (value == null) o.remove(key) else o.put(key, value)
        put(group, o)
    }

    fun setSystemPrompt(text: String?) = editCurrent {
        val t = text?.trim()
        if (t.isNullOrEmpty()) remove("systemPrompt") else put("systemPrompt", t)
    }

    fun setTemperature(v: Double?) = editCurrent { setIn("options", "temperature", v) }

    fun setMaxTokens(n: Int?) = editCurrent { setIn("options", "max_tokens", n) }

    fun setChatWeb(on: Boolean?) = editCurrent { setIn("appTools", "web", on) }

    fun setChatPlugins(on: Boolean?) = editCurrent { setIn("appTools", "plugins", on) }

    /**
     * Sends a question with what was attached to it. Pictures go to the model to look at; a text
     * file goes into the question itself. The message keeps the panel's `files` record of each.
     */
    fun send(text: String, attached: List<Attachment> = emptyList()) {
        val prompt = text.trim()
        if ((prompt.isEmpty() && attached.isEmpty()) || streaming) return
        val title = prompt.ifEmpty { attached.first().name }
        val base = chat ?: Chat.start(title, status?.model?.name, defaultContext).also { current = it.id }
        val question = Message.of("user", prompt)
        if (attached.isNotEmpty()) question.raw.put("files", org.json.JSONArray(attached.map { it.record() }))
        val asked = base.messages.filter { it.error == null && it.text.isNotBlank() } + question
        val reply = Message.of("assistant", "")
        reply.raw.put("streaming", true)
        changed(base.withMessages(asked + reply))
        val docs = attached.filter { it.text != null }.joinToString("") { "\n\n${it.name}:\n```\n${it.text}\n```" }
        stream(base.id, reply.id, asked, base.contextName, prompt + docs, attached.mapNotNull { it.image }, base.persona)
    }

    /** Asks again for the last answer. */
    fun retry() {
        val c = chat ?: return
        if (streaming) return
        val msgs = c.messages
        val cut = msgs.indexOfLast { it.role == "user" }
        if (cut < 0) return
        val asked = msgs.take(cut + 1)
        val reply = Message.of("assistant", "").also { it.raw.put("streaming", true) }
        changed(c.withMessages(asked + reply))
        stream(c.id, reply.id, asked, c.contextName, persona = c.persona)
    }

    private fun stream(
        chatId: String,
        replyId: String,
        asked: List<Message>,
        store: String?,
        lastText: String? = null,
        images: List<String> = emptyList(),
        persona: String? = null,
    ) {
        live = Live(chatId, replyId)
        answer = viewModelScope.launch {
            var receipt: JSONObject? = null
            var failed: String? = null
            var tools: Tools? = null
            try {
                val c = chats.firstOrNull { it.id == chatId }
                val a = api()
                val offered = if (c?.plugins ?: pluginsDefault) runCatching { a.mcpTools(true) }.getOrDefault(emptyList()) else emptyList()
                val box = Tools(a, c?.web ?: webDefault, offered).also { tools = it }
                fun line(role: String, content: String) = JSONObject().put("role", role).put("content", content)
                val talk = mutableListOf<JSONObject>()
                c?.systemPrompt?.let { talk += line("system", it) }
                Personas.firstOrNull { it.id == persona }?.let { talk += line("system", it.prompt) }
                asked.forEachIndexed { i, m -> talk += line(m.role, if (i == asked.lastIndex && lastText != null) lastText else m.text) }
                receipt = answerWith(a, box, talk, "chat:$chatId", store, images, c?.temperature, c?.maxTokens)
            } catch (e: ImpulseError) {
                failed = e.message
            } finally {
                val l = live
                live = null
                withContext(Dispatchers.Main) { settle(chatId, replyId, l, receipt, failed, tools) }
            }
        }
    }

    /**
     * The tool loop, as the panel runs it (data/tools/loop.ts): the model is offered the tools; each
     * call it makes is run and its result handed back, for up to [TOOL_STEPS] steps; then it answers
     * in words. Returns the last step's receipt, its time and tokens summed over every step.
     */
    private suspend fun answerWith(
        a: Impulse,
        tools: Tools,
        talk: MutableList<JSONObject>,
        actor: String,
        store: String?,
        images: List<String>,
        temperature: Double?,
        maxTokens: Int?,
    ): JSONObject? {
        var totalMs = 0L
        var generated = 0L
        var last: JSONObject? = null
        var toolRole = true
        var repaired = false
        var closed = false
        var step = 0
        while (true) {
            val wordsOnly = !tools.any || step >= TOOL_STEPS
            if (wordsOnly && step > 0 && !closed) {
                closed = true
                talk += JSONObject().put("role", "user").put("content", "(From the app, not the user.) $CLOSE")
            }
            var said = ""
            var callText = ""
            var calls: JSONArray? = null
            var unparsed: String? = null
            var r: JSONObject? = null
            val sent = if (toolRole) talk else talk.map { if (it.optString("role") == "tool") JSONObject().put("role", "user").put("content", "Tool result:\n${it.optString("content")}") else it }
            try {
                a.turn(sent, actor, store, effort, images, if (wordsOnly) null else tools.defs, temperature, maxTokens).collect { d ->
                    val l = live ?: return@collect
                    val now = System.currentTimeMillis()
                    live = when (d) {
                        is Delta.Text -> {
                            said += d.text
                            l.copy(text = said, step = null, queued = 0, textAt = l.textAt.takeIf { it > 0 } ?: now)
                        }
                        is Delta.Thinking -> l.copy(thought = l.thought + d.text, queued = 0, thoughtAt = l.thoughtAt.takeIf { it > 0 } ?: now)
                        is Delta.Queued -> l.copy(queued = d.ahead)
                        is Delta.ToolText -> l.also { callText += d.text }
                        is Delta.ToolCalls -> l.also { calls = d.calls }
                        is Delta.ToolUnparsed -> l.also { unparsed = d.raw }
                        is Delta.Done -> l.also { r = d.receipt }
                    }
                }
            } catch (e: ImpulseError) {
                // A chat template with no place for tool results: they go in as the user's words, once.
                if (toolRole && e.message.orEmpty().contains("template", true) && talk.any { it.optString("role") == "tool" }) {
                    toolRole = false
                    continue
                }
                throw e
            }
            r?.let {
                last?.let { p ->
                    totalMs += p.optLong("total_ms")
                    generated += p.optLong("generated_tokens")
                }
                last = it
            }
            val made = calls?.objects()?.filter { it.optJSONObject("function")?.optString("name").orEmpty().isNotBlank() }.orEmpty()
            if (!wordsOnly && made.isNotEmpty()) {
                live = live?.copy(text = "")
                talk += JSONObject().put("role", "assistant").put("content", said).put("tool_calls", JSONArray(made))
                for (call in made) {
                    val result = tools.call(call.getJSONObject("function").getString("name"), Tools.args(call)) { what ->
                        live = live?.copy(step = what, links = tools.links.toList())
                    }
                    live = live?.copy(links = tools.links.toList())
                    talk += JSONObject().put("role", "tool").put("content", result)
                }
                live = live?.copy(step = null)
                step++
                continue
            }
            // Two unreadable calls, or nothing said after the tools ran: the answer is due, without tools.
            if (!wordsOnly && (unparsed != null && repaired || said.isBlank() && tools.steps.isNotEmpty())) {
                step = TOOL_STEPS
                continue
            }
            if (!wordsOnly && unparsed != null) {
                repaired = true
                talk += JSONObject().put("role", "assistant").put("content", (said + (unparsed ?: callText)).take(2400))
                talk += JSONObject().put("role", "user").put(
                    "content",
                    "(From the app, not the user.) Your tool call could not be used: it is not valid JSON or is missing its name. " +
                        "Write the call again correctly - the exact tool name and every required argument - or answer without tools.",
                )
                live = live?.copy(text = "")
                step++
                continue
            }
            if (tools.steps.isNotEmpty()) {
                val clean = said.substringBefore("<tool_call>").substringBefore("<function=").trimEnd()
                if (clean != said) live = live?.copy(text = clean)
            }
            return last?.let { if (step > 0) JSONObject(it.toString()).put("total_ms", totalMs + it.optLong("total_ms")).put("generated_tokens", generated + it.optLong("generated_tokens")) else it }
        }
    }

    private fun settle(chatId: String, replyId: String, l: Live?, receipt: JSONObject?, failed: String?, tools: Tools? = null) {
        val c = chats.firstOrNull { it.id == chatId } ?: return
        val msgs = c.messages.map { m ->
            if (m.id != replyId) m else Message(JSONObject(m.raw.toString()).apply {
                put("text", l?.text.orEmpty())
                if (!l?.thought.isNullOrBlank()) put("thought", l!!.thought)
                if (l != null && l.thoughtAt > 0) put("thoughtMs", (if (l.textAt > 0) l.textAt else System.currentTimeMillis()) - l.thoughtAt)
                remove("streaming")
                receipt?.let { put("receipt", it) }
                tools?.seen?.takeIf { it.isNotEmpty() }?.let { s -> put("sources", JSONArray(s.map { (t, u) -> JSONObject().put("title", t).put("url", u) })) }
                tools?.steps?.takeIf { it.isNotEmpty() }?.let { s -> put("appSteps", JSONArray(s.map { it.json() })) }
                if (failed != null) put("error", failed)
            })
        }
        changed(c.withMessages(msgs))
        if (failed != null) error = failed
        pushSoon()
    }

    fun stop() {
        answer?.cancel()
        answer = null
    }

    private companion object {
        const val TOOL_STEPS = 6
        const val CLOSE =
            "No more tools: answer the user's latest message now, in their language, from what you found above. " +
                "Say in one sentence what is missing or could not be done. Never mention tools, steps or this note."
    }
}

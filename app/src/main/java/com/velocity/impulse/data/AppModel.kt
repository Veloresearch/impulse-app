package com.velocity.impulse.data

import android.app.Application
import android.content.Context.MODE_PRIVATE
import androidx.compose.runtime.getValue
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
data class Live(val chatId: String, val messageId: String, val text: String = "", val thought: String = "", val queued: Int = 0)

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
        Lang.pl = prefs.getString("lang", null)?.let { it == "pl" } ?: (java.util.Locale.getDefault().language == "pl")
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
        runCatching { contexts = api().contexts() }
        runCatching { readPrefs(api().prefs()) }
        runCatching { api().me()?.let { remember(it) } }
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
        value("vls.lang")?.let {
            Lang.pl = it == "pl"
            prefs.edit().putString("lang", it).apply()
        }
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

    fun pin(id: String) {
        chats.firstOrNull { it.id == id }?.let { c -> changed(c.edit { put("pinned", !c.pinned) }) }
        pushSoon()
    }

    fun send(text: String) {
        val prompt = text.trim()
        if (prompt.isEmpty() || streaming) return
        val base = chat ?: Chat.start(prompt, status?.model?.name, defaultContext).also { current = it.id }
        val asked = base.messages.filter { it.error == null && it.text.isNotBlank() } + Message.of("user", prompt)
        val reply = Message.of("assistant", "")
        reply.raw.put("streaming", true)
        changed(base.withMessages(asked + reply))
        stream(base.id, reply.id, asked, base.contextName)
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
        stream(c.id, reply.id, asked, c.contextName)
    }

    private fun stream(chatId: String, replyId: String, asked: List<Message>, store: String?) {
        live = Live(chatId, replyId)
        answer = viewModelScope.launch {
            var receipt: JSONObject? = null
            var failed: String? = null
            try {
                api().turn(asked.map { it.role to it.text }, chatId, store, effort).collect { d ->
                    val l = live ?: return@collect
                    live = when (d) {
                        is Delta.Text -> l.copy(text = l.text + d.text, queued = 0)
                        is Delta.Thinking -> l.copy(thought = l.thought + d.text, queued = 0)
                        is Delta.Queued -> l.copy(queued = d.ahead)
                        is Delta.Done -> l.also { receipt = d.receipt }
                    }
                }
            } catch (e: ImpulseError) {
                failed = e.message
            } finally {
                val l = live
                live = null
                withContext(Dispatchers.Main) { settle(chatId, replyId, l, receipt, failed) }
            }
        }
    }

    private fun settle(chatId: String, replyId: String, l: Live?, receipt: JSONObject?, failed: String?) {
        val c = chats.firstOrNull { it.id == chatId } ?: return
        val msgs = c.messages.map { m ->
            if (m.id != replyId) m else Message(JSONObject(m.raw.toString()).apply {
                put("text", l?.text.orEmpty())
                if (!l?.thought.isNullOrBlank()) put("thought", l!!.thought)
                remove("streaming")
                receipt?.let { put("receipt", it) }
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
}

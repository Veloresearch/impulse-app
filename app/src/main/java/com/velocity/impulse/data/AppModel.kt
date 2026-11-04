package com.velocity.impulse.data

import android.app.Application
import android.content.Context.MODE_PRIVATE
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

enum class Stage { Connect, SignIn, Ready }

class AppModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("impulse", MODE_PRIVATE)
    private val store = ChatStore(File(app.filesDir, "chats.json"))

    var server by mutableStateOf(loadServer())
        private set
    private var token by mutableStateOf(prefs.getString("token", null))
    var profileName by mutableStateOf(prefs.getString("profile", null))
        private set
    var stage by mutableStateOf(
        when {
            server == null -> Stage.Connect
            token == null -> Stage.SignIn
            else -> Stage.Ready
        },
    )
        private set

    var accent by mutableStateOf(prefs.getInt("accent", 0))
        private set
    var showThinking by mutableStateOf(prefs.getBoolean("thinking", true))
        private set
    var reduceMotion by mutableStateOf(prefs.getBoolean("reduce_motion", false))
        private set

    var profiles by mutableStateOf<List<Profile>>(emptyList())
        private set
    var models by mutableStateOf<List<Model>>(emptyList())
        private set
    var contexts by mutableStateOf<List<Context>>(emptyList())
        private set
    var context by mutableStateOf(prefs.getString("context", null))
        private set
    var online by mutableStateOf<Boolean?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    val chats = mutableStateListOf<Chat>().apply { addAll(store.load().sortedByDescending { it.updated }) }
    var current by mutableStateOf<String?>(null)
        private set
    var streaming by mutableStateOf(false)
        private set
    private var answer: Job? = null

    val chat: Chat? get() = chats.firstOrNull { it.id == current }
    val link: Link? get() = server?.let { Link.of(it.base) }
    private fun api() = Impulse(server!!, token)

    init {
        if (stage == Stage.Ready) refresh()
        if (stage == Stage.SignIn) loadProfiles()
    }

    private fun loadServer(): Server? = prefs.getString("server", null)?.let {
        Server(it, prefs.getString("access_id", null), prefs.getString("access_secret", null))
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

    fun connect(raw: String) = work {
        val s = try {
            Server.parse(raw)
        } catch (e: Exception) {
            throw e as? ImpulseError ?: ImpulseError("That does not look like an address.")
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

    fun loadProfiles() = work { profiles = api().profiles() }

    fun signIn(profile: Profile, password: String) = work {
        val t = Impulse(server!!).login(profile.id, password)
        token = t
        profileName = profile.name
        prefs.edit().putString("token", t).putString("profile", profile.name).apply()
        stage = Stage.Ready
        refresh()
    }

    fun signOut() {
        val old = api()
        viewModelScope.launch { old.logout() }
        token = null
        prefs.edit().remove("token").remove("profile").apply()
        stage = Stage.SignIn
        loadProfiles()
    }

    fun forgetServer() {
        signOut()
        server = null
        prefs.edit().remove("server").remove("access_id").remove("access_secret").apply()
        stage = Stage.Connect
    }

    fun refresh() = viewModelScope.launch {
        if (stage != Stage.Ready) return@launch
        try {
            val a = api()
            models = a.models()
            contexts = runCatching { a.contexts() }.getOrDefault(emptyList())
            online = true
        } catch (e: ImpulseError) {
            online = false
            if (e.message?.contains("401") == true || e.code == "unauthorized") signOut()
        }
    }

    fun useContext(name: String?) {
        context = name
        prefs.edit().putString("context", name).apply()
    }

    fun pickAccent(i: Int) {
        accent = i
        prefs.edit().putInt("accent", i).apply()
    }

    fun setThinking(on: Boolean) {
        showThinking = on
        prefs.edit().putBoolean("thinking", on).apply()
    }

    fun calmMotion(on: Boolean) {
        reduceMotion = on
        prefs.edit().putBoolean("reduce_motion", on).apply()
    }

    fun newChat() {
        stop()
        current = null
    }

    fun open(id: String) {
        stop()
        current = id
    }

    fun delete(id: String) {
        if (current == id) newChat()
        chats.removeAll { it.id == id }
        persist()
    }

    fun send(text: String) {
        val prompt = text.trim()
        if (prompt.isEmpty() || streaming) return
        val model = models.firstOrNull()?.id ?: "velocity"
        val base = chat ?: Chat(title = prompt.lineSequence().first().take(60)).also { chats.add(0, it); current = it.id }
        val asked = base.messages + Message("user", prompt)
        update(base.id) { it.copy(messages = asked + Message("assistant", ""), updated = System.currentTimeMillis()) }
        streaming = true
        answer = viewModelScope.launch {
            var thinking = ""
            var reply = ""
            try {
                api().chat(model, asked, context).collect { d ->
                    when (d) {
                        is Delta.Thinking -> thinking += d.text
                        is Delta.Text -> reply += d.text
                    }
                    update(base.id) { it.copy(messages = asked + Message("assistant", reply, thinking)) }
                }
            } catch (e: ImpulseError) {
                if (reply.isEmpty()) update(base.id) { it.copy(messages = asked) }
                error = e.message
            } finally {
                streaming = false
                persist()
            }
        }
    }

    fun stop() {
        answer?.cancel()
        answer = null
    }

    private fun update(id: String, f: (Chat) -> Chat) {
        val i = chats.indexOfFirst { it.id == id }
        if (i < 0) return
        val next = f(chats[i])
        if (i == 0) chats[0] = next else {
            chats.removeAt(i)
            chats.add(0, next)
        }
    }

    private fun persist() {
        val snapshot = chats.toList()
        viewModelScope.launch(Dispatchers.IO) { store.save(snapshot) }
    }
}

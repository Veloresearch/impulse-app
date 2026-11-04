package com.velocity.impulse.data

import android.Manifest
import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
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
import org.json.JSONTokener
import java.io.File
import java.time.LocalDate

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

/**
 * How full a chat's window was at its last answer, in tokens: [used] of [window], made of what
 * came from [memory], the [conversation] and the last [reply].
 */
data class Meter(val used: Long, val window: Int, val memory: Long, val conversation: Long, val reply: Long)

class AppModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("impulse", MODE_PRIVATE)
    private val secure = Secure(prefs)
    private val ctx: Application get() = getApplication()
    private val cache = ChatCache(File(app.filesDir, "chats.json"))
    private val media = Media(File(app.filesDir, "media"))
    private val syncing = Mutex()

    var server by mutableStateOf(loadServer())
        private set
    private var token by mutableStateOf(secure.get("token"))
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
    var pluginsDefault by mutableStateOf(prefs.getBoolean("plugins_default", true))
        private set
    /** "dark", "light", "amoled" (pure black) or "auto" (follows the phone). */
    var theme by mutableStateOf(prefs.getString("theme", "dark") ?: "dark")
        private set
    /** The phone's "Remove animations" is on. */
    var systemCalm by mutableStateOf(readSystemCalm())
        private set
    /** Motion is reduced, by the app's switch or the phone's. */
    val calm: Boolean get() = reduceMotion || systemCalm

    /** The intro plays on the first cold start of a day only. */
    val showIntro: Boolean = LocalDate.now().toString().let { today ->
        (prefs.getString("intro_day", null) != today).also { if (it) prefs.edit().putString("intro_day", today).apply() }
    }

    /** The app is on screen; when it is not, a finished answer is announced with a notification. */
    private var visible = false
    /** The moment to ask for notifications has come (Android 13+); MainActivity asks and calls [askedNotifications]. */
    var askNotifications by mutableStateOf(false)
        private set

    /** What another app shared, read and waiting for the new chat's composer; see [takeIncoming]. */
    var incoming by mutableStateOf<Incoming?>(null)
        private set
    /** A launcher shortcut's wish for the new chat, "voice" or "camera"; see [takeShortcut]. */
    var shortcut by mutableStateOf<String?>(null)
        private set
    /** An `impulse://connect` link opened while signed in, waiting for the owner to agree to switch; see [connect]. */
    var connectLink by mutableStateOf<String?>(null)
        private set

    /** How answers are written, shared with the panel through the profile (`vls.chat.style`). */
    var style by mutableStateOf(ChatStyle(json(prefs.getString("style", null)) ?: JSONObject()))
        private set

    /** The app's sampler defaults, under each chat's own: temperature, top_p, top_k, min_p, repeat_penalty, seed, stop, max_tokens. */
    var sampling by mutableStateOf(json(prefs.getString("sampling", null)) ?: JSONObject())
        private set

    /** Web search in a new chat: the shared "search by default". */
    val webDefault: Boolean get() = style.search != "off"

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
    /** Memories to answer from, the chats' own left out. */
    var contexts by mutableStateOf<List<Context>>(emptyList())
        private set
    private var memories by mutableStateOf<List<Context>>(emptyList())
    var defaultContext by mutableStateOf(prefs.getString("context", null))
        private set
    var online by mutableStateOf<Boolean?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    var chats by mutableStateOf(cache.load().sortedByDescending { it.updatedAt })
        private set
    var current by mutableStateOf(prefs.getString("current", null)?.takeIf { id -> chats.any { it.id == id } })
        private set
    /** The temporary chat on screen, never in [chats]. */
    private var scratch by mutableStateOf<Chat?>(null)
    private var temporaryNext by mutableStateOf(false)
    /** The chat on screen, or the one about to start, is temporary: not saved, not synced, nothing kept on the computer. */
    val temporary: Boolean get() = chat?.temporary ?: temporaryNext

    /** Answers being written, by chat: several chats can write at once. */
    val lives = mutableStateMapOf<String, Live>()
    private val answers = HashMap<String, Job>()
    private var pulse: Job? = null

    /** The answer being written in the chat on screen. */
    val live: Live? get() = current?.let { lives[it] }
    val streaming get() = live != null
    /** Some chat is writing an answer. */
    val writing get() = lives.isNotEmpty()

    val chat: Chat? get() = current?.let(::find)
    val link: Link? get() = server?.let { Link.of(it.base) }
    private fun api() = Impulse(server!!, token)

    /** The open side panel, restored on a cold start. */
    var panel by mutableStateOf(prefs.getString("panel", null))
        private set
    private val drafts = mutableStateMapOf<String, String>().apply { json(prefs.getString("drafts", null))?.let { o -> o.keys().forEach { put(it, o.optString(it)) } } }
    private val anchors = HashMap<String, String>().apply { json(prefs.getString("anchors", null))?.let { o -> o.keys().forEach { put(it, o.optString(it)) } } }
    /** Settings changed here that the computer has not taken yet; they win over what it sends back. */
    private val unsent = HashSet<String>()

    init {
        if (stage == Stage.SignIn) loadProfiles()
    }

    private fun json(s: String?): JSONObject? = s?.let { runCatching { JSONObject(it) }.getOrNull() }

    private fun loadServer(): Server? = prefs.getString("server", null)?.let {
        Server(it, prefs.getString("access_id", null), secure.get("access_secret"))
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

    /** Runs [block] against the computer for a screen that calls the API itself; a failure lands in [error]. */
    fun <T> request(block: suspend Impulse.() -> T, done: (T) -> Unit = {}) = viewModelScope.launch {
        if (stage != Stage.Ready) return@launch
        try {
            done(api().block())
        } catch (e: ImpulseError) {
            error = e.message
        }
    }

    fun connect(raw: String) = work {
        val s = try {
            Server.parse(raw)
        } catch (e: Exception) {
            throw e as? ImpulseError ?: ImpulseError(Lang.tr("That does not look like an address.", "To nie wygląda na adres."))
        }
        val found = Impulse(s).profiles()
        server = s
        connectLink = null
        val e = prefs.edit().putString("server", s.base).putString("access_id", s.accessId)
        secure.put(e, "access_secret", s.accessSecret)
        secure.put(e, "token", null).apply()
        token = null
        profiles = found
        stage = Stage.SignIn
    }

    fun loadProfiles() = work { profiles = Impulse(server!!).profiles() }

    fun signIn(profile: Profile, password: String) = work {
        val t = Impulse(server!!).login(profile.id, password)
        if (me?.id != profile.id) forgetChats()
        token = t
        secure.put(prefs.edit(), "token", t).apply()
        remember(profile)
        stage = Stage.Ready
        resume()
    }

    fun signOut() {
        val old = api()
        viewModelScope.launch { old.logout() }
        pause()
        token = null
        secure.put(prefs.edit(), "token", null).apply()
        stage = Stage.SignIn
        loadProfiles()
    }

    fun forgetServer() {
        signOut()
        server = null
        me = null
        forgetChats()
        secure.put(prefs.edit().remove("server").remove("access_id").remove("me"), "access_secret", null).apply()
        stage = Stage.Connect
    }

    private fun forgetChats() {
        answers.values.forEach { it.cancel() }
        cache.forget()
        chats = emptyList()
        scratch = null
        go(null)
        drafts.clear()
        anchors.clear()
        prefs.edit().remove("drafts").remove("anchors").apply()
        viewModelScope.launch(Dispatchers.IO) { media.clear() }
    }

    /** Changes the profile's name, emoji ("" for none) or photo (a data:image URL, "" to remove). */
    fun setProfile(name: String? = null, avatar: String? = null, photo: String? = null) = work {
        api().setProfile(name, avatar, photo)?.let(::remember)
    }

    /** Sets, changes or (with [next] null) removes the password; [done] runs when it worked. */
    fun setPassword(current: String?, next: String?, done: () -> Unit = {}) = work {
        api().setPassword(current, next)?.let(::remember)
        done()
    }

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
        refreshContexts()
        runCatching {
            sendPrefs()
            readPrefs(api().prefs())
        }
        runCatching { api().me()?.let { remember(it) } }
        runCatching { plugins = api().mcpTools(false).map { it.optString("name") }.filter { it.isNotBlank() } }
    }

    private suspend fun refreshContexts() {
        runCatching { api().contexts() }.getOrNull()?.let { all ->
            // The title helper is the service's own; the panel hides it too.
            contexts = all.filterNot { it.name.startsWith("chat-") || it.name == "vls-titles" }
            memories = all.filter { it.name.startsWith("chat-") }
        }
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
        fun value(key: String): Any? = if (key in unsent) null else when (val v = p.opt(key)) {
            is String -> runCatching { JSONTokener(v).nextValue() }.getOrNull()
            null, JSONObject.NULL -> null
            else -> v
        }
        (value("vls.accent") as? String)?.let {
            accent = it
            prefs.edit().putString("accent_name", it).apply()
        }
        (value("vls.chat.effort") as? String)?.let {
            effort = it
            prefs.edit().putString("effort", it).apply()
        }
        (value(ChatStyle.KEY) as? JSONObject)?.let {
            style = ChatStyle(it)
            prefs.edit().putString("style", it.toString()).apply()
        }
    }

    /** The settings shared with the panel, as the panel stores them: JSON text per key. */
    private fun shared(key: String): String? = when (key) {
        "vls.accent" -> JSONObject.quote(accent)
        "vls.chat.effort" -> effort?.let(JSONObject::quote)
        ChatStyle.KEY -> style.raw.toString()
        else -> null
    }

    private suspend fun sendPrefs() {
        if (unsent.isEmpty()) return
        val keys = unsent.toList()
        val set = JSONObject()
        keys.forEach { k -> shared(k)?.let { set.put(k, it) } }
        api().setPrefs(set)
        unsent -= keys.toSet()
    }

    private fun share(key: String) {
        unsent += key
        if (stage == Stage.Ready) viewModelScope.launch { runCatching { sendPrefs() } }
    }

    private suspend fun sync() = syncing.withLock {
        if (stage != Stage.Ready) return@withLock
        try {
            val a = api()
            val out = JSONArray()
            val holding = lives.keys.toSet()
            for (id in cache.dirty.toList()) {
                if (id in holding) continue
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
                    if (current == id && id !in lives) go(null)
                } else if (id !in cache.dirty && id !in lives) {
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

    private fun find(id: String): Chat? = scratch?.takeIf { it.id == id } ?: chats.firstOrNull { it.id == id }

    private fun changed(c: Chat) {
        if (c.temporary) {
            scratch = c
            return
        }
        chats = (chats.filterNot { it.id == c.id } + c).sortedByDescending { it.updatedAt }
        cache.dirty += c.id
        persist()
    }

    private fun pushSoon() = viewModelScope.launch { sync() }

    fun load(p: Probe) = loadWith(p.path, p.name) { api().load(p) }

    /** Loads with the load dialog's choices; see [Impulse.load]. */
    fun load(path: String, name: String, context: Int, gpuLayers: Int? = null, backend: String? = null, tuning: JSONObject? = null, remember: Boolean = false) =
        loadWith(path, name) { api().load(path, context, gpuLayers, backend, tuning, remember) }

    private fun loadWith(path: String, name: String, block: suspend () -> Unit) {
        if (loading != null) return
        loading = path
        viewModelScope.launch {
            try {
                block()
            } catch (e: ImpulseError) {
                error = Lang.tr("Could not load $name: ${e.message}", "Nie udało się wczytać $name: ${e.message}")
            }
            refreshStatus()
            loading = null
        }
    }

    /** Deletes a model file from the computer's library. */
    fun removeModel(path: String) = work {
        api().removeModel(path)
        library = api().library()
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

    /** What a chat's own memory holds, as the computer counts it; null before it holds anything. */
    fun memory(chatId: String): Context? = memories.firstOrNull { it.name == memoryOf(chatId) }

    /** Empties a chat's memory on the computer; the conversation itself stays. */
    fun forgetMemory(chatId: String) = work {
        api().removeContext(memoryOf(chatId))
        refreshContexts()
    }

    /** How full the chat's window was at its last answer; null before it has one. */
    fun meter(chatId: String? = current): Meter? {
        val c = chatId?.let(::find) ?: return null
        val r = c.lastReceipt ?: return null
        val prompt = r.optLong("working_tokens").takeIf { it > 0 } ?: r.optLong("prompt_tokens")
        if (prompt <= 0) return null
        val window = status?.model?.context?.takeIf { it > 0 } ?: c.messages.lastOrNull { it.meta != null }?.meta?.optInt("window") ?: 0
        val memory = minOf(r.optLong("selected_tokens"), prompt)
        val reply = r.optLong("generated_tokens")
        return Meter(prompt + reply, window, memory, prompt - memory, reply)
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
        share("vls.accent")
    }

    fun pickEffort(e: String) {
        effort = e
        prefs.edit().putString("effort", e).apply()
        share("vls.chat.effort")
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

    fun pickWebDefault(on: Boolean) = setStyle("search", if (on) "on" else "off")

    fun pickPluginsDefault(on: Boolean) {
        pluginsDefault = on
        prefs.edit().putBoolean("plugins_default", on).apply()
    }

    fun pickTheme(name: String) {
        if (name !in THEMES) return
        theme = name
        prefs.edit().putString("theme", name).apply()
    }

    private fun readSystemCalm(): Boolean {
        val cr = ctx.contentResolver
        return Settings.Global.getFloat(cr, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f ||
            Settings.Global.getFloat(cr, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f) == 0f
    }

    /** MainActivity says whether the app is on screen; coming back re-reads the phone's animation setting. */
    fun onScreen(on: Boolean) {
        visible = on
        if (on) systemCalm = readSystemCalm()
    }

    fun askedNotifications() {
        askNotifications = false
        prefs.edit().putBoolean("asked_notifications", true).apply()
    }

    /** Opens a chat a notification named, if it is still here. */
    fun reveal(chatId: String) {
        if (find(chatId) != null) open(chatId)
    }

    /** Starts a new chat with what another app shared; files that cannot go to the model are explained in [error]. */
    fun receive(s: Shared) {
        newChat()
        viewModelScope.launch {
            if (status == null && stage == Stage.Ready) refreshStatus()
            val vision = status?.model?.vision ?: true
            val refused = mutableListOf<String>()
            val files = s.uris.mapNotNull { uri ->
                runCatching { Attachment.read(ctx, uri, vision) { refused += it } }.getOrElse { refused += Lang.tr("A shared file could not be opened.", "Nie dało się otworzyć udostępnionego pliku."); null }
            }
            if (refused.isNotEmpty()) error = refused.joinToString("\n")
            if (s.text.isNotEmpty() || files.isNotEmpty()) incoming = Incoming(s.text, files)
        }
    }

    /** Hands the shared words and files to the composer, once. */
    fun takeIncoming(): Incoming? = incoming.also { incoming = null }

    /** A launcher shortcut: "new", "voice" or "camera". Each starts a new chat. */
    fun openShortcut(name: String) {
        newChat()
        shortcut = name.takeIf { it == "voice" || it == "camera" }
    }

    fun takeShortcut(): String? = shortcut.also { shortcut = null }

    /** An `impulse://connect` link: followed at once while pairing, otherwise kept in [connectLink] for the owner to confirm. */
    fun openLink(raw: String) {
        if (stage == Stage.Connect) connect(raw) else connectLink = raw
    }

    fun dropConnectLink() {
        connectLink = null
    }

    /**
     * Changes one answer setting shared with the panel: persona, tone, emoji, visuals, layout,
     * lang, byName, custom, autoTitle, thinkOpen, maxTokens or search (see [ChatStyle]).
     */
    fun setStyle(key: String, value: Any?) {
        style = style.with(key, value)
        prefs.edit().putString("style", style.raw.toString()).apply()
        share(ChatStyle.KEY)
    }

    /** Changes one app-wide sampler default (a key of [SAMPLING]); null goes back to the model's own. */
    fun setSampling(key: String, value: Any?) {
        sampling = JSONObject(sampling.toString()).apply { if (value == null) remove(key) else put(key, value) }
        prefs.edit().putString("sampling", sampling.toString()).apply()
    }

    /** Drops the phone's copy of the chats and pulls them all again; the sign-in stays. */
    fun clearCache() {
        answers.values.forEach { it.cancel() }
        go(null)
        viewModelScope.launch {
            syncing.withLock {
                synchronized(cache) { cache.forget() }
                chats = emptyList()
            }
            sync()
        }
    }

    private fun go(id: String?) {
        current = id
        prefs.edit().putString("current", id?.takeIf { find(it)?.temporary != true }).apply()
    }

    /** The unsent words of a chat's composer; null is the new chat. */
    fun draft(chatId: String?): String = drafts[chatId.orEmpty()].orEmpty()

    fun keepDraft(chatId: String?, text: String) {
        val k = chatId.orEmpty()
        if (text.isBlank()) drafts.remove(k) else drafts[k] = text
        prefs.edit().putString("drafts", JSONObject(drafts.toMap()).toString()).apply()
    }

    /** The message a chat was scrolled to, to open it there again. */
    fun anchor(chatId: String): String? = anchors[chatId]

    fun keepAnchor(chatId: String, messageId: String?) {
        if (messageId == null) anchors.remove(chatId) else anchors[chatId] = messageId
        prefs.edit().putString("anchors", JSONObject(anchors as Map<*, *>).toString()).apply()
    }

    fun keepPanel(name: String?) {
        panel = name
        prefs.edit().putString("panel", name).apply()
    }

    /** A new chat; [temporary] starts one that is not saved, not synced, and leaves nothing on the computer. An answer being written elsewhere goes on. */
    fun newChat(temporary: Boolean = false) {
        leaveScratch()
        temporaryNext = temporary
        go(null)
    }

    fun open(id: String) {
        if (scratch?.id != id) leaveScratch()
        temporaryNext = false
        go(id)
        AnswerService.dismiss(ctx, id)
    }

    private fun leaveScratch() {
        val s = scratch ?: return
        stop(s.id)
        scratch = null
        viewModelScope.launch(Dispatchers.IO) { media.drop(s.messages.map { it.id }) }
    }

    fun delete(id: String) {
        if (scratch?.id == id) return newChat()
        stop(id)
        if (current == id) newChat()
        val gone = find(id)
        chats = chats.filterNot { it.id == id }
        cache.dirty -= id
        cache.gone += id
        drafts.remove(id)
        anchors.remove(id)
        persist()
        pushSoon()
        gone?.let { c -> viewModelScope.launch(Dispatchers.IO) { media.drop(c.messages.map { it.id }) } }
    }

    /** The chat's personality: null follows the one in the answer settings, "off" is none. */
    fun setPersona(id: String?) {
        chat?.let { changed(it.edit { put("persona", id ?: JSONObject.NULL) }) }
        pushSoon()
    }

    fun pin(id: String) {
        find(id)?.let { c -> changed(c.edit { put("pinned", !c.pinned) }) }
        pushSoon()
    }

    /** Edits a chat's session record, keeps it and sends it to the computer. */
    fun editChat(id: String, block: JSONObject.() -> Unit) {
        find(id)?.let { changed(it.edit(block)) }
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

    /** One sampler setting for this chat only (a key of [SAMPLING]); null follows the app's default. */
    fun setChatOption(key: String, value: Any?) = editCurrent { setIn("options", key, value) }

    fun setTemperature(v: Double?) = setChatOption("temperature", v)

    fun setMaxTokens(n: Int?) = setChatOption("max_tokens", n)

    fun setChatWeb(on: Boolean?) = editCurrent { setIn("appTools", "web", on) }

    fun setChatPlugins(on: Boolean?) = editCurrent { setIn("appTools", "plugins", on) }

    /** Shows version [j] of an answer that was asked for more than once. */
    fun showVersion(messageId: String, j: Int) {
        val c = chat ?: return
        if (c.id in lives) return
        changed(c.withMessages(c.messages.map { if (it.id == messageId) it.showing(j) else it }))
        pushSoon()
    }

    /** A memory a chat may read from and write into: its own or a library put on it, never another chat's. */
    private fun store(c: Chat) = c.contextName?.takeIf { !it.startsWith("chat-") || it == c.memoryName }

    /**
     * Sends a question with what was attached to it. Pictures and scanned pages go to the model to
     * look at; a file's words go into the question and into the chat's memory, so later questions
     * have it too. The message keeps the panel's `files` record of each.
     */
    fun send(text: String, attached: List<Attachment> = emptyList()) {
        val prompt = text.trim()
        if ((prompt.isEmpty() && attached.isEmpty()) || streaming) return
        val base = chat ?: Chat.start(prompt.ifEmpty { attached.first().name }, status?.model?.name, defaultContext)
            .let { if (temporaryNext) it.edit { put("temporary", true) } else it }
        val question = Message.of("user", prompt)
        if (attached.isNotEmpty()) question.raw.put("files", JSONArray(attached.map { it.record() }))
        val docs = attached.mapNotNull { a -> a.text?.let { Doc(a.name, it) } }
        val pictures = attached.flatMap { it.pictures }
        keepDraft(chat?.id, "")
        answer(base, base.messages, question, null, pictures, docs)
        go(base.id)
        viewModelScope.launch(Dispatchers.IO) { media.keep(question.id, pictures, docs) }
        if (!base.temporary) for (d in docs) {
            viewModelScope.launch { runCatching { api().writeContext(d.text, d.name, "chat:${base.id}", store(base)) } }
        }
    }

    /**
     * Asks again for an answer, the last one unless [answerId] names an older one (what came after
     * it gives way). The question's pictures and files go again, and the answer it had stays as a
     * version, ‹ 1/2 ›.
     */
    fun retry(answerId: String? = null) {
        val c = chat ?: return
        if (streaming) return
        val msgs = c.messages
        val upTo = answerId?.let { id -> msgs.indexOfFirst { it.id == id } }?.takeIf { it >= 0 } ?: msgs.lastIndex
        val q = (upTo downTo 0).firstOrNull { msgs[it].role == "user" } ?: return
        val prior = msgs.subList(q + 1, upTo + 1).lastOrNull { it.role == "assistant" && (it.text.isNotBlank() || it.versions > 1) }
        answer(c, msgs.take(q), msgs[q], prior)
    }

    /** Changes a sent question and asks it again: what came after it gives way, its answer stays as a version. */
    fun edit(messageId: String, text: String) {
        val c = chat ?: return
        if (streaming) return
        val msgs = c.messages
        val ix = msgs.indexOfFirst { it.id == messageId && it.role == "user" }
        if (ix < 0 || (text.isBlank() && msgs[ix].files.isEmpty())) return
        val prior = msgs.getOrNull(ix + 1)?.takeIf { it.role == "assistant" && (it.text.isNotBlank() || it.versions > 1) }
        val user = Message(JSONObject(msgs[ix].raw.toString()).put("text", text.trim()).put("at", System.currentTimeMillis()))
        answer(c, msgs.take(ix), user, prior)
    }

    /** Sends a question again as it was, e.g. one whose answer failed. */
    fun resend(messageId: String) {
        chat?.messages?.firstOrNull { it.id == messageId }?.let { edit(messageId, it.text) }
    }

    /** Picks up the last answer where its length limit cut it ([Message.cut]), in the same message. */
    fun continueAnswer() {
        val c = chat ?: return
        if (streaming) return
        val last = c.messages.lastOrNull()?.takeIf { it.role == "assistant" && it.cut } ?: return
        changed(c.withMessages(c.messages.dropLast(1) + Message(JSONObject(last.raw.toString()).put("streaming", true))))
        stream(c.id, last.id, c.messages, Message.of("user", Wire.CONTINUE), emptyList(), emptyList(), keep = last.text)
    }

    /** Answers [user] after [before]: a new reply, or a new version of [prior]. Null [pictures]/[docs] are read back from what the question was sent with. */
    private fun answer(c: Chat, before: List<Message>, user: Message, prior: Message?, pictures: List<String>? = null, docs: List<Doc>? = null) {
        val fresh = Message.of("assistant", "").also { it.raw.put("streaming", true) }
        val reply = prior?.withVersion(fresh) ?: fresh
        changed(c.withMessages(before + user + reply).edit { status?.model?.name?.let { put("modelName", it) } })
        stream(c.id, reply.id, before, user, pictures, docs)
    }

    private fun stream(chatId: String, replyId: String, before: List<Message>, user: Message, pictures: List<String>?, docs: List<Doc>?, keep: String = "") {
        lives[chatId] = Live(chatId, replyId, text = keep)
        AnswerService.keep(ctx, lives.size)
        if (Build.VERSION.SDK_INT >= 33 && !prefs.getBoolean("asked_notifications", false) &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) askNotifications = true
        answers[chatId] = viewModelScope.launch {
            var receipt: JSONObject? = null
            var failed: String? = null
            var tools: Tools? = null
            var meta: JSONObject? = null
            try {
                val c = find(chatId) ?: return@launch
                val a = api()
                val model = status?.model
                val (shown, files, earlier) = withContext(Dispatchers.IO) {
                    val f = docs ?: media.docs(user.id)
                    val e = if (f.isEmpty()) before.filter { it.role == "user" && it.files.isNotEmpty() }.asReversed().flatMap { media.docs(it.id) }.distinctBy { it.name } else emptyList()
                    Triple(pictures ?: media.pictures(user.id), f, e)
                }
                val offered = if (c.plugins ?: pluginsDefault) runCatching { a.mcpTools(true) }.getOrDefault(emptyList()) else emptyList()
                val box = Tools(a, c.web ?: webDefault, offered).also { tools = it }
                val window = model?.context ?: 0
                val want = ((c.options.opt("max_tokens") ?: sampling.opt("max_tokens")) as? Number)?.toInt() ?: style.maxTokens.takeIf { it > 0 } ?: window.takeIf { it > 0 } ?: 8192
                val floor = maxOf(1024, minOf(want, window / 4))
                val talk = wire(c, before, user, files, earlier, box, model, floor)
                val options = JSONObject()
                for (src in listOf(sampling, c.options)) for (k in SAMPLING) if (src.has(k) && !src.isNull(k)) options.put(k, src.get(k))
                options.put("max_tokens", Docs.answerRoom(window, talk.map { it.optString("content") }, box.any, want, floor))
                if (keep.isEmpty()) meta = meta(model, box, user)
                receipt = answerWith(a, chatId, box, talk, store(c), shown, options, !c.temporary, keep)
            } catch (e: ImpulseError) {
                failed = e.message
            } finally {
                val l = lives[chatId]?.takeIf { it.messageId == replyId }
                if (l != null) lives.remove(chatId)
                answers.remove(chatId)
                settle(chatId, replyId, l, receipt, failed, tools, meta, keep.isNotEmpty(), user.text.takeIf { before.isEmpty() })
                AnswerService.keep(ctx, lives.size)
            }
        }
    }

    /** The turn as the model reads it (see [Wire]): standing facts and rules first, this turn's files, time and language with the question. */
    private fun wire(c: Chat, before: List<Message>, user: Message, docs: List<Doc>, earlier: List<Doc>, tools: Tools, model: Loaded?, floor: Int): MutableList<JSONObject> {
        val window = model?.context ?: 0
        val vision = model?.vision == true
        val s = style.forModel(small(model))
        val name = me?.name.orEmpty()
        val system = listOf(
            Wire.environment(name, ChatStyle.LANGUAGES[s.lang], window, tools.offered.contains("web"), tools.servers, vision),
            c.systemPrompt ?: Wire.rules(vision),
            s.lines(c.persona ?: s.persona, name),
        ).filter { it.isNotBlank() }.joinToString("\n\n")
        val history = Wire.history(before + user)
        val talk = mutableListOf(JSONObject().put("role", "system").put("content", system))
        talk += history
        val last = talk.last()
        if (last.optString("role") != "user") return talk
        val said = history.map { it.optString("content") }
        var content = last.optString("content")
        if (docs.isNotEmpty()) {
            val note = "(attached: ${user.files.joinToString(", ")})"
            content = Docs.withFiles(content.removePrefix(note).trim(), docs, Docs.budget(window, said, floor, 0.6))
        }
        val earlierText = if (docs.isEmpty()) Docs.earlierBlock(earlier, Docs.budget(window, said, floor, 0.4), user.text) else ""
        content = Wire.addContext(content, listOf(Wire.now(user.files + earlier.map { it.name }), earlierText, if (docs.isNotEmpty()) Docs.FILE_RULE else "").filter { it.isNotBlank() }.joinToString("\n\n"))
        last.put("content", "$content\n\n${Wire.note(s, user.text)}")
        return talk
    }

    /** Under ~14B parameters or a ternary/PQ quant: the panel's small-model defaults apply. */
    private fun small(model: Loaded?): Boolean {
        model ?: return false
        val params = library.firstOrNull { it.path == model.path }?.params ?: 0L
        return (params in 1 until 14_000_000_000L) || Regex("(?:^|[-_.\\s])(?:TQ|PQ|PTQ)\\d_\\d", RegexOption.IGNORE_CASE).containsMatchIn(model.path.substringAfterLast('/').substringAfterLast('\\'))
    }

    /** How an answer was asked for, as the panel notes it for "About this answer". */
    private fun meta(model: Loaded?, tools: Tools, user: Message) = JSONObject().apply {
        model?.let { m ->
            put("model", m.name).put("window", m.context)
            QUANT.find(m.path.substringAfterLast('/').substringAfterLast('\\'))?.let { put("quant", it.groupValues[1].uppercase()) }
            if (m.thinks && effort != null) put("effort", effort)
        }
        tools.offered.takeIf { it.isNotEmpty() }?.let { put("tools", JSONArray(it)) }
        user.files.takeIf { it.isNotEmpty() }?.let { put("files", JSONArray(it)) }
    }

    /**
     * The tool loop, as the panel runs it (data/tools/loop.ts): the model is offered the tools; each
     * call it makes is run and its result handed back, for up to [TOOL_STEPS] steps; then it answers
     * in words. Returns the last step's receipt, its time and tokens summed over every step.
     * [keep] is the text already written, for an answer being continued.
     */
    private suspend fun answerWith(
        a: Impulse,
        chatId: String,
        tools: Tools,
        talk: MutableList<JSONObject>,
        store: String?,
        images: List<String>,
        options: JSONObject,
        remember: Boolean,
        keep: String,
    ): JSONObject? {
        fun update(f: (Live) -> Live) {
            lives[chatId]?.let { lives[chatId] = f(it) }
        }
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
                a.turn(sent, "chat:$chatId", store, effort, images, if (wordsOnly) null else tools.defs, options, remember).collect { d ->
                    val l = lives[chatId] ?: return@collect
                    val now = System.currentTimeMillis()
                    lives[chatId] = when (d) {
                        is Delta.Text -> {
                            said += d.text
                            l.copy(text = keep + said, step = null, queued = 0, textAt = l.textAt.takeIf { it > 0 } ?: now)
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
                update { it.copy(text = keep, textAt = 0L) }
                talk += JSONObject().put("role", "assistant").put("content", said).put("tool_calls", JSONArray(made))
                for (call in made) {
                    val result = tools.call(call.getJSONObject("function").getString("name"), Tools.args(call)) { what ->
                        update { it.copy(step = what, links = tools.links.toList()) }
                    }
                    update { it.copy(links = tools.links.toList()) }
                    talk += JSONObject().put("role", "tool").put("content", result)
                }
                update { it.copy(step = null) }
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
                update { it.copy(text = keep, textAt = 0L) }
                step++
                continue
            }
            if (tools.steps.isNotEmpty()) {
                val clean = said.substringBefore("<tool_call>").substringBefore("<function=").trimEnd()
                if (clean != said) update { it.copy(text = keep + clean) }
            }
            return last?.let { if (step > 0) JSONObject(it.toString()).put("total_ms", totalMs + it.optLong("total_ms")).put("generated_tokens", generated + it.optLong("generated_tokens")) else it }
        }
    }

    private fun settle(chatId: String, replyId: String, l: Live?, receipt: JSONObject?, failed: String?, tools: Tools?, meta: JSONObject?, continued: Boolean, firstQuestion: String?) {
        val c = find(chatId) ?: return
        val msgs = c.messages.map { m ->
            if (m.id != replyId) m else Message(JSONObject(m.raw.toString()).apply {
                l?.let { put("text", it.text) }
                if (!l?.thought.isNullOrBlank()) put("thought", if (continued) optString("thought") + l!!.thought else l!!.thought)
                if (!continued && l != null && l.thoughtAt > 0) put("thoughtMs", (if (l.textAt > 0) l.textAt else System.currentTimeMillis()) - l.thoughtAt)
                remove("streaming")
                receipt?.let { put("receipt", it) }
                tools?.seen?.takeIf { it.isNotEmpty() && !continued }?.let { s -> put("sources", JSONArray(s.map { (t, u) -> JSONObject().put("title", t).put("url", u) })) }
                tools?.steps?.takeIf { it.isNotEmpty() && !continued }?.let { s -> put("appSteps", JSONArray(s.map { it.json() })) }
                meta?.let { put("meta", it.apply { if (tools?.seen?.isNotEmpty() == true) put("web", true) }.put("done", System.currentTimeMillis())) }
                if (failed != null) put("error", failed) else remove("error")
            })
        }
        changed(c.withMessages(msgs))
        if (failed != null && chatId == current) error = failed
        if (!visible && (receipt != null || failed != null)) {
            AnswerService.done(ctx, chatId, c.title, failed ?: l?.text.orEmpty().trim().take(280), failed != null)
        }
        if (c.temporary) return
        pushSoon()
        if (receipt != null) viewModelScope.launch {
            delay(1500)
            refreshContexts()
        }
        val answer = l?.text.orEmpty()
        if (failed == null && firstQuestion != null && !c.titled && style.autoTitle && answer.isNotBlank()) title(chatId, firstQuestion, answer)
    }

    /** The model names the conversation once it has answered; a failure keeps the provisional title. */
    private fun title(chatId: String, question: String, answer: String) = viewModelScope.launch {
        var out = ""
        val asked = listOf(
            JSONObject().put("role", "system").put("content", Wire.TITLE),
            JSONObject().put("role", "user").put("content", "User: ${question.take(600)}\nAssistant: ${answer.take(600)}\n\nTitle:"),
        )
        try {
            api().turn(asked, null, null, "off", options = JSONObject().put("max_tokens", 24).put("temperature", 0.3), remember = false).collect { if (it is Delta.Text) out += it.text }
        } catch (e: ImpulseError) {
            return@launch
        }
        val t = out.replace(Regex("<think>[\\s\\S]*?(</think>|$)"), "").trim().lineSequence().firstOrNull().orEmpty()
            .replace(Regex("^[\"'\\s#*]+|[\"'\\s.*]+$"), "").trim()
        if (t.isEmpty() || t.length > 60) return@launch
        find(chatId)?.takeIf { !it.titled }?.let { editChat(chatId) { put("title", t).put("titled", true) } }
    }

    /** Stops the answer being written in a chat, the one on screen by default; what it wrote stays. */
    fun stop(chatId: String? = current) {
        chatId?.let { answers[it]?.cancel() }
    }

    companion object {
        /** The sampler settings an app default or a chat may set, as `/api/v2/chat` takes them. */
        val SAMPLING = listOf("temperature", "top_p", "top_k", "min_p", "repeat_penalty", "seed", "stop", "max_tokens")
        val THEMES = listOf("dark", "light", "amoled", "auto")
        private const val TOOL_STEPS = 6
        private val QUANT = Regex("(?:^|[-_. ])(I?Q\\d(?:_[A-Za-z0-9]+)*|F16|BF16|F32|FP16)(?=\\.gguf$|\\.mfy$|$|[-_. ])", RegexOption.IGNORE_CASE)
        private const val CLOSE =
            "No more tools: answer the user's latest message now, in their language, from what you found above. " +
                "Say in one sentence what is missing or could not be done. Never mention tools, steps or this note."
    }
}

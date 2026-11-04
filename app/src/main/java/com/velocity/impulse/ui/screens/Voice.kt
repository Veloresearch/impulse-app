package com.velocity.impulse.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.velocity.impulse.R
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Speech
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.components.Feel
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.IconBtn
import com.velocity.impulse.ui.components.feel
import com.velocity.impulse.ui.components.glass
import com.velocity.impulse.ui.components.glassSource
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.LocalReduceMotion
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Signal
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/** Speech to text with the phone's own recognizer. Make one with [rememberDictation]. */
class Dictation internal constructor(private val ctx: Context) {
    var listening by mutableStateOf(false)
        private set
    /** How loud the microphone is, 0..1. Read it in draw or graphicsLayer so it never recomposes. */
    var level by mutableFloatStateOf(0f)
        private set
    /** The words heard so far, before they are final. */
    var partial by mutableStateOf("")
        private set
    /** Why listening stopped, as a sentence to show; null when nothing went wrong. */
    var problem by mutableStateOf<String?>(null)
        private set
    /** The microphone was refused or the phone cannot recognise speech; [start] asks again. */
    internal var blocked = false
        private set
    internal var ask: () -> Unit = {}
    internal var onFinal: (String) -> Unit = {}
    internal var onPartial: (String) -> Unit = {}
    internal var onEnd: (Int?) -> Unit = {}
    private var rec: SpeechRecognizer? = null
    private var asking = false
    private val intent by lazy {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName)
    }

    fun start() {
        if (listening || asking) return
        problem = null
        blocked = false
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            return refuse(tr("Speech recognition is not available on this phone. Install or turn on Speech Services by Google.", "Rozpoznawanie mowy nie jest dostępne na tym telefonie. Zainstaluj lub włącz Usługi mowy Google."))
        }
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            asking = true
            return ask()
        }
        val r = rec ?: SpeechRecognizer.createSpeechRecognizer(ctx).also { it.setRecognitionListener(listener); rec = it }
        partial = ""
        listening = true
        r.startListening(intent)
    }

    /** Stops listening; what was said so far still arrives as the final words. */
    fun stop() {
        if (listening) rec?.stopListening()
    }

    /** Stops listening and drops what was heard. */
    fun cancel() {
        rec?.cancel()
        quiet()
        partial = ""
    }

    internal fun allowed(ok: Boolean) {
        asking = false
        if (ok) start() else refuse(tr("Impulse needs the microphone to hear you. Allow it in the phone's settings.", "Impulse potrzebuje mikrofonu, żeby cię słyszeć. Zezwól na niego w ustawieniach telefonu."))
    }

    internal fun release() {
        rec?.destroy()
        rec = null
        quiet()
    }

    private fun quiet() {
        listening = false
        level = 0f
    }

    private fun refuse(why: String) {
        blocked = true
        problem = why
        quiet()
        onEnd(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
    }

    private fun best(b: Bundle?) = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {
            level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        }
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            level = 0f
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            val t = best(partialResults)
            if (t.isEmpty() || !listening) return
            partial = t
            onPartial(t)
        }

        override fun onResults(results: Bundle?) {
            if (!listening) return
            quiet()
            val t = best(results)
            partial = ""
            if (t.isNotEmpty()) onFinal(t)
            onEnd(null)
        }

        override fun onError(error: Int) {
            // A cancelled session still reports an error on some phones.
            if (!listening) return
            quiet()
            when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> return allowed(false)
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER ->
                    problem = tr("Speech recognition on this phone needs an internet connection.", "Rozpoznawanie mowy na tym telefonie potrzebuje internetu.")
                SpeechRecognizer.ERROR_AUDIO -> problem = tr("The microphone stopped. Try again.", "Mikrofon przestał działać. Spróbuj ponownie.")
            }
            onEnd(error)
        }
    }
}

/** Dictation for a text field: [onText] gets the final words of each turn. Asks for the microphone on the first [Dictation.start]. */
@Composable
fun rememberDictation(onText: (String) -> Unit): Dictation {
    val d = rememberRecognizer()
    val latest by rememberUpdatedState(onText)
    d.onFinal = { latest(it) }
    return d
}

@Composable
private fun rememberRecognizer(): Dictation {
    val ctx = LocalContext.current
    val d = remember { Dictation(ctx.applicationContext) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), d::allowed)
    d.ask = { ask.launch(Manifest.permission.RECORD_AUDIO) }
    DisposableEffect(d) { onDispose { d.release() } }
    return d
}

object VoiceState {
    var showing by mutableStateOf(false)
        private set
    internal var present by mutableStateOf(false)
        private set
    internal var origin = Rect.Zero
        private set
    private var source = Rect.Zero

    /** Opens voice mode, growing out of the button marked with [voiceSource]. */
    fun open() {
        origin = source
        present = true
        showing = true
    }

    fun close() {
        showing = false
    }

    internal fun done() {
        if (!showing) present = false
    }

    internal fun mark(r: Rect) {
        source = r
    }
}

/** Marks the button voice mode grows out of and shrinks back into. */
fun Modifier.voiceSource(): Modifier = onGloballyPositioned { VoiceState.mark(it.boundsInRoot()) }

@Composable
fun VoiceHost(m: AppModel = viewModel()) {
    if (VoiceState.present) Voice(m)
}

private enum class Turn { Listening, Thinking, Speaking }

/**
 * One spoken conversation: hears a question, sends it, reads the answer aloud sentence by sentence
 * as it streams, and goes back to listening. The microphone stays open while it speaks, so the
 * person can cut in; words that match what is being said are its own voice coming back.
 */
private class Talk(ctx: Context, private val m: AppModel, private val d: Dictation, private val scope: CoroutineScope, private val view: View) {
    var turn by mutableStateOf(Turn.Listening)
        private set
    var muted by mutableStateOf(false)
        private set
    var heard by mutableStateOf("")
        private set
    var saying by mutableStateOf("")
        private set
    var step by mutableStateOf<String?>(null)
        private set
    var problem by mutableStateOf<String?>(null)
        private set
    /** Jumps on each spoken word and fades every frame. */
    @Volatile var pulse = 0f

    private var open = false
    private var away = false
    private var voice = 0
    private var lang: Locale? = null
    private var answerLang: Locale? = null
    private var serial = 0
    private var queued = 0
    private var said = 0
    private var finished = false
    private val lines = HashMap<String, String>()
    private val waiting = ArrayList<String>()
    private val out = HashSet<String>()
    private var chatId: String? = null
    private var reply: Job? = null
    private var restart: Job? = null

    private val tts = TextToSpeech(ctx.applicationContext) { status -> scope.launch { ready(status == TextToSpeech.SUCCESS) } }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                scope.launch {
                    lines[utteranceId]?.let {
                        saying = it
                        turn = Turn.Speaking
                    }
                }
            }

            override fun onDone(utteranceId: String?) {
                scope.launch { spoke(utteranceId) }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                scope.launch { spoke(utteranceId) }
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                pulse = 1f
            }
        })
        d.onFinal = ::heardAll
        d.onPartial = ::hearing
        d.onEnd = ::ended
    }

    fun begin() {
        open = true
        listen()
    }

    fun end() {
        open = false
        restart?.cancel()
        shush()
        d.cancel()
        turn = Turn.Listening
        heard = ""
    }

    fun release() {
        end()
        tts.shutdown()
    }

    fun away(gone: Boolean) {
        away = gone
        if (gone) {
            restart?.cancel()
            d.cancel()
        } else {
            listen()
        }
    }

    fun toggleMute() {
        muted = !muted
        if (muted) {
            restart?.cancel()
            d.cancel()
        } else {
            listen()
        }
    }

    fun tap() {
        view.feel(Feel.Tap)
        when {
            muted -> toggleMute()
            turn != Turn.Listening -> interrupt()
            d.blocked -> d.start()
            d.listening -> d.stop()
            else -> listen()
        }
    }

    private fun listen() {
        if (!open || muted || away || d.listening) return
        restart?.cancel()
        d.start()
    }

    private fun ended(error: Int?) {
        if (!open || muted || away || d.blocked) return
        val wait = when (error) {
            null, SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 100L
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 400L
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER -> 2000L
            else -> 300L
        }
        restart?.cancel()
        restart = scope.launch {
            delay(wait)
            listen()
        }
    }

    private fun heardAll(text: String) {
        if (!open || (turn != Turn.Listening && echo(text))) return
        ask(text)
    }

    private fun hearing(text: String) {
        if (turn != Turn.Listening && words(text).size >= 2 && !echo(text)) interrupt()
    }

    private fun interrupt() {
        shush()
        chatId?.let(m::stop)
        turn = Turn.Listening
        view.feel(Feel.Tick)
        listen()
    }

    private fun ask(text: String) {
        heard = text
        shush()
        chatId?.let(m::stop)
        m.stop()
        turn = Turn.Thinking
        view.feel(Feel.Tap)
        reply = scope.launch {
            withTimeoutOrNull(3000) { snapshotFlow { m.streaming }.first { !it } }
            m.send(text)
            val id = m.current
            val live = id?.let { m.lives[it] }
            if (id == null || live == null) return@launch back()
            chatId = id
            follow(id, live.messageId)
        }
    }

    private suspend fun follow(id: String, messageId: String) {
        var last = ""
        var done = 0
        snapshotFlow { m.lives[id]?.takeIf { it.messageId == messageId } }
            .takeWhile { it != null }
            .collect { l ->
                last = l!!.text
                step = l.step
                done = feed(last, done, false)
            }
        step = null
        feed(last, done, true)
        finished = true
        if (said >= queued) back()
    }

    /** Speaks what of [text] after [done] is ready: whole sentences while it streams, the rest at the [end]. */
    private fun feed(text: String, done: Int, end: Boolean): Int {
        if (done > text.length) return done
        val to = if (end) text.length else cut(text, done)
        if (to <= done) return done
        say(text.substring(done, to))
        return to
    }

    private fun say(raw: String) {
        val s = spoken(raw)
        if (s.isEmpty()) return
        out += words(s)
        if (voice < 0) {
            saying = s
            turn = Turn.Speaking
            return
        }
        val id = "v${++serial}"
        lines[id] = s
        queued++
        if (voice == 0) waiting += id else speak(id, s)
    }

    private fun speak(id: String, s: String) {
        val want = answerLang ?: when {
            Speech.isPolish(s) -> POLISH
            Speech.isEnglish(s) -> Locale.US
            else -> Locale.getDefault()
        }.also { answerLang = it }
        if (want != lang) {
            tts.setLanguage(want)
            lang = want
        }
        tts.speak(s, TextToSpeech.QUEUE_ADD, null, id)
    }

    private fun ready(ok: Boolean) {
        voice = if (ok) 1 else -1
        if (!ok) problem = tr("This phone has no voice to read answers aloud. Install one in the phone's text-to-speech settings.", "Ten telefon nie ma głosu do czytania odpowiedzi. Zainstaluj go w ustawieniach zamiany tekstu na mowę.")
        for (id in waiting) {
            val s = lines[id] ?: continue
            if (ok) {
                speak(id, s)
            } else {
                lines.remove(id)
                said++
                saying = s
            }
        }
        waiting.clear()
        if (finished && said >= queued) back()
    }

    private fun spoke(id: String?) {
        if (lines.remove(id) == null) return
        said++
        if (finished && said >= queued) back()
    }

    private fun shush() {
        reply?.cancel()
        reply = null
        tts.stop()
        lines.clear()
        waiting.clear()
        out.clear()
        queued = 0
        said = 0
        finished = false
        answerLang = null
        saying = ""
        step = null
    }

    private fun back() {
        finished = false
        turn = Turn.Listening
        heard = ""
        saying = ""
        listen()
    }

    /** Mostly words just spoken: the phone hearing itself. */
    private fun echo(t: String): Boolean {
        val w = words(t)
        return w.isEmpty() || w.count { it in out } * 2 >= w.size
    }
}

private val POLISH = Locale.forLanguageTag("pl-PL")
private val END = Regex("[.!?…]+[\"'”’)\\]]*(?=\\s)|\\n+")
private const val FENCE = "```"
private val CODE = Regex("```[\\s\\S]*?(?:```|$)")
private val LINK = Regex("!?\\[([^\\]]*)]\\([^)]*\\)")
private val URL = Regex("https?://\\S+")
private val RULE = Regex("(?m)^[\\s|:-]{3,}$")
private val MARK = Regex("(?m)^[ \\t]*(?:#{1,6}|>|[-*+]|\\d+[.)])[ \\t]+")
private val SIGNS = Regex("[*_~`|#<>]+")
private val SPACE = Regex("\\s+")
private val WORD = Regex("[^\\p{L}\\p{N}]+")

private fun words(t: String) = t.lowercase().split(WORD).filter { it.length > 1 }

/** What a reader would say: no code, links read by their text, no markdown signs. */
private fun spoken(s: String) = s.replace(CODE, " ").replace(LINK, "\$1").replace(URL, "").replace(RULE, " ")
    .replace(MARK, "").replace(SIGNS, " ").replace(SPACE, " ").trim()

/** Where the next spoken piece of [text] after [from] ends: the last finished sentence outside code, or [from] for none yet. */
private fun cut(text: String, from: Int): Int {
    val ends = END.findAll(text, from).map { it.range.last + 1 }.toList()
    for (e in ends.asReversed()) if (e - from >= 16 && fences(text, e) % 2 == 0) return e
    if (text.length - from > 280 && fences(text, text.length) % 2 == 0) {
        val space = text.lastIndexOf(' ')
        if (space > from) return space
    }
    return from
}

private fun fences(text: String, end: Int): Int {
    var n = 0
    var i = text.indexOf(FENCE)
    while (i in 0 until end) {
        n++
        i = text.indexOf(FENCE, i + FENCE.length)
    }
    return n
}

@Composable
private fun Voice(m: AppModel) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val calm = LocalReduceMotion.current
    val accent = LocalAccent.current
    val keyboard = LocalSoftwareKeyboardController.current
    val showing = VoiceState.showing
    val d = rememberRecognizer()
    val talk = remember { Talk(ctx, m, d, scope, view) }
    val p = remember { Animatable(0f) }

    LaunchedEffect(showing) {
        if (showing) {
            keyboard?.hide()
            view.feel(Feel.Confirm)
            launch {
                delay(180)
                talk.begin()
            }
        } else {
            talk.end()
        }
        val spec: AnimationSpec<Float> = if (calm) tween(Motion.M2) else Motion.morph()
        p.animateTo(if (showing) 1f else 0f, spec)
        VoiceState.done()
    }
    DisposableEffect(talk) { onDispose { talk.release() } }
    LifecycleStartEffect(talk) {
        talk.away(false)
        onStopOrDispose { talk.away(true) }
    }
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    BackHandler(showing) { VoiceState.close() }

    val clock = remember { mutableFloatStateOf(0f) }
    val energy = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(talk) {
        var last = 0L
        var e = 0f
        while (true) withFrameNanos { now ->
            val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceIn(0f, 0.05f)
            last = now
            val time = clock.floatValue + dt
            talk.pulse *= exp(-dt * 7f)
            val want = when {
                talk.muted -> 0.04f
                talk.turn == Turn.Speaking -> 0.3f + 0.5f * max(talk.pulse, abs(sin(time * 7.3f) * sin(time * 2.9f)))
                talk.turn == Turn.Thinking -> 0.16f + 0.08f * sin(time * 2.4f)
                else -> d.level
            }
            e += (want - e) * (1f - exp(-dt * 10f))
            clock.floatValue = time
            energy.floatValue = e
        }
    }
    val mute = animateFloatAsState(if (talk.muted) 1f else 0f, tween(Motion.M3), label = "mute")

    val haze = remember { HazeState() }
    val shift = remember { arrayOf(Offset.Zero) }
    val path = remember { Path() }
    val shown = { ((p.value - 0.55f) / 0.45f).coerceIn(0f, 1f) }

    BoxWithConstraints(
        Modifier.fillMaxSize()
            .onPlaced { shift[0] = it.positionInRoot() }
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false) } },
    ) {
        val orb = min(maxWidth, maxHeight) * 0.24f
        Canvas(
            Modifier.fillMaxSize()
                .glassSource(haze)
                .pointerInput(talk) { detectTapGestures { if (VoiceState.showing) talk.tap() } },
        ) {
            val t = p.value
            val e = energy.floatValue
            val home = Offset(size.width / 2f, size.height * 0.4f)
            val start = VoiceState.origin.takeIf { it != Rect.Zero && !calm }?.translate(-shift[0])
            if (start == null) {
                drawRect(Tone.App, alpha = t)
            } else {
                drawRect(Color.Black, alpha = 0.35f * t)
                val from = start.minDimension / 2f
                drawCircle(Tone.App, from + (farthest(start.center, size) - from) * t, start.center)
            }
            val c = if (start == null) home else lerp(start.center, home, t)
            val r0 = start?.let { it.minDimension / 2f } ?: 0f
            val r = (r0 + (orb.toPx() - r0) * t) * (1f + 0.14f * e)
            if (r <= 0f) return@Canvas
            val a = if (start == null) t else 1f
            val hue = lerp(accent, Ink.Four, 0.7f * mute.value)
            val halo = r * (2f + 0.5f * e)
            drawCircle(Brush.radialGradient(listOf(hue.copy(alpha = 0.3f * (0.5f + e) * t), Color.Transparent), c, halo), halo, c)
            blob(path, c, r, if (calm) 0f else (0.35f + 1.4f * e) * t, clock.floatValue)
            val light = c + Offset(-0.32f * r, -0.38f * r)
            drawPath(path, Brush.radialGradient(listOf(lerp(hue, Color.White, 0.45f), hue, lerp(hue, Color.Black, 0.4f)), light, r * 1.7f), alpha = a)
            drawPath(path, Color.White.copy(alpha = 0.1f * t), style = Stroke(1.dp.toPx()))
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.26f * t), Color.Transparent), light, r * 0.45f), r * 0.45f, light)
        }

        Box(
            Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp)
                .graphicsLayer { alpha = shown() }
                .widthIn(max = maxWidth - 96.dp).height(36.dp)
                .glass(haze, CircleShape).padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(status(talk), style = Type.Label.copy(color = Ink.Two), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Column(
            Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .padding(top = maxHeight * 0.4f + orb * 1.5f, start = 28.dp, end = 28.dp)
                .graphicsLayer {
                    val k = shown()
                    alpha = k
                    translationY = (1f - k) * 12.dp.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Transcript(talk, d)
        }

        Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 28.dp)
                .graphicsLayer {
                    val k = shown()
                    alpha = k
                    translationY = (1f - k) * 16.dp.toPx()
                },
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            MuteButton(talk.muted, mute::value, haze, talk::toggleMute)
            Box(Modifier.size(64.dp).glass(haze, CircleShape)) {
                IconBtn(Glyph.Close, VoiceState::close, tint = Ink.One, fill = Color.Transparent, size = 64.dp, island = true)
            }
        }
    }
}

private fun status(talk: Talk) = when {
    talk.muted -> tr("Microphone off", "Mikrofon wyłączony")
    talk.turn == Turn.Thinking -> talk.step ?: tr("Thinking", "Myślę")
    talk.turn == Turn.Speaking -> tr("Speaking", "Mówię")
    else -> tr("Listening", "Słucham")
}

@Composable
private fun Transcript(talk: Talk, d: Dictation) {
    val big = Type.brand(26.sp).copy(textAlign = TextAlign.Center)
    val small = Type.Meta.copy(textAlign = TextAlign.Center)
    if (talk.turn == Turn.Listening) {
        val now = d.partial
        when {
            now.isNotEmpty() -> BasicText(tail(now), style = big, maxLines = 4, overflow = TextOverflow.Ellipsis)
            talk.muted -> BasicText(tr("Tap the microphone to talk again.", "Stuknij mikrofon, żeby znowu mówić."), style = big.copy(color = Ink.Three))
            else -> BasicText(tr("Go ahead, I'm listening.", "Mów, słucham."), style = big.copy(color = Ink.Three))
        }
    } else {
        if (talk.heard.isNotEmpty()) BasicText(tail(talk.heard), style = small, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (talk.saying.isNotEmpty()) BasicText(talk.saying, style = big.copy(fontSize = 22.sp, lineHeight = 28.sp), maxLines = 6, overflow = TextOverflow.Ellipsis)
        if (talk.turn == Turn.Speaking) BasicText(tr("Talk or tap to cut in.", "Mów albo stuknij, żeby przerwać."), style = Type.Label.copy(color = Ink.Four, textAlign = TextAlign.Center))
    }
    (d.problem ?: talk.problem)?.let { BasicText(it, style = small.copy(color = Signal.Warn)) }
}

private fun tail(s: String) = if (s.length > 180) "…" + s.takeLast(180).substringAfter(' ') else s

@Composable
private fun MuteButton(muted: Boolean, k: () -> Float, haze: HazeState, onClick: () -> Unit) {
    val ink = if (muted) Tone.App else Ink.One
    Box(Modifier.size(64.dp).glass(haze, CircleShape)) {
        Box(
            Modifier.fillMaxSize()
                .pressable(onClick, CircleShape, depth = 0.88f, stretch = true, feel = if (muted) Feel.On else Feel.Off, feelOnDown = true)
                .drawBehind { drawCircle(Ink.One, alpha = k()) },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(R.drawable.ph_microphone_bold),
                if (muted) tr("Turn the microphone on", "Włącz mikrofon") else tr("Turn the microphone off", "Wyłącz mikrofon"),
                Modifier.size(28.dp).drawWithContent {
                    drawContent()
                    val q = k()
                    if (q > 0f) {
                        val a = Offset(size.width * 0.12f, size.height * 0.12f)
                        drawLine(ink, a, lerp(a, Offset(size.width * 0.88f, size.height * 0.88f), q), 2.4.dp.toPx(), StrokeCap.Round)
                    }
                },
                colorFilter = ColorFilter.tint(ink),
            )
        }
    }
}

private fun farthest(c: Offset, s: Size) = hypot(max(c.x, s.width - c.x), max(c.y, s.height - c.y))

/** A sphere whose edge breathes: three slow waves around the rim, as tall as [wob]. */
private fun blob(path: Path, c: Offset, r: Float, wob: Float, time: Float) {
    path.reset()
    val n = 72
    for (i in 0..n) {
        val th = i * 2f * PI.toFloat() / n
        val k = 1f + wob * (0.045f * sin(3f * th + 1.3f * time) + 0.03f * sin(5f * th - 2.1f * time + 1f) + 0.018f * sin(8f * th + 3.4f * time))
        val x = c.x + cos(th) * r * k
        val y = c.y + sin(th) * r * k
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
}

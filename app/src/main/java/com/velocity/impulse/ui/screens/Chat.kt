package com.velocity.impulse.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Attachment
import com.velocity.impulse.data.Chat
import com.velocity.impulse.data.Message
import com.velocity.impulse.data.Step
import com.velocity.impulse.data.Tools
import com.velocity.impulse.ui.Chrome
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.LocalShell
import com.velocity.impulse.ui.Surface
import com.velocity.impulse.ui.components.Btn
import com.velocity.impulse.ui.components.CopyButton
import com.velocity.impulse.ui.components.Feel
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.IconBtn
import com.velocity.impulse.ui.components.Markdown
import com.velocity.impulse.ui.components.MorphSource
import com.velocity.impulse.ui.components.OnAccent
import com.velocity.impulse.ui.components.Roll
import com.velocity.impulse.ui.components.Variant
import com.velocity.impulse.ui.components.feel
import com.velocity.impulse.ui.components.glass
import com.velocity.impulse.ui.components.glassSource
import com.velocity.impulse.ui.components.morph
import com.velocity.impulse.ui.components.plainText
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Signal
import com.velocity.impulse.ui.theme.Squircle
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Stable
private class Toast {
    var text by mutableStateOf<String?>(null)
        private set
    var shown by mutableIntStateOf(0)
        private set

    fun show(t: String) {
        text = t
        shown++
    }

    fun hide() {
        text = null
    }
}

/**
 * The main screen: the conversation on stage, the frosted composer below it. Another chat or a
 * new one takes the stage as a choreographed scene change; the composer never moves for it.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Conversation(m: AppModel, modifier: Modifier = Modifier) {
    val shell = LocalShell.current
    val d = LocalDensity.current
    val ime = WindowInsets.ime
    val nav = WindowInsets.navigationBars
    val bar = WindowInsets.statusBars
    val toast = remember { Toast() }
    var composerH by remember { mutableIntStateOf(0) }
    val front = remember { mutableStateOf<LazyListState?>(null) }
    val far = with(d) { 48.dp.toPx() }
    val jump by remember {
        derivedStateOf { front.value?.let { it.firstVisibleItemIndex > 0 || it.firstVisibleItemScrollOffset > far } == true }
    }
    val key = remember(shell.scene) { shell.scene to m.current }
    val scenes = updateTransition(key, "scene")
    val rise = with(d) { 40.dp.roundToPx() }
    val scope = rememberCoroutineScope()

    Box(
        modifier.fillMaxSize().drawWithContent {
            drawContent()
            val h = bar.getTop(this) + 12.dp.toPx()
            drawRect(Brush.verticalGradient(0f to Tone.App.copy(alpha = 0.9f), 1f to Color.Transparent, endY = h), size = Size(size.width, h))
        },
    ) {
        Box(Modifier.fillMaxSize().graphicsLayer { translationY = -(ime.getBottom(this) - nav.getBottom(this)).coerceAtLeast(0).toFloat() }) {
            scenes.AnimatedContent(
                Modifier.fillMaxSize(),
                transitionSpec = { sceneChange(shell.calm, rise) },
                contentKey = { it.first },
            ) { k ->
                val live = k.first == scenes.targetState.first
                Scene(
                    m,
                    chatId = if (live) m.current else k.second,
                    live = live,
                    blurOut = !live && !shell.calm && scenes.targetState.second == null,
                    front = front,
                    composerH = { composerH },
                    toast = toast,
                )
            }
            Composer(m, toast) { composerH = it }
            AnimatedVisibility(
                jump,
                Modifier.align(Alignment.BottomCenter).offset { IntOffset(0, -(composerH + 12.dp.roundToPx())) },
                enter = if (shell.calm) fadeIn(tween(Motion.M1)) else scaleIn(spring(0.6f, 600f), 0.6f) + fadeIn(tween(Motion.M1)),
                exit = if (shell.calm) fadeOut(tween(90)) else scaleOut(tween(Motion.M1), 0.8f) + fadeOut(tween(90)),
            ) {
                Box(Modifier.size(40.dp).glass(shell.cardHaze, CircleShape)) {
                    IconBtn(Glyph.Down, { scope.launch { front.value?.animateScrollToItem(0) } }, fill = Color.Transparent, island = true)
                }
            }
            ToastIsland(toast) { composerH + (if (jump) 64.dp else 12.dp).roundToPx() }
        }
    }
}

private fun AnimatedContentTransitionScope<Pair<Int, String?>>.sceneChange(calm: Boolean, rise: Int): ContentTransform = when {
    calm -> fadeIn(tween(Motion.M2)) togetherWith fadeOut(tween(Motion.M2))
    targetState.second == null ->
        (fadeIn(tween(Motion.M3, delayMillis = 100)) + slideInVertically(spring(0.9f, 380f)) { it / 8 }) togetherWith
            (scaleOut(tween(Motion.M3, easing = Motion.Leave), 0.94f, TransformOrigin(0.5f, 0.35f)) + fadeOut(tween(Motion.M2)))
    else -> (fadeIn(tween(Motion.M2, delayMillis = 90)) + slideInVertically(spring(0.9f, 380f)) { rise }) togetherWith fadeOut(tween(Motion.M1))
}.using(null)

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedContentScope.Scene(
    m: AppModel,
    chatId: String?,
    live: Boolean,
    blurOut: Boolean,
    front: MutableState<LazyListState?>,
    composerH: () -> Int,
    toast: Toast,
) {
    val shell = LocalShell.current
    val d = LocalDensity.current
    val chat by remember(chatId) { derivedStateOf { m.chats.firstOrNull { it.id == chatId } } }
    val items = remember(chat) { chat?.messages.orEmpty().asReversed() }
    val seen = remember { items.mapTo(HashSet()) { it.id } }
    val list = rememberLazyListState()
    if (live) SideEffect { front.value = list }
    val shown = transition.animateFloat({ tween(Motion.M3, easing = Motion.Leave) }, "shown") { if (it == EnterExitState.Visible) 1f else 0f }
    val lift = with(d) { 12.dp.roundToPx() }
    val calm = shell.calm

    Box(
        Modifier.fillMaxSize().graphicsLayer {
            if (blurOut && Build.VERSION.SDK_INT >= 31) {
                val r = (1f - shown.value) * 14f
                renderEffect = if (r > 0.5f) BlurEffect(r, r) else null
            }
        },
    ) {
        AnimatedContent(
            items.isNotEmpty(),
            Modifier.fillMaxSize(),
            transitionSpec = {
                val change = if (calm) {
                    fadeIn(tween(Motion.M2)) togetherWith fadeOut(tween(Motion.M2))
                } else {
                    EnterTransition.None togetherWith
                        (fadeOut(tween(140, easing = Motion.Leave)) + slideOutVertically { -lift } + scaleOut(targetScale = 0.98f))
                }
                change using null
            },
            label = "fresh",
        ) { has ->
            if (has) {
                Messages(m, chat, items, seen, list, live, composerH, toast)
            } else {
                val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                Greeting(m, Modifier.fillMaxSize().padding(top = top + Chrome.Top, bottom = with(d) { composerH().toDp() }))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Messages(
    m: AppModel,
    chat: Chat?,
    items: List<Message>,
    seen: HashSet<String>,
    list: LazyListState,
    source: Boolean,
    composerH: () -> Int,
    toast: Toast,
) {
    val shell = LocalShell.current
    val d = LocalDensity.current
    val calm = shell.calm
    val liveId by remember { derivedStateOf { m.live?.messageId } }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val typing = with(d) { (WindowInsets.imeAnimationTarget.getBottom(d) - WindowInsets.navigationBars.getBottom(d)).coerceAtLeast(0).toDp() }
    val rise = with(d) { 56.dp.roundToPx() }
    val near = with(d) { 48.dp.toPx() }
    val atBottom = remember(list) { { list.firstVisibleItemIndex == 0 } }

    // New messages land below the item the list is anchored to; follow them when the reader was at the end.
    val count = remember { intArrayOf(items.size) }
    LaunchedEffect(items.size) {
        val added = items.size - count[0]
        count[0] = items.size
        if (added <= 0) return@LaunchedEffect
        // Your own message always brings you down to it; others follow only if you were already there.
        if (items.take(added).any { it.role == "user" }) list.animateScrollToItem(0)
        else if (list.firstVisibleItemIndex == added && list.firstVisibleItemScrollOffset < near) list.scrollToItem(0)
    }

    LazyColumn(
        Modifier.fillMaxSize().then(if (source) Modifier.glassSource(shell.cardHaze) else Modifier),
        state = list,
        reverseLayout = true,
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 16.dp,
            top = top + Chrome.Top + 8.dp + typing,
            bottom = with(d) { composerH().toDp() } + 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        itemsIndexed(items, key = { _, msg -> msg.id }) { i, msg ->
            val user = msg.role == "user"
            // Only a message that arrives while the chat is open animates in; scrolling never pays for it.
            val fresh = remember { seen.add(msg.id) }
            if (!fresh) {
                if (user) UserBubble(msg, toast) else Answer(m, chat, msg, msg.id == liveId, i == 0, atBottom, toast)
            } else AnimatedVisibility(
                visibleState = remember { MutableTransitionState(false).apply { targetState = true } },
                enter = when {
                    calm -> fadeIn(tween(Motion.M2))
                    user -> slideInVertically(spring(0.82f, 420f)) { rise } +
                        scaleIn(spring(0.82f, 420f), 0.94f, TransformOrigin(1f, 1f)) +
                        fadeIn(tween(Motion.M2, easing = Motion.Out))
                    else -> fadeIn(tween(Motion.M3))
                },
            ) {
                if (user) UserBubble(msg, toast) else Answer(m, chat, msg, msg.id == liveId, i == 0, atBottom, toast)
            }
        }
    }
}

private val Bubble = Squircle(22.dp)

@Composable
private fun UserBubble(msg: Message, toast: Toast) {
    val clip = LocalClipboardManager.current
    val files = remember(msg) { msg.raw.optJSONArray("files").let { a -> if (a == null) emptyList() else (0 until a.length()).mapNotNull { a.optJSONObject(it) } } }
    Column(Modifier.fillMaxWidth().padding(start = 48.dp), horizontalAlignment = Alignment.End) {
        if (files.isNotEmpty()) SentFiles(files)
        if (msg.text.isNotBlank()) BasicText(
            msg.text,
            Modifier.pressable(
                {}, Bubble, Tone.Raise, Tone.Hover, depth = 1f, feel = null,
                onLong = {
                    clip.setText(AnnotatedString(msg.text))
                    toast.show(tr("Copied", "Skopiowano"))
                },
            )
                .padding(horizontal = 18.dp, vertical = 12.dp),
            style = Type.Body,
        )
    }
}

@Immutable
private data class Think(val has: Boolean, val active: Boolean, val since: Long, val textAt: Long)

/** What the tools are doing for the live answer: the step now, and the pages seen so far. */
@Immutable
private data class ToolNow(val step: String?, val links: List<Pair<String, String>>)

@Composable
private fun Answer(m: AppModel, chat: Chat?, msg: Message, isLive: Boolean, last: Boolean, atBottom: () -> Boolean, toast: Toast) {
    val id = msg.id
    val current = rememberUpdatedState(msg)
    var revealing by remember { mutableStateOf(isLive) }
    LaunchedEffect(isLive) { if (isLive) revealing = true }
    val sentHere = remember { isLive }
    val think by remember {
        derivedStateOf {
            m.live?.takeIf { it.messageId == id }?.let {
                Think(it.thought.isNotBlank(), it.thought.isNotBlank() && it.text.isEmpty() && it.step == null, it.thoughtAt, it.textAt)
            }
        }
    }
    // Between the stream closing and the chat taking the final text, the last live picture holds.
    val held = remember { arrayOfNulls<Think>(1) }
    val t = think?.also { held[0] = it } ?: held[0].takeIf { revealing }
    val thought = remember { { m.live?.takeIf { it.messageId == id }?.thought ?: current.value.thought } }
    val tool by remember { derivedStateOf { m.live?.takeIf { it.messageId == id }?.let { ToolNow(it.step, it.links) } } }
    val steps = remember(msg) { msg.steps }
    val sources = remember(msg) { msg.sources }
    val tooled = remember { booleanArrayOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        if (m.showThinking && (t?.has ?: msg.thought.isNotBlank())) {
            Thinking(
                thought,
                active = t?.active == true,
                since = t?.since ?: 0L,
                tookMs = if (t != null && t.textAt > 0) t.textAt - t.since else msg.raw.optLong("thoughtMs"),
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        val now = tool
        // Once a tool has shown, its row stays until the answer settles, so it never blinks between steps.
        if (now != null && (now.step != null || now.links.isNotEmpty())) tooled[0] = true
        if (now != null && tooled[0] || steps.isNotEmpty()) {
            ToolTrail(now, steps, sources.size, Modifier.padding(bottom = 10.dp))
        }
        if (revealing) {
            LiveAnswer(m, id, current, atBottom) { revealing = false }
        } else if (msg.text.isNotEmpty()) {
            Markdown(msg.text, sources = sources, selectable = selecting)
        }
        msg.error?.let { ErrorBanner(m, it, last, sentHere) }
        AnimatedVisibility(
            !revealing && sources.isNotEmpty(),
            enter = fadeIn(tween(Motion.M3)) + expandVertically(spring(1f, 500f)),
            exit = fadeOut(tween(Motion.M1)) + shrinkVertically(tween(Motion.M1)),
        ) {
            SourceChips(sources)
        }
        AnimatedVisibility(
            !revealing,
            enter = fadeIn(tween(Motion.M3)) + expandVertically(spring(1f, 500f)),
            exit = fadeOut(tween(Motion.M1)) + shrinkVertically(tween(Motion.M1)),
        ) {
            Actions(
                m, chat, msg, last, toast,
                selecting = selecting,
                onSelect = { on ->
                    selecting = on
                    if (on) toast.show(tr("Hold a word, then drag to select", "Przytrzymaj słowo i przeciągnij"))
                },
                info = info,
                onInfo = { info = it },
            )
        }
    }
}

private fun wordEnd(t: String, i: Int): Int {
    var j = i.coerceIn(0, t.length)
    while (j < t.length && !t[j].isWhitespace()) j++
    return j
}

/**
 * The answer being written. The reveal trails the stream by a little so words settle in like ink
 * and the block never jumps; the text below it recomposes once per word, the rest is drawn.
 */
@Composable
private fun LiveAnswer(m: AppModel, id: String, msg: State<Message>, atBottom: () -> Boolean, onDone: () -> Unit) {
    val calm = LocalShell.current.calm
    val view = LocalView.current
    val held = remember { arrayOf("") }
    val text = remember {
        {
            val l = m.live
            when {
                l != null && l.messageId == id -> l.text.also { held[0] = it }
                msg.value.raw.optBoolean("streaming") -> held[0]
                else -> msg.value.text
            }
        }
    }
    val streaming = remember { { m.live?.messageId == id || msg.value.raw.optBoolean("streaming") } }
    var at by remember { mutableFloatStateOf(max(0, text().length - 60).toFloat()) }
    val caret = remember { Animatable(1f) }
    val cut by remember { derivedStateOf { wordEnd(text(), at.toInt()) } }
    val shown by remember { derivedStateOf { text().let { it.substring(0, min(cut, it.length)) } } }
    val start by remember { derivedStateOf { shown.indexOfLast { it.isWhitespace() } + 1 } }
    val queued by remember { derivedStateOf { m.live?.takeIf { it.messageId == id }?.queued ?: 0 } }
    // The thinking block or a tool line already says it is working; the caret would say it twice.
    val busy by remember {
        derivedStateOf { m.live?.takeIf { it.messageId == id }?.let { it.step != null || (m.showThinking && it.thought.isNotBlank()) } == true }
    }
    val edge = remember(calm) {
        if (calm) {
            { 1f }
        } else {
            { if (cut <= start) 1f else (at - start) / (cut - start) }
        }
    }

    LaunchedEffect(Unit) {
        if (text().isEmpty()) launch {
            snapshotFlow { text().isNotEmpty() }.first { it }
            if (atBottom()) view.feel(Feel.Tick)
        }
        var last = 0L
        while (true) {
            val now = withFrameNanos { it }
            val dt = if (last == 0L) 1f / 60f else ((now - last) / 1e9f).coerceIn(0f, 0.1f)
            last = now
            val target = text().length.toFloat()
            val backlog = target - at
            at = when {
                calm -> target
                backlog > 600f -> target - 60f
                else -> min(target, at + max(48f * dt, backlog * (1f - exp(-dt / 0.09f))))
            }
            if (at >= target) {
                if (!streaming()) break
                snapshotFlow { text().length > at || !streaming() }.first { it }
                last = 0L
            }
        }
        if (!calm) caret.animateTo(0f, tween(220, easing = Motion.Out))
        onDone()
    }

    when {
        shown.isNotEmpty() -> Markdown(shown, edge = edge, caret = { caret.value })
        queued > 0 || !busy -> Waiting(queued, calm)
    }
}

@Composable
private fun Waiting(queued: Int, calm: Boolean) {
    if (queued > 0) {
        val meta = Type.Meta
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(tr("Waiting for ", "W kolejce: "), style = meta)
            Roll("$queued", meta, digits = true)
            BasicText(tr(" other answer(s)…", ""), style = meta)
        }
        return
    }
    val accent = LocalAccent.current
    val breath = if (calm) null else rememberInfiniteTransition(label = "caret").animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(1100, easing = Motion.Standard), RepeatMode.Reverse), label = "breath",
    )
    Box(
        Modifier.padding(vertical = 2.dp).size(2.dp, 18.dp)
            .graphicsLayer { alpha = breath?.value ?: 1f }
            .background(accent, Squircle(1.dp)),
    )
}

/** Text that settles to its new value without overshoot: the old line leaves up, the new rises in. */
private fun <S> AnimatedContentTransitionScope<S>.textRoll(calm: Boolean): ContentTransform =
    if (calm) {
        fadeIn(tween(120)) togetherWith fadeOut(tween(90))
    } else {
        (slideInVertically(Motion.calmText()) { it * 6 / 10 } + fadeIn(tween(Motion.M2, easing = Motion.Out))) togetherWith
            (slideOutVertically(tween(Motion.M1, easing = Motion.Leave)) { -it / 2 } + fadeOut(tween(90)))
    }

@Composable
private fun Thinking(thought: () -> String, active: Boolean, since: Long, tookMs: Long, modifier: Modifier = Modifier) {
    val calm = LocalShell.current.calm
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(active) { if (active) open = false }
    val turn = animateFloatAsState(if (open && !active) 90f else 0f, spring(0.8f, 500f), label = "chevron")
    var secs by remember { mutableIntStateOf(0) }
    LaunchedEffect(active, since) {
        while (active && since > 0) {
            val gone = System.currentTimeMillis() - since
            secs = (gone / 1000).toInt()
            delay(1000 - gone % 1000)
        }
    }
    val done = if (tookMs > 0) {
        val n = max(1L, (tookMs + 500) / 1000)
        tr("Thought for $n s", "Myślał przez $n s")
    } else {
        tr("Thought it through", "Przemyślane")
    }

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.pressable({ if (!active) open = !open }, depth = 0.98f, feel = if (active) null else Feel.Tick)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedContent(active, transitionSpec = { textRoll(calm) using SizeTransform(clip = false) }, label = "think") { a ->
                if (a) Shimmer(if (secs > 0) tr("Thinking · $secs s", "Myśli · $secs s") else tr("Thinking", "Myśli"), calm)
                else BasicText(done, style = Type.Body.copy(color = Ink.Three), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            AnimatedVisibility(!active, enter = fadeIn(tween(Motion.M2)) + scaleIn(spring(0.6f, 600f), 0.6f), exit = fadeOut(tween(Motion.M1))) {
                Icon(Glyph.Right, Ink.Three, Modifier.padding(start = 4.dp).graphicsLayer { rotationZ = turn.value }, size = 14.dp)
            }
        }
        AnimatedVisibility(
            active,
            enter = fadeIn(tween(Motion.M2)) + expandVertically(spring(1f, 420f, IntSize.VisibilityThreshold)),
            exit = if (calm) fadeOut(tween(Motion.M1)) else shrinkVertically(spring(1f, 380f, IntSize.VisibilityThreshold)) + fadeOut(tween(Motion.M2)),
        ) {
            ThoughtWindow(thought, calm)
        }
        AnimatedVisibility(
            open && !active,
            enter = if (calm) fadeIn(tween(Motion.M2)) else expandVertically(spring(1f, 450f, IntSize.VisibilityThreshold)) + fadeIn(tween(Motion.M2, delayMillis = 60)),
            exit = if (calm) fadeOut(tween(Motion.M2)) else shrinkVertically(spring(1f, 500f, IntSize.VisibilityThreshold)) + fadeOut(tween(Motion.M1)),
        ) {
            Row(Modifier.padding(top = 6.dp, bottom = 4.dp).height(IntrinsicSize.Min)) {
                Box(Modifier.width(2.dp).fillMaxHeight().background(Tone.Raise, CircleShape))
                Spacer(Modifier.width(12.dp))
                BasicText(thought().trim(), style = Type.Meta.copy(color = Ink.Three))
            }
        }
    }
}

/** A label with light passing through it from left to right, the way a phone says it is working. */
@Composable
private fun Shimmer(text: String, calm: Boolean, style: TextStyle = Type.Body) {
    if (calm) {
        BasicText(text, style = style.copy(color = Ink.Two), maxLines = 1, overflow = TextOverflow.Ellipsis)
        return
    }
    val sweep = rememberInfiniteTransition(label = "shimmer").animateFloat(
        -0.6f, 1.6f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "sweep",
    )
    BasicText(
        text,
        Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
            drawContent()
            val w = size.width
            val x = sweep.value * w
            drawRect(
                Brush.linearGradient(
                    0f to Color.Transparent, 0.5f to Ink.One, 1f to Color.Transparent,
                    start = Offset(x - w * 0.35f, 0f), end = Offset(x + w * 0.35f, 0f),
                ),
                blendMode = BlendMode.SrcAtop,
            )
        },
        style = style.copy(color = Ink.Four),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private const val ThoughtRows = 5

/**
 * The thought from about 900 characters back, cut where a line or word starts. The cut moves only
 * every 900 characters, so the lines on screen keep their wrapping while new ones arrive.
 */
private fun thoughtTail(t: String): String {
    val cut = max(0, (t.length - 900) / 900 * 900)
    if (cut == 0) return t.trim()
    val nl = t.indexOf('\n', cut).takeIf { it >= 0 && it < cut + 300 }
    val at = nl?.plus(1) ?: (t.indexOf(' ', cut).takeIf { it >= 0 }?.plus(1) ?: cut)
    return t.substring(at).trim()
}

/**
 * The latest lines of a thought as it arrives. The window grows a line at a time up to
 * [ThoughtRows], then each new line pushes the others up and out under a soft fade. Height and
 * scroll are animated in layout, so the words recompose only when the thought changes.
 */
@Composable
private fun ThoughtWindow(thought: () -> String, calm: Boolean) {
    val style = Type.Meta.copy(color = Ink.Three)
    val line = with(LocalDensity.current) { style.lineHeight.toPx() }
    val scope = rememberCoroutineScope()
    val height = remember { Animatable(0f) }
    val slide = remember { Animatable(0f) }
    var lines by remember { mutableIntStateOf(0) }
    val tail by remember { derivedStateOf { thoughtTail(thought()) } }
    val glide = spring(1f, 320f, 0.5f)

    BasicText(
        tail,
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp)
            .clipToBounds()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                if (lines >= ThoughtRows) {
                    val h = 16.dp.toPx()
                    drawRect(Brush.verticalGradient(0f to Color.Black, 1f to Color.Transparent, endY = h), size = Size(size.width, h), blendMode = BlendMode.DstOut)
                }
            }
            .layout { measurable, c ->
                val p = measurable.measure(c.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                val h = height.value.roundToInt().coerceIn(c.minHeight, min(c.maxHeight, p.height))
                layout(p.width, h) { p.place(0, h - p.height + slide.value.roundToInt()) }
            },
        style = style,
        onTextLayout = { r ->
            val n = r.lineCount
            val was = lines
            lines = n
            val target = min(n, ThoughtRows) * line
            if (calm || was == 0 || n < was) {
                scope.launch {
                    slide.snapTo(0f)
                    height.snapTo(target)
                }
            } else if (n > was) {
                scope.launch {
                    slide.snapTo(slide.value + (n - was) * line)
                    slide.animateTo(0f, glide)
                }
                if (height.targetValue != target) scope.launch { height.animateTo(target, glide) }
            }
        },
    )
}

private fun toolGlyph(tool: String) = if (tool == "web_search" || tool == "read_page") Glyph.Globe else Glyph.Models

/** One line for what the tools did, e.g. "Searched the web · 4 sources". */
private fun toolLabel(steps: List<Step>, sources: Int, pages: Int): String {
    val searched = steps.count { it.tool == "web_search" }
    val read = steps.count { it.tool == "read_page" }
    val used = steps.filter { toolGlyph(it.tool) != Glyph.Globe }
    val what = when {
        searched > 0 || steps.isEmpty() -> tr("Searched the web", "Przeszukano internet")
        read == 1 -> tr("Read a page", "Przeczytano stronę")
        read > 1 -> tr("Read $read pages", "Przeczytano strony: $read")
        used.size == 1 -> tr("Used ${used[0].label}", "Użyto: ${used[0].label}")
        else -> tr("Used ${used.size} tools", "Użyto narzędzi: ${used.size}")
    }
    return when {
        sources > 0 -> what + " · " + if (sources == 1) tr("1 source", "1 źródło") else tr("$sources sources", "źródła: $sources")
        pages > 0 -> what + " · " + if (pages == 1) tr("1 page", "1 strona") else tr("$pages pages", "strony: $pages")
        else -> what
    }
}

/**
 * What the tools did for an answer. While one works: its shimmering line and the pages arriving
 * under it. After: one quiet line that opens into the steps.
 */
@Composable
private fun ToolTrail(now: ToolNow?, steps: List<Step>, sources: Int, modifier: Modifier = Modifier) {
    val calm = LocalShell.current.calm
    var open by remember { mutableStateOf(false) }
    val turn = animateFloatAsState(if (open) 90f else 0f, spring(0.8f, 500f), label = "chevron")
    // Leaving content still composes once more after the live answer is gone; it keeps what it showed.
    val lastStep = remember { arrayOf("") }
    val lastLinks = remember { arrayOf(emptyList<Pair<String, String>>()) }
    now?.step?.let { lastStep[0] = it }
    now?.links?.takeIf { it.isNotEmpty() }?.let { lastLinks[0] = it }
    val working = now?.step != null

    Column(modifier.fillMaxWidth()) {
        AnimatedContent(working, transitionSpec = { textRoll(calm) using SizeTransform(clip = false) }, label = "tools") { w ->
            if (w) {
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    val step = lastStep[0]
                    Icon(if (step.startsWith("Using")) Glyph.Models else Glyph.Globe, Ink.Three, size = 16.dp)
                    Spacer(Modifier.width(8.dp))
                    AnimatedContent(step, transitionSpec = { textRoll(calm) using SizeTransform(clip = false) }, label = "step") { s ->
                        Shimmer(s, calm)
                    }
                }
            } else {
                val plugin = lastStep[0].startsWith("Using")
                val glyph = steps.firstOrNull()?.let { toolGlyph(it.tool) } ?: if (plugin) Glyph.Models else Glyph.Globe
                val label = if (steps.isEmpty() && lastLinks[0].isEmpty() && plugin) lastStep[0].replaceFirst("Using", "Used") else toolLabel(steps, sources, lastLinks[0].size)
                Row(
                    Modifier.pressable({ if (steps.isNotEmpty()) open = !open }, depth = 0.98f, feel = if (steps.isEmpty()) null else Feel.Tick)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(glyph, Ink.Three, size = 16.dp)
                    Spacer(Modifier.width(8.dp))
                    Roll(label, Type.Body.copy(color = Ink.Three))
                    if (steps.isNotEmpty()) Icon(Glyph.Right, Ink.Three, Modifier.padding(start = 4.dp).graphicsLayer { rotationZ = turn.value }, size = 14.dp)
                }
            }
        }
        AnimatedVisibility(
            working && lastLinks[0].isNotEmpty(),
            enter = fadeIn(tween(Motion.M2)) + expandVertically(spring(1f, 500f, IntSize.VisibilityThreshold)),
            exit = fadeOut(tween(Motion.M1)) + shrinkVertically(spring(1f, 500f, IntSize.VisibilityThreshold)),
        ) {
            LinkChips(lastLinks[0], calm)
        }
        AnimatedVisibility(
            open && steps.isNotEmpty(),
            enter = if (calm) fadeIn(tween(Motion.M2)) else expandVertically(spring(1f, 450f, IntSize.VisibilityThreshold)) + fadeIn(tween(Motion.M2, delayMillis = 60)),
            exit = if (calm) fadeOut(tween(Motion.M2)) else shrinkVertically(spring(1f, 500f, IntSize.VisibilityThreshold)) + fadeOut(tween(Motion.M1)),
        ) {
            Row(Modifier.padding(top = 6.dp, bottom = 4.dp).height(IntrinsicSize.Min)) {
                Box(Modifier.width(2.dp).fillMaxHeight().background(Tone.Raise, CircleShape))
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    steps.forEach { s -> StepRow(s) }
                }
            }
        }
    }
}

@Composable
private fun StepRow(s: Step) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(if (s.ok) toolGlyph(s.tool) else Glyph.Alert, if (s.ok) Ink.Three else Ink.Four, size = 14.dp)
        Spacer(Modifier.width(8.dp))
        BasicText(s.label, style = Type.Meta.copy(color = Ink.Two), maxLines = 1)
        s.detail?.let { d ->
            BasicText(
                "  " + if (d.startsWith("http")) Tools.host(d) else "“$d”",
                Modifier.weight(1f, fill = false),
                style = Type.Meta.copy(color = Ink.Four),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The pages a search turned up, each arriving as a small chip; the row keeps the newest in view. */
@Composable
private fun LinkChips(links: List<Pair<String, String>>, calm: Boolean) {
    val scroll = rememberScrollState()
    LaunchedEffect(Unit) {
        snapshotFlow { scroll.maxValue }.collectLatest { if (calm) scroll.scrollTo(it) else scroll.animateScrollTo(it, spring(1f, 300f)) }
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp).horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        links.forEach { (_, url) ->
            key(url) {
                val a = remember { Animatable(if (calm) 1f else 0f) }
                LaunchedEffect(Unit) { a.animateTo(1f, spring(0.75f, 520f)) }
                BasicText(
                    Tools.host(url),
                    Modifier.graphicsLayer {
                        alpha = a.value.coerceIn(0f, 1f)
                        scaleX = 0.8f + 0.2f * a.value
                        scaleY = scaleX
                    }
                        .background(Tone.Raise, Chip)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .widthIn(max = 160.dp),
                    style = Type.Label.copy(color = Ink.Two),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val Chip = Squircle(13.dp)

/** The pages the answer cites, numbered as its [n]; a tap opens one in the browser. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SourceChips(sources: List<Pair<String, String>>) {
    val uri = LocalUriHandler.current
    val accent = LocalAccent.current
    FlowRow(
        Modifier.fillMaxWidth().padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        sources.forEachIndexed { i, (_, url) ->
            Row(
                Modifier.height(30.dp)
                    .pressable({ runCatching { uri.openUri(url) } }, Squircle(15.dp), Tone.Raise, Tone.Press, depth = 0.95f, stretch = true)
                    .padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText("${i + 1}", style = Type.Label.copy(color = accent, fontFeatureSettings = "tnum"))
                Spacer(Modifier.width(6.dp))
                BasicText(Tools.host(url), Modifier.widthIn(max = 180.dp), style = Type.Label.copy(color = Ink.Two), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ErrorBanner(m: AppModel, text: String, last: Boolean, fresh: Boolean) {
    val view = LocalView.current
    val shown = remember { MutableTransitionState(!fresh).apply { targetState = true } }
    LaunchedEffect(Unit) { if (fresh) view.feel(Feel.Reject) }
    AnimatedVisibility(shown, enter = expandVertically(tween(Motion.M2, easing = Motion.Out)) + fadeIn()) {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp).background(Tone.Panel, Shapes.Tile).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Glyph.Alert, Signal.Err, size = 18.dp)
            Spacer(Modifier.width(8.dp))
            BasicText(text, Modifier.weight(1f), style = Type.Body)
            if (last) {
                Spacer(Modifier.width(8.dp))
                Btn(tr("Try again", "Spróbuj ponownie"), { m.retry() }, variant = Variant.Quiet, height = 34.dp)
            }
        }
    }
}

@Composable
private fun Actions(
    m: AppModel,
    chat: Chat?,
    msg: Message,
    last: Boolean,
    toast: Toast,
    selecting: Boolean,
    onSelect: (Boolean) -> Unit,
    info: Boolean,
    onInfo: (Boolean) -> Unit,
) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    var menu by remember { mutableStateOf(false) }
    val words = msg.text.isNotBlank()
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        // The icons line up with the text above them, not their touch targets.
        Row(Modifier.fillMaxWidth().offset(x = (-9).dp), verticalAlignment = Alignment.CenterVertically) {
            if (words) {
                CopyButton({ msg.text })
                ActionBtn({ share(ctx, plainText(msg.text)) }) { Drawn(DrawScope::shareMark) }
            }
            if (last) RetryButton(m)
            Box {
                ActionBtn({ menu = true }) { Icon(Glyph.More, Ink.Three, size = 18.dp) }
                MoreMenu(
                    menu,
                    { menu = false },
                    listOfNotNull(
                        if (words) MenuRow(tr("Copy as plain text", "Kopiuj jako zwykły tekst"), { Icon(Glyph.Copy, Ink.One, size = 18.dp) }) {
                            clip.setText(AnnotatedString(plainText(msg.text)))
                            toast.show(tr("Copied", "Skopiowano"))
                        } else null,
                        if (words) MenuRow(
                            if (selecting) tr("Stop selecting", "Zakończ zaznaczanie") else tr("Select text", "Zaznacz tekst"),
                            { Drawn(DrawScope::cursorMark, Ink.One) },
                        ) { onSelect(!selecting) } else null,
                        MenuRow(if (info) tr("Hide details", "Ukryj szczegóły") else tr("Details", "Szczegóły"), { Icon(Glyph.Info, Ink.One, size = 18.dp) }) {
                            onInfo(!info)
                        },
                    ),
                )
            }
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(selecting, enter = fadeIn(tween(Motion.M2)) + scaleIn(spring(0.7f, 600f), 0.8f), exit = fadeOut(tween(Motion.M1))) {
                Btn(tr("Done", "Gotowe"), { onSelect(false) }, Modifier.offset(x = 9.dp), Variant.Quiet, height = 32.dp)
            }
        }
        AnimatedVisibility(
            info,
            enter = fadeIn(tween(Motion.M2)) + expandVertically(spring(1f, 450f, IntSize.VisibilityThreshold)),
            exit = fadeOut(tween(Motion.M1)) + shrinkVertically(spring(1f, 500f, IntSize.VisibilityThreshold)),
        ) {
            AnswerInfo(chat, msg)
        }
    }
}

@Composable
private fun ActionBtn(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(36.dp).pressable(onClick, CircleShape, Color.Transparent, Tone.Raise, depth = 0.88f, stretch = true),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private fun share(ctx: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    runCatching { ctx.startActivity(Intent.createChooser(send, null)) }
}

/** The answer's receipt in words: which model, how fast, how much, how long. */
@Composable
private fun AnswerInfo(chat: Chat?, msg: Message) {
    val r = msg.receipt
    val model = msg.raw.optJSONObject("meta")?.optString("model")?.ifBlank { null }
        ?: chat?.raw?.optString("modelName")?.takeIf { it.isNotBlank() && it != "null" }
    val speed = r?.optDouble("decode_tok_s")?.takeIf { !it.isNaN() && it > 0 }
    val written = r?.optLong("generated_tokens") ?: 0L
    val read = r?.optLong("prompt_tokens") ?: 0L
    val ms = r?.optLong("total_ms") ?: 0L
    val rows = listOfNotNull(
        model?.let { tr("Model", "Model") to it },
        speed?.let { tr("Speed", "Szybkość") to "%.1f tokens/s".format(it) },
        written.takeIf { it > 0 }?.let { tr("Tokens", "Tokeny") to "%,d written".format(it) + if (read > 0) ", %,d read".format(read) else "" },
        ms.takeIf { it > 0 }?.let { tr("Time", "Czas") to if (it < 10_000) "%.1f s".format(it / 1000.0) else "${(it + 500) / 1000} s" },
    )
    Column(
        Modifier.fillMaxWidth().padding(top = 6.dp).background(Tone.Panel, Shapes.Tile).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (rows.isEmpty()) BasicText(tr("No details were kept for this answer.", "Brak szczegółów tej odpowiedzi."), style = Type.Meta)
        rows.forEach { (k, v) ->
            Row {
                BasicText(k, Modifier.width(72.dp), style = Type.Meta)
                BasicText(v, style = Type.Meta.copy(color = Ink.One, fontFeatureSettings = "tnum"), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private class MenuRow(val label: String, val icon: @Composable () -> Unit, val go: () -> Unit)

/** Opens above its button when there is room, below it when not. */
private class MenuPlacement(private val gap: Int, private val clear: Int, private val up: MutableState<Boolean>) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val above = anchorBounds.top - popupContentSize.height - gap
        up.value = above >= clear
        val x = anchorBounds.left.coerceAtMost(windowSize.width - popupContentSize.width - gap * 2).coerceAtLeast(gap * 2)
        return IntOffset(x, if (up.value) above else anchorBounds.bottom + gap)
    }
}

/** A small island that grows out of the three dots, its rows following one after another. */
@Composable
private fun MoreMenu(open: Boolean, onDismiss: () -> Unit, rows: List<MenuRow>) {
    val calm = LocalShell.current.calm
    val state = remember { MutableTransitionState(false) }
    state.targetState = open
    if (!state.currentState && !state.targetState && state.isIdle) return
    val d = LocalDensity.current
    val up = remember { mutableStateOf(true) }
    val place = remember(d) { MenuPlacement(with(d) { 6.dp.roundToPx() }, with(d) { 120.dp.roundToPx() }, up) }
    val origin = TransformOrigin(0.12f, if (up.value) 1f else 0f)
    Popup(popupPositionProvider = place, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        AnimatedVisibility(
            state,
            enter = if (calm) fadeIn(tween(Motion.M2)) else fadeIn(tween(Motion.M1)) + scaleIn(spring(0.72f, 520f), 0.3f, origin),
            exit = if (calm) fadeOut(tween(Motion.M1)) else fadeOut(tween(Motion.M1)) + scaleOut(tween(Motion.M2, easing = Motion.Leave), 0.5f, origin),
        ) {
            Column(
                Modifier.widthIn(min = 220.dp, max = 280.dp).background(Tone.Float, Shapes.Card).padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                rows.forEachIndexed { i, row ->
                    Row(
                        Modifier.fillMaxWidth()
                            .animateEnterExit(
                                enter = if (calm) EnterTransition.None else fadeIn(tween(Motion.M2, delayMillis = 30 + 35 * i)) +
                                    slideInVertically(spring(1f, 420f)) { it / 3 },
                            )
                            .heightIn(min = 48.dp)
                            .pressable(
                                {
                                    onDismiss()
                                    row.go()
                                },
                                Shapes.ControlLg, Tone.Float, Tone.Raise, depth = 0.97f,
                            )
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        row.icon()
                        Spacer(Modifier.width(12.dp))
                        BasicText(row.label, style = Type.Body.copy(fontWeight = FontWeight.Medium))
                    }
                }
            }
        }
    }
}

/** An 18dp mark drawn in place, for meanings the icon set has no glyph for. */
@Composable
private fun Drawn(mark: DrawScope.(Color) -> Unit, tint: Color = Ink.Three) {
    Box(Modifier.size(18.dp).drawBehind { mark(tint) })
}

/** A 24-unit grid scaled to the box, stroked like the bold glyphs. */
private fun DrawScope.strokeMark(c: Color, build: Path.(Float) -> Unit) {
    val u = size.minDimension / 24f
    drawPath(Path().apply { build(u) }, c, style = Stroke(2.2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Share: an arrow leaving an open tray. */
private fun DrawScope.shareMark(c: Color) = strokeMark(c) { u ->
    moveTo(12f * u, 14f * u)
    lineTo(12f * u, 3f * u)
    moveTo(8f * u, 7f * u)
    lineTo(12f * u, 3f * u)
    lineTo(16f * u, 7f * u)
    moveTo(8.5f * u, 10f * u)
    lineTo(5f * u, 10f * u)
    lineTo(5f * u, 20.5f * u)
    lineTo(19f * u, 20.5f * u)
    lineTo(19f * u, 10f * u)
    lineTo(15.5f * u, 10f * u)
}

/** Select: a text cursor. */
private fun DrawScope.cursorMark(c: Color) = strokeMark(c) { u ->
    moveTo(9f * u, 4f * u)
    quadraticTo(12f * u, 4f * u, 12f * u, 7f * u)
    lineTo(12f * u, 17f * u)
    quadraticTo(12f * u, 20f * u, 9f * u, 20f * u)
    moveTo(15f * u, 4f * u)
    quadraticTo(12f * u, 4f * u, 12f * u, 7f * u)
    moveTo(15f * u, 20f * u)
    quadraticTo(12f * u, 20f * u, 12f * u, 17f * u)
    moveTo(9.5f * u, 12f * u)
    lineTo(14.5f * u, 12f * u)
}

@Composable
private fun RetryButton(m: AppModel) {
    val calm = LocalShell.current.calm
    val scope = rememberCoroutineScope()
    val spin = remember { Animatable(0f) }
    ActionBtn({
        if (!spin.isRunning) scope.launch {
            if (!calm) {
                spin.snapTo(0f)
                spin.animateTo(-360f, spring(0.8f, 300f, visibilityThreshold = 1f))
            }
            m.retry()
        }
    }) {
        Icon(Glyph.Refresh, Ink.Three, Modifier.graphicsLayer { rotationZ = spin.value }, size = 18.dp)
    }
}

@Composable
private fun BoxScope.Composer(m: AppModel, toast: Toast, onHeight: (Int) -> Unit) {
    val shell = LocalShell.current
    val calm = shell.calm
    var draft by rememberSaveable(m.current) { mutableStateOf("") }
    val attached = remember(m.current) { mutableStateListOf<Attachment>() }
    var adding by remember(m.current) { mutableStateOf(false) }
    var lines by remember { mutableIntStateOf(1) }
    val app = LocalContext.current
    val scope = rememberCoroutineScope()
    val take: (List<Uri>) -> Unit = { uris ->
        scope.launch {
            for (u in uris) {
                Attachment.read(app, u) { name -> toast.show(tr("$name is not a picture or a text file", "$name to nie zdjęcie ani plik tekstowy")) }
                    ?.let { a -> if (attached.none { it.name == a.name && it.bytes == a.bytes }) attached += a }
            }
        }
    }
    val pickers = rememberPickers(take)
    val empty by remember { derivedStateOf { (m.chat?.raw?.optJSONArray("messages")?.length() ?: 0) == 0 } }
    val loaded = m.status?.model != null
    val offline = m.online == false
    val loading = m.loading != null
    val streaming = m.streaming
    val pictures = attached.any { it.image != null }
    val blind = pictures && m.status?.model?.vision == false
    val placeholder = when {
        offline -> tr("Your computer is offline", "Komputer jest niedostępny")
        loading -> tr("Loading the model…", "Wczytuję model…")
        loaded -> tr("Ask anything", "Zapytaj o cokolwiek")
        else -> tr("Choose a model to get answers", "Wybierz model, by dostać odpowiedź")
    }
    val blocked = when {
        streaming || (draft.isBlank() && attached.isEmpty()) -> null
        blind -> tr("This model cannot see pictures", "Ten model nie widzi zdjęć")
        offline -> tr("Your computer is offline", "Komputer jest niedostępny")
        loading -> tr("Loading the model…", "Wczytuję model…")
        else -> null
    }
    val ready = (draft.isNotBlank() || attached.isNotEmpty()) && loaded && !offline && !streaming && !blind
    val send: () -> Unit = {
        m.send(draft, attached.toList())
        draft = ""
        attached.clear()
    }
    val view = LocalView.current
    val enter: () -> Unit = {
        when {
            ready -> {
                view.feel(Feel.Confirm)
                send()
            }
            blocked != null -> {
                view.feel(Feel.Reject)
                shell.nudgeModel()
                toast.show(blocked)
            }
        }
    }
    val tall = lines > 1 || attached.isNotEmpty()
    val corner by animateDpAsState(if (tall) 24.dp else 28.dp, spring(0.85f, 420f), label = "corner")
    val pill = RoundedCornerShape(corner)

    if (adding) Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { adding = false } })

    Column(
        Modifier.align(Alignment.BottomCenter).onSizeChanged { onHeight(it.height) }
            .navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
    ) {
        AddMenu(
            open = adding,
            calm = calm,
            onCamera = { adding = false; pickers.camera() },
            onPhotos = { adding = false; pickers.photos() },
            onFiles = { adding = false; pickers.files() },
        )
        Column(Modifier.fillMaxWidth().animateContentSize(spring(0.9f, 500f)).glass(shell.cardHaze, pill).padding(6.dp)) {
            AnimatedVisibility(
                attached.isNotEmpty(),
                enter = fadeIn(tween(Motion.M2)) + expandVertically(tween(Motion.M3, easing = Motion.Out)),
                exit = fadeOut(tween(Motion.M1)) + shrinkVertically(tween(Motion.M2, easing = Motion.Standard)),
            ) { AttachRow(attached) { attached.remove(it) } }
            Row(verticalAlignment = Alignment.Bottom) {
                val turn by animateFloatAsState(if (adding) 45f else 0f, spring(0.6f, 520f), label = "plus")
                IconBtn(
                    Glyph.Plus, { adding = !adding },
                    Modifier.graphicsLayer { rotationZ = turn },
                    tint = if (adding) Ink.One else Ink.Two, fill = if (adding) Tone.Raise else Color.Transparent, size = 44.dp,
                )
                BasicTextField(
                    draft,
                    { draft = it },
                    Modifier.weight(1f).heightIn(min = 44.dp).padding(horizontal = 6.dp, vertical = 11.dp)
                        .onPreviewKeyEvent { e ->
                            // A hardware Enter sends when Enter sends; Shift+Enter still breaks the line.
                            if (m.enterSends && e.key == Key.Enter && !e.isShiftPressed) {
                                if (e.type == KeyEventType.KeyDown) enter()
                                true
                            } else {
                                false
                            }
                        },
                    textStyle = Type.Body,
                    keyboardOptions = if (m.enterSends) KeyboardOptions(imeAction = ImeAction.Send) else KeyboardOptions.Default,
                    keyboardActions = KeyboardActions(onSend = { enter() }),
                    cursorBrush = SolidColor(LocalAccent.current),
                    maxLines = 8,
                    onTextLayout = { lines = it.lineCount },
                    decorationBox = { inner ->
                        Box {
                            if (draft.isEmpty()) Roll(placeholder, Type.Body.copy(color = Ink.Four))
                            inner()
                        }
                    },
                )
                AnimatedContent(
                    !loaded && !loading && !offline && !streaming,
                    transitionSpec = {
                        (fadeIn(tween(Motion.M2)) + scaleIn(tween(Motion.M2, easing = Motion.Out), 0.9f)) togetherWith
                            (fadeOut(tween(Motion.M1)) + scaleOut(tween(Motion.M1), 0.9f)) using
                            SizeTransform(clip = false) { _, _ -> spring(0.9f, 500f, IntSize.VisibilityThreshold) }
                    },
                    contentAlignment = Alignment.CenterEnd,
                    label = "send",
                ) { choose ->
                    if (choose) {
                        MorphSource("composer-model", Surface.Model) {
                            Btn(
                                tr("Model", "Model"),
                                { shell.open(Surface.Model, "composer-model") },
                                Modifier.morph("composer-model", this, Shapes.ControlLg),
                                Variant.Standard,
                                Glyph.Models,
                                height = 44.dp,
                            )
                        }
                    } else {
                        SendButton(
                            streaming = streaming,
                            ready = ready,
                            blocked = blocked,
                            onSend = send,
                            onStop = { m.stop() },
                            onBlocked = { toast.show(it) },
                        )
                    }
                }
            }
        }
        AnimatedVisibility(
            empty,
            enter = fadeIn(tween(Motion.M2)) + expandVertically(tween(Motion.M2, easing = Motion.Out)),
            exit = fadeOut(tween(Motion.M1)) + shrinkVertically(tween(Motion.M2, easing = Motion.Standard)),
        ) {
            BasicText(
                tr("Private: answered on your computer", "Prywatne, odpowiada Twój komputer"),
                Modifier.fillMaxWidth().padding(top = 6.dp),
                style = Type.Meta.copy(color = Ink.Four, fontSize = Type.Label.fontSize, textAlign = TextAlign.Center),
            )
        }
    }
}

/** Send and stop are one drawn glyph: the arrow folds into a bar that fills out into the stop square. */
@Composable
private fun SendButton(
    streaming: Boolean,
    ready: Boolean,
    blocked: String?,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onBlocked: (String) -> Unit,
) {
    val shell = LocalShell.current
    val calm = shell.calm
    val accent = LocalAccent.current
    val scope = rememberCoroutineScope()
    val p = remember { Animatable(if (streaming) 1f else 0f) }
    val ghost = remember { Animatable(0f) }
    val wake = remember { Animatable(1f) }
    val stopped = remember { booleanArrayOf(false) }
    val colour = tween<Color>(Motion.M2, easing = Motion.Standard)
    val fill = animateColorAsState(
        when {
            streaming -> Tone.Hover
            ready -> accent
            else -> Tone.Raise
        },
        colour, label = "fill",
    )
    val ink = animateColorAsState(
        when {
            streaming -> Ink.One
            ready -> OnAccent
            else -> Ink.Four
        },
        colour, label = "ink",
    )

    LaunchedEffect(streaming) {
        val spec: AnimationSpec<Float> = when {
            calm -> tween(120)
            streaming -> spring(0.78f, 520f)
            stopped[0] -> spring(0.8f, 420f)
            else -> spring(0.82f, 380f)
        }
        stopped[0] = false
        p.animateTo(if (streaming) 1f else 0f, spec)
    }
    LaunchedEffect(ready) {
        if (ready && !calm) {
            wake.snapTo(0f)
            wake.animateTo(1f, spring(0.5f, 600f))
        }
    }

    Box(
        Modifier.size(40.dp)
            .graphicsLayer {
                val w = wake.value
                translationY = (1f - w) * 2.dp.toPx()
                scaleX = 0.9f + 0.1f * w
                scaleY = scaleX
            }
            .pressable(
                {
                    if (streaming) {
                        stopped[0] = true
                        onStop()
                    } else if (ready) {
                        if (!calm) scope.launch {
                            ghost.snapTo(0f)
                            ghost.animateTo(1f, tween(160, easing = Motion.Out))
                            ghost.snapTo(0f)
                        }
                        onSend()
                    }
                },
                CircleShape,
                depth = 0.88f,
                enabled = streaming || ready,
                feel = if (streaming) Feel.Tap else Feel.Confirm,
                stretch = true,
                feelOnDown = streaming,
                blocked = blocked?.let { reason -> { shell.nudgeModel(); onBlocked(reason) } },
            )
            .drawWithCache {
                val u = 1.dp.toPx()
                onDrawBehind {
                    drawCircle(fill.value)
                    glyph(p.value.coerceIn(0f, 1f), ink.value, u, 0f, 1f)
                    val g = ghost.value
                    if (g > 0f && g < 1f) glyph(0f, ink.value, u, -14f * u * g, 1f - g)
                }
            },
    )
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/** In a 40dp box: 0 is the send arrow, 1 the stop square. */
private fun DrawScope.glyph(p: Float, color: Color, u: Float, dy: Float, alpha: Float) {
    val w = 2.2f * u
    val cx = 20f * u
    if (p < 0.6f) {
        val a = p / 0.6f
        val apex = lerp(13f, 20f, a) * u + dy
        val shaft = lerp(14f, 0f, (p / 0.5f).coerceIn(0f, 1f)) * u
        if (shaft > 0.1f) drawLine(color, Offset(cx, apex), Offset(cx, apex + shaft), w, StrokeCap.Round, alpha = alpha)
        val th = lerp(45f, 0f, a) * PI.toFloat() / 180f
        val arm = lerp(8.485f, 6f, a) * u
        val ax = arm * cos(th)
        val ay = arm * sin(th)
        drawLine(color, Offset(cx - ax, apex + ay), Offset(cx, apex), w, StrokeCap.Round, alpha = alpha)
        drawLine(color, Offset(cx + ax, apex + ay), Offset(cx, apex), w, StrokeCap.Round, alpha = alpha)
    } else {
        val b = (p - 0.6f) / 0.4f
        val width = lerp(14.2f, 12f, b) * u
        val height = lerp(2.2f, 12f, b) * u
        drawRoundRect(
            color,
            Offset(cx - width / 2f, 20f * u - height / 2f + dy),
            Size(width, height),
            CornerRadius(lerp(1.1f, 3.5f, b) * u),
            alpha = alpha,
        )
    }
}

@Composable
private fun BoxScope.ToastIsland(toast: Toast, lift: Density.() -> Int) {
    val calm = LocalShell.current.calm
    val last = remember { arrayOf("") }
    toast.text?.let { last[0] = it }
    LaunchedEffect(toast.shown) {
        if (toast.text != null) {
            delay(1400)
            toast.hide()
        }
    }
    AnimatedVisibility(
        toast.text != null,
        Modifier.align(Alignment.BottomCenter).offset { IntOffset(0, -lift()) },
        enter = if (calm) fadeIn(tween(Motion.M2)) else fadeIn(tween(Motion.M2)) + slideInVertically(spring(0.9f, 500f)) { it / 2 },
        exit = fadeOut(tween(Motion.M2)),
    ) {
        Box(Modifier.height(36.dp).background(Tone.Float, Shapes.Card).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            BasicText(last[0], style = Type.Meta.copy(color = Ink.One))
        }
    }
}

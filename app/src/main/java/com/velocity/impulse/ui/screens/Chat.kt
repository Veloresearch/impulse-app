package com.velocity.impulse.ui.screens

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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VisibilityThreshold
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Chat
import com.velocity.impulse.data.Message
import com.velocity.impulse.ui.Chrome
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.LocalShell
import com.velocity.impulse.ui.Surface
import com.velocity.impulse.ui.components.Btn
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
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
        if (added > 0 && list.firstVisibleItemIndex == added && list.firstVisibleItemScrollOffset < near) list.scrollToItem(0)
    }

    LazyColumn(
        Modifier.fillMaxSize().then(if (source) Modifier.glassSource(shell.cardHaze) else Modifier),
        state = list,
        reverseLayout = true,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = top + Chrome.Top + 8.dp + typing,
            bottom = with(d) { composerH().toDp() } + 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        itemsIndexed(items, key = { _, msg -> msg.id }) { i, msg ->
            val user = msg.role == "user"
            val appear = remember { MutableTransitionState(!seen.add(msg.id)).apply { targetState = true } }
            AnimatedVisibility(
                visibleState = appear,
                enter = when {
                    calm -> fadeIn(tween(Motion.M2))
                    user -> slideInVertically(spring(0.82f, 420f)) { rise } +
                        scaleIn(spring(0.82f, 420f), 0.94f, TransformOrigin(1f, 1f)) +
                        fadeIn(tween(Motion.M2, easing = Motion.Out))
                    else -> fadeIn(tween(Motion.M3))
                },
            ) {
                if (user) UserBubble(msg, toast) else Answer(m, chat, msg, msg.id == liveId, i == 0, atBottom)
            }
        }
    }
}

@Composable
private fun UserBubble(msg: Message, toast: Toast) {
    val clip = LocalClipboardManager.current
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        BasicText(
            msg.text,
            Modifier.widthIn(max = 300.dp)
                .pressable(
                    {}, Shapes.Card, Tone.Raise, Tone.Hover, depth = 1f, feel = null,
                    onLong = {
                        clip.setText(AnnotatedString(msg.text))
                        toast.show(tr("Copied", "Skopiowano"))
                    },
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            style = Type.Body,
        )
    }
}

@Immutable
private data class Think(val has: Boolean, val active: Boolean, val since: Long, val textAt: Long)

@Composable
private fun Answer(m: AppModel, chat: Chat?, msg: Message, isLive: Boolean, last: Boolean, atBottom: () -> Boolean) {
    val id = msg.id
    val current = rememberUpdatedState(msg)
    var revealing by remember { mutableStateOf(isLive) }
    LaunchedEffect(isLive) { if (isLive) revealing = true }
    val sentHere = remember { isLive }
    val think by remember {
        derivedStateOf {
            m.live?.takeIf { it.messageId == id }?.let {
                Think(it.thought.isNotBlank(), it.thought.isNotBlank() && it.text.isEmpty(), it.thoughtAt, it.textAt)
            }
        }
    }
    // Between the stream closing and the chat taking the final text, the last live picture holds.
    val held = remember { arrayOfNulls<Think>(1) }
    val t = think?.also { held[0] = it } ?: held[0].takeIf { revealing }
    val thought = remember { { m.live?.takeIf { it.messageId == id }?.thought ?: current.value.thought } }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (m.showThinking && (t?.has ?: msg.thought.isNotBlank())) {
            Thinking(
                thought,
                active = t?.active == true,
                since = t?.since ?: 0L,
                tookMs = if (t != null && t.textAt > 0) t.textAt - t.since else msg.raw.optLong("thoughtMs"),
            )
        }
        if (revealing) {
            LiveAnswer(m, id, current, atBottom) { revealing = false }
        } else if (msg.text.isNotEmpty()) {
            Markdown(msg.text)
        }
        msg.error?.let { ErrorBanner(m, it, last, sentHere) }
        AnimatedVisibility(
            !revealing,
            enter = fadeIn(tween(Motion.M3)) + expandVertically(spring(0.9f, 500f)),
            exit = fadeOut(tween(Motion.M1)) + shrinkVertically(tween(Motion.M1)),
        ) {
            Actions(m, chat, msg, last)
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

    if (shown.isEmpty()) Waiting(queued, calm) else Markdown(shown, edge = edge, caret = { caret.value })
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

@Composable
private fun Thinking(thought: () -> String, active: Boolean, since: Long, tookMs: Long) {
    val calm = LocalShell.current.calm
    var open by remember { mutableStateOf(false) }
    val turn = animateFloatAsState(if (open && !active) 90f else 0f, spring(0.8f, 500f), label = "chevron")
    var secs by remember { mutableIntStateOf(0) }
    LaunchedEffect(active, since) {
        while (active && since > 0) {
            val gone = System.currentTimeMillis() - since
            secs = (gone / 1000).toInt()
            delay(1000 - gone % 1000)
        }
    }
    val meta = Type.Meta.copy(fontWeight = FontWeight.Medium)
    val done = if (tookMs > 0) {
        val n = max(1L, (tookMs + 500) / 1000)
        tr("Thought for $n s", "Myślenie: $n s")
    } else {
        tr("Thought it through", "Przemyślane")
    }

    Column(Modifier.fillMaxWidth().background(Tone.Panel, Shapes.Tile)) {
        Row(
            Modifier.fillMaxWidth()
                .pressable({ if (!active) open = !open }, Shapes.Tile, Tone.Panel, Tone.Raise, depth = 1f, feel = if (active) null else Feel.Tick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Glyph.Brain, Ink.Three, size = 16.dp)
            Spacer(Modifier.width(8.dp))
            AnimatedContent(
                active,
                Modifier.weight(1f),
                transitionSpec = {
                    if (calm) {
                        fadeIn(tween(120)) togetherWith fadeOut(tween(90))
                    } else {
                        (slideInVertically(Motion.calmText()) { it * 6 / 10 } + fadeIn(tween(Motion.M2, easing = Motion.Out))) togetherWith
                            (slideOutVertically(tween(Motion.M1, easing = Motion.Leave)) { -it / 2 } + fadeOut(tween(90)))
                    }
                },
                label = "think",
            ) { a ->
                if (a) {
                    Row {
                        BasicText(tr("Thinking · ", "Myśli · "), style = meta)
                        Roll("$secs", meta, digits = true)
                        BasicText(" s", style = meta)
                    }
                } else {
                    BasicText(done, style = meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            AnimatedVisibility(!active, enter = scaleIn(spring(0.6f, 600f), 0.6f) + fadeIn(), exit = fadeOut(tween(Motion.M1))) {
                Icon(Glyph.Right, Ink.Three, Modifier.graphicsLayer { rotationZ = turn.value }, size = 14.dp)
            }
        }
        AnimatedVisibility(
            active,
            enter = fadeIn(tween(Motion.M2)),
            exit = if (calm) fadeOut(tween(Motion.M1)) else shrinkVertically(tween(Motion.M2, easing = Motion.Standard)) + fadeOut(tween(Motion.M1)),
        ) {
            ThoughtTail(thought, calm)
        }
        AnimatedVisibility(
            open && !active,
            enter = if (calm) fadeIn(tween(Motion.M2)) else expandVertically(spring(0.9f, 450f)) + fadeIn(tween(Motion.M2, delayMillis = 60)),
            exit = if (calm) fadeOut(tween(Motion.M2)) else shrinkVertically(tween(Motion.M2, easing = Motion.Leave)) + fadeOut(tween(Motion.M1)),
        ) {
            BasicText(thought().trim(), Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), style = Type.Meta)
        }
    }
}

/** A short window on the thought as it arrives; each new line slides up into place. */
@Composable
private fun ThoughtTail(thought: () -> String, calm: Boolean) {
    val d = LocalDensity.current
    val scope = rememberCoroutineScope()
    val slide = remember { Animatable(0f) }
    val lines = remember { intArrayOf(0) }
    val tail by remember { derivedStateOf { thought().takeLast(240) } }
    Box(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 10.dp).height(36.dp).clipToBounds()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val h = 12.dp.toPx()
                drawRect(Brush.verticalGradient(0f to Color.Black, 1f to Color.Transparent, endY = h), size = Size(size.width, h), blendMode = BlendMode.DstOut)
            },
    ) {
        BasicText(
            tail,
            Modifier.fillMaxWidth().wrapContentHeight(Alignment.Bottom, unbounded = true).graphicsLayer { translationY = slide.value },
            style = Type.Meta.copy(color = Ink.Three),
            onTextLayout = { r ->
                if (!calm && lines[0] > 0 && r.lineCount > lines[0]) scope.launch {
                    slide.snapTo(with(d) { 18.dp.toPx() })
                    slide.animateTo(0f, spring(0.9f, 500f))
                }
                lines[0] = r.lineCount
            },
        )
    }
}

@Composable
private fun ErrorBanner(m: AppModel, text: String, last: Boolean, fresh: Boolean) {
    val view = LocalView.current
    val shown = remember { MutableTransitionState(!fresh).apply { targetState = true } }
    LaunchedEffect(Unit) { if (fresh) view.feel(Feel.Reject) }
    AnimatedVisibility(shown, enter = expandVertically(tween(Motion.M2, easing = Motion.Out)) + fadeIn()) {
        Row(
            Modifier.fillMaxWidth().background(Tone.Panel, Shapes.Tile).padding(12.dp),
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
private fun Actions(m: AppModel, chat: Chat?, msg: Message, last: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (msg.text.isNotBlank()) CopyButton(msg.text)
        if (last) {
            RetryButton(m)
            val model = chat?.raw?.optString("modelName")?.takeIf { it.isNotBlank() && it != "null" }
            Spacer(Modifier.width(8.dp))
            BasicText(
                listOfNotNull(model, whenLabel(msg.at).ifBlank { null }).joinToString(" · "),
                style = Type.Meta.copy(color = Ink.Four),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CopyButton(text: String) {
    val clip = LocalClipboardManager.current
    val accent = LocalAccent.current
    val calm = LocalShell.current.calm
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1400)
            copied = false
        }
    }
    Box(
        Modifier.size(34.dp).pressable(
            { clip.setText(AnnotatedString(text)); copied = true },
            CircleShape, Color.Transparent, Tone.Raise, depth = 0.88f, stretch = true,
        ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            copied,
            transitionSpec = {
                if (targetState && !calm) {
                    (scaleIn(spring(0.55f, 700f), 0.6f) + fadeIn(tween(90))) togetherWith (scaleOut(tween(90), targetScale = 0.6f) + fadeOut(tween(90)))
                } else {
                    fadeIn(tween(Motion.M2)) togetherWith fadeOut(tween(Motion.M2))
                }
            },
            label = "copy",
        ) { done ->
            Icon(if (done) Glyph.Check else Glyph.Copy, if (done) accent else Ink.Three, size = 16.dp)
        }
    }
}

@Composable
private fun RetryButton(m: AppModel) {
    val calm = LocalShell.current.calm
    val scope = rememberCoroutineScope()
    val spin = remember { Animatable(0f) }
    Box(
        Modifier.size(34.dp).pressable(
            {
                if (!spin.isRunning) scope.launch {
                    if (!calm) {
                        spin.snapTo(0f)
                        spin.animateTo(-360f, spring(0.8f, 300f, visibilityThreshold = 1f))
                    }
                    m.retry()
                }
            },
            CircleShape, Color.Transparent, Tone.Raise, depth = 0.88f, stretch = true,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Glyph.Refresh, Ink.Three, Modifier.graphicsLayer { rotationZ = spin.value }, size = 16.dp)
    }
}

@Composable
private fun BoxScope.Composer(m: AppModel, toast: Toast, onHeight: (Int) -> Unit) {
    val shell = LocalShell.current
    val accent = LocalAccent.current
    var draft by rememberSaveable(m.current) { mutableStateOf("") }
    val empty by remember { derivedStateOf { (m.chat?.raw?.optJSONArray("messages")?.length() ?: 0) == 0 } }
    val loaded = m.status?.model != null
    val offline = m.online == false
    val loading = m.loading != null
    val streaming = m.streaming
    val ctx = m.chat?.contextName ?: m.defaultContext
    val placeholder = when {
        offline -> tr("Your computer is offline", "Komputer jest niedostępny")
        loading -> tr("Loading the model…", "Wczytuję model…")
        loaded -> tr("Ask anything", "Zapytaj o cokolwiek")
        else -> tr("Choose a model to get answers", "Wybierz model, by dostać odpowiedź")
    }
    val blocked = when {
        streaming || draft.isBlank() -> null
        offline -> tr("Your computer is offline", "Komputer jest niedostępny")
        loading -> tr("Loading the model…", "Wczytuję model…")
        else -> null
    }

    Column(
        Modifier.align(Alignment.BottomCenter).onSizeChanged { onHeight(it.height) }
            .navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
    ) {
        Column(Modifier.fillMaxWidth().glass(shell.cardHaze, Shapes.Panel).padding(8.dp)) {
            BasicTextField(
                draft,
                { draft = it },
                Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 12.dp, vertical = 10.dp),
                textStyle = Type.Body,
                cursorBrush = SolidColor(accent),
                maxLines = 7,
                decorationBox = { inner ->
                    Box {
                        if (draft.isEmpty()) Roll(placeholder, Type.Body.copy(color = Ink.Four))
                        inner()
                    }
                },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                MorphSource("composer-context", Surface.Context) {
                    val tint = if (ctx != null) accent else Ink.Three
                    Row(
                        Modifier.morph("composer-context", this, Shapes.ControlLg).height(36.dp)
                            .pressable(
                                { shell.open(Surface.Context, "composer-context") },
                                Shapes.ControlLg, Color.Transparent, Tone.Raise, depth = 0.95f, stretch = true, feelOnDown = true,
                            )
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Glyph.Contexts, tint, size = 18.dp)
                        Spacer(Modifier.width(6.dp))
                        Roll(ctx ?: tr("Context", "Kontekst"), Type.Meta.copy(color = tint, fontWeight = FontWeight.Medium), Modifier.widthIn(max = 130.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
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
                                tr("Choose model", "Wybierz model"),
                                { shell.open(Surface.Model, "composer-model") },
                                Modifier.morph("composer-model", this, Shapes.ControlLg),
                                Variant.Standard,
                                Glyph.Models,
                                height = 36.dp,
                            )
                        }
                    } else {
                        SendButton(
                            streaming = streaming,
                            ready = draft.isNotBlank() && loaded && !offline && !streaming,
                            blocked = blocked,
                            onSend = {
                                m.send(draft)
                                draft = ""
                            },
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

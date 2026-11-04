package com.velocity.impulse.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Message
import com.velocity.impulse.ui.components.IconBtn
import com.velocity.impulse.ui.components.ImpulseMark
import com.velocity.impulse.ui.components.Markdown
import com.velocity.impulse.ui.components.OnAccent
import com.velocity.impulse.ui.components.Sheet
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Signal
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type

@Composable
fun ChatScreen(m: AppModel, barHeight: androidx.compose.ui.unit.Dp, openContexts: () -> Unit) {
    var history by remember { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    val messages = m.chat?.messages.orEmpty()
    val list = rememberLazyListState()
    val seen = remember(m.current) { mutableStateOf(messages.size) }

    LaunchedEffect(m.current) { if (messages.isNotEmpty()) list.scrollToItem(messages.lastIndex) }
    LaunchedEffect(m.current) {
        snapshotFlow { m.chat?.messages?.let { it.size to it.lastOrNull()?.content?.length } }.collect {
            val last = list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (messages.isNotEmpty() && last >= list.layoutInfo.totalItemsCount - 2) list.animateScrollToItem(list.layoutInfo.totalItemsCount - 1)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().imePadding()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Space.S4, vertical = Space.S2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBtn(Icons.Default.List, { history = true })
                AnimatedContent(
                    m.chat?.title?.ifBlank { null } ?: "New chat",
                    Modifier.weight(1f).padding(horizontal = Space.S3),
                    transitionSpec = { (fadeIn(tween(Motion.M2)) + slideInVertically(Motion.travel()) { it / 2 }) togetherWith fadeOut(tween(Motion.M1)) },
                    label = "title",
                ) { t -> BasicText(t, style = Type.Section, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                IconBtn(Icons.Default.Add, { m.newChat(); draft = "" })
            }

            Box(Modifier.weight(1f)) {
                if (messages.isEmpty()) Welcome { draft = it } else LazyColumn(
                    state = list,
                    contentPadding = PaddingValues(start = Space.S4, end = Space.S4, top = Space.S2, bottom = Space.S5),
                    verticalArrangement = Arrangement.spacedBy(Space.S4),
                ) {
                    itemsIndexed(messages, key = { i, _ -> "${m.current}-$i" }) { i, msg ->
                        val appear = remember { MutableTransitionState(i < seen.value).apply { targetState = true } }
                        androidx.compose.animation.AnimatedVisibility(
                            visibleState = appear,
                            enter = fadeIn(tween(Motion.M3, easing = Motion.Out)) + slideInVertically(Motion.travel()) { it / 3 } + scaleIn(Motion.travel(), initialScale = 0.96f),
                        ) {
                            Bubble(msg, m.showThinking, live = m.streaming && i == messages.lastIndex)
                        }
                    }
                }
            }

            Composer(m, draft, { draft = it }, barHeight, openContexts) {
                m.send(draft)
                draft = ""
            }
        }

        Sheet(history, { history = false }) {
            BasicText("Chats", style = Type.Title, modifier = Modifier.padding(bottom = Space.S3))
            if (m.chats.isEmpty()) BasicText("Nothing yet. Your chats appear here and stay on this phone.", style = Type.Body.copy(color = Ink.Three))
            LazyColumn(Modifier.heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                itemsIndexed(m.chats, key = { _, c -> c.id }) { _, c ->
                    Row(
                        Modifier.animateItem().fillMaxWidth()
                            .pressable({ m.open(c.id); history = false }, Shapes.ControlLg, if (c.id == m.current) Tone.Raise else Tone.Float, Tone.Press, depth = 0.985f)
                            .padding(start = Space.S3, top = Space.S2, bottom = Space.S2),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            BasicText(c.title.ifBlank { "Chat" }, style = Type.Body.copy(fontWeight = FontWeight(550)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            BasicText("${c.messages.size / 2} questions", style = Type.Meta)
                        }
                        IconBtn(Icons.Default.Delete, { m.delete(c.id) }, tint = Ink.Four, fill = androidx.compose.ui.graphics.Color.Transparent, size = 40.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Welcome(onPick: (String) -> Unit) {
    val turn by rememberInfiniteTransition(label = "idle").animateFloat(-4f, 4f, infiniteRepeatable(tween(4200, easing = Motion.Standard), RepeatMode.Reverse), label = "spin")
    Column(
        Modifier.fillMaxSize().padding(Space.S5),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ImpulseMark(Modifier.size(84.dp), spin = turn)
        Spacer(Modifier.height(Space.S5))
        BasicText("What's on your mind?", style = Type.Title)
        Spacer(Modifier.height(Space.S2))
        BasicText("Answered by the model on your computer.", style = Type.Body.copy(color = Ink.Three))
        Spacer(Modifier.height(Space.S5))
        listOf("Summarise what's in my context", "Explain this like I'm new to it", "Draft a short reply").forEach {
            Row(
                Modifier.padding(vertical = 3.dp).pressable({ onPick(it) }, Shapes.ControlLg, Tone.Panel, Tone.Raise).padding(horizontal = Space.S4, vertical = Space.S3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(it, style = Type.Body.copy(color = Ink.Two))
            }
        }
    }
}

@Composable
private fun Bubble(msg: Message, showThinking: Boolean, live: Boolean) {
    if (msg.role == "user") {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            BasicText(
                msg.content,
                Modifier.widthIn(max = 300.dp).background(Tone.Raise, Shapes.Card).padding(horizontal = Space.S4, vertical = Space.S3),
                style = Type.Body,
            )
        }
        return
    }
    Column(Modifier.fillMaxWidth().animateContentSize(Motion.settle()), verticalArrangement = Arrangement.spacedBy(Space.S2)) {
        if (showThinking && msg.thinking.isNotBlank()) Thinking(msg.thinking, live && msg.content.isEmpty())
        if (msg.content.isEmpty() && live) Dots() else Markdown(msg.content)
    }
}

@Composable
private fun Thinking(text: String, active: Boolean) {
    var open by remember { mutableStateOf(false) }
    val turn by animateFloatAsState(if (open) 90f else 0f, Motion.travel(), label = "chevron")
    val shimmer by rememberInfiniteTransition(label = "think").animateFloat(0.45f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Column(Modifier.background(Tone.Panel, Shapes.Tile)) {
        Row(
            Modifier.fillMaxWidth().pressable({ open = !open }, depth = 0.985f).padding(horizontal = Space.S3, vertical = Space.S2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                if (active) "Thinking…" else "Thought it through",
                Modifier.weight(1f).graphicsLayer { alpha = if (active) shimmer else 1f },
                style = Type.Meta.copy(fontWeight = FontWeight(600)),
            )
            Image(Icons.Default.KeyboardArrowRight, null, Modifier.size(18.dp).rotate(turn), colorFilter = ColorFilter.tint(Ink.Three))
        }
        AnimatedVisibility(open, enter = expandVertically(Motion.travel()) + fadeIn(), exit = shrinkVertically(Motion.settle()) + fadeOut()) {
            BasicText(text.trim(), Modifier.padding(start = Space.S3, end = Space.S3, bottom = Space.S3), style = Type.Meta)
        }
    }
}

@Composable
private fun Dots() {
    val t = rememberInfiniteTransition(label = "dots")
    Row(Modifier.height(24.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val y by t.animateFloat(
                0f, 1f,
                infiniteRepeatable(androidx.compose.animation.core.keyframes {
                    durationMillis = 1100
                    0f at 0 + i * 140
                    1f at 260 + i * 140 using Motion.Out
                    0f at 560 + i * 140 using Motion.Standard
                }),
                label = "dot$i",
            )
            Box(Modifier.size(7.dp).graphicsLayer { translationY = -y * 6.dp.toPx(); alpha = 0.4f + y * 0.6f }.background(Ink.Three, CircleShape))
        }
    }
}

@Composable
private fun Composer(
    m: AppModel,
    draft: String,
    onDraft: (String) -> Unit,
    barHeight: androidx.compose.ui.unit.Dp,
    openContexts: () -> Unit,
    onSend: () -> Unit,
) {
    val accent = LocalAccent.current
    val ready = draft.isNotBlank() && m.online != false
    Column(
        Modifier.padding(start = Space.S3, end = Space.S3, bottom = barHeight + Space.S2)
            .background(Tone.Panel, Shapes.Panel).padding(Space.S2),
    ) {
        AnimatedVisibility(m.error != null, enter = expandVertically(Motion.travel()) + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            BasicText(m.error.orEmpty(), Modifier.padding(Space.S2), style = Type.Meta.copy(color = Signal.Err))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            BasicTextField(
                draft, onDraft,
                Modifier.weight(1f).heightIn(min = 44.dp).padding(horizontal = Space.S3, vertical = 11.dp),
                textStyle = Type.Body,
                cursorBrush = SolidColor(accent),
                maxLines = 6,
                decorationBox = { inner ->
                    Box {
                        if (draft.isEmpty()) BasicText("Ask anything", style = Type.Body.copy(color = Ink.Four))
                        inner()
                    }
                },
            )
            val live = m.streaming
            AnimatedContent(
                live,
                transitionSpec = { (scaleIn(Motion.press(), 0.6f) + fadeIn(tween(Motion.M1))) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut(tween(Motion.M1))) },
                label = "send",
            ) { streaming ->
                if (streaming) IconBtn(Icons.Default.Close, { m.stop() }, tint = Ink.One, fill = Tone.Hover, size = 44.dp)
                else IconBtn(Icons.Default.Send, onSend, tint = if (ready) OnAccent else Ink.Four, fill = if (ready) accent else Tone.Raise, size = 44.dp, enabled = ready)
            }
        }
        Row(
            Modifier.padding(start = Space.S1, top = Space.S1).pressable(openContexts, Shapes.Chip, Tone.Raise, Tone.Press).padding(horizontal = Space.S2, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).background(if (m.context != null) accent else Ink.Four, CircleShape))
            Spacer(Modifier.width(6.dp))
            BasicText(m.context ?: "No context", style = Type.Label.copy(color = Ink.Two))
        }
    }
}

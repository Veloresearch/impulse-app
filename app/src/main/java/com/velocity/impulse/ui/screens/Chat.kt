package com.velocity.impulse.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Chat
import com.velocity.impulse.data.Message
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.components.Field
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.IconBtn
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

// ── the list ───────────────────────────────────────────────────────────────────────────────

private fun sections(chats: List<Chat>): List<Pair<String, List<Chat>>> {
    val z = java.time.ZoneId.systemDefault()
    val today = java.time.LocalDate.now(z)
    fun day(c: Chat) = java.time.Instant.ofEpochMilli(c.updatedAt).atZone(z).toLocalDate()
    val rest = chats.filterNot { it.pinned }
    return listOf(
        tr("Pinned", "Przypięte") to chats.filter { it.pinned },
        tr("Today", "Dzisiaj") to rest.filter { day(it) == today },
        tr("Yesterday", "Wczoraj") to rest.filter { day(it) == today.minusDays(1) },
        tr("Last 7 days", "Ostatnie 7 dni") to rest.filter { day(it).isBefore(today.minusDays(1)) && day(it).isAfter(today.minusDays(8)) },
        tr("Earlier", "Wcześniej") to rest.filter { !day(it).isAfter(today.minusDays(8)) },
    ).filter { it.second.isNotEmpty() }
}

@Composable
fun ChatsScreen(m: AppModel, open: (String?) -> Unit) {
    val accent = LocalAccent.current
    var query by rememberSaveable { mutableStateOf("") }
    var held by remember { mutableStateOf<Chat?>(null) }
    val shown = remember(m.chats, query) {
        if (query.isBlank()) m.chats else m.chats.filter { c ->
            c.title.contains(query, true) || c.messages.any { it.text.contains(query, true) }
        }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Space.S4, end = Space.S4, top = Space.S3, bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item(key = "title") {
                Column(Modifier.padding(bottom = Space.S3), verticalArrangement = Arrangement.spacedBy(Space.S3)) {
                    BasicText(tr("Chats", "Czaty"), style = Type.Display)
                    Field(query, { query = it }, tr("Search chats", "Szukaj w czatach"))
                }
            }
            for ((label, list) in sections(shown)) {
                item(key = "h-$label") {
                    BasicText(label, Modifier.animateItem().padding(start = Space.S1, top = Space.S4, bottom = Space.S2), style = Type.Label)
                }
                items(list, key = { it.id }) { c ->
                    ChatRow(c, c.id == m.current, Modifier.animateItem(), { open(c.id) }, { held = c })
                }
            }
            if (shown.isEmpty()) item(key = "empty") {
                BasicText(
                    if (query.isBlank()) tr("No chats yet. They sync from Impulse on your computer.", "Nie ma jeszcze czatów. Synchronizują się z Impulse na komputerze.")
                    else tr("Nothing matches.", "Nic nie pasuje."),
                    Modifier.padding(Space.S4), style = Type.Body.copy(color = Ink.Three),
                )
            }
        }
        Row(
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = Space.S4, bottom = 96.dp)
                .height(50.dp).pressable({ open(null) }, Shapes.Card, accent, accent.copy(alpha = 0.8f), stretch = true)
                .padding(horizontal = Space.S5),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Glyph.Plus, OnAccent, size = 18.dp)
            Spacer(Modifier.width(Space.S2))
            BasicText(tr("New chat", "Nowy czat"), style = Type.Body.copy(color = OnAccent, fontWeight = FontWeight.SemiBold))
        }
        Sheet(held != null, { held = null }) {
            val c = held
            if (c != null) {
                BasicText(c.title.ifBlank { tr("Chat", "Czat") }, style = Type.Title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(Space.S4))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SheetAction(Glyph.Pin, if (c.pinned) tr("Unpin", "Odepnij") else tr("Pin", "Przypnij")) { m.pin(c.id); held = null }
                    SheetAction(Glyph.Trash, tr("Delete", "Usuń"), Signal.Err) { m.delete(c.id); held = null }
                }
            }
        }
    }
}

@Composable
private fun ChatRow(c: Chat, selected: Boolean, modifier: Modifier, onClick: () -> Unit, onLong: () -> Unit) {
    Row(
        modifier.fillMaxWidth()
            .pressable(onClick, Shapes.ControlLg, if (selected) Tone.Raise else Tone.App, Tone.Panel, depth = 0.985f, onLong = onLong)
            .padding(horizontal = Space.S3, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (c.pinned) {
            Icon(Glyph.Pin, Ink.Three, size = 14.dp)
            Spacer(Modifier.width(Space.S2))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(c.title.ifBlank { tr("Chat", "Czat") }, Modifier.weight(1f), style = Type.Body.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(Space.S2))
                BasicText(whenLabel(c.updatedAt), style = Type.Meta.copy(color = Ink.Four))
            }
            BasicText(c.messages.lastOrNull()?.text.orEmpty().replace('\n', ' '), style = Type.Meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SheetAction(glyph: Glyph, label: String, tint: Color = Ink.One, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).pressable(onClick, Shapes.ControlLg, Tone.Raise, Tone.Press, depth = 0.985f).padding(horizontal = Space.S4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(glyph, tint, size = 18.dp)
        Spacer(Modifier.width(Space.S3))
        BasicText(label, style = Type.Body.copy(color = tint, fontWeight = FontWeight.Medium))
    }
}

// ── one conversation ───────────────────────────────────────────────────────────────────────

@Composable
fun ChatView(m: AppModel, onBack: () -> Unit, onModels: () -> Unit) {
    var draft by rememberSaveable(m.current) { mutableStateOf("") }
    var pickContext by remember { mutableStateOf(false) }
    val c = m.chat
    val messages = c?.messages.orEmpty()
    val live = m.live?.takeIf { it.chatId == c?.id }
    val list = rememberLazyListState()
    val seen = remember(m.current) { mutableStateOf(messages.size) }
    BackHandler { onBack() }

    LaunchedEffect(m.current) { if (messages.isNotEmpty()) list.scrollToItem(messages.lastIndex) }
    LaunchedEffect(m.current) {
        snapshotFlow { (m.chat?.messages?.size ?: 0) to (m.live?.text?.length ?: 0) + (m.live?.thought?.length ?: 0) }.collect {
            val info = list.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (info.totalItemsCount > 0 && last >= info.totalItemsCount - 2) list.animateScrollToItem(info.totalItemsCount - 1, 10_000)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().imePadding()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Space.S3, vertical = Space.S2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBtn(Glyph.Left, onBack, fill = Color.Transparent)
                AnimatedContent(
                    c?.title?.ifBlank { null } ?: tr("New chat", "Nowy czat"),
                    Modifier.weight(1f).padding(horizontal = Space.S2),
                    transitionSpec = { (fadeIn(tween(Motion.M2)) + slideInVertically(Motion.travel()) { it / 2 }) togetherWith fadeOut(tween(Motion.M1)) },
                    label = "title",
                ) { t -> BasicText(t, style = Type.Section, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                IconBtn(Glyph.Plus, { m.newChat(); draft = "" }, fill = Color.Transparent)
            }

            Box(Modifier.weight(1f)) {
                if (messages.isEmpty()) Fresh(m, onModels) else LazyColumn(
                    state = list,
                    contentPadding = PaddingValues(start = Space.S4, end = Space.S4, top = Space.S2, bottom = Space.S5),
                    verticalArrangement = Arrangement.spacedBy(Space.S5),
                ) {
                    itemsIndexed(messages, key = { _, msg -> msg.id }) { i, msg ->
                        val appear = remember { MutableTransitionState(i < seen.value).apply { targetState = true } }
                        androidx.compose.animation.AnimatedVisibility(
                            visibleState = appear,
                            enter = fadeIn(tween(Motion.M3, easing = Motion.Out)) + slideInVertically(Motion.travel()) { it / 4 } +
                                scaleIn(Motion.travel(), initialScale = 0.94f, transformOrigin = TransformOrigin(if (msg.role == "user") 1f else 0f, 1f)),
                        ) {
                            val isLive = live?.messageId == msg.id
                            Bubble(
                                msg, m.showThinking,
                                text = if (isLive) live!!.text else msg.text,
                                thought = if (isLive) live!!.thought else msg.thought,
                                live = isLive, queued = if (isLive) live!!.queued else 0,
                                last = i == messages.lastIndex, onRetry = { m.retry() },
                            )
                        }
                    }
                }
            }

            Composer(m, draft, { draft = it }, { pickContext = true }, onModels) {
                m.send(draft)
                draft = ""
            }
        }

        Sheet(pickContext, { pickContext = false }) {
            BasicText(tr("Context", "Kontekst"), style = Type.Title)
            Spacer(Modifier.height(Space.S1))
            BasicText(tr("Documents this chat can draw on.", "Dokumenty, z których ten czat może korzystać."), style = Type.Meta)
            Spacer(Modifier.height(Space.S4))
            val chosen = c?.contextName ?: m.defaultContext
            val accent = LocalAccent.current
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                (listOf<String?>(null) + m.contexts.map { it.name }).forEach { name ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp)
                            .pressable({ m.useContext(name); pickContext = false }, Shapes.ControlLg, if (name == chosen) Tone.Press else Tone.Raise, Tone.Press, depth = 0.985f)
                            .padding(horizontal = Space.S4),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Glyph.Contexts, if (name == chosen) accent else Ink.Three, size = 18.dp)
                        Spacer(Modifier.width(Space.S3))
                        BasicText(name ?: tr("No context", "Bez kontekstu"), Modifier.weight(1f), style = Type.Body)
                        if (name == chosen) Icon(Glyph.Check, accent, size = 18.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Fresh(m: AppModel, onModels: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = Space.S6),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ModelPill(m, onModels)
        Spacer(Modifier.height(Space.S4))
        BasicText(
            if (m.status?.model == null) tr("Load a model, then ask anything.", "Wczytaj model i zapytaj o cokolwiek.")
            else tr("Ask anything. The answer comes from your computer.", "Zapytaj o cokolwiek. Odpowiada Twój komputer."),
            style = Type.Body.copy(color = Ink.Three, textAlign = TextAlign.Center),
        )
    }
}

@Composable
private fun Bubble(msg: Message, showThinking: Boolean, text: String, thought: String, live: Boolean, queued: Int, last: Boolean, onRetry: () -> Unit) {
    val clip = LocalClipboardManager.current
    if (msg.role == "user") {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            BasicText(
                text,
                Modifier.widthIn(max = 300.dp).background(Tone.Raise, Shapes.Card).padding(horizontal = Space.S4, vertical = Space.S3),
                style = Type.Body,
            )
            Spacer(Modifier.height(Space.S1))
            BasicText(whenLabel(msg.at), Modifier.padding(end = Space.S2), style = Type.Meta.copy(color = Ink.Four))
        }
        return
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.S2)) {
        if (showThinking && thought.isNotBlank()) Thinking(thought, live && text.isEmpty())
        if (text.isEmpty() && live) Waiting(queued) else Markdown(text)
        msg.error?.let { BasicText(it, style = Type.Meta.copy(color = Signal.Err)) }
        AnimatedVisibility(!live, enter = fadeIn(tween(Motion.M3)) + expandVertically(Motion.travel())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBtn(Glyph.Copy, { clip.setText(AnnotatedString(text)) }, tint = Ink.Three, fill = Color.Transparent, size = 34.dp)
                if (last) IconBtn(Glyph.Refresh, onRetry, tint = Ink.Three, fill = Color.Transparent, size = 34.dp)
                Spacer(Modifier.width(Space.S2))
                BasicText(whenLabel(msg.at), style = Type.Meta.copy(color = Ink.Four))
            }
        }
    }
}

@Composable
private fun Thinking(text: String, active: Boolean) {
    var open by remember { mutableStateOf(false) }
    val turn by animateFloatAsState(if (open) 90f else 0f, Motion.travel(), label = "chevron")
    val shimmer by rememberInfiniteTransition(label = "think").animateFloat(0.45f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Column(Modifier.background(Tone.Panel, Shapes.Tile)) {
        Row(
            Modifier.fillMaxWidth().pressable({ open = !open }, depth = 0.99f).padding(horizontal = Space.S3, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Glyph.Brain, Ink.Three, size = 16.dp)
            Spacer(Modifier.width(Space.S2))
            BasicText(
                if (active) tr("Thinking…", "Myśli…") else tr("Thought it through", "Przemyślane"),
                Modifier.weight(1f).graphicsLayer { alpha = if (active) shimmer else 1f },
                style = Type.Meta.copy(fontWeight = FontWeight.Medium),
            )
            Icon(Glyph.Right, Ink.Three, Modifier.rotate(turn), size = 14.dp)
        }
        AnimatedVisibility(open, enter = expandVertically(Motion.travel()) + fadeIn(), exit = shrinkVertically(Motion.settle()) + fadeOut()) {
            BasicText(text.trim(), Modifier.padding(start = Space.S3, end = Space.S3, bottom = Space.S3), style = Type.Meta)
        }
    }
}

@Composable
private fun Waiting(queued: Int) {
    if (queued > 0) {
        BasicText(tr("Waiting for $queued other answer(s)…", "Czeka w kolejce: $queued"), style = Type.Meta)
        return
    }
    val t = rememberInfiniteTransition(label = "dots")
    Row(Modifier.height(24.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val y by t.animateFloat(
                0f, 1f,
                infiniteRepeatable(keyframes {
                    durationMillis = 1100
                    0f at i * 140
                    1f at 260 + i * 140 using Motion.Out
                    0f at 560 + i * 140 using Motion.Standard
                }),
                label = "dot$i",
            )
            Box(Modifier.size(7.dp).graphicsLayer { translationY = -y * 5.dp.toPx(); alpha = 0.35f + y * 0.65f }.background(Ink.Three, CircleShape))
        }
    }
}

@Composable
private fun Composer(m: AppModel, draft: String, onDraft: (String) -> Unit, onContext: () -> Unit, onModels: () -> Unit, onSend: () -> Unit) {
    val accent = LocalAccent.current
    val loaded = m.status?.model != null
    val ready = draft.isNotBlank() && loaded && m.online != false
    val ctx = m.chat?.contextName ?: m.defaultContext
    Column(Modifier.navigationBarsPadding().padding(start = Space.S3, end = Space.S3, bottom = Space.S2)) {
        Column(Modifier.fillMaxWidth().background(Tone.Panel, Shapes.Panel).padding(Space.S2)) {
            BasicTextField(
                draft, onDraft,
                Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = Space.S3, vertical = 10.dp),
                textStyle = Type.Body,
                cursorBrush = SolidColor(accent),
                maxLines = 7,
                decorationBox = { inner ->
                    Box {
                        if (draft.isEmpty()) BasicText(
                            if (loaded) tr("Ask anything", "Zapytaj o cokolwiek") else tr("Load a model to start", "Wczytaj model, aby zacząć"),
                            style = Type.Body.copy(color = Ink.Four),
                        )
                        inner()
                    }
                },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Chip(Glyph.Contexts, ctx ?: tr("Context", "Kontekst"), if (ctx != null) accent else Ink.Three, onContext)
                Spacer(Modifier.width(Space.S1))
                if (!loaded) Chip(Glyph.Models, tr("Model", "Model"), Ink.Three, onModels)
                Spacer(Modifier.weight(1f))
                AnimatedContent(
                    m.streaming,
                    transitionSpec = { (scaleIn(Motion.press(), 0.5f) + fadeIn(tween(Motion.M1))) togetherWith (scaleOut(targetScale = 0.5f) + fadeOut(tween(Motion.M1))) },
                    label = "send",
                ) { streaming ->
                    if (streaming) IconBtn(Glyph.Stop, { m.stop() }, tint = Ink.One, fill = Tone.Hover, size = 40.dp)
                    else {
                        val fill by animateColorAsState(if (ready) accent else Tone.Raise, label = "send")
                        IconBtn(Glyph.Send, onSend, tint = if (ready) OnAccent else Ink.Four, fill = fill, size = 40.dp, enabled = ready)
                    }
                }
            }
        }
        BasicText(
            tr("Private: answered on your computer", "Prywatne, odpowiada Twój komputer"),
            Modifier.fillMaxWidth().padding(top = 6.dp),
            style = Type.Meta.copy(color = Ink.Four, fontSize = Type.Label.fontSize, textAlign = TextAlign.Center),
        )
    }
}

@Composable
private fun Chip(glyph: Glyph, label: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.height(36.dp).pressable(onClick, Shapes.ControlLg, Color.Transparent, Tone.Raise, stretch = true).padding(horizontal = Space.S2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(glyph, tint, size = 18.dp)
        Spacer(Modifier.width(6.dp))
        BasicText(label, Modifier.widthIn(max = 130.dp), style = Type.Meta.copy(color = tint, fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

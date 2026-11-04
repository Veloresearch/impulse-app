package com.velocity.impulse.ui.screens

import com.velocity.impulse.ui.components.IconBtn
import androidx.compose.foundation.shape.CircleShape
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.velocity.impulse.R
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Chat
import com.velocity.impulse.ui.Lang
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.LocalShell
import com.velocity.impulse.ui.Shell
import com.velocity.impulse.ui.Surface
import com.velocity.impulse.ui.components.Avatar
import com.velocity.impulse.ui.components.Btn
import com.velocity.impulse.ui.components.Feel
import com.velocity.impulse.ui.components.Field
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.MorphSource
import com.velocity.impulse.ui.components.Variant
import com.velocity.impulse.ui.components.feel
import com.velocity.impulse.ui.components.glass
import com.velocity.impulse.ui.components.glassSource
import com.velocity.impulse.ui.components.morph
import com.velocity.impulse.ui.components.morphText
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Signal
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class Opt { New, Models, Contexts }

private sealed class Entry(val key: String) {
    class Option(val opt: Opt) : Entry("opt-" + opt.name.lowercase())
    class Header(val label: String) : Entry("h-$label")
    class Item(val chat: Chat) : Entry(chat.id)
    class Empty(val searching: Boolean) : Entry("empty")
}

/** Where things sit, for hand-offs; plain fields so placement never recomposes. */
private class Places {
    var root: LayoutCoordinates? = null
    val rows = HashMap<String, LayoutCoordinates>()
    val titles = HashMap<String, LayoutCoordinates>()
}

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

/** The open cascade: slot 0 (the top island) arrives first, each later slot a beat after, reversing on close. */
private fun Modifier.cascade(shell: Shell, slot: () -> Int) = graphicsLayer {
    if (shell.calm) return@graphicsLayer
    val k = shell.drawer.value.coerceIn(0f, 1f)
    val i = slot()
    if (i !in 0..11) return@graphicsLayer
    val local = ((k - 0.035f * i) / (1f - 0.035f * i)).coerceIn(0f, 1f)
    translationX = -(1f - local) * 28.dp.toPx()
    alpha = local
    compositingStrategy = CompositingStrategy.ModulateAlpha
}

@Composable
fun Drawer(m: AppModel, modifier: Modifier = Modifier) {
    val shell = LocalShell.current
    val view = LocalView.current
    val focus = LocalFocusManager.current
    val density = LocalDensity.current
    val calm = shell.calm
    val haze = remember { HazeState() }
    val list = rememberLazyListState()
    val places = remember { Places() }
    var query by rememberSaveable { mutableStateOf("") }
    val pending = remember { mutableStateListOf<String>() }
    var topH by remember { mutableIntStateOf(0) }
    var bottomH by remember { mutableIntStateOf(0) }

    var held by remember { mutableStateOf<Chat?>(null) }
    var lifted by remember { mutableStateOf<Chat?>(null) }
    var heldRect by remember { mutableStateOf(Rect.Zero) }
    val wash = remember { Animatable(0f) }
    val washing by remember { derivedStateOf { wash.value > 0f } }
    var actionsH by remember { mutableIntStateOf(0) }
    var renaming by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(TextFieldValue()) }
    val nameFocus = remember { FocusRequester() }
    val ime = WindowInsets.ime

    val hay = remember(m.chats) { lazy { m.chats.associate { c -> c.id to c.messages.joinToString(" ") { it.text } } } }
    val gone = pending.toList()
    val entries = remember(m.chats, query, gone, m.current, Lang.pl) {
        val q = query.trim()
        val searching = q.isNotEmpty()
        val shown = m.chats.filter { c ->
            c.id !in gone && (!searching || c.title.contains(q, true) || hay.value[c.id]?.contains(q, true) == true)
        }
        buildList {
            if (!searching) Opt.entries.forEach { add(Entry.Option(it)) }
            for ((label, chats) in sections(shown)) {
                add(Entry.Header(label))
                chats.forEach { add(Entry.Item(it)) }
            }
            if (shown.isEmpty()) add(Entry.Empty(searching))
        }
    }

    fun commit() {
        if (pending.isEmpty()) return
        for (id in pending.toList()) {
            if (id == m.current) shell.newChat(closeDrawer = false)
            m.delete(id)
        }
        pending.clear()
    }
    LaunchedEffect(gone) {
        if (gone.isEmpty()) return@LaunchedEffect
        delay(4000)
        commit()
    }
    LaunchedEffect(Unit) {
        snapshotFlow { shell.drawer.targetValue }.collect {
            if (it == 0f) {
                focus.clearFocus()
                commit()
            }
        }
    }
    DisposableEffect(Unit) { onDispose { commit() } }

    LaunchedEffect(held != null) {
        wash.animateTo(if (held != null) 1f else 0f, tween(if (held != null) Motion.M2 else Motion.M1))
    }
    fun lift(c: Chat) {
        val root = places.root?.takeIf { it.isAttached } ?: return
        val row = places.rows[c.id]?.takeIf { it.isAttached } ?: return
        heldRect = root.localBoundingBoxOf(row)
        lifted = c
        held = c
    }
    fun drop() {
        if (renaming) focus.clearFocus()
        renaming = false
        held = null
    }
    fun rename(c: Chat) {
        if (name.text.isBlank()) return
        if (name.text.trim() != c.title) {
            m.rename(c.id, name.text)
            view.feel(Feel.Confirm)
        }
        drop()
    }
    LaunchedEffect(renaming) { if (renaming) nameFocus.requestFocus() }
    LaunchedEffect(Unit) { snapshotFlow { shell.drawer.targetValue < 1f }.collect { if (it) drop() } }
    BackHandler(held != null) { drop() }

    Box(modifier.fillMaxSize().onPlaced { places.root = it }) {
        LazyColumn(
            Modifier.fillMaxSize().glassSource(haze),
            state = list,
            contentPadding = with(density) {
                PaddingValues(start = 8.dp, end = 8.dp, top = topH.toDp() + 8.dp, bottom = bottomH.toDp() + 8.dp)
            },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(entries, key = { _, e -> e.key }, contentType = { _, e -> e::class }) { i, e ->
                val item = Modifier
                    .animateItem(
                        fadeInSpec = tween(Motion.M2),
                        placementSpec = if (calm) null else spring(0.88f, 420f, IntOffset.VisibilityThreshold),
                        fadeOutSpec = tween(Motion.M1),
                    )
                    .cascade(shell) { i - list.firstVisibleItemIndex + 1 }
                when (e) {
                    is Entry.Option -> OptionRow(m, e.opt, item)
                    is Entry.Header -> BasicText(e.label, item.padding(start = 12.dp, top = 24.dp, bottom = 8.dp), style = Type.Label)
                    is Entry.Item -> {
                        val c = e.chat
                        val current = c.id == m.current
                        Row(
                            item.fillMaxWidth().height(44.dp)
                                .onPlaced { places.rows[c.id] = it }
                                .pressable(
                                    {
                                        focus.clearFocus()
                                        shell.openChat(c.id, c.title.ifBlank { tr("Chat", "Czat") }, places.titles[c.id]?.takeIf { it.isAttached }?.boundsInRoot())
                                    },
                                    Shapes.ControlLg,
                                    if (current) Tone.Panel else Color.Transparent,
                                    if (current) Tone.Raise else Tone.Panel,
                                    depth = 1f,
                                    feel = null,
                                    onLong = { lift(c) },
                                )
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ChatLine(
                                c, current,
                                Modifier.onPlaced { places.titles[c.id] = it }
                                    .graphicsLayer { alpha = if (shell.flight.id == c.id) 0f else 1f },
                            )
                        }
                    }
                    is Entry.Empty -> key(e.searching) { EmptyNote(e.searching, calm, item) }
                }
            }
        }

        Column(
            Modifier.align(Alignment.TopCenter)
                .onSizeChanged { topH = it.height }
                .cascade(shell) { 0 }
                .statusBarsPadding()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileRow(m, haze, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                SettingsButton(haze)
            }
            Row(
                Modifier.fillMaxWidth().height(48.dp)
                    .glass(haze, androidx.compose.foundation.shape.RoundedCornerShape(percent = 50))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Glyph.Search, Ink.Three, size = 18.dp)
                Spacer(Modifier.width(10.dp))
                androidx.compose.foundation.text.BasicTextField(
                    query, { query = it },
                    Modifier.weight(1f),
                    singleLine = true,
                    textStyle = Type.Body,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(LocalAccent.current),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) BasicText(tr("Search chats", "Szukaj w czatach"), style = Type.Body.copy(color = Ink.Four))
                            inner()
                        }
                    },
                )
                if (query.isNotEmpty()) IconBtn(Glyph.Close, { query = "" }, tint = Ink.Three, fill = Color.Transparent, size = 32.dp)
            }
        }

        Box(Modifier.align(Alignment.BottomCenter).onSizeChanged { bottomH = it.height }.navigationBarsPadding())

        AnimatedVisibility(
            pending.isNotEmpty(),
            Modifier.align(Alignment.BottomCenter)
                .padding(bottom = with(density) { bottomH.toDp() })
                .padding(horizontal = 8.dp),
            enter = if (calm) fadeIn(tween(Motion.M1)) else
                slideInVertically(spring(0.85f, 500f, IntOffset.VisibilityThreshold)) { with(density) { 24.dp.roundToPx() } } + fadeIn(tween(Motion.M1)),
            exit = if (calm) fadeOut(tween(Motion.M1)) else
                slideOutVertically(tween(Motion.M1)) { with(density) { 16.dp.roundToPx() } } + fadeOut(tween(Motion.M1)),
        ) {
            Row(
                Modifier.fillMaxWidth().height(52.dp).glass(haze, Shapes.Panel).padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(tr("Chat deleted", "Czat usunięty"), Modifier.weight(1f), style = Type.Body)
                Btn(tr("Undo", "Cofnij"), { pending.clear() }, variant = Variant.Quiet, height = 36.dp)
            }
        }

        if (held != null || washing) Box(
            Modifier.fillMaxSize()
                .drawBehind { drawRect(Color.Black.copy(alpha = 0.5f * wash.value)) }
                .pointerInput(Unit) { detectTapGestures { drop() } },
        )

        val rect = heldRect
        val w = with(density) { rect.width.toDp() }
        val h = with(density) { rect.height.toDp() }
        val rootH = places.root?.size?.height ?: 0
        val above = rect.bottom > 0.7f * rootH
        // While renaming, the row and its island ride up together so the keyboard never covers them.
        val rise: Density.() -> Int = {
            if (!renaming) 0 else {
                val room = rootH - ime.getBottom(this) - 8.dp.roundToPx()
                val bottom = rect.bottom.roundToInt() + if (above) 0 else 8.dp.roundToPx() + actionsH
                (room - bottom).coerceAtMost(0)
            }
        }
        AnimatedVisibility(
            held != null,
            Modifier.offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt() + rise()) }.size(w, h),
            enter = fadeIn(tween(Motion.M1)),
            exit = fadeOut(tween(Motion.M1)),
        ) {
            val c = lifted ?: return@AnimatedVisibility
            val current = c.id == m.current
            Row(
                Modifier.fillMaxSize()
                    .shadow(12.dp, Shapes.ControlLg, ambientColor = Color.Black.copy(alpha = 0.4f))
                    .background(Tone.Raise, Shapes.ControlLg)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!renaming) ChatLine(c, current)
                else BasicTextField(
                    name,
                    { name = it.copy(text = it.text.replace('\n', ' ')) },
                    Modifier.weight(1f).focusRequester(nameFocus),
                    textStyle = Type.Body.copy(color = Ink.One, fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal),
                    cursorBrush = SolidColor(LocalAccent.current),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { rename(c) }),
                )
            }
        }

        AnimatedVisibility(
            held != null,
            Modifier
                .offset {
                    val gap = 8.dp.roundToPx()
                    val y = if (above) rect.top.roundToInt() - gap - actionsH else rect.bottom.roundToInt() + gap
                    IntOffset(rect.left.roundToInt(), y + rise())
                }
                .width(max(200.dp, w * 0.7f)),
            enter = if (calm) fadeIn(tween(Motion.M1)) else
                scaleIn(spring(0.85f, 520f), 0.85f, TransformOrigin(0.1f, if (above) 1f else 0f)) + fadeIn(tween(Motion.M1)),
            exit = if (calm) fadeOut(tween(Motion.M1)) else
                scaleOut(tween(140, easing = Motion.Leave), 0.96f) + fadeOut(tween(140, easing = Motion.Leave)),
        ) {
            val c = lifted ?: return@AnimatedVisibility
            val island = this
            AnimatedContent(
                renaming,
                Modifier.fillMaxWidth().onSizeChanged { actionsH = it.height }.glass(haze, Shapes.Panel),
                transitionSpec = {
                    fadeIn(tween(Motion.M2, delayMillis = 60)) togetherWith fadeOut(tween(Motion.M1)) using
                        SizeTransform(clip = true) { _, _ -> spring(1f, 520f, IntSize.VisibilityThreshold) }
                },
                label = "actions",
            ) { naming ->
                if (!naming) Column(Modifier.fillMaxWidth().padding(6.dp)) {
                    with(island) {
                        Action(0, painterResource(R.drawable.ph_pencil_simple_bold), tr("Rename", "Zmień nazwę"), Ink.One, calm) {
                            view.feel(Feel.Tap)
                            name = c.title.let { TextFieldValue(it, TextRange(0, it.length)) }
                            renaming = true
                        }
                        Action(1, painterResource(Glyph.Pin.res()), if (c.pinned) tr("Unpin", "Odepnij") else tr("Pin", "Przypnij"), Ink.One, calm) {
                            view.feel(Feel.Confirm)
                            m.pin(c.id)
                            drop()
                        }
                        Action(2, painterResource(Glyph.Copy.res()), tr("Duplicate", "Duplikuj"), Ink.One, calm) {
                            view.feel(Feel.Confirm)
                            m.duplicate(c.id)
                            drop()
                        }
                        Action(3, painterResource(Glyph.Trash.res()), tr("Delete", "Usuń"), Signal.Err, calm) {
                            view.feel(Feel.Tap)
                            pending += c.id
                            drop()
                        }
                    }
                } else Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Btn(tr("Cancel", "Anuluj"), ::drop, Modifier.weight(1f), variant = Variant.Quiet, height = 44.dp)
                    Btn(tr("Save", "Zapisz"), { rename(c) }, Modifier.weight(1f), variant = Variant.Primary, height = 44.dp, enabled = name.text.isNotBlank())
                }
            }
        }
    }
}

/** You and where you are connected; it opens the computer page, the gear beside it opens Settings. */
@Composable
private fun ProfileRow(m: AppModel, haze: HazeState, modifier: Modifier) {
    val shell = LocalShell.current
    MorphSource("drawer-computer", Surface.Computer, modifier) {
        val me = m.me
        val name = me?.name?.ifBlank { null } ?: "Impulse"
        val offline = m.online == false
        val dot = when (m.online) {
            true -> Signal.Ok
            false -> Signal.Err
            null -> Ink.Four
        }
        Row(
            Modifier.morph("drawer-computer", this, androidx.compose.foundation.shape.RoundedCornerShape(percent = 50))
                .glass(haze, androidx.compose.foundation.shape.RoundedCornerShape(percent = 50))
                .fillMaxWidth()
                .height(56.dp)
                .pressable({ shell.open(Surface.Computer, "drawer-computer") }, depth = 0.98f, feel = null)
                .padding(start = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(name, me?.avatar, me?.photo, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                BasicText(name, style = Type.Body.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(dot, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    BasicText(
                        if (offline) tr("Computer offline", "Komputer niedostępny") else linkLabel(m.link),
                        style = Type.Meta,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsButton(haze: HazeState) {
    val shell = LocalShell.current
    MorphSource("drawer-settings", Surface.Settings) {
        Box(
            Modifier.morph("drawer-settings", this, CircleShape).size(56.dp).glass(haze, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            IconBtn(Glyph.Settings, { shell.open(Surface.Settings, "drawer-settings") }, tint = Ink.One, fill = Color.Transparent, size = 56.dp, island = true)
        }
    }
}

@Composable
private fun OptionRow(m: AppModel, opt: Opt, modifier: Modifier) {
    val shell = LocalShell.current
    val view = LocalView.current
    val accent = LocalAccent.current
    when (opt) {
        Opt.New -> OptionLine(modifier, Glyph.Plus, accent, accent.copy(alpha = 0.16f), tr("New chat", "Nowy czat"), null) {
            view.feel(Feel.Tap)
            shell.newChat()
        }
        Opt.Models -> MorphSource("drawer-models", Surface.Model, modifier) {
            val detail = m.status?.model?.name ?: if (m.loading != null) tr("Loading…", "Wczytywanie…") else tr("None loaded", "Nie wczytano")
            OptionLine(Modifier.morph("drawer-models", this, Shapes.ControlLg), Glyph.Models, Ink.Two, Tone.Raise, tr("Models", "Modele"), detail) {
                shell.open(Surface.Model, "drawer-models")
            }
        }
        Opt.Contexts -> MorphSource("drawer-contexts", Surface.Context, modifier) {
            OptionLine(Modifier.morph("drawer-contexts", this, Shapes.ControlLg), Glyph.Contexts, Ink.Two, Tone.Raise, tr("Contexts", "Konteksty"), m.defaultContext ?: tr("None", "Brak")) {
                shell.open(Surface.Context, "drawer-contexts")
            }
        }
    }
}

/** [modifier] leads the chain so a morph wraps the press. */
@Composable
private fun OptionLine(modifier: Modifier, glyph: Glyph, ink: Color, tile: Color, title: String, detail: String?, onClick: () -> Unit) {
    val shell = LocalShell.current
    Row(
        modifier.fillMaxWidth()
            .height(48.dp)
            .pressable(onClick, Shapes.ControlLg, Color.Transparent, Tone.Panel, depth = 1f, feel = null)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tile(glyph, ink, tile)
        Spacer(Modifier.width(12.dp))
        BasicText(title, Modifier.weight(1f), style = Type.Body.copy(fontWeight = FontWeight.Medium), maxLines = 1)
        if (detail != null) BasicText(
            detail,
            Modifier.padding(start = 8.dp).widthIn(max = shell.drawerWidth * 0.45f),
            style = Type.Meta.copy(color = Ink.Three),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Tile(glyph: Glyph, ink: Color, fill: Color) {
    Box(Modifier.size(32.dp).background(fill, CircleShape), contentAlignment = Alignment.Center) {
        Icon(glyph, ink, size = 18.dp)
    }
}

/** Never touches [Chat.messages]: it re-parses the JSON on every read. */
@Composable
private fun RowScope.ChatLine(c: Chat, current: Boolean, titleModifier: Modifier = Modifier) {
    BasicText(
        c.title.ifBlank { tr("Chat", "Czat") },
        Modifier.weight(1f).then(titleModifier),
        style = Type.Body.copy(color = if (current) Ink.One else Ink.Two, fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.width(8.dp))
    BasicText(whenLabel(c.updatedAt), style = Type.Meta.copy(color = Ink.Four))
}

@Composable
private fun EmptyNote(searching: Boolean, calm: Boolean, modifier: Modifier) {
    val rise = remember { Animatable(if (searching) 0f else 1f) }
    LaunchedEffect(Unit) { rise.animateTo(1f, tween(Motion.M2, easing = Motion.Out)) }
    BasicText(
        if (searching) tr("Nothing matches.", "Nic nie pasuje.")
        else tr("No chats yet. Chats you start here or on your computer show up in both places.", "Nie ma jeszcze czatów. Czaty z telefonu i komputera pojawiają się w obu miejscach."),
        modifier.graphicsLayer {
            alpha = rise.value
            if (!calm) translationY = (1f - rise.value) * 6.dp.toPx()
        }.padding(12.dp),
        style = Type.Meta.copy(color = Ink.Three),
    )
}

@Composable
private fun AnimatedVisibilityScope.Action(
    index: Int,
    icon: Painter,
    label: String,
    tint: Color,
    calm: Boolean,
    onClick: () -> Unit,
) {
    val p = transition.animateFloat(
        { if (targetState == EnterExitState.Visible && !calm) tween(Motion.M2, delayMillis = 30 * (index + 1), easing = Motion.Out) else snap(if (calm) 0 else 140) },
        label = "action",
    ) { if (it == EnterExitState.Visible) 1f else 0f }
    Row(
        Modifier.fillMaxWidth()
            .height(48.dp)
            .graphicsLayer {
                if (calm) return@graphicsLayer
                alpha = p.value
                translationY = (1f - p.value) * 6.dp.toPx()
            }
            .pressable(onClick, Shapes.ControlLg, Color.Transparent, Tone.Raise, depth = 1f, feel = null)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(icon, null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(tint))
        Spacer(Modifier.width(12.dp))
        BasicText(label, style = Type.Body.copy(color = tint, fontWeight = FontWeight.Medium))
    }
}

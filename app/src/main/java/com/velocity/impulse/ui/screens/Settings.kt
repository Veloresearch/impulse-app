package com.velocity.impulse.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.impulse.BuildConfig
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Link
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.LocalShell
import com.velocity.impulse.ui.components.Avatar
import com.velocity.impulse.ui.components.Btn
import com.velocity.impulse.ui.components.Disclosure
import com.velocity.impulse.ui.components.Feel
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Group
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.ListRow
import com.velocity.impulse.ui.components.OnAccent
import com.velocity.impulse.ui.components.Roll
import com.velocity.impulse.ui.components.Segmented
import com.velocity.impulse.ui.components.Sheet
import com.velocity.impulse.ui.components.Toggle
import com.velocity.impulse.ui.components.Variant
import com.velocity.impulse.ui.components.feel
import com.velocity.impulse.ui.components.morphText
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.theme.Accents
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Radius
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Signal
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Squircle
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import com.velocity.impulse.ui.theme.accentOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val Source = "https://github.com/Veloresearch/impulse-app"

/** The effort values Impulse stores, in the order the slider shows them. */
private val Levels = listOf("off", "low", "medium", "high", "max")

/** Text sizes the slider stops at, 85% to 130% in steps of 5. */
private val Sizes = List(10) { 0.85f + it * 0.05f }
private val Knob = 26.dp
private val TitleStyle = Type.Brand.copy(fontSize = 36.sp, lineHeight = 42.sp)

private data class License(val name: String, val license: String, val url: String)

private val Licenses = listOf(
    License("Inter", "SIL Open Font License 1.1", "https://github.com/rsms/inter/blob/master/LICENSE.txt"),
    License("JetBrains Mono", "SIL Open Font License 1.1", "https://github.com/JetBrains/JetBrainsMono/blob/master/OFL.txt"),
    License("Phosphor Icons", "MIT License", "https://github.com/phosphor-icons/core/blob/main/LICENSE"),
)

@Composable
fun SettingsPage(m: AppModel, scope: AnimatedVisibilityScope, scroll: ScrollState) {
    val uri = LocalUriHandler.current
    var signOut by remember { mutableStateOf(false) }
    var leave by remember { mutableStateOf(false) }
    var clear by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Page(scroll) {
            Title(tr("Settings", "Ustawienia"), scope)
            val me = m.me
            val name = me?.name?.ifBlank { null } ?: "Impulse"
            Header(m, name, Modifier.arrive(0)) { Avatar(name, me?.avatar, me?.photo, 64.dp) }
            Group(tr("Chat", "Czat"), Modifier.arrive(1)) {
                Switch(tr("Show thinking", "Pokazuj myślenie"), tr("The model's reasoning, folded above each answer", "Rozumowanie modelu, zwinięte nad odpowiedzią"), m.showThinking) { m.setThinking(it) }
                Effort(m)
                Switch(tr("Enter sends", "Enter wysyła"), tr("Without it, Enter starts a new line", "Bez tego Enter zaczyna nową linię"), m.enterSends) { m.pickEnterSends(it) }
                Switch(
                    tr("Web search", "Wyszukiwanie w sieci"),
                    tr("New chats can search the web and read pages. Each chat can change it.", "Nowe czaty mogą szukać w sieci i czytać strony. Każdy czat może to zmienić."),
                    m.webDefault,
                ) { m.pickWebDefault(it) }
                val tools = m.plugins.size
                Switch(
                    tr("Plugins", "Wtyczki"),
                    when (tools) {
                        0 -> tr("The computer has no plugins yet", "Komputer nie ma jeszcze wtyczek")
                        1 -> tr("New chats can use 1 plugin tool from the computer", "Nowe czaty mogą używać 1 narzędzia wtyczek z komputera")
                        else -> tr("New chats can use $tools plugin tools from the computer", "Nowe czaty mogą używać $tools narzędzi wtyczek z komputera")
                    },
                    m.pluginsDefault,
                ) { m.pickPluginsDefault(it) }
                Switch(tr("Keep screen on", "Ekran zawsze włączony"), tr("While an answer is being written", "Gdy powstaje odpowiedź"), m.keepAwake) { m.pickKeepAwake(it) }
            }
            Group(tr("Appearance", "Wygląd"), Modifier.arrive(2)) {
                AccentPicker(m)
                TextSize(m)
                Switch(tr("Reduce motion", "Ogranicz ruch"), tr("Cross-fades instead of movement", "Przenikanie zamiast ruchu"), m.reduceMotion) { m.calmMotion(it) }
                Switch(tr("Haptics", "Wibracje"), tr("Light taps under your finger as things move", "Lekkie stuknięcia pod palcem, gdy coś się rusza"), m.haptics) { m.pickHaptics(it) }
            }
            Group(tr("Computer", "Komputer"), Modifier.arrive(3)) { ComputerRows(m) { leave = true } }
            Btn(tr("Sign out", "Wyloguj się"), { signOut = true }, Modifier.fillMaxWidth().arrive(3), Variant.Danger, Glyph.SignOut)
            Group(tr("Storage", "Pamięć"), Modifier.arrive(4)) {
                val n = m.chats.size
                Line(
                    tr("Clear local cache", "Wyczyść pamięć podręczną"),
                    if (n == 1) tr("1 chat kept on this phone", "1 czat na tym telefonie") else tr("$n chats kept on this phone", "Czaty na tym telefonie: $n"),
                    { clear = true },
                ) {
                    Icon(Glyph.Trash, Ink.Three, size = 20.dp)
                }
            }
            Group(tr("About", "O aplikacji"), Modifier.arrive(5)) {
                ListRow("Impulse for Android", detail = tr("Version", "Wersja") + " " + BuildConfig.VERSION_NAME)
                Line(tr("Source code", "Kod źródłowy"), Source.substringAfter("://"), { uri.openUri(Source) }) {
                    Icon(Glyph.Right, Ink.Four, size = 16.dp)
                }
                Disclosure(tr("Open-source licenses", "Licencje open source")) {
                    Licenses.forEach { l ->
                        Row(
                            Modifier.fillMaxWidth()
                                .pressable({ uri.openUri(l.url) }, Shapes.ControlLg, Color.Transparent, Tone.Raise, depth = 1f)
                                .padding(horizontal = Space.S2, vertical = Space.S2),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                BasicText(l.name, style = Type.Body)
                                BasicText(l.license, style = Type.Meta)
                            }
                            Icon(Glyph.Right, Ink.Four, size = 16.dp)
                        }
                    }
                }
            }
        }
        Leaving(m, signOut, leave) { signOut = false; leave = false }
        Confirm(
            clear, { clear = false },
            tr("Clear local cache?", "Wyczyścić pamięć podręczną?"),
            tr(
                "This phone drops its copy of your chats and pulls them again from the computer. Changes the computer hasn't received yet are lost.",
                "Telefon usunie swoją kopię czatów i pobierze je ponownie z komputera. Zmiany, których komputer jeszcze nie dostał, przepadną.",
            ),
            tr("Clear cache", "Wyczyść"),
        ) { m.clearCache() }
    }
}

@Composable
fun ComputerPage(m: AppModel, scope: AnimatedVisibilityScope, scroll: ScrollState) {
    var signOut by remember { mutableStateOf(false) }
    var leave by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Page(scroll) {
            Title(tr("Computer", "Komputer"), scope)
            Header(m, tr("Your computer", "Twój komputer"), Modifier.arrive(0)) {
                Box(Modifier.size(64.dp).background(Tone.Raise, Shapes.Tile), contentAlignment = Alignment.Center) {
                    Icon(linkGlyph(m.link), Ink.Two, size = 28.dp)
                }
            }
            Group(modifier = Modifier.arrive(1)) { ComputerRows(m) { leave = true } }
            Group(tr("Account", "Konto"), Modifier.arrive(2)) {
                ListRow(tr("Signed in as", "Zalogowano jako"), detail = m.me?.name)
            }
            Btn(tr("Sign out", "Wyloguj się"), { signOut = true }, Modifier.fillMaxWidth().arrive(2), Variant.Danger, Glyph.SignOut)
        }
        Leaving(m, signOut, leave) { signOut = false; leave = false }
    }
}

@Composable
private fun Title(text: String, scope: AnimatedVisibilityScope) {
    BasicText(text, Modifier.morphText(LocalShell.current.pageOrigin, scope), style = TitleStyle)
}

/** Who or what the page is about, the computer it reaches, and whether that computer answers. */
@Composable
private fun Header(m: AppModel, title: String, modifier: Modifier, picture: @Composable () -> Unit) {
    val host = m.server?.base?.substringAfter("://")?.trimEnd('/').orEmpty()
    val dot by animateColorAsState(
        when (m.online) {
            true -> Signal.Ok
            false -> Signal.Err
            null -> Ink.Four
        },
        tween(Motion.M2), label = "dot",
    )
    Row(modifier.fillMaxWidth().background(Tone.Panel, Shapes.Card).padding(Space.S4), verticalAlignment = Alignment.CenterVertically) {
        picture()
        Spacer(Modifier.width(Space.S4))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(title, style = Type.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (host.isNotEmpty()) BasicText(host, style = Type.Mono.copy(color = Ink.Three), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row {
                Box(Modifier.padding(top = 6.dp).size(7.dp).background(dot, CircleShape))
                Spacer(Modifier.width(Space.S2))
                Roll(
                    when (m.online) {
                        true -> tr("Connected over ${linkLabel(m.link)}", "Połączono: ${linkLabel(m.link)}")
                        false -> tr("Can't reach the computer. Check that Impulse is open and Settings › Phone is on.", "Brak połączenia z komputerem. Sprawdź, czy Impulse jest otwarty, a Ustawienia › Telefon włączone.")
                        null -> tr("Connecting", "Łączenie")
                    },
                    Type.Meta, Modifier.weight(1f), maxLines = 3,
                )
            }
        }
    }
}

@Composable
private fun Effort(m: AppModel) {
    val at = Levels.indexOf(m.effort ?: "medium").coerceAtLeast(0)
    val notes = listOf(
        tr("Answers straight away", "Odpowiada od razu"),
        tr("A quick think before answering", "Krótko myśli przed odpowiedzią"),
        tr("Thinks each answer through", "Przemyśla każdą odpowiedź"),
        tr("Takes longer on hard questions", "Dłużej myśli nad trudnymi pytaniami"),
        tr("Thinks as long as it needs", "Myśli tak długo, jak trzeba"),
    )
    Column(Modifier.fillMaxWidth().background(Tone.Panel).padding(Space.S4)) {
        BasicText(tr("Default thinking effort", "Domyślny wysiłek myślenia"), style = Type.Body.copy(fontWeight = FontWeight.Medium))
        Roll(notes[at], Type.Meta)
        Spacer(Modifier.height(Space.S3))
        Segmented(
            listOf(tr("Instant", "Od razu"), tr("Low", "Krótko"), tr("Medium", "Średnio"), tr("High", "Dużo"), "Max"),
            at,
            { m.pickEffort(Levels[it]) },
        )
    }
}

/** Large swatches; one ring travels to the chosen one and stretches along the way. */
@Composable
private fun AccentPicker(m: AppModel) {
    val names = remember { Accents.keys.toList() }
    val picked = names.indexOf(m.accent).coerceAtLeast(0)
    val calm = LocalShell.current.calm
    val density = LocalDensity.current
    Column(Modifier.fillMaxWidth().background(Tone.Panel).padding(Space.S4)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(tr("Accent", "Akcent"), Modifier.weight(1f), style = Type.Body.copy(fontWeight = FontWeight.Medium))
            Roll(names[picked].replaceFirstChar { it.uppercase() }, Type.Meta)
        }
        Spacer(Modifier.height(Space.S3))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cols = 4
            val gap = Space.S3
            val tall = 52.dp
            val cell = (maxWidth - gap * (cols - 1)) / cols
            fun spot(i: Int) = with(density) { Offset(((cell + gap) * (i % cols)).toPx(), ((tall + gap) * (i / cols)).toPx()) }
            val ring = remember { Animatable(spot(picked), Offset.VectorConverter) }
            LaunchedEffect(picked, cell) {
                if (calm) ring.snapTo(spot(picked)) else ring.animateTo(spot(picked), spring(0.72f, 380f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                names.chunked(cols).forEachIndexed { r, row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        row.forEachIndexed { c, name ->
                            Swatch(accentOf(name), r * cols + c == picked, Modifier.weight(1f).height(tall)) { m.pickAccent(name) }
                        }
                    }
                }
            }
            val out = 4.dp
            Box(
                Modifier.size(cell + out * 2, tall + out * 2)
                    .graphicsLayer {
                        val p = ring.value
                        val v = ring.velocity
                        val pad = out.toPx()
                        translationX = p.x - pad
                        translationY = p.y - pad
                        val sx = (abs(v.x) / 8000f).coerceAtMost(0.2f)
                        val sy = (abs(v.y) / 8000f).coerceAtMost(0.2f)
                        scaleX = 1f + sx - sy * 0.4f
                        scaleY = 1f + sy - sx * 0.4f
                    }
                    .border(2.dp, Ink.One, Squircle(Radius.ControlLg + out)),
            )
        }
    }
}

@Composable
private fun Swatch(color: Color, on: Boolean, modifier: Modifier, onPick: () -> Unit) {
    val mark = animateFloatAsState(if (on) 1f else 0f, Motion.press(), label = "mark")
    Box(
        modifier.pressable(onPick, Shapes.ControlLg, color, color, depth = 0.9f, feel = Feel.Tick, stretch = true),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Glyph.Check, OnAccent,
            Modifier.graphicsLayer {
                val k = mark.value
                alpha = k.coerceIn(0f, 1f)
                scaleX = 0.4f + 0.6f * k
                scaleY = scaleX
            },
            size = 20.dp,
        )
    }
}

/**
 * Steps from 85% to 130%. The knob follows the finger between stops, ticks at each one and settles
 * on the nearest when let go; only then is the size kept, so the app does not resize mid-drag.
 */
@Composable
private fun TextSize(m: AppModel) {
    val view = LocalView.current
    val accent = LocalAccent.current
    val calm = LocalShell.current.calm
    val scope = rememberCoroutineScope()
    val last = Sizes.lastIndex
    var at by remember { mutableIntStateOf(((m.textScale - Sizes[0]) / 0.05f).roundToInt().coerceIn(0, last)) }
    val pos = remember { Animatable(at.toFloat()) }
    var held by remember { mutableStateOf(false) }
    val swell by animateFloatAsState(if (held) 1f else 0f, spring(0.6f, 500f), label = "swell")
    fun follow(x: Float) {
        scope.launch { if (calm) pos.snapTo(x) else pos.animateTo(x, spring(1f, 1600f)) }
    }
    fun settle(i: Int) {
        at = i
        if (Sizes[i] != m.textScale) m.pickTextScale(Sizes[i])
        scope.launch { if (calm) pos.snapTo(i.toFloat()) else pos.animateTo(i.toFloat(), spring(0.85f, 520f)) }
    }
    val percent = "${(Sizes[at] * 100).roundToInt()}%"
    Column(Modifier.fillMaxWidth().background(Tone.Panel).padding(Space.S4)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(tr("Text size", "Rozmiar tekstu"), Modifier.weight(1f), style = Type.Body.copy(fontWeight = FontWeight.Medium))
            Roll(percent, Type.Meta, digits = true)
        }
        Spacer(Modifier.height(Space.S3))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText("A", style = Type.Meta.copy(fontSize = 12.sp))
            Spacer(Modifier.width(Space.S3))
            Box(
                Modifier.weight(1f).height(36.dp)
                    .semantics {
                        stateDescription = percent
                        progressBarRangeInfo = ProgressBarRangeInfo(Sizes[at], Sizes[0]..Sizes[last], steps = last - 1)
                        setProgress { v -> settle(((v - Sizes[0]) / 0.05f).roundToInt().coerceIn(0, last)); true }
                    }
                    .pointerInput(calm) {
                        detectTapGestures { o ->
                            val k = Knob.toPx()
                            val i = ((o.x - k / 2) / (size.width - k).coerceAtLeast(1f) * last).roundToInt().coerceIn(0, last)
                            if (i != at) view.feel(Feel.Tick)
                            settle(i)
                        }
                    }
                    .pointerInput(calm) {
                        fun stop(px: Float): Float {
                            val k = Knob.toPx()
                            return ((px - k / 2) / (size.width - k).coerceAtLeast(1f) * last).coerceIn(0f, last.toFloat())
                        }
                        var finger = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { o ->
                                held = true
                                finger = o.x
                                follow(stop(finger))
                            },
                            onDragEnd = { held = false; settle(at) },
                            onDragCancel = { held = false; settle(at) },
                        ) { c, dx ->
                            c.consume()
                            finger += dx
                            val x = stop(finger)
                            follow(x)
                            val i = x.roundToInt()
                            if (i != at) {
                                at = i
                                view.feel(Feel.Tick)
                            }
                        }
                    }
                    .drawBehind {
                        val r = Knob.toPx() / 2
                        val run = size.width - r * 2
                        val cy = size.height / 2
                        val cx = r + run * pos.value / last
                        val h = 6.dp.toPx()
                        val corner = CornerRadius(h / 2)
                        drawRoundRect(Tone.Inset, Offset(0f, cy - h / 2), Size(size.width, h), corner)
                        drawRoundRect(accent, Offset(0f, cy - h / 2), Size(cx + h / 2, h), corner)
                        val dot = 1.5.dp.toPx()
                        for (i in 0..last) {
                            val tx = r + run * i / last
                            drawCircle(if (tx <= cx) OnAccent.copy(alpha = 0.4f) else Ink.Four, dot, Offset(tx, cy))
                        }
                        val k = r * (1f + 0.14f * swell)
                        drawCircle(Color.Black.copy(alpha = 0.3f), k, Offset(cx, cy + 1.5.dp.toPx()))
                        drawCircle(Color.White, k, Offset(cx, cy))
                    },
            )
            Spacer(Modifier.width(Space.S3))
            BasicText("A", style = Type.Meta.copy(fontSize = 18.sp, color = Ink.Two))
        }
        Spacer(Modifier.height(Space.S3))
        // The phone's own font scale only, so the sample shows the chosen size whatever the app applies around it.
        val plain = Density(LocalDensity.current.density, LocalConfiguration.current.fontScale)
        CompositionLocalProvider(LocalDensity provides plain) {
            val s = Sizes[at]
            Box(Modifier.fillMaxWidth().height(with(plain) { (Type.Body.lineHeight * Sizes[last] * 2).toDp() })) {
                BasicText(
                    tr("This is how an answer reads at this size.", "Tak wygląda odpowiedź w tym rozmiarze."),
                    style = Type.Body.copy(color = Ink.Two, fontSize = Type.Body.fontSize * s, lineHeight = Type.Body.lineHeight * s),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A setting that is on or off; the whole row flips it. */
@Composable
private fun Switch(title: String, detail: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Line(title, detail, { onChange(!on) }, feel = if (on) Feel.Off else Feel.On) {
        Toggle(on, onChange)
    }
}

/** Rows of a Computer group: where it is, how it is reached, what it holds, and the way out. */
@Composable
private fun ComputerRows(m: AppModel, onSwitch: () -> Unit) {
    Address(m)
    ListRow(tr("Connection", "Połączenie"), detail = linkLabel(m.link)) {
        Icon(linkGlyph(m.link), Ink.Three, size = 20.dp)
    }
    ListRow(tr("Models", "Modele"), detail = tr("In the computer's models folder", "W folderze modeli na komputerze")) {
        Roll("${m.library.size}", Type.Body.copy(color = Ink.Two), digits = true)
    }
    Reconnect(m)
    Line(tr("Switch computer", "Zmień komputer"), tr("Connect this phone to a different computer", "Połącz telefon z innym komputerem"), onSwitch) {
        Icon(Glyph.Right, Ink.Four, size = 16.dp)
    }
}

@Composable
private fun Address(m: AppModel) {
    val address = m.server?.base?.substringAfter("://")?.trimEnd('/').orEmpty()
    val clipboard = LocalClipboardManager.current
    val accent = LocalAccent.current
    var copies by remember { mutableIntStateOf(0) }
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copies) {
        if (copies == 0) return@LaunchedEffect
        copied = true
        delay(1400)
        copied = false
    }
    val flip = animateFloatAsState(if (copied) 1f else 0f, Motion.press(), label = "copied")
    Line(
        tr("Address", "Adres"),
        if (copied) tr("Copied", "Skopiowano") else address,
        {
            clipboard.setText(AnnotatedString(address))
            copies++
        },
        feel = Feel.Confirm,
        detailStyle = if (copied) Type.Meta.copy(color = accent) else Type.Mono.copy(color = Ink.Three),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Glyph.Copy, Ink.Three, Modifier.graphicsLayer { val k = flip.value; alpha = (1f - k).coerceIn(0f, 1f); scaleX = 1f - 0.4f * k; scaleY = scaleX }, size = 18.dp)
            Icon(Glyph.Check, accent, Modifier.graphicsLayer { val k = flip.value; alpha = k.coerceIn(0f, 1f); scaleX = 0.6f + 0.4f * k; scaleY = scaleX }, size = 18.dp)
        }
    }
}

/** Asks the computer again; the arrows turn until it answers and the hand feels how it went. */
@Composable
private fun Reconnect(m: AppModel) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val calm = LocalShell.current.calm
    val accent = LocalAccent.current
    val spin = remember { Animatable(0f) }
    var checking by remember { mutableStateOf(false) }
    val tint by animateColorAsState(if (checking) accent else Ink.Three, tween(Motion.M2), label = "tint")
    Line(
        tr("Reconnect", "Połącz ponownie"),
        if (checking) tr("Checking the computer", "Sprawdzam komputer") else tr("Ask the computer how it is doing", "Zapytaj komputer o stan"),
        {
            if (!checking) {
                checking = true
                scope.launch {
                    val turning = if (calm) null else launch { while (true) spin.animateTo(spin.value + 360f, tween(800, easing = LinearEasing)) }
                    m.refreshAll().join()
                    turning?.cancel()
                    if (!calm) spin.animateTo(ceil(spin.value / 360f) * 360f, spring(1f, 220f))
                    checking = false
                    view.feel(if (m.online == true) Feel.Confirm else Feel.Reject)
                }
            }
        },
    ) {
        Icon(Glyph.Refresh, tint, Modifier.graphicsLayer { rotationZ = spin.value }, size = 20.dp)
    }
}

/** A tappable row on the group's ground. Its detail rolls when it changes; the row itself never moves. */
@Composable
private fun Line(
    title: String,
    detail: String,
    onClick: () -> Unit,
    feel: Feel? = Feel.Tap,
    detailStyle: TextStyle = Type.Meta,
    trailing: @Composable RowScope.() -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .pressable(onClick, RectangleShape, Tone.Panel, Tone.Raise, depth = 1f, feel = feel)
            .padding(horizontal = Space.S4, vertical = Space.S3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(title, style = Type.Body.copy(fontWeight = FontWeight.Medium))
            Roll(detail, detailStyle, maxLines = 2)
        }
        Spacer(Modifier.width(Space.S3))
        trailing()
    }
}

@Composable
private fun Leaving(m: AppModel, signOut: Boolean, leave: Boolean, onClose: () -> Unit) {
    Confirm(
        signOut, onClose,
        tr("Sign out?", "Wylogować?"),
        tr("Sign out of Impulse on this phone? Your chats stay on the computer.", "Wylogować z Impulse na tym telefonie? Czaty zostają na komputerze."),
        tr("Sign out", "Wyloguj się"),
    ) { m.signOut() }
    Confirm(
        leave, onClose,
        tr("Switch computer?", "Zmienić komputer?"),
        tr("This phone forgets this computer and its copy of your chats. Chats stay on the computer.", "Telefon zapomni ten komputer i swoją kopię czatów. Czaty zostają na komputerze."),
        tr("Switch computer", "Zmień komputer"),
    ) { m.forgetServer() }
}

@Composable
private fun Confirm(open: Boolean, onClose: () -> Unit, title: String, body: String, action: String, onAction: () -> Unit) {
    Sheet(open, onClose) {
        Column(Modifier.padding(top = Space.S2), verticalArrangement = Arrangement.spacedBy(Space.S2)) {
            BasicText(title, style = Type.Title)
            BasicText(body, style = Type.Body.copy(color = Ink.Three))
            Spacer(Modifier.height(Space.S4))
            Btn(action, { onClose(); onAction() }, Modifier.fillMaxWidth(), Variant.Danger)
            Btn(tr("Cancel", "Anuluj"), onClose, Modifier.fillMaxWidth(), Variant.Quiet)
        }
    }
}

private fun linkGlyph(l: Link?) = when (l) {
    Link.Local -> Glyph.Wifi
    Link.Tailscale -> Glyph.Shield
    Link.Cloudflare -> Glyph.Cloud
    else -> Glyph.Globe
}

/** Each block rises in after the one above it, on a spring with no overshoot, so text never bounces. */
@Composable
private fun Modifier.arrive(order: Int): Modifier {
    val calm = LocalShell.current.calm
    val lift = with(LocalDensity.current) { 28.dp.toPx() }
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(80L + 55L * order)
        p.animateTo(1f, if (calm) tween<Float>(Motion.M2) else spring<Float>(1f, 240f))
    }
    return graphicsLayer {
        val k = p.value
        alpha = k
        if (calm) return@graphicsLayer
        translationY = (1f - k) * lift * (1f + 0.2f * order)
        rotationX = (1f - k) * 6f
        cameraDistance = 16f * density
        transformOrigin = TransformOrigin(0.5f, 0f)
    }
}

package com.velocity.impulse.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.impulse.BuildConfig
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.ChatStyle
import com.velocity.impulse.data.Device
import com.velocity.impulse.data.KeepAwake
import com.velocity.impulse.data.Link
import com.velocity.impulse.data.Personas
import com.velocity.impulse.data.Release
import com.velocity.impulse.data.Updater
import com.velocity.impulse.data.strings
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.LocalShell
import com.velocity.impulse.ui.Pill
import com.velocity.impulse.ui.components.AfterLanding
import com.velocity.impulse.ui.components.LocalPresentation
import com.velocity.impulse.ui.components.Avatar
import com.velocity.impulse.ui.components.Btn
import com.velocity.impulse.ui.components.Choice
import com.velocity.impulse.ui.components.Disclosure
import com.velocity.impulse.ui.components.Feel
import com.velocity.impulse.ui.components.Field
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Group
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.IconBtn
import com.velocity.impulse.ui.components.ListRow
import com.velocity.impulse.ui.components.Markdown
import com.velocity.impulse.ui.components.OnAccent
import com.velocity.impulse.ui.components.Roll
import com.velocity.impulse.ui.components.Segmented
import com.velocity.impulse.ui.components.Sheet
import com.velocity.impulse.ui.components.Toggle
import com.velocity.impulse.ui.components.Variant
import com.velocity.impulse.ui.components.feel
import com.velocity.impulse.ui.components.glass
import com.velocity.impulse.ui.components.glassSource
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
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val Source = "https://github.com/Veloresearch/impulse-app"

/** The effort values Impulse stores, in the order the slider shows them. */
private val Levels = listOf("off", "low", "medium", "high", "max")

/** Text sizes the slider stops at, 85% to 130% in steps of 5. */
private val Sizes = List(10) { 0.85f + it * 0.05f }
private val KnobSize = 26.dp
private val TitleStyle get() = Type.Brand.copy(fontSize = 36.sp, lineHeight = 42.sp)
private val Capsule = RoundedCornerShape(percent = 50)
private val Medium get() = Type.Body.copy(fontWeight = FontWeight.Medium)

/** The panel's custom-instructions cap: 400 tokens at about 3.6 characters each. */
private const val InstructionChars = 1440

/** The tofus and emoji a profile can wear, as the service stores them. */
private val Tofus = listOf("classic", "sly", "cool", "nerd", "sleepy", "wink", "dj", "royal", "kitty", "sprout", "midnight", "artist")
private val Emoji = listOf("😀", "😎", "🤓", "🥳", "🤖", "👾", "🦊", "🐱", "🐼", "🐸", "🦄", "🐙", "🌵", "🍀", "🌈", "🔥", "⚡", "🚀", "🎧", "🎨", "📚", "☕", "🍕", "⭐")

private fun emojiId(e: String) = "e-" + e.codePoints().toArray().joinToString("-") { Integer.toHexString(it) }

private data class License(val name: String, val license: String, val url: String)

private val Licenses = listOf(
    License("Inter", "SIL Open Font License 1.1", "https://github.com/rsms/inter/blob/master/LICENSE.txt"),
    License("JetBrains Mono", "SIL Open Font License 1.1", "https://github.com/JetBrains/JetBrainsMono/blob/master/OFL.txt"),
    License("Phosphor Icons", "MIT License", "https://github.com/phosphor-icons/core/blob/main/LICENSE"),
)

@Composable
fun SettingsPage(m: AppModel, scroll: ScrollState) {
    val uri = LocalUriHandler.current
    val x = rememberMachine(m)
    var signOut by remember { mutableStateOf(false) }
    var leave by remember { mutableStateOf(false) }
    var clear by remember { mutableStateOf(false) }
    var restart by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf(false) }
    var device by remember { mutableStateOf<Device?>(null) }
    var dropping by remember { mutableStateOf(false) }
    val haze = remember { HazeState() }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().glassSource(haze)) {
            Page(scroll) {
                Title(tr("Settings", "Ustawienia"))
                val me = m.me
                val name = me?.name?.ifBlank { null } ?: "Impulse"
                Header(m, name, Modifier.arrive(0)) { Avatar(name, me?.avatar, me?.photo, 64.dp) }

                Section(tr("Appearance", "Wygląd"), 1) {
                    ThemePicker(m)
                    AccentPicker(m)
                    TextSize(m)
                    Switch(
                        tr("Reduce motion", "Ogranicz ruch"),
                        if (m.systemCalm) tr("On, because the phone's Remove animations is on", "Włączone, bo telefon ma wyłączone animacje")
                        else tr("Cross-fades instead of movement", "Przenikanie zamiast ruchu"),
                        m.calm,
                    ) { m.calmMotion(it) }
                    Switch(tr("Haptics", "Wibracje"), tr("Light taps under your finger as things move", "Lekkie stuknięcia pod palcem, gdy coś się rusza"), m.haptics) { m.pickHaptics(it) }
                }

                AfterLanding {
                    Section(tr("Personality", "Osobowość"), 2, tr("Who answers you, here and on your computer.", "Kto ci odpowiada, tu i na komputerze.")) { Personality(m) }

                    Section(tr("Answers", "Odpowiedzi"), 3, tr("Shared with Impulse on your computer.", "Wspólne z Impulse na komputerze.")) { AnswerRows(m) }

                    Section(tr("Model", "Model"), 4, tr("Defaults for every chat. A chat can set its own in its details.", "Domyślne dla każdego czatu. Czat może mieć własne w szczegółach.")) {
                        Effort(m)
                        Samplers.forEach { key(it.key) { Dial(m, it) } }
                        Seed(m)
                        Stops(m)
                    }
                    Btn(
                        tr("Reset to defaults", "Przywróć domyślne"),
                        {
                            AppModel.SAMPLING.forEach { if (m.sampling.has(it)) m.setSampling(it, null) }
                            if (m.effort != null && m.effort != "medium") m.pickEffort("medium")
                        },
                        Modifier.fillMaxWidth().arrive(4),
                        Variant.Standard,
                        Glyph.Refresh,
                        enabled = m.sampling.length() > 0 || (m.effort != null && m.effort != "medium"),
                    )

                    Section(tr("Chat", "Czat"), 5) { ChatRows(m) }

                    Section(tr("Account", "Konto"), 6) { AccountRows(m, name) { password = true } }

                    Section(tr("Computer", "Komputer"), 7) { ComputerRows(m, x, { leave = true }, { restart = true }) }

                    Section(tr("Signed-in devices", "Zalogowane urządzenia"), 8, tr("Phones and browsers that reach this computer from outside it.", "Telefony i przeglądarki, które łączą się z komputerem z zewnątrz.")) {
                        DeviceRows(x) { device = it; dropping = true }
                    }
                    Btn(tr("Sign out", "Wyloguj się"), { signOut = true }, Modifier.fillMaxWidth().arrive(8), Variant.Danger, Glyph.SignOut)

                    Section(tr("Storage", "Pamięć"), 9) {
                        val n = m.chats.size
                        Line(
                            tr("Clear local cache", "Wyczyść pamięć podręczną"),
                            if (n == 1) tr("1 chat kept on this phone", "1 czat na tym telefonie") else tr("$n chats kept on this phone", "Czaty na tym telefonie: $n"),
                            { clear = true },
                        ) {
                            Icon(Glyph.Trash, Ink.Three, size = 20.dp)
                        }
                    }

                    Section(tr("Updates", "Aktualizacje"), 10) { UpdateRows() }

                    Section(tr("About", "O aplikacji"), 11) {
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
                    if (Updater.release != null) Spacer(Modifier.height(IslandHeight + Space.S3))
                }
            }
        }
        UpdateIsland(haze)
        Leaving(m, signOut, leave) { signOut = false; leave = false }
        Restart(m, restart) { restart = false }
        DropDevice(m, x, device, dropping) { dropping = false }
        PasswordSheet(m, password) { password = false }
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
fun ComputerPage(m: AppModel, scroll: ScrollState) {
    val x = rememberMachine(m)
    var signOut by remember { mutableStateOf(false) }
    var leave by remember { mutableStateOf(false) }
    var restart by remember { mutableStateOf(false) }
    var device by remember { mutableStateOf<Device?>(null) }
    var dropping by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Page(scroll) {
            Title(tr("Computer", "Komputer"))
            Header(m, tr("Your computer", "Twój komputer"), Modifier.arrive(0)) {
                Box(Modifier.size(64.dp).background(Tone.Raise, Shapes.Tile), contentAlignment = Alignment.Center) {
                    Icon(linkGlyph(m.link), Ink.Two, size = 28.dp)
                }
            }
            Group(modifier = Modifier.arrive(1)) { ComputerRows(m, x, { leave = true }, { restart = true }) }
            Section(tr("Signed-in devices", "Zalogowane urządzenia"), 2) { DeviceRows(x) { device = it; dropping = true } }
            Group(tr("Account", "Konto"), Modifier.arrive(3)) {
                ListRow(tr("Signed in as", "Zalogowano jako"), detail = m.me?.name)
            }
            Btn(tr("Sign out", "Wyloguj się"), { signOut = true }, Modifier.fillMaxWidth().arrive(3), Variant.Danger, Glyph.SignOut)
        }
        Leaving(m, signOut, leave) { signOut = false; leave = false }
        Restart(m, restart) { restart = false }
        DropDevice(m, x, device, dropping) { dropping = false }
    }
}

@Composable
private fun Title(text: String) {
    BasicText(text, style = TitleStyle)
}

/** A titled block of settings: its rows share one rounded container, 2dp apart. */
@Composable
private fun Section(title: String, order: Int, caption: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().arrive(order).padding(top = Space.S3)) {
        BasicText(title, Modifier.padding(start = Space.S1), style = Type.BrandSection)
        if (caption != null) BasicText(caption, Modifier.padding(start = Space.S1, top = 2.dp), style = Type.Meta)
        Spacer(Modifier.height(Space.S3))
        Group(content = content)
    }
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
                Box(Modifier.padding(top = 6.dp).size(7.dp).drawBehind { drawCircle(dot) })
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

private class Look(val ground: Color, val island: Color, val ink: Color)

private val DarkLook = Look(Color(0xFF1C1A1F), Color(0xFF2D2B32), Color(0xFFF1EFF3))
private val LightLook = Look(Color(0xFFF5F3F7), Color.White, Color(0xFF1C1A1F))
private val AmoledLook = Look(Color.Black, Color(0xFF19181C), Color(0xFFF1EFF3))

/** A chat in miniature: the top island, a question, an answer and the composer. */
private fun DrawScope.scene(l: Look, accent: Color) {
    val w = size.width
    val h = size.height
    fun bar(x: Float, y: Float, bw: Float, bh: Float, c: Color) =
        drawRoundRect(c, Offset(x * w, y * h), Size(bw * w, bh * h), CornerRadius(bh * h / 2))
    drawRect(l.ground)
    bar(0.3f, 0.07f, 0.4f, 0.07f, l.island)
    bar(0.42f, 0.22f, 0.48f, 0.09f, accent)
    val line = l.ink.copy(alpha = 0.5f)
    bar(0.1f, 0.39f, 0.72f, 0.035f, line)
    bar(0.1f, 0.46f, 0.8f, 0.035f, line)
    bar(0.1f, 0.53f, 0.5f, 0.035f, line)
    bar(0.08f, 0.81f, 0.84f, 0.11f, l.island)
}

/** Each theme as a small live chat; the ring travels to the chosen one. */
@Composable
private fun ThemePicker(m: AppModel) {
    val accent = LocalAccent.current
    val names = AppModel.THEMES
    val labels = listOf(tr("Dark", "Ciemny"), tr("Light", "Jasny"), "AMOLED", tr("Auto", "Auto"))
    val notes = listOf(
        tr("Soft dark, easy on the eyes", "Miękka ciemność, łagodna dla oczu"),
        tr("Bright, for daylight", "Jasny, na dzień"),
        tr("Pure black, kind to an OLED screen at night", "Czysta czerń, dobra dla ekranu OLED w nocy"),
        tr("Follows the phone's dark mode", "Za trybem ciemnym telefonu"),
    )
    val picked = names.indexOf(m.theme).coerceAtLeast(0)
    Column(Modifier.fillMaxWidth().background(Tone.Panel).padding(Space.S4)) {
        BasicText(tr("Theme", "Motyw"), style = Medium)
        Roll(notes[picked], Type.Meta)
        Spacer(Modifier.height(Space.S3))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = Space.S3
            val cell = (maxWidth - gap * (names.size - 1)) / names.size
            val tall = cell * 1.45f
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                names.forEach { name ->
                    Box(
                        Modifier.weight(1f).height(tall)
                            .pressable({ m.pickTheme(name) }, Shapes.ControlLg, depth = 0.93f, feel = Feel.Tick, tilt = 8f)
                            .clip(Shapes.ControlLg)
                            .drawBehind {
                                when (name) {
                                    "light" -> scene(LightLook, accent)
                                    "amoled" -> scene(AmoledLook, accent)
                                    "auto" -> {
                                        clipRect(right = size.width / 2) { scene(LightLook, accent) }
                                        clipRect(left = size.width / 2) { scene(DarkLook, accent) }
                                    }
                                    else -> scene(DarkLook, accent)
                                }
                            },
                    )
                }
            }
            Ring(picked, names.size, cell, tall, gap)
        }
        Spacer(Modifier.height(Space.S2))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.S3)) {
            labels.forEachIndexed { i, l ->
                val ink by animateColorAsState(if (i == picked) Ink.One else Ink.Three, tween(Motion.M2), label = "theme")
                BasicText(l, Modifier.weight(1f), style = Type.Label.copy(color = ink, textAlign = TextAlign.Center), maxLines = 1)
            }
        }
    }
}

/** Large swatches; one ring travels to the chosen one and stretches along the way. */
@Composable
private fun AccentPicker(m: AppModel) {
    val names = remember { Accents.keys.toList() }
    val picked = names.indexOf(m.accent).coerceAtLeast(0)
    Column(Modifier.fillMaxWidth().background(Tone.Panel).padding(Space.S4)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(tr("Accent", "Akcent"), Modifier.weight(1f), style = Medium)
            Roll(names[picked].replaceFirstChar { it.uppercase() }, Type.Meta)
        }
        Spacer(Modifier.height(Space.S3))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cols = 4
            val gap = Space.S3
            val tall = 52.dp
            val cell = (maxWidth - gap * (cols - 1)) / cols
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                names.chunked(cols).forEachIndexed { r, row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        row.forEachIndexed { c, name ->
                            Swatch(accentOf(name), r * cols + c == picked, Modifier.weight(1f).height(tall)) { m.pickAccent(name) }
                        }
                    }
                }
            }
            Ring(picked, cols, cell, tall, gap)
        }
    }
}

/** The ring around a picked cell of a grid; it travels on a spring and stretches with its speed. */
@Composable
private fun Ring(picked: Int, cols: Int, cell: Dp, tall: Dp, gap: Dp) {
    val calm = LocalShell.current.calm
    val density = LocalDensity.current
    fun spot(i: Int) = with(density) { Offset(((cell + gap) * (i % cols)).toPx(), ((tall + gap) * (i / cols)).toPx()) }
    val ring = remember { Animatable(spot(picked), Offset.VectorConverter) }
    LaunchedEffect(picked, cell) {
        if (calm) ring.snapTo(spot(picked)) else ring.animateTo(spot(picked), spring(0.72f, 380f))
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

/** Steps from 85% to 130%; the size is kept only when the knob settles, so the app does not resize mid-drag. */
@Composable
private fun TextSize(m: AppModel) {
    val last = Sizes.lastIndex
    var at by remember { mutableIntStateOf(((m.textScale - Sizes[0]) / 0.05f).roundToInt().coerceIn(0, last)) }
    val percent = "${(Sizes[at] * 100).roundToInt()}%"
    Column(Modifier.fillMaxWidth().background(Tone.Panel).padding(Space.S4)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(tr("Text size", "Rozmiar tekstu"), Modifier.weight(1f), style = Medium)
            Roll(percent, Type.Meta, digits = true)
        }
        Spacer(Modifier.height(Space.S3))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText("A", style = Type.Meta.copy(fontSize = 12.sp))
            Spacer(Modifier.width(Space.S3))
            Box(Modifier.weight(1f)) {
                Track(Sizes.size, at, percent, { at = it }) {
                    at = it
                    if (Sizes[it] != m.textScale) m.pickTextScale(Sizes[it])
                }
            }
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

/**
 * A slider over [stops] steps. The knob follows the finger between them, ticks at each one and
 * settles on the nearest when let go; [onMove] hears each step on the way, [onSettle] the last.
 * [dim] greys the fill for a value not set yet.
 */
@Composable
private fun Track(stops: Int, at: Int, said: String, onMove: (Int) -> Unit, dim: Boolean = false, onSettle: (Int) -> Unit) {
    val view = LocalView.current
    val accent = LocalAccent.current
    val calm = LocalShell.current.calm
    val scope = rememberCoroutineScope()
    val move by rememberUpdatedState(onMove)
    val keep by rememberUpdatedState(onSettle)
    val last = stops - 1
    val pos = remember { Animatable(at.toFloat()) }
    var held by remember { mutableStateOf(false) }
    var near by remember { mutableIntStateOf(at) }
    val swell = animateFloatAsState(if (held) 1f else 0f, spring(0.6f, 500f), label = "swell")
    val lit = animateFloatAsState(if (dim) 0f else 1f, tween(Motion.M2), label = "lit")
    fun go(x: Float, s: SpringSpec<Float>) {
        scope.launch { if (calm) pos.snapTo(x) else pos.animateTo(x, s) }
    }
    fun settle(i: Int) {
        near = i
        keep(i)
        go(i.toFloat(), spring(0.85f, 520f))
    }
    LaunchedEffect(at) {
        if (!held) {
            near = at
            go(at.toFloat(), spring(0.85f, 520f))
        }
    }
    Box(
        Modifier.fillMaxWidth().height(36.dp)
            .semantics {
                stateDescription = said
                progressBarRangeInfo = ProgressBarRangeInfo(at.toFloat(), 0f..last.toFloat(), steps = (last - 1).coerceAtLeast(0))
                setProgress { v -> settle(v.roundToInt().coerceIn(0, last)); true }
            }
            .pointerInput(calm, stops) {
                detectTapGestures { o ->
                    val k = KnobSize.toPx()
                    val i = ((o.x - k / 2) / (size.width - k).coerceAtLeast(1f) * last).roundToInt().coerceIn(0, last)
                    if (i != near) view.feel(Feel.Tick)
                    settle(i)
                }
            }
            .pointerInput(calm, stops) {
                fun stop(px: Float): Float {
                    val k = KnobSize.toPx()
                    return ((px - k / 2) / (size.width - k).coerceAtLeast(1f) * last).coerceIn(0f, last.toFloat())
                }
                var finger = 0f
                detectHorizontalDragGestures(
                    onDragStart = { o ->
                        held = true
                        finger = o.x
                        go(stop(finger), spring(1f, 1600f))
                    },
                    onDragEnd = { held = false; settle(near) },
                    onDragCancel = { held = false; settle(near) },
                ) { c, dx ->
                    c.consume()
                    finger += dx
                    val x = stop(finger)
                    go(x, spring(1f, 1600f))
                    val i = x.roundToInt()
                    if (i != near) {
                        near = i
                        move(i)
                        view.feel(Feel.Tick)
                    }
                }
            }
            .drawBehind {
                val r = KnobSize.toPx() / 2
                val run = size.width - r * 2
                val cy = size.height / 2
                val cx = r + run * pos.value / last.coerceAtLeast(1)
                val h = 6.dp.toPx()
                val corner = CornerRadius(h / 2)
                drawRoundRect(Tone.Inset, Offset(0f, cy - h / 2), Size(size.width, h), corner)
                drawRoundRect(lerp(Ink.Four, accent, lit.value), Offset(0f, cy - h / 2), Size(cx + h / 2, h), corner)
                if (stops <= 12) {
                    val dot = 1.5.dp.toPx()
                    for (i in 0..last) {
                        val tx = r + run * i / last
                        drawCircle(if (tx <= cx) OnAccent.copy(alpha = 0.4f) else Ink.Four, dot, Offset(tx, cy))
                    }
                }
                val k = r * (1f + 0.14f * swell.value)
                drawCircle(Color.Black.copy(alpha = 0.3f), k, Offset(cx, cy + 1.5.dp.toPx()))
                drawCircle(Color.White, k, Offset(cx, cy))
            },
    )
}

private fun sample(id: String) = when (id) {
    "sarcastic" -> tr("A loop inside a loop: every item meets every other one. Make the list a set and it runs in 0.1 s. Ten seconds of networking, gone 🙄", "Pętla w pętli: każdy element wita się z każdym. Zrób z listy zbiór i masz 0,1 s. Dziesięć sekund networkingu poszło w piach 🙄")
    "friendly" -> tr("Easy fix! The loop inside a loop is the slow part: turn the list into a set and it'll fly 🚀", "Łatwa sprawa! Zwalnia pętla w pętli: zamień listę na zbiór i poleci 🚀")
    "pro" -> tr("The nested loop is O(n²). Use a set for the lookups and it drops to about 0.1 s.", "Zagnieżdżona pętla ma złożoność O(n²). Użyj zbioru do wyszukiwania, a czas spadnie do ok. 0,1 s.")
    "nerd" -> tr("Classic O(n²)! A set is a hash table, so every lookup is O(1): 10 s becomes 0.1 s 🤓", "Klasyczne O(n²)! Zbiór to tablica haszująca, więc każde wyszukiwanie to O(1): z 10 s robi się 0,1 s 🤓")
    "chill" -> tr("No stress. Swap the list for a set and it's quick 😎", "Luz. Zamień listę na zbiór i będzie śmigać 😎")
    else -> tr("The nested loop compares every pair of items. Using a set for the lookups should make it much faster.", "Zagnieżdżona pętla porównuje każdą parę elementów. Zbiór do wyszukiwania powinien wyraźnie przyspieszyć skrypt.")
}

/** Off first, then the panel's personalities as faces, the same grid as a chat's details; below, how the chosen one answers. */
@Composable
private fun Personality(m: AppModel) {
    val accent = LocalAccent.current
    val chosen = m.style.persona
    (listOf("off") + Personas.map { it.id }).chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            row.forEach { id ->
                key(id) {
                    val p = Personas.firstOrNull { it.id == id }
                    val on = id == chosen
                    val fill by animateColorAsState(if (on) accent.copy(alpha = 0.16f) else Tone.Panel, label = "voice")
                    val lift = animateFloatAsState(if (on) 1f else 0f, spring(0.55f, 480f), label = "lift")
                    Column(
                        Modifier.weight(1f).heightIn(min = 88.dp)
                            .pressable({ m.setStyle("persona", id) }, RectangleShape, fill, Tone.Raise, depth = 0.95f, feel = Feel.Tick, tilt = 6f)
                            .padding(vertical = Space.S3, horizontal = Space.S2),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        BasicText(
                            p?.face ?: "🤖",
                            Modifier.graphicsLayer {
                                scaleX = 1f + 0.18f * lift.value
                                scaleY = scaleX
                                translationY = -3.dp.toPx() * lift.value
                            },
                            style = Type.Title.copy(color = Ink.One),
                        )
                        Spacer(Modifier.size(4.dp))
                        BasicText(
                            p?.let { tr(it.nameEn, it.namePl) } ?: tr("Off", "Wyłączona"),
                            style = Type.Label.copy(color = if (on) accent else Ink.Two, fontSize = 11.sp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
    val p = Personas.firstOrNull { it.id == chosen }
    Column(Modifier.fillMaxWidth().background(Tone.Panel).padding(Space.S4)) {
        Roll(p?.let { tr(it.hintEn, it.hintPl) } ?: tr("The model as its makers ship it.", "Model taki, jakim go wydano."), Type.Meta)
        Spacer(Modifier.height(Space.S3))
        BasicText(
            tr("Why does my script take 10 seconds?", "Czemu mój skrypt mieli 10 sekund?"),
            Modifier.align(Alignment.End).background(Tone.Raise, RoundedCornerShape(16.dp)).padding(horizontal = Space.S3, vertical = Space.S2),
            style = Type.Meta.copy(color = Ink.Two),
        )
        Spacer(Modifier.height(Space.S2))
        Roll(sample(chosen), Type.Body.copy(color = Ink.Two), maxLines = 4)
        if (!m.style.personaSet) {
            Spacer(Modifier.height(Space.S3))
            BasicText(
                tr("Until you pick one, it follows the loaded model: laid-back on small models, sarcastic on bigger ones.", "Dopóki jej nie wybierzesz, zależy od wczytanego modelu: na małych modelach wyluzowana, na większych sarkastyczna."),
                style = Type.Meta,
            )
        }
    }
}

@Composable
private fun AnswerRows(m: AppModel) {
    val s = m.style
    Options(
        tr("Detail", "Szczegółowość"), tr("How long and thorough answers are", "Jak długie i dokładne są odpowiedzi"),
        listOf(tr("Short", "Krótko"), tr("Balanced", "Wyważone"), tr("Detailed", "Szczegółowo")),
        listOf("concise", "balanced", "detailed").indexOf(s.tone),
    ) { m.setStyle("tone", listOf("concise", "balanced", "detailed")[it]) }
    Options(
        "Emoji", tr("How many emoji answers use", "Ile emoji w odpowiedziach"),
        listOf(tr("None", "Bez"), tr("Some", "Trochę"), tr("Lots", "Dużo")),
        listOf("none", "some", "lots").indexOf(s.emoji),
    ) { m.setStyle("emoji", listOf("none", "some", "lots")[it]) }
    Options(
        tr("Charts and diagrams", "Wykresy i diagramy"),
        if (s.visuals == "often") tr("A chart, diagram or table whenever an answer has numbers or steps", "Wykres, diagram lub tabela, gdy odpowiedź ma liczby albo kroki")
        else tr("When answers add a chart, diagram or table", "Kiedy odpowiedzi dodają wykres, diagram lub tabelę"),
        listOf(tr("When asked", "Na prośbę"), tr("When useful", "Gdy pomaga"), tr("Often", "Często")),
        listOf("asked", "auto", "often").indexOf(s.visuals),
    ) { m.setStyle("visuals", listOf("asked", "auto", "often")[it]) }
    Options(
        tr("Formatting", "Formatowanie"), tr("Headings and lists, or plain paragraphs", "Nagłówki i listy albo zwykłe akapity"),
        listOf(tr("Headings and lists", "Nagłówki i listy"), tr("Plain", "Zwykłe")),
        if (s.layout == "plain") 1 else 0,
    ) { m.setStyle("layout", if (it == 1) "plain" else "rich") }
    Choice(
        tr("Language", "Język"),
        listOf("auto") + ChatStyle.LANGUAGES.keys,
        s.lang,
        { if (it == "auto") tr("Same as my message", "Jak moja wiadomość") else ChatStyle.LANGUAGES[it].orEmpty() },
        { m.setStyle("lang", it) },
    )
    Choice(
        tr("Longest answer", "Najdłuższa odpowiedź"),
        ChatStyle.LIMITS,
        s.maxTokens,
        { if (it == 0) tr("As the window allows", "Ile zmieści okno") else tr("${it / 1024}K tokens", "${it / 1024}K tokenów") },
        { m.setStyle("maxTokens", it) },
    )
    Creativity(m)
    val name = m.me?.name.orEmpty()
    Switch(
        tr("Call me by my name", "Zwracaj się po imieniu"),
        if (name.isNotBlank()) tr("Uses your account name, $name", "Używa imienia z konta: $name") else tr("Add your name under Account first", "Najpierw dodaj imię w Koncie"),
        s.byName,
    ) { m.setStyle("byName", it) }
    Switch(
        tr("Name chats automatically", "Nazywaj czaty automatycznie"),
        tr("The model titles a chat after its first answer", "Model nadaje tytuł po pierwszej odpowiedzi"),
        s.autoTitle,
    ) { m.setStyle("autoTitle", it) }
    Instructions(m)
}

private val Warmth = listOf(null, 0.0, 0.3, 0.7, 1.0)

/** The panel's creativity: the app-wide temperature at a few named steps; the dial under Model sets it exactly. */
@Composable
private fun Creativity(m: AppModel) {
    val t = (m.sampling.opt("temperature") as? Number)?.toDouble()
    val at = if (t == null) 0 else (1..4).minBy { abs(Warmth[it]!! - t) }
    Options(
        tr("Creativity", "Kreatywność"),
        when {
            t == null -> tr("The model's own setting", "Ustawienie modelu")
            t == 0.0 -> tr("Exact: the same question gets the same answer", "Dokładnie: to samo pytanie, ta sama odpowiedź")
            else -> tr("The same question can be answered differently each time", "To samo pytanie może dostać różne odpowiedzi")
        },
        listOf(tr("Model", "Model"), tr("Exact", "Dokładnie"), tr("Low", "Mało"), tr("Medium", "Średnio"), tr("High", "Dużo")),
        at,
    ) { m.setSampling("temperature", Warmth[it]) }
}

@Composable
private fun Instructions(m: AppModel) {
    val accent = LocalAccent.current
    var text by typed(m.style.custom, follow = false) { m.setStyle("custom", it) }
    Column(Modifier.fillMaxWidth().background(Tone.Panel).padding(Space.S4)) {
        BasicText(tr("How should Impulse answer?", "Jak Impulse ma odpowiadać?"), style = Medium)
        BasicText(tr("Added to every chat", "Dodawane do każdego czatu"), style = Type.Meta)
        Spacer(Modifier.height(Space.S3))
        BasicTextField(
            text,
            { text = it.take(InstructionChars) },
            Modifier.fillMaxWidth().heightIn(min = 96.dp).background(Tone.Inset, Shapes.ControlLg).padding(Space.S3),
            textStyle = Type.Body,
            cursorBrush = SolidColor(accent),
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) {
                        BasicText(tr("For example: I'm a teacher. Explain step by step and give one example.", "Na przykład: jestem nauczycielem. Tłumacz krok po kroku i daj jeden przykład."), style = Type.Body.copy(color = Ink.Four))
                    }
                    inner()
                }
            },
        )
        Spacer(Modifier.height(Space.S2))
        val tokens = ceil(text.length / 3.6).toInt()
        Roll(tr("About $tokens of 400 tokens", "Około $tokens z 400 tokenów"), Type.Meta, digits = true)
    }
}

/** One sampler default: [stops] steps of [step] from [from]; [rest] is where the knob waits while the model's own value applies. */
private class Sampler(val key: String, val en: String, val pl: String, val hintEn: String, val hintPl: String, val from: Double, val step: Double, val stops: Int, val rest: Double, val digits: Int) {
    fun at(v: Double) = ((v - from) / step).roundToInt().coerceIn(0, stops - 1)
    fun value(i: Int): Double = 10.0.pow(digits).let { p -> ((from + step * i) * p).roundToLong() / p }
    fun show(i: Int) = if (digits == 0) value(i).roundToInt().toString() else String.format(Locale.US, "%.${digits}f", value(i))
    fun stored(i: Int): Any = if (digits == 0) value(i).roundToInt() else value(i)
}

private val Samplers = listOf(
    Sampler("temperature", "Temperature", "Temperatura", "Higher is more varied, lower is more exact", "Wyżej to więcej odmiany, niżej więcej dokładności", 0.0, 0.05, 41, 0.8, 2),
    Sampler("top_p", "Top P", "Top P", "Picks from the likeliest tokens that add up to this share", "Wybiera z najbardziej prawdopodobnych tokenów, które razem dają ten udział", 0.0, 0.05, 21, 0.95, 2),
    Sampler("top_k", "Top K", "Top K", "Picks from this many likeliest tokens; 0 is no limit", "Wybiera spośród tylu najbardziej prawdopodobnych tokenów; 0 to bez limitu", 0.0, 5.0, 41, 40.0, 0),
    Sampler("min_p", "Min P", "Min P", "Drops tokens far less likely than the best one", "Odrzuca tokeny dużo mniej prawdopodobne od najlepszego", 0.0, 0.01, 31, 0.05, 2),
    Sampler("repeat_penalty", "Repeat penalty", "Kara za powtórzenia", "Above 1 the model repeats itself less", "Powyżej 1 model rzadziej się powtarza", 1.0, 0.01, 51, 1.0, 2),
)

@Composable
private fun Dial(m: AppModel, k: Sampler) {
    val set = (m.sampling.opt(k.key) as? Number)?.toDouble()
    var at by remember(set) { mutableIntStateOf(k.at(set ?: k.rest)) }
    var moved by remember(set) { mutableStateOf(false) }
    val own = set == null && !moved
    val said = if (own) tr("Model's own", "Jak w modelu") else k.show(at)
    Column(Modifier.fillMaxWidth().background(Tone.Panel).padding(Space.S4)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(tr(k.en, k.pl), Modifier.weight(1f), style = Medium)
            Roll(said, Type.Meta, digits = !own)
        }
        BasicText(tr(k.hintEn, k.hintPl), style = Type.Meta)
        Spacer(Modifier.height(Space.S2))
        Track(k.stops, at, said, { at = it; moved = true }, dim = own) {
            at = it
            m.setSampling(k.key, k.stored(it))
        }
    }
}

@Composable
private fun Seed(m: AppModel) {
    val saved = (m.sampling.opt("seed") as? Number)?.toLong()
    var text by typed(saved?.toString().orEmpty()) { m.setSampling("seed", it.toLongOrNull()) }
    Column(Modifier.fillMaxWidth().background(Tone.Panel).padding(Space.S4)) {
        BasicText(tr("Seed", "Ziarno"), style = Medium)
        BasicText(tr("The same seed and question give the same answer. Empty is random.", "To samo ziarno i pytanie dają tę samą odpowiedź. Puste to losowe."), style = Type.Meta)
        Spacer(Modifier.height(Space.S3))
        Field(text, { text = it.filter(Char::isDigit).take(10) }, tr("Random", "Losowe"), keyboard = KeyboardType.Number, style = Type.Mono.copy(color = Ink.One))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Stops(m: AppModel) {
    val list = m.sampling.optJSONArray("stop").strings()
    var text by remember { mutableStateOf("") }
    fun keep(next: List<String>) = m.setSampling("stop", if (next.isEmpty()) null else JSONArray(next))
    fun add() {
        val s = text.replace("\\n", "\n")
        if (s.isNotEmpty() && s !in list) keep(list + s)
        text = ""
    }
    Column(Modifier.fillMaxWidth().background(Tone.Panel).padding(Space.S4)) {
        BasicText(tr("Stop sequences", "Sekwencje stopu"), style = Medium)
        BasicText(tr("The answer ends where the model writes one of these. Type \\n for a new line.", "Odpowiedź kończy się tam, gdzie model napisze jedną z nich. \\n to nowa linia."), style = Type.Meta)
        Spacer(Modifier.height(Space.S3))
        if (list.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.S2), verticalArrangement = Arrangement.spacedBy(Space.S2)) {
                list.forEach { s ->
                    key(s) {
                        Row(Modifier.height(32.dp).background(Tone.Raise, Capsule).padding(start = Space.S3, end = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            BasicText(s.replace("\n", "\\n"), style = Type.Mono, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.width(2.dp))
                            IconBtn(Glyph.Close, { keep(list - s) }, fill = Color.Transparent, size = 26.dp, tint = Ink.Three)
                        }
                    }
                }
            }
            Spacer(Modifier.height(Space.S3))
        }
        Field(text, { text = it }, tr("Add one, then Done", "Dodaj i naciśnij Gotowe"), onAction = ::add, style = Type.Mono.copy(color = Ink.One))
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
    Options(
        tr("Thinking effort", "Wysiłek myślenia"), notes[at],
        listOf(tr("Instant", "Od razu"), tr("Low", "Krótko"), tr("Medium", "Średnio"), tr("High", "Dużo"), "Max"),
        at,
    ) { m.pickEffort(Levels[it]) }
}

@Composable
private fun ChatRows(m: AppModel) {
    Switch(tr("Show thinking", "Pokazuj myślenie"), tr("The model's reasoning, folded above each answer", "Rozumowanie modelu, zwinięte nad odpowiedzią"), m.showThinking) { m.setThinking(it) }
    Switch(tr("Enter sends", "Enter wysyła"), tr("Without it, Enter starts a new line", "Bez tego Enter zaczyna nową linię"), m.enterSends) { m.pickEnterSends(it) }
    Switch(tr("Keep screen on", "Ekran zawsze włączony"), tr("While an answer is being written", "Gdy powstaje odpowiedź"), m.keepAwake) { m.pickKeepAwake(it) }
    val search = listOf("auto", "on", "off")
    Choice(
        tr("Search the web", "Szukaj w sieci"),
        search,
        m.style.search,
        {
            when (it) {
                "on" -> tr("Always", "Zawsze")
                "off" -> tr("Never", "Nigdy")
                else -> tr("Automatic", "Automatycznie")
            }
        },
        { m.setStyle("search", it) },
        {
            when (it) {
                "on" -> tr("Every new chat starts with search on", "Każdy nowy czat zaczyna z wyszukiwaniem")
                "off" -> tr("Turn it on in a chat when you need it", "Włącz w czacie, gdy trzeba")
                else -> tr("On, so small models don't guess facts from memory", "Włączone, żeby małe modele nie zgadywały faktów z pamięci")
            }
        },
    )
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
}

@Composable
private fun AccountRows(m: AppModel, name: String, onPassword: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val me = m.me
    var tried by remember { mutableStateOf(false) }
    val photos = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            tried = true
            val photo = withContext(Dispatchers.IO) { squarePhoto(ctx, uri) }
            if (photo != null) {
                m.setProfile(photo = photo)
            } else {
                m.error = tr("That picture could not be opened.", "Nie dało się otworzyć tego zdjęcia.")
            }
        }
    }
    NameField(m) { tried = true }
    Pictures(m, name) { tried = true }
    Line(
        tr("Photo", "Zdjęcie"),
        if (me?.photo != null) tr("Your own picture, shown instead of the one above", "Twoje zdjęcie, zamiast obrazka wyżej") else tr("Choose one from your gallery", "Wybierz z galerii"),
        { photos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
    ) {
        if (me?.photo != null) IconBtn(Glyph.Trash, { tried = true; m.setProfile(photo = "") }, size = 36.dp, tint = Ink.Three)
        else Icon(Glyph.Image, Ink.Three, size = 20.dp)
    }
    Line(
        if (me?.hasPassword == false) tr("Set a password", "Ustaw hasło") else tr("Change password", "Zmień hasło"),
        tr("Asked when this profile signs in from a phone or browser", "Pytane przy logowaniu z telefonu lub przeglądarki"),
        onPassword,
    ) {
        Icon(Glyph.Lock, Ink.Three, size = 20.dp)
    }
    val problem = m.error.takeIf { tried && !m.busy }
    if (problem != null) {
        Row(Modifier.fillMaxWidth().background(Tone.Panel).padding(Space.S4), verticalAlignment = Alignment.CenterVertically) {
            Icon(Glyph.Alert, Signal.Err, size = 18.dp)
            Spacer(Modifier.width(Space.S3))
            BasicText(problem, style = Type.Meta.copy(color = Signal.Err))
        }
    }
}

@Composable
private fun NameField(m: AppModel, onSave: () -> Unit) {
    val focus = LocalFocusManager.current
    val saved = m.me?.name.orEmpty()
    var text by remember(saved) { mutableStateOf(saved) }
    fun commit() {
        val t = text.trim()
        if (t.isNotEmpty() && t != saved) {
            onSave()
            m.setProfile(name = t)
        }
    }
    Column(Modifier.fillMaxWidth().background(Tone.Panel).padding(Space.S4)) {
        BasicText(tr("Name", "Imię"), style = Medium)
        BasicText(tr("For the greeting, and in answers if you like", "Do powitania, a jeśli chcesz, także w odpowiedziach"), style = Type.Meta)
        Spacer(Modifier.height(Space.S3))
        Field(
            text, { text = it.take(60) }, tr("Your name or nickname", "Imię lub pseudonim"),
            Modifier.onFocusChanged { if (!it.isFocused) commit() },
            onAction = { commit(); focus.clearFocus() },
        )
    }
}

/** The panel's tofus and emoji; a pick replaces any photo so it shows at once. */
@Composable
private fun Pictures(m: AppModel, name: String, onPick: () -> Unit) {
    val accent = LocalAccent.current
    val me = m.me
    val chosen = if (me?.photo != null) null else me?.avatar
    val all = remember { Tofus + Emoji.map(::emojiId) }
    Column(Modifier.fillMaxWidth().background(Tone.Panel).padding(vertical = Space.S4)) {
        BasicText(tr("Picture", "Obrazek"), Modifier.padding(horizontal = Space.S4), style = Medium)
        BasicText(tr("Shown beside your name, here and on the computer", "Obok twojego imienia, tu i na komputerze"), Modifier.padding(horizontal = Space.S4), style = Type.Meta)
        Spacer(Modifier.height(Space.S3))
        LazyRow(contentPadding = PaddingValues(horizontal = Space.S3), horizontalArrangement = Arrangement.spacedBy(Space.S1)) {
            items(all, key = { it }) { id ->
                val on = id == chosen
                val ring = animateFloatAsState(if (on) 1f else 0f, Motion.press(), label = "picked")
                Box(
                    Modifier.size(54.dp)
                        .pressable({ onPick(); m.setProfile(avatar = id, photo = if (me?.photo != null) "" else null) }, CircleShape, depth = 0.86f, feel = Feel.Tick, stretch = true)
                        .drawBehind {
                            val k = ring.value.coerceIn(0f, 1f)
                            if (k > 0f) drawCircle(accent, size.minDimension / 2 - 1.dp.toPx(), alpha = k, style = Stroke(2.dp.toPx()))
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Avatar(name, id, null, 44.dp)
                }
            }
        }
    }
}

/** A gallery picture as the panel keeps it: a 256 px square JPEG in a data URL, cut from the middle. */
private fun squarePhoto(ctx: Context, uri: Uri, side: Int = 256): String? = runCatching {
    val cr = ctx.contentResolver
    val full = if (Build.VERSION.SDK_INT >= 28) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(cr, uri)) { d, info, _ ->
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val short = minOf(info.size.width, info.size.height)
            if (short > side * 2) d.setTargetSampleSize(short / (side * 2))
        }
    } else {
        // shortcut: Android 8 ignores the camera's EXIF rotation, so a sideways photo stays sideways; ExifInterface fixes it if it matters.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val short = minOf(bounds.outWidth, bounds.outHeight)
        val o = BitmapFactory.Options().apply { inSampleSize = (short / (side * 2)).coerceAtLeast(1) }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) } ?: return null
    }
    val s = minOf(full.width, full.height)
    val square = Bitmap.createScaledBitmap(Bitmap.createBitmap(full, (full.width - s) / 2, (full.height - s) / 2, s, s), side, side, true)
    val out = ByteArrayOutputStream()
    square.compress(Bitmap.CompressFormat.JPEG, 85, out)
    "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}.getOrNull()

@Composable
private fun PasswordSheet(m: AppModel, open: Boolean, onClose: () -> Unit) {
    val view = LocalView.current
    val has = m.me?.hasPassword != false
    var now by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    LaunchedEffect(open) {
        if (open) {
            now = ""; next = ""; again = ""; tried = false
        }
    }
    fun done() {
        view.feel(Feel.Confirm)
        onClose()
    }
    Sheet(open, onClose) {
        Column(Modifier.padding(top = Space.S2).imePadding(), verticalArrangement = Arrangement.spacedBy(Space.S2)) {
            BasicText(if (has) tr("Change password", "Zmień hasło") else tr("Set a password", "Ustaw hasło"), style = Type.Title)
            BasicText(
                tr("Phones and browsers ask for it when they sign in to this profile.", "Telefony i przeglądarki pytają o nie przy logowaniu do tego profilu."),
                style = Type.Body.copy(color = Ink.Three),
            )
            Spacer(Modifier.height(Space.S2))
            if (has) Field(now, { now = it }, tr("Current password", "Obecne hasło"), secret = true, keyboard = KeyboardType.Password, action = ImeAction.Next)
            Field(next, { next = it }, tr("New password", "Nowe hasło"), secret = true, keyboard = KeyboardType.Password, action = ImeAction.Next)
            Field(again, { again = it }, tr("New password again", "Nowe hasło jeszcze raz"), secret = true, keyboard = KeyboardType.Password)
            val problem = when {
                again.isNotEmpty() && again != next -> tr("The two new passwords are not the same.", "Nowe hasła się różnią.")
                tried && !m.busy -> m.error
                else -> null
            }
            Roll(problem.orEmpty(), Type.Meta.copy(color = Signal.Err), maxLines = 3)
            Btn(
                tr("Save password", "Zapisz hasło"),
                { tried = true; m.setPassword(now.takeIf { has }, next) { done() } },
                Modifier.fillMaxWidth(), Variant.Primary,
                enabled = next.isNotEmpty() && next == again && (!has || now.isNotEmpty()) && !m.busy,
            )
            if (has) {
                Btn(
                    tr("Remove password", "Usuń hasło"),
                    { tried = true; m.setPassword(now, null) { done() } },
                    Modifier.fillMaxWidth(), Variant.Danger,
                    enabled = now.isNotEmpty() && !m.busy,
                )
            }
            Btn(tr("Cancel", "Anuluj"), onClose, Modifier.fillMaxWidth(), Variant.Quiet)
        }
    }
}

private val IslandHeight = 64.dp

/** Half the folded height, so the folded island is a capsule and the unfolded one a rounded card. */
private val IslandShape = RoundedCornerShape(32.dp)

/** This phone's version and the check; the arrows turn while GitHub is asked. */
@Composable
private fun UpdateRows() {
    val ctx = LocalContext.current
    val calm = LocalShell.current.calm
    val accent = LocalAccent.current
    val spin = remember { Animatable(0f) }
    val step = Updater.step
    val checking = step == Updater.Step.Checking
    val tint by animateColorAsState(if (checking) accent else Ink.Three, tween(Motion.M2), label = "tint")
    LaunchedEffect(checking) {
        if (calm) return@LaunchedEffect
        if (checking) {
            while (true) spin.animateTo(spin.value + 360f, tween(800, easing = LinearEasing))
        }
        spin.animateTo(ceil(spin.value / 360f) * 360f, spring(1f, 220f))
    }
    ListRow("Impulse for Android", detail = tr("Version", "Wersja") + " " + BuildConfig.VERSION_NAME)
    val r = Updater.release
    Line(
        tr("Check for updates", "Sprawdź aktualizacje"),
        when {
            checking -> tr("Asking GitHub", "Pytam GitHub")
            step == Updater.Step.Failed -> Updater.problem.orEmpty()
            r != null -> tr("Version ${r.version} is out", "Jest wersja ${r.version}")
            step == Updater.Step.Current -> tr("Up to date", "Aktualna")
            else -> tr("Checked once a day when Impulse opens", "Sprawdzane raz dziennie przy otwarciu")
        },
        { Updater.check(ctx) },
    ) {
        Icon(Glyph.Refresh, tint, Modifier.graphicsLayer { rotationZ = spin.value }, size = 20.dp)
    }
}

/** Rises from the bottom of the page while a newer version waits. */
@Composable
private fun BoxScope.UpdateIsland(haze: HazeState) {
    val calm = LocalShell.current.calm
    AnimatedVisibility(
        Updater.release != null,
        Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = Space.S4, vertical = Space.S3),
        enter = if (calm) fadeIn(tween(Motion.M2)) else slideInVertically(spring(0.78f, 380f)) { it } + scaleIn(spring(0.7f, 420f), 0.86f) + fadeIn(tween(Motion.M2)),
        exit = if (calm) fadeOut(tween(Motion.M1)) else slideOutVertically(Motion.settle()) { it } + fadeOut(tween(Motion.M2)),
    ) {
        Updater.release?.let { Island(haze, it) }
    }
}

/** The new version, its notes folded under it, and one control that is Update, then a progress ring, then Install. */
@Composable
private fun Island(haze: HazeState, r: Release) {
    val ctx = LocalContext.current
    val accent = LocalAccent.current
    val calm = LocalShell.current.calm
    val step = Updater.step
    var open by remember { mutableStateOf(false) }
    val turn = animateFloatAsState(if (open) 180f else 0f, Motion.travel(), label = "notes")
    val percent by remember { derivedStateOf { (Updater.progress * 100).roundToInt() } }
    val size = String.format(Locale.US, "%.1f MB", r.bytes / 1_000_000.0)
    Column(Modifier.fillMaxWidth().glass(haze, IslandShape)) {
        Row(
            Modifier.fillMaxWidth().height(IslandHeight).padding(start = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).pressable({ open = !open }, Capsule, enabled = r.notes.isNotEmpty(), feel = Feel.Tick, depth = 0.98f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(40.dp).background(accent.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Glyph.Sparkle, accent, size = 20.dp)
                }
                Spacer(Modifier.width(Space.S3))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicText("Impulse ${r.version}", style = Medium, maxLines = 1)
                        if (r.notes.isNotEmpty()) {
                            Spacer(Modifier.width(Space.S1))
                            Icon(Glyph.Down, Ink.Three, Modifier.graphicsLayer { rotationZ = turn.value }, size = 14.dp)
                        }
                    }
                    Roll(
                        when (step) {
                            Updater.Step.Downloading -> tr("Downloading $percent%", "Pobieranie $percent%")
                            Updater.Step.Ready ->
                                if (Updater.asking) tr("Allow installs from Impulse, then come back", "Zezwól Impulse na instalowanie, potem wróć")
                                else tr("Downloaded and verified", "Pobrane i sprawdzone")
                            Updater.Step.Failed -> Updater.problem.orEmpty()
                            Updater.Step.Checking -> tr("Checking", "Sprawdzam")
                            else -> tr("Update available · $size", "Dostępna aktualizacja · $size")
                        },
                        Type.Meta,
                        digits = step == Updater.Step.Downloading,
                        maxLines = 2,
                    )
                }
            }
            Spacer(Modifier.width(Space.S2))
            AnimatedContent(
                step == Updater.Step.Downloading,
                transitionSpec = {
                    val swap = if (calm) fadeIn(tween(Motion.M2)) togetherWith fadeOut(tween(Motion.M1))
                    else (fadeIn(tween(Motion.M2)) + scaleIn(Motion.press(), 0.7f)) togetherWith (fadeOut(tween(Motion.M1)) + scaleOut(tween(Motion.M1), 0.7f))
                    swap using SizeTransform(clip = false) { _, _ -> Motion.travel() }
                },
                contentAlignment = Alignment.Center,
                label = "update",
            ) { downloading ->
                if (downloading) {
                    ProgressRing(accent)
                } else {
                    val ready = step == Updater.Step.Ready
                    Pill(
                        when {
                            ready -> tr("Install", "Zainstaluj")
                            step == Updater.Step.Failed -> tr("Try again", "Ponów")
                            else -> tr("Update", "Aktualizuj")
                        },
                        { if (ready) Updater.install(ctx) else Updater.download(ctx) },
                        ink = OnAccent,
                        fill = accent,
                        enabled = step != Updater.Step.Checking,
                    )
                }
            }
        }
        AnimatedVisibility(
            open,
            enter = expandVertically(Motion.travel()) + fadeIn(tween(Motion.M2, delayMillis = 60)),
            exit = shrinkVertically(Motion.settle()) + fadeOut(tween(Motion.M1)),
        ) {
            Box(Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()).padding(start = Space.S4, end = Space.S4, bottom = Space.S4)) {
                Markdown(r.notes)
            }
        }
    }
}

/** The download as an arc around a 40dp circle; only its drawing follows the bytes. */
@Composable
private fun ProgressRing(accent: Color) {
    Box(
        Modifier.size(40.dp)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(Updater.progress, 0f..1f) }
            .drawBehind {
                val w = 3.dp.toPx()
                val inset = Offset(w / 2, w / 2)
                val arc = Size(size.width - w, size.height - w)
                drawArc(Ink.Four.copy(alpha = 0.5f), 0f, 360f, false, inset, arc, style = Stroke(w))
                drawArc(accent, -90f, 360f * Updater.progress, false, inset, arc, style = Stroke(w, cap = StrokeCap.Round))
            },
    )
}

/** What the computer says about itself, asked once the page is open and the computer answers. */
@Stable
private class Machine {
    var awake by mutableStateOf<KeepAwake?>(null)
    var devices by mutableStateOf<List<Device>?>(null)
    var version by mutableStateOf<JSONObject?>(null)
    var backup by mutableStateOf<JSONObject?>(null)
}

@Composable
private fun rememberMachine(m: AppModel): Machine {
    val x = remember { Machine() }
    val reachable = m.online == true
    LaunchedEffect(reachable) {
        if (!reachable) return@LaunchedEffect
        m.request({ keepAwake() }) { x.awake = it }
        m.request({ devices() }) { x.devices = it }
        m.request({ runCatching { version() }.getOrDefault(JSONObject()) }) { x.version = it }
        m.request({ backup() }) { x.backup = it }
    }
    return x
}

/** Rows of a Computer group: where it is, how it is reached, what it holds, how it is kept, and the way out. */
@Composable
private fun ComputerRows(m: AppModel, x: Machine, onSwitch: () -> Unit, onRestart: () -> Unit) {
    Address(m)
    ListRow(tr("Connection", "Połączenie"), detail = linkLabel(m.link)) {
        Icon(linkGlyph(m.link), Ink.Three, size = 20.dp)
    }
    ListRow(tr("Models", "Modele"), detail = tr("In the computer's models folder", "W folderze modeli na komputerze")) {
        Roll("${m.library.size}", Type.Body.copy(color = Ink.Two), digits = true)
    }
    Reconnect(m)
    Awake(m, x)
    Version(m, x)
    Backups(m, x)
    Diagnostics(m, x)
    Line(tr("Restart Impulse", "Uruchom Impulse ponownie"), tr("Closes and opens Impulse on the computer", "Zamyka i otwiera Impulse na komputerze"), onRestart) {
        Icon(Glyph.Refresh, Ink.Three, size = 20.dp)
    }
    Line(tr("Switch computer", "Zmień komputer"), tr("Connect this phone to a different computer", "Połącz telefon z innym komputerem"), onSwitch) {
        Icon(Glyph.Right, Ink.Four, size = 16.dp)
    }
}

private val Modes = listOf("off", "working", "always")

@Composable
private fun Awake(m: AppModel, x: Machine) {
    val a = x.awake
    val why = when (a?.reason) {
        "answering" -> tr("answering", "odpowiada")
        "downloading" -> tr("downloading", "pobiera")
        "finishing" -> tr("finishing up", "kończy pracę")
        "always" -> tr("always on", "zawsze")
        else -> tr("working", "pracuje")
    }
    Options(
        tr("Keep the computer awake", "Nie usypiaj komputera"),
        when {
            a == null -> tr("Asking the computer", "Pytam komputer")
            a.reason == "unavailable" -> tr("This computer can't be kept awake", "Tego komputera nie da się trzymać w czuwaniu")
            a.holding -> tr("Awake now: $why", "Teraz nie śpi: $why")
            a.mode == "working" -> tr("Awake while Impulse answers, downloads or builds", "Nie śpi, gdy Impulse odpowiada, pobiera lub buduje")
            a.mode == "always" -> tr("Never sleeps while Impulse runs", "Nie zaśnie, póki działa Impulse")
            else -> tr("The computer sleeps on its own schedule", "Komputer zasypia według swoich ustawień")
        },
        listOf(tr("Off", "Wył."), tr("While working", "Przy pracy"), tr("Always", "Zawsze")),
        Modes.indexOf(a?.mode ?: "off").coerceAtLeast(0),
    ) { i ->
        x.awake = (a ?: KeepAwake("off", false, "")).copy(mode = Modes[i])
        m.request({ setKeepAwake(Modes[i]) }) { x.awake = it }
    }
}

@Composable
private fun Version(m: AppModel, x: Machine) {
    val accent = LocalAccent.current
    val v = x.version
    val current = v?.optString("current").orEmpty()
    val latest = v?.optJSONObject("latest")?.optString("version").orEmpty()
    val newer = v?.optBoolean("update_available") == true
    Line(
        if (current.isNotEmpty()) "Impulse $current" else "Impulse",
        when {
            v == null -> tr("Checking for updates", "Sprawdzam aktualizacje")
            newer -> tr("Version $latest is out. Update it on the computer.", "Jest wersja $latest. Zaktualizuj na komputerze.")
            v.optBoolean("checked") -> tr("Up to date", "Aktualna")
            else -> tr("Couldn't check for updates. Tap to try again.", "Nie udało się sprawdzić aktualizacji. Stuknij, by spróbować ponownie.")
        },
        {
            x.version = null
            m.request({ runCatching { version() }.getOrDefault(JSONObject()) }) { x.version = it }
        },
    ) {
        Icon(if (newer) Glyph.Sparkle else Glyph.Check, if (newer) accent else Ink.Three, size = 20.dp)
    }
}

@Composable
private fun Backups(m: AppModel, x: Machine) {
    val b = x.backup
    var running by remember { mutableStateOf(false) }
    val last = b?.optLong("last_unix") ?: 0L
    val failed = b?.optString("last_error")?.takeIf { it.isNotBlank() && it != "null" }
    Line(
        tr("Back up now", "Zrób kopię teraz"),
        when {
            running || b?.optBoolean("running") == true -> tr("Backing up into the computer's backup folder", "Kopia do folderu kopii na komputerze")
            b == null -> tr("Asking the computer", "Pytam komputer")
            failed != null -> tr("The last backup failed: $failed", "Ostatnia kopia się nie udała: $failed")
            last > 0 -> tr("Last backup ${ago(System.currentTimeMillis() / 1000 - last)}", "Ostatnia kopia ${ago(System.currentTimeMillis() / 1000 - last)}")
            else -> tr("No backup yet", "Jeszcze bez kopii")
        },
        {
            if (!running) {
                running = true
                m.request({
                    try {
                        backupNow()
                        delay(3000)
                        backup()
                    } finally {
                        running = false
                    }
                }) { x.backup = it }
            }
        },
    ) {
        Icon(Glyph.History, Ink.Three, size = 20.dp)
    }
    val folder = b?.optString("folder").orEmpty()
    Switch(
        tr("Back up automatically", "Kopia automatyczna"),
        (if (b?.optString("every") == "week") tr("Once a week", "Raz w tygodniu") else tr("Once a day", "Raz dziennie")) +
            (if (folder.isNotEmpty()) tr(", into $folder", ", do $folder") else ""),
        b?.optBoolean("enabled") == true,
    ) { on ->
        if (b != null) {
            x.backup = JSONObject(b.toString()).put("enabled", on)
            m.request({ setBackup(JSONObject().put("enabled", on)) }) { x.backup = it }
        }
    }
}

@Composable
private fun Diagnostics(m: AppModel, x: Machine) {
    val clipboard = LocalClipboardManager.current
    var copies by remember { mutableIntStateOf(0) }
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copies) {
        if (copies == 0) return@LaunchedEffect
        copied = true
        delay(1800)
        copied = false
    }
    Line(
        tr("Copy diagnostics", "Kopiuj diagnostykę"),
        if (copied) tr("Copied. Paste it into your bug report.", "Skopiowano. Wklej do zgłoszenia błędu.") else tr("Versions, model and hardware, for a bug report", "Wersje, model i sprzęt, do zgłoszenia błędu"),
        {
            m.request({ runCatching { hardware() }.getOrNull() }) { hw ->
                clipboard.setText(AnnotatedString(report(m, x, hw)))
                copies++
            }
        },
        feel = Feel.Confirm,
    ) {
        Icon(if (copied) Glyph.Check else Glyph.Copy, if (copied) LocalAccent.current else Ink.Three, size = 18.dp)
    }
}

/** What a bug report needs, and nothing that signs in: no address, keys or chat words. */
private fun report(m: AppModel, x: Machine, hardware: JSONObject?): String = buildString {
    appendLine("Impulse for Android ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
    appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), ${Build.MANUFACTURER} ${Build.MODEL}")
    appendLine("Connection: ${linkLabel(m.link)}, ${if (m.online == true) "online" else "offline"}")
    x.version?.let { v ->
        appendLine("Impulse: ${v.optString("current")} ${v.optString("channel")}".trimEnd() + (v.optJSONObject("latest")?.optString("version")?.takeIf { v.optBoolean("update_available") }?.let { ", $it available" } ?: ""))
    }
    val model = m.status?.model
    appendLine(
        if (model == null) "Model: none loaded"
        else "Model: ${model.name}, window ${model.context} tokens${if (model.thinks) ", thinks" else ""}${if (model.vision) ", sees pictures" else ""}",
    )
    appendLine("Models on the computer: ${m.library.size}")
    appendLine("Chats on this phone: ${m.chats.size}, answers being written: ${m.lives.size}")
    appendLine("Theme ${m.theme}, text ${(m.textScale * 100).roundToInt()}%, reduce motion ${m.calm}")
    if (hardware != null) {
        appendLine()
        appendLine("Hardware:")
        append(hardware.toString(2))
    }
}

@Composable
private fun DeviceRows(x: Machine, onSignOut: (Device) -> Unit) {
    val list = x.devices
    when {
        list == null -> ListRow(tr("Asking the computer", "Pytam komputer"))
        list.isEmpty() -> ListRow(tr("None", "Brak"), detail = tr("Only this computer uses Impulse right now", "Na razie Impulse używa tylko ten komputer"))
        else -> list.forEach { d -> key(d.id) { DeviceRow(d) { onSignOut(d) } } }
    }
}

private fun Device.mine() = app && name == Build.MODEL

@Composable
private fun DeviceRow(d: Device, onSignOut: () -> Unit) {
    val via = when (d.via) {
        "lan" -> tr("Local network", "Sieć lokalna")
        "tailscale" -> "Tailscale"
        "cloudflare" -> "Cloudflare Tunnel"
        else -> d.via
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).background(Tone.Panel).padding(start = Space.S4, end = Space.S2, top = Space.S3, bottom = Space.S3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (d.app) Glyph.Phone else Glyph.Globe, Ink.Two, size = 20.dp)
        Spacer(Modifier.width(Space.S3))
        Column(Modifier.weight(1f)) {
            BasicText(
                d.name.ifBlank { tr("Browser", "Przeglądarka") } + if (d.mine()) tr(" · this phone", " · ten telefon") else "",
                style = Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                listOfNotNull(via, d.ip.ifBlank { null }, d.profileName, tr("seen ${ago(d.seenAgo)}", "widziany ${ago(d.seenAgo)}")).joinToString(" · "),
                style = Type.Meta, maxLines = 2,
            )
        }
        IconBtn(Glyph.SignOut, onSignOut, fill = Color.Transparent, size = 40.dp, tint = Ink.Three)
    }
}

@Composable
private fun DropDevice(m: AppModel, x: Machine, d: Device?, open: Boolean, onClose: () -> Unit) {
    val mine = d?.mine() == true
    Confirm(
        open, onClose,
        tr("Sign out ${d?.name?.ifBlank { null } ?: tr("this browser", "tę przeglądarkę")}?", "Wylogować ${d?.name?.ifBlank { null } ?: "tę przeglądarkę"}?"),
        if (mine) tr("That is this phone. You'll need to sign in again.", "To ten telefon. Trzeba będzie zalogować się ponownie.")
        else tr("It has to sign in again to reach this computer.", "Żeby połączyć się z komputerem, musi zalogować się ponownie."),
        tr("Sign out", "Wyloguj"),
    ) {
        val id = d?.id ?: return@Confirm
        m.request({ signOutDevice(id) }) {
            x.devices = it
            if (mine) m.signOut()
        }
    }
}

@Composable
private fun Restart(m: AppModel, open: Boolean, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    Confirm(
        open, onClose,
        tr("Restart Impulse?", "Uruchomić Impulse ponownie?"),
        tr("Impulse closes and opens again on the computer. Answers being written stop; the phone reconnects on its own.", "Impulse zamknie się i otworzy na komputerze. Pisane odpowiedzi się zatrzymają; telefon połączy się sam."),
        tr("Restart", "Uruchom ponownie"),
    ) {
        m.request({ restart() }) {
            scope.launch {
                delay(6000)
                m.refreshAll()
            }
        }
    }
}

private fun ago(s: Long): String = when {
    s < 60 -> tr("just now", "przed chwilą")
    s < 3600 -> tr("${s / 60} min ago", "${s / 60} min temu")
    s < 86_400 -> tr("${s / 3600} h ago", "${s / 3600} godz. temu")
    s < 172_800 -> tr("yesterday", "wczoraj")
    else -> tr("${s / 86_400} days ago", "${s / 86_400} dni temu")
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

/**
 * Typing kept on the phone and handed to [keep] after a pause, or when the page goes, so each
 * letter is not a request to the computer. With [follow], a new [value] from outside replaces it.
 */
@Composable
private fun typed(value: String, follow: Boolean = true, keep: (String) -> Unit): MutableState<String> {
    val text = remember(if (follow) value else Unit) { mutableStateOf(value) }
    val base by rememberUpdatedState(value)
    val now by rememberUpdatedState(text.value)
    val save by rememberUpdatedState(keep)
    LaunchedEffect(text.value) {
        delay(700)
        if (text.value != base) save(text.value)
    }
    DisposableEffect(Unit) { onDispose { if (now != base) save(now) } }
    return text
}

/** A titled choice among a few: the hint rolls when it changes, the thumb travels between options. */
@Composable
private fun Options(title: String, hint: String, options: List<String>, at: Int, onPick: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Tone.Panel).padding(Space.S4)) {
        BasicText(title, style = Medium)
        Roll(hint, Type.Meta, maxLines = 3)
        Spacer(Modifier.height(Space.S3))
        Segmented(options, at.coerceAtLeast(0), onPick)
    }
}

/** A setting that is on or off; the whole row flips it. */
@Composable
private fun Switch(title: String, detail: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Line(title, detail, { onChange(!on) }, feel = if (on) Feel.Off else Feel.On) {
        Toggle(on, onChange, description = title)
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
            BasicText(title, style = Medium)
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

/** Each block rises in after the one above it, on a spring with no overshoot, so text never bounces; a grown surface brings them in itself. */
@Composable
private fun Modifier.arrive(order: Int): Modifier {
    if (LocalPresentation.current != null) return this
    val calm = LocalShell.current.calm
    val lift = with(LocalDensity.current) { 28.dp.toPx() }
    val p = remember { Animatable(0f) }
    val step = order.coerceAtMost(6)
    LaunchedEffect(Unit) {
        delay(80L + 55L * step)
        p.animateTo(1f, if (calm) tween<Float>(Motion.M2) else spring<Float>(1f, 240f))
    }
    return graphicsLayer {
        val k = p.value
        alpha = k
        if (calm) return@graphicsLayer
        translationY = (1f - k) * lift * (1f + 0.2f * step)
        rotationX = (1f - k) * 6f
        cameraDistance = 16f * density
        transformOrigin = TransformOrigin(0.5f, 0f)
    }
}

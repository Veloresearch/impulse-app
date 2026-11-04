package com.velocity.impulse.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Probe
import com.velocity.impulse.ui.Chrome
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.LocalShell
import com.velocity.impulse.ui.Shell
import com.velocity.impulse.ui.Surface
import com.velocity.impulse.ui.components.Btn
import com.velocity.impulse.ui.components.Feel
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.IconBtn
import com.velocity.impulse.ui.components.MorphSource
import com.velocity.impulse.ui.components.OnAccent
import com.velocity.impulse.ui.components.Roll
import com.velocity.impulse.ui.components.Segmented
import com.velocity.impulse.ui.components.Variant
import com.velocity.impulse.ui.components.feel
import com.velocity.impulse.ui.components.flightLanding
import com.velocity.impulse.ui.components.glass
import com.velocity.impulse.ui.components.morph
import com.velocity.impulse.ui.components.pressable
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PillShape = Squircle(Radius.Card)

private fun same(a: String?, b: String?) =
    a != null && b != null && a.replace('\\', '/').equals(b.replace('\\', '/'), ignoreCase = true)

/** Menu, the chat and its model, new chat: three islands that stay put while their text rolls. */
@Composable
fun TopIsland(m: AppModel, modifier: Modifier = Modifier) {
    val shell = LocalShell.current
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Chrome.Side, vertical = 8.dp).height(Chrome.TopIsland),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).graphicsLayer { alpha = if (shell.docked) 0f else 1f }.glass(shell.cardHaze, CircleShape)) {
            IconBtn(Glyph.Sidebar, shell::openDrawer, tint = Ink.One, fill = Color.Transparent, size = 44.dp, enabled = !shell.docked, island = true)
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Pill(m, shell) }
        Spacer(Modifier.width(8.dp))
        NewChat(m, shell)
    }
}

/** Ordered from worst to best, so the label rolls up when things get better. */
private enum class Kind { Offline, None, Loading, Ready }

@Composable
private fun Pill(m: AppModel, shell: Shell) {
    val accent = LocalAccent.current
    val calm = shell.calm
    val title by remember(m) { derivedStateOf { m.chat?.title?.ifBlank { null } } }
    val kind = when {
        m.online == false -> Kind.Offline
        m.loading != null -> Kind.Loading
        m.status?.model != null -> Kind.Ready
        else -> Kind.None
    }
    val seen = remember { object { var last = kind; var up = true } }
    if (seen.last != kind) {
        seen.up = kind > seen.last
        seen.last = kind
    }
    val label = when (kind) {
        Kind.Offline -> tr("Computer offline", "Komputer niedostępny")
        Kind.Loading -> {
            val path = m.loading.orEmpty()
            val name = m.library.firstOrNull { same(it.path, path) }?.name ?: path.substringAfterLast('/').substringAfterLast('\\')
            tr("Loading $name…", "Wczytywanie $name…")
        }
        Kind.Ready -> m.status?.model?.name.orEmpty()
        Kind.None -> tr("Choose a model", "Wybierz model")
    }

    val pulse = remember { Animatable(0f) }
    LaunchedEffect(shell.nudge) {
        if (shell.nudge > 0) {
            pulse.animateTo(1f, tween(120))
            pulse.animateTo(0f, spring(0.5f, 600f))
        }
    }

    MorphSource("pill", Surface.Model) {
        Column(
            Modifier.morph("pill", this, PillShape)
                .graphicsLayer {
                    if (calm) return@graphicsLayer
                    scaleX = 1f - 0.05f * pulse.value
                    scaleY = scaleX
                }
                .pressable({ shell.open(Surface.Model, "pill") }, depth = 0.95f, stretch = true, feelOnDown = true)
                .glass(shell.cardHaze, PillShape)
                .drawWithContent {
                    drawContent()
                    val p = pulse.value.coerceIn(0f, 1f)
                    if (p > 0f) drawRect(Color.White, alpha = 0.06f * p)
                }
                .widthIn(min = 160.dp)
                .height(52.dp)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.flightLanding(shell.flight)) {
                Roll(title ?: tr("New chat", "Nowy czat"), Type.Body.copy(fontWeight = FontWeight.SemiBold), up = shell.sceneDir > 0)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedContent(
                    kind,
                    Modifier.size(12.dp),
                    transitionSpec = {
                        val enter = if (targetState == Kind.Ready && !calm) scaleIn(spring(0.5f, 500f), 0f) else fadeIn(tween(Motion.M2))
                        val exit = if (initialState == Kind.Ready && !calm) scaleOut(tween(Motion.M2, easing = Motion.Out), 0f) else fadeOut(tween(Motion.M2))
                        enter togetherWith exit using null
                    },
                    contentAlignment = Alignment.Center,
                    label = "mark",
                ) { k ->
                    when (k) {
                        Kind.Ready -> Box(Modifier.size(7.dp).background(accent, CircleShape))
                        Kind.Loading -> Spinner(accent, 12.dp)
                        Kind.None -> Box(Modifier.size(7.dp).background(Ink.Four, CircleShape))
                        Kind.Offline -> Icon(Glyph.Alert, Signal.Warn, size = 12.dp)
                    }
                }
                Spacer(Modifier.width(6.dp))
                Roll(label, Type.Meta.copy(fontWeight = FontWeight.Medium, color = Ink.Two), Modifier.weight(1f, fill = false), up = seen.up)
                Spacer(Modifier.width(4.dp))
                Icon(Glyph.Down, Ink.Three, size = 12.dp)
            }
        }
    }
}

@Composable
private fun NewChat(m: AppModel, shell: Shell) {
    val empty = m.current == null
    val v by animateFloatAsState(if (empty) 0f else 1f, spring(0.8f, 500f), label = "new")
    val turn = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        Modifier.size(44.dp)
            .graphicsLayer {
                alpha = v.coerceIn(0f, 1f)
                if (shell.calm) return@graphicsLayer
                scaleX = 0.6f + 0.4f * v
                scaleY = scaleX
            }
            .glass(shell.cardHaze, CircleShape),
    ) {
        IconBtn(
            Glyph.Plus,
            {
                if (!shell.calm) scope.launch {
                    turn.animateTo(90f, spring(0.6f, 500f))
                    turn.snapTo(0f)
                }
                shell.newChat()
            },
            Modifier.graphicsLayer { rotationZ = turn.value },
            tint = Ink.One,
            fill = Color.Transparent,
            size = 44.dp,
            enabled = !empty,
            island = true,
        )
    }
}

@Composable
fun Spinner(color: Color, size: Dp = 14.dp) = Arc(color, Modifier.size(size)) { 0f }

/** A spinning arc whose sweep breathes; [whole] 0..1 grows it into a closed ring. */
@Composable
private fun Arc(color: Color, modifier: Modifier, whole: () -> Float) {
    val t = rememberInfiniteTransition(label = "spin")
    val turn = t.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "turn")
    val sweep = t.animateFloat(60f, 200f, infiniteRepeatable(tween(1200, easing = Motion.Standard), RepeatMode.Reverse), label = "sweep")
    Canvas(modifier.graphicsLayer { rotationZ = turn.value }) {
        val w = size.minDimension * 0.14f
        val box = Size(size.width - w, size.height - w)
        drawCircle(color.copy(alpha = 0.18f), radius = (size.minDimension - w) / 2, style = Stroke(w))
        val s = sweep.value
        drawArc(color, -90f, s + (360f - s) * whole(), false, Offset(w / 2, w / 2), box, style = Stroke(w, cap = StrokeCap.Round))
    }
}

private val Efforts = listOf("off", "low", "medium", "high", "max")

private enum class Phase { Idle, Loading, Landed, Failed }

/** The one model surface: what is loaded, how hard it thinks, and the rest on the computer. */
@Composable
fun ColumnScope.ModelSheetBody(m: AppModel) {
    val shell = LocalShell.current
    val view = LocalView.current
    val loaded = m.status?.model
    var held by remember { mutableStateOf(m.loading) }
    var landed by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(m) {
        var was: String? = null
        snapshotFlow { m.loading to m.status?.model?.path }.collect { (loading, path) ->
            if (loading != null) {
                was = loading
                held = loading
                landed = null
            } else if (was != null) {
                val ok = same(path, was)
                was = null
                landed = ok
                view.feel(if (ok) Feel.Lock else Feel.Reject)
                if (ok) {
                    delay(700)
                    shell.done()
                }
            }
        }
    }

    BasicText(tr("Model", "Model"), style = Type.Title)
    Spacer(Modifier.height(Space.S1))
    BasicText(tr("The model on your computer that writes the answers.", "Model na Twoim komputerze, który pisze odpowiedzi."), style = Type.Meta)
    Spacer(Modifier.height(Space.S4))

    AnimatedContent(
        loaded,
        Modifier.fillMaxWidth().background(Tone.Raise, Shapes.Card).padding(Space.S4),
        transitionSpec = {
            val enter = if (shell.calm) fadeIn(tween(Motion.M2)) else fadeIn(tween(Motion.M2)) + slideInVertically(Motion.calmText()) { it / 6 }
            enter togetherWith fadeOut(tween(Motion.M1)) using SizeTransform(clip = true)
        },
        contentKey = { it?.path },
        label = "hero",
    ) { l ->
        if (l == null) {
            Column(Modifier.fillMaxWidth()) {
                BasicText(tr("No model loaded", "Nie wczytano modelu"), style = Type.Section)
                BasicText(tr("Tap one below to load it.", "Dotknij jednego poniżej, żeby go wczytać."), style = Type.Meta)
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    BasicText(l.name, style = Type.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    m.library.firstOrNull { same(it.path, l.path) }?.let { BasicText(modelLine(it), style = Type.Meta, maxLines = 1) }
                    val memory = tokens(l.context.toLong())
                    BasicText(tr("Memory per chat: $memory", "Pamięć na czat: $memory"), style = Type.Meta, maxLines = 1)
                }
                Spacer(Modifier.width(Space.S2))
                Btn(tr("Unload", "Zwolnij"), { m.unload() }, variant = Variant.Quiet, height = 36.dp)
            }
        }
    }
    Spacer(Modifier.height(Space.S4))

    if (loaded?.thinks != false) {
        BasicText(tr("Thinking", "Myślenie"), style = Type.Label, modifier = Modifier.padding(start = Space.S1, bottom = Space.S2))
        Segmented(
            listOf(tr("Instant", "Od razu"), tr("Low", "Krótko"), tr("Medium", "Średnio"), tr("High", "Dużo"), "Max"),
            Efforts.indexOf(m.effort ?: "medium").coerceAtLeast(0),
            { m.pickEffort(Efforts[it]) },
        )
        Spacer(Modifier.height(Space.S4))
    }

    BasicText(tr("On your computer", "Na Twoim komputerze"), style = Type.Label, modifier = Modifier.padding(start = Space.S1, bottom = Space.S2))
    if (m.library.isEmpty()) {
        BasicText(
            tr("No models on the computer yet. Add one in Impulse › Models.", "Na komputerze nie ma jeszcze modeli. Dodaj je w Impulse › Modele."),
            style = Type.Body.copy(color = Ink.Three),
        )
    }
    // The row being loaded stays until the sheet closes, so its check is seen landing.
    val rows = m.library.filter { !same(it.path, loaded?.path) || same(it.path, held) }
    LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items(rows, key = { it.path }) { p ->
            val phase = when {
                same(m.loading, p.path) -> Phase.Loading
                !same(held, p.path) -> Phase.Idle
                landed == true -> Phase.Landed
                landed == false -> Phase.Failed
                else -> Phase.Idle
            }
            ModelRow(m, p, phase, Modifier.animateItem())
        }
    }
}

@Composable
private fun ModelRow(m: AppModel, p: Probe, phase: Phase, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    val calm = LocalShell.current.calm
    val tappable = m.loading == null && phase != Phase.Landed
    val dim by animateFloatAsState(if (m.loading != null && phase != Phase.Loading) 0.45f else 1f, tween(Motion.M2), label = "dim")
    val rest by animateColorAsState(if (phase == Phase.Landed) Tone.Raise else Tone.Panel, tween(Motion.M2), label = "rest")
    val wash = remember { Animatable(0f) }
    val err = remember { Animatable(0f) }
    val ring = remember { Animatable(0f) }
    val fill = remember { Animatable(0f) }
    val tick = remember { Animatable(0f) }
    var secs by remember { mutableIntStateOf(0) }
    LaunchedEffect(phase) {
        when (phase) {
            Phase.Loading -> {
                secs = 0
                launch {
                    while (true) {
                        delay(1000)
                        secs++
                    }
                }
                if (!calm) while (true) {
                    wash.snapTo(0f)
                    wash.animateTo(1f, tween(1400, easing = Motion.Standard))
                }
            }
            Phase.Landed -> {
                if (!calm) launch {
                    wash.snapTo(0f)
                    wash.animateTo(1f, tween(300, easing = Motion.Out))
                }
                ring.animateTo(1f, tween(200, easing = Motion.Out))
                fill.animateTo(1f, tween(120))
                tick.animateTo(1f, tween(220, easing = Motion.Out))
            }
            Phase.Failed -> {
                err.snapTo(1f)
                err.animateTo(0f, tween(1200))
            }
            Phase.Idle -> {
                ring.snapTo(0f)
                fill.snapTo(0f)
                tick.snapTo(0f)
            }
        }
    }

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 60.dp)
                .graphicsLayer { alpha = dim }
                .pressable(
                    { if (tappable) m.load(p) },
                    Shapes.ControlLg, rest, if (tappable) Tone.Press else rest,
                    depth = 1f, feel = if (tappable) Feel.Tick else null,
                )
                .clip(Shapes.ControlLg)
                .drawBehind {
                    if (phase == Phase.Loading && calm) drawRect(accent.copy(alpha = 0.10f))
                    if ((phase == Phase.Loading || phase == Phase.Landed) && !calm) {
                        val band = size.width * 0.45f
                        val left = -band + (size.width + band) * wash.value
                        drawRect(
                            Brush.horizontalGradient(
                                0f to Color.Transparent, 0.5f to accent.copy(alpha = 0.16f), 1f to Color.Transparent,
                                startX = left, endX = left + band,
                            ),
                        )
                    }
                    val e = err.value
                    if (e > 0f) drawRect(Signal.Err.copy(alpha = 0.12f * e))
                }
                .padding(horizontal = Space.S4, vertical = Space.S3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(36.dp).background(Tone.Raise, Shapes.ControlSm), contentAlignment = Alignment.Center) {
                if (phase == Phase.Loading || phase == Phase.Landed) LoadMark(accent, { ring.value }, { fill.value }, { tick.value })
                else Icon(Glyph.Models, Ink.Three, size = 18.dp)
            }
            Spacer(Modifier.width(Space.S3))
            Column(Modifier.weight(1f)) {
                BasicText(p.name, style = Type.Body.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText(modelLine(p), style = Type.Meta, maxLines = 1)
            }
            AnimatedVisibility(phase == Phase.Loading && secs >= 1, enter = fadeIn(tween(Motion.M2)), exit = fadeOut(tween(Motion.M1))) {
                Roll("$secs s", Type.Meta, Modifier.padding(start = Space.S2), digits = true)
            }
        }
        AnimatedVisibility(
            phase == Phase.Failed && m.error != null,
            enter = expandVertically(tween(Motion.M2, easing = Motion.Out)) + fadeIn(tween(Motion.M2)),
            exit = shrinkVertically(tween(Motion.M2, easing = Motion.Out)) + fadeOut(tween(Motion.M1)),
        ) {
            BasicText(
                m.error.orEmpty(),
                Modifier.padding(start = Space.S4, end = Space.S4, top = Space.S2, bottom = Space.S1),
                style = Type.Meta.copy(color = Signal.Err),
            )
        }
    }
}

/** The spinner closing into a ring, filling, and a check drawn in. */
@Composable
private fun LoadMark(accent: Color, ring: () -> Float, fill: () -> Float, tick: () -> Float) {
    val path = remember { Path() }
    val part = remember { Path() }
    val measure = remember { PathMeasure() }
    Box(Modifier.size(18.dp)) {
        Arc(accent, Modifier.matchParentSize(), ring)
        Canvas(Modifier.matchParentSize()) {
            val f = fill()
            if (f > 0f) drawCircle(accent, alpha = f)
            val t = tick()
            if (t <= 0f) return@Canvas
            val s = size.minDimension
            path.reset()
            path.moveTo(0.28f * s, 0.52f * s)
            path.lineTo(0.44f * s, 0.67f * s)
            path.lineTo(0.73f * s, 0.36f * s)
            measure.setPath(path, false)
            part.reset()
            measure.getSegment(0f, measure.length * t, part, true)
            drawPath(part, OnAccent, style = Stroke(s * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

fun modelLine(p: Probe): String {
    val params = when {
        p.params >= 1_000_000_000 -> "%.1fB".format(p.params / 1e9).replace(".0B", "B")
        p.params > 0 -> "%.0fM".format(p.params / 1e6)
        else -> null
    }
    val size = if (p.bytes > 0) "%.1f GB".format(p.bytes / 1e9) else null
    return listOfNotNull(params, p.format.uppercase(), size).joinToString(" · ")
}

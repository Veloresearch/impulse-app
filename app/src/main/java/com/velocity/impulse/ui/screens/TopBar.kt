package com.velocity.impulse.ui.screens

import android.os.Build
import com.velocity.impulse.ui.components.ModelFace
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
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.velocity.impulse.R
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Loaded
import com.velocity.impulse.data.Probe
import com.velocity.impulse.ui.Chrome
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.LocalShell
import com.velocity.impulse.ui.Shell
import com.velocity.impulse.ui.Surface
import com.velocity.impulse.ui.components.Btn
import com.velocity.impulse.ui.components.Feel
import com.velocity.impulse.ui.components.Field
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
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val HeadShape = Squircle(Radius.Card)
private val Island = Squircle(22.dp)

/** The same tone as the conversation, so the band is invisible until something scrolls under it. */
private val Band = HazeStyle(Tone.App, HazeTint(Tone.App.copy(alpha = 0.58f)), blurRadius = 20.dp, noiseFactor = 0f)

private fun same(a: String?, b: String?) =
    a != null && b != null && a.replace('\\', '/').equals(b.replace('\\', '/'), ignoreCase = true)

/**
 * The menu on the left, the chat and its model in the middle as plain text, new chat and details
 * on the right; all on a frosted band that fades out downward.
 */
@Composable
fun TopIsland(m: AppModel, modifier: Modifier = Modifier) {
    val shell = LocalShell.current
    Box(modifier.fillMaxWidth()) {
        Box(Modifier.matchParentSize().frostBand(shell))
        Box(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Chrome.Side, vertical = 8.dp).height(Chrome.TopIsland),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.align(Alignment.CenterStart).size(44.dp).graphicsLayer { alpha = if (shell.docked) 0f else 1f }.glass(shell.cardHaze, CircleShape)) {
                IconBtn(Glyph.Sidebar, shell::openDrawer, tint = Ink.One, fill = Color.Transparent, size = 44.dp, enabled = !shell.docked, island = true)
            }
            Heading(m, shell, Modifier.padding(horizontal = 96.dp))
            Actions(m, shell, Modifier.align(Alignment.CenterEnd))
        }
    }
}

/** Blur under the islands, solid above and gone at the bottom edge, drawn into one offscreen layer. */
private fun Modifier.frostBand(shell: Shell): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            drawRect(Brush.verticalGradient(0.62f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
        }
        .then(if (Build.VERSION.SDK_INT >= 31) Modifier.hazeChild(shell.cardHaze, Band) else Modifier.background(Tone.App.copy(alpha = 0.92f)))

/** Ordered from worst to best, so the label rolls up when things get better. */
private enum class Kind { Offline, None, Loading, Ready }

/** The chat's title over its model and a chevron, as plain text; the model sheet grows out of it. */
@Composable
private fun Heading(m: AppModel, shell: Shell, modifier: Modifier) {
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

    MorphSource("pill", Surface.Model, modifier) {
        Column(
            Modifier.morph("pill", this, HeadShape)
                .graphicsLayer {
                    if (calm) return@graphicsLayer
                    scaleX = 1f - 0.06f * pulse.value
                    scaleY = scaleX
                }
                .pressable({ shell.open(Surface.Model, "pill") }, depth = 0.95f, feelOnDown = true)
                .heightIn(min = 48.dp)
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.flightLanding(shell.flight)) {
                Roll(title ?: tr("New chat", "Nowy czat"), Type.Body.copy(fontWeight = FontWeight.SemiBold), up = shell.sceneDir > 0)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(
                    kind == Kind.Loading || kind == Kind.Offline,
                    enter = fadeIn(tween(Motion.M2)) + expandHorizontally(Motion.calmText()),
                    exit = fadeOut(tween(Motion.M1)) + shrinkHorizontally(Motion.calmText()),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (seen.last == Kind.Offline) Icon(Glyph.Alert, Signal.Warn, size = 12.dp) else Spinner(accent, 11.dp)
                        Spacer(Modifier.width(5.dp))
                    }
                }
                Roll(
                    label,
                    Type.Meta.copy(fontWeight = FontWeight.Medium, color = if (kind == Kind.Offline) Signal.Warn else Ink.Three),
                    Modifier.weight(1f, fill = false),
                    up = seen.up,
                )
                Spacer(Modifier.width(3.dp))
                Icon(Glyph.Down, Ink.Three, size = 11.dp)
            }
        }
    }
}

/** One island: a pencil for a new chat (only once this one has started) and the dots for its details. */
@Composable
private fun Actions(m: AppModel, shell: Shell, modifier: Modifier) {
    val empty = m.current == null
    val pen = animateFloatAsState(if (empty) 0f else 1f, if (shell.calm) tween(Motion.M2) else spring(0.78f, 520f), label = "pen")
    Row(
        modifier.height(44.dp).glass(shell.cardHaze, Island).padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .layout { measurable, constraints ->
                    val p = measurable.measure(constraints)
                    layout((p.width * pen.value.coerceIn(0f, 1.08f)).roundToInt(), p.height) { p.place(0, 0) }
                }
                .graphicsLayer {
                    val k = pen.value.coerceIn(0f, 1f)
                    alpha = k
                    if (shell.calm) return@graphicsLayer
                    scaleX = 0.5f + 0.5f * pen.value
                    scaleY = scaleX
                },
        ) {
            Box(
                Modifier.size(40.dp).pressable(
                    { shell.newChat() }, CircleShape, Color.Transparent, Tone.Raise,
                    depth = 0.88f, enabled = !empty, stretch = true, feelOnDown = true,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.ph_pencil_simple_bold), tr("New chat", "Nowy czat"), Modifier.size(19.dp), colorFilter = ColorFilter.tint(Ink.One))
            }
        }
        IconBtn(Glyph.More, shell::openDetails, tint = Ink.One, fill = Color.Transparent, size = 40.dp, enabled = !empty, island = true)
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

internal val Efforts = listOf("off", "low", "medium", "high", "max")

internal fun effortLabels() = listOf(tr("Instant", "Od razu"), tr("Low", "Krótko"), tr("Medium", "Średnio"), tr("High", "Dużo"), "Max")

private enum class Phase { Idle, Current, Loading, Landed, Failed }

/** Publishers by the same tests, in the same order, as the faces in ModelFace. */
private val Publishers = listOf(
    "bonsai|prism" to "Prism ML",
    "deepseek" to "DeepSeek",
    "qwen|qwq" to "Qwen",
    "gemma" to "Google",
    "llama" to "Meta",
    "mi[sx]tral|ministral|magistral|devstral|codestral" to "Mistral AI",
    "(^|[^a-z])phi" to "Microsoft",
    "gpt-?oss" to "OpenAI",
    "granite" to "IBM",
    "smollm" to "Hugging Face",
    "glm" to "Z.ai",
    "kimi" to "Moonshot AI",
    "olmo" to "Ai2",
    "nemotron" to "NVIDIA",
    "lfm" to "Liquid AI",
).map { (test, name) -> Regex(test, RegexOption.IGNORE_CASE) to name }

private fun publisherOf(p: Probe): String? = "${p.name} ${p.family}".let { s -> Publishers.firstOrNull { it.first.containsMatchIn(s) }?.second }

/** The search field shows up once the list no longer fits at a glance. */
private const val Searchable = 6

/** The one model surface: what is loaded and how hard it thinks, then the library by publisher. */
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
                    delay(900)
                    shell.done()
                }
            }
        }
    }

    BasicText(tr("Model", "Model"), style = Type.BrandTitle)
    Spacer(Modifier.height(Space.S1))
    BasicText(tr("The model on your computer that writes the answers.", "Model na Twoim komputerze, który pisze odpowiedzi."), style = Type.Meta)
    Spacer(Modifier.height(Space.S4))

    val hold = animateFloatAsState(if (m.loading != null) 0.55f else 1f, tween(Motion.M2), label = "hold")
    Column(Modifier.fillMaxWidth().graphicsLayer { alpha = hold.value }.background(Tone.Raise, Shapes.Card).padding(Space.S4)) {
        AnimatedContent(
            loaded,
            Modifier.fillMaxWidth(),
            transitionSpec = {
                val enter = if (shell.calm) fadeIn(tween(Motion.M2))
                else fadeIn(tween(Motion.M2)) + slideInVertically(Motion.calmText()) { it / 6 } + scaleIn(Motion.calmText(), 0.96f)
                enter togetherWith fadeOut(tween(Motion.M1)) using SizeTransform(clip = true)
            },
            contentKey = { it?.path },
            label = "hero",
        ) { Hero(m, it) }
        AnimatedVisibility(
            loaded?.thinks == true,
            enter = expandVertically(tween(Motion.M2, easing = Motion.Out)) + fadeIn(tween(Motion.M2)),
            exit = shrinkVertically(tween(Motion.M2, easing = Motion.Out)) + fadeOut(tween(Motion.M1)),
        ) {
            Column(Modifier.padding(top = Space.S4)) {
                BasicText(tr("Thinking", "Myślenie"), style = Type.Label, modifier = Modifier.padding(start = Space.S1, bottom = Space.S2))
                Segmented(
                    effortLabels(),
                    Efforts.indexOf(m.effort ?: "medium").coerceAtLeast(0),
                    { m.pickEffort(Efforts[it]) },
                )
            }
        }
    }
    Spacer(Modifier.height(Space.S4))

    BasicText(tr("On your computer", "Na Twoim komputerze"), style = Type.Label, modifier = Modifier.padding(start = Space.S1, bottom = Space.S2))
    if (m.library.isEmpty()) {
        BasicText(
            tr("No models on the computer yet. Add one in Impulse › Models.", "Na komputerze nie ma jeszcze modeli. Dodaj je w Impulse › Modele."),
            style = Type.Body.copy(color = Ink.Three),
        )
    }
    var query by remember { mutableStateOf("") }
    val searching = m.library.size > Searchable
    if (searching) {
        Field(query, { query = it }, tr("Search models", "Szukaj modeli"))
        Spacer(Modifier.height(Space.S2))
    }
    val q = if (searching) query.trim() else ""
    val groups = remember(m.library, q) {
        m.library
            .filter { q.isEmpty() || it.name.contains(q, true) || publisherOf(it)?.contains(q, true) == true }
            .groupBy(::publisherOf)
            .entries.sortedWith(compareBy(nullsLast<String>()) { it.key?.lowercase() })
    }
    if (q.isNotEmpty() && groups.isEmpty()) {
        BasicText(tr("No models match “$q”.", "Żaden model nie pasuje do „$q”."), Modifier.padding(start = Space.S1, top = Space.S2), style = Type.Meta)
    }
    LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for ((publisher, rows) in groups) {
            item(key = "group:${publisher.orEmpty()}") {
                Row(Modifier.animateItem().padding(start = Space.S1, top = Space.S3, bottom = Space.S1), verticalAlignment = Alignment.CenterVertically) {
                    BasicText(publisher ?: tr("Other", "Inne"), style = Type.Label.copy(color = Ink.Two, fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.width(Space.S2))
                    BasicText("${rows.size}", style = Type.Label.copy(color = Ink.Four))
                }
            }
            items(rows, key = { it.path }) { p ->
                val mine = same(held, p.path)
                val phase = when {
                    same(m.loading, p.path) -> Phase.Loading
                    mine && landed == true -> Phase.Landed
                    mine && landed == false -> Phase.Failed
                    same(loaded?.path, p.path) -> Phase.Current
                    else -> Phase.Idle
                }
                ModelRow(m, p, phase, Modifier.animateItem())
            }
        }
    }
}

/** The loaded model as a card: face, name, what it is, and what it can do. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Hero(m: AppModel, l: Loaded?) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (l == null) {
                Box(Modifier.size(52.dp).background(Tone.Hover, Squircle(52.dp * 0.22f)), contentAlignment = Alignment.Center) {
                    Icon(Glyph.Models, Ink.Three, size = 22.dp)
                }
            } else {
                ModelFace(m, l.name, l.family, 52.dp)
            }
            Spacer(Modifier.width(Space.S3))
            Column(Modifier.weight(1f)) {
                BasicText(l?.name ?: tr("No model loaded", "Nie wczytano modelu"), style = Type.Section, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val line = if (l == null) tr("Tap one below to load it.", "Dotknij jednego poniżej, żeby go wczytać.")
                else m.library.firstOrNull { same(it.path, l.path) }?.let(::modelLine).orEmpty()
                if (line.isNotEmpty()) BasicText(line, style = Type.Meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (l != null) {
                Spacer(Modifier.width(Space.S2))
                Btn(tr("Unload", "Zwolnij"), { m.unload() }, variant = Variant.Quiet, height = 36.dp, enabled = m.loading == null)
            }
        }
        if (l != null) {
            Spacer(Modifier.height(Space.S3))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val memory = tokens(l.context.toLong())
                Tag(tr("$memory per chat", "$memory na czat"))
                if (l.thinks) Tag(tr("Thinking", "Myślenie"), Glyph.Brain)
                if (l.vision) Tag(tr("Vision", "Obraz"), Glyph.Eye)
            }
        }
    }
}

@Composable
private fun Tag(text: String, icon: Glyph? = null) {
    Row(
        Modifier.background(Tone.Hover, Shapes.ControlSm).padding(horizontal = Space.S2, vertical = Space.S1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, Ink.Two, size = 12.dp)
            Spacer(Modifier.width(Space.S1))
        }
        BasicText(text, style = Type.Label.copy(color = Ink.Two))
    }
}

@Composable
private fun ModelRow(m: AppModel, p: Probe, phase: Phase, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    val shell = LocalShell.current
    val calm = shell.calm
    val tappable = m.loading == null && phase != Phase.Landed
    val dim by animateFloatAsState(if (m.loading != null && phase != Phase.Loading) 0.45f else 1f, tween(Motion.M2), label = "dim")
    val rest by animateColorAsState(if (phase == Phase.Landed || phase == Phase.Current) Tone.Raise else Tone.Panel, tween(Motion.M2), label = "rest")
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
            Phase.Idle, Phase.Current -> {
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
                    { if (phase == Phase.Current) shell.done() else if (tappable) m.load(p) },
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
                else ModelFace(m, p.name, p.family, 36.dp)
            }
            Spacer(Modifier.width(Space.S3))
            Column(Modifier.weight(1f)) {
                BasicText(p.name, style = Type.Body.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText(modelLine(p), style = Type.Meta, maxLines = 1)
            }
            AnimatedVisibility(phase == Phase.Loading && secs >= 1, enter = fadeIn(tween(Motion.M2)), exit = fadeOut(tween(Motion.M1))) {
                Roll("$secs s", Type.Meta, Modifier.padding(start = Space.S2), digits = true)
            }
            if (phase == Phase.Current) {
                BasicText(tr("Loaded", "Wczytany"), Modifier.padding(start = Space.S2), style = Type.Label.copy(color = accent, fontWeight = FontWeight.SemiBold))
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

private val QuantName = Regex("(?<![A-Za-z0-9])(I?Q\\d(?:_[A-Z0-9]+)*|BF16|F16|F32|MXFP4)(?![A-Za-z0-9])", RegexOption.IGNORE_CASE)

/** The quantisation as the file name spells it (Q4_K_M, IQ3_XXS, BF16), else the format. */
fun quantOf(p: Probe): String =
    QuantName.findAll(p.path.substringAfterLast('/').substringAfterLast('\\')).lastOrNull()?.value?.uppercase() ?: p.format.uppercase()

fun modelLine(p: Probe): String {
    val params = when {
        p.params >= 1_000_000_000 -> "%.1fB".format(p.params / 1e9).replace(".0B", "B")
        p.params > 0 -> "%.0fM".format(p.params / 1e6)
        else -> null
    }
    val size = if (p.bytes > 0) "%.1f GB".format(p.bytes / 1e9) else null
    return listOfNotNull(params, quantOf(p), size).joinToString(" · ")
}

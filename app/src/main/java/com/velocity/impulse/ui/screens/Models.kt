package com.velocity.impulse.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Impulse
import com.velocity.impulse.data.Plan
import com.velocity.impulse.data.Probe
import com.velocity.impulse.ui.Chrome
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.Pill
import com.velocity.impulse.ui.components.Empty
import com.velocity.impulse.ui.components.Feel
import com.velocity.impulse.ui.components.Field
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Group
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.IconBtn
import com.velocity.impulse.ui.components.ModelFace
import com.velocity.impulse.ui.components.OnAccent
import com.velocity.impulse.ui.components.Roll
import com.velocity.impulse.ui.components.Segmented
import com.velocity.impulse.ui.components.Sheet
import com.velocity.impulse.ui.components.Toggle
import com.velocity.impulse.ui.components.feel
import com.velocity.impulse.ui.components.glass
import com.velocity.impulse.ui.components.glassSource
import com.velocity.impulse.ui.components.morph
import com.velocity.impulse.ui.components.morphBody
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.LocalReduceMotion
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Signal
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Squircle
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.roundToInt

/** A model's details and where they grew from, so they can shrink back into it. */
private data class Opened(val origin: String, val probe: Probe)

private const val LoadedOrigin = "models:loaded"

/** The library on the computer and the models that can be fetched for it. */
@Composable
fun ModelsPage(m: AppModel) {
    val hub = remember { HubState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var opened by remember { mutableStateOf<Opened?>(null) }
    var showing by remember { mutableStateOf(false) }
    var hw by remember { mutableStateOf<JSONObject?>(null) }
    val calm = LocalReduceMotion.current
    LaunchedEffect(Unit) { m.ask({ hardware() }) { hw = it } }
    WatchDownloads(m, hub)
    val out = opened?.origin?.takeIf { showing }
    val open: (String, Probe) -> Unit = { origin, p ->
        opened = Opened(origin, p)
        showing = true
    }

    PageFrame(
        tr("Models", "Modele"),
        overlay = {
            ModelCard(m, opened, showing, hw) { showing = false }
            HubSheets(m, hub)
        },
    ) {
        LoadedCard(m, out == LoadedOrigin, Modifier.rise(0)) { p -> open(LoadedOrigin, p) }
        Segmented(
            listOf(tr("On the computer", "Na komputerze"), tr("Find models", "Znajdź modele")),
            tab,
            { tab = it },
            Modifier.rise(1),
        )
        AnimatedContent(
            tab,
            Modifier.fillMaxWidth().rise(2),
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                if (calm) fadeIn(tween(Motion.M2)) togetherWith fadeOut(tween(Motion.M1))
                else (fadeIn(tween(Motion.M2, delayMillis = 40)) + slideInVertically(Motion.calmText()) { it / 14 * dir }) togetherWith
                    fadeOut(tween(Motion.M1)) using SizeTransform(clip = false)
            },
            label = "tab",
        ) { t ->
            Column(verticalArrangement = Arrangement.spacedBy(Space.S4)) {
                if (t == 0) Library(m, hub, out, { tab = 1 }, open)
                else HubBody(m, hub)
            }
        }
    }
}

/** What is loaded, or loading, as the page's first island; Unload sits on it and its details grow out of it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoadedCard(m: AppModel, out: Boolean, modifier: Modifier, onOpen: (Probe) -> Unit) {
    val l = m.status?.model
    val loading = m.loading
    val calm = LocalReduceMotion.current
    var failed by remember { mutableStateOf<String?>(null) }
    var secs by remember { mutableIntStateOf(0) }
    LaunchedEffect(m) {
        var was: String? = null
        snapshotFlow { m.loading to m.status?.model?.path }.collect { (now, path) ->
            if (now != null) {
                was = now
                failed = null
            } else if (was != null) {
                failed = if (samePath(path, was)) null else m.error
                was = null
            }
        }
    }
    LaunchedEffect(loading) {
        secs = 0
        if (loading != null) while (true) {
            delay(1000)
            secs++
        }
    }
    val p = l?.let { x -> m.library.firstOrNull { samePath(it.path, x.path) } }
    Source(out, modifier) {
        Column(
            Modifier.morph(LoadedOrigin, this, Shapes.Card)
                .fillMaxWidth()
                .pressable({ p?.let(onOpen) }, Shapes.Card, Tone.Panel, if (p != null) Tone.Raise else Tone.Panel, depth = if (p != null) 0.985f else 1f, feel = if (p != null) Feel.Tap else null, tilt = if (p != null) 2f else 0f)
                .padding(Space.S4),
        ) {
            AnimatedContent(
                loading ?: l?.path,
                Modifier.fillMaxWidth(),
                transitionSpec = {
                    val enter = if (calm) fadeIn(tween(Motion.M2))
                    else fadeIn(tween(Motion.M2)) + slideInVertically(Motion.calmText()) { it / 6 } + scaleIn(Motion.calmText(), 0.97f)
                    enter togetherWith fadeOut(tween(Motion.M1)) using SizeTransform(clip = true)
                },
                label = "loaded",
            ) { shown ->
                val coming = loading != null && shown == loading
                val name = if (coming) m.library.firstOrNull { samePath(it.path, shown) }?.name ?: shown.orEmpty().substringAfterLast('/').substringAfterLast('\\') else l?.name
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (name == null) {
                            Box(Modifier.size(52.dp).background(Tone.Hover, Squircle(52.dp * 0.22f)), contentAlignment = Alignment.Center) {
                                Icon(Glyph.Models, Ink.Three, size = 22.dp)
                            }
                        } else {
                            ModelFace(m, name, if (coming) "" else l?.family.orEmpty(), 52.dp)
                        }
                        Spacer(Modifier.width(Space.S3))
                        Column(Modifier.weight(1f)) {
                            BasicText(name ?: tr("No model loaded", "Nie wczytano modelu"), style = Type.Section, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            BasicText(
                                when {
                                    coming -> tr("Loading on the computer", "Wczytuję na komputerze")
                                    name == null -> tr("Pick one below to load it", "Wybierz poniżej, żeby go wczytać")
                                    else -> p?.let(::modelLine) ?: tr("Ready", "Gotowy")
                                },
                                style = Type.Meta, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (coming || l != null) {
                            Spacer(Modifier.width(Space.S2))
                            LoadButton(loadState(coming, l != null), {}, { m.unload() }, enabled = loading == null, size = 40.dp)
                        }
                    }
                    if (!coming && l != null) {
                        Spacer(Modifier.height(Space.S3))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Badge(tr("${tokens(l.context.toLong())} per chat", "${tokens(l.context.toLong())} na czat"))
                            if (l.thinks) Badge(tr("Thinking", "Myślenie"), Glyph.Brain)
                            if (l.vision) Badge(tr("Vision", "Obraz"), Glyph.Eye)
                        }
                    }
                }
            }
            AnimatedVisibility(loading != null && secs > 0, enter = fadeIn(tween(Motion.M2)), exit = fadeOut(tween(Motion.M1))) {
                Roll(tr("$secs s", "$secs s"), Type.Meta, Modifier.padding(top = Space.S2), digits = true)
            }
            Reveal(failed != null) {
                BasicText(failed.orEmpty(), Modifier.padding(top = Space.S3), style = Type.Meta.copy(color = Signal.Err))
            }
        }
    }
}

/**
 * Wraps what a model's details grow out of. It hides while it "is" the card and comes back as the
 * card shrinks into it; its space is held meanwhile so the list never closes up.
 */
@Composable
private fun Source(out: Boolean, modifier: Modifier = Modifier, content: @Composable AnimatedVisibilityScope.() -> Unit) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val d = LocalDensity.current
    val calm = LocalReduceMotion.current
    Box(modifier.fillMaxWidth().defaultMinSize(with(d) { size.width.toDp() }, with(d) { size.height.toDp() })) {
        AnimatedVisibility(!out || calm, enter = EnterTransition.None, exit = ExitTransition.None) {
            Box(Modifier.onSizeChanged { size = it }) { content() }
        }
    }
}

@Composable
private fun Library(m: AppModel, hub: HubState, out: String?, onDownloads: () -> Unit, onOpen: (String, Probe) -> Unit) {
    Arriving(hub, onDownloads)
    if (m.library.isEmpty()) {
        Empty(
            tr("No models on the computer yet", "Na komputerze nie ma jeszcze modeli"),
            tr("Find one under Find models and the computer downloads it.", "Znajdź model w zakładce Znajdź modele, a komputer go pobierze."),
        )
        return
    }
    var query by rememberSaveable { mutableStateOf("") }
    if (m.library.size > 6) Field(query, { query = it }, tr("Search models", "Szukaj modeli"))
    val q = query.trim()
    val groups = remember(m.library, q) {
        m.library
            .filter { q.isEmpty() || it.name.contains(q, true) || publisherOf(it)?.contains(q, true) == true }
            .groupBy(::publisherOf)
            .entries.sortedWith(compareBy(nullsLast<String>()) { it.key?.lowercase() })
    }
    if (q.isNotEmpty() && groups.isEmpty()) {
        BasicText(tr("No models match “$q”.", "Żaden model nie pasuje do „$q”."), Modifier.padding(start = Space.S1), style = Type.Meta)
    }
    for ((publisher, rows) in groups) {
        key(publisher.orEmpty()) {
            Group("${publisher ?: tr("Other", "Inne")} · ${rows.size}") {
                rows.forEach { p ->
                    key(p.path) {
                        val origin = "models:${p.path}"
                        Source(out == origin) {
                            Box(Modifier.morph(origin, this, RectangleShape)) { ModelRow(m, p) { onOpen(origin, p) } }
                        }
                    }
                }
            }
        }
    }
}

/** Its own scope, so the progress polled every second redraws this row and not the library. */
@Composable
private fun Arriving(hub: HubState, onClick: () -> Unit) {
    val moving = hub.downloads.filter { it.state in Moving }
    if (moving.isEmpty()) return
    val have = moving.sumOf { it.have }
    val total = moving.sumOf { it.total }.coerceAtLeast(1)
    Group {
        Item(
            if (moving.size == 1) tr("Downloading 1 model", "Pobieram 1 model") else tr("Downloading ${moving.size} models", "Pobieram modele: ${moving.size}"),
            "${(have * 100 / total)}% · ${gb(have)} " + tr("of", "z") + " ${gb(total)}",
            onClick,
        ) { Icon(Glyph.Right, Ink.Four, size = 16.dp) }
    }
}

@Composable
private fun ModelRow(m: AppModel, p: Probe, onClick: () -> Unit) {
    val loaded = samePath(m.status?.model?.path, p.path)
    val loading = samePath(m.loading, p.path)
    Item(p.name, modelLine(p), onClick, leading = { ModelFace(m, p.name, p.family, 40.dp) }) {
        LoadButton(loadState(loading, loaded), { m.load(p) }, { m.unload() }, enabled = m.loading == null)
    }
}

private val ChatSizes = listOf(2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576)

/** A header that declares no trained window gets the usual ladder; the service caps what it cannot honour. */
private fun sizesFor(trained: Int) = if (trained <= 0) ChatSizes.take(8) else ChatSizes.filter { it <= trained }.ifEmpty { listOf(trained) }

/** 8K unless chosen: a larger window reserves memory a chat rarely fills and can push layers off the card. */
private fun defaultSize(sizes: List<Int>, current: Int?) = current?.takeIf { it in sizes } ?: sizes.lastOrNull { it <= 8192 } ?: sizes.first()

private val Backends = listOf("auto", "llamacpp", "freetoken")

private class Fit(val title: String, val detail: String, val tone: Color?)

private fun fitOf(plan: Plan, layers: Int, gpu: Boolean): Fit {
    val on = plan.gpuLayers?.let { if (layers > 0) minOf(it, layers) else it }
    return when {
        on == null -> Fit(tr("The engine decides where it goes", "Silnik sam decyduje, gdzie go umieścić"), tr("The layers are placed when it loads.", "Warstwy zostaną rozmieszczone przy wczytaniu."), null)
        layers > 0 && on >= layers -> Fit(tr("Runs on your graphics card", "Działa na karcie graficznej"), tr("All $layers layers fit in graphics memory.", "Wszystkie $layers warstw mieści się w pamięci karty."), Signal.Ok)
        on <= 0 -> Fit(
            tr("Runs on the processor", "Działa na procesorze"),
            if (gpu) tr("At this window none of its $layers layers fit in graphics memory, so it writes slower.", "Przy tym oknie żadna z $layers warstw nie mieści się w pamięci karty, więc pisze wolniej.")
            else tr("No graphics card was found, so the processor runs it.", "Nie znaleziono karty graficznej, więc działa na procesorze."),
            null,
        )
        else -> Fit(
            tr("Partly on your graphics card", "Częściowo na karcie graficznej"),
            tr("$on of $layers layers fit in graphics memory; the rest runs on the processor, slower.", "$on z $layers warstw mieści się w pamięci karty; reszta działa na procesorze, wolniej."),
            Signal.Warn,
        )
    }
}

/** The details card: it grows out of the row or card it was opened from and shrinks back into it. */
@Composable
private fun BoxScope.ModelCard(m: AppModel, opened: Opened?, showing: Boolean, hw: JSONObject?, onClose: () -> Unit) {
    val calm = LocalReduceMotion.current
    val tall = LocalConfiguration.current.screenHeightDp.dp * 0.88f
    BackHandler(showing, onClose)
    AnimatedVisibility(showing, Modifier.matchParentSize(), enter = fadeIn(tween(Motion.M2)), exit = fadeOut(tween(Motion.M2))) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable(remember { MutableInteractionSource() }, null) { onClose() })
    }
    AnimatedVisibility(
        showing && opened != null,
        Modifier.align(Alignment.BottomCenter),
        enter = if (calm) fadeIn(tween(Motion.M2)) else EnterTransition.None,
        exit = if (calm) fadeOut(tween(Motion.M2)) else ExitTransition.None,
    ) {
        val o = opened ?: return@AnimatedVisibility
        Column(
            Modifier.navigationBarsPadding()
                .padding(Space.S2)
                .morph(o.origin, this, Shapes.Sheet)
                .background(Tone.Float, Shapes.Sheet)
                .fillMaxWidth()
                .heightIn(max = tall)
                .morphBody()
                .verticalScroll(rememberScrollState())
                .padding(Space.S4),
            verticalArrangement = Arrangement.spacedBy(Space.S4),
        ) {
            key(o.probe.path) { ModelDetails(m, o.probe, hw, onClose) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelDetails(m: AppModel, p: Probe, hw: JSONObject?, onClose: () -> Unit) {
    val loadedNow = m.status?.model
    val isLoaded = samePath(loadedNow?.path, p.path)
    val mfy = p.format == "mfy"
    val sizes = remember(p.trainContext) { sizesFor(p.trainContext) }
    var context by remember { mutableIntStateOf(defaultSize(sizes, loadedNow?.context?.takeIf { isLoaded })) }
    var pinned by remember { mutableStateOf<Int?>(null) }
    val steps = remember(sizes, pinned) { (sizes + listOfNotNull(pinned)).distinct().sorted() }
    var backend by remember { mutableStateOf("auto") }
    var gpu by remember { mutableStateOf<Int?>(null) }
    var moe by remember { mutableStateOf<Int?>(null) }
    var keep by remember { mutableStateOf(true) }
    var saved by remember { mutableStateOf<JSONObject?>(null) }
    var thinking by remember { mutableStateOf(true) }
    var plan by remember { mutableStateOf<Plan?>(null) }
    var planProblem by remember { mutableStateOf<String?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }
    var speed by remember { mutableStateOf<Pair<Double, Int>?>(null) }
    var removing by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }

    LaunchedEffect(p.path) {
        m.ask({ modelSettings(p.path) }) { s ->
            saved = s
            if (s == null) return@ask
            s.optInt("context").takeIf { it > 0 }?.let {
                context = it
                pinned = it
            }
            backend = s.optString("backend").takeIf { it in Backends } ?: "auto"
            gpu = if (s.has("gpu_layers") && !s.isNull("gpu_layers")) s.optInt("gpu_layers") else null
            moe = s.optJSONObject("tuning")?.takeIf { it.has("n_cpu_moe") && !it.isNull("n_cpu_moe") }?.optInt("n_cpu_moe")
            if (s.has("thinking")) thinking = s.optBoolean("thinking", true)
        }
    }
    LaunchedEffect(p.name, m.chats) {
        speed = withContext(Dispatchers.Default) {
            val rates = m.chats.flatMap { it.messages }
                .filter { it.role == "assistant" && it.meta?.optString("model") == p.name }
                .mapNotNull { it.receipt?.optDouble("decode_tok_s")?.takeIf { v -> !v.isNaN() && v > 0 } }
                .sorted()
            if (rates.isEmpty()) null else rates[rates.size / 2] to rates.size
        }
    }
    if (!mfy) LaunchedEffect(context, backend) {
        delay(220)
        val asked = context to backend
        m.ask({ probe(p.path, context, backend.takeIf { it != "auto" }) }, { if (asked == (context to backend)) planProblem = it }) {
            if (asked == (context to backend)) {
                plan = it
                planProblem = null
            }
        }
    }

    val info = plan?.probe
    val layers = info?.optInt("layers") ?: 0
    val experts = info?.optInt("experts") ?: 0
    val unloadable = info?.optString("unloadable")?.takeIf { it.isNotBlank() && it != "null" }
    val sees = info?.optString("projector")?.let { it.isNotBlank() && it != "null" } == true
    val thinks = isLoaded && loadedNow?.thinks == true

    Row(verticalAlignment = Alignment.CenterVertically) {
        ModelFace(m, p.name, p.family, 52.dp)
        Spacer(Modifier.width(Space.S3))
        Column(Modifier.weight(1f)) {
            BasicText(p.name, style = Type.BrandSection, maxLines = 2, overflow = TextOverflow.Ellipsis)
            BasicText(publisherOf(p) ?: p.family.ifBlank { p.format.uppercase() }, style = Type.Meta, maxLines = 1)
        }
        Spacer(Modifier.width(Space.S2))
        IconBtn(Glyph.Close, onClose, size = 36.dp)
    }
    if (thinks || sees) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (thinks) Badge(tr("Thinking", "Myślenie"), Glyph.Brain)
            if (sees) Badge(tr("Vision", "Obraz"), Glyph.Eye)
        }
    }

    Group {
        if (p.params > 0) Item(tr("Parameters", "Parametry"), params(p.params), ground = Tone.Raise)
        Item(tr("Compression", "Kompresja"), quantOf(p), ground = Tone.Raise)
        Item(tr("Size on disk", "Rozmiar na dysku"), gb(p.bytes), ground = Tone.Raise)
        if (p.trainContext > 0) Item(tr("Longest chat it was trained on", "Najdłuższy czat z treningu"), tokens(p.trainContext.toLong()), ground = Tone.Raise)
        if (experts > 0) {
            val used = info?.optInt("experts_used") ?: 0
            Item(tr("Mixture of experts", "Mieszanka ekspertów"), tr("$used of $experts experts for each token", "$used z $experts ekspertów na token"), ground = Tone.Raise)
        }
        speed?.let { (v, n) ->
            Item(tr("Writing speed", "Szybkość pisania"), "%.1f ".format(v) + tr("tokens/s · median of $n answers", "tokenów/s · mediana z $n odpowiedzi"), ground = Tone.Raise)
        }
    }

    if (unloadable != null) {
        Note(tr("This file cannot load", "Tego pliku nie da się wczytać"), unloadable, Signal.Err)
    } else if (!mfy) {
        RunCard(plan, planProblem, context, layers, hw)
    }

    if (unloadable == null) {
        if (!mfy) {
            Column(
                Modifier.fillMaxWidth().background(Tone.Raise, Shapes.Tile).padding(Space.S4),
                verticalArrangement = Arrangement.spacedBy(Space.S4),
            ) {
                Window(steps, context, p.trainContext) { context = it }
                Column(verticalArrangement = Arrangement.spacedBy(Space.S2)) {
                    BasicText(tr("Engine", "Silnik"), style = Type.Body.copy(fontWeight = FontWeight.Medium))
                    Segmented(
                        listOf(tr("Automatic", "Auto"), "llama.cpp", tr("Experts", "Eksperci")),
                        Backends.indexOf(backend).coerceAtLeast(0),
                        { backend = Backends[it] },
                    )
                }
                Steps(
                    tr("Layers on the graphics card", "Warstwy na karcie graficznej"),
                    gpu, layers,
                    { n -> tr("$n of $layers", "$n z $layers") },
                ) { gpu = it }
                if (experts > 0) {
                    Steps(
                        tr("Expert layers on the processor", "Warstwy ekspertów na procesorze"),
                        moe, layers,
                        { n -> tr("$n of $layers", "$n z $layers") },
                    ) { moe = it }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        BasicText(tr("Remember these", "Zapamiętaj"), style = Type.Body.copy(fontWeight = FontWeight.Medium))
                        BasicText(tr("Loads this model this way next time", "Następnym razem wczyta ten model tak samo"), style = Type.Meta)
                    }
                    Toggle(keep, { keep = it }, description = tr("Remember these", "Zapamiętaj"))
                }
                if (saved != null) {
                    Pill(tr("Reset to recommended", "Przywróć zalecane"), {
                        m.ask({ modelSettings(p.path, reset = true) }, { problem = it }) {
                            saved = null
                            backend = "auto"
                            gpu = null
                            moe = null
                            pinned = null
                            context = defaultSize(sizes, null)
                        }
                    }, Modifier.fillMaxWidth(), ink = Ink.Two, fill = Tone.Hover)
                }
            }
        }
        val same = isLoaded && loadedNow?.context == context && saved?.optString("backend").orEmpty().ifBlank { "auto" } == backend
        val unload = isLoaded && same
        Pill(
            when {
                samePath(m.loading, p.path) -> tr("Loading", "Wczytuję")
                unload -> tr("Unload", "Zwolnij")
                isLoaded -> tr("Load again with ${windowLabel(context)}", "Wczytaj ponownie z ${windowLabel(context)}")
                else -> tr("Load", "Wczytaj")
            },
            {
                if (unload) {
                    m.unload()
                } else {
                    if (mfy) {
                        m.load(p)
                    } else {
                        val tuning = JSONObject(saved?.optJSONObject("tuning")?.toString() ?: "{}")
                        if (moe != null) tuning.put("n_cpu_moe", moe) else tuning.remove("n_cpu_moe")
                        m.load(p.path, p.name, context, gpu, backend.takeIf { it != "auto" }, tuning.takeIf { it.length() > 0 }, keep)
                    }
                    onClose()
                }
            },
            Modifier.fillMaxWidth(),
            ink = if (unload) Ink.One else OnAccent,
            fill = if (unload) Tone.Hover else LocalAccent.current,
            enabled = m.loading == null,
            height = 50.dp,
        )
    }

    Group {
        SwitchRow(
            tr("Think before answering", "Myśl przed odpowiedzią"),
            tr("For this model, when the chat's thinking is on. Off goes straight to the answer.", "Dla tego modelu, gdy myślenie w czacie jest włączone. Wyłączone od razu odpowiada."),
            thinking,
            ground = Tone.Raise,
        ) { v ->
            thinking = v
            m.ask({ modelSettings(p.path, thinking = v) }, { problem = it; thinking = !v })
        }
    }

    Reveal(!confirm) {
        Pill(
            if (removing) tr("Deleting", "Usuwam") else tr("Delete from the computer", "Usuń z komputera"),
            { confirm = true },
            Modifier.fillMaxWidth(),
            ink = Signal.Err,
            enabled = !isLoaded && !removing,
        )
    }
    Reveal(confirm) {
        Column(Modifier.fillMaxWidth().background(Tone.Raise, Shapes.Tile).padding(Space.S4), verticalArrangement = Arrangement.spacedBy(Space.S2)) {
            BasicText(tr("Delete ${p.name}?", "Usunąć ${p.name}?"), style = Type.Body.copy(fontWeight = FontWeight.SemiBold))
            BasicText(
                tr("The file (${gb(p.bytes)}) is removed from the computer's disk. Getting it back means downloading it again.", "Plik (${gb(p.bytes)}) zniknie z dysku komputera. Odzyskanie go to ponowne pobranie."),
                style = Type.Meta,
            )
            Row(Modifier.padding(top = Space.S1), horizontalArrangement = Arrangement.spacedBy(Space.S2)) {
                Pill(tr("Keep", "Zostaw"), { confirm = false }, Modifier.weight(1f), fill = Tone.Hover)
                Pill(tr("Delete", "Usuń"), {
                    confirm = false
                    removing = true
                    m.ask({ removeModel(p.path) }, { problem = it; removing = false }) {
                        m.refreshAll()
                        onClose()
                    }
                }, Modifier.weight(1f), ink = Signal.Err, fill = Tone.Hover)
            }
        }
    }
    if (isLoaded) BasicText(tr("Unload it first to delete it.", "Najpierw go zwolnij, żeby usunąć."), Modifier.fillMaxWidth(), style = Type.Meta.copy(textAlign = TextAlign.Center))
    Problem(problem)
}

@Composable
private fun RunCard(plan: Plan?, problem: String?, context: Int, layers: Int, hw: JSONObject?) {
    val gpu = hw?.optBoolean("gpu") ?: true
    Column(Modifier.fillMaxWidth().background(Tone.Raise, Shapes.Tile).padding(Space.S4), verticalArrangement = Arrangement.spacedBy(Space.S1)) {
        BasicText(tr("How it would run here", "Jak by tu działał"), style = Type.Label)
        if (plan == null) {
            if (problem != null) BasicText(problem, style = Type.Meta.copy(color = Signal.Err))
            else Row(verticalAlignment = Alignment.CenterVertically) {
                Spinner(Ink.Three, 14.dp)
                Spacer(Modifier.width(Space.S2))
                BasicText(tr("Working it out at ${windowLabel(context)}", "Liczę dla ${windowLabel(context)}"), style = Type.Meta)
            }
            return@Column
        }
        val fit = fitOf(plan, layers, gpu)
        val dot by animateColorAsState(fit.tone ?: Ink.Three, tween(Motion.M2), label = "fit")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).drawBehind { drawCircle(dot) })
            Spacer(Modifier.width(Space.S2))
            Roll(fit.title, Type.Body.copy(fontWeight = FontWeight.SemiBold))
        }
        BasicText(fit.detail, style = Type.Meta)
        val vram = hw?.optLong("vram_total_mb") ?: 0L
        if (plan.vramWeightsMb + plan.vramKvMb > 0) {
            BasicText(
                tr(
                    "Graphics memory: ${mbGb(plan.vramWeightsMb)} for the model, ${mbGb(plan.vramKvMb)} for the chat" + (if (vram > 0) ", of ${mbGb(vram)}" else ""),
                    "Pamięć karty: ${mbGb(plan.vramWeightsMb)} na model, ${mbGb(plan.vramKvMb)} na czat" + (if (vram > 0) ", z ${mbGb(vram)}" else ""),
                ),
                style = Type.Meta.copy(color = Ink.Two),
            )
        }
        plan.warnings.forEach { w ->
            Row(Modifier.padding(top = Space.S1)) {
                Icon(Glyph.Alert, Signal.Warn, Modifier.padding(top = 2.dp), size = 14.dp)
                Spacer(Modifier.width(Space.S2))
                BasicText(w, style = Type.Meta.copy(color = Ink.Two))
            }
        }
    }
}

/** An optional number with "Automatic" beside it: the slider sets it, the capsule gives the choice back. */
@Composable
private fun Steps(title: String, value: Int?, max: Int, label: (Int) -> String, onChange: (Int?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.S1)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BasicText(title, style = Type.Body.copy(fontWeight = FontWeight.Medium))
                Roll(value?.let(label) ?: tr("Automatic", "Automatycznie"), Type.Meta, digits = value != null)
            }
            Chips(listOf(tr("Auto", "Auto")), if (value == null) 0 else -1, { onChange(null) })
        }
        if (max > 0) Slider(value ?: 0, max, { onChange(it) }, dim = value == null)
    }
}

/** The window per chat on a slider that steps through sizes that make sense, up to what it was trained on. */
@Composable
private fun Window(steps: List<Int>, value: Int, trained: Int, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.S1)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BasicText(tr("Window per chat", "Okno na czat"), style = Type.Body.copy(fontWeight = FontWeight.Medium))
                BasicText(
                    if (trained > 0) tr("Trained on up to ${tokens(trained.toLong())}", "Trenowany do ${tokens(trained.toLong())}")
                    else tr("A larger window reserves more memory", "Większe okno rezerwuje więcej pamięci"),
                    style = Type.Meta,
                )
            }
            Roll(windowLabel(value), Type.Section, digits = true)
        }
        if (steps.size > 1) {
            Slider(steps.indexOf(value).coerceAtLeast(0), steps.lastIndex, { onChange(steps[it]) })
            Row {
                BasicText(windowLabel(steps.first()), style = Type.Label)
                Spacer(Modifier.weight(1f))
                BasicText(windowLabel(steps.last()), style = Type.Label)
            }
        }
    }
}

/** A transfer that is still on its way, paused or waiting. */
internal val Moving = setOf("queued", "running", "verifying", "paused")

/**
 * A full page. The large title scrolls away with the content and comes back small on a frosted
 * island at the top; [overlay] (sheets) floats above everything. The host draws the back button.
 */
@Composable
internal fun PageFrame(title: String, overlay: @Composable BoxScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    val haze = remember { HazeState() }
    val reach = with(LocalDensity.current) { 56.dp.toPx() }
    val titled by remember { derivedStateOf { scroll.value > reach } }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Tone.App).glassSource(haze)) {
            Page(scroll) {
                BasicText(
                    title,
                    Modifier.graphicsLayer {
                        val k = (scroll.value / reach).coerceIn(0f, 1f)
                        alpha = 1f - k
                        scaleX = 1f - 0.1f * k
                        scaleY = scaleX
                        transformOrigin = TransformOrigin(0f, 1f)
                    },
                    style = Type.BrandHero,
                )
                content()
            }
        }
        AnimatedVisibility(
            titled,
            Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(vertical = 8.dp),
            enter = fadeIn(tween(Motion.M1)) + scaleIn(spring(0.8f, 600f), 0.9f),
            exit = fadeOut(tween(Motion.M1)) + scaleOut(tween(Motion.M1), 0.94f),
        ) {
            Box(Modifier.height(44.dp).glass(haze, Chrome.Capsule).padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                BasicText(title, style = Type.Section)
            }
        }
        overlay()
    }
}

/** Runs [block] on the computer; a failure comes back as words to [fail] instead of the app-wide error. */
internal fun <T> AppModel.ask(block: suspend Impulse.() -> T, fail: (String) -> Unit = {}, done: (T) -> Unit = {}) =
    request({ runCatching { block(this) } }) { r ->
        r.fold(done) { e -> fail(e.message?.takeIf { it.isNotBlank() } ?: tr("The computer did not answer.", "Komputer nie odpowiedział.")) }
    }

/** Blocks rise in one after another on a spring with no overshoot, so text never bounces. */
@Composable
internal fun Modifier.rise(order: Int): Modifier {
    val calm = LocalReduceMotion.current
    val lift = with(LocalDensity.current) { 24.dp.toPx() }
    val p = remember { Animatable(if (calm) 1f else 0f) }
    LaunchedEffect(Unit) {
        delay(60L + 50L * order)
        p.animateTo(1f, spring(1f, 260f))
    }
    return graphicsLayer {
        val k = p.value
        alpha = k
        translationY = (1f - k) * lift
        rotationX = (1f - k) * 5f
        cameraDistance = 16f * density
        transformOrigin = TransformOrigin(0.5f, 0f)
    }
}

/** One row of a group. The row never moves; only its ground deepens under the finger. */
@Composable
internal fun Item(
    title: String,
    detail: String? = null,
    onClick: (() -> Unit)? = null,
    feel: Feel? = Feel.Tap,
    ground: Color = Tone.Panel,
    detailStyle: TextStyle = Type.Meta,
    lines: Int = 2,
    leading: (@Composable () -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val base = Modifier.fillMaxWidth().heightIn(min = 56.dp)
    Row(
        (if (onClick != null) base.pressable(onClick, RectangleShape, ground, if (ground == Tone.Panel) Tone.Raise else Tone.Hover, depth = 1f, feel = feel) else base.background(ground))
            .padding(horizontal = Space.S4, vertical = Space.S3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(Space.S3))
        }
        Column(Modifier.weight(1f)) {
            BasicText(title, style = Type.Body.copy(fontWeight = FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (detail != null) BasicText(detail, style = detailStyle, maxLines = lines, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(Space.S3))
        trailing()
    }
}

@Composable
internal fun SwitchRow(title: String, detail: String, on: Boolean, ground: Color = Tone.Panel, onChange: (Boolean) -> Unit) {
    Item(title, detail, { onChange(!on) }, feel = if (on) Feel.Off else Feel.On, ground = ground, lines = 3) { Toggle(on, onChange, description = title) }
}

/** A row that opens in place; the chevron turns and what it holds unfolds under it. */
@Composable
internal fun Unfold(title: String, open: Boolean, onToggle: () -> Unit, detail: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val turn by animateFloatAsState(if (open) 90f else 0f, Motion.travel(), label = "fold")
    Column(Modifier.fillMaxWidth().background(Tone.Raise)) {
        Item(title, detail, onToggle, feel = Feel.Tick, ground = Tone.Raise) {
            Icon(Glyph.Right, Ink.Three, Modifier.graphicsLayer { rotationZ = turn }, size = 16.dp)
        }
        Reveal(open) {
            Column(Modifier.padding(start = Space.S4, end = Space.S4, bottom = Space.S4), verticalArrangement = Arrangement.spacedBy(Space.S3), content = content)
        }
    }
}

@Composable
internal fun Reveal(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible,
        enter = expandVertically(Motion.calmText()) + fadeIn(tween(Motion.M2, delayMillis = 60)),
        exit = shrinkVertically(Motion.calmText()) + fadeOut(tween(Motion.M1)),
    ) { content() }
}

/** Capsules to pick one of; the chosen one fills with the accent. [selected] -1 leaves all unfilled. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Chips(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            val fill by animateColorAsState(if (on) accent else Tone.Hover, tween(Motion.M2), label = "chip")
            val ink by animateColorAsState(if (on) OnAccent else Ink.Two, tween(Motion.M2), label = "chipInk")
            Box(
                Modifier.height(34.dp).pressable({ onSelect(i) }, Chrome.Capsule, fill, fill, depth = 0.94f, feel = Feel.Tick, stretch = true).padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(label, style = Type.Meta.copy(color = ink, fontWeight = FontWeight.SemiBold), maxLines = 1)
            }
        }
    }
}

/** A whole-number slider from 0 to [max]: it ticks at each step and the knob follows the finger. */
@Composable
internal fun Slider(value: Int, max: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, dim: Boolean = false) {
    val view = LocalView.current
    val accent = LocalAccent.current
    val calm = LocalReduceMotion.current
    val now by rememberUpdatedState(value)
    val change by rememberUpdatedState(onChange)
    val auto by rememberUpdatedState(dim)
    val pos by animateFloatAsState(value.toFloat(), if (calm) tween(0) else spring(1f, 900f), label = "knob")
    val fade by animateFloatAsState(if (dim) 0.4f else 1f, tween(Motion.M2), label = "dim")
    val knob = 24.dp
    fun Float.toStep(width: Int, k: Float) = (((this - k / 2) / (width - k).coerceAtLeast(1f)) * max).roundToInt().coerceIn(0, max)
    fun set(i: Int) {
        if (i != now || auto) {
            view.feel(Feel.Tick)
            change(i)
        }
    }
    Box(
        modifier.fillMaxWidth().height(36.dp)
            .pointerInput(max) { detectTapGestures { o -> set(o.x.toStep(size.width, knob.toPx())) } }
            .pointerInput(max) {
                detectHorizontalDragGestures(onDragStart = { o -> set(o.x.toStep(size.width, knob.toPx())) }) { c, _ ->
                    c.consume()
                    set(c.position.x.toStep(size.width, knob.toPx()))
                }
            }
            .drawBehind {
                val r = knob.toPx() / 2
                val run = size.width - r * 2
                val cy = size.height / 2
                val cx = r + run * (pos / max.coerceAtLeast(1)).coerceIn(0f, 1f)
                val h = 6.dp.toPx()
                val corner = CornerRadius(h / 2)
                drawRoundRect(Tone.Inset, Offset(0f, cy - h / 2), Size(size.width, h), corner)
                drawRoundRect(accent.copy(alpha = fade), Offset(0f, cy - h / 2), Size(cx + h / 2, h), corner)
                drawCircle(Color.Black.copy(alpha = 0.3f), r, Offset(cx, cy + 1.5.dp.toPx()))
                drawCircle(Color.White.copy(alpha = 0.6f + 0.4f * fade), r, Offset(cx, cy))
            },
    )
}

/** A thin bar that fills to [fraction]; it glides, it never jumps. */
@Composable
internal fun Bar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), spring(1f, 120f), label = "bar")
    Box(
        modifier.fillMaxWidth().height(6.dp).drawBehind {
            val corner = CornerRadius(size.height / 2)
            drawRoundRect(Tone.Inset, cornerRadius = corner)
            if (f > 0f) drawRoundRect(color, size = Size((size.width * f).coerceAtLeast(size.height), size.height), cornerRadius = corner)
        },
    )
}

@Composable
internal fun Badge(text: String, icon: Glyph? = null, color: Color = Ink.Two) {
    Row(Modifier.background(Tone.Hover, Chrome.Capsule).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, color, size = 12.dp)
            Spacer(Modifier.width(Space.S1))
        }
        BasicText(text, style = Type.Label.copy(color = color))
    }
}

/** A short card with a coloured mark: something to know before acting. */
@Composable
internal fun Note(title: String, body: String, tone: Color) {
    Row(Modifier.fillMaxWidth().background(Tone.Raise, Shapes.Tile).padding(Space.S4)) {
        Icon(Glyph.Alert, tone, Modifier.padding(top = 2.dp), size = 18.dp)
        Spacer(Modifier.width(Space.S3))
        Column {
            BasicText(title, style = Type.Body.copy(fontWeight = FontWeight.SemiBold))
            BasicText(body, style = Type.Meta.copy(color = Ink.Two))
        }
    }
}

/** A question before something that cannot be undone. */
@Composable
internal fun Ask(open: Boolean, onClose: () -> Unit, title: String, body: String, action: String, onAction: () -> Unit) {
    Sheet(open, onClose) {
        Column(Modifier.padding(top = Space.S2), verticalArrangement = Arrangement.spacedBy(Space.S2)) {
            BasicText(title, style = Type.BrandSection)
            BasicText(body, style = Type.Body.copy(color = Ink.Three))
            Spacer(Modifier.height(Space.S3))
            Pill(action, { onClose(); onAction() }, Modifier.fillMaxWidth(), ink = Signal.Err, height = 50.dp)
            Pill(tr("Cancel", "Anuluj"), onClose, Modifier.fillMaxWidth(), ink = Ink.Two, fill = Color.Transparent, height = 50.dp)
        }
    }
}

internal fun samePath(a: String?, b: String?) =
    a != null && b != null && a.replace('\\', '/').equals(b.replace('\\', '/'), ignoreCase = true)

internal fun gb(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1e9)
    bytes >= 1_000_000 -> "%.0f MB".format(bytes / 1e6)
    else -> "%.0f KB".format(bytes / 1e3)
}

internal fun mbGb(mb: Long) = if (mb >= 1024) "%.1f GB".format(mb / 1024.0) else "$mb MB"

private fun params(n: Long) = if (n >= 1_000_000_000) "%.1fB".format(n / 1e9).replace(".0B", "B") else "%.0fM".format(n / 1e6)

/** Window sizes as the load picker names them: 8K, 128K, 1M. */
private fun windowLabel(n: Int): String = when {
    n >= 1_048_576 && n % 1_048_576 == 0 -> "${n / 1_048_576}M"
    n >= 1024 -> "${(n / 1024.0).roundToInt()}K"
    else -> "$n"
}

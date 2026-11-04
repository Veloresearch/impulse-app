package com.velocity.impulse.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.components.Feel
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.IconBtn
import com.velocity.impulse.ui.components.feel
import com.velocity.impulse.ui.components.glass
import com.velocity.impulse.ui.components.glassSource
import com.velocity.impulse.ui.components.rubber
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalReduceMotion
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Radius
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * A file to look at full screen. [image] is a data: URL or bare base64; [thumb], when given, is
 * shown while [image] decodes. [kept] means the chat only holds a small copy and the rest stays
 * on the computer.
 */
class PreviewItem(
    val name: String,
    val bytes: Long,
    val image: String? = null,
    val text: String? = null,
    val thumb: String? = null,
    val kept: Boolean = false,
)

object PreviewState {
    var item by mutableStateOf<PreviewItem?>(null)
        private set
    var showing by mutableStateOf(false)
        private set
    internal var from by mutableStateOf(Rect.Zero)
        private set

    /** Opens [item] growing out of [from], the tapped thumbnail's bounds in root. */
    fun open(item: PreviewItem, from: Rect) {
        this.item = item
        this.from = from
        showing = true
    }

    fun close() {
        showing = false
    }

    internal fun done() {
        if (!showing) item = null
    }
}

private const val MAX_ZOOM = 4f

@Composable
fun PreviewHost() {
    val item = PreviewState.item ?: return
    val showing = PreviewState.showing
    val calm = LocalReduceMotion.current
    val keyboard = LocalSoftwareKeyboardController.current
    val p = remember { Animatable(0f) }
    LaunchedEffect(showing) {
        if (showing) keyboard?.hide()
        val spec: AnimationSpec<Float> = if (calm) tween(Motion.M2) else Motion.morph()
        p.animateTo(if (showing) 1f else 0f, spec)
        PreviewState.done()
    }
    BackHandler(showing) { PreviewState.close() }

    val shift = remember { arrayOf(Offset.Zero) }
    val from = { if (calm) null else PreviewState.from.translate(-shift[0]) }
    val picture = remember(item) { (item.thumb ?: item.image)?.let(::bitmap) }
    Box(
        Modifier.fillMaxSize()
            .onPlaced { shift[0] = it.positionInRoot() }
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false) } },
    ) {
        if (picture != null) Picture(item, picture, { p.value }, from) else Document(item, { p.value }, from)
    }
}

@Composable
private fun Picture(item: PreviewItem, small: ImageBitmap, progress: () -> Float, from: () -> Rect?) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val haze = remember { HazeState() }
    val d = LocalDensity.current
    val tile = with(d) { Radius.Tile.toPx() }
    val card = with(d) { Radius.Card.toPx() }
    val reach = with(d) { 48.dp.toPx() }
    val edge = with(d) { 96.dp.toPx() }
    val zoom = remember(item) { Animatable(1f) }
    val pan = remember(item) { Animatable(Offset.Zero, Offset.VectorConverter) }
    val drag = remember(item) { Animatable(Offset.Zero, Offset.VectorConverter) }
    val full = produceState<ImageBitmap?>(null, item) {
        val src = item.image
        if (item.thumb != null && src != null) value = withContext(Dispatchers.Default) { bitmap(src) }
    }
    val hole = remember { Path() }
    val showing = PreviewState.showing
    LaunchedEffect(showing) {
        if (!showing) {
            launch { zoom.animateTo(1f, Motion.morph()) }
            pan.animateTo(Offset.Zero, Motion.morph())
        }
    }

    Canvas(
        Modifier.fillMaxSize()
            .glassSource(haze)
            .pointerInput(item) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    if (!PreviewState.showing) return@awaitEachGesture
                    val box = size.toSize()
                    val c = box.center
                    val f = fit(box, small)
                    val tracker = VelocityTracker()
                    var mode = 0
                    var travel = Offset.Zero
                    var rawZoom = zoom.value
                    var raw = pan.value
                    var moved = drag.value
                    do {
                        val e = awaitPointerEvent()
                        val step = e.calculatePan()
                        e.changes.firstOrNull { it.pressed }?.let { tracker.addPointerInputChange(it) }
                        if (mode == 0) {
                            travel += step
                            mode = when {
                                e.changes.count { it.pressed } > 1 -> 1
                                travel.getDistance() < viewConfiguration.touchSlop -> 0
                                zoom.value > 1.01f || abs(travel.x) > abs(travel.y) -> 1
                                else -> 2
                            }
                        }
                        if (mode == 1) {
                            val k = e.calculateZoom()
                            val q = e.calculateCentroid().takeIf { it.isSpecified } ?: c
                            rawZoom *= k
                            raw = (q - c) - (q - c - raw) * k + step
                            val z = when {
                                rawZoom > MAX_ZOOM -> MAX_ZOOM + rubber(rawZoom - MAX_ZOOM, 1f)
                                rawZoom < 1f -> 1f + rubber(rawZoom - 1f, 0.3f)
                                else -> rawZoom
                            }
                            val b = bounds(f, z, box)
                            val o = Offset(band(raw.x, b.x, reach), band(raw.y, b.y, reach))
                            scope.launch { zoom.snapTo(z) }
                            scope.launch { pan.snapTo(o) }
                        } else if (mode == 2) {
                            val was = abs(moved.y) > edge
                            moved += step
                            if (was != abs(moved.y) > edge) view.feel(if (was) Feel.Unedge else Feel.Edge)
                            val to = moved
                            scope.launch { drag.snapTo(to) }
                        }
                        if (mode != 0) e.changes.forEach { if (it.positionChanged()) it.consume() }
                    } while (e.changes.any { it.pressed })

                    val v = tracker.calculateVelocity().let { Offset(it.x, it.y) }
                    when (mode) {
                        1 -> {
                            val z = rawZoom.coerceIn(1f, MAX_ZOOM)
                            val glide = if (z > 1f) v * 0.15f else Offset.Zero
                            val to = clamp((pan.value + glide) * (z / zoom.value), bounds(f, z, box))
                            scope.launch { zoom.animateTo(z, Motion.morph()) }
                            scope.launch { pan.animateTo(to, spring(1f, 220f)) }
                        }
                        2 -> if (abs(moved.y) > edge || abs(v.y) > 1200.dp.toPx()) {
                            PreviewState.close()
                        } else {
                            scope.launch { drag.animateTo(Offset.Zero, spring(0.72f, 420f)) }
                        }
                    }
                }
            }
            .pointerInput(item) {
                detectTapGestures(onDoubleTap = { q ->
                    if (!PreviewState.showing) return@detectTapGestures
                    view.feel(Feel.Tick)
                    val box = size.toSize()
                    val z = if (zoom.value > 1.05f) 1f else 2.5f
                    val to = clamp((q - box.center) * (1f - z), bounds(fit(box, small), z, box))
                    scope.launch { zoom.animateTo(z, Motion.morph()) }
                    scope.launch { pan.animateTo(to, Motion.morph()) }
                })
            },
    ) {
        val t = progress()
        val dragged = (abs(drag.value.y) / size.height).coerceIn(0f, 1f)
        drawRect(Color.Black, alpha = 0.94f * t * (1f - dragged * 1.4f).coerceIn(0f, 1f))
        val f = fit(size, small)
        val k = zoom.value * (1f - 0.35f * dragged)
        val base = around(f.center + pan.value + drag.value, f.width * k, f.height * k)
        val start = from()
        val pic = if (start == null) base else lerp(cover(start, small), base, t)
        val window = if (start == null) base else lerp(start, base, t)
        hole.reset()
        hole.addRoundRect(RoundRect(window, CornerRadius(tile * (1f - t) + card * min(1f, dragged * 4f))))
        val img = full.value ?: small
        clipPath(hole) {
            translate(pic.left, pic.top) {
                scale(pic.width / img.width, pic.height / img.height, Offset.Zero) {
                    drawImage(img, alpha = if (start == null) t else 1f)
                }
            }
        }
    }
    Box(
        Modifier.fillMaxSize().graphicsLayer {
            alpha = ((progress() - 0.6f) / 0.4f).coerceIn(0f, 1f) * (1f - abs(drag.value.y) / size.height * 4f).coerceIn(0f, 1f)
        },
    ) {
        Top(item, haze, Modifier.align(Alignment.TopCenter))
        if (item.kept) BasicText(
            tr("The full picture stays on your computer.", "Pełne zdjęcie zostaje na komputerze."),
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp)
                .glass(haze, Shapes.Card).padding(horizontal = 16.dp, vertical = 10.dp),
            style = Type.Meta.copy(color = Ink.Two),
        )
    }
}

@Composable
private fun Document(item: PreviewItem, progress: () -> Float, from: () -> Rect?) {
    val haze = remember { HazeState() }
    val tile = with(LocalDensity.current) { Radius.Tile.toPx() }
    val lines = remember(item) { item.text?.lines().orEmpty() }
    Box(Modifier.fillMaxSize().drawBehind { drawRect(Color.Black, alpha = 0.6f * progress()) }) {
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    val t = progress()
                    val start = from()
                    if (start == null) {
                        alpha = t
                    } else {
                        shape = Window(lerp(start, Rect(Offset.Zero, size), t), tile * (1f - t))
                        clip = true
                    }
                }
                .background(Tone.App),
        ) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = ((progress() - 0.35f) / 0.65f).coerceIn(0f, 1f) }) {
                LazyColumn(
                    Modifier.fillMaxSize().glassSource(haze),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 120.dp, bottom = 48.dp),
                ) {
                    items(lines.size, key = { it }) { BasicText(lines[it], style = Type.Mono) }
                }
                if (item.text == null) BasicText(
                    tr("The file stays on your computer.", "Plik zostaje na komputerze."),
                    Modifier.align(Alignment.Center).padding(32.dp),
                    style = Type.Body.copy(color = Ink.Three, textAlign = TextAlign.Center),
                )
                Top(item, haze, Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

/** The file's name and size in a frosted island, with the way out beside it. */
@Composable
private fun Top(item: PreviewItem, haze: HazeState, modifier: Modifier) {
    Box(modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Column(
            Modifier.align(Alignment.CenterStart).padding(end = 52.dp).heightIn(min = 44.dp)
                .glass(haze, Shapes.Card).padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            BasicText(item.name, style = Type.Meta.copy(color = Ink.One, fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(sizeOf(item.bytes), style = Type.Label)
        }
        Box(Modifier.align(Alignment.CenterEnd).size(44.dp).glass(haze, CircleShape)) {
            IconBtn(Glyph.Close, PreviewState::close, tint = Ink.One, fill = Color.Transparent, size = 44.dp, island = true)
        }
    }
}

private class Window(private val r: Rect, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Rounded(RoundRect(r, CornerRadius(radius)))
}

private fun around(c: Offset, w: Float, h: Float) = Rect(c.x - w / 2f, c.y - h / 2f, c.x + w / 2f, c.y + h / 2f)

private fun fit(box: Size, b: ImageBitmap): Rect {
    val s = min(box.width / b.width, box.height / b.height)
    return around(box.center, b.width * s, b.height * s)
}

private fun cover(r: Rect, b: ImageBitmap): Rect {
    val s = max(r.width / b.width, r.height / b.height)
    return around(r.center, b.width * s, b.height * s)
}

/** How far the zoomed picture may move from centre before an edge shows. */
private fun bounds(f: Rect, z: Float, box: Size) =
    Offset(max(0f, (f.width * z - box.width) / 2f), max(0f, (f.height * z - box.height) / 2f))

private fun clamp(o: Offset, b: Offset) = Offset(o.x.coerceIn(-b.x, b.x), o.y.coerceIn(-b.y, b.y))

private fun band(v: Float, b: Float, reach: Float): Float {
    val inside = v.coerceIn(-b, b)
    return inside + rubber(v - inside, reach)
}

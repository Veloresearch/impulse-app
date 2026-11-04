package com.velocity.impulse.ui.components

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.velocity.impulse.ui.theme.LocalReduceMotion
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Tone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A surface that grows out of the element that opened it and shrinks back into it, the way the
 * Dynamic Island moves. One [progress] on one spring drives everything: the container's place,
 * size and corner, the cross-fade from the element's look to the surface's content, and whatever
 * the host draws behind it. Opening while it closes reverses from where it is.
 */
/** [covers] is true for a surface that hides everything behind it once open, like a full page. */
@Stable
class Presentation<T : Any>(private val scope: CoroutineScope, private val covers: Boolean = false) {
    val progress = Animatable(0f, 0.002f)

    /** What is shown; kept while it closes, null once it is back in its source. */
    var item by mutableStateOf<T?>(null)
        private set
    var origin by mutableStateOf("")
        private set
    var open by mutableStateOf(false)
        private set
    internal var calm = false
    internal val sources = HashMap<String, Source>()

    fun open(item: T, origin: String) {
        val fresh = this.item == null
        val other = !fresh && (this.item != item || this.origin != origin)
        sources[origin]?.capture()
        this.item = item
        this.origin = origin
        open = true
        scope.launch {
            if (other) progress.snapTo(0f)
            // A page's first frame composes all of it; the spring starts after, so a slow first frame is never skipped over.
            if (covers && (fresh || other)) repeat(2) { withFrameNanos {} }
            progress.animateTo(1f, spec())
        }
    }

    fun close() {
        if (!open) return
        sources[origin]?.capture()
        open = false
        scope.launch {
            // What the surface covered is drawn again first, so that frame is never the flight's first.
            if (covers && progress.value >= 1f) withFrameNanos {}
            settle()
        }
    }

    /** Back in flight: follows the thumb from [from] towards the source. */
    internal suspend fun seek(from: Float, back: Float) = progress.snapTo(from * (1f - back))

    internal fun reopen() {
        open = true
        scope.launch { progress.animateTo(1f, spec()) }
    }

    private suspend fun settle() {
        progress.animateTo(0f, spec())
        if (!open) item = null
    }

    private fun spec() = if (calm) tween<Float>(Motion.M2) else Motion.island()

    internal fun hides(key: String) = !calm && item != null && origin == key
}

internal class Source(val layer: GraphicsLayer) {
    var coords: LayoutCoordinates? = null
    var shape: Shape = RectangleShape
    var last: Rect? = null

    /** Where the source is now, transforms included; null when it is off screen, so the surface shrinks in place instead. */
    fun bounds(): Rect? {
        val c = coords?.takeIf { it.isAttached } ?: return last
        if (c.boundsInWindow().isEmpty) return null
        return c.findRootCoordinates().localBoundingBoxOf(c, clipBounds = false)
    }

    fun capture() {
        bounds()?.let { last = it }
    }
}

/**
 * Marks the element [p]'s surface grows out of when opened with [key]. It hides only while it is
 * the surface, and its look is recorded so the surface can wear it at the start of the flight.
 */
@Composable
fun Modifier.grows(p: Presentation<*>, key: String, shape: Shape): Modifier {
    val layer = rememberGraphicsLayer()
    val source = remember(p, key) { Source(layer) }
    source.shape = shape
    DisposableEffect(p, key, source) {
        p.sources[key] = source
        onDispose { if (p.sources[key] === source) p.sources.remove(key) }
    }
    return onGloballyPositioned { source.coords = it }.drawWithContent {
        layer.record { this@drawWithContent.drawContent() }
        if (!p.hides(key)) {
            layer.alpha = 1f
            drawLayer(layer)
        }
    }
}

/** The corner a [shape] has at [size]: a circle, a capsule, a rounded or a continuous corner. */
private fun cornerOf(shape: Shape, size: Size, density: Density): Float = when (val o = shape.createOutline(size, LayoutDirection.Ltr, density)) {
    is Outline.Rounded -> o.roundRect.topLeftCornerRadius.x
    is Outline.Rectangle -> 0f
    is Outline.Generic -> size.minDimension * 0.22f
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/** The part of the surface on show, in its own coordinates, and the scale that fits it into the flying box. */
private class Geometry {
    var scale = 1f
    var x = 0f
    var y = 0f
    var w = 0f
    var h = 0f
    var radius = 0f
    var dx = 0f
    var dy = 0f
    var ax = 0f
    var ay = 0f
    var from: Rect? = null

    /**
     * The box runs from the source's bounds [src] to the surface's [at] and [size]. The surface is
     * scaled to cover the box and cropped to it, cropped on the side where the source sits, so the
     * source's own corner of the surface is where the source was.
     */
    fun update(src: Rect?, srcCorner: Float, at: Offset, size: Size, corner: Float, t: Float, zoom: Boolean) {
        from = src
        val s = src ?: Rect(at, size).let { Rect(it.center, 0f).inflate(min(it.width, it.height) * 0.3f) }
        // Height trails width, as the island widens before it drops, so near the source the box never thins into a sliver.
        val tx = t
        val ty = t * sqrt(t)
        val l = lerp(s.left, at.x, tx)
        val top = lerp(s.top, at.y, ty)
        val bw = max(1f, lerp(s.width, size.width, tx))
        val bh = max(1f, lerp(s.height, size.height, ty))
        scale = if (zoom) max(bw / size.width, bh / size.height) else 1f
        w = min(bw / scale, size.width)
        h = min(bh / scale, size.height)
        ax = ((s.left - at.x) / (size.width - s.width)).let { if (it.isFinite()) it.coerceIn(0f, 1f) else 0.5f }
        ay = ((s.top - at.y) / (size.height - s.height)).let { if (it.isFinite()) it.coerceIn(0f, 1f) else 0.5f }
        x = (size.width - w) * ax
        y = (size.height - h) * ay
        radius = lerp(srcCorner, corner, t).coerceIn(0f, min(bw, bh) / 2f) / scale
        dx = l - at.x - x * scale
        dy = top - at.y - y * scale
    }
}

private class Window(private val g: Geometry) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(RoundRect(g.x, g.y, g.x + g.w, g.y + g.h, CornerRadius(g.radius)))
}

/** The presentation whose surface this is composed in; its flight already brings the content in. */
internal val LocalPresentation = staticCompositionLocalOf<Presentation<*>?> { null }

/**
 * Composed only once the surface around it has landed, for what starts below the fold: a long
 * page's first frame then holds just what shows, so the flight starts at once.
 */
@Composable
fun AfterLanding(content: @Composable () -> Unit) {
    val p = LocalPresentation.current
    var landed by remember { mutableStateOf(p == null) }
    if (!landed && p != null) LaunchedEffect(p) {
        snapshotFlow { !p.open || p.progress.value >= 1f && !p.progress.isRunning }.first { it }
        landed = true
    }
    if (landed) content()
}

/**
 * The source's look gives way to the content between 30% and 60% of the way. Content that is not
 * zoomed with the shape waits until the shape is most of the way there, from 45% to 80%, and so on
 * the way back it is gone before the shape has shrunk far.
 */
private fun crossFade(t: Float, zoom: Boolean): Float {
    val k = (if (zoom) (t - 0.3f) / 0.3f else (t - 0.45f) / 0.35f).coerceIn(0f, 1f)
    return k * k * (3f - 2f * k)
}

/**
 * Hosts [p]'s surface while it is shown (only when it shows [only], if given). [modifier] places
 * the surface where it rests; the content is laid out there once at full size and only scaled and
 * cropped in flight. [ground] fills the container from the first frame, so the shape is continuous
 * before the content appears; [frosted] content brings its own glass and the ground gives way to it.
 * A [zoom]ed surface (a page) is scaled with the box; any other keeps its size and is cropped by it.
 * With [vanish] the source is only a place, so the shape fades out as it gets there. [lift] is its resting elevation; the shadow belongs to the container, so the clip never cuts it.
 * A [scrim] darkens what is behind with the same progress; a tap outside or back closes it, and
 * predictive back moves it with the thumb.
 */
@Composable
fun <T : Any> Presented(
    p: Presentation<T>,
    modifier: Modifier,
    corner: Dp,
    ground: Color,
    only: T? = null,
    scrim: Float = 0f,
    frosted: Boolean = false,
    lift: Dp = 0.dp,
    vanish: Boolean = false,
    zoom: Boolean = false,
    content: @Composable (T) -> Unit,
) {
    val calm = LocalReduceMotion.current
    p.calm = calm
    val item = p.item ?: return
    if (only != null && item != only) return
    val density = LocalDensity.current
    val cornerPx = with(density) { corner.toPx() }
    val g = remember { Geometry() }
    val window = remember { Window(g) }
    val at = remember { arrayOf(Offset.Zero) }
    fun Density.measure(size: Size) {
        val src = if (calm) null else p.sources[p.origin]
        val bounds = src?.bounds()
        val srcCorner = bounds?.let { b ->
            val c = src.coords
            val local = c?.size?.let { Size(it.width.toFloat(), it.height.toFloat()) } ?: b.size
            cornerOf(src.shape, local, this) * (if (local.width > 0f) b.width / local.width else 1f)
        } ?: 0f
        g.update(bounds, srcCorner, at[0], size, cornerPx, p.progress.value.coerceAtLeast(0f), zoom)
    }

    PredictiveBackHandler(p.open) { events ->
        val from = p.progress.value
        try {
            events.collect { e -> p.seek(from, e.progress) }
            p.close()
        } catch (e: CancellationException) {
            p.reopen()
            throw e
        }
    }

    Box(
        Modifier.fillMaxSize()
            .then(if (scrim > 0f) Modifier.drawBehind { drawRect(Color.Black, alpha = scrim * p.progress.value.coerceIn(0f, 1f)) } else Modifier)
            .then(if (p.open) Modifier.pointerInput(p) { detectTapGestures { p.close() } } else Modifier),
    ) {
        Box(
            modifier
                .onGloballyPositioned { c -> at[0] = c.positionInRoot() }
                .graphicsLayer {
                    val t = p.progress.value
                    if (calm) {
                        alpha = t.coerceIn(0f, 1f)
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                        return@graphicsLayer
                    }
                    measure(size)
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = g.scale
                    scaleY = g.scale
                    translationX = g.dx
                    translationY = g.dy
                    shape = window
                    clip = true
                    val k = t.coerceIn(0f, 1f)
                    // The shadow is the container's own, on its outline: a page lifts off mid-flight, anything else rises to its resting lift.
                    shadowElevation = if (zoom) 12.dp.toPx() * 4f * k * (1f - k) else lift.toPx() * k * k
                }
                .drawWithContent {
                    if (calm) {
                        drawContent()
                        return@drawWithContent
                    }
                    val t = p.progress.value
                    val f = crossFade(t, zoom)
                    measure(size)
                    val fill = (if (vanish) (t - 0.3f) / 0.15f else t / 0.06f).coerceIn(0f, 1f) * if (frosted) 1f - f else 1f
                    // A small box wears a raised tone so its edge reads against what it flies over; it deepens into the surface's ground.
                    if (fill > 0f) drawRect(lerp(if (vanish) ground else Tone.Raise, ground, f), Offset(g.x, g.y), Size(g.w, g.h), alpha = fill)
                    drawContent()
                    val src = p.sources[p.origin]
                    val b = g.from
                    if (src != null && b != null && f < 1f && src.layer.size.width > 0) {
                        val k = b.width / src.layer.size.width / g.scale
                        val lw = b.width / g.scale
                        val lh = b.height / g.scale
                        translate(g.x + (g.w - lw) * g.ax, g.y + (g.h - lh) * g.ay) {
                            scale(k, k, Offset.Zero) {
                                src.layer.alpha = 1f - f
                                drawLayer(src.layer)
                            }
                        }
                    }
                }
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            Box(
                Modifier.graphicsLayer {
                    if (calm) return@graphicsLayer
                    val t = p.progress.value
                    alpha = crossFade(t, zoom)
                    // In flight the content is drawn once into a layer and only that layer moves.
                    compositingStrategy = if (t < 1f || !p.open) CompositingStrategy.Offscreen else CompositingStrategy.Auto
                },
            ) { CompositionLocalProvider(LocalPresentation provides p) { content(item) } }
        }
    }
}

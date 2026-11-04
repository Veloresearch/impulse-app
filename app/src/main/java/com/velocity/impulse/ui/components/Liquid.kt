package com.velocity.impulse.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.velocity.impulse.ui.theme.LocalReduceMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A drop of liquid behind a control. It swells under the finger, stretches toward it while it
 * moves and wobbles home when let go; whatever sits on top trails behind at a fraction of the
 * pull. [give] scales all of it: 1 for round buttons, less for controls that should stay calmer.
 */
@Stable
class Liquid internal constructor(private val scope: CoroutineScope, internal val give: Float) {
    internal val swell = Animatable(1f)
    internal val pull = Animatable(Offset.Zero, Offset.VectorConverter)
    internal val lag = Animatable(Offset.Zero, Offset.VectorConverter)

    fun press() {
        scope.launch { swell.animateTo(1f + 0.12f * give, spring(0.5f, 650f)) }
    }

    fun drag(to: Offset) {
        scope.launch { pull.snapTo(to) }
        scope.launch { lag.animateTo(to * 0.4f, spring(0.75f, 320f)) }
    }

    /** A fast [velocity] carries the drop past its rest point, so a flick bounces. */
    fun release(velocity: Offset = Offset.Zero) {
        val loose = (1f - give) * 0.25f
        scope.launch { swell.animateTo(1f, spring(0.3f + loose, 420f)) }
        scope.launch { pull.animateTo(Offset.Zero, spring(0.28f + loose, 380f), initialVelocity = velocity) }
        scope.launch { lag.animateTo(Offset.Zero, spring(0.45f, 240f)) }
    }
}

@Composable
fun rememberLiquid(give: Float = 1f): Liquid {
    val scope = rememberCoroutineScope()
    return remember(give) { Liquid(scope, give) }
}

/** Feeds a [Liquid] from touches on this element without taking them from anything else. */
fun Modifier.liquidTouch(l: Liquid): Modifier = pointerInput(l) {
    val reach = 24.dp.toPx() * (0.5f + 0.5f * l.give)
    awaitEachGesture {
        val first = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
        val track = VelocityTracker().apply { addPosition(first.uptimeMillis, first.position) }
        l.press()
        var fling = true
        while (true) {
            val c = awaitPointerEvent(PointerEventPass.Final).changes.firstOrNull { it.id == first.id } ?: break
            if (!c.pressed) break
            if (c.isConsumed) {
                fling = false
                break
            }
            track.addPosition(c.uptimeMillis, c.position)
            val d = c.position - first.position
            l.drag(Offset(rubber(d.x, reach), rubber(d.y, reach)))
        }
        val v = if (fling) track.calculateVelocity().let { Offset(it.x, it.y) * 0.2f } else Offset.Zero
        val speed = v.getDistance()
        val most = reach * 18f
        l.release(if (speed > most) v * (most / speed) else v)
    }
}

/**
 * The whole element is the drop: it swells under the finger and stretches toward it at constant
 * area, glass and all. For big bodies like the composer, which take a smaller [Liquid.give].
 */
fun Modifier.liquidBody(l: Liquid): Modifier = liquidTouch(l).graphicsLayer {
    val p = l.pull.value
    val sx = 1f + abs(p.x) / size.width.coerceAtLeast(1f) * 0.9f
    val sy = 1f + abs(p.y) / size.height.coerceAtLeast(1f) * 0.9f
    val swell = 1f + (l.swell.value - 1f) * minOf(1f, 56.dp.toPx() / size.minDimension.coerceAtLeast(1f))
    scaleX = swell * sx / sqrt(sy)
    scaleY = swell * sy / sqrt(sx)
    translationX = p.x * 0.45f
    translationY = p.y * 0.45f
}

/**
 * Draws [shape] as the drop: grown evenly by the swell, pushed halfway toward the finger and
 * drawn out along the pull at constant area.
 */
fun Modifier.liquidShape(l: Liquid, shape: Shape, color: () -> Color): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val short = size.minDimension.coerceAtLeast(1f)
    onDrawBehind {
        val c = color()
        if (c.alpha == 0f) return@onDrawBehind
        val s = l.swell.value - 1f
        val p = l.pull.value
        val a = atan2(p.y, p.x)
        val extent = (abs(cos(a)) * size.width + abs(sin(a)) * size.height).coerceAtLeast(1f)
        val along = 1f + p.getDistance() * 0.55f / extent
        val deg = a * 180f / PI.toFloat()
        val mid = center
        withTransform({
            translate(p.x * 0.5f, p.y * 0.5f)
            rotate(deg, mid)
            scale(along, 1f / along, mid)
            rotate(-deg, mid)
            scale(1f + s * short / size.width.coerceAtLeast(1f), 1f + s * short / size.height.coerceAtLeast(1f), mid)
        }) { drawOutline(outline, c) }
    }
}

/**
 * A row of equal slots with a highlight that behaves like the same liquid. Tapping a slot sends
 * it there with its leading edge running ahead and the trailing edge catching up, thinning as it
 * stretches; a touch swells it and the release wobbles it; dragging carries it with the finger,
 * ticking at every slot it crosses, and drops it into the nearest one on release.
 */
@Composable
fun LiquidSlots(
    count: Int,
    selected: Int,
    onSelect: (Int) -> Unit,
    color: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val view = LocalView.current
    val calm = LocalReduceMotion.current
    val pick = rememberUpdatedState(onSelect)
    val scope = rememberCoroutineScope()
    val blob = rememberLiquid(0.8f)
    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat() / count
        val lead = remember { Animatable(w * selected) }
        val trail = remember { Animatable(w * (selected + 1)) }
        var held by remember { mutableStateOf(false) }

        fun glide(from: Float, to: Float, fast: Float, slow: Float) {
            val right = from > lead.value
            scope.launch { lead.animateTo(from, spring(0.68f, if (right) slow else fast)) }
            scope.launch { trail.animateTo(to, spring(0.68f, if (right) fast else slow)) }
        }

        LaunchedEffect(selected, w, held) {
            if (!held) glide(w * selected, w * (selected + 1), 900f, 260f)
        }

        Row(
            Modifier.fillMaxSize()
                .drawBehind {
                    val l = lead.value
                    val width = (trail.value - l).coerceAtLeast(1f)
                    val s = blob.swell.value - 1f
                    val thin = sqrt((w / width).coerceIn(0.6f, 1f))
                    val outline = shape.createOutline(Size(width, size.height), layoutDirection, this)
                    withTransform({
                        translate(l, 0f)
                        scale(1f + s * 0.35f, (1f + s) * thin, Offset(width / 2f, size.height / 2f))
                    }) { drawOutline(outline, color.copy(alpha = (color.alpha * (1f + s * 6f)).coerceAtMost(1f))) }
                }
                .pointerInput(calm) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        if (!calm) blob.press()
                        do {
                            val e = awaitPointerEvent(PointerEventPass.Final)
                        } while (e.changes.any { it.pressed })
                        blob.release()
                    }
                }
                .pointerInput(count, w) {
                    var at = selected
                    var finger = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { o ->
                            held = true
                            finger = o.x
                            at = (o.x / w).toInt().coerceIn(0, count - 1)
                        },
                        onHorizontalDrag = { change, dx ->
                            change.consume()
                            finger = (finger + dx).coerceIn(0f, w * count)
                            glide((finger - w / 2).coerceIn(0f, w * (count - 1)), (finger + w / 2).coerceIn(w, w * count), 1400f, 520f)
                            val now = (finger / w).toInt().coerceIn(0, count - 1)
                            if (now != at) {
                                at = now
                                view.feel(Feel.Tick)
                                pick.value(now)
                            }
                        },
                        onDragEnd = { held = false },
                        onDragCancel = { held = false },
                    )
                },
            content = content,
        )
    }
}

package com.velocity.impulse.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch

/**
 * A row of equal slots with a highlight that behaves like a drop of liquid. Tapping a slot sends
 * it there with its leading edge running ahead and the trailing edge catching up; dragging picks
 * it up, it swells a little and follows the finger, ticking at every slot it crosses, and drops
 * into the nearest one on release.
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
    val hand = LocalHapticFeedback.current
    val density = LocalDensity.current
    val pick = rememberUpdatedState(onSelect)
    val scope = rememberCoroutineScope()
    BoxWithConstraints(modifier) {
        val w = maxWidth / count
        val wPx = with(density) { w.toPx() }
        val lead = remember { Animatable(w * selected, Dp.VectorConverter) }
        val trail = remember { Animatable(w * (selected + 1), Dp.VectorConverter) }
        var held by remember { mutableStateOf(false) }
        var finger by remember { mutableFloatStateOf(0f) }
        val swell by animateFloatAsState(if (held) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "swell")

        fun glide(from: Dp, to: Dp, fast: Float, slow: Float) {
            val right = from > lead.value
            scope.launch { lead.animateTo(from, spring(dampingRatio = 0.68f, stiffness = if (right) slow else fast)) }
            scope.launch { trail.animateTo(to, spring(dampingRatio = 0.68f, stiffness = if (right) fast else slow)) }
        }

        LaunchedEffect(selected, w, held) {
            if (!held) glide(w * selected, w * (selected + 1), 900f, 260f)
        }

        Box(
            Modifier.offset(x = lead.value).width(trail.value - lead.value).fillMaxHeight()
                .graphicsLayer {
                    scaleY = 1f + swell * 0.1f
                    scaleX = 1f + swell * 0.04f
                }
                .background(color.copy(alpha = color.alpha * (1f + swell * 0.6f)), shape),
        )
        Row(
            Modifier.fillMaxSize().pointerInput(count, wPx) {
                var at = selected
                detectHorizontalDragGestures(
                    onDragStart = { o ->
                        held = true
                        finger = o.x
                        at = (o.x / wPx).toInt().coerceIn(0, count - 1)
                    },
                    onHorizontalDrag = { change, dx ->
                        change.consume()
                        finger = (finger + dx).coerceIn(0f, wPx * count)
                        val c = with(density) { finger.toDp() }
                        glide((c - w / 2).coerceIn(Dp(0f), w * (count - 1)), (c + w / 2).coerceIn(w, w * count), 1400f, 520f)
                        val now = (finger / wPx).toInt().coerceIn(0, count - 1)
                        if (now != at) {
                            at = now
                            hand.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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


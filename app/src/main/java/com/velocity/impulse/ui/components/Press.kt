package com.velocity.impulse.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.velocity.impulse.ui.theme.LocalReduceMotion
import com.velocity.impulse.ui.theme.Motion
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

/** Pulls past [reach] give less and less, like stretching rubber. */
private fun rubber(d: Float, reach: Float) = d * reach / (abs(d) + reach)

/**
 * Press feedback shared by every tappable thing. It compresses to [depth] and springs back, the
 * fill deepens from [rest] to [pressed], a light tick goes to the hand, and while the finger is
 * down the element stretches toward it and wobbles home when let go. A scroll that takes the
 * gesture releases the stretch at once.
 */
fun Modifier.pressable(
    onClick: () -> Unit,
    shape: Shape? = null,
    rest: Color = Color.Transparent,
    pressed: Color = rest,
    depth: Float = 0.97f,
    enabled: Boolean = true,
    haptic: Boolean = true,
    role: Role = Role.Button,
    stretch: Boolean = false,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val down by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (down && enabled) depth else 1f, Motion.press(), label = "press")
    val fill by animateColorAsState(if (down && enabled) pressed else rest, label = "fill")
    val hand = LocalHapticFeedback.current
    val calm = LocalReduceMotion.current
    val pull = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()

    var m = this
    if (stretch && enabled && !calm) m = m.pointerInput(Unit) {
        val reach = 28.dp.toPx()
        awaitEachGesture {
            val first = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            while (true) {
                val e = awaitPointerEvent(PointerEventPass.Final)
                val c = e.changes.firstOrNull { it.id == first.id } ?: break
                if (!c.pressed || c.isConsumed) break
                val d = c.position - first.position
                scope.launch { pull.snapTo(Offset(rubber(d.x, reach), rubber(d.y, reach))) }
            }
            scope.launch { pull.animateTo(Offset.Zero, spring(dampingRatio = 0.38f, stiffness = 380f)) }
        }
    }
    m = m.graphicsLayer {
        val p = pull.value
        val w = size.width.coerceAtLeast(1f)
        val h = size.height.coerceAtLeast(1f)
        val sx = 1f + abs(p.x) / w * 0.9f
        val sy = 1f + abs(p.y) / h * 0.9f
        scaleX = scale * sx / sqrt(sy)
        scaleY = scale * sy / sqrt(sx)
        translationX = p.x * 0.45f
        translationY = p.y * 0.45f
        alpha = if (enabled) 1f else 0.45f
    }
    if (shape != null && (rest != Color.Transparent || pressed != Color.Transparent)) m = m.background(fill, shape)
    m.clickable(source, indication = null, enabled = enabled, role = role) {
        if (haptic) hand.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
    }
}

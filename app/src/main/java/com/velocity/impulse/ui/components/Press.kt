package com.velocity.impulse.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.velocity.impulse.ui.theme.LocalReduceMotion
import com.velocity.impulse.ui.theme.Motion
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

/** Pulls past [reach] give less and less, like stretching rubber. */
internal fun rubber(d: Float, reach: Float) = d * reach / (abs(d) + reach)

/**
 * Press feedback shared by every tappable thing. It compresses to [depth] and springs back, the
 * fill deepens from [rest] to [pressed] and [feel] goes to the hand. With [tilt] (degrees) a card
 * leans in 3D toward the finger and follows it while it is down. With [stretch], a button or a
 * pill also stretches toward the finger and wobbles home when let go. A scroll that takes the
 * gesture releases both at once.
 *
 * [feelOnDown] answers the touch itself rather than the release, for island controls. A control
 * with [blocked] looks unavailable but still takes the tap: it shakes, refuses, and calls
 * [blocked] so the caller can point at what is missing.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pressable(
    onClick: () -> Unit,
    shape: Shape? = null,
    rest: Color = Color.Transparent,
    pressed: Color = rest,
    depth: Float = 0.97f,
    enabled: Boolean = true,
    feel: Feel? = Feel.Tap,
    role: Role = Role.Button,
    stretch: Boolean = false,
    tilt: Float = 0f,
    feelOnDown: Boolean = false,
    blocked: (() -> Unit)? = null,
    onLong: (() -> Unit)? = null,
): Modifier = composed {
    val live = enabled || blocked != null
    val source = remember { MutableInteractionSource() }
    val down by source.collectIsPressedAsState()
    val sink = if (depth >= 0.98f) spring<Float>(1f, 700f) else spring<Float>(0.6f, 500f)
    val scale by animateFloatAsState(if (down && live && depth != 1f) depth else 1f, sink, label = "press")
    val fill by animateColorAsState(
        if (down && live) pressed else rest,
        tween(if (down) Motion.M1 else Motion.M2, easing = Motion.Standard),
        label = "fill",
    )
    val view = LocalView.current
    val calm = LocalReduceMotion.current
    val pull = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val lean = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()
    var refused by remember { mutableIntStateOf(0) }

    var m = this
    if (feelOnDown && enabled && blocked == null) m = m.pointerInput(feel) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            view.feel(feel ?: Feel.Tap)
        }
    }
    if ((stretch || tilt > 0f) && live && !calm) m = m.pointerInput(stretch, tilt) {
        val reach = 28.dp.toPx()
        fun aim(at: Offset) = Offset(
            ((at.x / size.width.coerceAtLeast(1)) * 2f - 1f).coerceIn(-1f, 1f),
            ((at.y / size.height.coerceAtLeast(1)) * 2f - 1f).coerceIn(-1f, 1f),
        )
        awaitEachGesture {
            val first = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            if (tilt > 0f) scope.launch { lean.animateTo(aim(first.position), spring(dampingRatio = 0.7f, stiffness = 600f)) }
            while (true) {
                val e = awaitPointerEvent(PointerEventPass.Final)
                val c = e.changes.firstOrNull { it.id == first.id } ?: break
                if (!c.pressed || c.isConsumed) break
                if (stretch) {
                    val d = c.position - first.position
                    scope.launch { pull.snapTo(Offset(rubber(d.x, reach), rubber(d.y, reach))) }
                }
                if (tilt > 0f) scope.launch { lean.snapTo(aim(c.position)) }
            }
            scope.launch { pull.animateTo(Offset.Zero, spring(dampingRatio = 0.38f, stiffness = 380f)) }
            scope.launch { lean.animateTo(Offset.Zero, spring(dampingRatio = 0.42f, stiffness = 260f)) }
        }
    }
    if (blocked != null) m = m.shake(refused)
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
        rotationY = lean.value.x * tilt
        rotationX = -lean.value.y * tilt
        cameraDistance = 14f * density
        alpha = when {
            blocked != null -> 0.55f
            enabled -> 1f
            else -> 0.45f
        }
    }
    if (shape != null && (rest != Color.Transparent || pressed != Color.Transparent)) m = m.background(fill, shape)
    m.combinedClickable(
        interactionSource = source,
        indication = null,
        enabled = live,
        role = role,
        onLongClick = onLong?.takeIf { blocked == null }?.let { long -> { view.feel(Feel.Hold); long() } },
    ) {
        if (blocked != null) {
            view.feel(Feel.Reject)
            refused++
            blocked()
        } else {
            if (feel != null && !feelOnDown) view.feel(feel)
            onClick()
        }
    }
}

/** A short no-shake, played every time [trigger] changes after the first composition. */
fun Modifier.shake(trigger: Int): Modifier = composed {
    val calm = LocalReduceMotion.current
    val x = remember { Animatable(0f) }
    val seen = remember { intArrayOf(trigger) }
    val d = LocalDensity.current.density
    LaunchedEffect(trigger) {
        if (trigger == seen[0]) return@LaunchedEffect
        seen[0] = trigger
        if (calm) return@LaunchedEffect
        x.snapTo(0f)
        x.animateTo(
            0f,
            keyframes {
                durationMillis = 300
                0f at 0
                -6f * d at 50
                5f * d at 110
                -3f * d at 170
                2f * d at 230
            },
        )
    }
    graphicsLayer { translationX = x.value }
}

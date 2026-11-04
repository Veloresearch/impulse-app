package com.velocity.impulse.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import com.velocity.impulse.ui.theme.Motion

/**
 * Press feedback shared by every tappable thing: it compresses to [depth] and springs back,
 * the fill deepens from [rest] to [pressed], and a light tick goes to the hand.
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
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val down by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (down && enabled) depth else 1f, Motion.press(), label = "press")
    val fill by animateColorAsState(if (down && enabled) pressed else rest, label = "fill")
    val hand = LocalHapticFeedback.current
    var m = this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        alpha = if (enabled) 1f else 0.45f
    }
    if (shape != null && (rest != Color.Transparent || pressed != Color.Transparent)) m = m.background(fill, shape)
    m.clickable(source, indication = null, enabled = enabled, role = role) {
        if (haptic) hand.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
    }
}

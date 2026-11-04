package com.velocity.impulse.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.velocity.impulse.ui.theme.Theme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

/** Marks the content that frosted glass above it blurs. Blur needs Android 12; older phones skip it. */
fun Modifier.glassSource(state: HazeState): Modifier = if (Build.VERSION.SDK_INT >= 31) haze(state) else this

/**
 * Light, matte frosted glass for floating islands: the content under it blurred with a faint
 * tint of the theme (graphite, white on paper, near black on AMOLED), no sheen. Below Android 12
 * it is a near-solid tone.
 * [state] must come from a [glassSource] that is a sibling, never an ancestor.
 */
fun Modifier.glass(state: HazeState, shape: Shape, shadow: Boolean = true): Modifier {
    val p = Theme.palette
    var m = this
    if (shadow) {
        m = m.shadow(
            6.dp, shape, clip = false,
            ambientColor = Color.Black.copy(alpha = 0.18f * p.shade),
            spotColor = Color.Black.copy(alpha = 0.28f * p.shade),
        )
    }
    m = m.clip(shape)
    m = if (Build.VERSION.SDK_INT >= 31) {
        m.hazeChild(state, HazeStyle(p.app, HazeTint(p.frost), blurRadius = 24.dp, noiseFactor = 0.04f))
    } else {
        m.background(p.float.copy(alpha = 0.94f))
    }
    return m
}

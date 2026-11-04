package com.velocity.impulse.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.velocity.impulse.ui.theme.Tone
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

private val Frost = HazeStyle(Tone.App, HazeTint(Tone.Float.copy(alpha = 0.62f)), blurRadius = 24.dp, noiseFactor = 0.04f)

/** Marks the content that frosted glass above it blurs. Blur needs Android 12; older phones skip it. */
fun Modifier.glassSource(state: HazeState): Modifier = if (Build.VERSION.SDK_INT >= 31) haze(state) else this

/**
 * Light, matte frosted glass for floating islands: the content under it blurred with a faint
 * tint and a soft highlight along the top edge. Below Android 12 it is a near-solid tone.
 * [state] must come from a [glassSource] that is a sibling, never an ancestor.
 */
fun Modifier.glass(state: HazeState, shape: Shape, shadow: Boolean = true): Modifier {
    var m = this
    if (shadow) m = m.shadow(8.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.35f))
    m = m.clip(shape)
    m = if (Build.VERSION.SDK_INT >= 31) m.hazeChild(state, Frost) else m.background(Tone.Float.copy(alpha = 0.94f))
    return m.drawWithCache {
        val sheen = Brush.verticalGradient(0f to Color.White.copy(alpha = 0.06f), 1f to Color.Transparent, endY = 16.dp.toPx())
        onDrawWithContent {
            drawContent()
            drawRect(sheen)
        }
    }
}

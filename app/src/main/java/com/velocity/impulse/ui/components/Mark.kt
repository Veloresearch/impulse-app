package com.velocity.impulse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import kotlin.math.pow

internal const val LAYERS = 18
private const val GREYS = 14
internal const val STEP = 0.905f
internal const val TWIST = 3.4f

private val Outer = Color(0xFF3B3841)
private val Light = Color(0xFFECE9F0)
private val EmberDeep = Color(0xFFB4602A)
internal val EmberLight = Color(0xFFF7B888)

internal fun layerColor(i: Int): Color =
    if (i < GREYS) lerp(Outer, Light, (i.toFloat() / (GREYS - 1)).pow(1.15f))
    else lerp(EmberDeep, EmberLight, (i - GREYS).toFloat() / (LAYERS - GREYS - 1))

private fun backOut(t: Float): Float {
    val c = 1.55f
    val x = t - 1f
    return 1f + (c + 1f) * x * x * x + c * x * x
}

/**
 * The Impulse mark: squares nested inward, each a little smaller and turned a little further,
 * from graphite to ember. [build] grows them in from the outside one after another; [spin] turns
 * the whole spiral.
 */
@Composable
fun ImpulseMark(modifier: Modifier = Modifier, build: Float = 1f, spin: Float = 0f, tile: Boolean = false) {
    Canvas(modifier) {
        val side = size.minDimension
        val c = Offset(size.width / 2, size.height / 2)
        if (tile) {
            drawRoundRect(Color(0xFF1C1A1F), Offset(c.x - side / 2, c.y - side / 2), Size(side, side), CornerRadius(side * 0.22f))
        }
        val base = side * if (tile) 0.68f else 1f
        for (i in 0 until LAYERS) {
            val start = i.toFloat() / LAYERS * 0.62f
            val t = ((build - start) / 0.38f).coerceIn(0f, 1f)
            if (t <= 0f) break
            val grow = backOut(t)
            val s = base * STEP.pow(i) * grow
            rotate(i * TWIST * t + spin * (1f + i * 0.04f), c) {
                drawRect(layerColor(i), Offset(c.x - s / 2, c.y - s / 2), Size(s, s), alpha = t.coerceAtMost(1f))
            }
        }
    }
}

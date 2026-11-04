package com.velocity.impulse.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.LocalReduceMotion
import com.velocity.impulse.ui.theme.Theme
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

enum class BackdropMood { Idle, Thinking, Writing, Tool }

private class Pace(val speed: Float, val breath: Float, val rate: Float, val flow: Float, val glow: Float)

private fun paceOf(mood: BackdropMood) = when (mood) {
    BackdropMood.Idle -> Pace(speed = 1f, breath = 0.035f, rate = 0.5f, flow = 0f, glow = 1f)
    BackdropMood.Thinking -> Pace(speed = 3.2f, breath = 0.14f, rate = 2f, flow = 0f, glow = 1.35f)
    BackdropMood.Writing -> Pace(speed = 1.8f, breath = 0.05f, rate = 0.9f, flow = 1f, glow = 1.2f)
    BackdropMood.Tool -> Pace(speed = 2.4f, breath = 0.09f, rate = 1.4f, flow = 0.35f, glow = 1.25f)
}

/** A glow's resting place and how it wanders, in fractions of the screen. */
private class Glow(val x: Float, val y: Float, val ax: Float, val ay: Float, val fx: Float, val fy: Float, val at: Float, val size: Float, val pull: Float, val strength: Float)

private val Glows = listOf(
    Glow(0.18f, 0.22f, 0.16f, 0.10f, 0.7f, 0.9f, 0f, 0.95f, 0.18f, 0.85f),
    Glow(0.88f, 0.52f, 0.12f, 0.14f, 1.1f, 0.6f, 2.1f, 0.85f, 0.26f, 0.65f),
    Glow(0.42f, 0.98f, 0.20f, 0.06f, 0.8f, 1.3f, 4.2f, 1.05f, 0.06f, 1f),
)

/**
 * Moves the glows. Every value eases toward the mood's target and phases only ever advance, so
 * a mood change bends the motion instead of restarting it.
 */
private class Drift {
    var frame by mutableLongStateOf(0L)
    var phase = 0f
    var breathPhase = 0f
    var speed = 1f
    var breath = 0.035f
    var rate = 0.5f
    var flow = 0f
    var glow = 1f

    private fun near(p: Pace) = abs(speed - p.speed) + abs(breath - p.breath) + abs(flow - p.flow) + abs(glow - p.glow) < 0.004f

    suspend fun run(mood: () -> BackdropMood) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1e9f).coerceAtMost(0.1f)
            last = now
            val m = mood()
            val p = paceOf(m)
            val k = 1f - exp(-dt / 0.7f)
            speed += (p.speed - speed) * k
            breath += (p.breath - breath) * k
            rate += (p.rate - rate) * k
            flow += (p.flow - flow) * k
            glow += (p.glow - glow) * k
            phase += dt * speed * 0.09f
            breathPhase += dt * rate
            frame = now
            // Resting, the glows move well under a pixel a frame; twenty frames a second is plenty.
            if (m == BackdropMood.Idle && near(p)) delay(40)
        }
    }

    fun DrawScope.paint(brush: Brush, radius: Float, aura: Float) {
        val w = size.width
        val h = size.height
        for (g in Glows) {
            val x = (g.x + g.ax * sin(phase * g.fx + g.at)) * w
            val y = (g.y + g.ay * cos(phase * g.fy + g.at) + flow * g.pull) * h
            val s = g.size * (1f + breath * sin(breathPhase + g.at))
            val a = (aura * glow * g.strength).coerceAtMost(1f)
            translate(x, y) { scale(s, Offset.Zero) { drawCircle(brush, radius, Offset.Zero, alpha = a) } }
        }
    }
}

/**
 * The living ground behind the conversation: a few very soft glows in the accent drifting over
 * whatever is beneath. Thinking makes them breathe and drift faster, writing draws them toward the
 * composer, idle lets them settle. Only drawing is invalidated per frame, never composition.
 */
@Composable
fun Backdrop(state: () -> BackdropMood, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    val calm = LocalReduceMotion.current
    val drift = remember { Drift() }
    if (!calm) LaunchedEffect(drift) { drift.run(state) }
    Spacer(
        modifier.fillMaxSize().drawWithCache {
            val radius = maxOf(size.width, size.height) * 0.62f
            val brush = Brush.radialGradient(
                0f to accent,
                0.35f to accent.copy(alpha = 0.55f),
                0.7f to accent.copy(alpha = 0.16f),
                1f to accent.copy(alpha = 0f),
                center = Offset.Zero,
                radius = radius,
            )
            val aura = Theme.palette.aura
            onDrawBehind {
                drift.frame
                with(drift) { paint(brush, radius, aura) }
            }
        },
    )
}

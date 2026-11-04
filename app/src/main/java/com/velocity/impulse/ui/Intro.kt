package com.velocity.impulse.ui

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.velocity.impulse.ui.components.LAYERS
import com.velocity.impulse.ui.components.STEP
import com.velocity.impulse.ui.components.TWIST
import com.velocity.impulse.ui.components.layerColor
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import kotlin.math.pow

const val INTRO_HANDOFF = 2450f
private const val LENGTH = 2650f
private const val LEAVE = 2100f

private val InOut = CubicBezierEasing(0.65f, 0f, 0.25f, 1f)
private val Glide = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

private const val MARK = 150f
private const val SMALL = 60f
private const val GAP = 16f

/** How far through [from, to] (ms on the timeline) [t] is, eased. */
private fun seg(t: Float, from: Float, to: Float, easing: Easing = Motion.Out): Float =
    easing.transform(((t - from) / (to - from)).coerceIn(0f, 1f))

private fun GraphicsLayerScope.blur(px: Float) {
    renderEffect = if (Build.VERSION.SDK_INT >= 31 && px > 0.5f) {
        RenderEffect.createBlurEffect(px, px, Shader.TileMode.DECAL).asComposeRenderEffect()
    } else null
}

/**
 * The opening, on one timeline. A graphite square turns into place while its layers sink inward
 * into the spiral; the mark then slides aside and the name is uncovered from behind it, with
 * "by Velocity" settling beneath; the lockup softens and the app comes up through it. A tap
 * does nothing but is swallowed, so nothing underneath reacts.
 */
@Composable
fun Intro(onHandoff: () -> Unit, onDone: () -> Unit) {
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        clock.animateTo(INTRO_HANDOFF, tween(INTRO_HANDOFF.toInt(), easing = LinearEasing))
        onHandoff()
        clock.animateTo(LENGTH, tween((LENGTH - INTRO_HANDOFF).toInt(), easing = LinearEasing))
        onDone()
    }
    var wordWidth by remember { mutableIntStateOf(0) }

    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = 1f - seg(clock.value, INTRO_HANDOFF, LENGTH, LinearEasing) }
            .background(Tone.App)
            .clickable(remember { MutableInteractionSource() }, null) {},
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                val out = seg(clock.value, LEAVE, INTRO_HANDOFF, Motion.Leave)
                alpha = 1f - out
                scaleX = 1f - out * 0.05f
                scaleY = 1f - out * 0.05f
                blur(out * 20f)
            },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                Modifier.size(MARK.dp).graphicsLayer {
                    val lock = seg(clock.value, 1250f, 1950f, Glide)
                    val total = (SMALL + GAP).dp.toPx() + wordWidth
                    val scale = 1f - (1f - SMALL / MARK) * lock
                    scaleX = scale
                    scaleY = scale
                    translationX = (-total / 2 + SMALL.dp.toPx() / 2) * lock
                },
            ) { drawVortex(clock.value) }

            Column(
                Modifier
                    .onSizeChanged { wordWidth = it.width }
                    .graphicsLayer {
                        val lock = seg(clock.value, 1250f, 1950f, Glide)
                        val total = (SMALL + GAP).dp.toPx() + wordWidth
                        val home = total / 2 - wordWidth / 2f
                        translationX = home - (1f - lock) * 14.dp.toPx()
                        alpha = if (wordWidth == 0) 0f else 1f
                    }
                    .drawWithContent {
                        val show = seg(clock.value, 1420f, 2000f, Glide)
                        clipRect(right = size.width * show) { this@drawWithContent.drawContent() }
                    },
            ) {
                BasicText("Impulse", style = Type.Brand.copy(fontSize = 46.sp, lineHeight = 48.sp))
                BasicText(
                    "by Velocity",
                    Modifier.graphicsLayer {
                        val p = seg(clock.value, 1650f, 2050f, Glide)
                        alpha = p
                        translationY = (1f - p) * 6.dp.toPx()
                    },
                    style = Type.Body.copy(color = Ink.Three, fontSize = 14.sp, letterSpacing = 0.01.em),
                )
            }
        }
    }
}

private fun DrawScope.drawVortex(t: Float) {
    val c = Offset(size.width / 2, size.height / 2)
    val base = size.minDimension
    val turn = -60f * (1f - seg(t, 0f, 1300f, InOut))
    for (i in 0 until LAYERS) {
        val start = 60f + i * 34f
        val p = seg(t, start, start + 900f, InOut)
        val shown = seg(t, start, start + 220f, LinearEasing)
        if (shown <= 0f) continue
        val s = base * STEP.pow(i) * (0.2f + 0.8f * p)
        rotate(i * TWIST * p + turn - 30f * (1f - p), c) {
            drawRect(layerColor(i), Offset(c.x - s / 2, c.y - s / 2), Size(s, s), alpha = shown)
        }
    }
}

package com.velocity.impulse.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import com.velocity.impulse.ui.theme.LocalReduceMotion
import com.velocity.impulse.ui.theme.Motion

/**
 * Text that rolls to its new value instead of cutting: the old line leaves upward (or downward
 * with [up] false) as the new one rises in, and the width glides between them. With [digits],
 * only the characters that changed roll, in the direction the number moved.
 */
@Composable
fun Roll(text: String, style: TextStyle, modifier: Modifier = Modifier, up: Boolean = true, digits: Boolean = false, maxLines: Int = 1) {
    val calm = LocalReduceMotion.current
    if (!digits) {
        AnimatedContent(text, modifier, transitionSpec = { roll(up, calm) }, label = "roll") {
            BasicText(it, style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
        }
        return
    }
    val last = remember { object { var was = text; var rising = up } }
    if (last.was != text) {
        val a = last.was.filter(Char::isDigit).toLongOrNull()
        val b = text.filter(Char::isDigit).toLongOrNull()
        if (a != null && b != null && a != b) last.rising = b > a
        last.was = text
    }
    val rising = last.rising
    val tabular = style.copy(fontFeatureSettings = "tnum")
    Row(modifier) {
        text.forEachIndexed { i, ch ->
            key(text.length - 1 - i) {
                AnimatedContent(ch, transitionSpec = { roll(rising, calm) }, label = "digit") {
                    BasicText(it.toString(), style = tabular, maxLines = 1)
                }
            }
        }
    }
}

private fun <S> AnimatedContentTransitionScope<S>.roll(up: Boolean, calm: Boolean): ContentTransform {
    if (calm) return fadeIn(tween(120)) togetherWith fadeOut(tween(90)) using null
    return (
        slideInVertically(Motion.calmText()) { if (up) it * 6 / 10 else -it * 6 / 10 } +
            fadeIn(tween(Motion.M2, easing = Motion.Out))
        ) togetherWith (
        slideOutVertically(tween(Motion.M1, easing = Motion.Leave)) { if (up) -it / 2 else it / 2 } +
            fadeOut(tween(90))
        ) using SizeTransform(clip = true) { _, _ -> spring(1f, 500f, IntSize.VisibilityThreshold) }
}

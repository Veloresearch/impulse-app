package com.velocity.impulse.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.velocity.impulse.ui.LocalShared
import com.velocity.impulse.ui.LocalShell
import com.velocity.impulse.ui.Surface
import kotlin.math.max
import kotlin.math.min

/**
 * The flight between an element and the surface it opens, the way the Dynamic Island moves: one
 * spring for position and size with a touch of overshoot, so it lands instead of stopping.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
private val Glide = BoundsTransform { _, _ -> spring(0.8f, 330f, Rect.VisibilityThreshold) }

/** The two ends of one morph, each as its smaller side and its corner radius at rest. */
private class Ends {
    var small = 0f
    var smallR = 0f
    var large = 0f
    var largeR = 0f

    fun note(side: Float, r: Float) {
        if (small == 0f || side <= small) {
            small = side
            smallR = r
        }
        if (side >= large) {
            large = side
            largeR = r
        }
    }
}

private val ends = HashMap<String, Ends>()

/** The corner a [shape] has at [size], read from its outline: a circle, a capsule, a rounded or a continuous corner. */
private fun cornerOf(shape: Shape, size: Size, density: Density): Float = when (val o = shape.createOutline(size, LayoutDirection.Ltr, density)) {
    is Outline.Rounded -> o.roundRect.topLeftCornerRadius.x
    is Outline.Rectangle -> 0f
    is Outline.Generic -> {
        // A continuous corner: how far the top edge stays curved, measured on its bounds.
        val b = o.path.getBounds()
        min(b.width, b.height) * 0.22f
    }
}

/**
 * The clip while flying: the corner travels from the small end's corner to the large end's as the
 * box grows, so a capsule becomes a sheet without a single jump in the curve.
 */
private class FlightShape(private val e: Ends) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val side = size.minDimension
        val span = e.large - e.small
        val t = if (span > 1f) ((side - e.small) / span).coerceIn(0f, 1f) else 1f
        val r = min(side / 2f, max(0f, e.smallR + (e.largeR - e.smallR) * t))
        return Outline.Rounded(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r)))
    }
}

/**
 * Wraps the element a page or sheet grows out of. The element hides while it "is" that page or
 * sheet and returns as the surface shrinks back into it; its space is held meanwhile.
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MorphSource(origin: String, surface: Surface, modifier: Modifier = Modifier, content: @Composable AnimatedVisibilityScope.() -> Unit) {
    val shell = LocalShell.current
    val t = if (surface.page) shell.pageTransition else shell.sheetTransition
    var size by remember { mutableStateOf(IntSize.Zero) }
    val d = LocalDensity.current
    Box(modifier.defaultMinSize(with(d) { size.width.toDp() }, with(d) { size.height.toDp() })) {
        t.AnimatedVisibility(
            visible = { !(it == surface && shell.originOf(surface) == origin) || shell.calm },
            enter = EnterTransition.None,
            exit = ExitTransition.None,
        ) {
            Box(Modifier.onSizeChanged { size = it }) { content() }
        }
    }
}

/**
 * Container transform between a [MorphSource] element and the surface it opens. Put it first in the
 * chain. Both ends report their resting size and corner, and the flight blends between them.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.morph(origin: String, scope: AnimatedVisibilityScope, shape: Shape): Modifier {
    val shared = LocalShared.current
    if (LocalShell.current.calm || shared == null) return this
    val density = LocalDensity.current
    val e = remember(origin) { ends.getOrPut(origin) { Ends() } }
    val clip = remember(e) { FlightShape(e) }
    return with(shared) {
        this@morph
            .onSizeChanged {
                if (!scope.transition.isRunning && it.width > 0 && it.height > 0) {
                    val s = Size(it.width.toFloat(), it.height.toFloat())
                    e.note(s.minDimension, cornerOf(shape, s, density))
                }
            }
            .sharedBounds(
                rememberSharedContentState("m:$origin"),
                scope,
                // The leaving face goes quickly; the arriving one waits until the box is most of the way there.
                enter = fadeIn(tween(200, delayMillis = 90)),
                exit = fadeOut(tween(110)),
                boundsTransform = Glide,
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                clipInOverlayDuringTransition = OverlayClip(clip),
            )
    }
}

/** A title travelling with its container: scaled to fit, never re-wrapped. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.morphText(origin: String, scope: AnimatedVisibilityScope): Modifier {
    val shared = LocalShared.current
    if (LocalShell.current.calm || shared == null) return this
    return with(shared) {
        this@morphText.sharedBounds(
            rememberSharedContentState("t:$origin"),
            scope,
            enter = fadeIn(tween(200, delayMillis = 60)),
            exit = fadeOut(tween(110)),
            boundsTransform = Glide,
            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(ContentScale.FillHeight, Alignment.CenterStart),
        )
    }
}

/**
 * Directly inside a [morph] container: the body is measured once at full size and only clipped
 * while the container grows, so a page is never laid out again on every frame.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.morphBody(): Modifier {
    val shared = LocalShared.current
    if (LocalShell.current.calm || shared == null) return this
    return with(shared) { this@morphBody.skipToLookaheadSize() }
}

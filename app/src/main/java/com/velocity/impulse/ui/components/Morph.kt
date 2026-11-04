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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.velocity.impulse.ui.LocalShared
import com.velocity.impulse.ui.LocalShell
import com.velocity.impulse.ui.Surface
import com.velocity.impulse.ui.theme.Motion

@OptIn(ExperimentalSharedTransitionApi::class)
private val Glide =BoundsTransform { _, _ -> spring(0.9f, 380f, Rect.VisibilityThreshold) }

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

/** Container transform between a [MorphSource] element and the surface it opens. Put it first in the chain. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.morph(origin: String, scope: AnimatedVisibilityScope, shape: Shape): Modifier {
    val shared = LocalShared.current
    if (LocalShell.current.calm || shared == null) return this
    return with(shared) {
        this@morph.sharedBounds(
            rememberSharedContentState("m:$origin"),
            scope,
            enter = fadeIn(tween(Motion.M2, delayMillis = 50)),
            exit = fadeOut(tween(Motion.M1)),
            boundsTransform = Glide,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
            clipInOverlayDuringTransition = OverlayClip(shape),
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
            enter = fadeIn(tween(Motion.M2)),
            exit = fadeOut(tween(Motion.M1)),
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

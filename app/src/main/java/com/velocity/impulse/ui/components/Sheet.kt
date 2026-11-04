package com.velocity.impulse.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.velocity.impulse.ui.LocalShell
import com.velocity.impulse.ui.Surface
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A sheet that rises from the bottom edge. It follows the finger, flies away when flicked down,
 * and springs back when let go half-way.
 */
@Composable
fun Sheet(open: Boolean, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val rise = remember { Animatable(0f) }
    var height by remember { mutableStateOf(1f) }
    var shown by remember { mutableStateOf(open) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    LaunchedEffect(open) {
        if (open) {
            shown = true
            rise.animateTo(1f, Motion.travel())
        } else {
            rise.animateTo(0f, tween(Motion.M2, easing = Motion.Leave))
            shown = false
        }
    }
    if (!shown) return
    BackHandler(open) { onClose() }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { alpha = rise.value.coerceIn(0f, 1f) }
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(remember { MutableInteractionSource() }, null) { onClose() },
        )
        Column(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { height = it.height.toFloat().coerceAtLeast(1f) }
                .graphicsLayer { translationY = (1f - rise.value) * height }
                .draggable(
                    rememberDraggableState { d ->
                        scope.launch {
                            val old = rise.value
                            rise.snapTo((old - d / height).coerceIn(0f, 1.02f))
                            if ((old - 0.7f) * (rise.value - 0.7f) < 0f) view.feel(Feel.Tick)
                        }
                    },
                    Orientation.Vertical,
                    onDragStopped = { v ->
                        if (v > 1200f || rise.value < 0.7f) onClose() else rise.animateTo(1f, Motion.travel())
                    },
                )
                .padding(Space.S2)
                .background(Tone.Float, Shapes.Sheet)
                .navigationBarsPadding()
                .padding(start = Space.S4, end = Space.S4, bottom = Space.S4),
        ) {
            Handle()
            content()
        }
    }
}

@Composable
private fun Handle() {
    Box(Modifier.fillMaxWidth().padding(vertical = Space.S2), contentAlignment = Alignment.Center) {
        Box(Modifier.size(36.dp, 5.dp).background(Ink.Four.copy(alpha = 0.6f), CircleShape))
    }
}

/**
 * A sheet that grows out of the pill, chip or row that opened it (Z3) and shrinks back into it.
 * Dragged down it follows the finger; let go past a quarter of its height, or flicked, it closes
 * from where the finger left it. Hosted only by the shell.
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MorphSheet(surface: Surface, content: @Composable ColumnScope.() -> Unit) {
    val shell = LocalShell.current
    val calm = shell.calm
    val view = LocalView.current
    val tallest = LocalConfiguration.current.screenHeightDp.dp * 0.88f
    val stretch = with(LocalDensity.current) { 24.dp.toPx() }
    shell.sheetTransition.AnimatedVisibility(
        { it == surface },
        enter = if (calm) fadeIn(tween(Motion.M2)) else EnterTransition.None,
        exit = if (calm) fadeOut(tween(Motion.M2)) else ExitTransition.None,
    ) {
        val pull = remember { Animatable(0f) }
        var h by remember { mutableFloatStateOf(1f) }
        val scope = rememberCoroutineScope()
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize()
                    .animateEnterExit(fadeIn(tween(Motion.M2)), fadeOut(tween(Motion.M2)))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(remember { MutableInteractionSource() }, null) { shell.close(surface) },
            )
            Column(
                Modifier.align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(Space.S2)
                    .offset { IntOffset(0, pull.value.roundToInt()) }
                    .morph(shell.sheetOrigin, this@AnimatedVisibility, Shapes.Sheet)
                    .background(Tone.Float, Shapes.Sheet)
                    .fillMaxWidth()
                    .heightIn(max = tallest)
                    .onSizeChanged { h = it.height.toFloat().coerceAtLeast(1f) }
                    .draggable(
                        rememberDraggableState { d ->
                            scope.launch {
                                val old = pull.value
                                val new = old + d
                                pull.snapTo(if (new < 0f) rubber(new, stretch) else new)
                                val edge = 0.25f * h
                                if ((old - edge) * (pull.value - edge) < 0f) view.feel(Feel.Tick)
                            }
                        },
                        Orientation.Vertical,
                        onDragStopped = { v ->
                            if (pull.value > 0.25f * h || v > 1200f) shell.close(surface)
                            else pull.animateTo(0f, spring(0.7f, 500f))
                        },
                    )
                    .morphBody()
                    .padding(start = Space.S4, end = Space.S4, bottom = Space.S4),
            ) {
                Handle()
                content()
            }
        }
    }
}

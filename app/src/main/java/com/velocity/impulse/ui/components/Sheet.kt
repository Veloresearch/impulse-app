package com.velocity.impulse.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.velocity.impulse.ui.LocalShell
import com.velocity.impulse.ui.Surface
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Radius
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
@Composable
fun BoxScope.MorphSheet(surface: Surface, content: @Composable ColumnScope.() -> Unit) {
    val shell = LocalShell.current
    val view = LocalView.current
    val tallest = LocalConfiguration.current.screenHeightDp.dp * 0.88f
    val stretch = with(LocalDensity.current) { 24.dp.toPx() }
    val pull = remember { Animatable(0f) }
    var h by remember { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(shell.sheet.open) { if (shell.sheet.open) pull.snapTo(0f) }
    fun follow(d: Float, floor: Boolean = false) {
        scope.launch {
            val old = pull.value
            val new = if (floor) (old + d).coerceAtLeast(0f) else old + d
            pull.snapTo(if (new < 0f) rubber(new, stretch) else new)
            val edge = 0.25f * h
            if ((old - edge) * (pull.value - edge) < 0f) view.feel(Feel.Tick)
        }
    }
    suspend fun settle(v: Float) {
        if (pull.value > 0.25f * h || v > 1200f) shell.close(surface)
        else pull.animateTo(0f, spring(0.7f, 500f))
    }
    // A list inside the sheet scrolls first; once it is at its top, pulling down moves the sheet.
    val pullDown = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y >= 0f || pull.value <= 0f) return Offset.Zero
                val used = maxOf(available.y, -pull.value)
                follow(used, floor = true)
                return Offset(0f, used)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
                follow(available.y)
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pull.value <= 0f) return Velocity.Zero
                settle(available.y)
                return available
            }
        }
    }
    Presented(
        shell.sheet,
        Modifier.align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(Space.S2)
            .offset { IntOffset(0, pull.value.roundToInt()) }
            .fillMaxWidth()
            .heightIn(max = tallest),
        Radius.Sheet,
        Tone.Float,
        only = surface,
        scrim = 0.45f,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .background(Tone.Float, Shapes.Sheet)
                .onSizeChanged { h = it.height.toFloat().coerceAtLeast(1f) }
                .draggable(rememberDraggableState { follow(it) }, Orientation.Vertical, onDragStopped = { settle(it) })
                .nestedScroll(pullDown)
                .padding(start = Space.S4, end = Space.S4, bottom = Space.S4),
        ) {
            Handle()
            content()
        }
    }
}

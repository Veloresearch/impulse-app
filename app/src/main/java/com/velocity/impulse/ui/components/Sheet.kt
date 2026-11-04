package com.velocity.impulse.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import kotlinx.coroutines.launch

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
                    rememberDraggableState { d -> scope.launch { rise.snapTo((rise.value - d / height).coerceIn(0f, 1.02f)) } },
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
            Box(Modifier.fillMaxWidth().padding(vertical = Space.S2), contentAlignment = Alignment.Center) {
                Box(Modifier.size(36.dp, 5.dp).background(Ink.Four.copy(alpha = 0.6f), CircleShape))
            }
            content()
        }
    }
}

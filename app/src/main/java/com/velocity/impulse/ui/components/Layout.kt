package com.velocity.impulse.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type

/** Rows of a group sit on the ground with 2dp between them; only the group's ends are rounded. */
@Composable
fun Group(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        if (title != null) {
            BasicText(title, Modifier.padding(start = Space.S1, bottom = Space.S2), style = Type.Label)
        }
        Column(Modifier.fillMaxWidth().clip(Shapes.Tile), verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
    }
}

@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val base = modifier.fillMaxWidth().heightIn(min = 56.dp)
    Row(
        (if (onClick != null) base.pressable(onClick, null, Tone.Panel, Tone.Raise, depth = 1f, feel = null) else base.background(Tone.Panel))
            .padding(horizontal = Space.S4, vertical = Space.S3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(title, style = Type.Body.copy(fontWeight = FontWeight.Medium))
            if (detail != null) BasicText(detail, style = Type.Meta)
        }
        Spacer(Modifier.width(Space.S3))
        trailing()
    }
}

/**
 * A row that opens in place to show its choices. The chevron turns, the list unfolds under it,
 * and the check travels with the choice.
 */
@Composable
fun <T> Choice(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    detail: ((T) -> String?)? = null,
) {
    var open by remember { mutableStateOf(false) }
    val turn by animateFloatAsState(if (open) 180f else 0f, Motion.travel(), label = "chevron")
    Column(Modifier.fillMaxWidth().background(Tone.Panel)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .pressable({ open = !open }, null, Tone.Panel, Tone.Raise, depth = 1f, feel = Feel.Tick)
                .padding(horizontal = Space.S4, vertical = Space.S3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(title, Modifier.weight(1f), style = Type.Body.copy(fontWeight = FontWeight.Medium))
            BasicText(label(selected), style = Type.Body.copy(color = Ink.Three))
            Spacer(Modifier.width(Space.S2))
            Image(Icons.Default.KeyboardArrowDown, null, Modifier.size(20.dp).rotate(turn), colorFilter = ColorFilter.tint(Ink.Three))
        }
        AnimatedVisibility(
            open,
            enter = expandVertically(Motion.travel()) + fadeIn(tween(Motion.M2, delayMillis = 60)),
            exit = shrinkVertically(Motion.settle()) + fadeOut(tween(Motion.M1)),
        ) {
            Column(Modifier.padding(start = Space.S2, end = Space.S2, bottom = Space.S2)) {
                options.forEachIndexed { i, o ->
                    val on = o == selected
                    val shown by animateFloatAsState(if (on) 1f else 0f, Motion.press(), label = "check")
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .pressable({ onSelect(o); open = false }, Shapes.ControlLg, Tone.Panel, Tone.Raise, depth = 1f, feel = Feel.Tick)
                            .padding(horizontal = Space.S3),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            BasicText(label(o), style = Type.Body.copy(color = if (on) Ink.One else Ink.Two))
                            detail?.invoke(o)?.let { BasicText(it, style = Type.Meta) }
                        }
                        Image(
                            Icons.Default.Check, null,
                            Modifier.size(20.dp).graphicsLayer { alpha = shown; scaleX = 0.5f + shown / 2; scaleY = 0.5f + shown / 2 },
                            colorFilter = ColorFilter.tint(LocalAccent.current),
                        )
                    }
                    if (i < options.lastIndex) Spacer(Modifier.size(2.dp))
                }
            }
        }
    }
}

/** A short explainer that unfolds when tapped. */
@Composable
fun Disclosure(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    var open by remember { mutableStateOf(false) }
    val turn by animateFloatAsState(if (open) 90f else 0f, Motion.travel(), label = "chevron")
    Column(modifier.fillMaxWidth().background(Tone.Panel)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .pressable({ open = !open }, null, Tone.Panel, Tone.Raise, depth = 1f, feel = Feel.Tick)
                .padding(horizontal = Space.S4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(title, Modifier.weight(1f), style = Type.Body.copy(fontWeight = FontWeight.Medium))
            Image(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(20.dp).rotate(turn), colorFilter = ColorFilter.tint(Ink.Three))
        }
        AnimatedVisibility(
            open,
            enter = expandVertically(Motion.travel()) + fadeIn(tween(Motion.M2, delayMillis = 60)),
            exit = shrinkVertically(Motion.settle()) + fadeOut(tween(Motion.M1)),
        ) {
            Column(Modifier.padding(start = Space.S4, end = Space.S4, bottom = Space.S4), verticalArrangement = Arrangement.spacedBy(Space.S2), content = content)
        }
    }
}

@Composable
fun PageTitle(
    title: String,
    lead: String? = null,
    modifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(title, Modifier.weight(1f).wrapContentWidth(Alignment.Start).then(titleModifier), style = Type.Display)
            trailing()
        }
        if (lead != null) {
            Spacer(Modifier.size(Space.S1))
            BasicText(lead, style = Type.Body.copy(color = Ink.Three))
        }
    }
}

@Composable
fun Empty(title: String, body: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Column(
        modifier.fillMaxWidth().background(Tone.Panel, Shapes.Card).padding(Space.S5),
        verticalArrangement = Arrangement.spacedBy(Space.S2),
    ) {
        BasicText(title, style = Type.Section)
        BasicText(body, style = Type.Body.copy(color = Ink.Three))
        Box(Modifier.padding(top = Space.S2)) { action() }
    }
}

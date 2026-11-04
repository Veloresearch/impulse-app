package com.velocity.impulse.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.ui.components.Btn
import com.velocity.impulse.ui.components.IconBtn
import com.velocity.impulse.ui.components.ImpulseMark
import com.velocity.impulse.ui.components.Variant
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Signal
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type

@Composable
fun HomeScreen(m: AppModel, openChat: () -> Unit, openContexts: () -> Unit) {
    Page {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ImpulseMark(Modifier.size(36.dp), tile = true)
            Spacer(Modifier.width(Space.S3))
            Column(Modifier.weight(1f)) {
                BasicText(greeting(m.profileName), style = Type.Title)
                BasicText(m.server?.base?.substringAfter("://") ?: "", style = Type.Meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconBtn(Icons.Default.Refresh, { m.refresh() })
        }

        Status(m)

        Btn("New chat", { m.newChat(); openChat() }, Modifier.fillMaxWidth(), Variant.Primary, Icons.Default.Add)

        Row(horizontalArrangement = Arrangement.spacedBy(Space.S2)) {
            Tile("Model", m.models.firstOrNull()?.id ?: "None loaded", m.models.firstOrNull()?.window?.let { "${tokens(it.toLong())} per chat" }, Modifier.weight(1f)) {}
            Tile("Context", m.context ?: "None", m.contexts.firstOrNull { it.name == m.context }?.tokens?.let { tokens(it) }, Modifier.weight(1f), openContexts)
        }

        if (m.chats.isNotEmpty()) {
            BasicText("Recent", style = Type.Section, modifier = Modifier.padding(top = Space.S2))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.background(Tone.App, Shapes.Tile)) {
                m.chats.take(4).forEach { c ->
                    Column(
                        Modifier.fillMaxWidth().pressable({ m.open(c.id); openChat() }, Shapes.Tile, Tone.Panel, Tone.Raise, depth = 0.985f).padding(Space.S4),
                    ) {
                        BasicText(c.title.ifBlank { "Chat" }, style = Type.Body.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        BasicText(c.messages.lastOrNull()?.content.orEmpty().replace('\n', ' '), style = Type.Meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun Status(m: AppModel) {
    val live = m.online == true
    val dot by animateColorAsState(
        when (m.online) {
            true -> Signal.Ok
            false -> Signal.Err
            null -> Ink.Four
        },
        label = "dot",
    )
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(1f, 2.4f, infiniteRepeatable(tween(1600), RepeatMode.Restart), label = "ring")
    Row(
        Modifier.fillMaxWidth().background(Tone.Panel, Shapes.Card).padding(Space.S4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            if (live) Box(
                Modifier.size(10.dp).graphicsLayer { scaleX = pulse; scaleY = pulse; alpha = (2.4f - pulse) / 1.4f * 0.5f }
                    .background(dot, CircleShape),
            )
            Box(Modifier.size(10.dp).background(dot, CircleShape))
        }
        Spacer(Modifier.width(Space.S3))
        Column(Modifier.weight(1f)) {
            BasicText(
                when (m.online) {
                    true -> "Connected"
                    false -> "Cannot reach your computer"
                    null -> "Checking…"
                },
                style = Type.Body.copy(fontWeight = FontWeight.SemiBold),
            )
            BasicText(m.link?.label ?: "", style = Type.Meta)
        }
    }
}

@Composable
private fun Tile(label: String, value: String, detail: String?, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.pressable(onClick, Shapes.Card, Tone.Panel, Tone.Raise).padding(Space.S4),
        verticalArrangement = Arrangement.spacedBy(Space.S1),
    ) {
        BasicText(label, style = Type.Label)
        BasicText(value, style = Type.Body.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        BasicText(detail ?: " ", style = Type.Meta, maxLines = 1)
    }
}

private fun greeting(name: String?): String {
    val h = java.time.LocalTime.now().hour
    val part = when (h) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    return if (name.isNullOrBlank()) part else "$part, $name"
}

fun tokens(n: Long): String = when {
    n >= 1_000_000 -> "%.1fM tokens".format(n / 1_000_000.0).replace(".0M", "M")
    n >= 1_000 -> "%.0fK tokens".format(n / 1_000.0)
    else -> "$n tokens"
}

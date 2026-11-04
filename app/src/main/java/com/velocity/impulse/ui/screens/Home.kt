package com.velocity.impulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type

@Composable
fun HomeScreen(m: AppModel, openChat: (String?) -> Unit, openModels: () -> Unit, openContexts: () -> Unit) {
    val accent = LocalAccent.current
    Page {
        Column(Modifier.padding(top = Space.S3), verticalArrangement = Arrangement.spacedBy(Space.S1)) {
            BasicText(m.me?.name?.let { tr("Hi, $it", "Cześć, $it") } ?: "Impulse", style = Type.Display)
            BasicText(
                when {
                    m.online == false -> tr("Your computer cannot be reached right now.", "Nie ma teraz połączenia z Twoim komputerem.")
                    m.status?.model != null -> tr("Ready. Everything runs on your computer.", "Gotowe. Wszystko działa na Twoim komputerze.")
                    else -> tr("Load a model to start chatting.", "Wczytaj model, żeby zacząć rozmowę.")
                },
                style = Type.Body.copy(color = Ink.Three),
            )
        }

        Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.S2)) {
            Tile(Glyph.Chat, tr("New chat", "Nowy czat"), tr("Ask anything", "Zapytaj o cokolwiek"), Modifier.weight(1f).fillMaxHeight(), accent) { openChat(null) }
            Tile(
                Glyph.Models, tr("Model", "Model"),
                m.status?.model?.name ?: tr("Not loaded", "Nie wczytano"), Modifier.weight(1f).fillMaxHeight(), null, openModels,
            )
        }
        Tile(
            Glyph.Contexts, tr("Context", "Kontekst"),
            m.defaultContext ?: tr("None. Answers use only the chat.", "Brak. Odpowiedzi biorą tylko z czatu."), Modifier.fillMaxWidth(), null, openContexts,
        )

        val recent = m.chats.take(5)
        if (recent.isNotEmpty()) {
            BasicText(tr("Continue", "Kontynuuj"), style = Type.Section, modifier = Modifier.padding(top = Space.S3))
            Column(Modifier.background(Tone.App, Shapes.Tile), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                recent.forEach { c ->
                    Column(
                        Modifier.fillMaxWidth().pressable({ openChat(c.id) }, Shapes.Tile, Tone.Panel, Tone.Raise, depth = 0.99f)
                            .padding(horizontal = Space.S4, vertical = Space.S3),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicText(c.title.ifBlank { tr("Chat", "Czat") }, Modifier.weight(1f), style = Type.Body.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            BasicText(whenLabel(c.updatedAt), style = Type.Meta.copy(color = Ink.Four))
                        }
                        BasicText(c.messages.lastOrNull()?.text.orEmpty().replace('\n', ' '), style = Type.Meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun Tile(glyph: Glyph, label: String, value: String, modifier: Modifier, tint: androidx.compose.ui.graphics.Color?, onClick: () -> Unit) {
    Column(
        modifier.pressable(onClick, Shapes.Card, Tone.Panel, Tone.Raise, stretch = true).padding(Space.S4),
    ) {
        Box(Modifier.size(34.dp).background(tint?.copy(alpha = 0.16f) ?: Tone.Raise, Shapes.ControlSm), contentAlignment = Alignment.Center) {
            Icon(glyph, tint ?: Ink.Two, size = 18.dp)
        }
        Spacer(Modifier.height(Space.S3))
        BasicText(label, style = Type.Label)
        Spacer(Modifier.height(2.dp))
        BasicText(value, style = Type.Body.copy(fontWeight = FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

fun tokens(n: Long): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0).replace(".0M", "M") + " " + com.velocity.impulse.ui.Lang.tr("tokens", "tokenów")
    n >= 1_000 -> "%.0fK".format(n / 1_000.0) + " " + com.velocity.impulse.ui.Lang.tr("tokens", "tokenów")
    else -> "$n " + com.velocity.impulse.ui.Lang.tr("tokens", "tokenów")
}

/** "15:06" today, "Wednesday" this week, "3 Nov" before that. */
fun whenLabel(ms: Long): String {
    if (ms <= 0) return ""
    val z = java.time.ZoneId.systemDefault()
    val t = java.time.Instant.ofEpochMilli(ms).atZone(z)
    val today = java.time.LocalDate.now(z)
    val loc = if (com.velocity.impulse.ui.Lang.pl) java.util.Locale.forLanguageTag("pl") else java.util.Locale.ENGLISH
    return when {
        t.toLocalDate() == today -> "%02d:%02d".format(t.hour, t.minute)
        t.toLocalDate().isAfter(today.minusDays(7)) -> t.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, loc)
        else -> "${t.dayOfMonth} ${t.month.getDisplayName(java.time.format.TextStyle.SHORT, loc)}"
    }
}

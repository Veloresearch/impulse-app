package com.velocity.impulse.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.data.Context
import com.velocity.impulse.ui.components.Empty
import com.velocity.impulse.ui.components.OnAccent
import com.velocity.impulse.ui.components.PageTitle
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type

@Composable
fun ContextsScreen(m: AppModel) {
    Page {
        PageTitle(
            tr("Contexts", "Konteksty"),
            tr("Large collections of your documents that a chat can draw on. Pick one and every answer can use it.", "Duże zbiory Twoich dokumentów, z których czat może korzystać. Wybierz jeden, a każda odpowiedź może z niego czerpać."),
        )
        if (m.contexts.isEmpty()) {
            Empty(
                tr("No contexts yet", "Nie ma jeszcze kontekstów"),
                tr("Contexts are made on the computer, from folders of documents. Create one in Impulse on your PC and it appears here.", "Konteksty tworzysz na komputerze, z folderów z dokumentami. Utwórz jeden w Impulse, a pojawi się tutaj."),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Space.S2)) {
                ContextCard(null, m.defaultContext == null) { m.useContext(null) }
                m.contexts.forEach { c -> ContextCard(c, m.defaultContext == c.name) { m.useContext(c.name) } }
            }
        }
    }
}

@Composable
private fun ContextCard(c: Context?, on: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current
    val fill by animateColorAsState(if (on) Tone.Raise else Tone.Panel, label = "card")
    val mark by animateFloatAsState(if (on) 1f else 0f, Motion.press(), label = "mark")
    Row(
        Modifier.fillMaxWidth().pressable(onClick, Shapes.Card, fill, Tone.Press).padding(Space.S4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(c?.name ?: tr("No context", "Bez kontekstu"), style = Type.Body.copy(fontWeight = FontWeight.SemiBold))
            BasicText(
                if (c == null) tr("Answers use only the chat itself", "Odpowiedzi biorą tylko z samego czatu")
                else listOfNotNull(c.tokens?.let { tokens(it) }, tr("${c.documents} documents", "dokumenty: ${c.documents}")).joinToString(" · "),
                style = Type.Meta,
            )
        }
        Spacer(Modifier.width(Space.S3))
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(22.dp).background(Tone.Hover, CircleShape))
            Box(
                Modifier.size(26.dp).graphicsLayer { scaleX = mark; scaleY = mark; alpha = mark }.background(accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                com.velocity.impulse.ui.components.Icon(com.velocity.impulse.ui.components.Glyph.Check, OnAccent, size = 14.dp)
            }
        }
    }
}

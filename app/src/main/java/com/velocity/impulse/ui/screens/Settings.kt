package com.velocity.impulse.ui.screens

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.velocity.impulse.BuildConfig
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.ui.Lang
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.Route
import com.velocity.impulse.ui.components.Avatar
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Group
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.ListRow
import com.velocity.impulse.ui.components.PageTitle
import com.velocity.impulse.ui.components.Toggle
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.theme.Accents
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Signal
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type

/** The fourth place: you, and a short list of everything that is not a page of its own. */
@Composable
fun MoreScreen(m: AppModel, go: (Route) -> Unit) {
    Page {
        val me = m.me
        Row(
            Modifier.fillMaxWidth().pressable({ go(Route.Settings) }, Shapes.Card, Tone.Panel, Tone.Raise, tilt = 5f).padding(Space.S4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (me != null) Avatar(me.name, me.avatar, me.photo, 56.dp)
            Spacer(Modifier.width(Space.S4))
            Column(Modifier.weight(1f)) {
                BasicText(me?.name ?: "Impulse", style = Type.Title)
                BasicText(
                    listOf(linkLabel(m.link), m.server?.base?.substringAfter("://").orEmpty()).joinToString(" · "),
                    style = Type.Meta, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Glyph.Right, Ink.Four, size = 16.dp)
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Entry(Glyph.Contexts, tr("Contexts", "Konteksty"), m.defaultContext ?: tr("None chosen", "Nie wybrano"), top = true) { go(Route.Contexts) }
            Entry(Glyph.Settings, tr("Settings", "Ustawienia"), tr("Thinking, accent, motion", "Myślenie, akcent, ruch")) { go(Route.Settings) }
            Entry(Glyph.Link, tr("Computer", "Komputer"), linkLabel(m.link), bottom = true) { go(Route.Computer) }
        }
        Row(
            Modifier.fillMaxWidth().pressable({ m.signOut() }, Shapes.Tile, Tone.Panel, Tone.Raise).padding(Space.S4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Glyph.SignOut, Signal.Err, size = 18.dp)
            Spacer(Modifier.width(Space.S3))
            BasicText(tr("Sign out", "Wyloguj się"), style = Type.Body.copy(color = Signal.Err, fontWeight = FontWeight.Medium))
        }
    }
}

@Composable
private fun Entry(glyph: Glyph, title: String, detail: String, top: Boolean = false, bottom: Boolean = false, onClick: () -> Unit) {
    val accent = LocalAccent.current
    val shape = when {
        top -> androidx.compose.foundation.shape.RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        bottom -> androidx.compose.foundation.shape.RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
        else -> androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
    }
    Row(
        Modifier.fillMaxWidth().pressable(onClick, shape, Tone.Panel, Tone.Raise, depth = 0.985f, tilt = 2f).padding(horizontal = Space.S4, vertical = Space.S3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).background(accent.copy(alpha = 0.14f), Shapes.ControlSm), contentAlignment = Alignment.Center) {
            Icon(glyph, accent, size = 18.dp)
        }
        Spacer(Modifier.width(Space.S3))
        Column(Modifier.weight(1f)) {
            BasicText(title, style = Type.Body.copy(fontWeight = FontWeight.Medium))
            BasicText(detail, style = Type.Meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Glyph.Right, Ink.Four, size = 16.dp)
    }
}

@Composable
fun SettingsScreen(m: AppModel) {
    val uri = LocalUriHandler.current
    Page {
        PageTitle(tr("Settings", "Ustawienia"))
        Group(tr("Chat", "Czat")) {
            ListRow(tr("Show thinking", "Pokazuj myślenie"), detail = tr("The model's reasoning, folded above each answer", "Rozumowanie modelu, zwinięte nad odpowiedzią")) {
                Toggle(m.showThinking, { m.setThinking(it) })
            }
        }
        Group(tr("Appearance", "Wygląd")) {
            Column(Modifier.fillMaxWidth().background(Tone.Panel).padding(Space.S4)) {
                BasicText(tr("Accent", "Akcent"), style = Type.Body)
                Spacer(Modifier.size(Space.S3))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Accents.forEach { (name, c) ->
                        val ring by animateDpAsState(if (name == m.accent) 5.dp else 0.dp, Motion.press(), label = "ring")
                        Box(
                            Modifier.size(32.dp).pressable({ m.pickAccent(name) }, CircleShape, c, c, depth = 0.85f, stretch = true),
                            contentAlignment = Alignment.Center,
                        ) { Box(Modifier.size(ring * 2.4f).background(Tone.Panel, CircleShape)) }
                    }
                }
            }
            ListRow(tr("Reduce motion", "Ogranicz ruch"), detail = tr("Cross-fades instead of movement", "Przenikanie zamiast ruchu")) {
                Toggle(m.reduceMotion, { m.calmMotion(it) })
            }
            ListRow(tr("Language", "Język"), detail = tr("Follows Impulse on your computer", "Tak jak w Impulse na komputerze")) {
                BasicText(if (Lang.pl) "Polski" else "English", style = Type.Body.copy(color = Ink.Three))
            }
        }
        Group(tr("About", "O aplikacji")) {
            ListRow("Impulse for Android", detail = tr("Version", "Wersja") + " " + BuildConfig.VERSION_NAME)
            ListRow(tr("Source code", "Kod źródłowy"), detail = "github.com/Veloresearch/impulse-app", onClick = { uri.openUri("https://github.com/Veloresearch/impulse-app") }) {
                Icon(Glyph.Right, Ink.Four, size = 16.dp)
            }
        }
    }
}

@Composable
fun ComputerScreen(m: AppModel) {
    Page {
        PageTitle(tr("Computer", "Komputer"), tr("The computer this phone talks to.", "Komputer, z którym rozmawia ten telefon."))
        Group {
            ListRow(linkLabel(m.link), detail = m.server?.base?.substringAfter("://")) {
                Icon(if (m.online == false) Glyph.Alert else Glyph.Link, if (m.online == false) Signal.Err else Ink.Three, size = 18.dp)
            }
            ListRow(tr("Signed in as", "Zalogowano jako"), detail = m.me?.name)
        }
        Group {
            ListRow(tr("Connect to another computer", "Połącz z innym komputerem"), onClick = { m.forgetServer() }) {
                Icon(Glyph.Right, Ink.Four, size = 16.dp)
            }
        }
        BasicText(
            tr("Your chats live on your computer and sync to this phone.", "Twoje czaty są na komputerze i synchronizują się z telefonem."),
            style = Type.Meta.copy(color = Ink.Four), modifier = Modifier.padding(start = Space.S1),
        )
    }
}

@Suppress("unused")
private val keep = Color.Transparent

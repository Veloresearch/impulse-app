package com.velocity.impulse.ui.screens

import com.velocity.impulse.data.Personas
import com.velocity.impulse.data.Persona
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.ui.LocalShell
import com.velocity.impulse.ui.Shell
import com.velocity.impulse.ui.Surface
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.ModelFace
import com.velocity.impulse.ui.components.Toggle
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Signal
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type

/** Rows slide in from the right edge one after another as the panel opens. */
private fun Modifier.arrive(shell: Shell, slot: Int) = graphicsLayer {
    if (shell.calm) return@graphicsLayer
    val k = (-shell.drawer.value).coerceIn(0f, 1f)
    val p = (k * 1.5f - slot * 0.07f).coerceIn(0f, 1f)
    translationX = (1f - p) * 48.dp.toPx()
    alpha = p
}

/**
 * The right side of the room: everything about the chat in front of you. Its model, the context it
 * reads from, pinning it, starting fresh, deleting it.
 */
@Composable
fun Details(m: AppModel, modifier: Modifier = Modifier) {
    val shell = LocalShell.current
    val accent = LocalAccent.current
    val c = m.chat
    Column(
        modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(Space.S3),
    ) {
        Column(Modifier.arrive(shell, 0).padding(start = 8.dp, top = 8.dp, bottom = 4.dp)) {
            BasicText(tr("This chat", "Ten czat"), style = Type.Label)
            Spacer(Modifier.size(2.dp))
            BasicText(c?.title?.ifBlank { null } ?: tr("New chat", "Nowy czat"), style = Type.Title, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (c != null) BasicText(
                tr("${c.messages.count { it.role == "user" }} questions · started ", "pytania: ${c.messages.count { it.role == "user" }} · od ") + whenLabel(c.raw.optLong("createdAt")),
                style = Type.Meta,
            )
        }

        Section(shell, 1, tr("Model", "Model")) {
            val name = m.status?.model?.name
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp)
                    .pressable({ shell.open(Surface.Model, "details-model") }, Shapes.ControlLg, Tone.Panel, Tone.Raise, depth = 0.985f, tilt = 2f)
                    .padding(horizontal = Space.S3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (name != null) ModelFace(m, name, m.status?.model?.family.orEmpty(), 36.dp)
                else Box(Modifier.size(36.dp).background(Tone.Raise, Shapes.ControlSm), contentAlignment = Alignment.Center) { Icon(Glyph.Models, Ink.Three, size = 18.dp) }
                Spacer(Modifier.width(Space.S3))
                Column(Modifier.weight(1f)) {
                    BasicText(name ?: tr("No model loaded", "Nie wczytano modelu"), style = Type.Body.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    BasicText(tr("Change", "Zmień"), style = Type.Meta.copy(color = accent))
                }
                Icon(Glyph.Right, Ink.Four, size = 16.dp)
            }
        }

        if (c != null) Section(shell, 2, tr("Personality", "Osobowość")) {
            Voices(c.persona) { m.setPersona(it) }
        }

        Section(shell, 3, tr("Context", "Kontekst")) {
            val chosen = c?.contextName ?: m.defaultContext
            val info = m.contexts.firstOrNull { it.name == chosen }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp)
                    .pressable({ shell.open(Surface.Context, "details-context") }, Shapes.ControlLg, Tone.Panel, Tone.Raise, depth = 0.985f, tilt = 2f)
                    .padding(horizontal = Space.S3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(36.dp).background(if (chosen != null) accent.copy(alpha = 0.16f) else Tone.Raise, Shapes.ControlSm), contentAlignment = Alignment.Center) {
                    Icon(Glyph.Contexts, if (chosen != null) accent else Ink.Three, size = 18.dp)
                }
                Spacer(Modifier.width(Space.S3))
                Column(Modifier.weight(1f)) {
                    BasicText(chosen ?: tr("No context", "Bez kontekstu"), style = Type.Body.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    BasicText(
                        if (chosen == null) tr("Answers from the chat alone", "Odpowiedzi tylko z czatu")
                        else info?.tokens?.let { tokens(it) } ?: tr("Change", "Zmień"),
                        style = Type.Meta,
                    )
                }
                Icon(Glyph.Right, Ink.Four, size = 16.dp)
            }
        }

        if (c != null) Section(shell, 4, null) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).background(Tone.Panel, Shapes.ControlLg).padding(horizontal = Space.S3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Glyph.Pin, Ink.Two, size = 18.dp)
                Spacer(Modifier.width(Space.S3))
                BasicText(tr("Pinned", "Przypięty"), Modifier.weight(1f), style = Type.Body)
                Toggle(c.pinned, { m.pin(c.id) })
            }
            Spacer(Modifier.size(2.dp))
            Line(Glyph.Plus, tr("New chat", "Nowy czat"), Ink.One) { shell.newChat() }
            Spacer(Modifier.size(2.dp))
            Line(Glyph.Trash, tr("Delete this chat", "Usuń ten czat"), Signal.Err) {
                m.delete(c.id)
                shell.closeDrawer()
            }
        }
    }
}

/** Off first, then the panel's personalities as faces; the chosen one is lifted in the accent. */
@Composable
private fun Voices(chosen: String?, onPick: (String?) -> Unit) {
    val accent = LocalAccent.current
    val all = listOf<Persona?>(null) + Personas
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        all.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { p ->
                    val on = p?.id == chosen
                    val fill by animateColorAsState(if (on) accent.copy(alpha = 0.16f) else Tone.Panel, label = "voice")
                    val lift by animateFloatAsState(if (on) 1f else 0f, spring(0.55f, 480f), label = "lift")
                    Column(
                        Modifier.weight(1f).heightIn(min = 88.dp)
                            .pressable({ onPick(p?.id) }, Shapes.ControlLg, fill, Tone.Raise, depth = 0.95f, tilt = 6f)
                            .padding(vertical = Space.S3, horizontal = Space.S2),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        BasicText(
                            p?.face ?: "∅",
                            Modifier.graphicsLayer {
                                scaleX = 1f + 0.18f * lift
                                scaleY = scaleX
                                translationY = -3.dp.toPx() * lift
                            },
                            style = Type.Title.copy(color = if (p == null) Ink.Three else Ink.One),
                        )
                        Spacer(Modifier.size(4.dp))
                        BasicText(
                            p?.let { tr(it.nameEn, it.namePl) } ?: tr("Off", "Wyłączona"),
                            style = Type.Label.copy(color = if (on) accent else Ink.Two, fontSize = 11.sp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Section(shell: Shell, slot: Int, title: String?, content: @Composable () -> Unit) {
    Column(Modifier.arrive(shell, slot)) {
        if (title != null) BasicText(title, Modifier.padding(start = 8.dp, bottom = 6.dp), style = Type.Label)
        content()
    }
}

@Composable
private fun Line(glyph: Glyph, label: String, ink: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).pressable(onClick, Shapes.ControlLg, Tone.Panel, Tone.Raise, depth = 0.985f).padding(horizontal = Space.S3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(glyph, ink, size = 18.dp)
        Spacer(Modifier.width(Space.S3))
        BasicText(label, style = Type.Body.copy(color = ink, fontWeight = FontWeight.Medium))
    }
}

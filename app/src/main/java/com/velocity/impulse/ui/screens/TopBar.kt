package com.velocity.impulse.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Probe
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.components.Avatar
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.Segmented
import com.velocity.impulse.ui.components.Sheet
import com.velocity.impulse.ui.components.Variant
import com.velocity.impulse.ui.components.Btn
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type

/** The bar every main page shares with the panel: the model, and who is signed in. */
@Composable
fun TopBar(m: AppModel, onModels: () -> Unit, onMe: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = Space.S3, end = Space.S3, top = Space.S2, bottom = Space.S1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ModelPill(m, onModels)
        Spacer(Modifier.weight(1f))
        val me = m.me
        if (me != null) Box(Modifier.pressable(onMe, depth = 0.9f, stretch = true)) { Avatar(me.name, me.avatar, me.photo, 36.dp) }
    }
}

@Composable
fun ModelPill(m: AppModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    val name = m.status?.model?.name
    val label = when {
        m.loading != null -> tr("Loading…", "Wczytywanie…")
        name != null -> name
        m.online == false -> tr("Computer offline", "Komputer niedostępny")
        else -> tr("No model loaded", "Nie wczytano modelu")
    }
    Row(
        modifier.height(40.dp).pressable(onClick, Shapes.ControlLg, Tone.Panel, Tone.Raise, stretch = true).padding(start = Space.S3, end = Space.S2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (m.loading != null) Spinner(accent) else Box(
            Modifier.size(7.dp).background(if (name != null) accent else Ink.Four, CircleShape),
        )
        Spacer(Modifier.width(Space.S2))
        AnimatedContent(
            label,
            Modifier.widthIn(max = 230.dp),
            transitionSpec = { (fadeIn(tween(Motion.M2)) + slideInVertically(Motion.travel()) { it / 2 }) togetherWith fadeOut(tween(Motion.M1)) },
            label = "model",
        ) { BasicText(it, style = Type.Body.copy(fontWeight = FontWeight.Medium, fontSize = Type.Meta.fontSize.times(1.08f)), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        Spacer(Modifier.width(Space.S1))
        Icon(Glyph.Down, Ink.Three, size = 14.dp)
    }
}

@Composable
fun Spinner(color: androidx.compose.ui.graphics.Color, size: androidx.compose.ui.unit.Dp = 14.dp) {
    val turn by rememberInfiniteTransition(label = "spin").animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = androidx.compose.animation.core.LinearEasing)), label = "a")
    Canvas(Modifier.size(size).graphicsLayer { rotationZ = turn }) {
        val w = this.size.minDimension * 0.14f
        drawCircle(color.copy(alpha = 0.18f), style = Stroke(w))
        drawArc(color, -90f, 90f, false, style = Stroke(w, cap = StrokeCap.Round), topLeft = Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(this.size.width - w, this.size.height - w))
    }
}

private val Efforts = listOf("off", "low", "medium", "high", "max")

/** Pick a model and how hard it thinks, from wherever you are. */
@Composable
fun ModelSheet(m: AppModel, open: Boolean, onClose: () -> Unit) {
    val accent = LocalAccent.current
    Sheet(open, onClose) {
        BasicText(tr("Model", "Model"), style = Type.Title)
        Spacer(Modifier.height(Space.S1))
        BasicText(tr("The model on your computer that writes the answers.", "Model na Twoim komputerze, który pisze odpowiedzi."), style = Type.Meta)
        Spacer(Modifier.height(Space.S4))
        if (m.status?.model?.thinks != false) {
            BasicText(tr("Thinking", "Myślenie"), style = Type.Label, modifier = Modifier.padding(start = Space.S1, bottom = Space.S2))
            Segmented(
                listOf(tr("Instant", "Od razu"), tr("Low", "Krótko"), tr("Medium", "Średnio"), tr("High", "Dużo"), "Max"),
                Efforts.indexOf(m.effort ?: "medium").coerceAtLeast(0),
                { m.pickEffort(Efforts[it]) },
            )
            Spacer(Modifier.height(Space.S4))
        }
        LazyColumn(Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(m.library, key = { it.path }) { p -> ModelRow(m, p, accent) }
        }
        if (m.library.isEmpty()) BasicText(
            tr("No models on the computer yet. Add one in Impulse › Models.", "Na komputerze nie ma jeszcze modeli. Dodaj je w Impulse › Modele."),
            style = Type.Body.copy(color = Ink.Three),
        )
        if (m.status?.model != null) {
            Spacer(Modifier.height(Space.S3))
            Btn(tr("Unload", "Zwolnij model"), { m.unload() }, Modifier.fillMaxWidth(), Variant.Quiet)
        }
    }
}

@Composable
fun ModelRow(m: AppModel, p: Probe, accent: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val on = m.status?.model?.path?.replace('\\', '/')?.equals(p.path.replace('\\', '/'), ignoreCase = true) == true
    val busy = m.loading == p.path
    Row(
        modifier.fillMaxWidth().heightIn(min = 60.dp)
            .pressable({ if (!on) m.load(p) }, Shapes.ControlLg, if (on) Tone.Raise else Tone.Panel, Tone.Press, depth = 0.985f, enabled = m.loading == null || busy)
            .padding(horizontal = Space.S4, vertical = Space.S3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).background(if (on) accent.copy(alpha = 0.16f) else Tone.Raise, Shapes.ControlSm), contentAlignment = Alignment.Center) {
            Icon(Glyph.Models, if (on) accent else Ink.Three, size = 18.dp)
        }
        Spacer(Modifier.width(Space.S3))
        Column(Modifier.weight(1f)) {
            BasicText(p.name, style = Type.Body.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(modelLine(p), style = Type.Meta, maxLines = 1)
        }
        Spacer(Modifier.width(Space.S2))
        when {
            busy -> Spinner(accent, 18.dp)
            on -> Icon(Glyph.Check, accent, size = 18.dp)
        }
    }
}

fun modelLine(p: Probe): String {
    val params = when {
        p.params >= 1_000_000_000 -> "%.1fB".format(p.params / 1e9).replace(".0B", "B")
        p.params > 0 -> "%.0fM".format(p.params / 1e6)
        else -> null
    }
    val size = if (p.bytes > 0) "%.1f GB".format(p.bytes / 1e9) else null
    return listOfNotNull(params, p.format.uppercase(), size).joinToString(" · ")
}

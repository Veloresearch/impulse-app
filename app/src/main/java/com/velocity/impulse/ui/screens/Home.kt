package com.velocity.impulse.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.LocalShell
import com.velocity.impulse.ui.Surface
import com.velocity.impulse.ui.components.Btn
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.grows
import com.velocity.impulse.ui.components.Variant
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Type
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Standing { Offline, Loading, Ready, None }

/** The empty new chat: a greeting that settles in word by word, then one line on where things stand. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Greeting(m: AppModel, modifier: Modifier = Modifier) {
    val shell = LocalShell.current
    val calm = shell.calm
    val hour = remember { java.time.LocalTime.now().hour }
    val hello = when {
        hour < 12 -> tr("Good morning", "Dzień dobry")
        hour < 18 -> tr("Good afternoon", "Dzień dobry")
        else -> tr("Good evening", "Dobry wieczór")
    }
    val name = m.me?.name?.substringBefore(' ')?.ifBlank { null }
    val title = if (name != null) "$hello, $name" else "Impulse"
    val words = remember(title) { title.split(' ') }
    val rise = remember(title) { words.map { Animatable(0f) } }
    val line = remember { Animatable(0f) }
    val action = remember { Animatable(0f) }

    LaunchedEffect(title) {
        rise.forEachIndexed { i, a ->
            launch {
                if (!calm) delay(40L * i)
                a.animateTo(1f, if (calm) tween(220) else spring(0.9f, 400f))
            }
        }
    }
    LaunchedEffect(Unit) {
        if (calm) {
            launch { line.animateTo(1f, tween(220)) }
            action.animateTo(1f, tween(220))
            return@LaunchedEffect
        }
        delay(40L * (words.size - 1) + 120)
        launch { line.animateTo(1f, tween(Motion.M3, easing = Motion.Out)) }
        delay(80)
        action.animateTo(1f, tween(Motion.M3, easing = Motion.Out))
    }

    val standing = when {
        m.online == false -> Standing.Offline
        m.loading != null -> Standing.Loading
        m.status?.model != null -> Standing.Ready
        else -> Standing.None
    }
    val lift = if (calm) 0.dp else 10.dp
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(Modifier.padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                words.forEachIndexed { i, w ->
                    BasicText(
                        w,
                        Modifier.graphicsLayer {
                            val a = rise[i].value
                            translationY = (1f - a) * lift.toPx()
                            alpha = a.coerceIn(0f, 1f)
                        },
                        style = Type.Display.copy(textAlign = TextAlign.Center),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            AnimatedContent(
                standing,
                Modifier.graphicsLayer {
                    val a = line.value
                    alpha = a
                    if (!calm) translationY = (1f - a) * 6.dp.toPx()
                },
                transitionSpec = { fadeIn(tween(Motion.M2)) togetherWith fadeOut(tween(Motion.M1)) },
                contentAlignment = Alignment.TopCenter,
                label = "standing",
            ) { s ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val note = Type.Body.copy(color = Ink.Three, textAlign = TextAlign.Center)
                    val button = Modifier.padding(top = 20.dp).graphicsLayer {
                        val a = action.value
                        alpha = a
                        if (!calm) {
                            scaleX = 0.94f + 0.06f * a
                            scaleY = scaleX
                        }
                    }
                    when (s) {
                        Standing.Ready -> {
                            val model = m.status?.model?.name.orEmpty()
                            BasicText(tr("$model is ready on your computer.", "$model czeka na Twoim komputerze."), style = note)
                        }
                        Standing.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                            val path = m.loading.orEmpty()
                            val model = m.library.firstOrNull { it.path == path }?.name ?: path.substringAfterLast('/').substringAfterLast('\\')
                            Spinner(LocalAccent.current, 14.dp)
                            Spacer(Modifier.width(8.dp))
                            BasicText(tr("Loading $model…", "Wczytywanie $model…"), style = note)
                        }
                        Standing.None -> {
                            BasicText(tr("Pick a model to start.", "Wybierz model, żeby zacząć."), style = note)
                            Btn(
                                tr("Choose a model", "Wybierz model"),
                                { shell.open(Surface.Model, "greeting-model") },
                                button.grows(shell.sheet, "greeting-model", Shapes.Card),
                                Variant.Primary,
                                Glyph.Models,
                            )
                        }
                        Standing.Offline -> {
                            BasicText(tr("Your computer can't be reached right now.", "Nie ma teraz połączenia z Twoim komputerem."), style = note)
                            Btn(
                                tr("Check connection", "Sprawdź połączenie"),
                                { shell.open(Surface.Computer, "greeting-computer") },
                                button.grows(shell.page, "greeting-computer", Shapes.Card),
                                Variant.Standard,
                            )
                        }
                    }
                }
            }
        }
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

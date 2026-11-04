package com.velocity.impulse.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.velocity.impulse.BuildConfig
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Link
import com.velocity.impulse.ui.Lang
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.LocalShell
import com.velocity.impulse.ui.components.Avatar
import com.velocity.impulse.ui.components.Btn
import com.velocity.impulse.ui.components.Feel
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Group
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.ListRow
import com.velocity.impulse.ui.components.PageTitle
import com.velocity.impulse.ui.components.Roll
import com.velocity.impulse.ui.components.Sheet
import com.velocity.impulse.ui.components.Toggle
import com.velocity.impulse.ui.components.Variant
import com.velocity.impulse.ui.components.morphText
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.theme.Accents
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Signal
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import kotlinx.coroutines.delay

@Composable
fun SettingsPage(m: AppModel, scope: AnimatedVisibilityScope, scroll: ScrollState) {
    val uri = LocalUriHandler.current
    var confirm by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Page(scroll) {
            PageTitle(tr("Settings", "Ustawienia"), titleModifier = Modifier.morphText(LocalShell.current.pageOrigin, scope))
            Column(Modifier.arrive(scope), verticalArrangement = Arrangement.spacedBy(Space.S4)) {
                m.me?.let { me ->
                    val host = m.server?.base?.substringAfter("://").orEmpty()
                    Row(Modifier.fillMaxWidth().background(Tone.Panel, Shapes.Card).padding(Space.S4), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(me.name, me.avatar, me.photo, 56.dp)
                        Spacer(Modifier.width(Space.S4))
                        Column(Modifier.weight(1f)) {
                            BasicText(me.name, style = Type.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            BasicText(tr("Signed in on $host", "Zalogowano na $host"), style = Type.Meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Group(tr("Chat", "Czat")) {
                    ListRow(tr("Show thinking", "Pokazuj myślenie"), detail = tr("The model's reasoning, folded above each answer", "Rozumowanie modelu, zwinięte nad odpowiedzią")) {
                        Toggle(m.showThinking, { m.setThinking(it) })
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(Space.S2)) {
                    Group(tr("Appearance", "Wygląd")) {
                        Column(Modifier.fillMaxWidth().background(Tone.Panel).padding(Space.S4)) {
                            BasicText(tr("Accent", "Akcent"), style = Type.Body)
                            Spacer(Modifier.size(Space.S3))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Accents.forEach { (name, c) ->
                                    val ring by animateDpAsState(if (name == m.accent) 5.dp else 0.dp, Motion.press(), label = "ring")
                                    Box(
                                        Modifier.size(32.dp).pressable({ m.pickAccent(name) }, CircleShape, c, c, depth = 0.85f, feel = Feel.Tick, stretch = true),
                                        contentAlignment = Alignment.Center,
                                    ) { Box(Modifier.size(ring * 2.4f).background(Tone.Panel, CircleShape)) }
                                }
                            }
                        }
                        ListRow(tr("Reduce motion", "Ogranicz ruch"), detail = tr("Cross-fades instead of movement", "Przenikanie zamiast ruchu")) {
                            Toggle(m.reduceMotion, { m.calmMotion(it) })
                        }
                    }
                    val lang = if (Lang.pl) "Polski" else "English"
                    BasicText(
                        tr("Language follows Impulse on your computer: $lang.", "Język jak w Impulse na komputerze: $lang."),
                        Modifier.padding(horizontal = Space.S1),
                        style = Type.Meta.copy(color = Ink.Four),
                    )
                }
                Group(tr("About", "O aplikacji")) {
                    ListRow("Impulse for Android", detail = tr("Version", "Wersja") + " " + BuildConfig.VERSION_NAME)
                    ListRow(tr("Source code", "Kod źródłowy"), detail = "github.com/Veloresearch/impulse-app", onClick = { uri.openUri("https://github.com/Veloresearch/impulse-app") }) {
                        Icon(Glyph.Right, Ink.Four, size = 16.dp)
                    }
                }
                Btn(tr("Sign out", "Wyloguj się"), { confirm = true }, Modifier.fillMaxWidth(), Variant.Danger, Glyph.SignOut)
            }
        }
        Confirm(
            confirm, { confirm = false },
            tr("Sign out?", "Wylogować?"),
            tr("Sign out of Impulse on this phone? Your chats stay on the computer.", "Wylogować z Impulse na tym telefonie? Czaty zostają na komputerze."),
            tr("Sign out", "Wyloguj się"),
        ) { m.signOut() }
    }
}

@Composable
fun ComputerPage(m: AppModel, scope: AnimatedVisibilityScope, scroll: ScrollState) {
    var confirm by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Page(scroll) {
            PageTitle(
                tr("Computer", "Komputer"),
                tr("The computer this phone talks to.", "Komputer, z którym rozmawia ten telefon."),
                titleModifier = Modifier.morphText(LocalShell.current.pageOrigin, scope),
            )
            Column(Modifier.arrive(scope), verticalArrangement = Arrangement.spacedBy(Space.S4)) {
                Machine(m)
                Group {
                    ListRow(tr("Signed in as", "Zalogowano jako"), detail = m.me?.name)
                }
                Btn(tr("Connect to another computer", "Połącz z innym komputerem"), { confirm = true }, Modifier.fillMaxWidth(), Variant.Danger)
            }
        }
        Confirm(
            confirm, { confirm = false }, null,
            tr("This phone forgets this computer and its copy of your chats. Chats stay on the computer.", "Telefon zapomni ten komputer i swoją kopię czatów. Czaty zostają na komputerze."),
            tr("Connect to another computer", "Połącz z innym komputerem"),
        ) { m.forgetServer() }
    }
}

/** The computer at a glance: how it is reached, whether it answers, and its address. */
@Composable
private fun Machine(m: AppModel) {
    val down = m.online == false
    val dot by animateColorAsState(if (down) Signal.Err else Signal.Ok, tween(Motion.M2), label = "dot")
    val address = m.server?.base?.substringAfter("://").orEmpty()
    val clipboard = LocalClipboardManager.current
    var copies by remember { mutableIntStateOf(0) }
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copies) {
        if (copies == 0) return@LaunchedEffect
        copied = true
        delay(1400)
        copied = false
    }
    val note by animateFloatAsState(if (copied) 1f else 0f, tween(Motion.M2), label = "copied")

    Column(Modifier.fillMaxWidth().background(Tone.Panel, Shapes.Card).padding(Space.S4), verticalArrangement = Arrangement.spacedBy(Space.S3)) {
        Box(Modifier.size(48.dp).background(Tone.Raise, Shapes.Tile), contentAlignment = Alignment.Center) {
            Icon(
                when (m.link) {
                    Link.Local -> Glyph.Wifi
                    Link.Tailscale -> Glyph.Shield
                    Link.Cloudflare -> Glyph.Cloud
                    else -> Glyph.Globe
                },
                Ink.Two, size = 24.dp,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.S1)) {
            BasicText(tr("Your computer", "Twój komputer"), style = Type.Section)
            Row {
                Box(Modifier.padding(top = 6.dp).size(7.dp).background(dot, CircleShape))
                Spacer(Modifier.width(Space.S2))
                Roll(
                    if (down) {
                        tr("Can't reach it. Check that Impulse is open and Settings › Phone is on.", "Brak połączenia. Sprawdź, czy Impulse jest otwarty, a Ustawienia › Telefon włączone.")
                    } else {
                        tr("Connected over ${linkLabel(m.link)}", "Połączono: ${linkLabel(m.link)}")
                    },
                    Type.Meta, Modifier.weight(1f), maxLines = 3,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                address,
                Modifier.weight(1f, fill = false).pressable(
                    {}, depth = 1f, feel = null,
                    onLong = { clipboard.setText(AnnotatedString(address)); copies++ },
                ),
                style = Type.Mono, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(Space.S2))
            BasicText(tr("Copied", "Skopiowano"), Modifier.graphicsLayer { alpha = note }, style = Type.Meta)
        }
    }
}

@Composable
private fun Confirm(open: Boolean, onClose: () -> Unit, title: String?, body: String, action: String, onAction: () -> Unit) {
    Sheet(open, onClose) {
        Column(Modifier.padding(top = Space.S2), verticalArrangement = Arrangement.spacedBy(Space.S2)) {
            if (title != null) BasicText(title, style = Type.Title)
            BasicText(body, style = Type.Body.copy(color = Ink.Three))
            Spacer(Modifier.height(Space.S4))
            Btn(action, { onClose(); onAction() }, Modifier.fillMaxWidth(), Variant.Danger)
            Btn(tr("Cancel", "Anuluj"), onClose, Modifier.fillMaxWidth(), Variant.Quiet)
        }
    }
}

/** The page body rises in once the title has landed: name, then state, then content. */
@Composable
private fun Modifier.arrive(scope: AnimatedVisibilityScope): Modifier {
    val calm = LocalShell.current.calm
    val lift = with(LocalDensity.current) { 24.dp.roundToPx() }
    return with(scope) {
        this@arrive.animateEnterExit(
            enter = if (calm) {
                fadeIn(tween(Motion.M2))
            } else {
                fadeIn(tween(Motion.M3, delayMillis = 120)) + slideInVertically(spring(0.9f, 380f)) { lift }
            },
            exit = fadeOut(tween(Motion.M1)),
        )
    }
}

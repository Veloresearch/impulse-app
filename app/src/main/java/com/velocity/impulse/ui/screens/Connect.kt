package com.velocity.impulse.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Discovery
import com.velocity.impulse.data.Found
import com.velocity.impulse.data.Link
import com.velocity.impulse.data.Profile
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.components.Avatar
import com.velocity.impulse.ui.components.Btn
import com.velocity.impulse.ui.components.Disclosure
import com.velocity.impulse.ui.components.Field
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Group
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.Segmented
import com.velocity.impulse.ui.components.Variant
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Signal
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun ConnectScreen(m: AppModel) {
    val ctx = LocalContext.current
    var mode by rememberSaveable { mutableStateOf(0) }
    var address by rememberSaveable { mutableStateOf("") }
    val found = remember { mutableStateListOf<Found>() }
    var looking by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        withTimeoutOrNull(12_000) {
            Discovery.find(ctx).collect { f -> if (found.none { it.base == f.base }) found += f }
        }
        looking = false
    }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Space.S5),
        verticalArrangement = Arrangement.spacedBy(Space.S5),
    ) {
        Spacer(Modifier.height(Space.S5))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.S2)) {
            BasicText(tr("Connect to your computer", "Połącz z komputerem"), style = Type.Display.copy(textAlign = TextAlign.Center))
            BasicText(
                tr("Pick Impulse found on your network, or scan the code from Impulse › Settings › Phone.", "Wybierz Impulse znaleziony w Twojej sieci albo zeskanuj kod z Impulse › Ustawienia › Telefon."),
                style = Type.Body.copy(color = Ink.Three, textAlign = TextAlign.Center),
            )
        }

        Nearby(found, looking, m.busy) { m.connect(it.base) }

        Segmented(listOf(tr("Scan code", "Skanuj kod"), tr("Type address", "Wpisz adres")), mode, { mode = it })
        AnimatedContent(
            mode,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (fadeIn(tween(Motion.M3, easing = Motion.Out)) + slideInHorizontally(Motion.travel()) { it / 6 * dir }) togetherWith
                    (fadeOut(tween(Motion.M1)) + slideOutHorizontally(tween(Motion.M2)) { -it / 8 * dir })
            },
            label = "mode",
        ) { which ->
            if (which == 0) {
                Scanner(onCode = { m.connect(it) })
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Space.S3)) {
                    Field(
                        address, { address = it }, "192.168.1.20 / pc.tailnet.ts.net",
                        keyboard = KeyboardType.Uri, action = ImeAction.Go, onAction = { m.connect(address) },
                    )
                    Btn(
                        if (m.busy) tr("Connecting…", "Łączenie…") else tr("Connect", "Połącz"), { m.connect(address) },
                        Modifier.fillMaxWidth(), Variant.Primary, enabled = address.isNotBlank() && !m.busy,
                    )
                }
            }
        }
        Problem(m.error)
        Group(tr("Other ways in", "Inne sposoby")) {
            Disclosure(tr("Same Wi-Fi", "Ta sama sieć Wi-Fi")) {
                Hint(tr("Turn on Settings › Phone › Local network in Impulse. The phone and the computer must be on the same network.", "Włącz w Impulse Ustawienia › Telefon › Sieć lokalna. Telefon i komputer muszą być w tej samej sieci."))
            }
            Disclosure(tr("Tailscale, from anywhere", "Tailscale, z każdego miejsca")) {
                Hint(tr("Install Tailscale on the computer and on this phone, signed in to the same account. Then choose Tailscale in Settings › Phone and scan its code.", "Zainstaluj Tailscale na komputerze i na telefonie, na tym samym koncie. Wybierz Tailscale w Ustawienia › Telefon i zeskanuj kod."))
            }
            Disclosure(tr("Cloudflare Tunnel", "Tunel Cloudflare")) {
                Hint(tr("Impulse can open a Cloudflare Tunnel: a quick link with no account, or your own tunnel behind Zero Trust. Its code carries everything this app needs.", "Impulse może otworzyć tunel Cloudflare: szybki link bez konta albo własny tunel za Zero Trust. Kod zawiera wszystko, czego potrzebuje aplikacja."))
            }
        }
        Spacer(Modifier.height(Space.S6))
    }
}

@Composable
private fun Nearby(found: List<Found>, looking: Boolean, busy: Boolean, onPick: (Found) -> Unit) {
    val accent = LocalAccent.current
    Column(verticalArrangement = Arrangement.spacedBy(Space.S2)) {
        Row(Modifier.padding(start = Space.S1), verticalAlignment = Alignment.CenterVertically) {
            BasicText(tr("On your network", "W Twojej sieci"), Modifier.weight(1f), style = Type.Label)
            AnimatedVisibility(looking, enter = fadeIn(), exit = fadeOut()) { Spinner(Ink.Three, 13.dp) }
        }
        found.forEach { f ->
            androidx.compose.animation.AnimatedVisibility(
                visibleState = remember { androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } },
                enter = fadeIn(tween(Motion.M3)) + slideInVertically(Motion.travel()) { it / 2 } + scaleIn(Motion.travel(), 0.94f),
            ) {
                Row(
                    Modifier.fillMaxWidth().pressable({ onPick(f) }, Shapes.Card, Tone.Panel, Tone.Raise, enabled = !busy, stretch = true).padding(Space.S4),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(42.dp).background(accent.copy(alpha = 0.14f), Shapes.ControlLg), contentAlignment = Alignment.Center) {
                        Icon(Glyph.Phone, accent, size = 20.dp)
                    }
                    Spacer(Modifier.width(Space.S3))
                    Column(Modifier.weight(1f)) {
                        BasicText(f.people.firstOrNull()?.let { tr("$it's computer", "Komputer: $it") } ?: f.name, style = Type.Body.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        BasicText(f.base.substringAfter("://") + " · " + linkLabel(Link.of(f.base)), style = Type.Meta, maxLines = 1)
                    }
                    Icon(Glyph.Right, Ink.Three, size = 16.dp)
                }
            }
        }
        if (!looking && found.isEmpty()) BasicText(
            tr("Nothing found. Turn on Local network in Impulse, or scan the code below.", "Nic nie znaleziono. Włącz Sieć lokalną w Impulse albo zeskanuj kod poniżej."),
            Modifier.padding(horizontal = Space.S1), style = Type.Meta,
        )
    }
}

fun linkLabel(l: Link?): String = when (l) {
    Link.Local -> tr("Local network", "Sieć lokalna")
    Link.Tailscale -> "Tailscale"
    Link.Cloudflare -> "Cloudflare Tunnel"
    Link.Secure -> tr("Secure link", "Bezpieczny link")
    Link.Remote -> tr("Internet", "Internet")
    null -> tr("Not connected", "Brak połączenia")
}

@Composable
fun SignInScreen(m: AppModel) {
    var chosen by remember { mutableStateOf<Profile?>(null) }
    var password by remember { mutableStateOf("") }
    val usable = m.profiles.filter { it.hasPassword && it.remote }
    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Space.S5),
        verticalArrangement = Arrangement.spacedBy(Space.S5),
    ) {
        Spacer(Modifier.height(Space.S6))
        Column(verticalArrangement = Arrangement.spacedBy(Space.S2)) {
            BasicText(tr("Who is this?", "Kto korzysta?"), style = Type.Display)
            BasicText(tr("Pick your profile and type its password.", "Wybierz swój profil i wpisz hasło."), style = Type.Body.copy(color = Ink.Three))
        }
        if (m.profiles.isNotEmpty() && usable.isEmpty()) {
            Hint(tr("No profile can be opened from a phone yet. In Impulse, open Settings › Profiles, give your profile a password and allow other devices.", "Żaden profil nie może być jeszcze otwarty z telefonu. W Impulse otwórz Ustawienia › Profile, ustaw hasło i zezwól na inne urządzenia."))
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.S2)) {
            m.profiles.forEach { p ->
                ProfileCard(p, p == chosen, p in usable) { chosen = if (chosen == p) null else p; password = "" }
                AnimatedVisibility(
                    p == chosen,
                    enter = expandVertically(Motion.travel()) + fadeIn(tween(Motion.M2, 80)),
                    exit = shrinkVertically(Motion.settle()) + fadeOut(tween(Motion.M1)),
                ) {
                    Column(Modifier.padding(top = Space.S1), verticalArrangement = Arrangement.spacedBy(Space.S2)) {
                        Field(password, { password = it }, tr("Password", "Hasło"), secret = true, keyboard = KeyboardType.Password, action = ImeAction.Go, onAction = { m.signIn(p, password) })
                        Btn(if (m.busy) tr("Signing in…", "Logowanie…") else tr("Sign in", "Zaloguj"), { m.signIn(p, password) }, Modifier.fillMaxWidth(), Variant.Primary, enabled = password.isNotEmpty() && !m.busy)
                    }
                }
            }
        }
        Problem(m.error)
        Btn(tr("Use another computer", "Użyj innego komputera"), { m.forgetServer() }, Modifier.fillMaxWidth(), Variant.Quiet)
    }
}

@Composable
private fun ProfileCard(p: Profile, selected: Boolean, usable: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().pressable(onClick, Shapes.Card, if (selected) Tone.Raise else Tone.Panel, Tone.Press, enabled = usable, stretch = true)
            .padding(Space.S4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(p.name, p.avatar, p.photo, 44.dp)
        Spacer(Modifier.width(Space.S3))
        Column(Modifier.weight(1f)) {
            BasicText(p.name, style = Type.Body.copy(fontWeight = FontWeight.SemiBold))
            BasicText(
                when {
                    !p.hasPassword -> tr("No password yet", "Bez hasła")
                    !p.remote -> tr("Only on that computer", "Tylko na tamtym komputerze")
                    else -> tr("Password", "Hasło")
                },
                style = Type.Meta,
            )
        }
        Icon(if (usable) Glyph.Right else Glyph.Lock, Ink.Four, size = 16.dp)
    }
}

@Composable
fun Hint(text: String) = BasicText(text, style = Type.Body.copy(color = Ink.Three))

@Composable
fun Problem(message: String?) {
    var last by remember { mutableStateOf(message.orEmpty()) }
    if (message != null) last = message
    AnimatedVisibility(
        message != null,
        enter = fadeIn(tween(Motion.M2)) + scaleIn(Motion.press(), initialScale = 0.96f) + expandVertically(Motion.travel()),
        exit = fadeOut(tween(Motion.M1)) + shrinkVertically(tween(Motion.M2)) + scaleOut(targetScale = 0.98f),
    ) {
        Row(Modifier.fillMaxWidth().background(Signal.Err.copy(alpha = 0.12f), Shapes.Tile).padding(Space.S4)) {
            BasicText(last, style = Type.Body.copy(color = Signal.Err))
        }
    }
}

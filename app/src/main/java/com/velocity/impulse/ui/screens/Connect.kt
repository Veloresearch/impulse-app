package com.velocity.impulse.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Discovery
import com.velocity.impulse.data.Found
import com.velocity.impulse.data.Link
import com.velocity.impulse.data.Profile
import com.velocity.impulse.data.Server
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.components.Avatar
import com.velocity.impulse.ui.components.Btn
import com.velocity.impulse.ui.components.Feel
import com.velocity.impulse.ui.components.Field
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.ImpulseMark
import com.velocity.impulse.ui.components.Roll
import com.velocity.impulse.ui.components.Sheet
import com.velocity.impulse.ui.components.Variant
import com.velocity.impulse.ui.components.feel
import com.velocity.impulse.ui.components.polite
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.components.shake
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.LocalReduceMotion
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Signal
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private enum class From { Nearby, Scan, Typed, Link }

@Composable
fun ConnectScreen(m: AppModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val rise = rememberRise()
    var address by rememberSaveable { mutableStateOf("") }
    var open by rememberSaveable { mutableStateOf<From?>(null) }
    var from by remember { mutableStateOf(From.Nearby) }
    var pending by remember { mutableStateOf<String?>(null) }
    val found = remember { mutableStateListOf<Found>() }
    var looking by remember { mutableStateOf(true) }
    var round by remember { mutableIntStateOf(0) }
    // A connect this screen did not start came from an impulse://connect link. Work already
    // running when the screen arrived belongs to the screen before it.
    var mine by remember { mutableStateOf(false) }
    val was = remember { booleanArrayOf(m.busy) }
    LaunchedEffect(m.busy) {
        val rose = m.busy && !was[0]
        was[0] = m.busy
        if (rose && !mine) {
            from = From.Link
            open = null
        }
        if (!m.busy) mine = false
    }
    LaunchedEffect(round) {
        looking = true
        withTimeoutOrNull(12_000) {
            Discovery.find(ctx).collect { f ->
                if (found.none { it.base == f.base }) {
                    found += f
                    delay(90)
                }
            }
        }
        looking = false
    }
    fun connect(raw: String, how: From) {
        if (m.busy) return
        mine = true
        from = how
        m.connect(raw)
    }
    fun toggle(f: From) {
        open = if (open == f) null else f
    }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Space.S5),
        verticalArrangement = Arrangement.spacedBy(Space.S6),
    ) {
        Spacer(Modifier.height(Space.S5))
        Head(
            tr("Connect to your computer", "Połącz z komputerem"),
            tr("Impulse runs there. This app talks to it.", "Impulse działa na komputerze. Ta aplikacja z nim rozmawia."),
            rise,
        )

        AnimatedVisibility(
            from == From.Link && (m.busy || m.error != null),
            enter = expandVertically(Motion.travel(), Alignment.Top) + fadeIn(tween(Motion.M2, delayMillis = 40)),
            exit = shrinkVertically(Motion.settle(), Alignment.Top) + fadeOut(tween(Motion.M1)),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.S3)) {
                LinkCard(m.busy)
                Problem(m.error)
            }
        }

        Column(rise(3), verticalArrangement = Arrangement.spacedBy(Space.S3)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Label(tr("On this network", "W tej sieci"), Modifier.weight(1f))
                Roll(if (looking && found.isNotEmpty()) tr("Still looking", "Szukam dalej") else "", Type.Label)
            }
            found.forEach { f ->
                key(f.base) {
                    AnimatedVisibility(
                        visibleState = remember { MutableTransitionState(false).apply { targetState = true } },
                        enter = expandVertically(Motion.travel(), Alignment.Top) +
                            fadeIn(tween(Motion.M3, easing = Motion.Out)) +
                            slideInVertically(Motion.calmText()) { it / 3 } +
                            scaleIn(Motion.calmText(), 0.95f),
                    ) {
                        FoundCard(f, m.busy && pending == f.base) {
                            if (!m.busy) {
                                pending = f.base
                                connect(f.base, From.Nearby)
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(
                found.isEmpty(),
                enter = expandVertically(Motion.travel(), Alignment.Top) + fadeIn(tween(Motion.M2)),
                exit = shrinkVertically(Motion.settle(), Alignment.Top) + fadeOut(tween(Motion.M1)),
            ) { Looking(looking) { round++ } }
            Problem(m.error.takeIf { from == From.Nearby })
        }

        Column(rise(4), verticalArrangement = Arrangement.spacedBy(Space.S3)) {
            Label(tr("Or connect directly", "Albo połącz bezpośrednio"))
            Fold(
                Glyph.QrCode,
                tr("Scan the code", "Zeskanuj kod"),
                if (m.busy && from == From.Scan) tr("Connecting…", "Łączenie…") else tr("Impulse › Settings › Phone", "Impulse › Ustawienia › Telefon"),
                open == From.Scan,
                { toggle(From.Scan) },
            ) {
                Scanner(
                    onCode = { code ->
                        isImpulseCode(code).also { ok ->
                            if (ok) scope.launch {
                                delay(520)
                                open = null
                                connect(code, From.Scan)
                            }
                        }
                    },
                    shape = Shapes.Control,
                )
            }
            Fold(
                Glyph.Link,
                tr("Type an address", "Wpisz adres"),
                tr("An IP, a Tailscale name or a tunnel link", "IP, nazwa Tailscale albo link tunelu"),
                open == From.Typed,
                { toggle(From.Typed) },
            ) {
                val focus = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    delay(280)
                    focus.requestFocus()
                }
                Column(verticalArrangement = Arrangement.spacedBy(Space.S2)) {
                    Field(
                        address, { address = it }, "192.168.1.20", Modifier.focusRequester(focus),
                        keyboard = KeyboardType.Uri, action = ImeAction.Go, onAction = { if (address.isNotBlank()) connect(address, From.Typed) },
                    )
                    Btn(
                        if (m.busy && from == From.Typed) tr("Connecting…", "Łączenie…") else tr("Connect", "Połącz"),
                        { connect(address, From.Typed) },
                        Modifier.fillMaxWidth(), Variant.Primary, enabled = address.isNotBlank() && !m.busy,
                    )
                }
            }
            Problem(m.error.takeIf { from == From.Scan || from == From.Typed })
        }

        Column(rise(5), verticalArrangement = Arrangement.spacedBy(Space.S3)) {
            Label(tr("Away from home", "Poza domem"))
            Column(Modifier.fillMaxWidth().island()) {
                Way(Glyph.Shield, "Tailscale", tr("Your own private network, from anywhere", "Twoja prywatna sieć, z każdego miejsca"))
                Way(Glyph.Cloud, "Cloudflare Tunnel", tr("A secure link, no port to open", "Bezpieczny link, bez otwierania portów"))
            }
            BasicText(
                tr("Turn either on in Impulse › Settings › Phone, then scan its code.", "Włącz jedno z nich w Impulse › Ustawienia › Telefon i zeskanuj kod."),
                Modifier.padding(horizontal = Space.S1),
                style = Type.Meta,
            )
        }
        Spacer(Modifier.height(Space.S5))
    }
}

/** Impulse shows either its connect link or the bare origin of the computer, nothing else. */
private fun isImpulseCode(raw: String): Boolean {
    val s = raw.trim()
    if (s.startsWith("impulse://")) return runCatching { Server.parse(s) }.isSuccess
    return Regex("https?://[^\\s/?#]+/?").matches(s)
}

fun linkLabel(l: Link?): String = when (l) {
    Link.Local -> tr("Local network", "Sieć lokalna")
    Link.Tailscale -> "Tailscale"
    Link.Cloudflare -> "Cloudflare Tunnel"
    Link.Secure -> tr("Secure link", "Bezpieczny link")
    Link.Remote -> tr("Internet", "Internet")
    null -> tr("Not connected", "Brak połączenia")
}

private fun Modifier.island(shape: Shape = Shapes.Card) =
    shadow(10.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.28f), spotColor = Color.Black.copy(alpha = 0.4f))
        .background(Tone.Panel, shape)

@Composable
private fun Head(title: String, line: String, rise: (Int) -> Modifier) {
    Column(Modifier.fillMaxWidth()) {
        ImpulseMark(Modifier.size(30.dp).then(rise(0)))
        Spacer(Modifier.height(Space.S5))
        BasicText(title, rise(1).semantics { heading() }, style = Type.BrandHero)
        Spacer(Modifier.height(Space.S2))
        BasicText(line, rise(2), style = Type.Body.copy(color = Ink.Three))
    }
}

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) =
    BasicText(text, modifier.padding(start = Space.S1), style = Type.Label)

@Composable
private fun Tile(glyph: Glyph, size: Int = 40) {
    Box(Modifier.size(size.dp).background(Tone.Raise, Shapes.ControlLg), contentAlignment = Alignment.Center) {
        Icon(glyph, Ink.Two, size = (size * 0.48f).dp)
    }
}

@Composable
private fun LinkCard(busy: Boolean) {
    Row(Modifier.fillMaxWidth().island().padding(Space.S4), verticalAlignment = Alignment.CenterVertically) {
        Tile(Glyph.Link)
        Spacer(Modifier.width(Space.S3))
        Column(Modifier.weight(1f)) {
            BasicText(tr("Opening the link", "Otwieram link"), style = Type.Body.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
            Roll(if (busy) tr("Connecting…", "Łączenie…") else tr("It didn't connect", "Nie udało się połączyć"), Type.Meta, Modifier.polite())
        }
        if (busy) Spinner(LocalAccent.current)
    }
}

/** Discovery names a computer by its address when it did not announce a name of its own. */
private fun Found.title(): String =
    if (name != base.substringAfter("://").substringBefore(':')) name
    else people.firstOrNull()?.let { tr("$it's computer", "Komputer: $it") } ?: name

@Composable
private fun FoundCard(f: Found, connecting: Boolean, onPick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .pressable(onPick, depth = 0.98f, tilt = 3f)
            .island()
            .padding(start = Space.S4, end = Space.S3, top = Space.S4, bottom = Space.S4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(f.title(), style = Type.Section, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Roll(if (connecting) tr("Connecting…", "Łączenie…") else f.base.substringAfter("://"), Type.Meta)
        }
        People(f.people)
        Spacer(Modifier.width(Space.S3))
        Box(Modifier.size(34.dp).background(Tone.Raise, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Glyph.Send, Ink.One, Modifier.graphicsLayer { rotationZ = 90f }, size = 16.dp)
        }
    }
}

@Composable
private fun People(names: List<String>) {
    if (names.isEmpty()) return
    val shown = names.take(3)
    Box(Modifier.size(width = (26 + 16 * (shown.size - 1)).dp, height = 26.dp)) {
        for (i in shown.indices.reversed()) {
            Box(Modifier.offset(x = (16 * i).dp).size(26.dp).background(Tone.Panel, CircleShape), contentAlignment = Alignment.Center) {
                Avatar(shown[i], null, null, 22.dp)
            }
        }
    }
    if (names.size > shown.size) BasicText("+${names.size - shown.size}", Modifier.padding(start = Space.S1), style = Type.Label)
}

/** Holds the place of the first computer while the network is searched; tapping it after a miss searches again. */
@Composable
private fun Looking(looking: Boolean, onAgain: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(Shapes.Card).background(Tone.Panel.copy(alpha = 0.55f))
            .then(if (looking) Modifier else Modifier.pressable(onAgain, depth = 0.98f, feel = Feel.Tick)),
    ) {
        Column(Modifier.padding(Space.S4), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Roll(
                if (looking) tr("Looking for Impulse", "Szukam Impulse") else tr("No computer found", "Nie znaleziono komputera"),
                Type.Body.copy(color = Ink.Two, fontWeight = FontWeight.Medium),
            )
            Roll(
                if (looking) tr("Computers with Local network on show up here.", "Komputery z włączoną siecią lokalną pojawią się tutaj.")
                else tr("Turn on Local network in Impulse › Settings › Phone, then tap to look again.", "Włącz Sieć lokalną w Impulse › Ustawienia › Telefon i stuknij, by szukać ponownie."),
                Type.Meta,
                maxLines = 3,
            )
        }
        if (looking) Box(Modifier.align(Alignment.TopEnd).padding(Space.S4)) { Spinner(LocalAccent.current) }
    }
}

/** A card whose body unfolds under its header in place. */
@Composable
private fun Fold(
    glyph: Glyph,
    title: String,
    line: String,
    open: Boolean,
    onToggle: () -> Unit,
    body: @Composable () -> Unit,
) {
    val turn = animateFloatAsState(if (open) 180f else 0f, Motion.calmText(), label = "turn")
    Column(Modifier.fillMaxWidth().island()) {
        Row(
            Modifier.fillMaxWidth()
                .semantics { stateDescription = if (open) tr("Open", "Otwarte") else tr("Closed", "Zamknięte") }
                .pressable(onToggle, depth = 1f, feel = Feel.Tick)
                .padding(Space.S4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tile(glyph)
            Spacer(Modifier.width(Space.S3))
            Column(Modifier.weight(1f)) {
                BasicText(title, style = Type.Body.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
                Roll(line, Type.Meta)
            }
            Icon(Glyph.Down, Ink.Three, Modifier.graphicsLayer { rotationZ = turn.value }, size = 16.dp)
        }
        AnimatedVisibility(
            open,
            enter = expandVertically(Motion.travel(), Alignment.Top) +
                fadeIn(tween(Motion.M2, delayMillis = 60)) +
                scaleIn(Motion.calmText(), 0.96f, TransformOrigin(0.5f, 0f)),
            exit = shrinkVertically(Motion.settle(), Alignment.Top) + fadeOut(tween(Motion.M1)),
        ) {
            Box(Modifier.padding(start = Space.S2, end = Space.S2, bottom = Space.S2)) { body() }
        }
    }
}

@Composable
private fun Way(glyph: Glyph, title: String, line: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Space.S4, vertical = Space.S3), verticalAlignment = Alignment.CenterVertically) {
        Tile(glyph, 36)
        Spacer(Modifier.width(Space.S3))
        Column(Modifier.weight(1f)) {
            BasicText(title, style = Type.Body.copy(fontWeight = FontWeight.Medium))
            BasicText(line, style = Type.Meta)
        }
    }
}

@Composable
fun SignInScreen(m: AppModel) {
    val rise = rememberRise()
    val view = LocalView.current
    val usable = m.profiles.filter { it.hasPassword && it.remote }
    val locked = m.profiles - usable.toSet()
    var chosen by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var shakes by remember { mutableIntStateOf(0) }
    val only = usable.singleOrNull()
    LaunchedEffect(only?.id) { if (only != null && chosen == null) chosen = only.id }
    LaunchedEffect(m.error) {
        if (m.error != null && chosen != null) {
            shakes++
            view.feel(Feel.Reject)
        }
    }
    val host = m.server?.base?.substringAfter("://").orEmpty()

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Space.S5),
            verticalArrangement = Arrangement.spacedBy(Space.S6),
        ) {
            Spacer(Modifier.height(Space.S5))
            Head(
                tr("Sign in", "Zaloguj się"),
                tr("Choose your profile on $host · ${linkLabel(m.link)}", "Wybierz profil na $host · ${linkLabel(m.link)}"),
                rise,
            )

            if (m.profiles.isEmpty()) Profiles(m.busy, rise(3)) { m.loadProfiles() }

            if (usable.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(Space.S3)) {
                usable.forEachIndexed { i, p ->
                    key(p.id) {
                        ProfileCard(
                            p, chosen == p.id, chosen != null && chosen != p.id, rise(3 + i),
                            {
                                chosen = if (chosen == p.id) null else p.id
                                password = ""
                            },
                        ) {
                            val focus = remember { FocusRequester() }
                            LaunchedEffect(Unit) {
                                delay(280)
                                focus.requestFocus()
                            }
                            Field(
                                password, { password = it }, tr("Password", "Hasło"), Modifier.focusRequester(focus).shake(shakes),
                                secret = true, keyboard = KeyboardType.Password, action = ImeAction.Go,
                                onAction = { if (password.isNotEmpty() && !m.busy) m.signIn(p, password) },
                            )
                            Btn(
                                if (m.busy) tr("Signing in…", "Logowanie…") else tr("Sign in", "Zaloguj"), { m.signIn(p, password) },
                                Modifier.fillMaxWidth(), Variant.Primary, enabled = password.isNotEmpty() && !m.busy,
                            )
                            Problem(m.error)
                        }
                    }
                }
            }

            if (locked.isNotEmpty()) {
                val start = 3 + usable.size
                Column(rise(start), verticalArrangement = Arrangement.spacedBy(Space.S3)) {
                    Label(tr("Can't be opened from a phone", "Niedostępne z telefonu"))
                    if (usable.isEmpty()) {
                        Hint(tr("In Impulse, open Settings › Profiles, give your profile a password and allow other devices.", "W Impulse otwórz Ustawienia › Profile, ustaw hasło i zezwól na inne urządzenia."))
                    }
                    locked.forEach { p -> key(p.id) { LockedRow(p) } }
                }
            }
            if (chosen == null) Problem(m.error)

            Btn(
                tr("Use another computer", "Użyj innego komputera"), { m.forgetServer() },
                Modifier.align(Alignment.CenterHorizontally).then(rise(4 + m.profiles.size)), Variant.Quiet, height = 40.dp,
            )
            Spacer(Modifier.height(Space.S5))
        }
        SwitchComputer(m)
    }
}

/**
 * Asks before following an `impulse://connect` link that arrived while this phone already uses a
 * computer. Hosted by the sign-in screen and, signed in, by the app's top level.
 */
@Composable
fun SwitchComputer(m: AppModel) {
    val link = m.connectLink
    var last by remember { mutableStateOf("") }
    if (link != null) last = link
    val base = remember(last) { runCatching { Server.parse(last).base }.getOrNull() }
    Sheet(link != null, m::dropConnectLink) {
        Column(Modifier.padding(top = Space.S2), verticalArrangement = Arrangement.spacedBy(Space.S2)) {
            BasicText(tr("Switch to this computer?", "Przełączyć na ten komputer?"), Modifier.semantics { heading() }, style = Type.Title)
            if (base != null) {
                val way = Link.of(base)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = Space.S2).background(Tone.Raise, Shapes.Card).padding(Space.S3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Tile(linkGlyph(way), 36)
                    Spacer(Modifier.width(Space.S3))
                    Column(Modifier.weight(1f)) {
                        BasicText(base.substringAfter("://").trimEnd('/'), style = Type.Mono.copy(color = Ink.One), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        BasicText(linkLabel(way), style = Type.Meta)
                    }
                }
                BasicText(
                    tr("You'll choose a profile and sign in there. Chats stay on each computer.", "Wybierzesz profil i zalogujesz się tam. Czaty zostają na każdym komputerze."),
                    style = Type.Body.copy(color = Ink.Three),
                )
                Problem(m.error)
                Spacer(Modifier.height(Space.S2))
                Btn(
                    if (m.busy) tr("Connecting…", "Łączenie…") else tr("Switch", "Przełącz"), { m.connect(last) },
                    Modifier.fillMaxWidth(), Variant.Primary, enabled = !m.busy,
                )
                Btn(tr("Not now", "Nie teraz"), m::dropConnectLink, Modifier.fillMaxWidth(), Variant.Quiet)
            } else {
                BasicText(
                    tr("This link has no computer address in it.", "Ten link nie zawiera adresu komputera."),
                    style = Type.Body.copy(color = Ink.Three),
                )
                Spacer(Modifier.height(Space.S2))
                Btn(tr("Close", "Zamknij"), m::dropConnectLink, Modifier.fillMaxWidth(), Variant.Quiet)
            }
        }
    }
}

private fun linkGlyph(l: Link) = when (l) {
    Link.Local -> Glyph.Wifi
    Link.Tailscale -> Glyph.Shield
    Link.Cloudflare -> Glyph.Cloud
    Link.Secure -> Glyph.Lock
    Link.Remote -> Glyph.Globe
}

/** Stands in for the profiles while they load; after a miss, a tap asks again. */
@Composable
private fun Profiles(loading: Boolean, modifier: Modifier, onAgain: () -> Unit) {
    Row(
        modifier.fillMaxWidth().clip(Shapes.Card).background(Tone.Panel.copy(alpha = 0.55f))
            .then(if (loading) Modifier else Modifier.pressable(onAgain, depth = 0.98f, feel = Feel.Tick))
            .padding(Space.S4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Roll(
                if (loading) tr("Looking for profiles", "Szukam profili") else tr("No profiles came back", "Nie przyszły żadne profile"),
                Type.Body.copy(color = Ink.Two, fontWeight = FontWeight.Medium),
            )
            Roll(
                if (loading) tr("Asking the computer who uses it.", "Pytam komputer, kto go używa.")
                else tr("Check that Impulse is open, then tap to ask again.", "Sprawdź, czy Impulse jest otwarty, i stuknij, by zapytać ponownie."),
                Type.Meta,
                maxLines = 3,
            )
        }
        if (loading) Spinner(LocalAccent.current)
    }
}

/** The chosen card lifts toward the hand and the others step back; its password field grows under it. */
@Composable
private fun ProfileCard(
    p: Profile,
    selected: Boolean,
    dimmed: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    body: @Composable ColumnScope.() -> Unit,
) {
    val lift = animateFloatAsState(if (selected) 1f else 0f, Motion.calmText(), label = "lift")
    val dim = animateFloatAsState(if (dimmed) 1f else 0f, tween(Motion.M3, easing = Motion.Standard), label = "dim")
    Column(
        modifier.fillMaxWidth()
            .semantics { this.selected = selected }
            .graphicsLayer {
                val k = lift.value
                shape = Shapes.Card
                clip = false
                shadowElevation = (8f + 14f * k).dp.toPx()
                ambientShadowColor = Color.Black.copy(alpha = 0.3f)
                spotShadowColor = Color.Black.copy(alpha = 0.45f)
                translationY = -3.dp.toPx() * k
                scaleX = 1f + 0.015f * k
                scaleY = 1f + 0.015f * k
                alpha = 1f - 0.45f * dim.value
            }
            .background(Tone.Panel, Shapes.Card),
    ) {
        Row(
            Modifier.fillMaxWidth().pressable(onClick, depth = 1f, feel = Feel.Tick).padding(Space.S4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(p.name, p.avatar, p.photo, 52.dp)
            Spacer(Modifier.width(Space.S4))
            BasicText(p.name, Modifier.weight(1f), style = Type.Section, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AnimatedVisibility(
            selected,
            enter = expandVertically(Motion.travel(), Alignment.Top) + fadeIn(tween(Motion.M2, delayMillis = 60)),
            exit = shrinkVertically(Motion.settle(), Alignment.Top) + fadeOut(tween(Motion.M1)),
        ) {
            Column(
                Modifier.padding(start = Space.S4, end = Space.S4, bottom = Space.S4),
                verticalArrangement = Arrangement.spacedBy(Space.S2),
                content = body,
            )
        }
    }
}

@Composable
private fun LockedRow(p: Profile) {
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}
            .background(Tone.Panel.copy(alpha = 0.55f), Shapes.Card).padding(horizontal = Space.S4, vertical = Space.S3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(p.name, p.avatar, p.photo, 36.dp, Modifier.graphicsLayer { alpha = 0.7f })
        Spacer(Modifier.width(Space.S3))
        Column(Modifier.weight(1f)) {
            BasicText(p.name, style = Type.Body.copy(color = Ink.Two, fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(
                if (!p.hasPassword) tr("No password yet", "Bez hasła") else tr("Only on that computer", "Tylko na tamtym komputerze"),
                style = Type.Meta,
            )
        }
        Icon(Glyph.Lock, Ink.Four, size = 16.dp)
    }
}

/**
 * First-run arrival on one 900 ms clock read only in the draw phase: item i rises 40 ms after
 * the one before it, capped at the eighth. Calm fades everything in together.
 */
@Composable
private fun rememberRise(): (Int) -> Modifier {
    val calm = LocalReduceMotion.current
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(if (calm) 220 else 900, easing = LinearEasing)) }
    return remember(calm) {
        { i ->
            Modifier.graphicsLayer {
                if (calm) {
                    alpha = t.value
                } else {
                    val p = Motion.Out.transform(((t.value * 900f - minOf(i, 8) * 40f) / 340f).coerceIn(0f, 1f))
                    alpha = p
                    translationY = (1f - p) * 12.dp.toPx()
                }
            }
        }
    }
}

@Composable
fun Hint(text: String) = BasicText(text, Modifier.padding(horizontal = Space.S1), style = Type.Body.copy(color = Ink.Three))

@Composable
fun Problem(message: String?) {
    var last by remember { mutableStateOf(message.orEmpty()) }
    if (message != null) last = message
    AnimatedVisibility(
        message != null,
        enter = expandVertically(Motion.travel(), Alignment.Top) + fadeIn(tween(Motion.M2, delayMillis = 40)),
        exit = shrinkVertically(Motion.settle(), Alignment.Top) + fadeOut(tween(Motion.M1)),
    ) {
        Row(Modifier.fillMaxWidth().polite().padding(horizontal = Space.S1, vertical = Space.S1), verticalAlignment = Alignment.Top) {
            Icon(Glyph.Alert, Signal.Err, Modifier.padding(top = 2.dp), size = 14.dp)
            Spacer(Modifier.width(Space.S2))
            BasicText(last, style = Type.Meta.copy(color = Signal.Err))
        }
    }
}

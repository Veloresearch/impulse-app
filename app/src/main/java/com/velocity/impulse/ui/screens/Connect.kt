package com.velocity.impulse.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
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
import com.velocity.impulse.data.Server
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.components.Avatar
import com.velocity.impulse.ui.components.Btn
import com.velocity.impulse.ui.components.Disclosure
import com.velocity.impulse.ui.components.Field
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Group
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.ImpulseMark
import com.velocity.impulse.ui.components.Sheet
import com.velocity.impulse.ui.components.Variant
import com.velocity.impulse.ui.components.pressable
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

private enum class From { Nearby, Scan, Typed }

@Composable
fun ConnectScreen(m: AppModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val rise = rememberRise()
    var address by rememberSaveable { mutableStateOf("") }
    var typing by rememberSaveable { mutableStateOf(false) }
    var scan by remember { mutableStateOf(false) }
    var from by remember { mutableStateOf(From.Scan) }
    val found = remember { mutableStateListOf<Found>() }
    var looking by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        withTimeoutOrNull(12_000) {
            Discovery.find(ctx).collect { f -> if (found.none { it.base == f.base }) found += f }
        }
        looking = false
    }
    fun connect(raw: String, how: From) {
        from = how
        m.connect(raw)
    }
    val scanFirst = found.isEmpty() && !typing

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Space.S5),
            verticalArrangement = Arrangement.spacedBy(Space.S5),
        ) {
            Spacer(Modifier.height(Space.S5))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.S2)) {
                ImpulseMark(Modifier.size(40.dp).then(rise(0)))
                Spacer(Modifier.height(Space.S1))
                BasicText(tr("Connect to your computer", "Połącz z komputerem"), rise(1), style = Type.Display.copy(textAlign = TextAlign.Center))
                BasicText(
                    tr("Impulse runs on your computer. This app talks to it.", "Impulse działa na Twoim komputerze. Ta aplikacja z nim rozmawia."),
                    rise(2),
                    style = Type.Body.copy(color = Ink.Three, textAlign = TextAlign.Center),
                )
            }

            Column(rise(3), verticalArrangement = Arrangement.spacedBy(Space.S2)) {
                Nearby(found, looking, m.busy) { connect(it.base, From.Nearby) }
                Problem(m.error.takeIf { from == From.Nearby })
            }

            Column(verticalArrangement = Arrangement.spacedBy(Space.S2)) {
                Btn(
                    if (m.busy && from == From.Scan) tr("Connecting…", "Łączenie…") else tr("Scan the code", "Zeskanuj kod"),
                    { scan = true },
                    Modifier.fillMaxWidth().then(rise(if (scanFirst) 5 else 4)),
                    if (scanFirst) Variant.Primary else Variant.Standard,
                    Glyph.QrCode,
                    enabled = !m.busy,
                )
                Problem(m.error.takeIf { from == From.Scan })
                Btn(tr("Type the address instead", "Wpisz adres ręcznie"), { typing = !typing }, Modifier.fillMaxWidth().then(rise(5)), Variant.Quiet)
                AnimatedVisibility(
                    typing,
                    enter = expandVertically(spring(0.82f, 420f)) + fadeIn(tween(Motion.M2, delayMillis = 60)),
                    exit = shrinkVertically(Motion.settle()) + fadeOut(tween(Motion.M1)),
                ) {
                    val focus = remember { FocusRequester() }
                    LaunchedEffect(Unit) {
                        delay(280)
                        focus.requestFocus()
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(Space.S3)) {
                        Field(
                            address, { address = it }, "192.168.1.20 / pc.tailnet.ts.net", Modifier.focusRequester(focus),
                            keyboard = KeyboardType.Uri, action = ImeAction.Go, onAction = { connect(address, From.Typed) },
                        )
                        Btn(
                            if (m.busy && from == From.Typed) tr("Connecting…", "Łączenie…") else tr("Connect", "Połącz"),
                            { connect(address, From.Typed) },
                            Modifier.fillMaxWidth(), Variant.Primary, enabled = address.isNotBlank() && !m.busy,
                        )
                        Problem(m.error.takeIf { from == From.Typed })
                    }
                }
                BasicText(
                    tr("The code is in Impulse › Settings › Phone.", "Kod jest w Impulse › Ustawienia › Telefon."),
                    Modifier.fillMaxWidth().then(rise(6)),
                    style = Type.Meta.copy(textAlign = TextAlign.Center),
                )
            }

            Group(tr("Other ways in", "Inne sposoby"), rise(7)) {
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

        Sheet(scan, { scan = false }) {
            Column(Modifier.padding(top = Space.S2), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.S2)) {
                Scanner(onCode = { code ->
                    isImpulseCode(code).also { ok ->
                        if (ok) scope.launch {
                            delay(520)
                            scan = false
                            connect(code, From.Scan)
                        }
                    }
                })
                BasicText(
                    tr("Point at the code in Impulse › Settings › Phone.", "Skieruj na kod w Impulse › Ustawienia › Telefon."),
                    style = Type.Meta.copy(textAlign = TextAlign.Center),
                )
            }
        }
    }
}

/** Impulse shows either its connect link or the bare origin of the computer, nothing else. */
private fun isImpulseCode(raw: String): Boolean {
    val s = raw.trim()
    if (s.startsWith("impulse://")) return runCatching { Server.parse(s) }.isSuccess
    return Regex("https?://[^\\s/?#]+/?").matches(s)
}

@Composable
private fun Nearby(found: List<Found>, looking: Boolean, busy: Boolean, onPick: (Found) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.S2)) {
        BasicText(tr("On your network", "W Twojej sieci"), Modifier.padding(start = Space.S1), style = Type.Label)
        AnimatedContent(
            found.firstOrNull() to looking,
            transitionSpec = { fadeIn(tween(Motion.M2)) togetherWith fadeOut(tween(Motion.M1)) },
            contentKey = { (first, wait) -> first?.base ?: wait },
            label = "nearby",
        ) { (first, wait) ->
            when {
                first != null -> FoundCard(first, true, busy, onPick)
                wait -> Skeleton()
                else -> BasicText(
                    tr("Nothing found. Turn on Local network in Impulse, or scan the code below.", "Nic nie znaleziono. Włącz Sieć lokalną w Impulse albo zeskanuj kod poniżej."),
                    Modifier.padding(horizontal = Space.S1),
                    style = Type.Meta,
                )
            }
        }
        found.drop(1).forEach { f ->
            key(f.base) {
                AnimatedVisibility(
                    visibleState = remember { MutableTransitionState(false).apply { targetState = true } },
                    enter = fadeIn(tween(Motion.M3)) + slideInVertically(Motion.travel()) { it / 2 } + scaleIn(Motion.travel(), 0.94f),
                ) { FoundCard(f, false, busy, onPick) }
            }
        }
    }
}

@Composable
private fun FoundCard(f: Found, primary: Boolean, busy: Boolean, onPick: (Found) -> Unit) {
    val accent = LocalAccent.current
    Row(
        Modifier.fillMaxWidth().pressable({ onPick(f) }, Shapes.Card, Tone.Panel, Tone.Raise, depth = 0.98f, enabled = !busy).padding(Space.S4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).background(if (primary) accent.copy(alpha = 0.14f) else Tone.Raise, Shapes.ControlLg),
            contentAlignment = Alignment.Center,
        ) { Icon(Glyph.Phone, if (primary) accent else Ink.Two, size = 20.dp) }
        Spacer(Modifier.width(Space.S3))
        Column(Modifier.weight(1f)) {
            BasicText(f.people.firstOrNull()?.let { tr("$it's computer", "Komputer: $it") } ?: f.name, style = Type.Body.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(f.base.substringAfter("://") + " · " + linkLabel(Link.of(f.base)), style = Type.Meta, maxLines = 1)
        }
        Icon(Glyph.Right, Ink.Three, size = 16.dp)
    }
}

/** Holds the place where the first computer found will appear. */
@Composable
private fun Skeleton() {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).background(Tone.Panel, Shapes.Card).padding(Space.S4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).background(Tone.Raise, Shapes.ControlLg))
        Spacer(Modifier.width(Space.S3))
        Column(verticalArrangement = Arrangement.spacedBy(Space.S2)) {
            Box(Modifier.size(132.dp, 12.dp).background(Ink.Four.copy(alpha = 0.3f), Shapes.Chip))
            Box(Modifier.size(88.dp, 10.dp).background(Ink.Four.copy(alpha = 0.3f), Shapes.Chip))
        }
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
    val rise = rememberRise()
    val usable = m.profiles.filter { it.hasPassword && it.remote }
    val locked = m.profiles - usable.toSet()
    var chosen by remember { mutableStateOf<Profile?>(null) }
    var password by remember { mutableStateOf("") }
    val only = usable.singleOrNull()
    LaunchedEffect(only?.id) { if (only != null && chosen == null) chosen = only }
    val host = m.server?.base?.substringAfter("://").orEmpty()

    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Space.S5),
        verticalArrangement = Arrangement.spacedBy(Space.S5),
    ) {
        Spacer(Modifier.height(Space.S6))
        Column(verticalArrangement = Arrangement.spacedBy(Space.S1)) {
            BasicText(tr("Who is this?", "Kto korzysta?"), rise(0), style = Type.Display)
            Row(rise(1), verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    tr("On $host · ${linkLabel(m.link)}", "Na $host · ${linkLabel(m.link)}"),
                    Modifier.weight(1f), style = Type.Meta, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Btn(tr("Use another computer", "Użyj innego komputera"), { m.forgetServer() }, variant = Variant.Quiet, height = 36.dp)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.S2)) {
            usable.forEachIndexed { i, p ->
                ProfileCard(p, p == chosen, true, rise(2 + i), { chosen = if (chosen == p) null else p; password = "" }) {
                    val focus = remember { FocusRequester() }
                    LaunchedEffect(Unit) {
                        delay(280)
                        focus.requestFocus()
                    }
                    Field(
                        password, { password = it }, tr("Password", "Hasło"), Modifier.focusRequester(focus),
                        secret = true, keyboard = KeyboardType.Password, action = ImeAction.Go, onAction = { m.signIn(p, password) },
                    )
                    Btn(
                        if (m.busy) tr("Signing in…", "Logowanie…") else tr("Sign in", "Zaloguj"), { m.signIn(p, password) },
                        Modifier.fillMaxWidth().animateEnterExit(fadeIn(tween(Motion.M2, delayMillis = 80)), fadeOut(tween(Motion.M1))),
                        Variant.Primary, enabled = password.isNotEmpty() && !m.busy,
                    )
                    Problem(m.error)
                }
            }
        }
        if (locked.isNotEmpty()) {
            val start = 2 + usable.size
            Column(verticalArrangement = Arrangement.spacedBy(Space.S2)) {
                BasicText(tr("Not available from a phone", "Niedostępne z telefonu"), rise(start).padding(start = Space.S1), style = Type.Label)
                if (usable.isEmpty()) {
                    Box(rise(start)) {
                        Hint(tr("No profile can be opened from a phone yet. In Impulse, open Settings › Profiles, give your profile a password and allow other devices.", "Żaden profil nie może być jeszcze otwarty z telefonu. W Impulse otwórz Ustawienia › Profile, ustaw hasło i zezwól na inne urządzenia."))
                    }
                }
                locked.forEachIndexed { i, p -> ProfileCard(p, false, false, rise(start + 1 + i), {}) }
            }
        }
        if (chosen == null) Problem(m.error)
    }
}

@Composable
private fun ProfileCard(
    p: Profile,
    selected: Boolean,
    usable: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    body: @Composable AnimatedVisibilityScope.() -> Unit = {},
) {
    val turn by animateFloatAsState(if (selected) 90f else 0f, Motion.calmText(), label = "turn")
    Column(modifier.fillMaxWidth().background(Tone.Panel, Shapes.Card)) {
        Row(
            Modifier.fillMaxWidth().pressable(onClick, Shapes.Card, Tone.Panel, Tone.Raise, depth = 1f, enabled = usable).padding(Space.S4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(p.name, p.avatar, p.photo, 44.dp)
            Spacer(Modifier.width(Space.S3))
            Column(Modifier.weight(1f)) {
                BasicText(p.name, style = Type.Body.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText(
                    when {
                        !p.hasPassword -> tr("No password yet", "Bez hasła")
                        !p.remote -> tr("Only on that computer", "Tylko na tamtym komputerze")
                        else -> tr("Password", "Hasło")
                    },
                    style = Type.Meta,
                )
            }
            if (usable) {
                Icon(Glyph.Right, Ink.Four, Modifier.graphicsLayer { rotationZ = turn }, size = 16.dp)
            } else {
                Icon(Glyph.Lock, Ink.Four, size = 16.dp)
            }
        }
        AnimatedVisibility(
            selected,
            enter = expandVertically(Motion.travel()) + fadeIn(tween(Motion.M2)),
            exit = shrinkVertically(Motion.settle()) + fadeOut(tween(Motion.M1)),
        ) {
            Column(
                Modifier.padding(start = Space.S4, end = Space.S4, bottom = Space.S4),
                verticalArrangement = Arrangement.spacedBy(Space.S2),
            ) { body() }
        }
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

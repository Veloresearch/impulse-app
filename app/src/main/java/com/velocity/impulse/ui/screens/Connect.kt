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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Profile
import com.velocity.impulse.ui.components.Btn
import com.velocity.impulse.ui.components.Disclosure
import com.velocity.impulse.ui.components.Field
import com.velocity.impulse.ui.components.Group
import com.velocity.impulse.ui.components.ImpulseMark
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

@Composable
fun ConnectScreen(m: AppModel) {
    var mode by rememberSaveable { mutableStateOf(0) }
    var address by rememberSaveable { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Space.S5),
        verticalArrangement = Arrangement.spacedBy(Space.S5),
    ) {
        Spacer(Modifier.height(Space.S6))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.S3)) {
            BasicText("Connect to your computer", style = Type.Display.copy(textAlign = TextAlign.Center))
            BasicText(
                "On your PC, open Impulse › Settings › Phone and scan the code it shows.",
                style = Type.Body.copy(color = Ink.Three, textAlign = TextAlign.Center),
            )
        }
        Segmented(listOf("Scan code", "Type address"), mode, { mode = it })
        AnimatedContent(
            mode,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (fadeIn(tween(Motion.M3, easing = Motion.Out)) + androidx.compose.animation.slideInHorizontally(Motion.travel()) { it / 6 * dir }) togetherWith
                    (fadeOut(tween(Motion.M1)) + androidx.compose.animation.slideOutHorizontally(tween(Motion.M2)) { -it / 8 * dir })
            },
            label = "mode",
        ) { which ->
            if (which == 0) {
                Scanner(onCode = { m.connect(it) })
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Space.S3)) {
                    Field(
                        address, { address = it }, "192.168.1.20 or pc.tailnet.ts.net",
                        keyboard = KeyboardType.Uri, action = ImeAction.Go, onAction = { m.connect(address) },
                    )
                    Btn(
                        if (m.busy) "Connecting…" else "Connect", { m.connect(address) },
                        Modifier.fillMaxWidth(), Variant.Primary, enabled = address.isNotBlank() && !m.busy,
                    )
                }
            }
        }
        Problem(m.error)
        Group("Other ways in") {
            Disclosure("Same Wi-Fi") {
                Hint("Turn on Settings › Phone › Local network on the PC. Your phone and the PC must be on the same network; the address looks like 192.168.1.20.")
            }
            Disclosure("Tailscale, from anywhere") {
                Hint("Install Tailscale on the PC and on this phone and sign in to the same account. Then choose Tailscale in Settings › Phone on the PC and scan its code. The traffic stays inside your own tailnet.")
            }
            Disclosure("Cloudflare Tunnel") {
                Hint("Impulse can open a Cloudflare Tunnel: a quick link with no account, or your own tunnel behind Cloudflare Zero Trust. The code it shows carries everything this app needs, including an Access service token.")
            }
        }
        Spacer(Modifier.height(Space.S6))
    }
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
            BasicText("Who is this?", style = Type.Display)
            BasicText("Pick your profile on ${m.link?.label?.lowercase() ?: "the computer"} and type its password.", style = Type.Body.copy(color = Ink.Three))
        }
        if (m.profiles.isNotEmpty() && usable.isEmpty()) {
            Hint("No profile can be opened from a phone yet. On the PC, open Settings › Profiles, give your profile a password and allow other devices.")
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
                        Field(password, { password = it }, "Password", secret = true, keyboard = KeyboardType.Password, action = ImeAction.Go, onAction = { m.signIn(p, password) })
                        Btn(if (m.busy) "Signing in…" else "Sign in", { m.signIn(p, password) }, Modifier.fillMaxWidth(), Variant.Primary, enabled = password.isNotEmpty() && !m.busy)
                    }
                }
            }
        }
        Problem(m.error)
        Btn("Use another computer", { m.forgetServer() }, Modifier.fillMaxWidth(), Variant.Quiet)
    }
}

@Composable
private fun ProfileCard(p: Profile, selected: Boolean, usable: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current
    Row(
        Modifier.fillMaxWidth().pressable(onClick, Shapes.Card, if (selected) Tone.Raise else Tone.Panel, Tone.Press, enabled = usable)
            .padding(Space.S4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).background(if (selected) accent else Tone.Hover, CircleShape), contentAlignment = Alignment.Center) {
            BasicText(p.name.take(1).uppercase(), style = Type.Section.copy(color = if (selected) com.velocity.impulse.ui.components.OnAccent else Ink.One))
        }
        Spacer(Modifier.width(Space.S3))
        Column(Modifier.weight(1f)) {
            BasicText(p.name, style = Type.Body.copy(fontWeight = FontWeight.SemiBold))
            BasicText(
                when {
                    !p.hasPassword -> "No password yet"
                    !p.remote -> "Only on that computer"
                    else -> "Password"
                },
                style = Type.Meta,
            )
        }
        androidx.compose.foundation.Image(
            if (usable) Icons.Default.Person else Icons.Default.Lock, null, Modifier.size(18.dp),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Ink.Four),
        )
    }
}

@Composable
fun Hint(text: String) = BasicText(text, style = Type.Body.copy(color = Ink.Three))

@Composable
fun Problem(message: String?) {
    AnimatedVisibility(
        message != null,
        enter = fadeIn(tween(Motion.M2)) + scaleIn(Motion.press(), initialScale = 0.96f) + expandVertically(Motion.travel()),
        exit = fadeOut(tween(Motion.M1)) + shrinkVertically(tween(Motion.M2)) + scaleOut(targetScale = 0.98f),
    ) {
        Row(Modifier.fillMaxWidth().background(Signal.Err.copy(alpha = 0.12f), Shapes.Tile).padding(Space.S4)) {
            BasicText(message.orEmpty(), style = Type.Body.copy(color = Signal.Err))
        }
    }
}

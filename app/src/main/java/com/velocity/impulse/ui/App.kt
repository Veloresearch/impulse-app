package com.velocity.impulse.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Stage
import com.velocity.impulse.ui.components.ImpulseMark
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.screens.ChatScreen
import com.velocity.impulse.ui.screens.ConnectScreen
import com.velocity.impulse.ui.screens.ContextsScreen
import com.velocity.impulse.ui.screens.HomeScreen
import com.velocity.impulse.ui.screens.SettingsScreen
import com.velocity.impulse.ui.screens.SignInScreen
import com.velocity.impulse.ui.theme.Accents
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.LocalReduceMotion
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Chat("Chat", Icons.Default.Menu),
    Contexts("Contexts", Icons.Default.Build),
    Settings("Settings", Icons.Default.Settings),
}

@Composable
fun App(m: AppModel) {
    CompositionLocalProvider(LocalAccent provides Accents[m.accent], LocalReduceMotion provides m.reduceMotion) {
        var intro by rememberSaveable { mutableStateOf(!m.reduceMotion) }
        val reveal = remember { Animatable(if (intro) 0f else 1f) }
        LaunchedEffect(intro) { if (!intro) reveal.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 140f)) }

        Box(Modifier.fillMaxSize().background(Tone.App)) {
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    val p = reveal.value
                    alpha = p.coerceIn(0f, 1f)
                    scaleX = 0.92f + 0.08f * p
                    scaleY = 0.92f + 0.08f * p
                },
            ) {
                AnimatedContent(
                    m.stage,
                    transitionSpec = {
                        (fadeIn(tween(Motion.M4, easing = Motion.Out)) + scaleIn(tween(Motion.M4, easing = Motion.Out), 0.94f)) togetherWith
                            (fadeOut(tween(Motion.M2)) + scaleOut(tween(Motion.M3, easing = Motion.Leave), 1.04f))
                    },
                    label = "stage",
                ) { stage ->
                    when (stage) {
                        Stage.Connect -> ConnectScreen(m)
                        Stage.SignIn -> SignInScreen(m)
                        Stage.Ready -> Main(m)
                    }
                }
            }
            if (intro) Intro { intro = false }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Main(m: AppModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    val calm = LocalReduceMotion.current
    val typing = WindowInsets.isImeVisible
    val bar by animateDpAsState(if (typing) 0.dp else 84.dp, Motion.travel(), label = "bar")
    BackHandler(tab != Tab.Home) { tab = Tab.Home }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            tab,
            transitionSpec = { pageMotion(initialState.ordinal, targetState.ordinal, calm) },
            label = "tab",
        ) { t ->
            when (t) {
                Tab.Home -> HomeScreen(m, openChat = { tab = Tab.Chat }, openContexts = { tab = Tab.Contexts })
                Tab.Chat -> ChatScreen(m, bar, openContexts = { tab = Tab.Contexts })
                Tab.Contexts -> ContextsScreen(m)
                Tab.Settings -> SettingsScreen(m)
            }
        }
        AnimatedVisibility(
            !typing,
            Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(Motion.travel()) { it } + fadeIn(),
            exit = slideOutVertically(tween(Motion.M2, easing = Motion.Leave)) { it } + fadeOut(),
        ) {
            TabBar(tab) { tab = it }
        }
    }
}

private fun pageMotion(from: Int, to: Int, calm: Boolean): ContentTransform {
    if (calm) return fadeIn(tween(Motion.M2)) togetherWith fadeOut(tween(Motion.M2))
    val dir = if (to > from) 1 else -1
    return (slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 380f)) { it / 5 * dir } + fadeIn(tween(Motion.M3, easing = Motion.Out))) togetherWith
        (slideOutHorizontally(tween(Motion.M3, easing = Motion.Standard)) { -it / 8 * dir } + fadeOut(tween(Motion.M2)))
}

@Composable
private fun TabBar(tab: Tab, onTab: (Tab) -> Unit) {
    val accent = LocalAccent.current
    BoxWithConstraints(
        Modifier.navigationBarsPadding().padding(horizontal = Space.S4, vertical = Space.S2)
            .fillMaxWidth().height(64.dp)
            .background(Tone.Float, Shapes.Panel).padding(6.dp),
    ) {
        val w = maxWidth / Tab.entries.size
        val x by animateDpAsState(w * tab.ordinal, spring(dampingRatio = 0.72f, stiffness = 420f), label = "pill")
        Box(Modifier.offset(x = x).width(w).fillMaxHeight().background(accent.copy(alpha = 0.16f), Shapes.Card))
        Row(Modifier.fillMaxSize()) {
            Tab.entries.forEach { t ->
                val on = t == tab
                val ink by animateColorAsState(if (on) accent else Ink.Three, label = "ink")
                Column(
                    Modifier.weight(1f).fillMaxHeight().pressable({ onTab(t) }, depth = 0.9f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    Image(t.icon, t.label, Modifier.size(22.dp), colorFilter = ColorFilter.tint(ink))
                    Spacer(Modifier.height(2.dp))
                    BasicText(t.label, style = Type.Label.copy(color = ink, fontSize = 11.sp, fontWeight = FontWeight(600)))
                }
            }
        }
    }
}

/**
 * The opening: the spiral grows in from the outside and turns into place, the name settles under
 * it, and the whole thing lifts away into the app.
 */
@Composable
private fun Intro(onDone: () -> Unit) {
    val build = remember { Animatable(0f) }
    val spin = remember { Animatable(-70f) }
    val name = remember { Animatable(0f) }
    val leave = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        coroutineScope {
            launch { build.animateTo(1f, tween(1050, easing = Motion.Out)) }
            launch { spin.animateTo(0f, spring(dampingRatio = 0.62f, stiffness = 60f)) }
            launch {
                delay(520)
                name.animateTo(1f, tween(Motion.M4, easing = Motion.Out))
            }
        }
        delay(140)
        leave.animateTo(1f, tween(460, easing = Motion.Sheet))
        onDone()
    }
    Box(
        Modifier.fillMaxSize().graphicsLayer { alpha = 1f - leave.value }.background(Tone.App),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ImpulseMark(
                Modifier.size(132.dp).graphicsLayer {
                    val l = leave.value
                    scaleX = 1f + l * 0.9f
                    scaleY = 1f + l * 0.9f
                },
                build = build.value,
                spin = spin.value,
            )
            Spacer(Modifier.height(Space.S5))
            BasicText(
                "Impulse",
                Modifier.graphicsLayer {
                    alpha = name.value * (1f - leave.value * 2f).coerceAtLeast(0f)
                    translationY = (1f - name.value) * 14.dp.toPx()
                },
                style = Type.Hero.copy(letterSpacing = (0.12f - 0.15f * name.value).em),
            )
        }
    }
}

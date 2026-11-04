package com.velocity.impulse.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.unit.Dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
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
import com.velocity.impulse.ui.components.LiquidSlots
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
        var handed by rememberSaveable { mutableStateOf(!intro) }
        val reveal = remember { Animatable(if (handed) 1f else 0f) }
        LaunchedEffect(handed) { if (handed) reveal.animateTo(1f, tween(650, easing = Motion.Out)) }

        Box(Modifier.fillMaxSize().background(Tone.App)) {
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    val p = reveal.value
                    alpha = p
                    scaleX = 0.96f + 0.04f * p
                    scaleY = 0.96f + 0.04f * p
                    renderEffect = if (android.os.Build.VERSION.SDK_INT >= 31 && p < 0.99f) {
                        val r = (1f - p) * 18f
                        android.graphics.RenderEffect.createBlurEffect(r, r, android.graphics.Shader.TileMode.CLAMP).asComposeRenderEffect()
                    } else null
                },
            ) {
                if (handed) AnimatedContent(
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
            if (intro) Intro(onHandoff = { handed = true }, onDone = { intro = false })
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

    val glass = remember { HazeState() }
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            tab,
            Modifier.haze(glass),
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
            TabBar(tab, glass) { tab = it }
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
private fun TabBar(tab: Tab, glass: HazeState, onTab: (Tab) -> Unit) {
    val accent = LocalAccent.current
    LiquidSlots(
        Tab.entries.size, tab.ordinal, { onTab(Tab.entries[it]) }, accent.copy(alpha = 0.18f), Shapes.Card,
        Modifier.navigationBarsPadding().padding(horizontal = Space.S4, vertical = Space.S2)
            .fillMaxWidth().height(64.dp)
            .clip(Shapes.Panel)
            .hazeChild(glass, HazeStyle(Tone.App, HazeTint(Tone.Float.copy(alpha = 0.55f)), blurRadius = 26.dp, noiseFactor = 0.04f))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.Transparent)))
            .padding(6.dp),
    ) {
        Tab.entries.forEach { t ->
            val on = t == tab
            val ink by animateColorAsState(if (on) accent else Ink.Three, label = "ink")
            val lift by animateFloatAsState(if (on) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 500f), label = "lift")
            Column(
                Modifier.weight(1f).fillMaxHeight().pressable({ onTab(t) }, depth = 0.88f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Image(t.icon, t.label, Modifier.size(22.dp).graphicsLayer { scaleX = 1f + lift * 0.1f; scaleY = 1f + lift * 0.1f }, colorFilter = ColorFilter.tint(ink))
                Spacer(Modifier.height(2.dp))
                BasicText(t.label, style = Type.Label.copy(color = ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

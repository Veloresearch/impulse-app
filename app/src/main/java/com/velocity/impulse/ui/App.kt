package com.velocity.impulse.ui

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Stage
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.IconBtn
import com.velocity.impulse.ui.components.LiquidSlots
import com.velocity.impulse.ui.components.Weight
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.screens.ChatView
import com.velocity.impulse.ui.screens.ChatsScreen
import com.velocity.impulse.ui.screens.ComputerScreen
import com.velocity.impulse.ui.screens.ConnectScreen
import com.velocity.impulse.ui.screens.ContextsScreen
import com.velocity.impulse.ui.screens.HomeScreen
import com.velocity.impulse.ui.screens.ModelSheet
import com.velocity.impulse.ui.screens.ModelsScreen
import com.velocity.impulse.ui.screens.MoreScreen
import com.velocity.impulse.ui.screens.SettingsScreen
import com.velocity.impulse.ui.screens.SignInScreen
import com.velocity.impulse.ui.screens.TopBar
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.LocalReduceMotion
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import com.velocity.impulse.ui.theme.accentOf

private enum class Tab(val glyph: Glyph) { Home(Glyph.Home), Chats(Glyph.Chat), Models(Glyph.Models), More(Glyph.More) }

private fun Tab.label() = when (this) {
    Tab.Home -> Lang.tr("Start", "Start")
    Tab.Chats -> Lang.tr("Chats", "Czaty")
    Tab.Models -> Lang.tr("Models", "Modele")
    Tab.More -> Lang.tr("More", "Więcej")
}

/** What sits on top of the tabs: a chat, or one of the pages under More. */
enum class Route { Tabs, Chat, Contexts, Settings, Computer }

@Composable
fun App(m: AppModel) {
    CompositionLocalProvider(LocalAccent provides accentOf(m.accent), LocalReduceMotion provides m.reduceMotion) {
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
                    renderEffect = if (Build.VERSION.SDK_INT >= 31 && p < 0.99f) {
                        val r = (1f - p) * 18f
                        RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP).asComposeRenderEffect()
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

@Composable
private fun Main(m: AppModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var route by rememberSaveable { mutableStateOf(Route.Tabs) }
    var models by remember { mutableStateOf(false) }
    val calm = LocalReduceMotion.current
    BackHandler(route == Route.Tabs && tab != Tab.Home) { tab = Tab.Home }

    val openChat: (String?) -> Unit = { id ->
        if (id == null) m.newChat() else m.open(id)
        route = Route.Chat
    }
    val back = { route = Route.Tabs }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(route, transitionSpec = { push(targetState != Route.Tabs, calm) }, label = "route") { r ->
            Depth(front = r != Route.Tabs, calm) {
                when (r) {
                    Route.Tabs -> Box(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize()) {
                            TopBar(m, onModels = { models = true }, onMe = { route = Route.Settings })
                            AnimatedContent(
                                tab,
                                Modifier.weight(1f),
                                transitionSpec = { pageMotion(initialState.ordinal, targetState.ordinal, calm) },
                                label = "tab",
                            ) { t ->
                                when (t) {
                                    Tab.Home -> HomeScreen(m, openChat, { tab = Tab.Models }, { route = Route.Contexts })
                                    Tab.Chats -> ChatsScreen(m, openChat)
                                    Tab.Models -> ModelsScreen(m)
                                    Tab.More -> MoreScreen(m) { route = it }
                                }
                            }
                        }
                        TabBar(tab, Modifier.align(Alignment.BottomCenter)) { tab = it }
                    }
                    Route.Chat -> ChatView(m, onBack = back, onModels = { models = true })
                    Route.Contexts -> Pushed(back) { ContextsScreen(m) }
                    Route.Settings -> Pushed(back) { SettingsScreen(m) }
                    Route.Computer -> Pushed(back) { ComputerScreen(m) }
                }
            }
        }
        ModelSheet(m, models) { models = false }
    }
}

/** A page pushed over the tabs: a back button, then the page with its own large title. */
@Composable
private fun Pushed(onBack: () -> Unit, content: @Composable () -> Unit) {
    BackHandler { onBack() }
    Column(Modifier.fillMaxSize().background(Tone.App)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Space.S3, vertical = Space.S2)) {
            IconBtn(Glyph.Left, onBack, fill = Color.Transparent)
        }
        Box(Modifier.weight(1f)) { content() }
    }
}

/**
 * Depth for a push: the page coming in turns toward you from its left edge as it slides, and the
 * page underneath shrinks back and turns away, so the two read as cards in space, not as a wipe.
 */
@Composable
private fun AnimatedContentScope.Depth(front: Boolean, calm: Boolean, content: @Composable () -> Unit) {
    val away by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = 0.9f, stiffness = 340f) },
        label = "depth",
    ) { if (it == EnterExitState.Visible) 0f else 1f }
    Box(
        Modifier.fillMaxSize().graphicsLayer {
            if (calm) return@graphicsLayer
            cameraDistance = 18f * density
            if (front) {
                rotationY = 16f * away
                transformOrigin = TransformOrigin(0f, 0.5f)
            } else {
                val s = 1f - 0.08f * away
                scaleX = s
                scaleY = s
                rotationY = -7f * away
                transformOrigin = TransformOrigin(1f, 0.5f)
                alpha = 1f - 0.35f * away
            }
        },
    ) { content() }
}

private fun push(entering: Boolean, calm: Boolean): ContentTransform {
    if (calm) return fadeIn(tween(Motion.M2)) togetherWith fadeOut(tween(Motion.M2))
    val glide = spring<IntOffset>(dampingRatio = 0.9f, stiffness = 340f)
    return if (entering) {
        slideInHorizontally(glide) { it } togetherWith slideOutHorizontally(glide) { -it / 5 }
    } else {
        slideInHorizontally(glide) { -it / 5 } togetherWith slideOutHorizontally(glide) { it }
    }
}

private fun pageMotion(from: Int, to: Int, calm: Boolean): ContentTransform {
    if (calm) return fadeIn(tween(Motion.M2)) togetherWith fadeOut(tween(Motion.M2))
    val dir = if (to > from) 1 else -1
    return (slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 380f)) { it / 6 * dir } + fadeIn(tween(Motion.M3, easing = Motion.Out))) togetherWith
        (slideOutHorizontally(tween(Motion.M3, easing = Motion.Standard)) { -it / 10 * dir } + fadeOut(tween(Motion.M2)))
}

/**
 * The floating island: four places, the highlight that can be dragged between them, and an icon
 * that turns over in 3D and fills in as it is chosen.
 */
@Composable
private fun TabBar(tab: Tab, modifier: Modifier, onTab: (Tab) -> Unit) {
    val accent = LocalAccent.current
    Box(modifier.navigationBarsPadding().padding(bottom = Space.S3), contentAlignment = Alignment.Center) {
        LiquidSlots(
            Tab.entries.size, tab.ordinal, { onTab(Tab.entries[it]) }, accent.copy(alpha = 0.16f), Shapes.Card,
            Modifier.widthIn(max = 380.dp).fillMaxWidth(0.86f).height(64.dp)
                .background(Tone.Float, Shapes.Sheet)
                .padding(6.dp),
        ) {
            Tab.entries.forEach { t ->
                val on = t == tab
                val ink by animateColorAsState(if (on) accent else Ink.Three, label = "ink")
                val turn by animateFloatAsState(if (on) 360f else 0f, spring(dampingRatio = 0.62f, stiffness = 170f), label = "turn")
                Column(
                    Modifier.weight(1f).fillMaxHeight().pressable({ onTab(t) }, depth = 0.88f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        t.glyph, ink,
                        Modifier.graphicsLayer {
                            rotationY = turn
                            cameraDistance = 10f * density
                        },
                        size = 22.dp, weight = if (on) Weight.Fill else Weight.Regular,
                    )
                    Spacer(Modifier.height(3.dp))
                    BasicText(t.label(), style = Type.Label.copy(color = ink, fontSize = 11.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium))
                }
            }
        }
    }
}

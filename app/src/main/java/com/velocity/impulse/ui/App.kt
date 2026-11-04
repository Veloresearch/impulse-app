package com.velocity.impulse.ui

import com.velocity.impulse.ui.screens.Details
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.zIndex
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Stage
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.components.Feel
import com.velocity.impulse.ui.components.FlightOverlay
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.IconBtn
import com.velocity.impulse.ui.components.MorphSheet
import com.velocity.impulse.ui.components.feel
import com.velocity.impulse.ui.components.glass
import com.velocity.impulse.ui.components.glassSource
import com.velocity.impulse.ui.components.morph
import com.velocity.impulse.ui.components.morphBody
import com.velocity.impulse.ui.components.rubber
import com.velocity.impulse.ui.screens.ComputerPage
import com.velocity.impulse.ui.screens.ConnectScreen
import com.velocity.impulse.ui.screens.ContextSheetBody
import com.velocity.impulse.ui.screens.Conversation
import com.velocity.impulse.ui.screens.Drawer
import com.velocity.impulse.ui.screens.ModelSheetBody
import com.velocity.impulse.ui.screens.PreviewHost
import com.velocity.impulse.ui.screens.SettingsPage
import com.velocity.impulse.ui.screens.SignInScreen
import com.velocity.impulse.ui.screens.TopIsland
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.LocalReduceMotion
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Radius
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import com.velocity.impulse.ui.theme.accentOf
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sign

@Composable
fun App(m: AppModel) {
    val view = LocalView.current
    val base = LocalDensity.current
    SideEffect { Feel.enabled = m.haptics }
    LaunchedEffect(m, view) { snapshotFlow { m.keepAwake && m.streaming }.collect { view.keepScreenOn = it } }
    DisposableEffect(view) { onDispose { view.keepScreenOn = false } }
    CompositionLocalProvider(
        LocalAccent provides accentOf(m.accent),
        LocalReduceMotion provides m.reduceMotion,
        LocalDensity provides Density(base.density, base.fontScale * m.textScale),
    ) {
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

/**
 * Four planes, back to front: the room with the drawer (Z0), the conversation card (Z1), pages
 * (Z2) and sheets (Z3). Whatever stands in front pushes the planes behind it back. The drawer
 * never slides over the conversation: the card slides aside and recedes while the drawer comes
 * forward out of the room.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalTransitionApi::class)
@Composable
private fun Main(m: AppModel) {
    val scope = rememberCoroutineScope()
    val shell = remember { Shell(m, scope) }
    shell.calm = LocalReduceMotion.current
    shell.pageTransition = rememberTransition(shell.pages, "page")
    shell.sheetTransition = rememberTransition(shell.sheets, "sheet")
    val pageDepth = shell.pageTransition.animateFloat({ Motion.morph() }, "pd") { if (it != null) 1f else 0f }
    val sheetDepth = shell.sheetTransition.animateFloat({ Motion.morph() }, "sd") { if (it != null) 1f else 0f }
    val depth = { max(pageDepth.value, sheetDepth.value) }

    SharedTransitionLayout(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalShell provides shell, LocalShared provides this) {
            BoxWithConstraints(Modifier.fillMaxSize().background(Tone.Void)) {
                val docked = maxWidth >= 720.dp
                shell.docked = docked
                shell.drawerWidth = min(maxWidth - 64.dp, 340.dp)
                if (docked) {
                    LaunchedEffect(Unit) { shell.drawer.snapTo(1f) }
                    Row(Modifier.fillMaxSize().recede(shell, depth)) {
                        Drawer(m, Modifier.width(shell.drawerWidth).fillMaxHeight())
                        Card(m, shell, Modifier.weight(1f).fillMaxHeight().background(Tone.App))
                    }
                } else {
                    Phone(m, shell, depth)
                }
                PageHost(Surface.Settings) { s, scroll -> SettingsPage(m, s, scroll) }
                PageHost(Surface.Computer) { s, scroll -> ComputerPage(m, s, scroll) }
                MorphSheet(Surface.Model) { ModelSheetBody(m) }
                MorphSheet(Surface.Context) { ContextSheetBody(m) }
                FlightOverlay(shell.flight)
                Backs(shell)
                PreviewHost()
            }
        }
    }
}

/** The phone layout: the drawer in the room and the card in front of it, moved by one gesture. */
@Composable
private fun Phone(m: AppModel, shell: Shell, depth: () -> Float) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val keyboard = LocalSoftwareKeyboardController.current
    val drawer = shell.drawer
    val wPx = with(LocalDensity.current) { shell.drawerWidth.toPx() }
    val offstage = (wPx * 2).roundToInt()
    val calm = shell.calm
    val facing by remember { derivedStateOf { sign(drawer.value) } }

    Box(
        Modifier.fillMaxSize()
            .recede(shell, depth)
            .sideSwipe(shell, wPx, scope, view) { keyboard?.hide() },
    ) {
        // The hidden side panel steps off stage so it never takes touches meant for the open one.
        Drawer(
            m,
            Modifier.zIndex(if (calm && facing > 0f) 1f else 0f)
                .width(shell.drawerWidth)
                .fillMaxHeight()
                .offset { IntOffset(if (drawer.value < 0f) -offstage else 0, 0) }
                .graphicsLayer {
                    val k = drawer.value.coerceIn(0f, 1f)
                    alpha = k
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                    if (calm) return@graphicsLayer
                    translationX = -(1f - k) * 0.28f * size.width
                    scaleX = 0.96f + 0.04f * k
                    scaleY = scaleX
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
                .then(if (calm) Modifier.background(Tone.Void) else Modifier),
        )
        Details(
            m,
            Modifier.align(Alignment.CenterEnd)
                .zIndex(if (calm && facing < 0f) 1f else 0f)
                .width(shell.drawerWidth)
                .fillMaxHeight()
                .offset { IntOffset(if (drawer.value > 0f) offstage else 0, 0) }
                .graphicsLayer {
                    val k = (-drawer.value).coerceIn(0f, 1f)
                    alpha = k
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                    if (calm) return@graphicsLayer
                    translationX = (1f - k) * 0.28f * size.width
                    scaleX = 0.96f + 0.04f * k
                    scaleY = scaleX
                    transformOrigin = TransformOrigin(1f, 0.5f)
                }
                .then(if (calm) Modifier.background(Tone.Void) else Modifier),
        )
        Card(
            m, shell,
            Modifier.fillMaxSize()
                .graphicsLayer {
                    val p = drawer.value
                    if (p == 0f || calm) return@graphicsLayer
                    val side = if (p > 0f) 1f else -1f
                    val k = abs(p).coerceAtMost(1f)
                    val s = 1f - 0.08f * k
                    transformOrigin = TransformOrigin(if (p > 0f) 1f else 0f, 0.5f)
                    scaleX = s
                    scaleY = s
                    translationX = p * (wPx + 12.dp.toPx()) - side * (1f - s) * size.width
                    rotationY = -7f * k * side
                    cameraDistance = 18f * density
                    shape = Moving
                    clip = true
                }
                .drawWithContent {
                    drawContent()
                    val p = abs(drawer.value).coerceAtMost(1f)
                    if (p > 0f) drawRect(Color.Black, alpha = 0.35f * p)
                }
                .background(Tone.App),
        )
    }
}

@Composable
private fun Card(m: AppModel, shell: Shell, modifier: Modifier) {
    val open by remember { derivedStateOf { shell.drawer.targetValue >= 1f || shell.drawer.targetValue <= -1f } }
    Box(modifier.onPlaced { shell.flight.card = it }) {
        Conversation(m, Modifier.fillMaxSize())
        TopIsland(m, Modifier.align(Alignment.TopCenter))
        // Swallows taps on the receded card; the root drag still closes it under the finger.
        if (open && !shell.docked) Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { shell.closeDrawer() } })
    }
}

/**
 * Clip for whole planes in motion. A squircle is a path, which older GPUs clip without
 * antialiasing and at extra cost on every frame; a rounded rectangle clips in hardware.
 */
private val Moving = RoundedCornerShape(Radius.Sheet)

/**
 * The one horizontal gesture that moves the drawer and the details. Lists, code blocks and pills
 * see every move first; the swipe claims a drag only once it is clearly sideways and gives it up
 * as soon as it turns out vertical, so scrolling always wins.
 */
private fun Modifier.sideSwipe(shell: Shell, wPx: Float, scope: CoroutineScope, view: View, onStart: () -> Unit) =
    pointerInput(shell, wPx) {
        val drawer = shell.drawer
        val tracker = VelocityTracker()
        fun low() = if (shell.m.current != null) -1f else 0f
        fun band(x: Float): Float {
            val lo = low()
            return when {
                x > 1f -> 1f + rubber(x - 1f, 0.04f)
                x < lo -> lo - rubber(lo - x, 0.04f)
                else -> x
            }
        }
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val free = shell.pages.currentState == null && shell.pages.targetState == null &&
                shell.sheets.currentState == null && shell.sheets.targetState == null
            if (!free) return@awaitEachGesture
            val slop = viewConfiguration.touchSlop
            var change = down
            while (true) {
                val e = awaitPointerEvent()
                change = e.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                if (!change.pressed || change.isConsumed) return@awaitEachGesture
                val d = change.position - down.position
                if (abs(d.x) > slop && abs(d.x) > 1.5f * abs(d.y)) break
                if (abs(d.y) > slop) return@awaitEachGesture
            }
            change.consume()
            onStart()
            var raw = drawer.value
            tracker.resetTracking()
            tracker.addPointerInputChange(change)
            val lifted = horizontalDrag(change.id) { c ->
                raw += c.positionChange().x / wPx
                tracker.addPointerInputChange(c)
                c.consume()
                val to = band(raw)
                scope.launch {
                    val old = drawer.value
                    drawer.snapTo(to)
                    if ((old - 0.5f) * (to - 0.5f) < 0f || (old + 0.5f) * (to + 0.5f) < 0f) view.feel(Feel.Tick)
                }
            }
            val vp = if (lifted) tracker.calculateVelocity().x / wPx else 0f
            val at = band(raw)
            val target = when {
                vp > 1.4f -> if (at < 0f) 0f else 1f
                vp < -1.4f -> if (at > 0f) 0f else low()
                at > 0.5f -> 1f
                at < -0.5f -> low()
                else -> 0f
            }
            scope.launch { drawer.animateTo(target, Motion.drawer(), initialVelocity = vp) }
        }
    }

/** Pushes the planes behind a page or sheet back: smaller, rounded, washed dark. */
private fun Modifier.recede(shell: Shell, amount: () -> Float) = graphicsLayer {
    val p = amount().coerceIn(0f, 1f)
    if (p == 0f || shell.calm) return@graphicsLayer
    scaleX = 1f - 0.06f * p
    scaleY = scaleX
    shape = Moving
    clip = true
}.drawWithContent {
    drawContent()
    val p = amount().coerceIn(0f, 1f)
    if (p > 0f) drawRect(Color.Black, alpha = 0.4f * p)
}

/** A page (Z2) growing out of the drawer row that opened it, with a frosted back island. */
@OptIn(ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun PageHost(surface: Surface, content: @Composable (AnimatedVisibilityScope, ScrollState) -> Unit) {
    val shell = LocalShell.current
    val calm = shell.calm
    val reach = with(LocalDensity.current) { 64.dp.toPx() }
    shell.pageTransition.AnimatedVisibility(
        { it == surface },
        enter = if (calm) fadeIn(tween(Motion.M2)) else EnterTransition.None,
        exit = if (calm) fadeOut(tween(Motion.M2)) else ExitTransition.None,
    ) {
        val page = this
        Box(Modifier.fillMaxSize().morph(shell.pageOrigin, page, Shapes.Sheet).background(Tone.App).morphBody()) {
            val haze = remember { HazeState() }
            val scroll = rememberScrollState()
            val titled by remember { derivedStateOf { scroll.value > reach } }
            Box(Modifier.fillMaxSize().glassSource(haze)) { content(page, scroll) }
            Row(Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp).height(44.dp)) {
                Box(Modifier.size(44.dp).glass(haze, CircleShape)) {
                    IconBtn(Glyph.Left, { shell.close(surface) }, fill = Color.Transparent, size = 44.dp, island = true)
                }
            }
            AnimatedVisibility(
                titled,
                Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(vertical = 8.dp),
                enter = fadeIn(tween(Motion.M1)),
                exit = fadeOut(tween(Motion.M1)),
            ) {
                Box(Modifier.height(44.dp).glass(haze, Shapes.Card).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    BasicText(
                        if (surface == Surface.Settings) tr("Settings", "Ustawienia") else tr("Computer", "Komputer"),
                        style = Type.Section,
                    )
                }
            }
        }
    }
}

/**
 * Back follows the thumb: it closes the drawer, a page or a sheet, whichever is in front. With
 * nothing open it is left to the system, so the back-to-home preview runs.
 */
@OptIn(ExperimentalTransitionApi::class)
@Composable
private fun Backs(shell: Shell) {
    val scope = rememberCoroutineScope()
    val drawer = shell.drawer
    val open by remember { derivedStateOf { drawer.targetValue >= 1f || drawer.targetValue <= -1f } }
    val bare = shell.pages.currentState == null && shell.sheets.currentState == null

    PredictiveBackHandler(open && bare && !(shell.docked && drawer.targetValue > 0f)) { progress ->
        val from = drawer.targetValue
        try {
            progress.collect { e -> if (!shell.calm) drawer.snapTo(from * (1f - e.progress)) }
            scope.launch { drawer.animateTo(if (shell.docked) 1f else 0f, Motion.drawer()) }
        } catch (e: CancellationException) {
            scope.launch { drawer.animateTo(from, Motion.drawer()) }
            throw e
        }
    }
    PredictiveBackHandler(shell.pages.currentState != null && shell.sheets.currentState == null) { progress ->
        try {
            progress.collect { e -> shell.pages.seekTo(e.progress, targetState = null) }
            scope.launch { shell.pages.animateTo(null) }
        } catch (e: CancellationException) {
            scope.launch { shell.pages.animateTo(shell.pages.currentState) }
            throw e
        }
    }
    PredictiveBackHandler(shell.sheets.currentState != null) { progress ->
        try {
            progress.collect { e -> shell.sheets.seekTo(e.progress, targetState = null) }
            scope.launch { shell.sheets.animateTo(null) }
        } catch (e: CancellationException) {
            scope.launch { shell.sheets.animateTo(shell.sheets.currentState) }
            throw e
        }
    }
}

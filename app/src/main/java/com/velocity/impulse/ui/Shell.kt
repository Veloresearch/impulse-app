package com.velocity.impulse.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.ui.components.Flight
import com.velocity.impulse.ui.theme.Motion
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** What can stand in front of the conversation: pages (Z2) and sheets (Z3). */
enum class Surface(val page: Boolean) { Settings(true), Computer(true), Model(false), Context(false) }

object Chrome {
    val Side = 12.dp
    val TopIsland = 52.dp

    /** The top island row below the status bar: 8 + 52 + 8. */
    val Top = 68.dp
}

/**
 * The shell's moving parts: how far the drawer is open, which page and sheet stand in front of
 * the conversation and where each grew from, and the scene counter that choreographs chat
 * switches.
 */
@OptIn(ExperimentalTransitionApi::class)
@Stable
class Shell(val m: AppModel, private val scope: CoroutineScope) {
    /** 1 the chats on the left are open, -1 the details on the right, 0 neither; drags rubber-band past. */
    val drawer = Animatable(0f)
    var drawerWidth by mutableStateOf(300.dp)
        internal set

    /** Wide screens (720dp and up) keep the drawer open beside the conversation. */
    var docked by mutableStateOf(false)
        internal set
    var calm by mutableStateOf(false)
        internal set

    val pages = SeekableTransitionState<Surface?>(null)
    val sheets = SeekableTransitionState<Surface?>(null)
    lateinit var pageTransition: Transition<Surface?>
        internal set
    lateinit var sheetTransition: Transition<Surface?>
        internal set
    var pageOrigin by mutableStateOf("")
        private set
    var sheetOrigin by mutableStateOf("")
        private set

    /** Bumps only when another chat or a new one takes the stage, never on send. */
    var scene by mutableIntStateOf(0)
        private set

    /** +1 for a newer or new chat, -1 for an older one. */
    var sceneDir by mutableIntStateOf(1)
        private set

    /** Bumps when a blocked action points the eye at the model pill. */
    var nudge by mutableIntStateOf(0)
        private set

    val flight = Flight()
    val cardHaze = HazeState()

    internal fun originOf(s: Surface) = if (s.page) pageOrigin else sheetOrigin

    fun openDrawer() {
        scope.launch { drawer.animateTo(1f, Motion.drawer()) }
    }

    fun openDetails() {
        if (m.current != null) scope.launch { drawer.animateTo(-1f, Motion.drawer()) }
    }

    fun closeDrawer() {
        if (docked && drawer.value > 0f) return
        scope.launch { drawer.animateTo(if (docked) 1f else 0f, Motion.drawer()) }
    }

    fun openChat(id: String, title: String, from: Rect? = null) {
        if (id == m.current) {
            closeDrawer()
            return
        }
        val current = m.chats.indexOfFirst { it.id == m.current }
        val next = m.chats.indexOfFirst { it.id == id }
        sceneDir = if (current >= 0 && next < current) 1 else -1
        if (from != null && !calm && !docked) scope.launch { flight.fly(id, title, from) }
        m.open(id)
        scene++
        closeDrawer()
    }

    fun newChat(closeDrawer: Boolean = true) {
        m.newChat()
        sceneDir = 1
        scene++
        if (closeDrawer) this.closeDrawer()
    }

    fun open(s: Surface, origin: String) {
        if (s.page) {
            pageOrigin = origin
            scope.launch { pages.animateTo(s) }
        } else {
            sheetOrigin = origin
            scope.launch { sheets.animateTo(s) }
        }
    }

    fun close(s: Surface) {
        scope.launch { (if (s.page) pages else sheets).animateTo(null) }
    }

    /** After a pick: the sheet and the drawer leave together. */
    fun done() {
        scope.launch { sheets.animateTo(null) }
        closeDrawer()
    }

    fun nudgeModel() {
        nudge++
    }
}

val LocalShell = staticCompositionLocalOf<Shell> { error("Shell lives in Main") }

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalShared = staticCompositionLocalOf<SharedTransitionScope?> { null }

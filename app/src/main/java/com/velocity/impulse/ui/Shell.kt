package com.velocity.impulse.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.ui.components.Flight
import com.velocity.impulse.ui.components.Presentation
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Radius
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** What can stand in front of the conversation: pages (Z2) and sheets (Z3). */
enum class Surface(val page: Boolean) { Settings(true), Computer(true), Models(true), Contexts(true), Model(false), Context(false) }

object Chrome {
    val Side = 12.dp
    val TopIsland = 52.dp

    /** The top island row below the status bar: 8 + 52 + 8. */
    val Top = 68.dp

    /** Controls are capsules; icon buttons are circles. */
    val Capsule = RoundedCornerShape(percent = 50)

    /** A section's rows share one container; a rounded rectangle clips in hardware on every phone. */
    val Group = RoundedCornerShape(Radius.Card)
}

/** A text button in the one control shape. */
@Composable
fun Pill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ink: Color = Ink.One,
    fill: Color = Tone.Raise,
    enabled: Boolean = true,
    height: Dp = 40.dp,
) {
    val pressed = when (fill) {
        Color.Transparent -> Tone.Raise
        Tone.Raise -> Tone.Press
        else -> fill.copy(alpha = 0.82f)
    }
    Box(
        modifier.height(height)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .pressable(onClick, Chrome.Capsule, fill, pressed, depth = 0.94f, enabled = enabled, stretch = true)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = Type.Body.copy(color = ink, fontWeight = FontWeight.SemiBold), maxLines = 1)
    }
}

/**
 * The shell's moving parts: how far the drawer is open, which page and sheet stand in front of
 * the conversation, and the scene counter that choreographs chat switches.
 */
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

    val page = Presentation<Surface>(scope, covers = true)
    val sheet = Presentation<Surface>(scope)

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

    fun presentation(s: Surface) = if (s.page) page else sheet

    /** Nothing stands in front of the conversation, not even on its way out. */
    val bare get() = page.item == null && sheet.item == null

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

    fun newChat(closeDrawer: Boolean = true, temporary: Boolean = false) {
        m.newChat(temporary)
        sceneDir = 1
        scene++
        if (closeDrawer) this.closeDrawer()
    }

    /** Opens [s] out of the element marked with [origin]. */
    fun open(s: Surface, origin: String) = presentation(s).open(s, origin)

    fun close(s: Surface) {
        val p = presentation(s)
        if (p.item == s) p.close()
    }

    /** After a pick: the sheet and the drawer leave together. */
    fun done() {
        sheet.close()
        closeDrawer()
    }

    fun nudgeModel() {
        nudge++
    }
}

val LocalShell = staticCompositionLocalOf<Shell> { error("Shell lives in Main") }

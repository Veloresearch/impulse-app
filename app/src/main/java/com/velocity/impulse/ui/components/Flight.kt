package com.velocity.impulse.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.velocity.impulse.ui.theme.Type

/** A chat title flying from its drawer row into the top island while the conversation returns. */
@Stable
class Flight {
    var id by mutableStateOf<String?>(null)
        private set
    var text = ""
        private set
    internal var from = Rect.Zero
    internal var to = Rect.Zero
    val p = Animatable(0f)
    internal var card: LayoutCoordinates? = null
    internal var landing: LayoutCoordinates? = null

    /** [from] is in root coordinates. */
    suspend fun fly(id: String, text: String, from: Rect) {
        val card = card?.takeIf { it.isAttached } ?: return
        val landing = landing?.takeIf { it.isAttached } ?: return
        // The card rests at the root origin, so this is where the title lands once the card is back.
        to = card.localBoundingBoxOf(landing)
        this.from = from
        this.text = text
        p.snapTo(0f)
        this.id = id
        p.animateTo(1f, spring(1f, 400f))
        this.id = null
    }
}

/** On the island's title slot: where a flight lands, hidden while one is in the air. */
fun Modifier.flightLanding(flight: Flight): Modifier =
    onPlaced { flight.landing = it }.graphicsLayer { alpha = if (flight.id != null) 0f else 1f }

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

@Composable
fun FlightOverlay(flight: Flight) {
    if (flight.id == null) return
    val box = remember { FloatArray(2) }
    val maxWidth = with(LocalDensity.current) { flight.from.width.toDp() }
    BasicText(
        flight.text,
        Modifier.widthIn(max = maxWidth)
            .onSizeChanged { box[0] = it.width.toFloat(); box[1] = it.height.toFloat() }
            .graphicsLayer {
                val t = flight.p.value
                val from = flight.from
                val to = flight.to
                translationX = lerp(from.left, to.center.x - box[0] / 2, t)
                translationY = lerp(from.center.y - box[1] / 2, to.center.y - box[1] / 2, t)
            },
        style = Type.Body.copy(fontWeight = FontWeight.SemiBold),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

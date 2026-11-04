package com.velocity.impulse.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontVariation
import com.velocity.impulse.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * One theme's colours. [frost] is the glass tint, [shade] scales island shadows, [aura] is how
 * strongly the conversation backdrop glows.
 */
@Immutable
data class Palette(
    val void: Color, val app: Color, val work: Color, val panel: Color, val raise: Color,
    val hover: Color, val press: Color, val float: Color, val inset: Color,
    val one: Color, val two: Color, val three: Color, val four: Color,
    val ok: Color, val warn: Color, val err: Color,
    val frost: Color, val shade: Float, val aura: Float, val light: Boolean,
)

object Theme {
    val Dark = Palette(
        void = Color(0xFF161419), app = Color(0xFF1C1A1F), work = Color(0xFF211F25), panel = Color(0xFF26242B),
        raise = Color(0xFF2D2B32), hover = Color(0xFF343139), press = Color(0xFF3B3841), float = Color(0xFF2A282F),
        inset = Color(0xFF141317),
        one = Color(0xFFF1EFF3), two = Color(0xFFC9C1D1), three = Color(0xFF9C93A6), four = Color(0xFF716A79),
        ok = Color(0xFF7FD1A0), warn = Color(0xFFE8C170), err = Color(0xFFF07A7A),
        frost = Color(0x9E2A282F), shade = 1f, aura = 0.13f, light = false,
    )

    val Light = Palette(
        void = Color(0xFFEAE6DF), app = Color(0xFFF6F3EE), work = Color(0xFFF1EDE7), panel = Color(0xFFFFFDFA),
        raise = Color(0xFFEBE6DF), hover = Color(0xFFE3DED6), press = Color(0xFFD9D3CA), float = Color(0xFFFFFDFA),
        inset = Color(0xFFEDE9E3),
        one = Color(0xFF1E1C21), two = Color(0xFF45414A), three = Color(0xFF6D6774), four = Color(0xFF99939F),
        ok = Color(0xFF2E8F5C), warn = Color(0xFFA9741A), err = Color(0xFFC93A3A),
        frost = Color(0xA8FFFFFF), shade = 0.5f, aura = 0.16f, light = true,
    )

    val Amoled = Palette(
        void = Color(0xFF000000), app = Color(0xFF000000), work = Color(0xFF060607), panel = Color(0xFF0F0E11),
        raise = Color(0xFF18171B), hover = Color(0xFF201E23), press = Color(0xFF28262C), float = Color(0xFF131215),
        inset = Color(0xFF09090A),
        one = Color(0xFFF1EFF3), two = Color(0xFFC9C1D1), three = Color(0xFF9C93A6), four = Color(0xFF716A79),
        ok = Color(0xFF7FD1A0), warn = Color(0xFFE8C170), err = Color(0xFFF07A7A),
        frost = Color(0xA8141316), shade = 1f, aura = 0.09f, light = false,
    )

    var palette by mutableStateOf(Dark)

    fun of(name: String, systemDark: Boolean) = when (name) {
        "light" -> Light
        "amoled" -> Amoled
        "auto" -> if (systemDark) Dark else Light
        else -> Dark
    }
}

/** Keeps [Theme.palette] on the theme called [name]. It switches in one frame: a fade would recompose every screen on every frame. */
@Composable
fun FollowTheme(name: String) {
    val target = Theme.of(name, isSystemInDarkTheme())
    remember(target) { Theme.palette = target }
}

object Tone {
    val Void get() = Theme.palette.void
    val App get() = Theme.palette.app
    val Work get() = Theme.palette.work
    val Panel get() = Theme.palette.panel
    val Raise get() = Theme.palette.raise
    val Hover get() = Theme.palette.hover
    val Press get() = Theme.palette.press
    val Float get() = Theme.palette.float
    val Inset get() = Theme.palette.inset
}

object Ink {
    val One get() = Theme.palette.one
    val Two get() = Theme.palette.two
    val Three get() = Theme.palette.three
    val Four get() = Theme.palette.four
}

object Signal {
    val Ok get() = Theme.palette.ok
    val Warn get() = Theme.palette.warn
    val Err get() = Theme.palette.err
}

/** The panel's accents, by the names it stores them under. */
val Accents = linkedMapOf(
    "velocity" to Color(0xFFB3A3C9),
    "ice" to Color(0xFF7AA2F7),
    "amber" to Color(0xFFE8BD72),
    "mint" to Color(0xFF7FD4AC),
    "violet" to Color(0xFFAB9AF0),
    "rose" to Color(0xFFEB9AA0),
    "sand" to Color(0xFFCFC3AC),
    "mono" to Color(0xFFE8E8E6),
)

fun accentOf(name: String) = Accents[name] ?: Accents.getValue("velocity")

val LocalAccent = compositionLocalOf { Accents.getValue("velocity") }
val LocalReduceMotion = compositionLocalOf { false }

object Radius {
    val Chip = 6.dp
    val ControlSm = 8.dp
    val Control = 10.dp
    val ControlLg = 12.dp
    val Tile = 14.dp
    val Card = 18.dp
    val Panel = 22.dp
    val Sheet = 26.dp
}

object Space {
    val S1 = 4.dp
    val S2 = 8.dp
    val S3 = 12.dp
    val S4 = 16.dp
    val S5 = 24.dp
    val S6 = 32.dp
    val S7 = 48.dp
}

object Motion {
    const val M1 = 120
    const val M2 = 220
    const val M3 = 340
    const val M4 = 520
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Out = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val Sheet = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    val Leave = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    fun <T> press() = spring<T>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
    fun <T> travel() = spring<T>(dampingRatio = 0.82f, stiffness = 420f)
    fun <T> settle() = spring<T>(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow)
    fun <T> drawer() = spring<T>(dampingRatio = 0.92f, stiffness = 420f)
    fun <T> morph() = spring<T>(dampingRatio = 0.9f, stiffness = 380f)

    /** Moving text never overshoots. */
    fun <T> calmText() = spring<T>(dampingRatio = 1f, stiffness = 600f)
}

private val Weights = listOf(300, 400, 500, 600)

@OptIn(ExperimentalTextApi::class)
/** Inter with its optical size fixed at [opsz]: 14 is the text cut, 32 the display cut. */
private fun inter(opsz: Float) = FontFamily(
    Weights.map { w ->
        Font(
            R.font.inter,
            FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w), FontVariation.Setting("opsz", opsz)),
        )
    },
)

val InterText = inter(14f)
private val InterTitle = inter(22f)
private val InterDisplay = inter(32f)
@OptIn(ExperimentalTextApi::class)
val JetBrainsMono = FontFamily(
    listOf(400, 500, 600).map { w -> Font(R.font.jetbrains_mono, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w))) },
)

/** Text styles take their colour from the palette on every read, so a stored copy keeps the old theme. */
object Type {
    private val base = TextStyle(fontSynthesis = FontSynthesis.None)
    private val display = base.copy(fontFamily = InterDisplay, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.026).em, lineHeight = 34.sp)
    private val hero = base.copy(fontFamily = InterDisplay, fontSize = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.03).em)
    private val wordmark = base.copy(fontFamily = InterDisplay, fontSize = 44.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.035).em)
    private val title = base.copy(fontFamily = InterTitle, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.02).em, lineHeight = 28.sp)
    private val section = base.copy(fontFamily = InterText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.014).em)
    private val body = base.copy(fontFamily = InterText, fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 23.sp)
    private val meta = base.copy(fontFamily = InterText, fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp)
    private val label = base.copy(fontFamily = InterText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    private val mono = base.copy(fontFamily = JetBrainsMono, fontSize = 13.sp, lineHeight = 19.sp)

    val Display get() = display.copy(color = Ink.One)
    val Hero get() = hero.copy(color = Ink.One)
    val Brand get() = wordmark.copy(color = Ink.One)
    val Title get() = title.copy(color = Ink.One)
    val Section get() = section.copy(color = Ink.One)
    val Body get() = body.copy(color = Ink.One)
    val Meta get() = meta.copy(color = Ink.Three)
    val Label get() = label.copy(color = Ink.Three)
    val Mono get() = mono.copy(color = Ink.Two)

    /** The wordmark face at [size]; tracking loosens as it shrinks so small titles stay legible. */
    fun brand(size: TextUnit): TextStyle {
        val t = ((size.value - 16f) / 28f).coerceIn(0f, 1f)
        return Brand.copy(fontSize = size, lineHeight = size * 1.15f, letterSpacing = (-(0.012f + 0.023f * t)).em)
    }

    val BrandHero get() = brand(38.sp)
    val BrandTitle get() = brand(30.sp)
    val BrandSection get() = brand(22.sp)
}

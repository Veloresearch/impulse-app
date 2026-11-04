package com.velocity.impulse.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontVariation
import com.velocity.impulse.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

object Tone {
    val Void = Color(0xFF161419)
    val App = Color(0xFF1C1A1F)
    val Work = Color(0xFF211F25)
    val Panel = Color(0xFF26242B)
    val Raise = Color(0xFF2D2B32)
    val Hover = Color(0xFF343139)
    val Press = Color(0xFF3B3841)
    val Float = Color(0xFF2A282F)
    val Inset = Color(0xFF141317)
}

object Ink {
    val One = Color(0xFFF1EFF3)
    val Two = Color(0xFFC9C1D1)
    val Three = Color(0xFF9C93A6)
    val Four = Color(0xFF716A79)
}

object Signal {
    val Ok = Color(0xFF7FD1A0)
    val Warn = Color(0xFFE8C170)
    val Err = Color(0xFFF07A7A)
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

object Type {
    private val base = TextStyle(color = Ink.One, fontSynthesis = FontSynthesis.None)
    val Display = base.copy(fontFamily = InterDisplay, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.026).em, lineHeight = 34.sp)
    val Hero = base.copy(fontFamily = InterDisplay, fontSize = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.03).em)
    val Brand = base.copy(fontFamily = InterDisplay, fontSize = 44.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.035).em)
    val Title = base.copy(fontFamily = InterTitle, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.02).em, lineHeight = 28.sp)
    val Section = base.copy(fontFamily = InterText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.014).em)
    val Body = base.copy(fontFamily = InterText, fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 23.sp)
    val Meta = base.copy(fontFamily = InterText, fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp, color = Ink.Three)
    val Label = base.copy(fontFamily = InterText, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Ink.Three)
    val Mono = base.copy(fontFamily = JetBrainsMono, fontSize = 13.sp, lineHeight = 19.sp, color = Ink.Two)
}

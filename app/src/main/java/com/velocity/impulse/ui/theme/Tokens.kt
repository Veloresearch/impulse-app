package com.velocity.impulse.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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

val Accents = listOf(
    Color(0xFF7AA2F7),
    Color(0xFFF0A96A),
    Color(0xFF9D8CF2),
    Color(0xFF6CC9B4),
    Color(0xFFF08AB4),
)

val LocalAccent = compositionLocalOf { Accents[0] }
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
}

private val Sans = FontFamily.Default

object Type {
    val Display = TextStyle(fontFamily = Sans, fontSize = 30.sp, fontWeight = FontWeight(680), letterSpacing = (-0.026).em, color = Ink.One)
    val Hero = TextStyle(fontFamily = Sans, fontSize = 34.sp, fontWeight = FontWeight(680), letterSpacing = (-0.03).em, color = Ink.One)
    val Title = TextStyle(fontFamily = Sans, fontSize = 22.sp, fontWeight = FontWeight(650), letterSpacing = (-0.02).em, color = Ink.One)
    val Section = TextStyle(fontFamily = Sans, fontSize = 17.sp, fontWeight = FontWeight(640), letterSpacing = (-0.014).em, color = Ink.One)
    val Body = TextStyle(fontFamily = Sans, fontSize = 15.sp, fontWeight = FontWeight(450), lineHeight = 23.sp, color = Ink.One)
    val Meta = TextStyle(fontFamily = Sans, fontSize = 13.sp, fontWeight = FontWeight(450), lineHeight = 18.sp, color = Ink.Three)
    val Label = TextStyle(fontFamily = Sans, fontSize = 12.sp, fontWeight = FontWeight(550), color = Ink.Three)
    val Mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp, color = Ink.Two)
}

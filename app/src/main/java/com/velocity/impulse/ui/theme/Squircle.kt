package com.velocity.impulse.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/** A rectangle with continuous (superellipse) corners, the shape every surface in Impulse uses. */
class Squircle(private val radius: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = min(with(density) { radius.toPx() } * 1.25f, min(size.width, size.height) / 2f)
        val w = size.width
        val h = size.height
        val path = Path()
        var first = true

        fun corner(cx: Float, cy: Float, sx: Float, sy: Float, from: Float, to: Float) {
            for (i in 0..STEPS) {
                val t = from + (to - from) * i / STEPS
                val c = cos(t)
                val s = sin(t)
                val x = cx + sx * r * abs(c).pow(EXP) * c.sign
                val y = cy + sy * r * abs(s).pow(EXP) * s.sign
                if (first) path.moveTo(x, y).also { first = false } else path.lineTo(x, y)
            }
        }

        val q = (PI / 2).toFloat()
        corner(r, r, -1f, -1f, 0f, q)
        corner(w - r, r, 1f, -1f, q, 0f)
        corner(w - r, h - r, 1f, 1f, 0f, q)
        corner(r, h - r, -1f, 1f, q, 0f)
        path.close()
        return Outline.Generic(path)
    }

    override fun equals(other: Any?) = other is Squircle && other.radius == radius
    override fun hashCode() = radius.hashCode()

    private companion object {
        const val STEPS = 14
        const val EXP = 2f / 3.4f
    }
}

object Shapes {
    val Chip = Squircle(Radius.Chip)
    val ControlSm = Squircle(Radius.ControlSm)
    val Control = Squircle(Radius.Control)
    val ControlLg = Squircle(Radius.ControlLg)
    val Tile = Squircle(Radius.Tile)
    val Card = Squircle(Radius.Card)
    val Panel = Squircle(Radius.Panel)
    val Sheet = Squircle(Radius.Sheet)
}

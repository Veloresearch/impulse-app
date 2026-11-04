package com.velocity.impulse.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.velocity.impulse.ui.theme.Tone

private data class Spec(val id: String, val body: String, val bg: Long, val eye: String = "dot", val mouth: String = "smile", val acc: String? = null)

private val K = Color(0xFF1F1F22)
private val Bodies = mapOf(
    "orange" to listOf(0xFFE3A273, 0xFFF3C7A0, 0xFFC27C4C),
    "lilac" to listOf(0xFFCDBFE0, 0xFFE7DFF1, 0xFFA794C3),
    "cream" to listOf(0xFFF3ECE2, 0xFFFFFFFF, 0xFFD6CBBC),
    "plum" to listOf(0xFF8A75A2, 0xFFA592BC, 0xFF68557E),
    "ink" to listOf(0xFF33333A, 0xFF46464E, 0xFF222226),
    "peach" to listOf(0xFFF0B79A, 0xFFF9D7C5, 0xFFD2906F),
)

private val Tofus = listOf(
    Spec("classic", "orange", 0xFFF3ECE2),
    Spec("sly", "lilac", 0xFF6E5A7E, "sly", "smirk"),
    Spec("cool", "peach", 0xFF2C2C31, "shades", "smirk"),
    Spec("nerd", "cream", 0xFFCDBFE0, "specs"),
    Spec("sleepy", "lilac", 0xFFF3ECE2, "sleep", acc = "zz"),
    Spec("wink", "orange", 0xFFCDBFE0, "wink", "grin"),
    Spec("dj", "cream", 0xFFD99A6C, "happy", "grin", "phones"),
    Spec("royal", "plum", 0xFFF3ECE2, acc = "crown"),
    Spec("kitty", "peach", 0xFF6E5A7E, acc = "ears"),
    Spec("sprout", "cream", 0xFF2C2C31, mouth = "o", acc = "sprout"),
    Spec("midnight", "ink", 0xFFCDBFE0, acc = "antenna"),
    Spec("artist", "lilac", 0xFFD99A6C, "happy", acc = "beret"),
)

/** The tofu the panel shows for this profile: the chosen one, or the one its name picks. */
private fun specFor(name: String, chosen: String?): Spec {
    Tofus.firstOrNull { it.id == chosen }?.let { return it }
    var h = 0L
    for (c in name.trim().lowercase()) h = (h * 31 + c.code) and 0xFFFFFFFFL
    return Tofus[(h % Tofus.size).toInt()]
}

private fun pixels(t: Spec): List<Triple<Int, Int, Color>> {
    val out = ArrayList<Triple<Int, Int, Color>>()
    val (b, top, side) = Bodies.getValue(t.body).map { Color(it) }
    val e = if (t.body == "ink") Color(0xFFF4EFE8) else K
    fun put(x: Int, y: Int, c: Color) = out.add(Triple(x, y, c))
    fun all(vararg l: Any) {
        var i = 0
        while (i < l.size) {
            val x = l[i] as Int
            val y = l[i + 1] as Int
            val c = l.getOrNull(i + 2) as? Color
            if (c != null) {
                put(x, y, c); i += 3
            } else {
                put(x, y, e); i += 2
            }
        }
    }
    for (y in 4..13) for (x in 2..13) if (!((x == 2 || x == 13) && (y == 4 || y == 13))) put(x, y, b)
    for (x in 3..12) put(x, 4, top)
    for (x in 3..5) put(x, 5, top)
    for (y in 5..12) put(13, y, side)
    for (x in 3..12) put(x, 13, side)
    when (t.eye) {
        "dot" -> all(5, 8, 5, 9, 10, 8, 10, 9)
        "wink" -> all(4, 9, 5, 9, 6, 9, 10, 8, 10, 9)
        "sleep" -> all(4, 9, 5, 9, 10, 9, 11, 9)
        "sly" -> all(4, 8, 5, 8, 6, 8, 5, 9, 9, 8, 10, 8, 11, 8, 10, 9)
        "happy" -> all(4, 9, 5, 8, 6, 9, 9, 9, 10, 8, 11, 9)
        "shades" -> {
            for (x in 3..12) put(x, 8, K)
            val g = Color(0xFF77777F)
            all(4, 9, K, 5, 9, K, 6, 9, K, 9, 9, K, 10, 9, K, 11, 9, K, 4, 8, g, 9, 8, g)
        }
        "specs" -> {
            for (x0 in listOf(3, 9)) {
                for (x in x0..x0 + 3) {
                    put(x, 7, K); put(x, 10, K)
                }
                all(x0, 8, K, x0, 9, K, x0 + 3, 8, K, x0 + 3, 9, K)
            }
            all(7, 8, K, 8, 8, K, 5, 9, 10, 9)
        }
    }
    when (t.mouth) {
        "smile" -> all(7, 11, 8, 11)
        "grin" -> all(6, 11, 7, 12, 8, 12, 9, 11)
        "smirk" -> all(8, 11, 9, 11, 10, 10)
        "o" -> all(7, 11, 8, 11, 7, 12, 8, 12)
    }
    val plum = Color(0xFF6E5A7E)
    when (t.acc) {
        "phones" -> {
            for (x in 3..12) put(x, 2, K)
            all(2, 3, K, 13, 3, K)
            for (y in 7..10) for (x in listOf(1, 2, 13, 14)) put(x, y, plum)
        }
        "crown" -> {
            val d = Color(0xFFE0A93A)
            val l = Color(0xFFF2C14E)
            for (x in 4..11) {
                put(x, 3, d); put(x, 2, l)
            }
            all(4, 1, l, 7, 1, l, 8, 1, l, 11, 1, l, 7, 0, l, 8, 0, l)
        }
        "ears" -> all(3, 1, b, 3, 2, b, 4, 2, b, 3, 3, b, 4, 3, b, 5, 3, b, 12, 1, b, 12, 2, b, 11, 2, b, 12, 3, b, 11, 3, b, 10, 3, b)
        "sprout" -> {
            val g1 = Color(0xFF5E9E5A)
            val g2 = Color(0xFF7CC277)
            all(8, 3, g1, 8, 2, g1, 7, 1, g2, 6, 1, g2, 9, 1, g1, 10, 0, g1)
        }
        "zz" -> all(12, 0, plum, 13, 0, plum, 13, 1, plum, 12, 2, plum, 13, 2, plum)
        "antenna" -> {
            val w = Color(0xFFF4EFE8)
            val o = Color(0xFFD99A6C)
            all(8, 3, w, 8, 2, w, 8, 1, o, 7, 1, o, 8, 0, Color(0xFFF3C7A0))
        }
        "beret" -> {
            for (x in 4..12) put(x, 3, plum)
            for (x in 5..11) put(x, 2, plum)
            put(8, 1, plum)
        }
    }
    return out
}

private fun emojiOf(id: String?): String? {
    if (id == null || !id.startsWith("e-")) return null
    val cps = id.drop(2).split('-').mapNotNull { it.toIntOrNull(16) }
    return if (cps.isEmpty()) null else String(cps.toIntArray(), 0, cps.size)
}

/** The profile's picture as the panel draws it: a photo, an emoji, or a tofu in pixels. */
@Composable
fun Avatar(name: String, avatar: String?, photo: String?, size: Dp, modifier: Modifier = Modifier) {
    val image = remember(photo) {
        photo?.substringAfter("base64,", "")?.takeIf { it.isNotEmpty() }?.let {
            runCatching { Base64.decode(it, Base64.DEFAULT).let { b -> BitmapFactory.decodeByteArray(b, 0, b.size)?.asImageBitmap() } }.getOrNull()
        }
    }
    val emoji = remember(avatar) { emojiOf(avatar) }
    val spec = remember(name, avatar) { specFor(name, avatar) }
    val px = remember(spec) { pixels(spec) }
    Box(modifier.size(size).clip(CircleShape), contentAlignment = Alignment.Center) {
        when {
            image != null -> Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            emoji != null -> Box(Modifier.fillMaxSize().background(Tone.Raise), contentAlignment = Alignment.Center) {
                BasicText(emoji, style = androidx.compose.ui.text.TextStyle(fontSize = (size.value * 0.5f).sp))
            }
            else -> Canvas(Modifier.fillMaxSize().background(Color(spec.bg))) {
                val cell = this.size.width / 20f
                val top = (this.size.height - 19f * cell) / 2f
                for ((x, y, c) in px) drawRect(c, Offset((x + 2f) * cell, top + (y + 1.5f) * cell), Size(cell * 1.03f, cell * 1.03f))
            }
        }
    }
}

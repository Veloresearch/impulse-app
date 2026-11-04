package com.velocity.impulse.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Squircle
import com.velocity.impulse.ui.theme.Type

/** Who publishes a family of models, its letter, and a colour near its own. Same table as the panel. */
private class Family(val test: Regex, val org: String, val mark: String, val tint: Long)

/** Order matters: a distill names its teacher before its base, and a Bonsai is a Qwen underneath. */
private val Families = listOf(
    Family(Regex("bonsai|prism", RegexOption.IGNORE_CASE), "prism-ml", "B", 0xFF3F7A58),
    Family(Regex("deepseek", RegexOption.IGNORE_CASE), "deepseek-ai", "D", 0xFF4D63E8),
    Family(Regex("qwen|qwq", RegexOption.IGNORE_CASE), "Qwen", "Q", 0xFF5B55D6),
    Family(Regex("gemma", RegexOption.IGNORE_CASE), "google", "G", 0xFF3A73D0),
    Family(Regex("llama", RegexOption.IGNORE_CASE), "meta-llama", "L", 0xFF2560C4),
    Family(Regex("mi[sx]tral|ministral|magistral|devstral|codestral", RegexOption.IGNORE_CASE), "mistralai", "M", 0xFFC4552C),
    Family(Regex("(^|[^a-z])phi", RegexOption.IGNORE_CASE), "microsoft", "P", 0xFF1F7FB8),
    Family(Regex("gpt-?oss", RegexOption.IGNORE_CASE), "openai", "O", 0xFF3A3640),
    Family(Regex("granite", RegexOption.IGNORE_CASE), "ibm-granite", "G", 0xFF3358C4),
    Family(Regex("smollm", RegexOption.IGNORE_CASE), "HuggingFaceTB", "S", 0xFFB07A12),
    Family(Regex("glm", RegexOption.IGNORE_CASE), "zai-org", "Z", 0xFF33357A),
    Family(Regex("kimi", RegexOption.IGNORE_CASE), "moonshotai", "K", 0xFF2E2C33),
    Family(Regex("olmo", RegexOption.IGNORE_CASE), "allenai", "A", 0xFFC23F6F),
    Family(Regex("nemotron", RegexOption.IGNORE_CASE), "nvidia", "N", 0xFF5A8F17),
    Family(Regex("lfm", RegexOption.IGNORE_CASE), "LiquidAI", "L", 0xFF5A43C9),
)

private fun familyOf(name: String, arch: String) = "$name $arch".let { s -> Families.firstOrNull { it.test.containsMatchIn(s) } }

private val Brand = Color(0xFF6B5B95)

private fun decode(uri: String): ImageBitmap? = runCatching {
    val bytes = Base64.decode(uri.substringAfter("base64,"), Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}.getOrNull()

/**
 * A model's face as the panel draws it: its publisher's picture from Hugging Face (kept by the
 * service, asked for once per publisher), or until then the family's letter on the family's colour.
 */
@Composable
fun ModelFace(m: AppModel, name: String, arch: String, size: Dp, modifier: Modifier = Modifier) {
    val family = remember(name, arch) { familyOf(name, arch) }
    val org = family?.org
    LaunchedEffect(org) { if (org != null) m.publisher(org) }
    val picture = org?.let { m.publishers[it] }
    val bitmap = remember(picture) { picture?.let(::decode) }
    val shape = Squircle(size * 0.22f)
    Box(modifier.size(size).clip(shape), contentAlignment = Alignment.Center) {
        Crossfade(bitmap, animationSpec = tween(Motion.M3), label = "face") { b ->
            if (b != null) {
                Image(b, null, Modifier.fillMaxSize().background(Color(0xFFF1EFF3)), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(family?.let { Color(it.tint) } ?: Brand), contentAlignment = Alignment.Center) {
                    BasicText(
                        family?.mark ?: name.trim().take(1).uppercase(),
                        style = Type.Body.copy(color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = (size.value * 0.46f).sp),
                    )
                }
            }
        }
    }
}

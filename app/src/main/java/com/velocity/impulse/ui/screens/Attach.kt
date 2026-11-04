package com.velocity.impulse.ui.screens

import com.velocity.impulse.ui.theme.Motion
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import java.io.File
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.velocity.impulse.data.Attachment
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type

class Pickers(val camera: () -> Unit, val photos: () -> Unit, val files: () -> Unit)

@Composable
fun rememberPickers(onPicked: (List<Uri>) -> Unit): Pickers {
    val ctx = LocalContext.current
    var shot by remember { mutableStateOf<Uri?>(null) }
    val photos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(6)) { onPicked(it) }
    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { onPicked(it) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> shot?.let { if (ok) onPicked(listOf(it)) } }
    fun snap() {
        val dir = File(ctx.cacheDir, "shots").apply { mkdirs() }
        val uri = FileProvider.getUriForFile(ctx, "com.velocity.impulse.files", File(dir, "photo-${System.currentTimeMillis()}.jpg"))
        shot = uri
        camera.launch(uri)
    }
    val allow = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) snap() }
    return remember(photos, files, camera, allow) {
        Pickers(
            {
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) snap()
                else allow.launch(Manifest.permission.CAMERA)
            },
            { photos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            { files.launch(arrayOf("text/*", "application/json", "application/xml", "image/*")) },
        )
    }
}

/**
 * The plus opens into this: a small island that grows out of the plus's corner, its rows arriving
 * one after another from where the finger is.
 */
@Composable
fun AddMenu(open: Boolean, calm: Boolean, onCamera: () -> Unit, onPhotos: () -> Unit, onFiles: () -> Unit) {
    AnimatedVisibility(
        open,
        enter = if (calm) fadeIn(tween(Motion.M2)) else fadeIn(tween(Motion.M1)) + scaleIn(spring(0.72f, 520f), 0.2f, TransformOrigin(0.08f, 1f)),
        exit = if (calm) fadeOut(tween(Motion.M1)) else fadeOut(tween(Motion.M1)) + scaleOut(tween(Motion.M2, easing = Motion.Leave), 0.4f, TransformOrigin(0.08f, 1f)),
    ) {
        Column(
            Modifier.padding(bottom = Space.S2).widthIn(min = 220.dp, max = 280.dp)
                .background(Tone.Float, Shapes.Card).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            listOf(
                Triple(Glyph.Camera, tr("Take a photo", "Zrób zdjęcie"), onCamera),
                Triple(Glyph.Image, tr("Add photos", "Dodaj zdjęcia"), onPhotos),
                Triple(Glyph.File, tr("Add a file", "Dodaj plik"), onFiles),
            ).forEachIndexed { i, (glyph, label, go) ->
                Row(
                    Modifier.fillMaxWidth()
                        .animateEnterExit(
                            enter = if (calm) EnterTransition.None else fadeIn(tween(Motion.M2, delayMillis = 40 + 40 * (2 - i))) +
                                slideInVertically(spring(0.8f, 420f)) { it / 2 },
                            exit = ExitTransition.None,
                        )
                        .heightIn(min = 52.dp)
                        .pressable(go, Shapes.ControlLg, Tone.Float, Tone.Raise, depth = 0.97f)
                        .padding(horizontal = Space.S3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(glyph, Ink.One, size = 20.dp)
                    Spacer(Modifier.width(Space.S3))
                    BasicText(label, style = Type.Body.copy(fontWeight = FontWeight.Medium))
                }
            }
        }
    }
}

/** What will go with the question: pictures as thumbnails, files as chips, each with a way out. */
@Composable
fun AttachRow(items: List<Attachment>, onRemove: (Attachment) -> Unit) {
    LazyRow(
        Modifier.fillMaxWidth().padding(top = Space.S1),
        contentPadding = PaddingValues(horizontal = Space.S2),
        horizontalArrangement = Arrangement.spacedBy(Space.S2),
    ) {
        items(items, key = { it.name + it.bytes }) { a ->
            val item = remember(a) { PreviewItem(a.name, a.bytes, a.image, a.text, a.thumb) }
            Box(Modifier.animateItem().hiddenWhile(item)) {
                val thumb = remember(a.thumb) { a.thumb?.let(::bitmap) }
                if (thumb != null) {
                    Image(thumb, a.name, Modifier.size(60.dp).opens(item).clip(Shapes.Tile), contentScale = ContentScale.Crop)
                } else {
                    Row(
                        Modifier.height(60.dp).widthIn(max = 200.dp).opens(item).background(Tone.Raise, Shapes.Tile).padding(horizontal = Space.S3),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Glyph.File, LocalAccent.current, size = 20.dp)
                        Spacer(Modifier.width(Space.S2))
                        Column {
                            BasicText(a.name, style = Type.Meta.copy(color = Ink.One, fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            BasicText(sizeOf(a.bytes), style = Type.Label)
                        }
                    }
                }
                Box(
                    Modifier.align(Alignment.TopEnd).padding(3.dp).size(22.dp)
                        .pressable({ onRemove(a) }, CircleShape, Tone.Void.copy(alpha = 0.75f), Tone.Void, depth = 0.85f, stretch = true),
                    contentAlignment = Alignment.Center,
                ) { Icon(Glyph.Close, Ink.One, size = 12.dp) }
            }
        }
    }
}

/** The pictures and files a sent question carried, small, above its text. */
@Composable
fun SentFiles(files: List<org.json.JSONObject>) {
    Row(Modifier.padding(bottom = Space.S1), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        files.take(4).forEach { f ->
            val url = remember(f) { f.optString("thumb").takeIf { it.startsWith("data:image/") } }
            val thumb = remember(f) { url?.let(::bitmap) }
            val item = remember(f) { PreviewItem(f.optString("name"), f.optLong("bytes"), url, kept = true) }
            if (thumb != null) {
                Image(thumb, null, Modifier.size(72.dp).hiddenWhile(item).opens(item).clip(Shapes.Tile), contentScale = ContentScale.Crop)
            } else {
                Row(
                    Modifier.height(36.dp).hiddenWhile(item).opens(item).background(Tone.Raise, Shapes.ControlLg).padding(horizontal = Space.S3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Glyph.File, Ink.Three, size = 16.dp)
                    Spacer(Modifier.width(6.dp))
                    BasicText(f.optString("name"), Modifier.widthIn(max = 160.dp), style = Type.Meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** A thumbnail that grows into the full-screen preview when tapped. */
@Composable
private fun Modifier.opens(item: PreviewItem): Modifier {
    val at = remember { arrayOfNulls<LayoutCoordinates>(1) }
    return onPlaced { at[0] = it }.pressable(
        { at[0]?.takeIf { it.isAttached }?.let { PreviewState.open(item, it.boundsInRoot()) } },
        depth = 0.94f,
    )
}

/** Hidden while the preview is the thumbnail, so it never shows twice. */
private fun Modifier.hiddenWhile(item: PreviewItem) = graphicsLayer { alpha = if (PreviewState.item === item) 0f else 1f }

internal fun bitmap(uri: String) = runCatching {
    val b = Base64.decode(uri.substringAfter("base64,"), Base64.DEFAULT)
    BitmapFactory.decodeByteArray(b, 0, b.size)?.asImageBitmap()
}.getOrNull()

internal fun sizeOf(n: Long) = when {
    n >= 1_000_000 -> "%.1f MB".format(n / 1e6)
    n >= 1_000 -> "%.0f KB".format(n / 1e3)
    else -> "$n B"
}

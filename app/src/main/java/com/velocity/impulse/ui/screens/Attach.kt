package com.velocity.impulse.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.velocity.impulse.data.Attachment
import com.velocity.impulse.data.Chat
import com.velocity.impulse.data.Extract
import com.velocity.impulse.data.Media
import com.velocity.impulse.data.objects
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.LocalShell
import com.velocity.impulse.ui.components.Feel
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.feel
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.LocalReduceMotion
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Signal
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class Pickers(val camera: () -> Unit, val photos: () -> Unit, val files: () -> Unit)

private val FILE_TYPES = arrayOf(
    "text/*", "application/json", "application/xml", "image/*", "application/pdf", "application/rtf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.oasis.opendocument.text",
    "application/vnd.oasis.opendocument.presentation",
    "application/vnd.oasis.opendocument.spreadsheet",
    "application/vnd.ms-excel",
)

@Composable
fun rememberPickers(onPicked: (List<Uri>) -> Unit): Pickers {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var shot by remember { mutableStateOf<Uri?>(null) }
    val take: (List<Uri>) -> Unit = { uris ->
        if (uris.isNotEmpty()) scope.launch(Dispatchers.IO) { uris.forEach { runCatching { Originals.keep(ctx, it) } } }
        onPicked(uris)
    }
    val photos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(6)) { take(it) }
    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { take(it) }
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
            { files.launch(FILE_TYPES) },
        )
    }
}

/**
 * Picked PDFs, copied while the picker's permission lasts, so the preview can draw their pages
 * later. Keyed by name and size, as the chat records them.
 */
internal object Originals {
    private const val MAX = 50L * 1024 * 1024
    private const val AGE = 14L * 86_400_000L

    fun of(ctx: Context, name: String, bytes: Long) = File(File(ctx.cacheDir, "picked"), "$bytes-" + name.replace(Regex("[^A-Za-z0-9._-]"), "_"))

    fun keep(ctx: Context, uri: Uri) {
        val cr = ctx.contentResolver
        var name = ""
        var size = 0L
        cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                name = c.getString(0).orEmpty()
                size = if (c.isNull(1)) 0L else c.getLong(1)
            }
        }
        if (Extract.ext(name) != "pdf" && cr.getType(uri) != "application/pdf") return
        if (size > MAX) return
        val to = of(ctx, name, size)
        val now = System.currentTimeMillis()
        to.parentFile?.apply { mkdirs() }?.listFiles()?.forEach { if (now - it.lastModified() > AGE) it.delete() }
        if (to.isFile && to.length() > 0) return
        val part = File(to.path + ".part")
        cr.openInputStream(uri)?.use { input -> part.outputStream().use { input.copyTo(it) } } ?: return
        part.renameTo(to)
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

/**
 * What will go with the question: pictures as thumbnails, files as chips, each with a way out. A
 * long press on a picture offers saving or sharing it.
 */
@Composable
fun AttachRow(items: List<Attachment>, onRemove: (Attachment) -> Unit) {
    val pairs = remember(items.toList()) { items.map { it to PreviewItem(it.name, it.bytes, it.image, it.text, it.thumb, pages = it.pages, key = "draft/${it.name}/${it.bytes}") } }
    val album = remember(pairs) { pairs.map { it.second } }
    LazyRow(
        Modifier.fillMaxWidth().padding(top = Space.S1),
        contentPadding = PaddingValues(horizontal = Space.S2),
        horizontalArrangement = Arrangement.spacedBy(Space.S2),
    ) {
        items(pairs, key = { it.second.key }) { (a, item) ->
            var acting by remember { mutableStateOf(false) }
            Box(Modifier.animateItem().hiddenWhile(item)) {
                val thumb = remember(a.thumb) { a.thumb?.let(::bitmap) }
                if (thumb != null) {
                    val image = a.image
                    Image(
                        thumb, a.name,
                        Modifier.size(60.dp).opens(item, { album }, if (image != null) ({ acting = true }) else null).clip(Shapes.Tile),
                        contentScale = ContentScale.Crop,
                    )
                    if (acting && image != null) PictureActions(a.name, { image }) { acting = false }
                } else {
                    Row(
                        Modifier.height(60.dp).widthIn(max = 200.dp).opens(item, { album }).background(Tone.Raise, Shapes.Tile).padding(horizontal = Space.S3),
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

/**
 * The pictures and files a sent question carried, small, above its text. A tap opens the preview
 * with every file of the chat to step through; a long press on a picture saves or shares it.
 */
@Composable
fun SentFiles(files: List<JSONObject>) {
    val ctx = LocalContext.current
    val m = LocalShell.current.m
    // Read once, unobserved: the chat changes as answers stream and these tiles must not follow it.
    val owner = remember(files) { Snapshot.withoutReadObservation { m.chat }?.let { ownerOf(it, files) } }
    val items = remember(files, owner) { sentItems(owner, files) }
    Row(Modifier.padding(bottom = Space.S1), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        files.take(4).forEachIndexed { i, f ->
            val item = items[i]
            val thumb = remember(f) { item.thumb?.let(::bitmap) }
            val album = { albumOf(m.chat).takeIf { all -> all.any { it.key == item.key } } ?: items }
            if (thumb != null) {
                var acting by remember { mutableStateOf(false) }
                Box {
                    Image(
                        thumb, null,
                        Modifier.size(72.dp).hiddenWhile(item).opens(item, album, { acting = true }).clip(Shapes.Tile),
                        contentScale = ContentScale.Crop,
                    )
                    if (acting) PictureActions(item.name, { owner?.let { Media(File(ctx.filesDir, "media")).pictures(it).getOrNull(item.slot) } }) { acting = false }
                }
            } else {
                Row(
                    Modifier.height(36.dp).hiddenWhile(item).opens(item, album).background(Tone.Raise, Shapes.ControlLg).padding(horizontal = Space.S3),
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

/** The question these files went with, found by its record. */
private fun ownerOf(chat: Chat, files: List<JSONObject>): String? {
    val want = JSONArray(files).toString()
    return chat.messages.lastOrNull { it.role == "user" && it.raw.optJSONArray("files")?.toString() == want }?.id
}

private fun sentItems(owner: String?, files: List<JSONObject>): List<PreviewItem> {
    var slot = 0
    return files.mapIndexed { i, f ->
        val looked = f.optInt("looked")
        PreviewItem(
            f.optString("name"), f.optLong("bytes"),
            thumb = f.optString("thumb").takeIf { it.startsWith("data:image/") },
            kept = true, id = owner, slot = slot, looked = looked,
            key = "${owner ?: f.optString("name")}/$i",
        ).also { slot += looked }
    }
}

/** Every file sent in [chat], oldest first, as the preview steps through them. */
internal fun albumOf(chat: Chat?): List<PreviewItem> =
    chat?.messages.orEmpty().filter { it.role == "user" }.flatMap { sentItems(it.id, it.raw.optJSONArray("files").objects()) }

/** A thumbnail that grows into the full-screen preview when tapped. */
@Composable
private fun Modifier.opens(item: PreviewItem, album: () -> List<PreviewItem>, onLong: (() -> Unit)? = null): Modifier {
    val at = remember { arrayOfNulls<LayoutCoordinates>(1) }
    return onPlaced { at[0] = it }.pressable(
        { at[0]?.takeIf { it.isAttached }?.let { PreviewState.open(item, it.boundsInRoot(), album()) } },
        depth = 0.94f,
        onLong = onLong,
    )
}

/** Hidden while the preview is the thumbnail, so it never shows twice. */
private fun Modifier.hiddenWhile(item: PreviewItem) = graphicsLayer { alpha = if (PreviewState.item?.key == item.key) 0f else 1f }

/**
 * A small island over a held picture: save it to the gallery or share it. Saving answers in place
 * with a confirmation, then the island leaves. [load] gives the full picture, null when the phone
 * has only the thumbnail.
 */
@Composable
private fun PictureActions(name: String, load: suspend () -> String?, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val calm = LocalReduceMotion.current
    val scope = rememberCoroutineScope()
    val gap = with(LocalDensity.current) { 8.dp.roundToPx() }
    var said by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    val grow = remember { Animatable(if (calm) 1f else 0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, spring(0.7f, 520f)) }
    fun act(save: Boolean) = scope.launch {
        val data = withContext(Dispatchers.IO) { load() }
        if (data != null && !save) {
            shareFile(ctx, name.substringBeforeLast('.') + ".jpg", "image/jpeg") { it.writeBytes(bytesOf(data)) }
            onDone()
            return@launch
        }
        val ok = data != null && withContext(Dispatchers.IO) { savePicture(ctx, bytesOf(data), name) }
        view.feel(if (ok) Feel.Confirm else Feel.Reject)
        said = when {
            ok -> tr("Saved to Photos", "Zapisano w Zdjęciach") to true
            data == null -> tr("The full picture is on your computer", "Pełne zdjęcie jest na komputerze") to false
            else -> tr("Could not save the picture", "Nie udało się zapisać zdjęcia") to false
        }
        delay(1400)
        onDone()
    }
    Popup(popupPositionProvider = remember(gap) { Above(gap) }, onDismissRequest = onDone, properties = PopupProperties(focusable = true)) {
        Row(
            Modifier
                .graphicsLayer {
                    val g = grow.value
                    alpha = g.coerceIn(0f, 1f)
                    scaleX = 0.82f + 0.18f * g
                    scaleY = 0.82f + 0.18f * g
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .shadow(10.dp, CircleShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.32f))
                .background(Tone.Float, CircleShape)
                .animateContentSize(spring(0.86f, 520f))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val s = said
            if (s != null) {
                Row(Modifier.height(44.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (s.second) Glyph.Check else Glyph.Alert, if (s.second) Signal.Ok else Signal.Err, size = 16.dp)
                    Spacer(Modifier.width(8.dp))
                    BasicText(s.first, style = Type.Meta.copy(color = Ink.One, fontWeight = FontWeight.Medium))
                }
            } else {
                Capsule(tr("Save to Photos", "Zapisz w Zdjęciach")) { act(true) }
                Capsule(tr("Share", "Udostępnij")) { act(false) }
            }
        }
    }
}

/** Centred above its anchor, kept inside the window. */
private class Above(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize) = IntOffset(
        (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(0, maxOf(0, windowSize.width - popupContentSize.width)),
        (anchorBounds.top - popupContentSize.height - gap).coerceAtLeast(0),
    )
}

/** A text control on a floating island: a capsule that lights under the finger. */
@Composable
internal fun Capsule(label: String, onClick: () -> Unit) {
    Box(
        Modifier.height(44.dp)
            .pressable(onClick, CircleShape, Color.Transparent, Tone.Raise, depth = 0.94f, stretch = true)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { BasicText(label, style = Type.Meta.copy(color = Ink.One, fontWeight = FontWeight.SemiBold)) }
}

internal fun bitmap(uri: String) = runCatching {
    val b = bytesOf(uri)
    BitmapFactory.decodeByteArray(b, 0, b.size)?.asImageBitmap()
}.getOrNull()

/** The bytes of a data: URL or bare base64. */
internal fun bytesOf(data: String): ByteArray = Base64.decode(data.substringAfter("base64,"), Base64.DEFAULT)

internal fun sizeOf(n: Long) = when {
    n >= 1_000_000 -> "%.1f MB".format(n / 1e6)
    n >= 1_000 -> "%.0f KB".format(n / 1e3)
    else -> "$n B"
}

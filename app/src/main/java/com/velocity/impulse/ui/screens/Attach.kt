package com.velocity.impulse.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
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
import com.velocity.impulse.ui.components.IconBtn
import com.velocity.impulse.ui.components.feel
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.LocalReduceMotion
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Radius
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Signal
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Theme
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

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

/** The island's rounded outline: [w] by [h] from the bottom-left corner, where the plus sits. */
private data class Grown(val w: Float, val h: Float, val r: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Rounded(RoundRect(0f, size.height - h, w, size.height, CornerRadius(r)))
}

/**
 * The plus opening into its menu the way the Dynamic Island grows: one island anchored at the
 * plus, its size and corner on a single spring, the plus turning into the island's close mark.
 * The rows arrive once the shape is most of the way there and leave first on the way back.
 * [grow] runs 0 (the plus) to 1 (the island); [plus] is the plus's bounds in root.
 */
@Composable
fun BoxScope.AddIsland(
    open: Boolean,
    grow: Animatable<Float, AnimationVector1D>,
    plus: State<Rect>,
    onClose: () -> Unit,
    onCamera: () -> Unit,
    onPhotos: () -> Unit,
    onFiles: () -> Unit,
) {
    val calm = LocalReduceMotion.current
    LaunchedEffect(open) {
        grow.animateTo(
            if (open) 1f else 0f,
            when {
                calm -> tween(Motion.M2)
                open -> spring(0.74f, 380f)
                else -> spring(0.92f, 460f)
            },
        )
    }
    val shown by remember { derivedStateOf { grow.value > 0f } }
    if (!open && !shown) return
    BackHandler(open, onClose)
    val host = remember { mutableStateOf(Offset.Zero) }
    val corner = with(LocalDensity.current) { Radius.Sheet.toPx() }
    Box(
        Modifier.matchParentSize()
            .onPlaced { host.value = it.positionInRoot() }
            .then(if (open) Modifier.pointerInput(Unit) { detectTapGestures { onClose() } } else Modifier),
    ) {
        Box(
            Modifier
                .layout { measurable, c ->
                    val p = measurable.measure(c.copy(minWidth = 0, minHeight = 0))
                    layout(p.width, p.height) {
                        val at = plus.value
                        p.place((at.left - host.value.x).roundToInt(), (at.bottom - host.value.y).roundToInt() - p.height)
                    }
                }
                .width(248.dp)
                .graphicsLayer {
                    val t = grow.value
                    val k = t.coerceIn(0f, 1f)
                    val s = plus.value.width
                    shape = Grown(s + (size.width - s) * k, s + (size.height - s) * k, s / 2f + (corner - s / 2f) * k)
                    clip = true
                    shadowElevation = 10.dp.toPx() * k
                    ambientShadowColor = Color.Black.copy(alpha = 0.2f * Theme.palette.shade)
                    spotShadowColor = Color.Black.copy(alpha = 0.3f * Theme.palette.shade)
                    val over = 1f + max(0f, t - 1f) * 0.6f
                    scaleX = over
                    scaleY = over
                    transformOrigin = TransformOrigin(0f, 1f)
                }
                .drawBehind { drawRect(Tone.Float, alpha = (grow.value * 6f).coerceIn(0f, 1f)) }
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            Column(
                Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 46.dp)
                    .graphicsLayer {
                        val c = ((grow.value - 0.55f) / 0.4f).coerceIn(0f, 1f)
                        alpha = c
                        translationY = (1f - c) * 8.dp.toPx()
                        if (Build.VERSION.SDK_INT >= 31) renderEffect = if (c in 0.01f..0.99f) BlurEffect((1f - c) * 12f, (1f - c) * 12f) else null
                    },
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                listOf(
                    Triple(Glyph.Camera, tr("Take a photo", "Zrób zdjęcie"), onCamera),
                    Triple(Glyph.Image, tr("Add photos", "Dodaj zdjęcia"), onPhotos),
                    Triple(Glyph.File, tr("Add a file", "Dodaj plik"), onFiles),
                ).forEach { (glyph, label, go) ->
                    Row(
                        Modifier.fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .pressable(go, Shapes.ControlLg, Tone.Float, Tone.Raise, depth = 0.97f, enabled = open)
                            .padding(horizontal = Space.S3),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(glyph, Ink.One, size = 20.dp)
                        Spacer(Modifier.width(Space.S3))
                        BasicText(label, style = Type.Body.copy(fontWeight = FontWeight.Medium))
                    }
                }
            }
            IconBtn(
                Glyph.Plus, onClose,
                Modifier.align(Alignment.BottomStart).graphicsLayer { rotationZ = 45f * grow.value.coerceIn(0f, 1f) },
                tint = Ink.Two, fill = Color.Transparent, size = 44.dp, description = tr("Close", "Zamknij"),
            )
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
 * The pictures and files a sent question carried, above its text. A picture sent from this phone
 * shows its kept full copy; one sent elsewhere shows its thumbnail, never larger than it is. A tap
 * opens the preview with every file of the chat to step through; a long press on a picture saves
 * or shares it.
 */
@Composable
fun SentFiles(files: List<JSONObject>) {
    val ctx = LocalContext.current
    val d = LocalDensity.current
    val m = LocalShell.current.m
    val media = remember { Media(File(ctx.filesDir, "media")) }
    // Read once, unobserved: the chat changes as answers stream and these tiles must not follow it.
    val owner = remember(files) { Snapshot.withoutReadObservation { m.chat }?.let { ownerOf(it, files) } }
    val items = remember(files, owner) { sentItems(owner, files) }
    val tile = if (files.size == 1) 132.dp else 72.dp
    Row(Modifier.padding(bottom = Space.S1), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        files.take(4).forEachIndexed { i, f ->
            val item = items[i]
            val thumb = remember(f) { item.thumb?.let(::bitmap) }
            val album = { albumOf(m.chat).takeIf { all -> all.any { it.key == item.key } } ?: items }
            if (thumb != null) {
                var acting by remember { mutableStateOf(false) }
                val kept = remember(owner, item.slot) { owner?.takeIf { item.looked > 0 }?.let { media.picture(it, item.slot) } }
                val side = if (kept != null) tile else minOf(tile, with(d) { minOf(thumb.width, thumb.height).toDp() })
                val px = with(d) { side.roundToPx() }
                val sharp by produceState<ImageBitmap?>(null, kept, px) { kept?.let { k -> value = withContext(Dispatchers.IO) { sampled(k, px) } } }
                Box {
                    Image(
                        sharp ?: thumb, null,
                        Modifier.size(side).hiddenWhile(item).opens(item, album, { acting = true }).clip(Shapes.Tile),
                        contentScale = ContentScale.Crop,
                    )
                    if (acting) PictureActions(item.name, { owner?.let { media.pictures(it).getOrNull(item.slot) } }) { acting = false }
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

/** A kept picture decoded at no less than [side] px on its shorter side, and no more than twice that. */
internal fun sampled(f: File, side: Int): ImageBitmap? = runCatching {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(f.path, o)
    var s = 1
    while (minOf(o.outWidth, o.outHeight) / (s * 2) >= side) s *= 2
    BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = s })?.asImageBitmap()
}.getOrNull()

/** The bytes of a data: URL or bare base64. */
internal fun bytesOf(data: String): ByteArray = Base64.decode(data.substringAfter("base64,"), Base64.DEFAULT)

internal fun sizeOf(n: Long) = when {
    n >= 1_000_000 -> "%.1f MB".format(n / 1e6)
    n >= 1_000 -> "%.0f KB".format(n / 1e3)
    else -> "$n B"
}

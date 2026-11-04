package com.velocity.impulse.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.velocity.impulse.data.Extract
import com.velocity.impulse.data.Media
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.components.Feel
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.IconBtn
import com.velocity.impulse.ui.components.feel
import com.velocity.impulse.ui.components.glass
import com.velocity.impulse.ui.components.glassSource
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.components.rubber
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalReduceMotion
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Radius
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Signal
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * A file to look at full screen. [image] is a data: URL or bare base64; [thumb], when given, is
 * shown while [image] decodes. [kept] means the chat only holds a small copy and the rest stays
 * on the computer. [pages] are a scan's pages as pictures. A sent file names its question [id]:
 * the phone may keep its full pictures (from [slot], [looked] of them) and words under it.
 * [key] tells files apart across a chat's [PreviewState.album].
 */
class PreviewItem(
    val name: String,
    val bytes: Long,
    val image: String? = null,
    val text: String? = null,
    val thumb: String? = null,
    val kept: Boolean = false,
    val pages: List<String> = emptyList(),
    val id: String? = null,
    val slot: Int = 0,
    val looked: Int = 0,
    val key: String = "$name/$bytes",
)

object PreviewState {
    var item by mutableStateOf<PreviewItem?>(null)
        private set
    var showing by mutableStateOf(false)
        private set
    internal var from by mutableStateOf(Rect.Zero)
        private set

    /** The files the preview can step through, in chat order; empty for one file alone. */
    var album by mutableStateOf<List<PreviewItem>>(emptyList())
        private set

    /** Opens [item] growing out of [from], the tapped thumbnail's bounds in root. */
    fun open(item: PreviewItem, from: Rect, album: List<PreviewItem> = emptyList()) {
        this.item = item
        this.from = from
        this.album = album
        showing = true
    }

    /** Moves [by] files along the album; false at either end. */
    fun step(by: Int): Boolean {
        val i = album.indexOfFirst { it.key == item?.key }
        if (i < 0) return false
        item = album.getOrNull(i + by) ?: return false
        from = Rect.Zero
        return true
    }

    fun close() {
        showing = false
    }

    internal fun done() {
        if (!showing) {
            item = null
            album = emptyList()
        }
    }
}

private const val MAX_ZOOM = 4f
private val TABLES = setOf("csv", "tsv", "xlsx", "ods")

/** What the phone holds of a file beyond its record: the full picture, a scan's pages, its words, the PDF itself. */
private class Found(val full: String?, val pages: List<String>, val text: String?, val pdf: File?)

private fun find(ctx: Context, item: PreviewItem): Found {
    val media = Media(File(ctx.filesDir, "media"))
    val own = if (item.id != null && item.looked > 0 && item.image == null && item.pages.isEmpty()) {
        media.pictures(item.id).drop(item.slot).take(item.looked)
    } else emptyList()
    val document = Extract.isDocument(item.name, "")
    val text = item.text ?: if (item.id != null && item.looked == 0) media.docs(item.id).firstOrNull { it.name == item.name }?.text else null
    val pdf = Originals.of(ctx, item.name, item.bytes).takeIf { f -> f.isFile && runCatching { PdfDoc(f).close() }.isSuccess }
    return Found(
        full = item.image?.takeIf { !item.kept } ?: own.firstOrNull()?.takeIf { !document },
        pages = item.pages.ifEmpty { if (document) own else emptyList() },
        text = text,
        pdf = pdf,
    )
}

@Composable
fun PreviewHost() {
    val item = PreviewState.item ?: return
    val showing = PreviewState.showing
    val calm = LocalReduceMotion.current
    val keyboard = LocalSoftwareKeyboardController.current
    val ctx = LocalContext.current
    val p = remember { Animatable(0f) }
    LaunchedEffect(showing) {
        if (showing) keyboard?.hide()
        val spec: AnimationSpec<Float> = if (calm) tween(Motion.M2) else Motion.morph()
        p.animateTo(if (showing) 1f else 0f, spec)
        PreviewState.done()
    }
    BackHandler(showing) { PreviewState.close() }

    val found by produceState<Found?>(null, item) { value = withContext(Dispatchers.IO) { runCatching { find(ctx, item) }.getOrNull() ?: Found(null, emptyList(), item.text, null) } }
    // A file reached by stepping fades in where the last one was; one opened from a tile grows out of it.
    val enter = remember(item) { Animatable(if (PreviewState.from == Rect.Zero) 0f else 1f) }
    LaunchedEffect(item) { enter.animateTo(1f, tween(Motion.M2)) }
    val shift = remember { arrayOf(Offset.Zero) }
    val from = { if (calm || PreviewState.from == Rect.Zero) null else PreviewState.from.translate(-shift[0]) }
    val picture = remember(item) { (item.thumb ?: item.image)?.let(::bitmap) }
    Box(
        Modifier.fillMaxSize()
            .onPlaced { shift[0] = it.positionInRoot() }
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false) } },
    ) {
        if (picture != null) Picture(item, picture, found, { p.value }, from, { enter.value })
        else Document(item, found, { p.value }, from, { enter.value })
    }
}

@Composable
private fun Picture(item: PreviewItem, small: ImageBitmap, found: Found?, progress: () -> Float, from: () -> Rect?, enter: () -> Float) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val haze = remember { HazeState() }
    val d = LocalDensity.current
    val tile = with(d) { Radius.Tile.toPx() }
    val card = with(d) { Radius.Card.toPx() }
    val reach = with(d) { 48.dp.toPx() }
    val edge = with(d) { 96.dp.toPx() }
    val zoom = remember(item) { Animatable(1f) }
    val pan = remember(item) { Animatable(Offset.Zero, Offset.VectorConverter) }
    val drag = remember(item) { Animatable(Offset.Zero, Offset.VectorConverter) }
    val src = found?.full
    // Only a thumbnail on the phone: shown at its own pixels, never blown up into a blur.
    val natural = found != null && src == null
    val full = produceState<ImageBitmap?>(null, item, src) {
        if (item.thumb != null && src != null) value = withContext(Dispatchers.Default) { bitmap(src) }
    }
    val hole = remember { Path() }
    val showing = PreviewState.showing
    LaunchedEffect(showing) {
        if (!showing) {
            launch { zoom.animateTo(1f, Motion.morph()) }
            pan.animateTo(Offset.Zero, Motion.morph())
        }
    }

    Canvas(
        Modifier.fillMaxSize()
            .glassSource(haze)
            .pointerInput(item, natural) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    if (!PreviewState.showing) return@awaitEachGesture
                    val box = size.toSize()
                    val c = box.center
                    val f = fit(box, small, natural)
                    val tracker = VelocityTracker()
                    var mode = 0
                    var travel = Offset.Zero
                    var rawZoom = zoom.value
                    var raw = pan.value
                    var moved = drag.value
                    do {
                        val e = awaitPointerEvent()
                        val step = e.calculatePan()
                        e.changes.firstOrNull { it.pressed }?.let { tracker.addPointerInputChange(it) }
                        if (mode == 0) {
                            travel += step
                            mode = when {
                                e.changes.count { it.pressed } > 1 -> 1
                                travel.getDistance() < viewConfiguration.touchSlop -> 0
                                zoom.value > 1.01f || abs(travel.x) > abs(travel.y) -> 1
                                else -> 2
                            }
                        }
                        if (mode == 1) {
                            val k = e.calculateZoom()
                            val q = e.calculateCentroid().takeIf { it.isSpecified } ?: c
                            rawZoom *= k
                            raw = (q - c) - (q - c - raw) * k + step
                            val z = when {
                                rawZoom > MAX_ZOOM -> MAX_ZOOM + rubber(rawZoom - MAX_ZOOM, 1f)
                                rawZoom < 1f -> 1f + rubber(rawZoom - 1f, 0.3f)
                                else -> rawZoom
                            }
                            val b = bounds(f, z, box)
                            val o = Offset(band(raw.x, b.x, reach), band(raw.y, b.y, reach))
                            scope.launch { zoom.snapTo(z) }
                            scope.launch { pan.snapTo(o) }
                        } else if (mode == 2) {
                            val was = abs(moved.y) > edge
                            moved += step
                            if (was != abs(moved.y) > edge) view.feel(if (was) Feel.Unedge else Feel.Edge)
                            val to = moved
                            scope.launch { drag.snapTo(to) }
                        }
                        if (mode != 0) e.changes.forEach { if (it.positionChanged()) it.consume() }
                    } while (e.changes.any { it.pressed })

                    val v = tracker.calculateVelocity().let { Offset(it.x, it.y) }
                    when (mode) {
                        1 -> {
                            val z = rawZoom.coerceIn(1f, MAX_ZOOM)
                            val flung = abs(raw.x) > edge * 0.6f || (abs(v.x) > 900.dp.toPx() && abs(raw.x) > viewConfiguration.touchSlop)
                            if (z <= 1.01f && flung && PreviewState.step(if (raw.x < 0) 1 else -1)) {
                                view.feel(Feel.Tick)
                            } else {
                                val glide = if (z > 1f) v * 0.15f else Offset.Zero
                                val to = clamp((pan.value + glide) * (z / zoom.value), bounds(f, z, box))
                                scope.launch { zoom.animateTo(z, Motion.morph()) }
                                scope.launch { pan.animateTo(to, spring(1f, 220f)) }
                            }
                        }
                        2 -> if (abs(moved.y) > edge || abs(v.y) > 1200.dp.toPx()) {
                            PreviewState.close()
                        } else {
                            scope.launch { drag.animateTo(Offset.Zero, spring(0.72f, 420f)) }
                        }
                    }
                }
            }
            .pointerInput(item, natural) {
                detectTapGestures(onDoubleTap = { q ->
                    if (!PreviewState.showing) return@detectTapGestures
                    view.feel(Feel.Tick)
                    val box = size.toSize()
                    val z = if (zoom.value > 1.05f) 1f else 2.5f
                    val to = clamp((q - box.center) * (1f - z), bounds(fit(box, small, natural), z, box))
                    scope.launch { zoom.animateTo(z, Motion.morph()) }
                    scope.launch { pan.animateTo(to, Motion.morph()) }
                })
            },
    ) {
        val t = progress()
        val dragged = (abs(drag.value.y) / size.height).coerceIn(0f, 1f)
        drawRect(Color.Black, alpha = 0.94f * t * (1f - dragged * 1.4f).coerceIn(0f, 1f))
        val f = fit(size, small, natural)
        val k = zoom.value * (1f - 0.35f * dragged)
        val base = around(f.center + pan.value + drag.value, f.width * k, f.height * k)
        val start = from()
        val pic = if (start == null) base else lerp(cover(start, small), base, t)
        val window = if (start == null) base else lerp(start, base, t)
        hole.reset()
        hole.addRoundRect(RoundRect(window, CornerRadius(tile * (1f - t) + card * min(1f, dragged * 4f))))
        val img = full.value ?: small
        clipPath(hole) {
            translate(pic.left, pic.top) {
                scale(pic.width / img.width, pic.height / img.height, Offset.Zero) {
                    drawImage(img, alpha = (if (start == null) t else 1f) * enter())
                }
            }
        }
    }
    Box(
        Modifier.fillMaxSize().graphicsLayer {
            alpha = ((progress() - 0.6f) / 0.4f).coerceIn(0f, 1f) * (1f - abs(drag.value.y) / size.height * 4f).coerceIn(0f, 1f)
        },
    ) {
        Top(item, haze, Modifier.align(Alignment.TopCenter))
        Dock(
            item, found, haze, Modifier.align(Alignment.BottomCenter),
            note = if (item.kept && found != null && src == null) tr("The full picture stays on your computer.", "Pełne zdjęcie zostaje na komputerze.") else null,
        )
    }
}

/** Every file that is not a picture: PDF pages, a scan, a table, or its words, on a sheet that grows out of the tile. */
@Composable
private fun Document(item: PreviewItem, found: Found?, progress: () -> Float, from: () -> Rect?, enter: () -> Float) {
    val haze = remember { HazeState() }
    val tile = with(LocalDensity.current) { Radius.Tile.toPx() }
    var where by remember(item) { mutableStateOf("") }
    val pdf by produceState<PdfDoc?>(null, found?.pdf) {
        val f = found?.pdf ?: return@produceState
        val doc = withContext(Dispatchers.IO) { runCatching { PdfDoc(f) }.getOrNull() }
        value = doc
        awaitDispose { doc?.close() }
    }
    val ext = Extract.ext(item.name)
    Box(Modifier.fillMaxSize().drawBehind { drawRect(Color.Black, alpha = 0.6f * progress()) }) {
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    val t = progress()
                    val start = from()
                    if (start == null) {
                        alpha = t
                    } else {
                        shape = Window(lerp(start, Rect(Offset.Zero, size), t), tile * (1f - t))
                        clip = true
                    }
                }
                .background(Tone.App),
        ) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = ((progress() - 0.35f) / 0.65f).coerceIn(0f, 1f) * enter() }) {
                Box(Modifier.fillMaxSize().glassSource(haze)) {
                    val doc = pdf
                    when {
                        found == null || (found.pdf != null && doc == null) -> {}
                        doc != null -> Pages(doc.count, { i, w -> withContext(Dispatchers.IO) { doc.draw(i, w) } }) { where = it }
                        found.pages.isNotEmpty() -> Pages(found.pages.size, { i, _ -> withContext(Dispatchers.Default) { bitmap(found.pages[i]) } }) { where = it }
                        found.text != null && ext in TABLES -> Table(found.text, item.name)
                        found.text != null -> Lines(found.text, Extract.isDocument(item.name, ""))
                        else -> BasicText(
                            tr("The file stays on your computer.", "Plik zostaje na komputerze."),
                            Modifier.align(Alignment.Center).padding(32.dp),
                            style = Type.Body.copy(color = Ink.Three, textAlign = TextAlign.Center),
                        )
                    }
                }
                Top(item, haze, Modifier.align(Alignment.TopCenter), where)
                Dock(item, found, haze, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun Lines(text: String, prose: Boolean) {
    val lines = remember(text) { text.lines() }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 120.dp, bottom = 120.dp),
    ) {
        items(lines.size, key = { it }) { BasicText(lines[it], style = if (prose) Type.Body.copy(color = Ink.Two) else Type.Mono) }
    }
}

/** A PDF's pages (or a scan's) side by side: swipe between them, pinch or double-tap to look closer. */
@Composable
private fun Pages(count: Int, draw: suspend (Int, Int) -> ImageBitmap?, onPage: (String) -> Unit) {
    val state = rememberPagerState { count }
    val view = LocalView.current
    val width = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val render = (width * 1.5f).toInt().coerceIn(600, 2200)
    LaunchedEffect(state, count) {
        snapshotFlow { state.currentPage }.collect { onPage(if (count > 1) tr("Page ${it + 1} of $count", "Strona ${it + 1} z $count") else "") }
    }
    LaunchedEffect(state) { snapshotFlow { state.settledPage }.drop(1).collect { view.feel(Feel.Tick) } }
    HorizontalPager(
        state,
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 84.dp, bottom = 100.dp),
        pageSpacing = 16.dp,
        key = { it },
    ) { i ->
        val page by produceState<ImageBitmap?>(null, i) { value = runCatching { draw(i, render) }.getOrNull() }
        PdfPage(page)
    }
}

@Composable
private fun PdfPage(bmp: ImageBitmap?) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val zoom = remember { Animatable(1f) }
    val pan = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val ratio = bmp?.let { it.width.toFloat() / it.height } ?: (1f / 1.414f)
    Box(Modifier.fillMaxSize().zoomable(zoom, pan, scope, view), contentAlignment = Alignment.Center) {
        Box(
            Modifier.padding(horizontal = 16.dp)
                .aspectRatio(ratio)
                .graphicsLayer {
                    scaleX = zoom.value
                    scaleY = zoom.value
                    translationX = pan.value.x
                    translationY = pan.value.y
                }
                .shadow(10.dp, Shapes.ControlSm)
                .background(Color.White, Shapes.ControlSm),
        ) {
            if (bmp != null) Image(bmp, null, Modifier.fillMaxSize().clip(Shapes.ControlSm), contentScale = ContentScale.FillBounds)
        }
    }
}

/** Pinch and pan once zoomed; a single finger at rest size is left to the pager. */
private fun Modifier.zoomable(
    zoom: Animatable<Float, AnimationVector1D>,
    pan: Animatable<Offset, AnimationVector2D>,
    scope: CoroutineScope,
    view: View,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var z = zoom.value
        var o = pan.value
        do {
            val e = awaitPointerEvent()
            if (e.changes.count { it.pressed } > 1 || z > 1.01f) {
                val c = size.toSize().center
                val q = e.calculateCentroid().takeIf { it.isSpecified } ?: c
                val next = (z * e.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                o = (q - c) - (q - c - o) * (next / z) + e.calculatePan()
                z = next
                o = clamp(o, Offset(size.width * (z - 1f) / 2f, size.height * (z - 1f) / 2f))
                val zz = z
                val oo = o
                scope.launch { zoom.snapTo(zz) }
                scope.launch { pan.snapTo(oo) }
                e.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } while (e.changes.any { it.pressed })
        if (z < 1.05f) {
            scope.launch { zoom.animateTo(1f, Motion.morph()) }
            scope.launch { pan.animateTo(Offset.Zero, Motion.morph()) }
        }
    }
}.pointerInput(Unit) {
    detectTapGestures(onDoubleTap = { q ->
        view.feel(Feel.Tick)
        val z = if (zoom.value > 1.05f) 1f else 2.5f
        val to = clamp((q - size.toSize().center) * (1f - z), Offset(size.width * (z - 1f) / 2f, size.height * (z - 1f) / 2f))
        scope.launch { zoom.animateTo(z, Motion.morph()) }
        scope.launch { pan.animateTo(to, Motion.morph()) }
    })
}

/** An open PDF; pages are drawn one at a time, as the renderer allows. */
private class PdfDoc(file: File) : Closeable {
    private val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = try {
        PdfRenderer(fd)
    } catch (e: Exception) {
        fd.close()
        throw e
    }
    val count = renderer.pageCount
    private var closed = false

    fun draw(i: Int, width: Int): ImageBitmap? = synchronized(this) {
        if (closed) return null
        renderer.openPage(i).use { p ->
            var w = width
            var h = (w.toLong() * p.height / p.width.coerceAtLeast(1)).toInt()
            if (h > 4000) {
                w = (w * 4000L / h).toInt()
                h = 4000
            }
            val b = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            b.eraseColor(android.graphics.Color.WHITE)
            p.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            b.asImageBitmap()
        }
    }

    override fun close() = synchronized(this) {
        if (!closed) {
            closed = true
            renderer.close()
            fd.close()
        }
    }
}

/** A sheet as a grid: the first row stays on top, columns sized to what they hold, numbers to the right. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Table(text: String, name: String) {
    val sheets by produceState<List<Pair<String, List<List<String>>>>?>(null, text) {
        value = withContext(Dispatchers.Default) { sheetsOf(text, name) }
    }
    val all = sheets ?: return
    var at by remember(text) { mutableIntStateOf(0) }
    val rows = all.getOrNull(at)?.second.orEmpty()
    val widths = remember(rows) { widthsOf(rows) }
    Column(Modifier.fillMaxSize().padding(top = 84.dp)) {
        if (all.size > 1) LazyRow(
            Modifier.padding(bottom = 8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(all.size) { i ->
                Box(
                    Modifier.height(36.dp)
                        .pressable({ at = i }, CircleShape, if (i == at) Tone.Hover else Tone.Panel, Tone.Press, depth = 0.94f, stretch = true)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { BasicText(all[i].first, style = Type.Meta.copy(color = if (i == at) Ink.One else Ink.Three, fontWeight = FontWeight.Medium)) }
            }
        }
        if (rows.isEmpty()) {
            BasicText(tr("This sheet is empty.", "Ten arkusz jest pusty."), Modifier.padding(24.dp), style = Type.Body.copy(color = Ink.Three))
            return@Column
        }
        Box(Modifier.weight(1f).fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
            LazyColumn(
                Modifier.width(widths.fold(0.dp) { a, b -> a + b }).fillMaxHeight().clip(Shapes.Tile).background(Tone.Panel),
                contentPadding = PaddingValues(bottom = 112.dp),
            ) {
                stickyHeader(key = "head") { Cells(rows[0], widths, Tone.Raise, head = true) }
                items(rows.size - 1, key = { it }) { Cells(rows[it + 1], widths, if (it % 2 == 1) Tone.Work else Tone.Panel, head = false) }
            }
        }
    }
}

@Composable
private fun Cells(row: List<String>, widths: List<Dp>, fill: Color, head: Boolean) {
    Row(Modifier.background(fill).height(36.dp), verticalAlignment = Alignment.CenterVertically) {
        widths.forEachIndexed { c, w ->
            val s = row.getOrElse(c) { "" }
            val number = !head && s.isNotBlank() && s.trim().replace(",", "").replace(" ", "").toDoubleOrNull() != null
            BasicText(
                s,
                Modifier.width(w).padding(horizontal = 12.dp),
                style = (if (head) Type.Label.copy(color = Ink.Two, fontWeight = FontWeight.SemiBold) else Type.Meta.copy(color = Ink.One))
                    .copy(textAlign = if (number) TextAlign.End else TextAlign.Start, fontFeatureSettings = "tnum"),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun widthsOf(rows: List<List<String>>): List<Dp> {
    val n = rows.maxOfOrNull { it.size } ?: 0
    val sample = rows.take(300)
    return (0 until n).map { c -> ((sample.maxOf { it.getOrElse(c) { "" }.length }) * 7.2f + 24f).coerceIn(56f, 260f).dp }
}

/** A workbook's text comes as "Sheet n" blocks of tab-separated rows; CSV and TSV are split here. */
private fun sheetsOf(text: String, name: String): List<Pair<String, List<List<String>>>> {
    val clean = text.removePrefix("\uFEFF")
    return when (Extract.ext(name)) {
        "xlsx" -> clean.split(Regex("\n\n(?=Sheet \\d+\n)")).map { s ->
            s.substringBefore('\n') to s.substringAfter('\n', "").lines().filter { it.isNotBlank() }.map { it.split('\t') }
        }
        "ods" -> listOf(name to clean.lines().filter { it.isNotBlank() }.map { it.split('\t') })
        else -> {
            val head = clean.substringBefore('\n')
            val sep = when {
                Extract.ext(name) == "tsv" || head.count { it == '\t' } > head.count { it == ',' } -> '\t'
                head.count { it == ';' } > head.count { it == ',' } -> ';'
                else -> ','
            }
            listOf(name to csv(clean, sep))
        }
    }
}

/** RFC 4180 rows: quoted cells may hold the separator, line breaks and doubled quotes. */
private fun csv(text: String, sep: Char): List<List<String>> {
    val rows = ArrayList<List<String>>()
    var row = ArrayList<String>()
    val cell = StringBuilder()
    var quoted = false
    var i = 0
    fun endRow() {
        row += cell.toString()
        cell.clear()
        if (row.any { it.isNotEmpty() }) rows += row
        row = ArrayList()
    }
    while (i < text.length) {
        val c = text[i]
        when {
            quoted && c == '"' && text.getOrNull(i + 1) == '"' -> {
                cell.append('"')
                i++
            }
            c == '"' && (quoted || cell.isEmpty()) -> quoted = !quoted
            quoted -> cell.append(c)
            c == sep -> {
                row += cell.toString()
                cell.clear()
            }
            c == '\r' -> {
                if (text.getOrNull(i + 1) == '\n') i++
                endRow()
            }
            c == '\n' -> endRow()
            else -> cell.append(c)
        }
        i++
    }
    endRow()
    return rows
}

/** The file's name and size in a frosted island, with the way out beside it. */
@Composable
private fun Top(item: PreviewItem, haze: HazeState, modifier: Modifier, detail: String = "") {
    Box(modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Column(
            Modifier.align(Alignment.CenterStart).padding(end = 52.dp).heightIn(min = 44.dp)
                .glass(haze, Shapes.Card).padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            BasicText(item.name, style = Type.Meta.copy(color = Ink.One, fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(listOfNotNull(sizeOf(item.bytes).takeIf { item.bytes > 0 }, detail.ifEmpty { null }).joinToString(" · "), style = Type.Label)
        }
        Box(Modifier.align(Alignment.CenterEnd).size(44.dp).glass(haze, CircleShape)) {
            IconBtn(Glyph.Close, PreviewState::close, tint = Ink.One, fill = Color.Transparent, size = 44.dp, island = true)
        }
    }
}

/**
 * The floating island at the bottom: back and forth through the chat's files, and saving or
 * sharing what the phone holds of this one. A short confirmation rises above it.
 */
@Composable
private fun Dock(item: PreviewItem, found: Found?, haze: HazeState, modifier: Modifier, note: String? = null) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val album = PreviewState.album
    val at = album.indexOfFirst { it.key == item.key }
    var said by remember { mutableStateOf("") }
    var ok by remember { mutableStateOf(true) }
    var told by remember { mutableIntStateOf(0) }
    var telling by remember { mutableStateOf(false) }
    LaunchedEffect(told) {
        if (told == 0) return@LaunchedEffect
        telling = true
        delay(1800)
        telling = false
    }
    fun tell(text: String, good: Boolean) {
        view.feel(if (good) Feel.Confirm else Feel.Reject)
        said = text
        ok = good
        told++
    }
    val full = found?.full
    val pdf = found?.pdf
    val text = found?.text
    val send: (() -> Unit)? = when {
        full != null -> { { scope.launch { shareFile(ctx, item.name.substringBeforeLast('.') + ".jpg", "image/jpeg") { it.writeBytes(bytesOf(full)) } } } }
        pdf != null -> { { scope.launch { shareFile(ctx, item.name, "application/pdf") { pdf.copyTo(it, overwrite = true) } } } }
        text != null -> {
            val csv = Extract.ext(item.name) in setOf("csv", "tsv")
            val name = if (csv || !Extract.isDocument(item.name, "")) item.name else item.name.substringBeforeLast('.') + ".txt"
            ({ scope.launch { shareFile(ctx, name, if (csv) "text/csv" else "text/plain") { it.writeText(text) } } })
        }
        else -> null
    }
    val steps = at >= 0 && album.size > 1
    Column(modifier.navigationBarsPadding().padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(
            telling,
            enter = fadeIn(tween(Motion.M2)) + slideInVertically(spring(0.8f, 420f)) { it / 2 },
            exit = fadeOut(tween(Motion.M2)),
        ) {
            Row(
                Modifier.padding(bottom = 8.dp).glass(haze, CircleShape).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (ok) Glyph.Check else Glyph.Alert, if (ok) Signal.Ok else Signal.Err, size = 16.dp)
                Spacer(Modifier.width(8.dp))
                BasicText(said, style = Type.Meta.copy(color = Ink.One, fontWeight = FontWeight.Medium))
            }
        }
        if (note != null) BasicText(
            note,
            Modifier.padding(bottom = 8.dp).glass(haze, Shapes.Card).padding(horizontal = 16.dp, vertical = 10.dp),
            style = Type.Meta.copy(color = Ink.Two),
        )
        if (steps || full != null || send != null) Row(
            Modifier.glass(haze, CircleShape).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (steps) {
                IconBtn(Glyph.Left, { PreviewState.step(-1) }, tint = Ink.One, fill = Color.Transparent, size = 44.dp, enabled = at > 0, island = true)
                BasicText(
                    "${at + 1} / ${album.size}",
                    Modifier.padding(horizontal = 4.dp),
                    style = Type.Meta.copy(color = Ink.Two, fontFeatureSettings = "tnum"),
                )
                IconBtn(Glyph.Right, { PreviewState.step(1) }, tint = Ink.One, fill = Color.Transparent, size = 44.dp, enabled = at < album.size - 1, island = true)
            }
            if (full != null) Capsule(tr("Save", "Zapisz")) {
                scope.launch {
                    val saved = withContext(Dispatchers.IO) { savePicture(ctx, bytesOf(full), item.name) }
                    tell(
                        if (saved) tr("Saved to Photos", "Zapisano w Zdjęciach") else tr("Could not save the picture", "Nie udało się zapisać zdjęcia"),
                        saved,
                    )
                }
            }
            if (send != null) Capsule(tr("Share", "Udostępnij"), send)
        }
    }
}

private class Window(private val r: Rect, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Rounded(RoundRect(r, CornerRadius(radius)))
}

private fun around(c: Offset, w: Float, h: Float) = Rect(c.x - w / 2f, c.y - h / 2f, c.x + w / 2f, c.y + h / 2f)

/** The picture fitted into [box]; [natural] stops it at its own pixel size. */
private fun fit(box: Size, b: ImageBitmap, natural: Boolean = false): Rect {
    val s = min(box.width / b.width, box.height / b.height).let { if (natural) min(1f, it) else it }
    return around(box.center, b.width * s, b.height * s)
}

private fun cover(r: Rect, b: ImageBitmap): Rect {
    val s = max(r.width / b.width, r.height / b.height)
    return around(r.center, b.width * s, b.height * s)
}

/** How far the zoomed picture may move from centre before an edge shows. */
private fun bounds(f: Rect, z: Float, box: Size) =
    Offset(max(0f, (f.width * z - box.width) / 2f), max(0f, (f.height * z - box.height) / 2f))

private fun clamp(o: Offset, b: Offset) = Offset(o.x.coerceIn(-b.x, b.x), o.y.coerceIn(-b.y, b.y))

private fun band(v: Float, b: Float, reach: Float): Float {
    val inside = v.coerceIn(-b, b)
    return inside + rubber(v - inside, reach)
}

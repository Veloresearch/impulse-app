package com.velocity.impulse.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ResultPoint
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.detector.Detector
import com.velocity.impulse.ui.components.Btn
import com.velocity.impulse.ui.components.OnAccent
import com.velocity.impulse.ui.components.Variant
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

/** Where the code sits in the camera view, as fractions of the view's width and height. */
private data class Spot(val l: Float, val t: Float, val r: Float, val b: Float)

/**
 * The camera with a frame over it. The frame breathes while it waits, glides onto a code the
 * moment the reader sees one and follows it as the phone moves, then closes on it in the accent
 * with a tick before the app connects.
 */
@Composable
fun Scanner(onCode: (String) -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var allowed by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    LaunchedEffect(Unit) { if (!allowed) ask.launch(Manifest.permission.CAMERA) }

    var spot by remember { mutableStateOf<Spot?>(null) }
    var code by remember { mutableStateOf<String?>(null) }
    val hand = LocalHapticFeedback.current
    LaunchedEffect(code) {
        val c = code ?: return@LaunchedEffect
        hand.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(520)
        onCode(c)
        delay(1500)
        code = null
    }

    Box(modifier.fillMaxWidth().aspectRatio(0.92f).clip(Shapes.Panel).background(Tone.Inset), contentAlignment = Alignment.Center) {
        if (allowed) {
            Camera(onSpot = { spot = it }, onCode = { if (code == null) code = it }, paused = code != null)
            Frame(spot, code != null)
        } else {
            Column(
                Modifier.padding(Space.S5),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.S3),
            ) {
                BasicText("The camera reads the code. Nothing is recorded.", style = Type.Body.copy(color = Ink.Three, textAlign = TextAlign.Center))
                Btn("Allow camera", { ask.launch(Manifest.permission.CAMERA) }, variant = Variant.Primary)
            }
        }
    }
}

@Composable
private fun Camera(onSpot: (Spot?) -> Unit, onCode: (String) -> Unit, paused: Boolean) {
    val owner = LocalLifecycleOwner.current
    val worker = remember { Executors.newSingleThreadExecutor() }
    val hold = rememberUpdatedState(paused)
    val spotted = rememberUpdatedState(onSpot)
    val decoded = rememberUpdatedState(onCode)
    DisposableEffect(Unit) { onDispose { worker.shutdown() } }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { c ->
            val view = PreviewView(c).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val main = ContextCompat.getMainExecutor(c)
            val reader = QRCodeReader()
            val hints = mapOf(DecodeHintType.TRY_HARDER to true)
            var lastSeen = 0L
            val future = ProcessCameraProvider.getInstance(c)
            future.addListener({
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(worker) { img ->
                    if (!hold.value) {
                        val plane = img.planes[0]
                        val bytes = ByteArray(plane.buffer.remaining()).also { plane.buffer.get(it) }
                        val source = PlanarYUVLuminanceSource(bytes, plane.rowStride, img.height, 0, 0, img.width, img.height, false)
                        val bits = runCatching { HybridBinarizer(source).blackMatrix }.getOrNull()
                        val points = bits?.let { runCatching { Detector(it).detect(hints).points }.getOrNull() }
                        val now = System.currentTimeMillis()
                        val found = points?.takeIf { it.size >= 3 }
                            ?.let { place(it, img.width, img.height, img.imageInfo.rotationDegrees, view.width, view.height) }
                        if (found != null) lastSeen = now
                        if (found != null || now - lastSeen > 700) main.execute { spotted.value(found) }
                        if (points != null) {
                            runCatching { reader.decode(BinaryBitmap(HybridBinarizer(source)), hints) }.getOrNull()?.text?.let { t ->
                                main.execute { decoded.value(t) }
                            }
                            reader.reset()
                        }
                    }
                    img.close()
                }
                provider.unbindAll()
                provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }, main)
            view
        },
    )
}

/** Finder-pattern centres in the camera frame, turned into a square around the whole code. */
private fun place(points: Array<ResultPoint>, w: Int, h: Int, rotation: Int, vw: Int, vh: Int): Spot? {
    if (vw == 0 || vh == 0) return null
    val (iw, ih) = if (rotation % 180 == 0) w to h else h to w
    val scale = maxOf(vw.toFloat() / iw, vh.toFloat() / ih)
    val ox = (vw - iw * scale) / 2
    val oy = (vh - ih * scale) / 2
    val xs = ArrayList<Float>()
    val ys = ArrayList<Float>()
    for (p in points) {
        val (x, y) = when (rotation) {
            90 -> (h - p.y) to p.x
            180 -> (w - p.x) to (h - p.y)
            270 -> p.y to (w - p.x)
            else -> p.x to p.y
        }
        xs += x * scale + ox
        ys += y * scale + oy
    }
    val cx = (xs.min() + xs.max()) / 2
    val cy = (ys.min() + ys.max()) / 2
    // Finder centres sit three and a half modules in from the corners of the code.
    val half = maxOf(xs.max() - xs.min(), ys.max() - ys.min()) * 0.74f
    return Spot((cx - half) / vw, (cy - half) / vh, (cx + half) / vw, (cy + half) / vh)
}

@Composable
private fun Frame(spot: Spot?, locked: Boolean) {
    val accent = LocalAccent.current
    val t = rememberInfiniteTransition(label = "frame")
    val breath by t.animateFloat(0.6f, 0.64f, infiniteRepeatable(tween(1600, easing = Motion.Standard), RepeatMode.Reverse), label = "breath")
    val sweep by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = Motion.Standard), RepeatMode.Reverse), label = "sweep")
    val lock by animateFloatAsState(if (locked) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = 420f), label = "lock")
    val near by animateFloatAsState(if (spot != null) 1f else 0f, Motion.travel(), label = "near")
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val side = minOf(w, h) * breath
        val rest = Rect(Offset((w - side) / 2, (h - side) / 2), Size(side, side))
        val target = spot?.let { Rect(it.l * w, it.t * h, it.r * w, it.b * h) } ?: rest
        val box by animateRectAsState(target, spring(dampingRatio = 0.78f, stiffness = 340f), label = "box")
        Canvas(Modifier.fillMaxSize()) {
            val shrink = box.width * 0.05f * lock
            val o = Offset(box.left + shrink, box.top + shrink)
            val s = Size(box.width - 2 * shrink, box.height - 2 * shrink)
            val small = minOf(s.width, s.height)
            val arm = small * (0.18f + 0.08f * near)
            val r = small * 0.12f
            val ink = lerp(lerp(Color.White, accent, near * 0.5f), accent, lock)
            val shade = Path().apply {
                addRect(Rect(Offset.Zero, size))
                addRoundRect(RoundRect(o.x, o.y, o.x + s.width, o.y + s.height, CornerRadius(r)))
                fillType = PathFillType.EvenOdd
            }
            drawPath(shade, Color.Black.copy(alpha = 0.32f + 0.2f * near))
            for ((cx, cy) in listOf(0f to 0f, 1f to 0f, 1f to 1f, 0f to 1f)) {
                val x = o.x + cx * s.width
                val y = o.y + cy * s.height
                val dx = if (cx == 0f) 1f else -1f
                val dy = if (cy == 0f) 1f else -1f
                val corner = Path().apply {
                    moveTo(x, y + dy * arm)
                    lineTo(x, y + dy * r)
                    quadraticTo(x, y, x + dx * r, y)
                    lineTo(x + dx * arm, y)
                }
                drawPath(corner, ink, style = Stroke((4.5f + 1.5f * lock) * density, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            if (near < 0.99f && lock < 0.01f) {
                val y = o.y + s.height * (0.08f + 0.84f * sweep)
                val band = s.height * 0.09f
                drawRect(
                    Brush.verticalGradient(listOf(Color.Transparent, accent.copy(alpha = 0.32f * (1f - near)), Color.Transparent), y - band, y + band),
                    Offset(o.x + s.width * 0.06f, y - band), Size(s.width * 0.88f, band * 2),
                )
            }
            if (lock > 0.01f) {
                val c = Offset(o.x + s.width / 2, o.y + s.height / 2)
                val d = small * 0.22f * lock
                drawCircle(accent, d, c)
                val tick = Path().apply {
                    moveTo(c.x - d * 0.42f, c.y + d * 0.02f)
                    lineTo(c.x - d * 0.1f, c.y + d * 0.34f)
                    lineTo(c.x + d * 0.46f, c.y - d * 0.3f)
                }
                drawPath(tick, OnAccent, style = Stroke(d * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

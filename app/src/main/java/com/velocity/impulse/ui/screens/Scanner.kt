package com.velocity.impulse.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
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
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.components.Btn
import com.velocity.impulse.ui.components.Feel
import com.velocity.impulse.ui.components.OnAccent
import com.velocity.impulse.ui.components.Roll
import com.velocity.impulse.ui.components.Variant
import com.velocity.impulse.ui.components.feel
import com.velocity.impulse.ui.components.shake
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.LocalReduceMotion
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Signal
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/** Where the code sits in the camera view, as fractions of the view's width and height. */
private data class Spot(val l: Float, val t: Float, val r: Float, val b: Float)

/**
 * The camera with a frame over it. The frame breathes while it waits, glides onto a code the
 * moment the reader sees one and follows it as the phone moves. [onCode] judges what was read:
 * an Impulse code seals the frame in the accent with a check, anything else shakes it red.
 */
@Composable
fun Scanner(onCode: (String) -> Boolean, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var allowed by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    var awake by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(380)
        awake = true
        if (!allowed) ask.launch(Manifest.permission.CAMERA)
    }
    val live by animateFloatAsState(if (awake && allowed) 1f else 0f, tween(600, easing = Motion.Out), label = "live")

    var spot by remember { mutableStateOf<Spot?>(null) }
    var code by remember { mutableStateOf<String?>(null) }
    var verdict by remember { mutableIntStateOf(0) }
    var refused by remember { mutableIntStateOf(0) }
    // The last wrong code stays refused until it leaves the view, so holding it there buzzes once.
    var rejected by remember { mutableStateOf<String?>(null) }
    val judge by rememberUpdatedState(onCode)
    val view = LocalView.current
    LaunchedEffect(code) {
        val c = code ?: return@LaunchedEffect
        if (judge(c)) {
            rejected = null
            verdict = 1
            view.feel(Feel.Lock)
            delay(2000)
        } else {
            rejected = c
            verdict = -1
            refused++
            view.feel(Feel.Reject)
            delay(900)
        }
        verdict = 0
        code = null
    }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.S2)) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.92f).clip(Shapes.Panel).background(Tone.Inset), contentAlignment = Alignment.Center) {
            if (allowed && awake) {
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = live }) {
                    Camera(
                        onSpot = { spot = it; if (it == null) rejected = null },
                        onCode = { if (code == null && it != rejected) code = it },
                        paused = code != null,
                    )
                }
                Frame(spot, verdict, refused)
            } else if (awake) {
                Column(
                    Modifier.padding(Space.S5),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Space.S3),
                ) {
                    BasicText(
                        tr("The camera reads the code. Nothing is recorded.", "Aparat tylko odczytuje kod. Nic nie jest nagrywane."),
                        style = Type.Body.copy(color = Ink.Three, textAlign = TextAlign.Center),
                    )
                    Btn(tr("Allow camera", "Zezwól na aparat"), { ask.launch(Manifest.permission.CAMERA) }, variant = Variant.Primary)
                }
            }
        }
        Roll(if (rejected != null) tr("That code isn't from Impulse", "To nie jest kod z Impulse") else "", Type.Meta)
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

/**
 * Everything moves in the draw phase. [verdict] is 1 for an Impulse code (the corners grow into
 * one outline and a check draws itself), -1 for a wrong one (red and a shake), 0 while looking.
 */
@Composable
private fun Frame(spot: Spot?, verdict: Int, refused: Int) {
    val accent = LocalAccent.current
    val calm = LocalReduceMotion.current
    val view = LocalView.current
    val breath = rememberInfiniteTransition(label = "frame")
        .animateFloat(0.6f, 0.64f, infiniteRepeatable(tween(1600, easing = Motion.Standard), RepeatMode.Reverse), label = "breath")
    val near = remember { Animatable(0f) }
    val aim = remember { Animatable(Rect.Zero, Rect.VectorConverter) }
    val pinch = remember { Animatable(0f) }
    val seal = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    val wrong = remember { Animatable(0f) }
    val held = remember { booleanArrayOf(false) }
    val ticked = remember { longArrayOf(0L) }

    LaunchedEffect(spot) {
        if (spot == null) {
            held[0] = false
            near.animateTo(0f, spring(0.78f, 340f))
            return@LaunchedEffect
        }
        val r = Rect(spot.l, spot.t, spot.r, spot.b)
        if (!held[0]) {
            held[0] = true
            val now = SystemClock.uptimeMillis()
            if (now - ticked[0] > 700) {
                ticked[0] = now
                view.feel(Feel.Tick)
            }
            if (near.value < 0.05f) aim.snapTo(r)
        }
        coroutineScope {
            launch { aim.animateTo(r, spring(0.82f, 520f)) }
            near.animateTo(1f, spring(0.78f, 340f))
        }
    }
    LaunchedEffect(verdict) {
        when (verdict) {
            1 -> coroutineScope {
                launch { pinch.animateTo(1f, spring(0.55f, 420f)) }
                launch { seal.animateTo(1f, tween(260, easing = Motion.Out)) }
                check.animateTo(1f, tween(240, delayMillis = 80, easing = Motion.Out))
            }
            -1 -> wrong.animateTo(1f, tween(Motion.M1))
            else -> {
                pinch.snapTo(0f)
                seal.snapTo(0f)
                check.snapTo(0f)
                wrong.animateTo(0f, tween(Motion.M2))
            }
        }
    }

    val corner = remember { Path() }
    val shade = remember { Path() }
    val tick = remember { Path() }
    val drawn = remember { Path() }
    val measure = remember { PathMeasure() }
    Canvas(Modifier.fillMaxSize().shake(refused)) {
        val w = size.width
        val h = size.height
        val side = minOf(w, h) * breath.value
        val rest = Rect(Offset((w - side) / 2, (h - side) / 2), Size(side, side))
        val a = aim.value
        val k = near.value
        val box = lerp(rest, Rect(a.left * w, a.top * h, a.right * w, a.bottom * h), k)
        val p = seal.value
        val shrink = if (calm) 0f else box.width * 0.05f * pinch.value
        val o = Offset(box.left + shrink, box.top + shrink)
        val s = Size(box.width - 2 * shrink, box.height - 2 * shrink)
        val small = minOf(s.width, s.height)
        val arm = small * if (calm) 0.2f else 0.2f + 0.3f * p
        val r = small * 0.16f

        val dim = 0.35f * k.coerceIn(0f, 1f)
        shade.reset()
        shade.fillType = PathFillType.EvenOdd
        shade.addRect(Rect(Offset.Zero, size))
        shade.addRoundRect(RoundRect(Rect(o, s), CornerRadius(r)))
        drawPath(shade, Color.Black, alpha = dim + (0.55f - dim) * p)

        val ink = lerp(lerp(Color.White, accent, p), Signal.Err, wrong.value)
        val line = Stroke((3f + 1f * pinch.value) * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        for ((cx, cy) in Corners) {
            val x = o.x + cx * s.width
            val y = o.y + cy * s.height
            val dx = if (cx == 0f) 1f else -1f
            val dy = if (cy == 0f) 1f else -1f
            corner.reset()
            corner.moveTo(x, y + dy * arm)
            corner.lineTo(x, y + dy * r)
            corner.quadraticTo(x, y, x + dx * r, y)
            corner.lineTo(x + dx * arm, y)
            drawPath(corner, ink.copy(alpha = 0.92f), style = line)
        }

        if (p > 0.01f) {
            val c = Offset(o.x + s.width / 2, o.y + s.height / 2)
            val d = small * 0.22f * if (calm) 1f else pinch.value
            drawCircle(accent, d, c, alpha = if (calm) p else 1f)
            tick.reset()
            tick.moveTo(c.x - d * 0.42f, c.y + d * 0.02f)
            tick.lineTo(c.x - d * 0.1f, c.y + d * 0.34f)
            tick.lineTo(c.x + d * 0.46f, c.y - d * 0.3f)
            val stroke = Stroke(d * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            if (calm) {
                drawPath(tick, OnAccent, alpha = check.value, style = stroke)
            } else if (check.value > 0f) {
                measure.setPath(tick, false)
                drawn.reset()
                measure.getSegment(0f, measure.length * check.value, drawn, true)
                drawPath(drawn, OnAccent, style = stroke)
            }
        }
    }
}

private val Corners = listOf(0f to 0f, 1f to 0f, 1f to 1f, 0f to 1f)

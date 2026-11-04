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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import java.util.concurrent.Executors

/** The camera, a breathing frame over it, and a QR reader on every frame until one decodes. */
@Composable
fun Scanner(onCode: (String) -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var allowed by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    LaunchedEffect(Unit) { if (!allowed) ask.launch(Manifest.permission.CAMERA) }

    Box(modifier.fillMaxWidth().aspectRatio(1f).clip(Shapes.Card).background(Tone.Inset), contentAlignment = Alignment.Center) {
        if (allowed) Camera(onCode) else BasicText(
            "Impulse needs the camera to read the code. Nothing is recorded.",
            Modifier.padding(Space.S5),
            style = Type.Body.copy(color = Ink.Three),
        )
        Frame()
    }
}

@Composable
private fun Camera(onCode: (String) -> Unit) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val hand = LocalHapticFeedback.current
    val worker = remember { Executors.newSingleThreadExecutor() }
    var done by remember { mutableStateOf(false) }
    val reader = remember {
        MultiFormatReader().apply { setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE), DecodeHintType.TRY_HARDER to true)) }
    }
    DisposableEffect(Unit) { onDispose { worker.shutdown() } }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { c ->
            val view = PreviewView(c).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val future = ProcessCameraProvider.getInstance(c)
            future.addListener({
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(worker) { img ->
                    if (!done) {
                        val plane = img.planes[0]
                        val bytes = ByteArray(plane.buffer.remaining()).also { plane.buffer.get(it) }
                        val source = PlanarYUVLuminanceSource(bytes, plane.rowStride, img.height, 0, 0, img.width, img.height, false)
                        runCatching { reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))) }.getOrNull()?.text?.let { text ->
                            done = true
                            ContextCompat.getMainExecutor(ctx).execute {
                                hand.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCode(text)
                            }
                        }
                        reader.reset()
                    }
                    img.close()
                }
                provider.unbindAll()
                provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }, ContextCompat.getMainExecutor(c))
            view
        },
    )
}

@Composable
private fun Frame() {
    val accent = LocalAccent.current
    val breath by rememberInfiniteTransition(label = "frame").animateFloat(
        0.62f, 0.66f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "breath",
    )
    Canvas(Modifier.fillMaxSize()) {
        val s = size.minDimension * breath
        val o = Offset((size.width - s) / 2, (size.height - s) / 2)
        drawRoundRect(Color.Black.copy(alpha = 0.18f), size = size)
        drawRoundRect(accent, o, Size(s, s), CornerRadius(s * 0.12f), style = Stroke(width = 3.5f * density))
    }
}

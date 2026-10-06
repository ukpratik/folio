// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.capture

import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.ukpratik.folio.core.model.Quad
import io.github.ukpratik.folio.core.ui.geometry.FitRect
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import timber.log.Timber

private val OutlineColor = Color(0xFF3FC7B5)

/** 4:3 everywhere so preview, analysis and capture show the same field of view (overlay maps 1:1). */
private val FourByThree = AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY

/** Imperative CameraX handles the screen needs (capture, torch). */
class CameraController internal constructor(
    private val capture: ImageCapture,
    private val camera: Camera,
    private val executor: ExecutorService,
) {
    val hasTorch: Boolean get() = camera.cameraInfo.hasFlashUnit()

    fun setTorch(on: Boolean) {
        if (hasTorch) camera.cameraControl.enableTorch(on)
    }

    fun takePicture(target: File, onSaved: (File) -> Unit, onError: (Exception) -> Unit) {
        capture.takePicture(
            ImageCapture.OutputFileOptions.Builder(target).build(),
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) = onSaved(target)
                override fun onError(exception: ImageCaptureException) = onError(exception)
            },
        )
    }
}

/**
 * S2b: CameraX preview (FIT_CENTER, 4:3) with the live document outline drawn on top. Binds to the
 * composition's lifecycle and unbinds when it leaves.
 */
@Composable
internal fun CameraPreview(
    analyzer: DocumentFrameAnalyzer,
    quad: Quad?,
    onReady: (CameraController) -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(lifecycleOwner) {
        try {
            val provider = ProcessCameraProvider.awaitInstance(context)
            val preview = Preview.Builder()
                .setResolutionSelector(ResolutionSelector.Builder().setAspectRatioStrategy(FourByThree).build())
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(selector(Size(640, 480)))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(executor, analyzer) }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                // ~12 MP cap keeps memory bounded on 3–4 GB phones (ADR-0008).
                .setResolutionSelector(selector(Size(4000, 3000)))
                .build()
            provider.unbindAll()
            val camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis, capture)
            onReady(CameraController(capture, camera, executor))
        } catch (e: Exception) {
            Timber.w(e, "Camera unavailable")
            onUnavailable()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
            analyzer.reset()
            executor.shutdown()
        }
    }

    Box(modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        DocumentOutline(quad)
    }
}

private fun selector(target: Size) = ResolutionSelector.Builder()
    .setAspectRatioStrategy(FourByThree)
    .setResolutionStrategy(ResolutionStrategy(target, ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER))
    .build()

/** The outline in the same 3:4 (portrait) fit rect PreviewView uses with FIT_CENTER. */
@Composable
private fun DocumentOutline(quad: Quad?) {
    Canvas(Modifier.fillMaxSize()) {
        val q = quad ?: return@Canvas
        val rect = FitRect.fit(3, 4, size.width, size.height)
        val path = Path().apply {
            listOf(q.tl, q.tr, q.br, q.bl).forEachIndexed { i, p ->
                val o = Offset(rect.left + p.x * rect.width, rect.top + p.y * rect.height)
                if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
            }
            close()
        }
        drawPath(path, OutlineColor.copy(alpha = 0.14f))
        drawPath(path, OutlineColor, style = Stroke(width = 3.dp.toPx()))
        listOf(q.tl, q.tr, q.br, q.bl).forEach { p ->
            drawCircle(OutlineColor, radius = 7.dp.toPx(), center = Offset(rect.left + p.x * rect.width, rect.top + p.y * rect.height))
        }
    }
}

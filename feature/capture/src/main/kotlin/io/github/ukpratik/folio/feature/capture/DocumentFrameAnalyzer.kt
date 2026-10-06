// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.capture

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import io.github.ukpratik.folio.core.model.Quad
import io.github.ukpratik.folio.core.processing.detect.EdgeDetector
import io.github.ukpratik.folio.core.processing.detect.GrayImage
import io.github.ukpratik.folio.core.processing.detect.QuadSmoother
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/** What the camera overlay shows: a steadied document outline (display orientation) and a lighting hint. */
data class FrameState(val quad: Quad? = null, val tooDark: Boolean = false, val sensorRotation: Int = 0)

/**
 * Live detection for the camera preview (FR-03, LLD §5.3). Runs on CameraX's single analysis thread with
 * KEEP_ONLY_LATEST back-pressure, using the Y plane directly (no RGB conversion).
 */
class DocumentFrameAnalyzer @Inject constructor(private val detector: EdgeDetector) : ImageAnalysis.Analyzer {
    private val smoother = QuadSmoother()
    private val _state = MutableStateFlow(FrameState())
    val state: StateFlow<FrameState> = _state.asStateFlow()

    override fun analyze(image: ImageProxy) {
        try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val luma = ByteArray(buffer.remaining()).also { buffer.get(it) }
            val gray = GrayImage(luma, image.width, image.height, plane.rowStride)
            val rotation = image.imageInfo.rotationDegrees
            val detected = try {
                detector.detect(gray)?.quad
            } catch (e: Exception) {
                Timber.w(e, "Frame detection failed")
                null
            }
            val steady = smoother.update(detected)
            _state.value = FrameState(
                quad = steady?.let { FrameGeometry.rotate(it, rotation) },
                tooDark = FrameGeometry.meanLuma(luma, image.width, image.height, plane.rowStride) < DARK_LUMA,
                sensorRotation = rotation,
            )
        } finally {
            image.close()
        }
    }

    fun reset() {
        smoother.reset()
        _state.value = FrameState()
    }

    private companion object {
        /** Mean luma below this → "More light needed". */
        const val DARK_LUMA = 45.0
    }
}

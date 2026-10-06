// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.processing.detect.GrayImage
import io.github.ukpratik.folio.core.processing.detect.OpenCvEdgeDetector
import io.github.ukpratik.folio.core.processing.detect.Px
import io.github.ukpratik.folio.core.processing.detect.QuadGeometry
import io.github.ukpratik.folio.core.processing.opencv.OpenCvRuntime
import org.junit.Test
import org.junit.runner.RunWith

/** Spike S1 checks on synthetic images (the real-photo set is QA's T01–T14). */
@RunWith(AndroidJUnit4::class)
class OpenCvEdgeDetectorTest {
    private val detector = OpenCvEdgeDetector(OpenCvRuntime())
    private val width = 900
    private val height = 1200

    private fun assertCornersNear(expected: List<Px>, actual: io.github.ukpratik.folio.core.model.Quad) {
        val tolerance = 0.03 // of the frame diagonal (S1 exit criterion)
        val expectedQuad = QuadGeometry.toQuad(expected, width, height)
        com.google.common.truth.Truth.assertWithMessage("expected %s but detected %s", expectedQuad, actual)
            .that(QuadGeometry.maxCornerDistance(expectedQuad, actual)).isLessThan(tolerance)
    }

    @Test fun findsTiltedPageOnDarkTable() {
        val corners = listOf(Px(160.0, 140.0), Px(760.0, 100.0), Px(800.0, 1060.0), Px(110.0, 1100.0))
        val detection = detector.detect(SyntheticImages.documentPhoto(width, height, corners))
        assertThat(detection).isNotNull()
        assertCornersNear(corners, detection!!.quad)
    }

    @Test fun findsPerspectivePage() {
        val corners = listOf(Px(250.0, 200.0), Px(650.0, 200.0), Px(820.0, 1100.0), Px(80.0, 1100.0))
        val detection = detector.detect(SyntheticImages.documentPhoto(width, height, corners))
        assertThat(detection).isNotNull()
        assertCornersNear(corners, detection!!.quad)
    }

    @Test fun findsPageOnLightTable() {
        val corners = listOf(Px(150.0, 150.0), Px(750.0, 150.0), Px(750.0, 1050.0), Px(150.0, 1050.0))
        val light = Color.rgb(200, 196, 188)
        val detection = detector.detect(SyntheticImages.documentPhoto(width, height, corners, table = light))
        assertThat(detection).isNotNull()
        assertCornersNear(corners, detection!!.quad)
    }

    @Test fun ignoresScreenshotsAndPlainPhotos() {
        assertThat(detector.detect(SyntheticImages.screenshot(width, height))).isNull()
        assertThat(detector.detect(SyntheticImages.gradient(width, height))).isNull()
    }

    @Test fun worksOnACameraLumaPlaneWithRowPadding() {
        val corners = listOf(Px(100.0, 80.0), Px(540.0, 60.0), Px(560.0, 420.0), Px(80.0, 440.0))
        val bmp = SyntheticImages.documentPhoto(640, 480, corners)
        val stride = 704 // camera buffers often pad rows
        val luma = ByteArray(stride * 480)
        for (y in 0 until 480) for (x in 0 until 640) {
            val c = bmp.getPixel(x, y)
            luma[y * stride + x] = ((Color.red(c) * 299 + Color.green(c) * 587 + Color.blue(c) * 114) / 1000).toByte()
        }
        val detection = detector.detect(GrayImage(luma, 640, 480, stride))
        assertThat(detection).isNotNull()
        val expected = QuadGeometry.toQuad(corners, 640, 480)
        assertThat(QuadGeometry.maxCornerDistance(expected, detection!!.quad)).isLessThan(0.03)
    }
}

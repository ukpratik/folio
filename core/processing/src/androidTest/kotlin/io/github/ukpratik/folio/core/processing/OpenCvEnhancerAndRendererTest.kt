// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.model.Adjustments
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.EnhancementMode
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.Rotation
import io.github.ukpratik.folio.core.processing.detect.Px
import io.github.ukpratik.folio.core.processing.detect.QuadGeometry
import io.github.ukpratik.folio.core.processing.enhance.OpenCvEnhancer
import io.github.ukpratik.folio.core.processing.opencv.OpenCvRuntime
import io.github.ukpratik.folio.core.processing.render.OpenCvPageRenderer
import io.github.ukpratik.folio.core.processing.render.RenderRequest
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

@RunWith(AndroidJUnit4::class)
class OpenCvEnhancerAndRendererTest {
    private val runtime = OpenCvRuntime().also { it.ensureLoaded() }
    private val enhancer = OpenCvEnhancer(runtime)
    private val renderer = OpenCvPageRenderer(runtime, enhancer, Dispatchers.Default)

    private fun rgbOf(bitmap: Bitmap): Mat {
        val rgba = Mat()
        Utils.bitmapToMat(bitmap, rgba)
        return Mat().also { Imgproc.cvtColor(rgba, it, Imgproc.COLOR_RGBA2RGB); rgba.release() }
    }

    @Test fun blackAndWhiteIsBinary() {
        val src = rgbOf(SyntheticImages.washedOut(300, 400))
        val bw = enhancer.enhance(src, EnhancementMode.BW, Adjustments())
        assertThat(bw.channels()).isEqualTo(1)
        val hist = IntArray(256)
        for (y in 0 until bw.rows()) for (x in 0 until bw.cols()) hist[bw.get(y, x)[0].toInt()]++
        assertThat((1..254).sumOf { hist[it] }).isEqualTo(0)
        src.release(); bw.release()
    }

    @Test fun autoStretchesLowContrast() {
        val src = rgbOf(SyntheticImages.washedOut(300, 400))
        val auto = enhancer.enhance(src, EnhancementMode.AUTO, Adjustments())
        val gray = Mat().also { Imgproc.cvtColor(auto, it, Imgproc.COLOR_RGB2GRAY) }
        val range = Core.minMaxLoc(gray)
        assertThat(range.maxVal - range.minVal).isGreaterThan(180.0) // was 50
        listOf(src, auto, gray).forEach(Mat::release)
    }

    @Test fun grayscaleIsSingleChannelAndAdjustmentsBrighten() {
        val src = rgbOf(SyntheticImages.washedOut(100, 100))
        val gray = enhancer.enhance(src, EnhancementMode.GRAYSCALE, Adjustments())
        assertThat(gray.channels()).isEqualTo(1)
        val plain = enhancer.enhance(src, EnhancementMode.ORIGINAL, Adjustments())
        val bright = enhancer.enhance(src, EnhancementMode.ORIGINAL, Adjustments(brightness = 0.5f))
        assertThat(Core.mean(bright).`val`[0]).isGreaterThan(Core.mean(plain).`val`[0] + 20)
        listOf(src, gray, plain, bright).forEach(Mat::release)
    }

    @Test fun rendererCropsRotatesAndRespectsTargetSize() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val corners = listOf(Px(150.0, 150.0), Px(750.0, 150.0), Px(750.0, 1050.0), Px(150.0, 1050.0))
        val source = File(context.cacheDir, "render-src.jpg").apply {
            outputStream().use { SyntheticImages.documentPhoto(900, 1200, corners).compress(Bitmap.CompressFormat.JPEG, 95, it) }
        }
        val page = Page(
            id = PageId("p"), documentId = DocumentId("d"), order = 0, sourceId = "s",
            corners = QuadGeometry.toQuad(corners, 900, 1200), rotation = Rotation.R90, mode = EnhancementMode.AUTO,
        )

        val out = renderer.render(RenderRequest(page, source, targetLongEdgePx = 500, forExport = true))

        // 600×900 crop, rotated → landscape, scaled so the long edge is 500.
        assertThat(out.width).isEqualTo(500)
        assertThat(out.height).isWithin(2).of(333)
        // The dark table must be gone: the centre and edges are page-coloured.
        assertThat(Color.red(out.getPixel(5, out.height / 2))).isGreaterThan(150)
        out.recycle()
    }

    @Test fun previewRendersReuseTheCachedBase() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "render-src2.jpg").apply {
            outputStream().use { SyntheticImages.screenshot(800, 1000).compress(Bitmap.CompressFormat.JPEG, 90, it) }
        }
        val page = Page(PageId("p"), DocumentId("d"), 0, "s", mode = EnhancementMode.ORIGINAL)
        val first = renderer.render(RenderRequest(page, source, 300, forExport = false))
        source.delete() // a second preview must come from the cache
        val second = renderer.render(RenderRequest(page.copy(rotation = Rotation.R180), source, 300, forExport = false))
        assertThat(second.width).isEqualTo(first.width)
        first.recycle(); second.recycle()
    }
}

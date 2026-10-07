// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing

import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.model.ByteSize
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.EnhancementMode
import io.github.ukpratik.folio.core.model.ExportSettings
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.model.QualityPreset
import io.github.ukpratik.folio.core.processing.detect.Px
import io.github.ukpratik.folio.core.processing.detect.QuadGeometry
import io.github.ukpratik.folio.core.processing.encode.SizeOptimizer
import io.github.ukpratik.folio.core.processing.enhance.OpenCvEnhancer
import io.github.ukpratik.folio.core.processing.export.ExportRasterizer
import io.github.ukpratik.folio.core.processing.opencv.OpenCvRuntime
import io.github.ukpratik.folio.core.processing.pdf.PageGeometry
import io.github.ukpratik.folio.core.processing.pdf.PdfPage
import io.github.ukpratik.folio.core.processing.pdf.StreamingPdfWriter
import io.github.ukpratik.folio.core.processing.render.OpenCvPageRenderer
import io.github.ukpratik.folio.core.testing.FakeDocumentFiles
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/** Real renderer + encoder + optimiser + writer on 10 photographed-looking pages (spike S2, NFR-04). */
@RunWith(AndroidJUnit4::class)
class ExportEndToEndTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val files = FakeDocumentFiles(File(context.cacheDir, "e2e").apply { deleteRecursively(); mkdirs() })
    private val runtime = OpenCvRuntime().also { it.ensureLoaded() }
    private val renderer = OpenCvPageRenderer(runtime, OpenCvEnhancer(runtime), Dispatchers.Default)
    private val doc = DocumentId("doc")

    private val pages: List<Page> by lazy {
        val corners = listOf(Px(200.0, 180.0), Px(1800.0, 140.0), Px(1860.0, 2460.0), Px(140.0, 2500.0))
        (0 until 10).map { i ->
            val source = files.sourceFile(doc, "s$i").apply { parentFile!!.mkdirs() }
            source.outputStream().use { SyntheticImages.documentPhoto(2000, 2600, corners).compress(Bitmap.CompressFormat.JPEG, 92, it) }
            Page(
                PageId("p$i"), doc, i, "s$i",
                corners = QuadGeometry.toQuad(corners, 2000, 2600),
                mode = if (i % 3 == 0) EnhancementMode.BW else EnhancementMode.AUTO,
                status = PageStatus.READY,
            )
        }
    }

    /** Returns (pdf bytes, milliseconds, targetMet). */
    private fun export(settings: ExportSettings, pages: List<Page> = this.pages): Triple<Long, Long, Boolean?> = runBlocking {
        val start = SystemClock.elapsedRealtime()
        val encoded = mutableListOf<Pair<File, io.github.ukpratik.folio.core.processing.encode.EncodedPage>>()
        val budget = settings.target?.bytes?.let { it - StreamingPdfWriter.overheadBytes(pages.size, 40) }
        val met = SizeOptimizer(ExportRasterizer(renderer, files, settings)).encode(
            pages, settings.quality, budget, perImage = false,
            sink = { page -> encoded += files.newWorkFile("page", "jpg").apply { writeBytes(page.jpeg) } to page },
            progress = { _, _ -> },
        )
        val pdf = files.outputFile(doc, "out.pdf").apply { parentFile!!.mkdirs() }
        pdf.outputStream().use { out ->
            StreamingPdfWriter().write(out, "E2E", encoded.map { (f, p) ->
                PdfPage(PageGeometry.layout(p.widthPx, p.heightPx, p.dpi, settings.pageSize, settings.orientation, settings.margin), f)
            })
        }
        val ms = SystemClock.elapsedRealtime() - start
        Triple(pdf.length(), ms, met)
    }

    @Test fun balancedTenPagesWithinTimeBudget() {
        val (size, ms, _) = export(ExportSettings(quality = QualityPreset.BALANCED))
        Log.i("FolioE2E", "Balanced 10 pages: ${size / 1000} KB in $ms ms")
        assertThat(ms).isLessThan(10_000) // NFR-04
    }

    @Test fun meetsCommonPortalLimits() {
        // These synthetic pages are photo-noisy (~82 KB each at the 100 dpi / q35 floor), so page counts are
        // scaled to stay achievable; real scans of printed documents compress far smaller.
        val cases = listOf(100L to pages.take(1), 500L to pages.take(5), 2_000L to pages)
        for ((limitKb, subset) in cases) {
            val (size, ms, met) = export(ExportSettings(quality = QualityPreset.HIGH, target = ByteSize.kb(limitKb)), subset)
            Log.i("FolioE2E", "Target $limitKb KB, ${subset.size} pages: ${size / 1000} KB in $ms ms (met=$met)")
            assertThat(met).isTrue()
            assertThat(size).isAtMost(limitKb * 1000)
            assertThat(ms).isLessThan(15_000)
        }
    }

    @Test fun impossibleTargetIsReportedQuickly() {
        val (size, ms, met) = export(ExportSettings(quality = QualityPreset.HIGH, target = ByteSize.kb(100)))
        Log.i("FolioE2E", "Impossible 100 KB, 10 pages: ${size / 1000} KB in $ms ms (met=$met)")
        assertThat(met).isFalse()
        assertThat(ms).isLessThan(15_000)
    }
}

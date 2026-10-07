// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing

import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.QualityPreset
import io.github.ukpratik.folio.core.processing.encode.EncodedPage
import io.github.ukpratik.folio.core.processing.encode.PageRasterizer
import io.github.ukpratik.folio.core.processing.encode.QualitySearch
import io.github.ukpratik.folio.core.processing.encode.RasterPage
import io.github.ukpratik.folio.core.processing.encode.SizeOptimizer
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SizeOptimizerTest {
    /** Fake page: encoded size grows with DPI² and with quality, scaled by a per-page "complexity". */
    private class FakeRaster(val dpi: Int, val complexity: Double) : RasterPage {
        override val widthPx = dpi * 8
        override val heightPx = dpi * 11
        var closed = false
        override fun encode(quality: Int) = ByteArray((widthPx * heightPx * complexity * (0.05 + quality / 100.0) / 10).toInt())
        override fun close() { closed = true }
    }

    private val rasters = mutableListOf<FakeRaster>()
    private val rasterizer = PageRasterizer { page, dpi ->
        FakeRaster(dpi, if (page.id.value == "busy") 3.0 else 1.0).also { rasters += it }
    }
    private val optimizer = SizeOptimizer(rasterizer)
    private fun pages(vararg ids: String) = ids.mapIndexed { i, id -> Page(PageId(id), DocumentId("d"), i, "s$i") }

    private suspend fun run(pages: List<Page>, preset: QualityPreset, target: Long?, perImage: Boolean = false): Pair<List<EncodedPage>, Boolean?> {
        val out = mutableListOf<EncodedPage>()
        val met = optimizer.encode(pages, preset, target, perImage, sink = { out += it }, progress = { _, _ -> })
        return out to met
    }

    @Test fun noTargetUsesThePreset() = runTest {
        val (out, met) = run(pages("a", "b"), QualityPreset.BALANCED, target = null)
        assertThat(met).isNull()
        assertThat(out.map { it.dpi to it.quality }).containsExactly(200 to 75, 200 to 75)
        assertThat(rasters.all { it.closed }).isTrue()
    }

    @Test fun meetsATightTargetAndKeepsOrder() = runTest {
        // Floor (100 dpi, q35) for these three pages is ~176 KB, so 200 KB is tight but achievable.
        val target = 200_000L
        val (out, met) = run(pages("a", "busy", "c"), QualityPreset.HIGH, target)
        assertThat(met).isTrue()
        assertThat(out.sumOf { it.jpeg.size.toLong() }).isAtMost(target)
        assertThat(out.map { it.index }).containsExactly(0, 1, 2).inOrder()
        assertThat(out.first().dpi).isLessThan(300) // had to step down the ladder
    }

    @Test fun busyPagesGetABiggerShare() = runTest {
        val (out, _) = run(pages("a", "busy"), QualityPreset.BALANCED, target = 400_000L)
        assertThat(out[1].jpeg.size).isGreaterThan(out[0].jpeg.size)
    }

    @Test fun impossibleTargetReturnsTheFloorAndReportsMiss() = runTest {
        val (out, met) = run(pages("a", "b"), QualityPreset.BALANCED, target = 1_000L)
        assertThat(met).isFalse()
        assertThat(out.map { it.dpi to it.quality }.toSet()).containsExactly(100 to SizeOptimizer.MIN_QUALITY)
    }

    @Test fun perImageTargetAppliesToEachFile() = runTest {
        val (out, met) = run(pages("a", "busy"), QualityPreset.BALANCED, target = 120_000L, perImage = true)
        assertThat(met).isTrue()
        assertThat(out.all { it.jpeg.size <= 120_000 }).isTrue()
    }

    @Test fun qualitySearchFindsHighestFittingQuality() {
        val (q, bytes) = QualitySearch.highestFitting(35, 88, budget = 60) { ByteArray(it) }!!
        assertThat(q).isEqualTo(60)
        assertThat(bytes.size).isEqualTo(60)
        assertThat(QualitySearch.highestFitting(35, 88, budget = 10) { ByteArray(it) }).isNull()
    }
}

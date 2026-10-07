// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.encode

import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.QualityPreset
import kotlin.math.max

/** A page rendered at some DPI, encodable at any JPEG quality. Closing frees its pixels. */
interface RasterPage : AutoCloseable {
    val widthPx: Int
    val heightPx: Int
    fun encode(quality: Int): ByteArray
}

/** Renders a page (with its edits) for export at a given DPI. Real one: renderer + Bitmap.compress. */
fun interface PageRasterizer {
    suspend fun rasterize(page: Page, dpi: Int): RasterPage
}

/** One encoded page, handed to the sink immediately so only one page's bytes are ever in memory. */
data class EncodedPage(val index: Int, val jpeg: ByteArray, val widthPx: Int, val heightPx: Int, val dpi: Int, val quality: Int)

enum class OptimizerPhase { PLANNING, ENCODING, SHRINKING }

/**
 * ADR-0011. With no target: each page at its preset. With a target: a cheap low-DPI pass weighs pages by
 * complexity, the budget is split by weight, then each page gets the highest quality that fits its share
 * (binary search), stepping DPI down the ladder only when needed; unused budget carries forward.
 */
class SizeOptimizer(private val rasterizer: PageRasterizer) {

    /** [targetBytes] is for all pages together (PDF) unless [perImage] (JPG export: each file ≤ target). */
    suspend fun encode(
        pages: List<Page>,
        preset: QualityPreset,
        targetBytes: Long?,
        perImage: Boolean,
        sink: suspend (EncodedPage) -> Unit,
        progress: suspend (phase: OptimizerPhase, done: Int) -> Unit,
    ): Boolean? {
        if (targetBytes == null) {
            pages.forEachIndexed { i, page ->
                rasterizer.rasterize(page, preset.dpi).use { raster ->
                    sink(EncodedPage(i, raster.encode(preset.jpegQuality), raster.widthPx, raster.heightPx, preset.dpi, preset.jpegQuality))
                }
                progress(OptimizerPhase.ENCODING, i + 1)
            }
            return null
        }
        if (perImage) {
            var allMet = true
            pages.forEachIndexed { i, page ->
                val out = encodeToBudget(i, page, targetBytes, preset) { progress(OptimizerPhase.SHRINKING, i) }
                allMet = allMet && out.jpeg.size <= targetBytes
                sink(out)
                progress(OptimizerPhase.ENCODING, i + 1)
            }
            return allMet
        }

        // Pass 1 (cheap): a low-DPI render of each page, encoded at the preset quality (weighs the budget by
        // complexity) and at the minimum quality (predicts which DPIs can possibly fit).
        val sampleDpi = max(MIN_SAMPLE_DPI, preset.dpi / 2)
        val samples = pages.mapIndexed { i, page ->
            rasterizer.rasterize(page, sampleDpi).use { Sample(it.encode(preset.jpegQuality).size, it.encode(MIN_QUALITY).size) }
                .also { progress(OptimizerPhase.PLANNING, i + 1) }
        }
        val totalWeight = samples.sumOf { it.atPreset.toDouble() }.coerceAtLeast(1.0)
        var carry = 0L
        var total = 0L
        pages.forEachIndexed { i, page ->
            val share = (targetBytes * (samples[i].atPreset / totalWeight)).toLong()
            val budget = share + carry
            val ladder = feasibleLadder(preset, budget, samples[i].atMinQuality, sampleDpi)
            val out = encodeToBudget(i, page, budget, preset, ladder) { progress(OptimizerPhase.SHRINKING, i) }
            carry = budget - out.jpeg.size
            total += out.jpeg.size
            sink(out)
            progress(OptimizerPhase.ENCODING, i + 1)
        }
        return total <= targetBytes
    }

    private data class Sample(val atPreset: Int, val atMinQuality: Int)

    /**
     * JPEG size scales roughly with pixel count (DPI²). Skip DPIs whose predicted minimum-quality size can't
     * fit (with a margin for prediction error); if none can, go straight to the floor.
     */
    private fun feasibleLadder(preset: QualityPreset, budget: Long, minAtSample: Int, sampleDpi: Int): List<Int> {
        val ladder = ladderFor(preset)
        val feasible = ladder.filter { dpi ->
            val scale = (dpi.toDouble() / sampleDpi).let { it * it }
            minAtSample * scale <= budget * PREDICTION_MARGIN
        }
        return feasible.ifEmpty { listOf(ladder.last()) }
    }

    private fun ladderFor(preset: QualityPreset) = DPI_LADDER.filter { it <= preset.dpi }.ifEmpty { listOf(preset.dpi) }

    private suspend fun encodeToBudget(
        index: Int,
        page: Page,
        budget: Long,
        preset: QualityPreset,
        ladder: List<Int> = ladderFor(preset),
        onShrink: suspend () -> Unit,
    ): EncodedPage {
        var smallest: EncodedPage? = null
        ladder.forEachIndexed { step, dpi ->
            if (step > 0 || dpi < preset.dpi) onShrink()
            rasterizer.rasterize(page, dpi).use { raster ->
                QualitySearch.highestFitting(MIN_QUALITY, preset.jpegQuality, budget, raster::encode)?.let { (quality, bytes) ->
                    return EncodedPage(index, bytes, raster.widthPx, raster.heightPx, dpi, quality)
                }
                // Readability floor reached: keep the smallest version (no second render); the UI explains (FR-18).
                if (step == ladder.lastIndex) {
                    smallest = EncodedPage(index, raster.encode(MIN_QUALITY), raster.widthPx, raster.heightPx, dpi, MIN_QUALITY)
                }
            }
        }
        return checkNotNull(smallest)
    }

    companion object {
        /** Quality never goes below this (ADR-0011). */
        const val MIN_QUALITY = 35

        /** 100 dpi is the readability floor. */
        val DPI_LADDER = listOf(300, 200, 150, 120, 100)
        private const val MIN_SAMPLE_DPI = 72
        private const val PREDICTION_MARGIN = 1.25
    }
}

/** Binary search for the highest JPEG quality whose output fits the budget (≤ 6 encodes for a 35–88 range). */
object QualitySearch {
    fun highestFitting(min: Int, max: Int, budget: Long, encode: (Int) -> ByteArray): Pair<Int, ByteArray>? {
        val top = encode(max)
        if (top.size <= budget) return max to top
        val bottom = encode(min)
        if (bottom.size > budget) return null
        var best = min to bottom
        var lo = min + 1
        var hi = max - 1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            val bytes = encode(mid)
            if (bytes.size <= budget) {
                best = mid to bytes
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return best
    }
}

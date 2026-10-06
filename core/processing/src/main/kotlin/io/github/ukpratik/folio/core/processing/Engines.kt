// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing

import android.graphics.Bitmap
import io.github.ukpratik.folio.core.model.ExportSettings
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.Quad
import io.github.ukpratik.folio.core.processing.pdf.PdfPageSpec
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/** Processing-engine contracts (LLD §5.1). Implementations land with epics E2–E7 after spikes S1–S4. */

data class NormalizeResult(val sourceId: String, val widthPx: Int, val heightPx: Int)

interface ImageNormalizer {
    suspend fun normalize(input: InputStream, mime: String?, out: File): NormalizeResult
}

data class Detection(val quad: Quad, val confidence: Float)

interface EdgeDetector {
    /** [gray] is an 8-bit single-channel image, long edge ≤ 1024 px. */
    fun detect(gray: ByteArray, width: Int, height: Int, rowStride: Int): Detection?
}

data class RenderRequest(val page: Page, val sourceFile: File, val targetLongEdgePx: Int, val forExport: Boolean)

interface PageRenderer {
    /** The caller owns the returned bitmap and must recycle it. */
    suspend fun render(request: RenderRequest): Bitmap
}

data class EncodedPage(val file: File, val sizeBytes: Long, val widthPx: Int, val heightPx: Int, val grayscale: Boolean)

interface SizeOptimizer {
    suspend fun encode(pages: List<Page>, settings: ExportSettings, onProgress: (done: Int, total: Int) -> Unit): List<EncodedPage>
}

interface PdfWriter {
    suspend fun write(out: OutputStream, title: String, pages: List<PdfPageSpec>)
}

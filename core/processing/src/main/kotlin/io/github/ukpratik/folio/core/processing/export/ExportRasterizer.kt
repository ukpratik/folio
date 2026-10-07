// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.export

import android.graphics.Bitmap
import io.github.ukpratik.folio.core.domain.files.DocumentFiles
import io.github.ukpratik.folio.core.model.ExportSettings
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageSize
import io.github.ukpratik.folio.core.processing.encode.PageRasterizer
import io.github.ukpratik.folio.core.processing.encode.RasterPage
import io.github.ukpratik.folio.core.processing.render.PageRenderer
import io.github.ukpratik.folio.core.processing.render.RenderRequest
import java.io.ByteArrayOutputStream
import kotlin.math.ceil
import kotlin.math.max

/**
 * Renders pages for export at a DPI (LLD §5.6): the long edge is sized to fill the page's content box at that
 * DPI. The renderer never upscales, so small sources stay at their own resolution.
 */
class ExportRasterizer(
    private val renderer: PageRenderer,
    private val files: DocumentFiles,
    private val settings: ExportSettings,
) : PageRasterizer {

    override suspend fun rasterize(page: Page, dpi: Int): RasterPage {
        val longEdgeInches = contentLongEdgePoints(settings) / POINTS_PER_INCH
        val target = ceil(longEdgeInches * dpi).toInt()
        val source = files.sourceFile(page.documentId, page.sourceId)
        return BitmapRaster(renderer.render(RenderRequest(page, source, target, forExport = true)))
    }

    companion object {
        private const val POINTS_PER_INCH = 72f

        /** Long edge of the area images are drawn into. "Fit to image" uses A4 as its reference (LLD §5.7). */
        fun contentLongEdgePoints(settings: ExportSettings): Float {
            val size = if (settings.pageSize == PageSize.FIT) PageSize.A4 else settings.pageSize
            return max(size.widthPt, size.heightPt) - 2 * settings.margin.points
        }
    }
}

private class BitmapRaster(private val bitmap: Bitmap) : RasterPage {
    override val widthPx: Int = bitmap.width
    override val heightPx: Int = bitmap.height

    /** Bitmap.compress writes no EXIF, so nothing about the device or place leaks into exports (FR-32). */
    override fun encode(quality: Int): ByteArray = ByteArrayOutputStream(bitmap.byteCount / 10).also {
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it)) { "JPEG encode failed" }
    }.toByteArray()

    override fun close() = bitmap.recycle()
}

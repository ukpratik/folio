// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.pdf

import io.github.ukpratik.folio.core.model.Margin
import io.github.ukpratik.folio.core.model.Orientation
import io.github.ukpratik.folio.core.model.PageSize
import kotlin.math.min

/** Where one image sits on one PDF page, in points (LLD §5.6). */
data class PdfPageSpec(
    val pageWidthPt: Float,
    val pageHeightPt: Float,
    val drawX: Float,
    val drawY: Float,
    val drawWidth: Float,
    val drawHeight: Float,
)

object PageGeometry {
    private const val POINTS_PER_INCH = 72f

    /**
     * Fits an image inside the page minus margins: aspect preserved, never stretched or cropped, centred.
     * With [Orientation.AUTO], a landscape image gets a landscape page.
     */
    fun layout(
        imageWidthPx: Int,
        imageHeightPx: Int,
        dpi: Int,
        size: PageSize,
        orientation: Orientation,
        margin: Margin,
    ): PdfPageSpec {
        require(imageWidthPx > 0 && imageHeightPx > 0 && dpi > 0)
        val imgW = imageWidthPx / dpi.toFloat() * POINTS_PER_INCH
        val imgH = imageHeightPx / dpi.toFloat() * POINTS_PER_INCH

        var pageW = if (size == PageSize.FIT) imgW + 2 * margin.points else size.widthPt
        var pageH = if (size == PageSize.FIT) imgH + 2 * margin.points else size.heightPt
        val wantLandscape = when (orientation) {
            Orientation.AUTO -> imageWidthPx > imageHeightPx
            Orientation.LANDSCAPE -> true
            Orientation.PORTRAIT -> false
        }
        if (size != PageSize.FIT && wantLandscape != (pageW > pageH)) {
            pageW = pageH.also { pageH = pageW }
        }

        val boxW = pageW - 2 * margin.points
        val boxH = pageH - 2 * margin.points
        val scale = min(boxW / imgW, boxH / imgH)
        val w = imgW * scale
        val h = imgH * scale
        return PdfPageSpec(pageW, pageH, (pageW - w) / 2, (pageH - h) / 2, w, h)
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.render

import android.graphics.Bitmap
import io.github.ukpratik.folio.core.model.Page
import java.io.File

/** [targetLongEdgePx] caps the output; the source is never upscaled. */
data class RenderRequest(val page: Page, val sourceFile: File, val targetLongEdgePx: Int, val forExport: Boolean)

/** Applies a page's edits to its source (LLD §5.4): crop/warp → rotate → enhance → scale. */
interface PageRenderer {
    /** The caller owns the returned bitmap and must recycle it. */
    suspend fun render(request: RenderRequest): Bitmap
}

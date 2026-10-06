// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.ui.geometry

import kotlin.math.min

/** Where the image is drawn inside the crop canvas (ContentScale.Fit). Shared by the crop editor and the camera overlay. */
data class FitRect(val left: Float, val top: Float, val width: Float, val height: Float) {
    companion object {
        fun fit(imageWidth: Int, imageHeight: Int, canvasWidth: Float, canvasHeight: Float): FitRect {
            val scale = min(canvasWidth / imageWidth, canvasHeight / imageHeight)
            val w = imageWidth * scale
            val h = imageHeight * scale
            return FitRect((canvasWidth - w) / 2, (canvasHeight - h) / 2, w, h)
        }
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.detect

import android.graphics.Bitmap
import io.github.ukpratik.folio.core.model.Quad

data class Detection(val quad: Quad, val confidence: Float)

/** 8-bit single-channel pixels, e.g. the Y plane of a camera frame. */
class GrayImage(val pixels: ByteArray, val width: Int, val height: Int, val rowStride: Int = width)

/** Finds a document's corners. Returns null unless a document is clearly detected (D-02). */
interface EdgeDetector {
    fun detect(gray: GrayImage): Detection?
    fun detect(bitmap: Bitmap): Detection?
}

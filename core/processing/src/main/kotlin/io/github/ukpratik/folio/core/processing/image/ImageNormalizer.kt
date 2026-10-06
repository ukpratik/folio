// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.image

import java.io.File
import java.io.IOException
import java.io.OutputStream

data class NormalizedImage(val widthPx: Int, val heightPx: Int)

/** Thrown for files we can't read: unknown format, HEIC on Android 8, or damaged data. */
class UnsupportedImageException(message: String) : IOException(message)

/**
 * Import step (LLD §5.2): decode within the memory cap, apply EXIF orientation, flatten transparency,
 * and write a metadata-free JPEG. The source file is never modified afterwards (FR-11).
 */
interface ImageNormalizer {
    suspend fun normalize(input: File, output: OutputStream): NormalizedImage

    companion object {
        /** Long edge of A4 at 300 dpi — the largest the High preset can use (LLD §5.2). */
        const val MAX_LONG_EDGE_PX = 3508
        const val SOURCE_JPEG_QUALITY = 95

        /** Smallest power-of-two sample size that brings the long edge within [maxLongEdge]. */
        fun sampleSizeFor(width: Int, height: Int, maxLongEdge: Int = MAX_LONG_EDGE_PX): Int {
            var sample = 1
            while (maxOf(width, height) / sample > maxLongEdge) sample *= 2
            return sample
        }
    }
}

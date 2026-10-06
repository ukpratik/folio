// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.importing

import android.graphics.BitmapFactory
import io.github.ukpratik.folio.core.model.Quad
import io.github.ukpratik.folio.core.processing.detect.EdgeDetector
import java.io.File
import javax.inject.Inject

/** Finds a document's corners in a normalised source so they can be auto-applied on import (FR-03). */
fun interface ImportAnalyzer {
    /** Returns null when no document is clearly detected; the full image is then kept. */
    fun detectCorners(source: File): Quad?
}

internal class EdgeDetectingImportAnalyzer @Inject constructor(private val detector: EdgeDetector) : ImportAnalyzer {
    override fun detectCorners(source: File): Quad? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > ANALYSIS_EDGE) sample *= 2
        val small = BitmapFactory.decodeFile(source.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        return try {
            detector.detect(small)?.quad
        } finally {
            small.recycle()
        }
    }

    private companion object {
        /** Detection runs on a ≤ 1024 px copy (LLD §5.3); decode near that size. */
        const val ANALYSIS_EDGE = 1024
    }
}

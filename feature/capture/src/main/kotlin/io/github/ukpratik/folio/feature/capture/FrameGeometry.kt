// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.capture

import io.github.ukpratik.folio.core.model.PointF01
import io.github.ukpratik.folio.core.model.Quad

/** Camera frames arrive in sensor orientation; the overlay draws in display orientation. */
object FrameGeometry {
    /** Rotates a normalised quad clockwise by [degrees] (0/90/180/270) and re-labels its corners. */
    fun rotate(quad: Quad, degrees: Int): Quad {
        fun r(p: PointF01) = when (((degrees % 360) + 360) % 360) {
            90 -> PointF01(1f - p.y, p.x)
            180 -> PointF01(1f - p.x, 1f - p.y)
            270 -> PointF01(p.y, 1f - p.x)
            else -> p
        }
        val points = listOf(quad.tl, quad.tr, quad.br, quad.bl).map(::r)
        // After rotation the old top-left is no longer top-left: re-order clockwise from the new top-left.
        val start = points.indices.minBy { points[it].x + points[it].y }
        val ordered = List(4) { points[(start + it) % 4] }
        return Quad(ordered[0], ordered[1], ordered[2], ordered[3])
    }

    /** Average of every [step]-th luma byte: cheap brightness for the "More light needed" hint. */
    fun meanLuma(y: ByteArray, width: Int, height: Int, rowStride: Int, step: Int = 8): Double {
        var sum = 0L
        var count = 0
        for (row in 0 until height step step) for (col in 0 until width step step) {
            sum += y[row * rowStride + col].toInt() and 0xFF
            count++
        }
        return if (count == 0) 0.0 else sum.toDouble() / count
    }
}

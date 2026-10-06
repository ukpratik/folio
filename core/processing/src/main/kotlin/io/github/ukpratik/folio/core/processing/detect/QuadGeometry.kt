// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.detect

import io.github.ukpratik.folio.core.model.PointF01
import io.github.ukpratik.folio.core.model.Quad
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** A point in pixel space. */
data class Px(val x: Double, val y: Double)

/**
 * Pure geometry for document quads (LLD §5.3). No OpenCV, no Android — unit-tested on the JVM.
 * Corner lists are always clockwise from top-left: [tl, tr, br, bl].
 */
object QuadGeometry {
    const val MIN_AREA_FRACTION = 0.20
    const val BORDER_FRACTION = 0.02
    const val MIN_ANGLE_DEG = 45.0
    const val MAX_ANGLE_DEG = 135.0

    /** Orders any four points as tl, tr, br, bl (smallest/largest x+y and y−x). */
    fun order(points: List<Px>): List<Px> {
        require(points.size == 4) { "A quad needs 4 points" }
        val tl = points.minBy { it.x + it.y }
        val br = points.maxBy { it.x + it.y }
        val tr = points.minBy { it.y - it.x }
        val bl = points.maxBy { it.y - it.x }
        return if (setOf(tl, tr, br, bl).size == 4) listOf(tl, tr, br, bl) else orderByAngle(points)
    }

    /** Fallback for degenerate cases (e.g. a diamond): sort by angle around the centroid, start at top-left. */
    private fun orderByAngle(points: List<Px>): List<Px> {
        val cx = points.sumOf { it.x } / 4
        val cy = points.sumOf { it.y } / 4
        val sorted = points.sortedBy { kotlin.math.atan2(it.y - cy, it.x - cx) }
        val start = sorted.indices.minBy { sorted[it].x + sorted[it].y }
        return List(4) { sorted[(start + it) % 4] }
    }

    /** Shoelace area. */
    fun area(c: List<Px>): Double =
        abs((0 until 4).sumOf { i -> val a = c[i]; val b = c[(i + 1) % 4]; a.x * b.y - b.x * a.y }) / 2

    fun interiorAnglesDeg(c: List<Px>): List<Double> = (0 until 4).map { i ->
        val prev = c[(i + 3) % 4]
        val cur = c[i]
        val next = c[(i + 1) % 4]
        val v1x = prev.x - cur.x
        val v1y = prev.y - cur.y
        val v2x = next.x - cur.x
        val v2y = next.y - cur.y
        val cos = (v1x * v2x + v1y * v2y) / (hypot(v1x, v1y) * hypot(v2x, v2y))
        Math.toDegrees(acos(cos.coerceIn(-1.0, 1.0)))
    }

    /**
     * Auto-apply only when a document is clearly detected (D-02):
     * big enough, not just the frame itself, and roughly rectangular.
     */
    fun isConfident(corners: List<Px>, width: Int, height: Int): Boolean {
        val frameArea = width.toDouble() * height
        if (area(corners) < MIN_AREA_FRACTION * frameArea) return false
        val bx = BORDER_FRACTION * width
        val by = BORDER_FRACTION * height
        // A side lying on the frame border means the "document" is the frame or runs out of it
        // (screenshots, cropped scans, pages partly out of shot): keep the full image instead.
        fun side(p: Px) = setOfNotNull(
            "left".takeIf { p.x <= bx }, "right".takeIf { p.x >= width - bx },
            "top".takeIf { p.y <= by }, "bottom".takeIf { p.y >= height - by },
        )
        val touchesFrameWithASide = (0 until 4).any { i -> (side(corners[i]) intersect side(corners[(i + 1) % 4])).isNotEmpty() }
        if (touchesFrameWithASide) return false
        return interiorAnglesDeg(corners).all { it in MIN_ANGLE_DEG..MAX_ANGLE_DEG }
    }

    /** Size of the flattened page: the average lengths of opposite edges. */
    fun outputSize(c: List<Px>): Pair<Int, Int> {
        fun dist(a: Px, b: Px) = hypot(a.x - b.x, a.y - b.y)
        val width = (dist(c[0], c[1]) + dist(c[3], c[2])) / 2
        val height = (dist(c[0], c[3]) + dist(c[1], c[2])) / 2
        return width.roundToInt().coerceAtLeast(1) to height.roundToInt().coerceAtLeast(1)
    }

    fun toQuad(c: List<Px>, width: Int, height: Int): Quad {
        fun n(p: Px) = PointF01((p.x / width).toFloat().coerceIn(0f, 1f), (p.y / height).toFloat().coerceIn(0f, 1f))
        return Quad(n(c[0]), n(c[1]), n(c[2]), n(c[3]))
    }

    fun fromQuad(q: Quad, width: Int, height: Int): List<Px> =
        listOf(q.tl, q.tr, q.br, q.bl).map { Px(it.x.toDouble() * width, it.y.toDouble() * height) }

    /** Distance between two quads relative to the frame diagonal, in normalised space. */
    fun maxCornerDistance(a: Quad, b: Quad): Double = listOf(a.tl to b.tl, a.tr to b.tr, a.br to b.br, a.bl to b.bl)
        .maxOf { (p, q) -> hypot((p.x - q.x).toDouble(), (p.y - q.y).toDouble()) } / sqrt(2.0)
}

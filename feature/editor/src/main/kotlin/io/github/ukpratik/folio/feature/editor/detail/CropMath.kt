// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor.detail

import io.github.ukpratik.folio.core.model.PointF01
import io.github.ukpratik.folio.core.model.Quad
import kotlin.math.hypot
import kotlin.math.min

/** Corner identity, clockwise from top-left (matches [Quad]). */
enum class Corner { TL, TR, BR, BL }

/** Where the image is drawn inside the crop canvas (ContentScale.Fit). */
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

/** Pure crop-handle maths (FR-04), kept out of Compose so it's unit-tested on the JVM. */
object CropMath {
    /** 1 % of the image per TalkBack nudge (UX §7). */
    const val NUDGE = 0.01f

    val FULL_IMAGE = Quad(PointF01(0f, 0f), PointF01(1f, 0f), PointF01(1f, 1f), PointF01(0f, 1f))

    fun Quad.point(corner: Corner): PointF01 = when (corner) {
        Corner.TL -> tl
        Corner.TR -> tr
        Corner.BR -> br
        Corner.BL -> bl
    }

    fun Quad.with(corner: Corner, p: PointF01): Quad = when (corner) {
        Corner.TL -> copy(tl = p)
        Corner.TR -> copy(tr = p)
        Corner.BR -> copy(br = p)
        Corner.BL -> copy(bl = p)
    }

    fun toScreen(p: PointF01, rect: FitRect): Pair<Float, Float> = (rect.left + p.x * rect.width) to (rect.top + p.y * rect.height)

    /** Screen → normalised image space, clamped to the image. */
    fun toImage(x: Float, y: Float, rect: FitRect): PointF01 =
        PointF01(((x - rect.left) / rect.width).coerceIn(0f, 1f), ((y - rect.top) / rect.height).coerceIn(0f, 1f))

    /** The handle within [touchRadiusPx] of the touch, nearest first; null if none. */
    fun nearestCorner(quad: Quad, x: Float, y: Float, rect: FitRect, touchRadiusPx: Float): Corner? =
        Corner.entries
            .map { c -> c to toScreen(quad.point(c), rect).let { (sx, sy) -> hypot(sx - x, sy - y) } }
            .filter { (_, d) -> d <= touchRadiusPx }
            .minByOrNull { (_, d) -> d }
            ?.first

    fun nudge(quad: Quad, corner: Corner, dx: Float, dy: Float): Quad {
        val p = quad.point(corner)
        return quad.with(corner, PointF01((p.x + dx).coerceIn(0f, 1f), (p.y + dy).coerceIn(0f, 1f)))
    }
}

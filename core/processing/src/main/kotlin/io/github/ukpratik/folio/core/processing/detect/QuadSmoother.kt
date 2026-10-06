// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.detect

import io.github.ukpratik.folio.core.model.PointF01
import io.github.ukpratik.folio.core.model.Quad

/**
 * Steadies the live camera outline (LLD §5.3): an exponential moving average over recent detections,
 * dropped after [maxMisses] frames without one. Not thread-safe; use from the single analyzer thread.
 */
class QuadSmoother(private val alpha: Float = 0.5f, private val maxMisses: Int = 3) {
    private var current: Quad? = null
    private var misses = 0

    fun update(detected: Quad?): Quad? {
        if (detected == null) {
            misses++
            if (misses >= maxMisses) current = null
            return current
        }
        misses = 0
        current = current?.let { blend(it, detected) } ?: detected
        return current
    }

    fun reset() {
        current = null
        misses = 0
    }

    private fun blend(old: Quad, new: Quad) = Quad(mix(old.tl, new.tl), mix(old.tr, new.tr), mix(old.br, new.br), mix(old.bl, new.bl))

    private fun mix(a: PointF01, b: PointF01) = PointF01(a.x + (b.x - a.x) * alpha, a.y + (b.y - a.y) * alpha)
}

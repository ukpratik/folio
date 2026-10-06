// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.opencv

import org.opencv.core.Mat

/**
 * Native memory isn't managed by the GC fast enough for multi-megapixel images.
 * Every Mat created through [track] is released when the scope ends, success or failure.
 */
class MatScope {
    private val mats = mutableListOf<Mat>()

    fun <T : Mat> track(mat: T): T = mat.also { mats += it }

    /** Removes [mat] from the scope so the caller can keep it (and must release it). */
    fun <T : Mat> keep(mat: T): T = mat.also { mats.remove(it) }

    @PublishedApi internal fun releaseAll() {
        mats.forEach { it.release() }
        mats.clear()
    }
}

inline fun <T> withMats(block: MatScope.() -> T): T {
    val scope = MatScope()
    try {
        return scope.block()
    } finally {
        scope.releaseAll()
    }
}

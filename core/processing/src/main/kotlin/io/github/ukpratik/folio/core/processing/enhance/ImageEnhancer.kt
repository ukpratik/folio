// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.enhance

import io.github.ukpratik.folio.core.model.Adjustments
import io.github.ukpratik.folio.core.model.EnhancementMode
import org.opencv.core.Mat

/** FR-12/13. Input is 8-bit RGB; output is RGB, or single-channel gray for GRAYSCALE and BW. Caller releases the result. */
interface ImageEnhancer {
    fun enhance(rgb: Mat, mode: EnhancementMode, adjustments: Adjustments): Mat
}

/** Brightness/contrast as a linear transform around mid-grey: out = (in − 128)·(1 + c) + 128 + 64·b. */
internal object LinearAdjust {
    fun alpha(a: Adjustments): Double = (1.0 + a.contrast).coerceIn(0.0, 2.0)

    fun beta(a: Adjustments): Double = 128.0 * (1.0 - alpha(a)) + 64.0 * a.brightness.coerceIn(-1f, 1f)

    fun isIdentity(a: Adjustments) = a.brightness == 0f && a.contrast == 0f
}

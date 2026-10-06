// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor.detail

import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.model.PointF01
import io.github.ukpratik.folio.feature.editor.detail.CropMath.point
import org.junit.Test

class CropMathTest {
    private val rect = FitRect.fit(imageWidth = 600, imageHeight = 800, canvasWidth = 1000f, canvasHeight = 1000f)

    @Test fun fitCentresPortraitImage() {
        assertThat(rect).isEqualTo(FitRect(125f, 0f, 750f, 1000f))
    }

    @Test fun screenImageRoundTripAndClamp() {
        val (x, y) = CropMath.toScreen(PointF01(0.5f, 0.25f), rect)
        assertThat(CropMath.toImage(x, y, rect)).isEqualTo(PointF01(0.5f, 0.25f))
        assertThat(CropMath.toImage(-50f, 2000f, rect)).isEqualTo(PointF01(0f, 1f))
    }

    @Test fun picksNearestHandleWithinTouchRadius() {
        val quad = CropMath.FULL_IMAGE
        assertThat(CropMath.nearestCorner(quad, 130f, 10f, rect, touchRadiusPx = 72f)).isEqualTo(Corner.TL)
        assertThat(CropMath.nearestCorner(quad, 500f, 500f, rect, touchRadiusPx = 72f)).isNull()
    }

    @Test fun nudgeMovesOnePercentAndClamps() {
        val moved = CropMath.nudge(CropMath.FULL_IMAGE, Corner.BR, -CropMath.NUDGE, 0f)
        assertThat(moved.point(Corner.BR).x).isWithin(1e-6f).of(0.99f)
        assertThat(CropMath.nudge(CropMath.FULL_IMAGE, Corner.TL, -CropMath.NUDGE, 0f)).isEqualTo(CropMath.FULL_IMAGE)
    }
}

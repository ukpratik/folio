// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.capture

import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.model.PointF01
import io.github.ukpratik.folio.core.model.Quad
import org.junit.Test

class CameraLogicTest {
    @Test fun permissionStates() {
        assertThat(CameraPermission.resolve(granted = true, requestedBefore = true, shouldShowRationale = false)).isEqualTo(CameraAccess.GRANTED)
        assertThat(CameraPermission.resolve(granted = false, requestedBefore = false, shouldShowRationale = false)).isEqualTo(CameraAccess.SHOW_RATIONALE)
        assertThat(CameraPermission.resolve(granted = false, requestedBefore = true, shouldShowRationale = true)).isEqualTo(CameraAccess.SHOW_RATIONALE)
        assertThat(CameraPermission.resolve(granted = false, requestedBefore = true, shouldShowRationale = false)).isEqualTo(CameraAccess.DENIED)
        assertThat(CameraPermission.afterRequest(granted = false)).isEqualTo(CameraAccess.DENIED)
        assertThat(CameraPermission.afterRequest(granted = true)).isEqualTo(CameraAccess.GRANTED)
    }

    private val sensorQuad = Quad(PointF01(0.1f, 0.2f), PointF01(0.6f, 0.2f), PointF01(0.6f, 0.9f), PointF01(0.1f, 0.9f))

    @Test fun rotate90KeepsCornersClockwiseFromTopLeft() {
        val upright = FrameGeometry.rotate(sensorQuad, 90)
        // Sensor (x, y) → display (1 − y, x); the sensor's bottom-left becomes the display's top-left.
        assertNear(upright.tl, 0.1f, 0.1f)
        assertNear(upright.tr, 0.8f, 0.1f)
        assertNear(upright.br, 0.8f, 0.6f)
        assertNear(upright.bl, 0.1f, 0.6f)
    }

    private fun assertNear(p: PointF01, x: Float, y: Float) {
        assertThat(p.x).isWithin(1e-5f).of(x)
        assertThat(p.y).isWithin(1e-5f).of(y)
    }

    @Test fun rotate0IsIdentityAnd360Wraps() {
        assertThat(FrameGeometry.rotate(sensorQuad, 0)).isEqualTo(sensorQuad)
        assertThat(FrameGeometry.rotate(sensorQuad, 360)).isEqualTo(sensorQuad)
    }

    @Test fun meanLumaHonoursRowStride() {
        val width = 4
        val stride = 8
        val bytes = ByteArray(stride * 2) { i -> if (i % stride < width) 100.toByte() else 0 }
        assertThat(FrameGeometry.meanLuma(bytes, width, 2, stride, step = 1)).isEqualTo(100.0)
    }
}

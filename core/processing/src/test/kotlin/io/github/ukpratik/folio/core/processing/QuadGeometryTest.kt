// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing

import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.model.PointF01
import io.github.ukpratik.folio.core.model.Quad
import io.github.ukpratik.folio.core.processing.detect.Px
import io.github.ukpratik.folio.core.processing.detect.QuadGeometry
import io.github.ukpratik.folio.core.processing.detect.QuadSmoother
import org.junit.Test

class QuadGeometryTest {
    private val page = listOf(Px(100.0, 80.0), Px(500.0, 60.0), Px(520.0, 700.0), Px(90.0, 690.0))

    @Test fun ordersShuffledCornersClockwiseFromTopLeft() {
        assertThat(QuadGeometry.order(page.shuffled(kotlin.random.Random(7)))).containsExactlyElementsIn(page).inOrder()
    }

    @Test fun confidentForATiltedPage() {
        assertThat(QuadGeometry.isConfident(page, 600, 800)).isTrue()
    }

    @Test fun rejectsSmallShapes() {
        val tiny = listOf(Px(10.0, 10.0), Px(60.0, 10.0), Px(60.0, 60.0), Px(10.0, 60.0))
        assertThat(QuadGeometry.isConfident(tiny, 600, 800)).isFalse()
    }

    @Test fun rejectsTheFrameItself() {
        val frame = listOf(Px(1.0, 1.0), Px(599.0, 0.0), Px(600.0, 799.0), Px(0.0, 800.0))
        assertThat(QuadGeometry.isConfident(frame, 600, 800)).isFalse()
    }

    @Test fun rejectsSkewedShapes() {
        // Big enough, but the top-left corner is ~35°: not a photographed page.
        val sliver = listOf(Px(0.0, 0.0), Px(600.0, 0.0), Px(600.0, 400.0), Px(560.0, 400.0))
        assertThat(QuadGeometry.isConfident(sliver, 600, 800)).isFalse()
    }

    @Test fun rejectsShapesWithASideOnTheFrameBorder() {
        val rightHalf = listOf(Px(300.0, 0.0), Px(600.0, 0.0), Px(600.0, 800.0), Px(300.0, 800.0))
        assertThat(QuadGeometry.isConfident(rightHalf, 600, 800)).isFalse()
    }

    @Test fun outputSizeAveragesOppositeEdges() {
        val rect = listOf(Px(0.0, 0.0), Px(400.0, 0.0), Px(400.0, 300.0), Px(0.0, 300.0))
        assertThat(QuadGeometry.outputSize(rect)).isEqualTo(400 to 300)
    }

    @Test fun quadRoundTripsThroughNormalisedSpace() {
        val quad = QuadGeometry.toQuad(page, 600, 800)
        val back = QuadGeometry.fromQuad(quad, 600, 800)
        back.zip(page).forEach { (a, b) ->
            assertThat(a.x).isWithin(0.01).of(b.x)
            assertThat(a.y).isWithin(0.01).of(b.y)
        }
    }

    @Test fun smootherBlendsAndDropsAfterMisses() {
        val smoother = QuadSmoother(alpha = 0.5f, maxMisses = 2)
        val a = Quad(PointF01(0f, 0f), PointF01(0.4f, 0f), PointF01(0.4f, 0.4f), PointF01(0f, 0.4f))
        val b = Quad(PointF01(0.2f, 0.2f), PointF01(0.6f, 0.2f), PointF01(0.6f, 0.6f), PointF01(0.2f, 0.6f))
        assertThat(smoother.update(a)).isEqualTo(a)
        assertThat(smoother.update(b)!!.tl).isEqualTo(PointF01(0.1f, 0.1f))
        assertThat(smoother.update(null)).isNotNull()
        assertThat(smoother.update(null)).isNull()
    }
}

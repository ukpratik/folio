// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing

import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.model.Margin
import io.github.ukpratik.folio.core.model.Orientation
import io.github.ukpratik.folio.core.model.PageSize
import io.github.ukpratik.folio.core.processing.pdf.PageGeometry
import org.junit.Test

class PageGeometryTest {
    @Test fun portraitImageFillsA4Width() {
        val spec = PageGeometry.layout(1654, 2339, 200, PageSize.A4, Orientation.AUTO, Margin.NONE)
        assertThat(spec.pageWidthPt).isWithin(0.01f).of(595.28f)
        assertThat(spec.drawWidth).isWithin(0.5f).of(595.28f)
        assertThat(spec.drawX).isWithin(0.5f).of(0f)
    }

    @Test fun autoOrientationTurnsPageForLandscapeImage() {
        val spec = PageGeometry.layout(3000, 2000, 300, PageSize.A4, Orientation.AUTO, Margin.NONE)
        assertThat(spec.pageWidthPt).isGreaterThan(spec.pageHeightPt)
    }

    @Test fun imageIsCentredAndNeverExceedsMarginBox() {
        val spec = PageGeometry.layout(1000, 1000, 200, PageSize.LETTER, Orientation.PORTRAIT, Margin.SMALL)
        assertThat(spec.drawWidth).isAtMost(612f - 2 * 28.35f + 0.01f)
        assertThat(spec.drawY).isWithin(0.01f).of((spec.pageHeightPt - spec.drawHeight) / 2)
    }

    @Test fun fitPageMatchesImageSize() {
        val spec = PageGeometry.layout(600, 300, 200, PageSize.FIT, Orientation.AUTO, Margin.NONE)
        assertThat(spec.pageWidthPt).isWithin(0.01f).of(216f)
        assertThat(spec.pageHeightPt).isWithin(0.01f).of(108f)
    }
}

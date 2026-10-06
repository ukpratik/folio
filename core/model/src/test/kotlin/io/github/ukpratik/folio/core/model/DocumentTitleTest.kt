// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.model

import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
import org.junit.Test

class DocumentTitleTest {
    @Test fun defaultTitleUsesTimestampWithUnderscores() {
        assertThat(DocumentTitle.default(LocalDateTime.of(2026, 10, 6, 14, 30))).isEqualTo("Folio_20261006_1430")
    }

    @Test fun sanitizeReplacesForbiddenCharacters() {
        assertThat(DocumentTitle.sanitize("a/b:c*d?e\"f<g>h|i\\j")).isEqualTo("a_b_c_d_e_f_g_h_i_j")
    }

    @Test fun sanitizeTrimsToMaxLength() {
        assertThat(DocumentTitle.sanitize("x".repeat(150))).hasLength(DocumentTitle.MAX_LENGTH)
    }

    @Test fun sanitizeRejectsBlank() {
        assertThat(DocumentTitle.sanitize("   ")).isNull()
    }

    @Test fun defaultPageSizeFollowsRegion() {
        assertThat(ExportSettings.defaultPageSizeFor("us")).isEqualTo(PageSize.LETTER)
        assertThat(ExportSettings.defaultPageSizeFor("IN")).isEqualTo(PageSize.A4)
    }

    @Test fun rotationCyclesClockwise() {
        assertThat(Rotation.R270.clockwise()).isEqualTo(Rotation.R0)
    }
}

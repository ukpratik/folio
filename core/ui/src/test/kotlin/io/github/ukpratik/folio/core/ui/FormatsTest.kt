// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.ui

import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.model.ByteSize
import io.github.ukpratik.folio.core.ui.format.RecentDay
import io.github.ukpratik.folio.core.ui.format.display
import io.github.ukpratik.folio.core.ui.format.recentDay
import io.github.ukpratik.folio.core.ui.format.sizeRange
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Test

class FormatsTest {
    private val en = Locale.UK

    @Test fun sizesUsePortalStyleUnits() {
        assertThat(ByteSize(438_400).display(en)).isEqualTo("438 KB")
        assertThat(ByteSize(99_600).display(en)).isEqualTo("100 KB")
        assertThat(ByteSize(1_240_000).display(en)).isEqualTo("1.2 MB")
        assertThat(ByteSize(2_000_000).display(en)).isEqualTo("2 MB")
        assertThat(ByteSize(12).display(en)).isEqualTo("1 KB")
        assertThat(ByteSize(999_700).display(en)).isEqualTo("1 MB")
        assertThat(ByteSize(1_250_000).display(Locale.GERMANY)).isEqualTo("1,3 MB")
    }

    @Test fun sizeRanges() {
        assertThat(sizeRange(ByteSize(96_000), ByteSize(141_000), en)).isEqualTo("96–141 KB")
        assertThat(sizeRange(ByteSize(800_000), ByteSize(1_200_000), en)).isEqualTo("800 KB–1.2 MB")
        assertThat(sizeRange(ByteSize(140_600), ByteSize(141_000), en)).isEqualTo("141 KB")
    }

    @Test fun recentDays() {
        val today = LocalDate.of(2026, 10, 7)
        fun at(y: Int, m: Int, d: Int) = ZonedDateTime.of(y, m, d, 14, 30, 0, 0, ZoneOffset.UTC).toInstant()
        assertThat(at(2026, 10, 7).recentDay(today, ZoneOffset.UTC, en)).isEqualTo(RecentDay.Today)
        assertThat(at(2026, 10, 6).recentDay(today, ZoneOffset.UTC, en)).isEqualTo(RecentDay.Yesterday)
        assertThat(at(2026, 10, 2).recentDay(today, ZoneOffset.UTC, en)).isEqualTo(RecentDay.On("2 Oct"))
        assertThat(at(2025, 12, 31).recentDay(today, ZoneOffset.UTC, en)).isEqualTo(RecentDay.On("31 Dec 2025"))
    }
}

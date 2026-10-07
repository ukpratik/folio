// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.ui.format

import io.github.ukpratik.folio.core.model.ByteSize
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * UX §6: KB/MB with at most one decimal ("438 KB", "1.2 MB"), 1 KB = 1000 bytes like upload portals.
 * Never shows "0 KB" for a real file.
 */
fun ByteSize.display(locale: Locale = Locale.getDefault()): String {
    val number = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }
    return when {
        bytes >= 999_500 -> number.format(Math.round(bytes / 100_000.0) / 10.0) + " MB"
        else -> NumberFormat.getIntegerInstance(locale).format(maxOf(1, Math.round(bytes / 1_000.0))) + " KB"
    }
}

/** "96–141 KB", "800 KB–1.2 MB", or a single size when both ends look the same. */
fun sizeRange(min: ByteSize, max: ByteSize, locale: Locale = Locale.getDefault()): String {
    val low = min.display(locale)
    val high = max.display(locale)
    return when {
        low == high -> high
        low.substringAfterLast(' ') == high.substringAfterLast(' ') -> low.substringBeforeLast(' ') + "–" + high
        else -> "$low–$high"
    }
}

/** Day labels for Recents ("Today", "Yesterday", or a short date), resolved against [today]. */
sealed interface RecentDay {
    data object Today : RecentDay
    data object Yesterday : RecentDay
    data class On(val text: String) : RecentDay
}

fun Instant.recentDay(
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): RecentDay {
    val date = atZone(zone).toLocalDate()
    return when (date) {
        today -> RecentDay.Today
        today.minusDays(1) -> RecentDay.Yesterday
        else -> {
            val pattern = if (date.year == today.year) "d MMM" else "d MMM yyyy"
            RecentDay.On(DateTimeFormatter.ofPattern(pattern, locale).format(date))
        }
    }
}

fun Instant.shortTime(zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(atZone(zone))

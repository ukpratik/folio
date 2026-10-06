// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Document naming rules (FR-23, D-25). */
object DocumentTitle {
    const val MAX_LENGTH = 100
    private val forbidden = Regex("""[/\\:*?"<>|]""")
    private val stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")

    fun default(now: LocalDateTime): String = "Folio_" + now.format(stamp)

    /** Replaces characters that aren't allowed in file names and trims to [MAX_LENGTH]. Returns null when blank. */
    fun sanitize(input: String): String? {
        val cleaned = input.trim().replace(forbidden, "_").take(MAX_LENGTH).trim()
        return cleaned.ifEmpty { null }
    }
}

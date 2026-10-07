// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.model

import java.util.Locale

/** Output file names (UX §5, FR-24): `Title.pdf`, or `Title_01.jpg`, `Title_02.jpg`… */
object ExportFileNames {
    fun pdf(title: String): String = "$title.pdf"

    fun jpg(title: String, index: Int): String = String.format(Locale.ROOT, "%s_%02d.jpg", title, index + 1)

    fun of(format: ExportFormat, title: String, count: Int): List<String> = when (format) {
        ExportFormat.PDF -> listOf(pdf(title))
        ExportFormat.JPG -> List(count) { jpg(title, it) }
    }

    fun mimeType(format: ExportFormat): String = when (format) {
        ExportFormat.PDF -> "application/pdf"
        ExportFormat.JPG -> "image/jpeg"
    }
}

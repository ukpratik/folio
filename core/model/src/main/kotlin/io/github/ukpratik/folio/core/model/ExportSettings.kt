// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.model

enum class ExportFormat { PDF, JPG }

/** Page sizes in PDF points (1/72 inch). [FIT] takes the image's own size. */
enum class PageSize(val widthPt: Float, val heightPt: Float) {
    A4(595.28f, 841.89f),
    LETTER(612f, 792f),
    LEGAL(612f, 1008f),
    FIT(0f, 0f),
}

enum class Orientation { AUTO, PORTRAIT, LANDSCAPE }

enum class Margin(val points: Float) { NONE(0f), SMALL(28.35f) }

/** Quality presets (ADR-0011). */
enum class QualityPreset(val dpi: Int, val jpegQuality: Int) {
    SMALL(150, 60),
    BALANCED(200, 75),
    HIGH(300, 88),
}

data class ExportSettings(
    val format: ExportFormat = ExportFormat.PDF,
    val pageSize: PageSize = PageSize.A4,
    val orientation: Orientation = Orientation.AUTO,
    val margin: Margin = Margin.NONE,
    val quality: QualityPreset = QualityPreset.BALANCED,
    /** null = no limit (default, D-30). */
    val target: ByteSize? = null,
) {
    companion object {
        val TARGET_RANGE = ByteSize.kb(50).bytes..ByteSize.mb(50).bytes

        /** Letter for the US and Canada, A4 everywhere else (PRD §8). */
        fun defaultPageSizeFor(countryCode: String): PageSize =
            if (countryCode.uppercase() in setOf("US", "CA")) PageSize.LETTER else PageSize.A4
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.export.sheet

import io.github.ukpratik.folio.core.model.ByteSize
import io.github.ukpratik.folio.core.model.ExportFormat
import io.github.ukpratik.folio.core.model.ExportSettings
import io.github.ukpratik.folio.core.model.Margin
import io.github.ukpratik.folio.core.model.Orientation
import io.github.ukpratik.folio.core.model.PageSize
import io.github.ukpratik.folio.core.model.QualityPreset

/** The Max size chips (UX S5). */
sealed interface SizeChoice {
    data object None : SizeChoice
    data class Preset(val size: ByteSize) : SizeChoice
    data object Custom : SizeChoice

    companion object {
        val PRESETS = listOf(100L, 200L, 500L).map { Preset(ByteSize.kb(it)) } + listOf(1L, 2L, 5L).map { Preset(ByteSize.mb(it)) }
        val ALL: List<SizeChoice> = listOf(None) + PRESETS + Custom
    }
}

enum class SizeUnit(val bytes: Long) { KB(1_000), MB(1_000_000) }

data class ExportSheetState(
    val loaded: Boolean = false,
    val name: String = "",
    val format: ExportFormat = ExportFormat.PDF,
    val size: SizeChoice = SizeChoice.None,
    val customValue: String = "",
    val customUnit: SizeUnit = SizeUnit.KB,
    val quality: QualityPreset = QualityPreset.BALANCED,
    val pageSize: PageSize = PageSize.A4,
    val orientation: Orientation = Orientation.AUTO,
    val margin: Margin = Margin.NONE,
    val showCustomError: Boolean = false,
    val starting: Boolean = false,
) {
    /** The custom size, or null when the text isn't a number in range (50 KB – 50 MB). */
    val customTarget: ByteSize?
        get() = customValue.replace(',', '.').toDoubleOrNull()
            ?.let { (it * customUnit.bytes).toLong() }
            ?.takeIf { it in ExportSettings.TARGET_RANGE }
            ?.let(::ByteSize)

    val target: ByteSize?
        get() = when (val choice = size) {
            SizeChoice.None -> null
            is SizeChoice.Preset -> choice.size
            SizeChoice.Custom -> customTarget
        }

    val canCreate: Boolean get() = loaded && !starting && name.isNotBlank()

    fun toSettings() = ExportSettings(format, pageSize, orientation, margin, quality, target)
}

sealed interface ExportSheetIntent {
    data class NameChanged(val name: String) : ExportSheetIntent
    data class FormatChanged(val format: ExportFormat) : ExportSheetIntent
    data class SizeChosen(val choice: SizeChoice) : ExportSheetIntent
    data class CustomValueChanged(val text: String) : ExportSheetIntent
    data class CustomUnitChanged(val unit: SizeUnit) : ExportSheetIntent
    data class QualityChanged(val quality: QualityPreset) : ExportSheetIntent
    data class PageSizeChanged(val pageSize: PageSize) : ExportSheetIntent
    data class OrientationChanged(val orientation: Orientation) : ExportSheetIntent
    data class MarginChanged(val margin: Margin) : ExportSheetIntent
    data object Create : ExportSheetIntent
}

sealed interface ExportSheetEffect {
    /** The export is running (just started, or already was): show the Processing screen. */
    data object Started : ExportSheetEffect
}

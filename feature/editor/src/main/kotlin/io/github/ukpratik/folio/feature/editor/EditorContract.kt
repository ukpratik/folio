// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor

import io.github.ukpratik.folio.core.domain.engine.ImportProgress
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.ui.text.UiText

data class EditorState(
    val title: String = "",
    val pages: List<Page> = emptyList(),
    val importing: ImportProgress? = null,
) {
    val isImporting: Boolean get() = importing?.isRunning == true

    /** FR-19: at least one ready page, and nothing still importing (S3c). */
    val canCreatePdf: Boolean get() = !isImporting && pages.any { it.status == PageStatus.READY }
}

sealed interface EditorEffect {
    data class ShowMessage(val text: UiText) : EditorEffect
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor

import io.github.ukpratik.folio.core.domain.engine.ImportProgress
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.ui.text.UiText

data class EditorState(
    val loaded: Boolean = false,
    val title: String = "",
    val pages: List<Page> = emptyList(),
    val importing: ImportProgress? = null,
) {
    val isImporting: Boolean get() = importing?.isRunning == true

    /** FR-19: at least one ready page, and nothing still importing (S3c). */
    val canCreatePdf: Boolean get() = !isImporting && pages.any { it.status == PageStatus.READY }

    val isEmpty: Boolean get() = loaded && pages.isEmpty() && !isImporting
}

sealed interface EditorIntent {
    data class Move(val pageId: PageId, val toIndex: Int) : EditorIntent
    data class Rotate(val pageId: PageId) : EditorIntent
    data class Duplicate(val pageId: PageId) : EditorIntent
    data class Delete(val pageId: PageId) : EditorIntent
    data class UndoDelete(val pageId: PageId) : EditorIntent
    data class Rename(val title: String) : EditorIntent
    data class AddImages(val uris: List<String>) : EditorIntent
    data object DeleteDocument : EditorIntent
    data object Leave : EditorIntent
}

sealed interface EditorEffect {
    data class ShowMessage(val text: UiText) : EditorEffect
    data class ShowUndo(val pageId: PageId) : EditorEffect
    data class Close(val message: UiText?) : EditorEffect
}

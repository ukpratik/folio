// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.home

import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.ExportResult
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.ui.text.UiText
import java.time.Instant

/** One Recents row (FR-25). */
data class RecentUi(
    val id: DocumentId,
    val title: String,
    val isDraft: Boolean,
    /** Only when the export isn't running right now (a running one isn't "unfinished"). */
    val exportInterrupted: Boolean,
    val pageCount: Int,
    val cover: Page?,
    val updatedAt: Instant,
    /** The latest export, for Share / Save to device. */
    val export: ExportResult?,
)

data class HomeState(
    val loading: Boolean = true,
    val recents: List<RecentUi> = emptyList(),
    val lastFolderUri: String? = null,
)

sealed interface HomeIntent {
    data class ImagesPicked(val uris: List<String>) : HomeIntent
    data class Rename(val id: DocumentId, val title: String) : HomeIntent
    data class Delete(val id: DocumentId) : HomeIntent
    data class SaveTo(val id: DocumentId, val uri: String) : HomeIntent
}

sealed interface HomeEffect {
    data class OpenEditor(val documentId: DocumentId) : HomeEffect
    data class ShowMessage(val text: UiText) : HomeEffect
}

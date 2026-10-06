// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.home

import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.ui.text.UiText

data class RecentUi(val id: DocumentId, val title: String, val isDraft: Boolean, val exportInterrupted: Boolean)

data class HomeState(val loading: Boolean = true, val recents: List<RecentUi> = emptyList())

sealed interface HomeIntent {
    data class ImagesPicked(val uris: List<String>) : HomeIntent
}

sealed interface HomeEffect {
    data class OpenEditor(val documentId: DocumentId) : HomeEffect
    data class ShowMessage(val text: UiText) : HomeEffect
}

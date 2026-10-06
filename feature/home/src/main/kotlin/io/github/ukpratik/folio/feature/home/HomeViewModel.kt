// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.model.Document
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.DocumentStatus
import io.github.ukpratik.folio.core.model.DocumentTitle
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecentUi(val id: DocumentId, val title: String, val isDraft: Boolean, val exportInterrupted: Boolean)

data class HomeState(val loading: Boolean = true, val recents: List<RecentUi> = emptyList())

sealed interface HomeEffect {
    data class OpenEditor(val documentId: DocumentId) : HomeEffect
}

/** UDF: state from Room, one-shot effects through a channel (ADR-0004). */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val documents: DocumentRepository,
) : ViewModel() {

    val state: StateFlow<HomeState> = documents.observeRecents()
        .map { list -> HomeState(loading = false, recents = list.map(Document::toUi)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** Called with the Photo Picker result. Copying and normalising the images is epic E2 (ImportCoordinator). */
    fun onImagesPicked(count: Int) {
        if (count == 0) return
        viewModelScope.launch {
            val doc = documents.create(DocumentTitle.default(LocalDateTime.now()))
            _effects.send(HomeEffect.OpenEditor(doc.id))
        }
    }
}

private fun Document.toUi() = RecentUi(
    id = id,
    title = title,
    isDraft = status == DocumentStatus.DRAFT,
    exportInterrupted = lastExport?.interrupted == true,
)

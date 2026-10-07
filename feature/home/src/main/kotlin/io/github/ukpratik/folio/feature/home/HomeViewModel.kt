// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ukpratik.folio.core.domain.engine.ImportSource
import io.github.ukpratik.folio.core.domain.error.Outcome
import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.domain.usecase.StartDocumentFromImages
import io.github.ukpratik.folio.core.model.Document
import io.github.ukpratik.folio.core.model.DocumentStatus
import io.github.ukpratik.folio.core.model.Limits
import io.github.ukpratik.folio.core.ui.text.UiText
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** UDF: state derived from Room, one-shot effects through a channel (ADR-0004). */
@HiltViewModel
class HomeViewModel @Inject constructor(
    documents: DocumentRepository,
    private val startDocument: StartDocumentFromImages,
) : ViewModel() {

    val state: StateFlow<HomeState> = documents.observeRecents()
        .map { list -> HomeState(loading = false, recents = list.map(Document::toUi)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.ImagesPicked -> startFrom(intent.uris)
        }
    }

    private fun startFrom(uris: List<String>) {
        viewModelScope.launch {
            when (val outcome = startDocument(uris.map(::ImportSource))) {
                null -> Unit // picker cancelled
                is Outcome.Success -> {
                    val skipped = outcome.value.result.skippedOverLimit
                    if (skipped > 0) _effects.send(HomeEffect.ShowMessage(UiText.Res(R.string.import_page_limit, listOf(Limits.MAX_PAGES))))
                    _effects.send(HomeEffect.OpenEditor(outcome.value.documentId))
                }
                is Outcome.Failure -> _effects.send(HomeEffect.ShowMessage(UiText.Res(R.string.import_page_limit, listOf(Limits.MAX_PAGES))))
            }
        }
    }
}

private fun Document.toUi() = RecentUi(
    id = id,
    title = title,
    isDraft = status == DocumentStatus.DRAFT,
    exportInterrupted = exportInterrupted,
)

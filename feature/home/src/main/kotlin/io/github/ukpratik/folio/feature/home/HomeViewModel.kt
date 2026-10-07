// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ukpratik.folio.core.domain.engine.ExportEngine
import io.github.ukpratik.folio.core.domain.engine.ExportState
import io.github.ukpratik.folio.core.domain.engine.ImportSource
import io.github.ukpratik.folio.core.domain.error.FolioError
import io.github.ukpratik.folio.core.domain.error.Outcome
import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.domain.repository.PageRepository
import io.github.ukpratik.folio.core.domain.repository.PreferencesRepository
import io.github.ukpratik.folio.core.domain.usecase.DeleteDocument
import io.github.ukpratik.folio.core.domain.usecase.RenameDocument
import io.github.ukpratik.folio.core.domain.usecase.SaveExport
import io.github.ukpratik.folio.core.domain.usecase.StartDocumentFromImages
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.DocumentStatus
import io.github.ukpratik.folio.core.model.Limits
import io.github.ukpratik.folio.core.ui.text.UiText
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** UDF: state derived from Room and the export engine, one-shot effects through a channel (ADR-0004). */
@HiltViewModel
class HomeViewModel @Inject constructor(
    documents: DocumentRepository,
    pages: PageRepository,
    exports: ExportEngine,
    preferences: PreferencesRepository,
    private val startDocument: StartDocumentFromImages,
    private val renameDocument: RenameDocument,
    private val deleteDocument: DeleteDocument,
    private val saveExport: SaveExport,
) : ViewModel() {

    val state: StateFlow<HomeState> = combine(
        documents.observeRecents(),
        pages.observeCovers(),
        exports.states,
        preferences.preferences,
    ) { list, covers, running, prefs ->
        HomeState(
            loading = false,
            recents = list.map { document ->
                val cover = covers[document.id]
                RecentUi(
                    id = document.id,
                    title = document.title,
                    isDraft = document.status == DocumentStatus.DRAFT,
                    exportInterrupted = document.exportInterrupted && running[document.id] !is ExportState.Running,
                    pageCount = cover?.pageCount ?: 0,
                    cover = cover?.firstPage,
                    updatedAt = document.updatedAt,
                    export = document.lastExport,
                )
            },
            lastFolderUri = prefs.lastSaveFolderUri,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.ImagesPicked -> startFrom(intent.uris)
            is HomeIntent.Rename -> viewModelScope.launch {
                if (renameDocument(intent.id, intent.title) is Outcome.Failure) message(R.string.rename_empty)
            }
            is HomeIntent.Delete -> viewModelScope.launch { deleteDocument(intent.id) }
            is HomeIntent.SaveTo -> save(intent.id, intent.uri)
        }
    }

    private fun save(id: DocumentId, uri: String) = viewModelScope.launch {
        when (val outcome = saveExport(id, uri)) {
            is Outcome.Success -> outcome.value?.let { message(R.string.home_saved_to, it) } ?: message(R.string.home_saved)
            is Outcome.Failure -> message(
                when (outcome.error) {
                    FolioError.SaveTargetUnavailable -> R.string.home_save_unavailable
                    else -> R.string.home_save_failed
                },
            )
        }
    }

    private fun startFrom(uris: List<String>) {
        viewModelScope.launch {
            when (val outcome = startDocument(uris.map(::ImportSource))) {
                null -> Unit // picker cancelled
                is Outcome.Success -> {
                    val skipped = outcome.value.result.skippedOverLimit
                    if (skipped > 0) message(R.string.import_page_limit, Limits.MAX_PAGES)
                    _effects.send(HomeEffect.OpenEditor(outcome.value.documentId))
                }
                is Outcome.Failure -> message(R.string.import_page_limit, Limits.MAX_PAGES)
            }
        }
    }

    private suspend fun message(id: Int, vararg args: Any) = _effects.send(HomeEffect.ShowMessage(UiText.Res(id, args.toList())))
}

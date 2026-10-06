// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ukpratik.folio.core.domain.engine.ImportEngine
import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.domain.repository.PageRepository
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.ui.text.UiText
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    documents: DocumentRepository,
    pages: PageRepository,
    private val importEngine: ImportEngine,
) : ViewModel() {

    val documentId = DocumentId(checkNotNull(savedState.get<String>(EditorDestination.ARG_DOCUMENT_ID)))

    private val importProgress = importEngine.progress.map { it[documentId] }

    val state: StateFlow<EditorState> = combine(
        documents.observe(documentId),
        pages.observePages(documentId),
        importProgress,
    ) { document, pageList, progress ->
        EditorState(title = document?.title.orEmpty(), pages = pageList, importing = progress)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditorState())

    private val _effects = Channel<EditorEffect>(Channel.BUFFERED)
    val effects: Flow<EditorEffect> = _effects.receiveAsFlow()

    init {
        // When an import batch finishes, summarise failures once, then clear it (UX §8).
        viewModelScope.launch {
            importProgress.filterNotNull().filter { !it.isRunning }.collect { finished ->
                if (finished.failed > 0) {
                    _effects.send(EditorEffect.ShowMessage(UiText.Plural(R.plurals.import_failed, finished.failed)))
                }
                importEngine.acknowledge(documentId)
            }
        }
    }
}

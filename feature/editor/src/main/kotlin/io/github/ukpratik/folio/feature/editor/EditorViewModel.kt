// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ukpratik.folio.core.domain.engine.ImportEngine
import io.github.ukpratik.folio.core.domain.engine.ImportSource
import io.github.ukpratik.folio.core.domain.error.Outcome
import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.domain.repository.PageRepository
import io.github.ukpratik.folio.core.domain.usecase.AddPages
import io.github.ukpratik.folio.core.domain.usecase.DeleteDocument
import io.github.ukpratik.folio.core.domain.usecase.DeletePage
import io.github.ukpratik.folio.core.domain.usecase.DuplicatePage
import io.github.ukpratik.folio.core.domain.usecase.MovePage
import io.github.ukpratik.folio.core.domain.usecase.RenameDocument
import io.github.ukpratik.folio.core.domain.usecase.RestorePage
import io.github.ukpratik.folio.core.domain.usecase.RotatePage
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.Limits
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

/** S3a/b/c. Every change is written immediately (autosave, FR-26); the UI never holds unsaved edits. */
@HiltViewModel
class EditorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    documents: DocumentRepository,
    pages: PageRepository,
    private val importEngine: ImportEngine,
    private val movePage: MovePage,
    private val rotatePage: RotatePage,
    private val duplicatePage: DuplicatePage,
    private val deletePage: DeletePage,
    private val restorePage: RestorePage,
    private val renameDocument: RenameDocument,
    private val addPages: AddPages,
    private val deleteDocument: DeleteDocument,
) : ViewModel() {

    val documentId = DocumentId(checkNotNull(savedState.get<String>(EditorDestination.ARG_DOCUMENT_ID)))

    private val importProgress = importEngine.progress.map { it[documentId] }

    val state: StateFlow<EditorState> = combine(
        documents.observe(documentId),
        pages.observePages(documentId),
        importProgress,
    ) { document, pageList, progress ->
        EditorState(loaded = true, title = document?.title.orEmpty(), pages = pageList, importing = progress)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditorState())

    private val _effects = Channel<EditorEffect>(Channel.BUFFERED)
    val effects: Flow<EditorEffect> = _effects.receiveAsFlow()

    init {
        // When an import batch finishes, summarise failures once, then clear it (UX §8).
        viewModelScope.launch {
            importProgress.filterNotNull().filter { !it.isRunning }.collect { finished ->
                if (finished.failed > 0) send(EditorEffect.ShowMessage(UiText.Plural(R.plurals.import_failed, finished.failed)))
                importEngine.acknowledge(documentId)
            }
        }
    }

    fun onIntent(intent: EditorIntent) {
        viewModelScope.launch {
            when (intent) {
                is EditorIntent.Move -> movePage(documentId, state.value.pages, intent.pageId, intent.toIndex)
                is EditorIntent.Rotate -> rotatePage(intent.pageId)
                is EditorIntent.Duplicate -> duplicate(intent)
                is EditorIntent.Delete -> {
                    deletePage(intent.pageId)
                    send(EditorEffect.ShowUndo(intent.pageId))
                }
                is EditorIntent.UndoDelete -> restorePage(intent.pageId)
                is EditorIntent.Rename -> rename(intent.title)
                is EditorIntent.AddImages -> addImages(intent.uris)
                EditorIntent.DeleteDocument -> {
                    deleteDocument(documentId)
                    send(EditorEffect.Close(message = null))
                }
                EditorIntent.Leave -> leave()
            }
        }
    }

    private suspend fun duplicate(intent: EditorIntent.Duplicate) {
        if (state.value.pages.size >= Limits.MAX_PAGES) {
            send(EditorEffect.ShowMessage(UiText.Res(R.string.editor_page_limit, listOf(Limits.MAX_PAGES))))
        } else {
            duplicatePage(state.value.pages, intent.pageId)
        }
    }

    private suspend fun rename(title: String) {
        if (renameDocument(documentId, title) is Outcome.Failure) send(EditorEffect.ShowMessage(UiText.Res(R.string.rename_empty)))
    }

    private suspend fun addImages(uris: List<String>) {
        when (val outcome = addPages(documentId, uris.map(::ImportSource))) {
            is Outcome.Failure -> send(EditorEffect.ShowMessage(UiText.Res(R.string.editor_page_limit, listOf(Limits.MAX_PAGES))))
            is Outcome.Success -> if (outcome.value.skippedOverLimit > 0) {
                send(EditorEffect.ShowMessage(UiText.Res(R.string.editor_page_limit, listOf(Limits.MAX_PAGES))))
            }
        }
    }

    /** Drafts are saved continuously; a draft with no pages isn't worth keeping in Recents. */
    private suspend fun leave() {
        val current = state.value
        if (current.loaded && current.pages.isEmpty() && !current.isImporting) {
            deleteDocument(documentId)
            send(EditorEffect.Close(message = null))
        } else {
            send(EditorEffect.Close(message = UiText.Res(R.string.editor_saved_draft)))
        }
    }

    private suspend fun send(effect: EditorEffect) = _effects.send(effect)
}

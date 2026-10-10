// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.export.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ukpratik.folio.core.domain.concurrency.IoDispatcher
import io.github.ukpratik.folio.core.domain.error.FolioError
import io.github.ukpratik.folio.core.domain.error.Outcome
import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.domain.repository.PageRepository
import io.github.ukpratik.folio.core.domain.repository.PreferencesRepository
import io.github.ukpratik.folio.core.domain.usecase.ExportInBlackAndWhite
import io.github.ukpratik.folio.core.domain.usecase.RenameDocument
import io.github.ukpratik.folio.core.domain.usecase.SaveExport
import io.github.ukpratik.folio.core.model.ByteSize
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.EnhancementMode
import io.github.ukpratik.folio.core.model.ExportFormat
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.model.ExportResult
import io.github.ukpratik.folio.core.ui.text.UiText
import io.github.ukpratik.folio.feature.export.ExportArgs
import io.github.ukpratik.folio.feature.export.R
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OutputFile(val path: String, val name: String, val size: ByteSize)

data class ResultState(
    val loaded: Boolean = false,
    val title: String = "",
    val export: ExportResult? = null,
    /** One entry per output file (the JPG list, S7e). */
    val files: List<OutputFile> = emptyList(),
    /** FR-18 card: shown right after an export that missed its target, until the user picks an option. */
    val showAlternatives: Boolean = false,
    val lastFolderUri: String? = null,
    /** Every page is already B&W, so "Try black & white" can't help any further. */
    val allBlackAndWhite: Boolean = false,
) {
    /**
     * Whether the size limit was met, judged from the files themselves for JPG (the limit is per image).
     * Exports made by 1.0.0 saved a wrong verdict for JPG (it compared the total), so don't trust the stored flag there.
     */
    val targetMet: Boolean?
        get() {
            val e = export ?: return null
            val target = e.target ?: return null
            return if (e.format == ExportFormat.JPG && files.isNotEmpty()) files.all { it.size.bytes <= target.bytes } else e.targetMet
        }

    val overLimit: Boolean get() = targetMet == false

    /** JPG: how many images are over the per-image limit. */
    val imagesOverLimit: Int
        get() = export?.target?.let { t -> files.count { it.size.bytes > t.bytes } } ?: 0

    /** What "smallest possible" means: the PDF, or for JPG (per-image limit) the largest image. */
    val smallestPossible: ByteSize?
        get() = export?.let { e -> if (e.format == ExportFormat.JPG) files.maxOfOrNull { it.size.bytes }?.let(::ByteSize) ?: e.size else e.size }
}

sealed interface ResultIntent {
    data class Rename(val title: String) : ResultIntent
    data class SaveTo(val uri: String) : ResultIntent
    data object TryBlackAndWhite : ResultIntent
    data object Keep : ResultIntent
}

sealed interface ResultEffect {
    data class ShowMessage(val text: UiText) : ResultEffect

    /** A new export is running (Try black & white). */
    data object Reexporting : ResultEffect

    /** The document is gone (deleted from another screen). */
    data object Close : ResultEffect
}

/** S7a–c, S7e. */
@HiltViewModel
class ResultViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    documents: DocumentRepository,
    pages: PageRepository,
    preferences: PreferencesRepository,
    private val renameDocument: RenameDocument,
    private val saveExport: SaveExport,
    private val exportInBlackAndWhite: ExportInBlackAndWhite,
    @IoDispatcher io: CoroutineDispatcher,
) : ViewModel() {
    private val documentId = DocumentId(checkNotNull(savedState.get<String>(ExportArgs.DOCUMENT_ID)))
    private val offerAlternatives = savedState.getStateFlow(ExportArgs.OFFER_ALTERNATIVES, false)

    private val _effects = Channel<ResultEffect>(Channel.BUFFERED)
    val effects: Flow<ResultEffect> = _effects.receiveAsFlow()

    val state: StateFlow<ResultState> = combine(
        documents.observe(documentId),
        pages.observePages(documentId),
        preferences.preferences,
        offerAlternatives,
    ) { document, pageList, prefs, offer ->
        val export = document?.lastExport
        if (document == null || export == null) {
            _effects.send(ResultEffect.Close)
            return@combine ResultState()
        }
        val state = ResultState(
            loaded = true,
            title = document.title,
            export = export,
            files = export.paths.map { path -> File(path).let { OutputFile(path, it.name, ByteSize(it.length())) } },
            showAlternatives = offer,
            lastFolderUri = prefs.lastSaveFolderUri,
            // Only exported (READY) pages count: a page that failed to import is never in the output.
            allBlackAndWhite = pageList.filter { it.status == PageStatus.READY }.let { ready -> ready.isNotEmpty() && ready.all { it.mode == EnhancementMode.BW } },
        )
        state.copy(showAlternatives = offer && state.overLimit)
    }.flowOn(io).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResultState())

    fun onIntent(intent: ResultIntent) {
        viewModelScope.launch {
            when (intent) {
                is ResultIntent.Rename -> if (renameDocument(documentId, intent.title) is Outcome.Failure) {
                    message(R.string.rename_empty)
                }
                is ResultIntent.SaveTo -> when (val outcome = saveExport(documentId, intent.uri)) {
                    is Outcome.Success -> outcome.value?.let { message(R.string.saved_to, it) } ?: message(R.string.saved)
                    is Outcome.Failure -> message(
                        when (outcome.error) {
                            FolioError.SaveTargetUnavailable -> R.string.save_unavailable
                            FolioError.NothingToExport -> R.string.file_missing
                            else -> R.string.save_failed
                        },
                    )
                }
                ResultIntent.Keep -> savedState[ExportArgs.OFFER_ALTERNATIVES] = false
                ResultIntent.TryBlackAndWhite -> {
                    exportInBlackAndWhite(documentId)
                    _effects.send(ResultEffect.Reexporting)
                }
            }
        }
    }

    private suspend fun message(id: Int, vararg args: Any) = _effects.send(ResultEffect.ShowMessage(UiText.Res(id, args.toList())))
}

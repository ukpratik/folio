// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.export.processing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ukpratik.folio.core.domain.engine.ExportEngine
import io.github.ukpratik.folio.core.domain.engine.ExportPhase
import io.github.ukpratik.folio.core.domain.engine.ExportState
import io.github.ukpratik.folio.core.domain.error.FolioError
import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.domain.usecase.ExportDocument
import io.github.ukpratik.folio.core.model.ByteSize
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.ExportFormat
import io.github.ukpratik.folio.feature.export.ExportArgs
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ProcessingState {
    data class Working(
        val format: ExportFormat,
        val phase: ExportPhase,
        val page: Int,
        val pageCount: Int,
        val target: ByteSize?,
        val progress: Float,
    ) : ProcessingState

    data class LowStorage(val shortBy: ByteSize) : ProcessingState
    data object Failed : ProcessingState
}

sealed interface ProcessingIntent {
    /** Cancel button, system back and predictive back (UX S6). */
    data object Cancel : ProcessingIntent
    data object Retry : ProcessingIntent
    data object Close : ProcessingIntent
}

sealed interface ProcessingEffect {
    data class Finished(val targetMissed: Boolean) : ProcessingEffect

    /** Cancelled, closed after an error, or nothing to show (e.g. the process was restarted). */
    data object Leave : ProcessingEffect
}

/** S6. Mirrors the export engine's state for one document; the engine keeps running if the screen goes away. */
@HiltViewModel
class ProcessingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val engine: ExportEngine,
    private val documents: DocumentRepository,
    private val exportDocument: ExportDocument,
) : ViewModel() {
    private val documentId = DocumentId(checkNotNull(savedState.get<String>(ExportArgs.DOCUMENT_ID)))

    private val _state = MutableStateFlow<ProcessingState>(ProcessingState.Working(ExportFormat.PDF, ExportPhase.PREPARING, 0, 0, null, 0f))
    val state: StateFlow<ProcessingState> = _state.asStateFlow()

    private val _effects = Channel<ProcessingEffect>(Channel.BUFFERED)
    val effects: Flow<ProcessingEffect> = _effects.receiveAsFlow()

    /** Set once this screen has handed off (finished or left); later engine updates are ignored. */
    private var settled = false

    init {
        viewModelScope.launch {
            val settings = documents.get(documentId)?.exportSettings
            engine.states.map { it[documentId] }.distinctUntilChanged().collect { export ->
                when (export) {
                    null -> if (!settled && _state.value is ProcessingState.Working) leave()
                    is ExportState.Running -> _state.value = ProcessingState.Working(
                        format = settings?.format ?: ExportFormat.PDF,
                        phase = export.phase,
                        page = export.pagesDone,
                        pageCount = export.pageCount,
                        target = settings?.target,
                        progress = progressOf(export),
                    )
                    is ExportState.Succeeded -> finish(targetMissed = false)
                    is ExportState.TargetMissed -> finish(targetMissed = true)
                    is ExportState.Failed -> {
                        engine.acknowledge(documentId)
                        _state.value = when (val error = export.error) {
                            is FolioError.LowStorage -> ProcessingState.LowStorage(error.shortBy)
                            FolioError.NothingToExport -> return@collect leave()
                            else -> ProcessingState.Failed
                        }
                    }
                    ExportState.Cancelled -> {
                        engine.acknowledge(documentId)
                        leave()
                    }
                }
            }
        }
    }

    fun onIntent(intent: ProcessingIntent) {
        when (intent) {
            ProcessingIntent.Cancel -> if (_state.value is ProcessingState.Working) {
                engine.cancel(documentId)
            } else {
                _effects.trySend(ProcessingEffect.Leave)
            }
            ProcessingIntent.Close -> _effects.trySend(ProcessingEffect.Leave)
            ProcessingIntent.Retry -> viewModelScope.launch {
                settled = false
                val document = documents.get(documentId) ?: return@launch _effects.send(ProcessingEffect.Leave)
                _state.update { ProcessingState.Working(document.exportSettings.format, ExportPhase.PREPARING, 0, 0, document.exportSettings.target, 0f) }
                exportDocument(documentId, document.exportSettings)
            }
        }
    }

    private suspend fun finish(targetMissed: Boolean) {
        settled = true
        engine.acknowledge(documentId)
        _effects.send(ProcessingEffect.Finished(targetMissed))
    }

    private suspend fun leave() {
        settled = true
        _effects.send(ProcessingEffect.Leave)
    }

    companion object {
        /** Planning is the first ~15 %, encoding the next ~80 %, writing the rest. */
        internal fun progressOf(running: ExportState.Running): Float {
            val count = running.pageCount.coerceAtLeast(1)
            val fraction = running.pagesDone.toFloat() / count
            return when (running.phase) {
                ExportPhase.PREPARING -> 0.15f * fraction
                ExportPhase.ENCODING, ExportPhase.SHRINKING -> 0.15f + 0.8f * fraction
                ExportPhase.WRITING -> 0.97f
            }
        }
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.export.sheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ukpratik.folio.core.domain.engine.ExportEngine
import io.github.ukpratik.folio.core.domain.engine.ExportState
import io.github.ukpratik.folio.core.domain.error.Outcome
import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.domain.usecase.ExportDocument
import io.github.ukpratik.folio.core.domain.usecase.RenameDocument
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.DocumentTitle
import io.github.ukpratik.folio.core.model.ExportFormat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * S5. Pre-filled from the settings this document used last time (or the defaults), so Create works
 * straight away. Create renames the document if the name changed, remembers the settings, and starts the export.
 */
@HiltViewModel(assistedFactory = ExportSheetViewModel.Factory::class)
class ExportSheetViewModel @AssistedInject constructor(
    @Assisted documentIdValue: String,
    private val documents: DocumentRepository,
    private val engine: ExportEngine,
    private val renameDocument: RenameDocument,
    private val exportDocument: ExportDocument,
) : ViewModel() {

    /** Takes the raw id: Hilt can't generate factories for Kotlin value-class parameters. */
    @AssistedFactory
    interface Factory {
        fun create(documentId: String): ExportSheetViewModel
    }

    private val documentId = DocumentId(documentIdValue)

    private val _state = MutableStateFlow(ExportSheetState())
    val state: StateFlow<ExportSheetState> = _state.asStateFlow()

    private val _effects = Channel<ExportSheetEffect>(Channel.BUFFERED)
    val effects: Flow<ExportSheetEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            if (engine.states.value[documentId] is ExportState.Running) {
                _effects.send(ExportSheetEffect.Started) // already exporting: go straight to its progress
                return@launch
            }
            val document = documents.get(documentId) ?: return@launch
            val settings = document.exportSettings
            val target = settings.target
            val preset = SizeChoice.PRESETS.firstOrNull { it.size == target }
            _state.value = ExportSheetState(
                loaded = true,
                name = document.title,
                format = settings.format,
                size = when {
                    target == null -> SizeChoice.None
                    preset != null -> preset
                    else -> SizeChoice.Custom
                },
                customValue = if (target != null && preset == null) customText(target.bytes) else "",
                customUnit = if (target != null && preset == null && target.bytes >= SizeUnit.MB.bytes) SizeUnit.MB else SizeUnit.KB,
                quality = settings.quality,
                pageSize = settings.pageSize,
                orientation = settings.orientation,
                margin = settings.margin,
            )
        }
    }

    fun onIntent(intent: ExportSheetIntent) {
        when (intent) {
            is ExportSheetIntent.NameChanged -> _state.update { it.copy(name = intent.name.take(DocumentTitle.MAX_LENGTH)) }
            is ExportSheetIntent.FormatChanged -> _state.update { it.copy(format = intent.format) }
            is ExportSheetIntent.SizeChosen -> _state.update { it.copy(size = intent.choice, showCustomError = false) }
            is ExportSheetIntent.CustomValueChanged -> _state.update {
                it.copy(customValue = intent.text.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6), showCustomError = false)
            }
            is ExportSheetIntent.CustomUnitChanged -> _state.update { it.copy(customUnit = intent.unit, showCustomError = false) }
            is ExportSheetIntent.QualityChanged -> _state.update { it.copy(quality = intent.quality) }
            is ExportSheetIntent.PageSizeChanged -> _state.update { it.copy(pageSize = intent.pageSize) }
            is ExportSheetIntent.OrientationChanged -> _state.update { it.copy(orientation = intent.orientation) }
            is ExportSheetIntent.MarginChanged -> _state.update { it.copy(margin = intent.margin) }
            ExportSheetIntent.Create -> create()
        }
    }

    private fun create() {
        val current = _state.value
        if (!current.canCreate) return
        if (current.size == SizeChoice.Custom && current.customTarget == null) {
            _state.update { it.copy(showCustomError = true) }
            return
        }
        _state.update { it.copy(starting = true) }
        viewModelScope.launch {
            val existing = documents.get(documentId)?.title
            if (current.name.trim() != existing && renameDocument(documentId, current.name) is Outcome.Failure) {
                _state.update { it.copy(starting = false) }
                return@launch
            }
            exportDocument(documentId, current.toSettings())
            _effects.send(ExportSheetEffect.Started)
        }
    }

    private companion object {
        fun customText(bytes: Long): String {
            val unit = if (bytes >= SizeUnit.MB.bytes) SizeUnit.MB else SizeUnit.KB
            val value = bytes.toDouble() / unit.bytes
            return if (value % 1.0 == 0.0) value.toLong().toString() else "%.1f".format(java.util.Locale.ROOT, value)
        }
    }
}

/** File extension shown after the name field. */
internal val ExportFormat.extension: String get() = if (this == ExportFormat.PDF) ".pdf" else ".jpg"

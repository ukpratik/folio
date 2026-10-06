// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.capture

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ukpratik.folio.core.domain.engine.ImportSource
import io.github.ukpratik.folio.core.domain.error.Outcome
import io.github.ukpratik.folio.core.domain.files.DocumentFiles
import io.github.ukpratik.folio.core.domain.repository.PreferencesRepository
import io.github.ukpratik.folio.core.domain.usecase.AddPages
import io.github.ukpratik.folio.core.domain.usecase.StartDocumentFromImages
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.Limits
import io.github.ukpratik.folio.core.ui.text.UiText
import java.io.File
import java.net.URI
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * S2a/b/c. Batch capture (FR-02): shots are written to app-private work files and only turned into pages when
 * the user taps Done (or Keep on close), through the same use cases as imports (DRY).
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    savedState: SavedStateHandle,
    val frameAnalyzer: DocumentFrameAnalyzer,
    private val preferences: PreferencesRepository,
    private val files: DocumentFiles,
    private val addPages: AddPages,
    private val startDocument: StartDocumentFromImages,
) : ViewModel() {

    /** null = scanning starts a new document. */
    private val documentId = savedState.get<String>(CameraDestination.ARG_DOCUMENT_ID)?.let(::DocumentId)

    private val _state = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state.asStateFlow()

    private val _effects = Channel<CameraEffect>(Channel.BUFFERED)
    val effects: Flow<CameraEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: CameraIntent) {
        when (intent) {
            is CameraIntent.PermissionChecked -> viewModelScope.launch {
                val asked = preferences.preferences.first().cameraPermissionRequested
                _state.update { it.copy(access = CameraPermission.resolve(intent.granted, asked, intent.shouldShowRationale)) }
            }
            CameraIntent.RationaleAccepted -> viewModelScope.launch {
                preferences.update { it.copy(cameraPermissionRequested = true) }
                send(CameraEffect.RequestPermission)
            }
            is CameraIntent.PermissionResult -> _state.update { it.copy(access = CameraPermission.afterRequest(intent.granted)) }
            CameraIntent.Shutter -> shutter()
            is CameraIntent.Captured -> _state.update { it.copy(captured = it.captured + intent.uri, capturing = false) }
            CameraIntent.CaptureFailed -> {
                _state.update { it.copy(capturing = false) }
                send(CameraEffect.ShowMessage(UiText.Res(R.string.camera_capture_failed)))
            }
            CameraIntent.ToggleTorch -> _state.update { it.copy(torchOn = !it.torchOn) }
            CameraIntent.Done, CameraIntent.KeepPages -> finish(_state.value.captured)
            CameraIntent.Close -> if (_state.value.captured.isEmpty()) send(CameraEffect.Close) else _state.update { it.copy(confirmClose = true) }
            CameraIntent.DismissCloseDialog -> _state.update { it.copy(confirmClose = false) }
            CameraIntent.DiscardPages -> {
                deleteCaptures()
                send(CameraEffect.Close)
            }
            is CameraIntent.ImagesPicked -> finish(intent.uris)
        }
    }

    private fun shutter() {
        val current = _state.value
        if (current.capturing) return
        if (current.captured.size >= Limits.MAX_PAGES) {
            send(CameraEffect.ShowMessage(UiText.Res(R.string.camera_page_limit, listOf(Limits.MAX_PAGES))))
            return
        }
        _state.update { it.copy(capturing = true) }
        send(CameraEffect.TakePicture(files.newWorkFile("capture", "jpg")))
    }

    private fun finish(uris: List<String>) {
        if (_state.value.finishing) return
        if (uris.isEmpty()) {
            send(CameraEffect.Close)
            return
        }
        _state.update { it.copy(finishing = true, confirmClose = false) }
        viewModelScope.launch {
            val sources = uris.map(::ImportSource)
            val effect = if (documentId == null) {
                when (val outcome = startDocument(sources)) {
                    is Outcome.Success -> CameraEffect.OpenEditor(outcome.value.documentId, isNewDocument = true)
                    else -> CameraEffect.ShowMessage(UiText.Res(R.string.camera_page_limit, listOf(Limits.MAX_PAGES)))
                }
            } else {
                when (addPages(documentId, sources)) {
                    is Outcome.Success -> CameraEffect.OpenEditor(documentId, isNewDocument = false)
                    is Outcome.Failure -> CameraEffect.ShowMessage(UiText.Res(R.string.camera_page_limit, listOf(Limits.MAX_PAGES)))
                }
            }
            _state.update { it.copy(finishing = false) }
            send(effect)
        }
    }

    private fun deleteCaptures() {
        _state.value.captured.forEach { uri -> runCatching { File(URI(uri)).delete() } }
        _state.update { it.copy(captured = emptyList(), confirmClose = false) }
    }

    private fun send(effect: CameraEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}

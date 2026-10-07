// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ukpratik.folio.core.domain.engine.ImportSource
import io.github.ukpratik.folio.core.domain.error.Outcome
import io.github.ukpratik.folio.core.domain.update.AppUpdates
import io.github.ukpratik.folio.core.domain.update.DecideUpdatePrompt
import io.github.ukpratik.folio.core.domain.update.UpdatePrompt
import io.github.ukpratik.folio.core.domain.usecase.StartDocumentFromImages
import io.github.ukpratik.folio.core.model.DocumentId
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface MainEffect {
    data class OpenEditor(val documentId: DocumentId) : MainEffect
}

/**
 * Handles "Share → Folio" (FR-05) with the same use case as the Photo Picker (DRY), and the update prompt
 * (D-48): checked whenever Folio comes to the foreground.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val startDocument: StartDocumentFromImages,
    private val appUpdates: AppUpdates,
    private val decideUpdatePrompt: DecideUpdatePrompt,
) : ViewModel() {
    private val _effects = Channel<MainEffect>(Channel.BUFFERED)
    val effects: Flow<MainEffect> = _effects.receiveAsFlow()

    private val _updatePrompt = MutableStateFlow<UpdatePrompt?>(null)
    val updatePrompt: StateFlow<UpdatePrompt?> = _updatePrompt.asStateFlow()

    fun checkForUpdate() {
        viewModelScope.launch { _updatePrompt.value = decideUpdatePrompt(appUpdates.available()) }
    }

    /** "Later" on a non-critical update. */
    fun postponeUpdate() {
        _updatePrompt.value = null
        viewModelScope.launch { decideUpdatePrompt.snooze() }
    }

    /** Play's update screen is showing; a critical prompt comes back on the next resume if it's cancelled. */
    fun updateStarted() {
        _updatePrompt.value = null
    }

    fun onImagesShared(uris: List<String>) {
        viewModelScope.launch {
            val outcome = startDocument(uris.map(::ImportSource))
            if (outcome is Outcome.Success) _effects.send(MainEffect.OpenEditor(outcome.value.documentId))
        }
    }
}

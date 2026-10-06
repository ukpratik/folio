// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ukpratik.folio.core.domain.engine.ImportSource
import io.github.ukpratik.folio.core.domain.error.Outcome
import io.github.ukpratik.folio.core.domain.usecase.StartDocumentFromImages
import io.github.ukpratik.folio.core.model.DocumentId
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface MainEffect {
    data class OpenEditor(val documentId: DocumentId) : MainEffect
}

/** Handles "Share → Folio" (FR-05) with the same use case as the Photo Picker (DRY). */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val startDocument: StartDocumentFromImages,
) : ViewModel() {
    private val _effects = Channel<MainEffect>(Channel.BUFFERED)
    val effects: Flow<MainEffect> = _effects.receiveAsFlow()

    fun onImagesShared(uris: List<String>) {
        viewModelScope.launch {
            val outcome = startDocument(uris.map(::ImportSource))
            if (outcome is Outcome.Success) _effects.send(MainEffect.OpenEditor(outcome.value.documentId))
        }
    }
}

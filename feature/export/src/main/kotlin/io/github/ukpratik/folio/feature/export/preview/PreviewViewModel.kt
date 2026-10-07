// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.export.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.model.ByteSize
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.ExportFormat
import io.github.ukpratik.folio.feature.export.ExportArgs
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PreviewState(
    val title: String = "",
    val path: String? = null,
    val pageCount: Int = 0,
    val size: ByteSize? = null,
    /** The document or its PDF is gone (deleted elsewhere): close. */
    val missing: Boolean = false,
)

@HiltViewModel
class PreviewViewModel @Inject constructor(savedState: SavedStateHandle, documents: DocumentRepository) : ViewModel() {
    private val documentId = DocumentId(checkNotNull(savedState.get<String>(ExportArgs.DOCUMENT_ID)))

    val state: StateFlow<PreviewState> = documents.observe(documentId).map { document ->
        val export = document?.lastExport?.takeIf { it.format == ExportFormat.PDF }
        if (document == null || export == null) {
            PreviewState(missing = true)
        } else {
            PreviewState(document.title, export.paths.single(), export.pageCount, export.size)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PreviewState())
}

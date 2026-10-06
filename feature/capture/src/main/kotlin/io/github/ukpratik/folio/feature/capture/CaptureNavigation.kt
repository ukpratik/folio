// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.capture

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.ukpratik.folio.core.model.DocumentId
import kotlinx.serialization.Serializable

/** [documentId] null = scanning starts a new draft; otherwise pages are added to it (FR-06). */
@Serializable data class CameraDestination(val documentId: String? = null) {
    companion object {
        const val ARG_DOCUMENT_ID = "documentId"
    }
}

fun NavGraphBuilder.cameraScreen(onClose: () -> Unit, onOpenEditor: (DocumentId, isNewDocument: Boolean) -> Unit) {
    composable<CameraDestination> { CameraRoute(onClose = onClose, onOpenEditor = onOpenEditor) }
}

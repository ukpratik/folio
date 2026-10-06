// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.PageId
import kotlinx.serialization.Serializable

@Serializable data class EditorDestination(val documentId: String) {
    companion object {
        /** SavedStateHandle key — matches the property name above. */
        const val ARG_DOCUMENT_ID = "documentId"
    }
}

fun NavController.navigateToEditor(id: DocumentId) = navigate(EditorDestination(id.value)) { launchSingleTop = true }

fun NavGraphBuilder.editorScreen(
    onClose: () -> Unit,
    onScan: (DocumentId) -> Unit,
    onOpenPage: (DocumentId, PageId) -> Unit,
    onCreatePdf: (DocumentId) -> Unit,
) {
    composable<EditorDestination> {
        EditorRoute(onClose = onClose, onScan = onScan, onOpenPage = onOpenPage, onCreatePdf = onCreatePdf)
    }
}

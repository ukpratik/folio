// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor.detail

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.PageId
import kotlinx.serialization.Serializable

@Serializable data class PageDetailDestination(val documentId: String, val pageId: String) {
    companion object {
        const val ARG_DOCUMENT_ID = "documentId"
        const val ARG_PAGE_ID = "pageId"
    }
}

fun NavController.navigateToPageDetail(documentId: DocumentId, pageId: PageId) =
    navigate(PageDetailDestination(documentId.value, pageId.value)) { launchSingleTop = true }

fun NavGraphBuilder.pageDetailScreen(onClose: () -> Unit) {
    composable<PageDetailDestination> { PageDetailRoute(onClose = onClose) }
}

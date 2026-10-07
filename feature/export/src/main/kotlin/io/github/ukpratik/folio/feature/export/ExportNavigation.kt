// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.export

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.feature.export.preview.PreviewRoute
import io.github.ukpratik.folio.feature.export.processing.ProcessingRoute
import io.github.ukpratik.folio.feature.export.result.ResultRoute
import kotlinx.serialization.Serializable

/** S6 Processing. */
@Serializable data class ProcessingDestination(val documentId: String)

/** S7 Result. [offerAlternatives]: just exported and missed the size limit, so show the FR-18 options first. */
@Serializable data class ResultDestination(val documentId: String, val offerAlternatives: Boolean = false)

/** S7d PDF preview. */
@Serializable data class PreviewDestination(val documentId: String)

/** SavedStateHandle keys — they match the destination property names above. */
internal object ExportArgs {
    const val DOCUMENT_ID = "documentId"
    const val OFFER_ALTERNATIVES = "offerAlternatives"
}

fun NavGraphBuilder.exportGraph(
    onExportFinished: (DocumentId, targetMissed: Boolean) -> Unit,
    onBackToPages: (DocumentId) -> Unit,
    onReexport: (DocumentId) -> Unit,
    onOpenPreview: (DocumentId) -> Unit,
    onCloseResult: () -> Unit,
    onBack: () -> Unit,
) {
    composable<ProcessingDestination> { entry ->
        val id = DocumentId(entry.toRoute<ProcessingDestination>().documentId)
        ProcessingRoute(onFinished = { missed -> onExportFinished(id, missed) }, onLeave = { onBackToPages(id) })
    }
    composable<ResultDestination> { entry ->
        val id = DocumentId(entry.toRoute<ResultDestination>().documentId)
        ResultRoute(
            onClose = onCloseResult,
            onEdit = { onBackToPages(id) },
            onReexport = { onReexport(id) },
            onPreview = { onOpenPreview(id) },
        )
    }
    composable<PreviewDestination> { PreviewRoute(onBack = onBack) }
}

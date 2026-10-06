// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.export

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable data class ExportDestination(val documentId: String)

fun NavGraphBuilder.exportScreen(onBack: () -> Unit) {
    composable<ExportDestination> { entry ->
        ExportScreen(onBack = onBack, detail = entry.toRoute<ExportDestination>().documentId)
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.ukpratik.folio.feature.capture.CameraScreen
import io.github.ukpratik.folio.feature.editor.EditorScreen
import io.github.ukpratik.folio.feature.export.ExportScreen
import io.github.ukpratik.folio.feature.home.HomeRoute
import io.github.ukpratik.folio.feature.home.PrivacyScreen
import io.github.ukpratik.folio.feature.home.SettingsScreen
import kotlinx.serialization.Serializable

// Type-safe routes carry IDs only; screens load state from Room (ADR-0015, LLD §7).
@Serializable data object HomeDestination
@Serializable data object SettingsDestination
@Serializable data object PrivacyDestination
@Serializable data class CameraDestination(val documentId: String? = null)
@Serializable data class EditorDestination(val documentId: String)
@Serializable data class ExportDestination(val documentId: String)

@Composable
fun FolioNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = HomeDestination) {
        composable<HomeDestination> {
            HomeRoute(
                onScan = { nav.navigate(CameraDestination()) },
                onOpenDocument = { nav.navigate(EditorDestination(it.value)) },
                onSettings = { nav.navigate(SettingsDestination) },
                onPrivacy = { nav.navigate(PrivacyDestination) },
            )
        }
        composable<SettingsDestination> {
            SettingsScreen(onBack = nav::popBackStack, onPrivacy = { nav.navigate(PrivacyDestination) })
        }
        composable<PrivacyDestination> { PrivacyScreen(onBack = nav::popBackStack) }
        composable<CameraDestination> { CameraScreen(onBack = nav::popBackStack) }
        composable<EditorDestination> { entry ->
            val id = entry.toRoute<EditorDestination>().documentId
            EditorScreen(onBack = nav::popBackStack, detail = "Document $id")
        }
        composable<ExportDestination> { ExportScreen(onBack = nav::popBackStack) }
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import io.github.ukpratik.folio.feature.capture.CameraDestination
import io.github.ukpratik.folio.feature.capture.cameraScreen
import io.github.ukpratik.folio.feature.editor.detail.navigateToPageDetail
import io.github.ukpratik.folio.feature.editor.detail.pageDetailScreen
import io.github.ukpratik.folio.feature.editor.EditorDestination
import io.github.ukpratik.folio.feature.editor.editorScreen
import io.github.ukpratik.folio.feature.editor.navigateToEditor
import io.github.ukpratik.folio.feature.export.ExportDestination
import io.github.ukpratik.folio.feature.export.exportScreen
import io.github.ukpratik.folio.feature.home.HomeDestination
import io.github.ukpratik.folio.feature.home.PrivacyDestination
import io.github.ukpratik.folio.feature.home.SettingsDestination
import io.github.ukpratik.folio.feature.home.homeGraph
import kotlinx.coroutines.flow.Flow

/** Composes the feature graphs. Features never reference each other; all cross-feature navigation lives here. */
@Composable
fun FolioNavHost(mainEffects: Flow<MainEffect>) {
    val nav = rememberNavController()
    LaunchedEffect(nav) {
        mainEffects.collect { effect ->
            when (effect) {
                is MainEffect.OpenEditor -> nav.navigateToEditor(effect.documentId)
            }
        }
    }
    NavHost(navController = nav, startDestination = HomeDestination) {
        homeGraph(
            onScan = { nav.navigate(CameraDestination()) },
            onOpenDocument = nav::navigateToEditor,
            onOpenSettings = { nav.navigate(SettingsDestination) },
            onOpenPrivacy = { nav.navigate(PrivacyDestination) },
            onBack = { nav.popBackStack() },
        )
        cameraScreen(
            onClose = { nav.popBackStack() },
            onOpenEditor = { id, isNew ->
                if (isNew) {
                    // A new scan replaces the camera with its editor, so Back goes Home.
                    nav.navigate(EditorDestination(id.value)) { popUpTo<CameraDestination> { inclusive = true } }
                } else {
                    nav.popBackStack() // back to the editor we came from; it observes the new pages
                }
            },
        )
        editorScreen(
            onClose = { nav.popBackStack() },
            onScan = { nav.navigate(CameraDestination(it.value)) },
            onOpenPage = nav::navigateToPageDetail,
            onCreatePdf = { nav.navigate(ExportDestination(it.value)) },
        )
        pageDetailScreen(onClose = { nav.popBackStack() })
        exportScreen(onBack = { nav.popBackStack() })
    }
}

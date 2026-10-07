// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.feature.capture.CameraDestination
import io.github.ukpratik.folio.feature.capture.cameraScreen
import io.github.ukpratik.folio.feature.editor.EditorDestination
import io.github.ukpratik.folio.feature.editor.detail.navigateToPageDetail
import io.github.ukpratik.folio.feature.editor.detail.pageDetailScreen
import io.github.ukpratik.folio.feature.editor.editorScreen
import io.github.ukpratik.folio.feature.editor.navigateToEditor
import io.github.ukpratik.folio.feature.export.PreviewDestination
import io.github.ukpratik.folio.feature.export.ProcessingDestination
import io.github.ukpratik.folio.feature.export.ResultDestination
import io.github.ukpratik.folio.feature.export.exportGraph
import io.github.ukpratik.folio.feature.export.sheet.ExportSheetRoute
import io.github.ukpratik.folio.feature.home.HomeDestination
import io.github.ukpratik.folio.feature.home.homeGraph
import io.github.ukpratik.folio.feature.settings.LicencesDestination
import io.github.ukpratik.folio.feature.settings.PrivacyDestination
import io.github.ukpratik.folio.feature.settings.SettingsDestination
import io.github.ukpratik.folio.feature.settings.settingsGraph
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
            onOpenDraft = nav::navigateToEditor,
            onOpenExported = { nav.navigate(ResultDestination(it.value)) { launchSingleTop = true } },
            onOpenSettings = { nav.navigate(SettingsDestination) },
            onOpenPrivacy = { nav.navigate(PrivacyDestination) },
        )
        settingsGraph(
            onBack = { nav.popBackStack() },
            onOpenPrivacy = { nav.navigate(PrivacyDestination) },
            onOpenLicences = { nav.navigate(LicencesDestination) },
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
            exportSheet = { id, onDismiss ->
                ExportSheetRoute(id, onStarted = { nav.navigate(ProcessingDestination(id.value)) }, onDismiss = onDismiss)
            },
        )
        pageDetailScreen(onClose = { nav.popBackStack() })
        exportGraph(
            // The result replaces everything above Home: Back from a finished document goes Home (S7).
            onExportFinished = { id, missed ->
                nav.navigate(ResultDestination(id.value, offerAlternatives = missed)) { popUpTo<HomeDestination>() }
            },
            onBackToPages = nav::backToEditor,
            onReexport = { id -> nav.navigate(ProcessingDestination(id.value)) { popUpTo<HomeDestination>() } },
            onOpenPreview = { nav.navigate(PreviewDestination(it.value)) },
            onCloseResult = { nav.popBackStack<HomeDestination>(inclusive = false) },
            onBack = { nav.popBackStack() },
        )
    }
}

/** Cancel / Remove pages / Edit (UX S6, S7): the editor it came from if it's still there, else a fresh one above Home. */
private fun NavController.backToEditor(id: DocumentId) {
    if (!popBackStack<EditorDestination>(inclusive = false)) {
        navigate(EditorDestination(id.value)) { popUpTo<HomeDestination>() }
    }
}

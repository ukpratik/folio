// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.capture

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** [documentId] null = scanning starts a new draft. */
@Serializable data class CameraDestination(val documentId: String? = null)

fun NavGraphBuilder.cameraScreen(onBack: () -> Unit) {
    composable<CameraDestination> { CameraScreen(onBack = onBack) }
}

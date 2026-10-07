// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.ukpratik.folio.core.model.DocumentId
import kotlinx.serialization.Serializable

// Each feature owns its routes; :app only composes them (ADR-0005, ADR-0015).
@Serializable data object HomeDestination

fun NavGraphBuilder.homeGraph(
    onScan: () -> Unit,
    onOpenDraft: (DocumentId) -> Unit,
    onOpenExported: (DocumentId) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    composable<HomeDestination> {
        HomeRoute(
            onScan = onScan,
            onOpenDraft = onOpenDraft,
            onOpenExported = onOpenExported,
            onSettings = onOpenSettings,
            onPrivacy = onOpenPrivacy,
        )
    }
}

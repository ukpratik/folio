// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.ukpratik.folio.core.model.DocumentId
import kotlinx.serialization.Serializable

// Each feature owns its routes; :app only composes them (ADR-0005, ADR-0015).
@Serializable data object HomeDestination
@Serializable data object SettingsDestination
@Serializable data object PrivacyDestination

fun NavGraphBuilder.homeGraph(
    onScan: () -> Unit,
    onOpenDraft: (DocumentId) -> Unit,
    onOpenExported: (DocumentId) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onBack: () -> Unit,
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
    composable<SettingsDestination> { SettingsScreen(onBack = onBack, onPrivacy = onOpenPrivacy) }
    composable<PrivacyDestination> { PrivacyScreen(onBack = onBack) }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object SettingsDestination
@Serializable data object PrivacyDestination
@Serializable data object LicencesDestination

fun NavGraphBuilder.settingsGraph(onBack: () -> Unit, onOpenPrivacy: () -> Unit, onOpenLicences: () -> Unit) {
    composable<SettingsDestination> { SettingsRoute(onBack = onBack, onPrivacy = onOpenPrivacy, onLicences = onOpenLicences) }
    composable<PrivacyDestination> { PrivacyScreen(onBack = onBack) }
    composable<LicencesDestination> { LicencesRoute(onBack = onBack) }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.model

/**
 * Build facts that screens show or link to (Settings, FR-34). Provided by :app from BuildConfig, so
 * feature modules don't need the app's BuildConfig or resources.
 */
data class AppInfo(
    val versionName: String,
    /** Store listing: Google Play or F-Droid, per flavour (ADR-0021). */
    val rateUrl: String,
    /** Feedback recipient; blank means the user picks one in their email app. */
    val feedbackEmail: String,
    /** Raw resource with the generated open-source licence list (AboutLibraries). */
    val licencesResId: Int,
    /** Play build only: update prompts come from the Play Store app (F-Droid's client handles its own). */
    val supportsUpdatePrompts: Boolean = false,
)

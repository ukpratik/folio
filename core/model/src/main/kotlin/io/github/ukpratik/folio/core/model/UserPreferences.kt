// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.model

/** Settings defaults and first-run flags (FR-34, LLD §6.2). */
data class UserPreferences(
    /** null = derive from the device region (PRD §8). */
    val defaultPageSize: PageSize? = null,
    val defaultQuality: QualityPreset = QualityPreset.BALANCED,
    /** Distinguishes "never asked" from "permanently denied" for the camera permission. */
    val cameraPermissionRequested: Boolean = false,
    val lastSaveFolderUri: String? = null,
    /** How often to remind about a non-critical update (Play build only). Critical updates are always required. */
    val updateReminder: UpdateReminder = UpdateReminder.EVERY_LAUNCH,
    /** When the user last tapped "Later" on an update prompt (epoch millis). */
    val lastUpdatePromptAt: Long? = null,
)

enum class UpdateReminder(val intervalMillis: Long) {
    EVERY_LAUNCH(0),
    DAILY(24 * 60 * 60 * 1000L),
    WEEKLY(7 * 24 * 60 * 60 * 1000L),
}

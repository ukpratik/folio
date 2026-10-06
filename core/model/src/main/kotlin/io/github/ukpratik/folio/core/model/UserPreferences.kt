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
)

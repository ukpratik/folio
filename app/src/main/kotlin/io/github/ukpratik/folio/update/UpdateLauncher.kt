// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest

/** Starts the store's own update screen for the update [io.github.ukpratik.folio.core.domain.update.AppUpdates] found. */
interface UpdateLauncher {
    /** Returns false if there's nothing to start (no update, or the store refused). */
    fun launch(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean
}

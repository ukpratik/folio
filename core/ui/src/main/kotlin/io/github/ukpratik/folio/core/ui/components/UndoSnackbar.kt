// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.withTimeoutOrNull

/** FR-09: Undo stays about 5 s (M3's Short is 4 s, Long is 10 s). */
const val UNDO_WINDOW_MS = 5_000L

/**
 * Shows a single "… · UNDO" snackbar, replacing any current one, for [windowMs].
 * Returns true only if the user tapped the action in time.
 */
suspend fun SnackbarHostState.showUndo(message: String, actionLabel: String, windowMs: Long = UNDO_WINDOW_MS): Boolean {
    currentSnackbarData?.dismiss()
    val result = withTimeoutOrNull(windowMs) {
        showSnackbar(message, actionLabel = actionLabel, duration = SnackbarDuration.Indefinite)
    }
    if (result == null) currentSnackbarData?.dismiss()
    return result == SnackbarResult.ActionPerformed
}

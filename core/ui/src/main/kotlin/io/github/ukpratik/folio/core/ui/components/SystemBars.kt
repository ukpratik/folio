// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.ui.components

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat

/**
 * For screens that are always dark (camera, page detail): light status/navigation bar icons while the screen
 * is shown, restoring the theme's choice when it leaves. Otherwise the clock and icons are dark-on-dark.
 */
@Composable
fun DarkScreenSystemBars() {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        val status = controller.isAppearanceLightStatusBars
        val navigation = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        onDispose {
            controller.isAppearanceLightStatusBars = status
            controller.isAppearanceLightNavigationBars = navigation
        }
    }
}

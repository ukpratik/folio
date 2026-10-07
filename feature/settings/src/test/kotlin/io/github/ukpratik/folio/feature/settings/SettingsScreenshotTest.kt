// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.ukpratik.folio.core.model.AppInfo
import io.github.ukpratik.folio.core.model.PageSize
import io.github.ukpratik.folio.core.ui.theme.FolioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Settings and Privacy boards (S8), phone 390×844 dp. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class SettingsScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val state = SettingsState(loaded = true, pageSize = PageSize.A4, appInfo = AppInfo("1.0.0", "", "", 0, supportsUpdatePrompts = true))

    private fun capture(name: String, dark: Boolean = false, content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent { FolioTheme(darkTheme = dark) { content() } }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test fun settings() = capture("settings") { SettingsScreen(state, {}, {}, {}, {}, {}, {}) }

    @Test fun settingsDark() = capture("settings_dark", dark = true) { SettingsScreen(state, {}, {}, {}, {}, {}, {}) }

    @Test fun privacy() = capture("privacy") { PrivacyScreen {} }
}

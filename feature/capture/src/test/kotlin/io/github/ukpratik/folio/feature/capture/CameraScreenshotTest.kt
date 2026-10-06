// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.ukpratik.folio.core.ui.theme.FolioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Design boards S2a (rationale), S2b (camera chrome; preview stubbed), S2c (denied). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class CameraScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun capture(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent { FolioTheme(darkTheme = true) { Box(Modifier.fillMaxSize().background(Color(0xFF0E1012))) { content() } } }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test fun rationale() = capture("camera_rationale") { PermissionRationale(onContinue = {}, onNotNow = {}) }

    @Test fun denied() = capture("camera_denied") { CameraDenied(onClose = {}, onImport = {}, onOpenSettings = {}) }

    @Test fun cameraWithThreePages() = capture("camera_scanning") {
        CameraScreen(
            state = CameraState(access = CameraAccess.GRANTED, captured = listOf("a", "b", "c")),
            frame = FrameState(),
            hasTorch = true,
            onIntent = {},
            preview = { Box(Modifier.fillMaxSize().background(Color(0xFF3B3430))) },
        )
    }
}

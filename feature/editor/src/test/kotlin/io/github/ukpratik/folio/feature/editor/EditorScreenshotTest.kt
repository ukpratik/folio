// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.ukpratik.folio.core.domain.engine.ImportProgress
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.ui.theme.FolioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Visual checks against the design canvas boards S3a/S3c + empty state (phone 390×844 dp). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class EditorScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val doc = DocumentId("d")
    private fun pages(n: Int, importingFrom: Int = n) = (0 until n).map {
        Page(PageId("p$it"), doc, it, "s$it", status = if (it >= importingFrom) PageStatus.IMPORTING else PageStatus.READY)
    }

    private fun capture(name: String, state: EditorState, dark: Boolean = false) {
        compose.setContent {
            // Inspection mode draws placeholder "paper" instead of rendering real page images.
            CompositionLocalProvider(LocalInspectionMode provides true) {
                FolioTheme(darkTheme = dark) {
                    EditorScreen(state, SnackbarHostState(), onIntent = {}, onBack = {}, onScan = {}, onImport = {}, onOpenPage = {}, onCreatePdf = {})
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test fun editorWithPages() = capture("editor_pages", EditorState(true, "Folio_20261006_1430", pages(5)))

    @Test fun editorWithPagesDark() = capture("editor_pages_dark", EditorState(true, "Folio_20261006_1430", pages(5)), dark = true)

    @Test fun editorImporting() = capture(
        "editor_importing",
        EditorState(true, "Folio_20261006_1502", pages(6, importingFrom = 4), ImportProgress(total = 12, done = 4)),
    )

    @Test fun editorEmpty() = capture("editor_empty", EditorState(true, "Folio_20261006_1430", emptyList()))
}

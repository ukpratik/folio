// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor.detail

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.model.PointF01
import io.github.ukpratik.folio.core.model.Quad
import io.github.ukpratik.folio.core.ui.theme.FolioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Visual checks against design boards S4a (crop) and S4b (enhance). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class PageDetailScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val corners = Quad(PointF01(0.18f, 0.12f), PointF01(0.84f, 0.08f), PointF01(0.89f, 0.88f), PointF01(0.12f, 0.92f))
    private val pages = (0 until 5).map { Page(PageId("p$it"), DocumentId("d"), it, "s$it", corners = corners, status = PageStatus.READY) }

    /** A photographed page on a dark table, drawn in code. */
    private fun photo(): Bitmap = Bitmap.createBitmap(900, 1200, Bitmap.Config.ARGB_8888).apply {
        val canvas = Canvas(this)
        canvas.drawColor(Color.rgb(59, 52, 48))
        val path = android.graphics.Path().apply {
            moveTo(162f, 144f); lineTo(756f, 96f); lineTo(801f, 1056f); lineTo(108f, 1104f); close()
        }
        canvas.drawPath(path, Paint().apply { color = Color.rgb(244, 242, 236) })
        val ink = Paint().apply { color = Color.rgb(60, 60, 60); strokeWidth = 7f }
        for (y in 260..900 step 40) canvas.drawLine(260f, y.toFloat(), 640f, y.toFloat(), ink)
    }

    private fun capture(name: String, tab: DetailTab) {
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                FolioTheme(darkTheme = true) {
                    PageDetailScreen(
                        state = PageDetailState(loaded = true, pages = pages, currentIndex = 1, tab = tab),
                        snackbar = SnackbarHostState(),
                        onIntent = {},
                        onClose = {},
                        cropImageOverride = photo().asImageBitmap(),
                    )
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test fun cropTab() = capture("detail_crop", DetailTab.CROP)

    @Test fun enhanceTab() = capture("detail_enhance", DetailTab.ENHANCE)

    @Test fun rotateTab() = capture("detail_rotate", DetailTab.ROTATE)
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.ukpratik.folio.core.model.ByteSize
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.ExportFormat
import io.github.ukpratik.folio.core.model.ExportResult
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.ui.theme.FolioTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Visual checks against Home — empty / recents (phone 390×844 dp). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class HomeScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun capture(name: String, state: HomeState, dark: Boolean = false) {
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                FolioTheme(darkTheme = dark) {
                    HomeScreen(state, SnackbarHostState(), {}, {}, {}, {}, {}, {}, {}, {})
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    private fun row(title: String, pages: Int, daysAgo: Long, size: Long? = null, interrupted: Boolean = false): RecentUi {
        val id = DocumentId(title)
        val at = LocalDate.now().minusDays(daysAgo).atTime(14, 30).atZone(ZoneId.systemDefault()).toInstant()
        return RecentUi(
            id = id,
            title = title,
            isDraft = size == null,
            exportInterrupted = interrupted,
            pageCount = pages,
            cover = Page(PageId("$title-p"), id, 0, "s", status = PageStatus.READY),
            updatedAt = at,
            export = size?.let { ExportResult(ExportFormat.PDF, listOf("/x.pdf"), ByteSize(it), pages, null, null, Instant.EPOCH) },
        )
    }

    private val recents = HomeState(
        loading = false,
        recents = listOf(
            row("Folio_20261006_1430", 6, 0, 438_000),
            row("Marksheet_Class12", 3, 0, interrupted = true),
            row("Rent_Receipt_Sept", 1, 1, 182_000),
            row("Degree_Certificate", 2, 5, 1_200_000),
            row("Folio_20260928_0915", 4, 9),
        ),
    )

    @Test fun homeRecents() = capture("home_recents", recents)

    @Test fun homeRecentsDark() = capture("home_recents_dark", recents, dark = true)

    @Test fun homeEmpty() = capture("home_empty", HomeState(loading = false))

    @Test @Config(qualifiers = "w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun homeRecentsAt200PercentFont() = capture("home_recents_font200", recents)
}

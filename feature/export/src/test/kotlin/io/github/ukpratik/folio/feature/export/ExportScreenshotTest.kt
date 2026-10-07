// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.export

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.ukpratik.folio.core.domain.engine.ExportPhase
import io.github.ukpratik.folio.core.model.ByteSize
import io.github.ukpratik.folio.core.model.ExportFormat
import io.github.ukpratik.folio.core.model.ExportResult
import io.github.ukpratik.folio.core.ui.theme.FolioTheme
import io.github.ukpratik.folio.feature.export.processing.ProcessingScreen
import io.github.ukpratik.folio.feature.export.processing.ProcessingState
import io.github.ukpratik.folio.feature.export.result.OutputFile
import io.github.ukpratik.folio.feature.export.result.ResultScreen
import io.github.ukpratik.folio.feature.export.result.ResultState
import io.github.ukpratik.folio.feature.export.sheet.ExportSheetContent
import io.github.ukpratik.folio.feature.export.sheet.ExportSheetState
import io.github.ukpratik.folio.feature.export.sheet.SizeChoice
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Visual checks against the design canvas boards S5–S7 (phone 390×844 dp). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class ExportScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun capture(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                FolioTheme(darkTheme = dark) { Surface(color = MaterialTheme.colorScheme.surface) { content() } }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    private val sheet = ExportSheetState(loaded = true, name = "Folio_20261006_1430")

    @Test fun sheetDefault() = capture("export_sheet") { ExportSheetContent(sheet) {} }

    @Test fun sheetCustomSizeDark() = capture("export_sheet_custom_dark", dark = true) {
        ExportSheetContent(sheet.copy(size = SizeChoice.Custom, customValue = "20", showCustomError = true)) {}
    }

    @Test fun sheetJpg() = capture("export_sheet_jpg") {
        ExportSheetContent(sheet.copy(format = ExportFormat.JPG, size = SizeChoice.Preset(ByteSize.kb(200)))) {}
    }

    @Test fun processing() = capture("processing") {
        ProcessingScreen(ProcessingState.Working(ExportFormat.PDF, ExportPhase.ENCODING, 2, 6, null, 0.5f)) {}
    }

    @Test fun processingShrinking() = capture("processing_shrinking") {
        ProcessingScreen(ProcessingState.Working(ExportFormat.PDF, ExportPhase.SHRINKING, 5, 6, ByteSize.kb(500), 0.9f)) {}
    }

    @Test fun lowStorage() = capture("error_low_storage") { ProcessingScreen(ProcessingState.LowStorage(ByteSize.mb(40))) {} }

    @Test fun exportFailed() = capture("error_export_failed") { ProcessingScreen(ProcessingState.Failed) {} }

    private fun pdf(size: Long, target: Long?, met: Boolean?) =
        ExportResult(ExportFormat.PDF, listOf("/x/Marksheet.pdf"), ByteSize(size), 3, target?.let(::ByteSize), met, Instant.EPOCH)

    private fun result(name: String, state: ResultState, dark: Boolean = false) = capture(name, dark) {
        ResultScreen(state, SnackbarHostState(), {}, {}, {}, {}, {}, {})
    }

    @Test fun resultSuccess() = result("result_success", ResultState(true, "Folio_20261006_1430", pdf(438_000, 500_000, true)))

    @Test fun resultSuccessDark() =
        result("result_success_dark", ResultState(true, "Folio_20261006_1430", pdf(438_000, 500_000, true)), dark = true)

    @Test fun resultTargetMissed() =
        result("result_target_missed", ResultState(true, "Marksheet_Class12", pdf(142_000, 100_000, false), showAlternatives = true))

    @Test fun resultOverLimit() = result("result_over_limit", ResultState(true, "Marksheet_Class12", pdf(142_000, 100_000, false)))

    @Test fun resultJpg() {
        val sizes = listOf(141_000L, 118_000L, 96_000L)
        val files = sizes.mapIndexed { i, s -> OutputFile("/x/$i.jpg", "Marksheet_Class12_0${i + 1}.jpg", ByteSize(s)) }
        val export = ExportResult(ExportFormat.JPG, files.map { it.path }, ByteSize(sizes.sum()), 3, ByteSize.kb(200), true, Instant.EPOCH)
        result("result_jpg", ResultState(true, "Marksheet_Class12", export, files))
    }

    @Test @Config(qualifiers = "w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun sheetAt200PercentFont() = capture("export_sheet_font200") { ExportSheetContent(sheet) {} }

    @Test @Config(qualifiers = "w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun resultMissedAt200PercentFont() =
        result("result_target_missed_font200", ResultState(true, "Marksheet_Class12", pdf(142_000, 100_000, false), showAlternatives = true))
}

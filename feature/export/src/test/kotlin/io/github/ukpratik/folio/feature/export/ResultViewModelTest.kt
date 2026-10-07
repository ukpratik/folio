// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.export

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.domain.usecase.ExportDocument
import io.github.ukpratik.folio.core.domain.usecase.ExportInBlackAndWhite
import io.github.ukpratik.folio.core.domain.usecase.RenameDocument
import io.github.ukpratik.folio.core.domain.usecase.SaveExport
import io.github.ukpratik.folio.core.model.ByteSize
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.ExportFormat
import io.github.ukpratik.folio.core.model.ExportResult
import io.github.ukpratik.folio.core.testing.FakeDocumentFiles
import io.github.ukpratik.folio.core.testing.FakeDocumentRepository
import io.github.ukpratik.folio.core.testing.FakeExportEngine
import io.github.ukpratik.folio.core.testing.FakePageRepository
import io.github.ukpratik.folio.core.testing.FakePreferencesRepository
import io.github.ukpratik.folio.core.testing.FakeSaveDestinations
import io.github.ukpratik.folio.core.testing.MainDispatcherRule
import io.github.ukpratik.folio.core.ui.text.UiText
import io.github.ukpratik.folio.feature.export.result.ResultEffect
import io.github.ukpratik.folio.feature.export.result.ResultIntent
import io.github.ukpratik.folio.feature.export.result.ResultState
import io.github.ukpratik.folio.feature.export.result.ResultViewModel
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ResultViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val documents = FakeDocumentRepository()
    private val pages = FakePageRepository()
    private val files = FakeDocumentFiles()
    private val engine = FakeExportEngine()
    private val destinations = FakeSaveDestinations()
    private val preferences = FakePreferencesRepository()
    private var id = DocumentId("")

    private suspend fun vm(targetMet: Boolean?, offerAlternatives: Boolean = false): ResultViewModel {
        id = documents.create("Marksheet").id
        val pdf = files.outputFile(id, "Marksheet.pdf").apply { parentFile!!.mkdirs(); writeBytes(ByteArray(142_000)) }
        documents.markExported(
            id,
            ExportResult(ExportFormat.PDF, listOf(pdf.path), ByteSize(142_000), 3, ByteSize.kb(100), targetMet, Instant.EPOCH),
        )
        return ResultViewModel(
            SavedStateHandle(mapOf("documentId" to id.value, "offerAlternatives" to offerAlternatives)),
            documents,
            preferences,
            RenameDocument(documents, files),
            SaveExport(documents, destinations, preferences),
            ExportInBlackAndWhite(documents, pages, ExportDocument(documents, engine)),
            UnconfinedTestDispatcher(),
        )
    }

    private suspend fun ResultViewModel.loaded(): ResultState = state.first { it.loaded }

    @Test fun showsTheExportAndItsFiles() = runTest {
        val state = vm(targetMet = true).loaded()
        assertThat(state.title).isEqualTo("Marksheet")
        assertThat(state.files.single().name).isEqualTo("Marksheet.pdf")
        assertThat(state.files.single().size).isEqualTo(ByteSize(142_000))
        assertThat(state.showAlternatives).isFalse()
    }

    @Test fun missedTargetOffersAlternativesUntilKept() = runTest {
        val vm = vm(targetMet = false, offerAlternatives = true)
        vm.state.test {
            assertThat(awaitLoaded().showAlternatives).isTrue()
            vm.onIntent(ResultIntent.Keep)
            val kept = awaitItem()
            assertThat(kept.showAlternatives).isFalse()
            assertThat(kept.overLimit).isTrue() // the warning badge, never a tick
        }
    }

    @Test fun reopeningFromRecentsShowsTheBadgeNotTheCard() = runTest {
        val state = vm(targetMet = false, offerAlternatives = false).loaded()
        assertThat(state.showAlternatives).isFalse()
        assertThat(state.overLimit).isTrue()
    }

    @Test fun savingShowsSaved() = runTest {
        val vm = vm(targetMet = true)
        vm.loaded()
        vm.effects.test {
            vm.onIntent(ResultIntent.SaveTo("content://doc/1"))
            assertThat(awaitItem()).isEqualTo(ResultEffect.ShowMessage(UiText.Res(R.string.saved)))
        }
        assertThat(destinations.copies).hasSize(1)
    }

    @Test fun lostDestinationAsksForAnotherFolder() = runTest {
        val vm = vm(targetMet = true)
        vm.loaded()
        destinations.unavailable = true
        vm.effects.test {
            vm.onIntent(ResultIntent.SaveTo("content://doc/1"))
            assertThat(awaitItem()).isEqualTo(ResultEffect.ShowMessage(UiText.Res(R.string.save_unavailable)))
        }
    }

    @Test fun tryBlackAndWhiteStartsANewExport() = runTest {
        val vm = vm(targetMet = false, offerAlternatives = true)
        vm.loaded()
        vm.effects.test {
            vm.onIntent(ResultIntent.TryBlackAndWhite)
            assertThat(awaitItem()).isEqualTo(ResultEffect.Reexporting)
        }
        assertThat(engine.started.single().first).isEqualTo(id)
    }

    @Test fun renameUpdatesTitleAndFile() = runTest {
        val vm = vm(targetMet = true)
        vm.loaded()
        vm.onIntent(ResultIntent.Rename("Class 12"))
        val state = vm.state.first { it.title == "Class 12" }
        assertThat(state.files.single().name).isEqualTo("Class 12.pdf")
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<ResultState>.awaitLoaded(): ResultState {
        var item = awaitItem()
        while (!item.loaded) item = awaitItem()
        return item
    }
}

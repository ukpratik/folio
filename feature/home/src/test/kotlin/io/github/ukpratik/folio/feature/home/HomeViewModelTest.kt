// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.domain.engine.ExportState
import io.github.ukpratik.folio.core.domain.usecase.AddPages
import io.github.ukpratik.folio.core.domain.usecase.DeleteDocument
import io.github.ukpratik.folio.core.domain.usecase.RenameDocument
import io.github.ukpratik.folio.core.domain.usecase.SaveExport
import io.github.ukpratik.folio.core.model.ByteSize
import io.github.ukpratik.folio.core.model.ExportFormat
import io.github.ukpratik.folio.core.model.ExportResult
import io.github.ukpratik.folio.core.model.ExportSettings
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.testing.FakeDocumentFiles
import io.github.ukpratik.folio.core.testing.FakeExportEngine
import io.github.ukpratik.folio.core.testing.FakePreferencesRepository
import io.github.ukpratik.folio.core.testing.FakeSaveDestinations
import java.time.Instant
import kotlinx.coroutines.flow.first
import io.github.ukpratik.folio.core.domain.usecase.CreateDocument
import io.github.ukpratik.folio.core.domain.usecase.StartDocumentFromImages
import io.github.ukpratik.folio.core.model.Limits
import io.github.ukpratik.folio.core.testing.FakeClock
import io.github.ukpratik.folio.core.testing.FakeDocumentRepository
import io.github.ukpratik.folio.core.testing.FakeImportEngine
import io.github.ukpratik.folio.core.testing.FakePageRepository
import io.github.ukpratik.folio.core.testing.MainDispatcherRule
import io.github.ukpratik.folio.core.ui.text.UiText
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val clock = FakeClock()
    private val documents = FakeDocumentRepository(clock)
    private val pages = FakePageRepository(clock)
    private val engine = FakeImportEngine()
    private val exports = FakeExportEngine()
    private val files = FakeDocumentFiles()
    private val destinations = FakeSaveDestinations()
    private val preferences = FakePreferencesRepository()
    // Lazy: built after MainDispatcherRule has installed the test Main dispatcher.
    private val vm by lazy {
        HomeViewModel(
        documents,
        pages,
        exports,
        preferences,
        StartDocumentFromImages(CreateDocument(documents, clock), AddPages(pages, documents, engine, clock)),
        RenameDocument(documents, files),
        DeleteDocument(documents, files, exports),
        SaveExport(documents, destinations, preferences),
        )
    }

    private suspend fun loaded(): HomeState = vm.state.first { !it.loading }

    @Test fun pickingImagesOpensANewDraft() = runTest {
        vm.effects.test {
            vm.onIntent(HomeIntent.ImagesPicked(listOf("content://a", "content://b")))
            val effect = awaitItem() as HomeEffect.OpenEditor
            assertThat(effect.documentId).isEqualTo(documents.all.single().id)
        }
        assertThat(engine.enqueued).hasSize(2)
    }

    @Test fun cancelledPickerDoesNothing() = runTest {
        vm.effects.test {
            vm.onIntent(HomeIntent.ImagesPicked(emptyList()))
            expectNoEvents()
        }
        assertThat(documents.all).isEmpty()
    }

    @Test fun overTheLimitWarnsThenOpens() = runTest {
        vm.effects.test {
            vm.onIntent(HomeIntent.ImagesPicked((1..Limits.MAX_PAGES + 3).map { "content://$it" }))
            assertThat(awaitItem()).isEqualTo(HomeEffect.ShowMessage(UiText.Res(R.string.import_page_limit, listOf(Limits.MAX_PAGES))))
            assertThat(awaitItem()).isInstanceOf(HomeEffect.OpenEditor::class.java)
        }
    }

    @Test fun recentsAreListedAsDrafts() = runTest {
        documents.create("Marksheet")
        vm.state.test {
            val loaded = expectMostRecentItem().takeIf { !it.loading } ?: awaitItem()
            assertThat(loaded.recents.single().title).isEqualTo("Marksheet")
            assertThat(loaded.recents.single().isDraft).isTrue()
        }
    }

    @Test fun rowsShowPageCountCoverAndExport() = runTest {
        val id = documents.create("Marksheet").id
        pages.insertAll((0 until 3).map { Page(PageId("p$it"), id, it, "s$it", status = PageStatus.READY) })
        val pdf = files.outputFile(id, "Marksheet.pdf").apply { parentFile!!.mkdirs(); writeText("pdf") }
        documents.markExported(id, ExportResult(ExportFormat.PDF, listOf(pdf.path), ByteSize(438_000), 3, null, null, Instant.EPOCH))

        val row = vm.state.first { s -> s.recents.any { it.export != null } }.recents.single()
        assertThat(row.isDraft).isFalse()
        assertThat(row.pageCount).isEqualTo(3)
        assertThat(row.cover?.id).isEqualTo(PageId("p0"))
        assertThat(row.export?.size).isEqualTo(ByteSize(438_000))
    }

    @Test fun anExportThatIsRunningIsNotShownAsUnfinished() = runTest {
        val id = documents.create("Marksheet").id
        documents.setExportRunning(id, true)
        exports.start(id, ExportSettings())
        assertThat(loaded().recents.single().exportInterrupted).isFalse()

        exports.set(id, ExportState.Cancelled)
        exports.acknowledge(id) // e.g. after a process restart nothing is running
        assertThat(vm.state.first { it.recents.singleOrNull()?.exportInterrupted == true }.recents.single().exportInterrupted).isTrue()
    }

    @Test fun deleteAndRenameFromTheMenu() = runTest {
        val a = documents.create("A").id
        val b = documents.create("B").id
        vm.onIntent(HomeIntent.Rename(a, "Receipt"))
        vm.onIntent(HomeIntent.Delete(b))
        assertThat(documents.all.map { it.title }).containsExactly("Receipt")
    }

    @Test fun savingFromRecentsShowsSavedToFolder() = runTest {
        val id = documents.create("Marksheet").id
        val jpg = files.outputFile(id, "Marksheet_01.jpg").apply { parentFile!!.mkdirs(); writeText("jpg") }
        documents.markExported(id, ExportResult(ExportFormat.JPG, listOf(jpg.path), ByteSize(3), 1, null, null, Instant.EPOCH))
        vm.effects.test {
            vm.onIntent(HomeIntent.SaveTo(id, "content://tree/docs"))
            assertThat(awaitItem()).isEqualTo(HomeEffect.ShowMessage(UiText.Res(R.string.home_saved_to, listOf("Downloads"))))
        }
    }
}

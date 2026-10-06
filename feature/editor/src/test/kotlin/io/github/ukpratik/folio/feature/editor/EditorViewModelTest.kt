// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.domain.engine.ImportProgress
import io.github.ukpratik.folio.core.domain.usecase.AddPages
import io.github.ukpratik.folio.core.domain.usecase.DeleteDocument
import io.github.ukpratik.folio.core.domain.usecase.DeletePage
import io.github.ukpratik.folio.core.domain.usecase.DuplicatePage
import io.github.ukpratik.folio.core.domain.usecase.MovePage
import io.github.ukpratik.folio.core.domain.usecase.RenameDocument
import io.github.ukpratik.folio.core.domain.usecase.RestorePage
import io.github.ukpratik.folio.core.domain.usecase.RotatePage
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.model.Rotation
import io.github.ukpratik.folio.core.testing.FakeClock
import io.github.ukpratik.folio.core.testing.FakeDocumentFiles
import io.github.ukpratik.folio.core.testing.FakeDocumentRepository
import io.github.ukpratik.folio.core.testing.FakeImportEngine
import io.github.ukpratik.folio.core.testing.FakePageRepository
import io.github.ukpratik.folio.core.testing.MainDispatcherRule
import io.github.ukpratik.folio.core.ui.text.UiText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class EditorViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val clock = FakeClock()
    private val documents = FakeDocumentRepository(clock)
    private val pages = FakePageRepository(clock)
    private val engine = FakeImportEngine()
    private val files = FakeDocumentFiles()

    private suspend fun viewModel(pageCount: Int = 0): EditorViewModel {
        val doc = documents.create("Marksheet")
        pages.insertAll((0 until pageCount).map { Page(PageId("p$it"), doc.id, it, "s$it", status = PageStatus.READY) })
        return EditorViewModel(
            SavedStateHandle(mapOf(EditorDestination.ARG_DOCUMENT_ID to doc.id.value)),
            documents, pages, engine,
            MovePage(pages), RotatePage(pages, documents), DuplicatePage(pages, documents, clock),
            DeletePage(pages), RestorePage(pages), RenameDocument(documents),
            AddPages(pages, documents, engine, clock), DeleteDocument(documents, files),
        )
    }

    /** Keeps the state flow subscribed so state.value reflects the repositories. */
    private suspend fun EditorViewModel.loaded(): EditorState = state.first { it.loaded }

    @Test fun createPdfIsDisabledWhileImporting() = runTest {
        val vm = viewModel(pageCount = 1)
        vm.state.test {
            engine.progress.value = mapOf(vm.documentId to ImportProgress(total = 2, done = 1))
            val importing = expectMostRecentItem()
            assertThat(importing.isImporting).isTrue()
            assertThat(importing.canCreatePdf).isFalse()
        }
    }

    @Test fun summarisesImportFailuresOnceThenAcknowledges() = runTest {
        val vm = viewModel()
        vm.effects.test {
            engine.progress.value = mapOf(vm.documentId to ImportProgress(total = 3, done = 1, failed = 2))
            assertThat(awaitItem()).isEqualTo(EditorEffect.ShowMessage(UiText.Plural(R.plurals.import_failed, 2)))
            assertThat(engine.progress.value).doesNotContainKey(vm.documentId)
        }
    }

    @Test fun deleteOffersUndoAndUndoRestores() = runTest {
        val vm = viewModel(pageCount = 3)
        vm.state.test {
            awaitItem()
            vm.effects.test {
                vm.onIntent(EditorIntent.Delete(PageId("p1")))
                assertThat(awaitItem()).isEqualTo(EditorEffect.ShowUndo(PageId("p1")))
            }
            assertThat(expectMostRecentItem().pages.map { it.id.value }).containsExactly("p0", "p2").inOrder()
            vm.onIntent(EditorIntent.UndoDelete(PageId("p1")))
            assertThat(expectMostRecentItem().pages.map { it.id.value }).containsExactly("p0", "p1", "p2").inOrder()
        }
    }

    @Test fun moveRotateAndDuplicate() = runTest {
        val vm = viewModel(pageCount = 3)
        vm.state.test {
            awaitItem()
            vm.onIntent(EditorIntent.Move(PageId("p2"), 0))
            assertThat(expectMostRecentItem().pages.map { it.id.value }).containsExactly("p2", "p0", "p1").inOrder()
            vm.onIntent(EditorIntent.Rotate(PageId("p0")))
            vm.onIntent(EditorIntent.Duplicate(PageId("p0")))
            val latest = expectMostRecentItem().pages
            assertThat(latest).hasSize(4)
            assertThat(latest[2].sourceId).isEqualTo("s0")
            assertThat(latest[2].rotation).isEqualTo(Rotation.R90)
        }
    }

    @Test fun blankRenameIsRejected() = runTest {
        val vm = viewModel(pageCount = 1)
        vm.effects.test {
            vm.onIntent(EditorIntent.Rename("   "))
            assertThat(awaitItem()).isEqualTo(EditorEffect.ShowMessage(UiText.Res(R.string.rename_empty)))
        }
        vm.onIntent(EditorIntent.Rename("Fees / Receipt"))
        assertThat(documents.all.single().title).isEqualTo("Fees _ Receipt")
    }

    @Test fun leavingAnEmptyDraftDeletesIt() = runTest {
        val vm = viewModel(pageCount = 0)
        vm.state.test {
            awaitItem()
            vm.effects.test {
                vm.onIntent(EditorIntent.Leave)
                assertThat(awaitItem()).isEqualTo(EditorEffect.Close(message = null))
            }
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(documents.all).isEmpty()
    }

    @Test fun leavingADraftWithPagesKeepsIt() = runTest {
        val vm = viewModel(pageCount = 2)
        vm.state.test {
            awaitItem()
            vm.effects.test {
                vm.onIntent(EditorIntent.Leave)
                assertThat(awaitItem()).isEqualTo(EditorEffect.Close(UiText.Res(R.string.editor_saved_draft)))
            }
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(documents.all).hasSize(1)
    }
}

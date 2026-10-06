// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.domain.engine.ImportProgress
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.testing.FakeDocumentRepository
import io.github.ukpratik.folio.core.testing.FakeImportEngine
import io.github.ukpratik.folio.core.testing.FakePageRepository
import io.github.ukpratik.folio.core.testing.MainDispatcherRule
import io.github.ukpratik.folio.core.ui.text.UiText
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class EditorViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val documents = FakeDocumentRepository()
    private val pages = FakePageRepository()
    private val engine = FakeImportEngine()

    private suspend fun viewModel(): EditorViewModel {
        val doc = documents.create("Marksheet")
        return EditorViewModel(SavedStateHandle(mapOf(EditorDestination.ARG_DOCUMENT_ID to doc.id.value)), documents, pages, engine)
    }

    @Test fun createPdfIsDisabledWhileImporting() = runTest {
        val vm = viewModel()
        pages.insertAll(listOf(Page(PageId("p"), vm.documentId, 0, "s", status = PageStatus.READY)))

        vm.state.test {
            skipItems(1)
            engine.progress.value = mapOf(vm.documentId to ImportProgress(total = 2, done = 1))
            val importing = expectMostRecentItem()
            assertThat(importing.title).isEqualTo("Marksheet")
            assertThat(importing.isImporting).isTrue()
            assertThat(importing.canCreatePdf).isFalse()
        }
    }

    @Test fun summarisesFailuresOnceThenAcknowledges() = runTest {
        val vm = viewModel()
        vm.effects.test {
            engine.progress.value = mapOf(vm.documentId to ImportProgress(total = 3, done = 1, failed = 2))
            val effect = awaitItem() as EditorEffect.ShowMessage
            assertThat(effect.text).isEqualTo(UiText.Plural(R.plurals.import_failed, 2))
            assertThat(engine.progress.value).doesNotContainKey(vm.documentId)
        }
    }
}

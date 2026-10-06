// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.domain.usecase.AddPages
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
    private val vm = HomeViewModel(
        documents,
        StartDocumentFromImages(CreateDocument(documents, clock), AddPages(pages, documents, engine, clock)),
    )

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
}

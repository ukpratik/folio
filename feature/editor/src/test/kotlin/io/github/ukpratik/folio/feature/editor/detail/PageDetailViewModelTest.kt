// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.domain.usecase.ApplyToAllPages
import io.github.ukpratik.folio.core.domain.usecase.ResetPage
import io.github.ukpratik.folio.core.domain.usecase.RotatePage
import io.github.ukpratik.folio.core.domain.usecase.SetEnhancement
import io.github.ukpratik.folio.core.domain.usecase.UpdateCorners
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.EnhancementMode
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.PointF01
import io.github.ukpratik.folio.core.model.Quad
import io.github.ukpratik.folio.core.model.Rotation
import io.github.ukpratik.folio.core.testing.FakeClock
import io.github.ukpratik.folio.core.testing.FakeDocumentRepository
import io.github.ukpratik.folio.core.testing.FakePageRepository
import io.github.ukpratik.folio.core.testing.MainDispatcherRule
import io.github.ukpratik.folio.core.ui.text.UiText
import io.github.ukpratik.folio.feature.editor.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class PageDetailViewModelTest {
    @get:Rule val main = MainDispatcherRule(StandardTestDispatcher())

    private val clock = FakeClock()
    private val pages = CountingPages(FakePageRepository(clock))
    private val documents = FakeDocumentRepository(clock)
    private val doc = DocumentId("d")
    private val auto = Quad(PointF01(0.1f, 0.1f), PointF01(0.9f, 0.1f), PointF01(0.9f, 0.9f), PointF01(0.1f, 0.9f))

    /** Counts enhancement writes so the debounce can be asserted. */
    private class CountingPages(val delegate: FakePageRepository) : io.github.ukpratik.folio.core.domain.repository.PageRepository by delegate {
        var enhancementWrites = 0
        override suspend fun setEnhancements(changes: Map<PageId, io.github.ukpratik.folio.core.model.Enhancement>) {
            enhancementWrites++
            delegate.setEnhancements(changes)
        }
    }

    private suspend fun viewModel(start: String = "p1", autoCorners: Quad? = auto): PageDetailViewModel {
        pages.insertAll((0 until 3).map { Page(PageId("p$it"), doc, it, "s$it", corners = autoCorners, autoCorners = autoCorners) })
        return PageDetailViewModel(
            SavedStateHandle(mapOf(PageDetailDestination.ARG_DOCUMENT_ID to doc.value, PageDetailDestination.ARG_PAGE_ID to start)),
            pages, UpdateCorners(pages), RotatePage(pages, documents), SetEnhancement(pages), ApplyToAllPages(pages), ResetPage(pages),
        )
    }

    private suspend fun PageDetailViewModel.ready() = state.first { it.loaded }

    @Test fun opensOnTheTappedPage() = runTest(main.dispatcher) {
        val vm = viewModel(start = "p1")
        vm.state.test {
            val loaded = expectMostRecentItem().takeIf { it.loaded } ?: awaitItem()
            assertThat(loaded.currentIndex).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun sliderChangesAreDebouncedIntoOneWrite() = runTest(main.dispatcher) {
        val vm = viewModel()
        vm.state.test {
            skipItems(1)
            runCurrent()
            repeat(10) { i -> vm.onIntent(PageDetailIntent.SetBrightness(i / 20f)) }
            runCurrent()
            val latest = expectMostRecentItem()
            assertThat(latest.enhancement!!.adjustments.brightness).isEqualTo(0.45f) // draft shown instantly
            advanceTimeBy(PageDetailViewModel.SLIDER_DEBOUNCE_MS + 10)
            runCurrent()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(pages.enhancementWrites).isEqualTo(1)
        assertThat(pages.delegate.current[1].adjustments.brightness).isEqualTo(0.45f)
    }

    @Test fun modeChangesSaveImmediately() = runTest(main.dispatcher) {
        val vm = viewModel()
        vm.state.test {
            skipItems(1); runCurrent()
            vm.onIntent(PageDetailIntent.SetMode(EnhancementMode.BW)); runCurrent()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(pages.delegate.current[1].mode).isEqualTo(EnhancementMode.BW)
    }

    @Test fun applyToAllThenUndo() = runTest(main.dispatcher) {
        val vm = viewModel()
        vm.state.test {
            skipItems(1); runCurrent()
            vm.onIntent(PageDetailIntent.SetMode(EnhancementMode.GRAYSCALE)); runCurrent()
            vm.effects.test {
                vm.onIntent(PageDetailIntent.ApplyToAll); runCurrent()
                assertThat(awaitItem()).isEqualTo(PageDetailEffect.ShowApplyAllUndo(3))
            }
            assertThat(pages.delegate.current.map { it.mode }.toSet()).containsExactly(EnhancementMode.GRAYSCALE)
            vm.onIntent(PageDetailIntent.UndoApplyToAll); runCurrent()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(pages.delegate.current.map { it.mode })
            .containsExactly(EnhancementMode.AUTO, EnhancementMode.GRAYSCALE, EnhancementMode.AUTO).inOrder()
    }

    @Test fun cropCommitAutoAndFullImage() = runTest(main.dispatcher) {
        val vm = viewModel()
        val custom = Quad(PointF01(0.2f, 0.2f), PointF01(0.8f, 0.2f), PointF01(0.8f, 0.8f), PointF01(0.2f, 0.8f))
        vm.state.test {
            skipItems(1); runCurrent()
            vm.onIntent(PageDetailIntent.CommitCorners(custom)); runCurrent()
            assertThat(pages.delegate.current[1].corners).isEqualTo(custom)
            vm.onIntent(PageDetailIntent.FullImage); runCurrent()
            assertThat(pages.delegate.current[1].corners).isNull()
            vm.onIntent(PageDetailIntent.AutoCrop); runCurrent()
            assertThat(pages.delegate.current[1].corners).isEqualTo(auto)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun autoCropWithoutADetectionExplains() = runTest(main.dispatcher) {
        val vm = viewModel(autoCorners = null)
        vm.state.test {
            skipItems(1); runCurrent()
            vm.effects.test {
                vm.onIntent(PageDetailIntent.AutoCrop); runCurrent()
                assertThat(awaitItem()).isEqualTo(PageDetailEffect.ShowMessage(UiText.Res(R.string.detail_no_document_found)))
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun rotateAndReset() = runTest(main.dispatcher) {
        val vm = viewModel()
        vm.state.test {
            skipItems(1); runCurrent()
            vm.onIntent(PageDetailIntent.Rotate(clockwise = false)); runCurrent()
            assertThat(pages.delegate.current[1].rotation).isEqualTo(Rotation.R270)
            vm.onIntent(PageDetailIntent.FullImage); runCurrent()
            vm.onIntent(PageDetailIntent.ResetPage); runCurrent()
            cancelAndIgnoreRemainingEvents()
        }
        val page = pages.delegate.current[1]
        assertThat(page.rotation).isEqualTo(Rotation.R0)
        assertThat(page.corners).isEqualTo(auto)
    }
}

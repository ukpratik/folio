// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain

import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.domain.usecase.ApplyToAllPages
import io.github.ukpratik.folio.core.domain.usecase.DeletePage
import io.github.ukpratik.folio.core.domain.usecase.DuplicatePage
import io.github.ukpratik.folio.core.domain.usecase.ResetPage
import io.github.ukpratik.folio.core.domain.usecase.RestorePage
import io.github.ukpratik.folio.core.domain.usecase.RotatePage
import io.github.ukpratik.folio.core.domain.usecase.SetEnhancement
import io.github.ukpratik.folio.core.domain.usecase.UpdateCorners
import io.github.ukpratik.folio.core.model.Adjustments
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.Enhancement
import io.github.ukpratik.folio.core.model.EnhancementMode
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.PointF01
import io.github.ukpratik.folio.core.model.Quad
import io.github.ukpratik.folio.core.model.Rotation
import io.github.ukpratik.folio.core.testing.FakeClock
import io.github.ukpratik.folio.core.testing.FakeDocumentRepository
import io.github.ukpratik.folio.core.testing.FakePageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PageEditsTest {
    private val clock = FakeClock()
    private val pages = FakePageRepository(clock)
    private val documents = FakeDocumentRepository(clock)

    private suspend fun seed(n: Int): List<Page> {
        val doc = documents.create("d")
        return (0 until n).map { Page(PageId("p$it"), doc.id, it, "s$it") }.also { pages.insertAll(it) }
    }

    @Test fun rotateCyclesAndBumpsEditVersion() = runTest {
        seed(1)
        val rotate = RotatePage(pages, documents)
        repeat(3) { rotate(PageId("p0")) }
        assertThat(pages.current.single().rotation).isEqualTo(Rotation.R270)
        assertThat(pages.current.single().editVersion).isEqualTo(3)
        rotate(PageId("p0"))
        assertThat(pages.current.single().rotation).isEqualTo(Rotation.R0)
    }

    @Test fun duplicateGoesRightAfterOriginalAndKeepsEdits() = runTest {
        val seeded = seed(3)
        pages.setRotation(PageId("p1"), Rotation.R90)
        val withEdit = pages.current.map { if (it.id.value == "p1") it.copy(mode = EnhancementMode.BW) else it }

        val copyId = DuplicatePage(pages, documents, clock)(withEdit, PageId("p1"))!!

        assertThat(pages.current.map { it.id }).containsExactly(PageId("p0"), PageId("p1"), copyId, PageId("p2")).inOrder()
        val copy = pages.current.first { it.id == copyId }
        assertThat(copy.sourceId).isEqualTo("s1")
        assertThat(copy.rotation).isEqualTo(Rotation.R90)
        assertThat(copy.mode).isEqualTo(EnhancementMode.BW)
        assertThat(seeded).hasSize(3)
    }

    @Test fun deleteThenUndoRestoresOrder() = runTest {
        seed(3)
        DeletePage(pages)(PageId("p1"))
        assertThat(pages.current.map { it.id.value }).containsExactly("p0", "p2").inOrder()
        RestorePage(pages)(PageId("p1"))
        assertThat(pages.current.map { it.id.value }).containsExactly("p0", "p1", "p2").inOrder()
    }
}

class PageDetailEditsTest {
    private val clock = FakeClock()
    private val pages = FakePageRepository(clock)
    private val auto = Quad(
        PointF01(0.1f, 0.1f), PointF01(0.9f, 0.1f),
        PointF01(0.9f, 0.9f), PointF01(0.1f, 0.9f),
    )

    private suspend fun seed(n: Int) = (0 until n).map {
        Page(PageId("p$it"), DocumentId("d"), it, "s$it", corners = auto, autoCorners = auto)
    }.also { pages.insertAll(it) }

    @Test fun applyToAllCopiesTheLookAndUndoRestoresEachPage() = runTest {
        seed(3)
        SetEnhancement(pages)(
            PageId("p1"), Enhancement(EnhancementMode.GRAYSCALE, Adjustments(0.2f, 0f)),
        )
        val bw = Enhancement(EnhancementMode.BW, Adjustments(0f, 0.3f))
        val applyAll = ApplyToAllPages(pages)

        val previous = applyAll(pages.current, bw)
        assertThat(pages.current.map { it.enhancement }.toSet()).containsExactly(bw)

        applyAll.undo(previous)
        assertThat(pages.current.map { it.mode }).containsExactly(EnhancementMode.AUTO, EnhancementMode.GRAYSCALE, EnhancementMode.AUTO).inOrder()
        assertThat(pages.current[1].adjustments.brightness).isEqualTo(0.2f)
    }

    @Test fun resetRestoresAutoCropAndClearsEdits() = runTest {
        seed(1)
        UpdateCorners(pages)(PageId("p0"), null)
        pages.setRotation(PageId("p0"), Rotation.R180)
        ResetPage(pages)(PageId("p0"))
        val page = pages.current.single()
        assertThat(page.corners).isEqualTo(auto)
        assertThat(page.rotation).isEqualTo(Rotation.R0)
        assertThat(page.mode).isEqualTo(EnhancementMode.AUTO)
    }

    @Test fun rotateBothWays() = runTest {
        seed(1)
        val docs = FakeDocumentRepository(clock)
        val rotate = RotatePage(pages, docs)
        rotate(PageId("p0"), clockwise = false)
        assertThat(pages.current.single().rotation).isEqualTo(Rotation.R270)
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.data.db.FolioDatabase
import io.github.ukpratik.folio.core.data.repo.RoomDocumentRepository
import io.github.ukpratik.folio.core.data.repo.RoomPageRepository
import io.github.ukpratik.folio.core.model.ExportSettings
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.testing.FakeClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Exercises the real SQL in PageDao against an in-memory database. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomPageRepositoryTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), FolioDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val clock = FakeClock(now = 100)
    private val documents = RoomDocumentRepository(db.documentDao(), clock)
    private val pages = RoomPageRepository(db.pageDao(), clock)

    @After fun close() = db.close()

    @Test fun orderingCountsAndCascade() = runTest {
        val doc = documents.create("Doc", ExportSettings())
        assertThat(pages.maxOrder(doc.id)).isEqualTo(-1)
        pages.insertAll((0..2).map { Page(PageId("p$it"), doc.id, it, "s$it", status = PageStatus.IMPORTING, createdAt = 50) })

        pages.reorder(doc.id, listOf(PageId("p2"), PageId("p0"), PageId("p1")))
        pages.softDelete(PageId("p1"))

        assertThat(pages.observePages(doc.id).first().map { it.id.value }).containsExactly("p2", "p0").inOrder()
        assertThat(pages.count(doc.id)).isEqualTo(2)
        assertThat(pages.maxOrder(doc.id)).isEqualTo(1)
        assertThat(pages.countSourceReferences(doc.id, "s1")).isEqualTo(1) // soft-deleted rows still count
        assertThat(pages.findSoftDeletedBefore(101).map { it.id.value }).containsExactly("p1")
        assertThat(pages.findWithStatusCreatedBefore(PageStatus.IMPORTING, 60).map { it.id.value }).containsExactly("p0", "p2")

        documents.delete(doc.id)
        assertThat(pages.get(PageId("p0"))).isNull()
    }

    @Test fun quadsAndStatusRoundTrip() = runTest {
        val doc = documents.create("Doc", ExportSettings())
        val page = Page(PageId("p"), doc.id, 0, "s", status = PageStatus.IMPORTING)
        pages.insertAll(listOf(page))
        pages.setStatus(page.id, PageStatus.READY)
        assertThat(pages.get(page.id)?.status).isEqualTo(PageStatus.READY)
    }

    @Test fun coversUseTheFirstLivePageAndLiveCount() = runTest {
        val a = documents.create("A", ExportSettings())
        val b = documents.create("B", ExportSettings())
        documents.create("Empty", ExportSettings())
        pages.insertAll((0..2).map { Page(PageId("a$it"), a.id, it, "s$it", status = PageStatus.READY) })
        pages.insertAll(listOf(Page(PageId("b0"), b.id, 0, "t", status = PageStatus.READY)))
        pages.softDelete(PageId("a0"))

        val covers = pages.observeCovers().first()

        assertThat(covers.keys).containsExactly(a.id, b.id)
        assertThat(covers.getValue(a.id).firstPage.id).isEqualTo(PageId("a1"))
        assertThat(covers.getValue(a.id).pageCount).isEqualTo(2)
        assertThat(covers.getValue(b.id).pageCount).isEqualTo(1)
    }
}

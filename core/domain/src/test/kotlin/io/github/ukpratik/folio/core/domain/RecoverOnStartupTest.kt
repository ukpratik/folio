// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain

import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.domain.time.SessionInfo
import io.github.ukpratik.folio.core.domain.usecase.RecoverOnStartup
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.testing.FakeClock
import io.github.ukpratik.folio.core.testing.FakeDocumentFiles
import io.github.ukpratik.folio.core.testing.FakePageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RecoverOnStartupTest {
    private val doc = DocumentId("doc")
    private val clock = FakeClock(now = 100)
    private val pages = FakePageRepository(clock)
    private val files = FakeDocumentFiles()
    private val sessionStart = 500L
    private val recover = RecoverOnStartup(pages, files, SessionInfo(sessionStart))

    private fun page(id: String, status: PageStatus, source: String = id, createdAt: Long = 100) =
        Page(PageId(id), doc, order = 0, sourceId = source, status = status, createdAt = createdAt)

    private fun writeSource(source: String) = files.sourceFile(doc, source).apply { parentFile.mkdirs(); writeText("jpeg") }

    @Test fun finishesInterruptedImportsWhoseSourceWasWritten() = runTest {
        writeSource("a")
        pages.insertAll(listOf(page("a", PageStatus.IMPORTING), page("b", PageStatus.IMPORTING)))

        val report = recover()

        assertThat(pages.current.map { it.id.value to it.status }).containsExactly("a" to PageStatus.READY)
        assertThat(report.pagesRecovered).isEqualTo(1)
        assertThat(report.pagesRemoved).isEqualTo(1)
        assertThat(files.workWipes).isEqualTo(1)
    }

    @Test fun leavesThisSessionsImportsAlone() = runTest {
        pages.insertAll(listOf(page("new", PageStatus.IMPORTING, createdAt = sessionStart + 1)))
        recover()
        assertThat(pages.current.single().status).isEqualTo(PageStatus.IMPORTING)
    }

    @Test fun purgesOldSoftDeletesButKeepsSharedSources() = runTest {
        val shared = writeSource("shared")
        pages.insertAll(listOf(page("orig", PageStatus.READY, "shared"), page("dup", PageStatus.READY, "shared")))
        val lonely = writeSource("lonely")
        pages.insertAll(listOf(page("gone", PageStatus.READY, "lonely")))
        pages.softDelete(PageId("dup"))
        pages.softDelete(PageId("gone"))

        val report = recover()

        assertThat(pages.allIds.map { it.value }).containsExactly("orig")
        assertThat(shared.exists()).isTrue()
        assertThat(lonely.exists()).isFalse()
        assertThat(report.sourcesDeleted).isEqualTo(1)
    }

    @Test fun keepsUndoableDeletesFromThisSession() = runTest {
        pages.insertAll(listOf(page("p", PageStatus.READY)))
        clock.now = sessionStart + 10
        pages.softDelete(PageId("p"))
        recover()
        assertThat(pages.allIds.map { it.value }).containsExactly("p")
    }
}

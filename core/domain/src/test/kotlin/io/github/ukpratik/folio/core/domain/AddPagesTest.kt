// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain

import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.domain.engine.ImportSource
import io.github.ukpratik.folio.core.domain.error.FolioError
import io.github.ukpratik.folio.core.domain.error.Outcome
import io.github.ukpratik.folio.core.domain.usecase.AddPages
import io.github.ukpratik.folio.core.domain.usecase.AddPagesResult
import io.github.ukpratik.folio.core.domain.usecase.CreateDocument
import io.github.ukpratik.folio.core.domain.usecase.StartDocumentFromImages
import io.github.ukpratik.folio.core.model.Limits
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.testing.FakeClock
import io.github.ukpratik.folio.core.testing.FakeDocumentRepository
import io.github.ukpratik.folio.core.testing.FakeImportEngine
import io.github.ukpratik.folio.core.testing.FakePageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AddPagesTest {
    private val clock = FakeClock()
    private val pages = FakePageRepository(clock)
    private val documents = FakeDocumentRepository(clock)
    private val engine = FakeImportEngine()
    private val addPages = AddPages(pages, documents, engine, clock)
    private fun sources(n: Int) = (1..n).map { ImportSource("content://img/$it") }

    @Test fun appendsPagesInOrderAndEnqueuesImports() = runTest {
        val doc = documents.create("d")
        addPages(doc.id, sources(2))
        val result = addPages(doc.id, sources(3))

        assertThat(result).isEqualTo(Outcome.Success(AddPagesResult(added = 3, skippedOverLimit = 0)))
        assertThat(pages.current.map { it.order }).containsExactly(0, 1, 2, 3, 4).inOrder()
        assertThat(pages.current.map { it.status }.toSet()).containsExactly(PageStatus.IMPORTING)
        assertThat(engine.enqueued.map { it.pageId }).containsExactlyElementsIn(pages.current.map { it.id }).inOrder()
        assertThat(engine.enqueued.last().source.uri).isEqualTo("content://img/3")
    }

    @Test fun capsAtMaxPagesAndReportsSkipped() = runTest {
        val doc = documents.create("d")
        val result = addPages(doc.id, sources(Limits.MAX_PAGES + 5))
        assertThat(result).isEqualTo(Outcome.Success(AddPagesResult(added = Limits.MAX_PAGES, skippedOverLimit = 5)))
        assertThat(addPages(doc.id, sources(1))).isEqualTo(Outcome.Failure(FolioError.TooManyPages))
    }

    @Test fun startFromImagesCreatesDraftNamedByTimestamp() = runTest {
        val start = StartDocumentFromImages(CreateDocument(documents, clock), addPages)
        val outcome = start(sources(2))
        assertThat(outcome).isInstanceOf(Outcome.Success::class.java)
        assertThat(documents.all.single().title).startsWith("Folio_")
        assertThat(start(emptyList())).isNull()
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain

import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.domain.usecase.MovePage
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.testing.FakePageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MovePageTest {
    private val doc = DocumentId("doc")
    private val repo = FakePageRepository()

    private suspend fun seed(n: Int) = (0 until n).map { i ->
        Page(id = PageId("p$i"), documentId = doc, order = i, sourceId = "s$i").also { repo.insertAll(listOf(it)) }
    }

    @Test fun movesPageAndKeepsOrderDense() = runTest {
        val pages = seed(5)
        MovePage(repo)(doc, pages, PageId("p4"), toIndex = 1)
        assertThat(repo.current.map { it.id.value }).containsExactly("p0", "p4", "p1", "p2", "p3").inOrder()
        assertThat(repo.current.map { it.order }).containsExactly(0, 1, 2, 3, 4).inOrder()
    }

    @Test fun clampsTargetIndex() = runTest {
        val pages = seed(3)
        MovePage(repo)(doc, pages, PageId("p0"), toIndex = 99)
        assertThat(repo.current.map { it.id.value }).containsExactly("p1", "p2", "p0").inOrder()
    }
}

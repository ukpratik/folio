// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.testing

import io.github.ukpratik.folio.core.domain.repository.PageRepository
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [PageRepository] for tests. Prefer fakes over mocks (ADR-0018). */
class FakePageRepository : PageRepository {
    private val pages = MutableStateFlow<Map<PageId, Page>>(emptyMap())
    private val deleted = mutableSetOf<PageId>()

    val current: List<Page> get() = pages.value.values.filterNot { it.id in deleted }.sortedBy { it.order }

    override fun observePages(documentId: DocumentId): Flow<List<Page>> =
        pages.map { all -> all.values.filter { it.documentId == documentId && it.id !in deleted }.sortedBy { it.order } }

    override suspend fun upsert(page: Page) {
        pages.value = pages.value + (page.id to page)
    }

    override suspend fun reorder(documentId: DocumentId, orderedIds: List<PageId>) {
        pages.value = pages.value.mapValues { (id, p) ->
            val i = orderedIds.indexOf(id)
            if (i >= 0) p.copy(order = i) else p
        }
    }

    override suspend fun softDelete(id: PageId) { deleted += id }
    override suspend fun restore(id: PageId) { deleted -= id }
}

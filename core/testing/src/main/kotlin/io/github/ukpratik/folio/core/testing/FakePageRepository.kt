// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.testing

import io.github.ukpratik.folio.core.domain.repository.PageRepository
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.PageStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [PageRepository]. Prefer fakes over mocks (ADR-0018). */
class FakePageRepository(private val clock: FakeClock = FakeClock()) : PageRepository {
    private data class Row(val page: Page, val deletedAt: Long? = null)

    private val rows = MutableStateFlow<Map<PageId, Row>>(emptyMap())

    /** Live pages, ordered. */
    val current: List<Page> get() = live().sortedBy { it.order }

    /** Every row including soft-deleted ones. */
    val allIds: Set<PageId> get() = rows.value.keys

    private fun live() = rows.value.values.filter { it.deletedAt == null }.map { it.page }

    private fun edit(id: PageId, change: (Row) -> Row) {
        rows.value[id]?.let { rows.value = rows.value + (id to change(it)) }
    }

    override fun observePages(documentId: DocumentId): Flow<List<Page>> =
        rows.map { all -> all.values.filter { it.deletedAt == null && it.page.documentId == documentId }.map { it.page }.sortedBy { it.order } }

    override suspend fun get(id: PageId): Page? = rows.value[id]?.page

    override suspend fun count(documentId: DocumentId): Int = live().count { it.documentId == documentId }

    override suspend fun maxOrder(documentId: DocumentId): Int =
        live().filter { it.documentId == documentId }.maxOfOrNull { it.order } ?: -1

    override suspend fun insertAll(pages: List<Page>) {
        rows.value = rows.value + pages.associate { it.id to Row(it) }
    }

    override suspend fun upsert(page: Page) {
        rows.value = rows.value + (page.id to Row(page, rows.value[page.id]?.deletedAt))
    }

    override suspend fun setStatus(id: PageId, status: PageStatus) = edit(id) { it.copy(page = it.page.copy(status = status)) }

    override suspend fun reorder(documentId: DocumentId, orderedIds: List<PageId>) {
        orderedIds.forEachIndexed { index, id -> edit(id) { it.copy(page = it.page.copy(order = index)) } }
    }

    override suspend fun softDelete(id: PageId) = edit(id) { it.copy(deletedAt = clock.nowMillis()) }

    override suspend fun restore(id: PageId) = edit(id) { it.copy(deletedAt = null) }

    override suspend fun deleteHard(ids: List<PageId>) {
        rows.value = rows.value - ids.toSet()
    }

    override suspend fun countSourceReferences(documentId: DocumentId, sourceId: String): Int =
        rows.value.values.count { it.page.documentId == documentId && it.page.sourceId == sourceId }

    override suspend fun findWithStatusCreatedBefore(status: PageStatus, before: Long): List<Page> =
        live().filter { it.status == status && it.createdAt < before }

    override suspend fun findSoftDeletedBefore(before: Long): List<Page> =
        rows.value.values.filter { it.deletedAt != null && it.deletedAt < before }.map { it.page }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.usecase

import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.domain.repository.PageRepository
import io.github.ukpratik.folio.core.domain.time.Clock
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import javax.inject.Inject

/** FR-08: rotates clockwise in 90° steps. */
class RotatePage @Inject constructor(private val pages: PageRepository, private val documents: DocumentRepository) {
    suspend operator fun invoke(id: PageId) {
        val page = pages.get(id) ?: return
        pages.setRotation(id, page.rotation.clockwise())
        documents.touch(page.documentId)
    }
}

/** FR-10: inserts a copy (sharing the same source file) directly after the original, with all its edits. */
class DuplicatePage @Inject constructor(
    private val pages: PageRepository,
    private val documents: DocumentRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(current: List<Page>, id: PageId): PageId? {
        val original = current.firstOrNull { it.id == id } ?: return null
        val copy = original.copy(id = PageId.new(), createdAt = clock.nowMillis(), editVersion = 0)
        val ordered = current.sortedBy { it.order }.map { it.id }.toMutableList()
        ordered.add(ordered.indexOf(id) + 1, copy.id)
        pages.insertAndReorder(copy, ordered)
        documents.touch(original.documentId)
        return copy.id
    }
}

/** FR-09: soft delete so the Undo snackbar can restore it. Startup recovery purges it later. */
class DeletePage @Inject constructor(private val pages: PageRepository) {
    suspend operator fun invoke(id: PageId) = pages.softDelete(id)
}

class RestorePage @Inject constructor(private val pages: PageRepository) {
    suspend operator fun invoke(id: PageId) = pages.restore(id)
}

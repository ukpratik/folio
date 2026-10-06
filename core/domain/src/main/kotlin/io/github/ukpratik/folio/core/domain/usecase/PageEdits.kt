// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.usecase

import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.domain.repository.PageRepository
import io.github.ukpratik.folio.core.domain.time.Clock
import io.github.ukpratik.folio.core.model.Enhancement
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.Quad
import javax.inject.Inject

/** FR-08: rotates in 90° steps (clockwise by default). */
class RotatePage @Inject constructor(private val pages: PageRepository, private val documents: DocumentRepository) {
    suspend operator fun invoke(id: PageId, clockwise: Boolean = true) {
        val page = pages.get(id) ?: return
        pages.setRotation(id, if (clockwise) page.rotation.clockwise() else page.rotation.counterClockwise())
        documents.touch(page.documentId)
    }
}

/** FR-04: sets the crop corners; null keeps the full image. */
class UpdateCorners @Inject constructor(private val pages: PageRepository) {
    suspend operator fun invoke(id: PageId, corners: Quad?) = pages.setCorners(id, corners)
}

/** FR-12/13 for one page. */
class SetEnhancement @Inject constructor(private val pages: PageRepository) {
    suspend operator fun invoke(id: PageId, enhancement: Enhancement) = pages.setEnhancements(mapOf(id to enhancement))
}

/**
 * FR-12 "Apply to all pages": copies one look to every page in one transaction.
 * Returns the previous looks so Undo can restore each page exactly.
 */
class ApplyToAllPages @Inject constructor(private val pages: PageRepository) {
    suspend operator fun invoke(current: List<Page>, enhancement: Enhancement): Map<PageId, Enhancement> {
        val previous = current.associate { it.id to it.enhancement }
        pages.setEnhancements(current.associate { it.id to enhancement })
        return previous
    }

    suspend fun undo(previous: Map<PageId, Enhancement>) = pages.setEnhancements(previous)
}

/** FR-11. */
class ResetPage @Inject constructor(private val pages: PageRepository) {
    suspend operator fun invoke(id: PageId) = pages.reset(id)
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

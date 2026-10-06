// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.usecase

import io.github.ukpratik.folio.core.domain.repository.PageRepository
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import javax.inject.Inject

/** FR-07: moves a page and rewrites the dense order in one transaction. */
class MovePage @Inject constructor(private val pages: PageRepository) {
    suspend operator fun invoke(documentId: DocumentId, current: List<Page>, pageId: PageId, toIndex: Int) {
        val ids = current.sortedBy { it.order }.map { it.id }.toMutableList()
        val from = ids.indexOf(pageId)
        if (from < 0) return
        ids.removeAt(from)
        ids.add(toIndex.coerceIn(0, ids.size), pageId)
        pages.reorder(documentId, ids)
    }
}

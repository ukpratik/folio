// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.usecase

import io.github.ukpratik.folio.core.domain.engine.ImportEngine
import io.github.ukpratik.folio.core.domain.engine.ImportJob
import io.github.ukpratik.folio.core.domain.engine.ImportSource
import io.github.ukpratik.folio.core.domain.error.FolioError
import io.github.ukpratik.folio.core.domain.error.Outcome
import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.domain.repository.PageRepository
import io.github.ukpratik.folio.core.domain.time.Clock
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.Limits
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.PageStatus
import java.util.UUID
import javax.inject.Inject

data class AddPagesResult(val added: Int, val skippedOverLimit: Int)

/**
 * FR-01/02/05/06: appends pages in the given order and hands them to the [ImportEngine].
 * Rows are created first (status IMPORTING) so the editor shows placeholders immediately.
 */
class AddPages @Inject constructor(
    private val pages: PageRepository,
    private val documents: DocumentRepository,
    private val importEngine: ImportEngine,
    private val clock: Clock,
) {
    suspend operator fun invoke(documentId: DocumentId, sources: List<ImportSource>): Outcome<AddPagesResult> {
        if (sources.isEmpty()) return Outcome.Success(AddPagesResult(added = 0, skippedOverLimit = 0))
        val room = Limits.MAX_PAGES - pages.count(documentId)
        if (room <= 0) return Outcome.Failure(FolioError.TooManyPages)

        val accepted = sources.take(room)
        val firstOrder = pages.maxOrder(documentId) + 1
        val now = clock.nowMillis()
        val newPages = accepted.mapIndexed { i, _ ->
            Page(
                id = PageId.new(),
                documentId = documentId,
                order = firstOrder + i,
                sourceId = UUID.randomUUID().toString(),
                status = PageStatus.IMPORTING,
                createdAt = now,
            )
        }
        pages.insertAll(newPages)
        importEngine.enqueue(newPages.zip(accepted) { page, source -> ImportJob(documentId, page.id, page.sourceId, source) })
        documents.touch(documentId)
        return Outcome.Success(AddPagesResult(added = accepted.size, skippedOverLimit = sources.size - accepted.size))
    }
}

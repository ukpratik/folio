// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.usecase

import io.github.ukpratik.folio.core.domain.files.DocumentFiles
import io.github.ukpratik.folio.core.domain.repository.PageRepository
import io.github.ukpratik.folio.core.domain.time.SessionInfo
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageStatus
import javax.inject.Inject

data class RecoveryReport(val pagesRecovered: Int, val pagesRemoved: Int, val sourcesDeleted: Int)

/**
 * LLD §3.4. Cleans up after process death. Only touches data created before this session started,
 * so it can't race with imports or Undo snackbars that began after launch.
 */
class RecoverOnStartup @Inject constructor(
    private val pages: PageRepository,
    private val files: DocumentFiles,
    private val session: SessionInfo,
) {
    suspend operator fun invoke(): RecoveryReport {
        files.wipeWorkDir()
        val before = session.startedAtMillis

        var recovered = 0
        val broken = mutableListOf<Page>()
        for (page in pages.findWithStatusCreatedBefore(PageStatus.IMPORTING, before)) {
            val source = files.sourceFile(page.documentId, page.sourceId)
            if (source.isFile && source.length() > 0) {
                pages.setStatus(page.id, PageStatus.READY)
                recovered++
            } else {
                broken += page
            }
        }
        broken += pages.findWithStatusCreatedBefore(PageStatus.FAILED, before)
        val removed = broken + pages.findSoftDeletedBefore(before)
        pages.deleteHard(removed.map { it.id })

        var sourcesDeleted = 0
        for (page in removed.distinctBy { it.documentId to it.sourceId }) {
            if (pages.countSourceReferences(page.documentId, page.sourceId) == 0) {
                files.deleteSource(page.documentId, page.sourceId)
                sourcesDeleted++
            }
        }
        return RecoveryReport(recovered, removed.size, sourcesDeleted)
    }
}

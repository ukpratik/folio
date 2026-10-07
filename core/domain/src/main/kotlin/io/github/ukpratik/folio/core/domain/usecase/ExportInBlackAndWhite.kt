// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.usecase

import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.domain.repository.PageRepository
import io.github.ukpratik.folio.core.model.Adjustments
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.Enhancement
import io.github.ukpratik.folio.core.model.EnhancementMode
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** FR-18 "Try black & white": B&W text compresses far smaller. Sets every page to B&W and exports again. */
class ExportInBlackAndWhite @Inject constructor(
    private val documents: DocumentRepository,
    private val pages: PageRepository,
    private val exportDocument: ExportDocument,
) {
    suspend operator fun invoke(id: DocumentId) {
        val document = documents.get(id) ?: return
        val bw = Enhancement(EnhancementMode.BW, Adjustments())
        pages.setEnhancements(pages.observePages(id).first().associate { it.id to bw })
        exportDocument(id, document.exportSettings)
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.usecase

import io.github.ukpratik.folio.core.domain.engine.ImportSource
import io.github.ukpratik.folio.core.domain.error.Outcome
import io.github.ukpratik.folio.core.model.DocumentId
import javax.inject.Inject

data class StartedDocument(val documentId: DocumentId, val result: AddPagesResult)

/** One entry point for "new document from these images", shared by the Photo Picker and share-in (DRY). */
class StartDocumentFromImages @Inject constructor(
    private val createDocument: CreateDocument,
    private val addPages: AddPages,
) {
    suspend operator fun invoke(sources: List<ImportSource>): Outcome<StartedDocument>? {
        if (sources.isEmpty()) return null
        val document = createDocument()
        return when (val outcome = addPages(document.id, sources)) {
            is Outcome.Success -> Outcome.Success(StartedDocument(document.id, outcome.value))
            is Outcome.Failure -> outcome
        }
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.usecase

import io.github.ukpratik.folio.core.domain.error.FolioError
import io.github.ukpratik.folio.core.domain.error.Outcome
import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.DocumentTitle
import javax.inject.Inject

/** FR-23: sanitises the name; rejects blank names. */
class RenameDocument @Inject constructor(private val documents: DocumentRepository) {
    suspend operator fun invoke(id: DocumentId, input: String): Outcome<String> {
        val title = DocumentTitle.sanitize(input) ?: return Outcome.Failure(FolioError.InvalidTitle)
        documents.rename(id, title)
        return Outcome.Success(title)
    }
}

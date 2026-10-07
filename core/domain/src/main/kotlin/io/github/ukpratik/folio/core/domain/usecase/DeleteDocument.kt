// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.usecase

import io.github.ukpratik.folio.core.domain.engine.ExportEngine
import io.github.ukpratik.folio.core.domain.files.DocumentFiles
import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.model.DocumentId
import javax.inject.Inject

/** FR-25: removes Folio's copy only. Files the user saved elsewhere are untouched. */
class DeleteDocument @Inject constructor(
    private val documents: DocumentRepository,
    private val files: DocumentFiles,
    private val exports: ExportEngine,
) {
    suspend operator fun invoke(id: DocumentId) {
        exports.cancel(id) // a running export would otherwise write into the deleted folder
        documents.delete(id) // rows first (cascade), so the UI updates immediately
        files.deleteDocument(id)
    }
}

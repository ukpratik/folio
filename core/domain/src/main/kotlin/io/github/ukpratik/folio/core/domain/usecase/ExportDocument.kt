// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.usecase

import io.github.ukpratik.folio.core.domain.engine.ExportEngine
import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.ExportSettings
import javax.inject.Inject

/** FR-19: remembers the chosen settings for this document, then starts the export. */
class ExportDocument @Inject constructor(
    private val documents: DocumentRepository,
    private val engine: ExportEngine,
) {
    suspend operator fun invoke(id: DocumentId, settings: ExportSettings) {
        documents.saveExportSettings(id, settings)
        engine.start(id, settings)
    }
}

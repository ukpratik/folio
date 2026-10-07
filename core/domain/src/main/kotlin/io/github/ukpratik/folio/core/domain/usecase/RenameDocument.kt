// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.usecase

import io.github.ukpratik.folio.core.domain.error.FolioError
import io.github.ukpratik.folio.core.domain.error.Outcome
import io.github.ukpratik.folio.core.domain.files.DocumentFiles
import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.DocumentTitle
import io.github.ukpratik.folio.core.model.ExportFileNames
import java.io.File
import javax.inject.Inject

/**
 * FR-23: sanitises the name; rejects blank names. An existing export is renamed too, so what the user
 * shares or saves next carries the new name (upload portals show the file name).
 */
class RenameDocument @Inject constructor(
    private val documents: DocumentRepository,
    private val files: DocumentFiles,
) {
    suspend operator fun invoke(id: DocumentId, input: String): Outcome<String> {
        val title = DocumentTitle.sanitize(input) ?: return Outcome.Failure(FolioError.InvalidTitle)
        documents.rename(id, title)
        documents.get(id)?.lastExport?.let { export ->
            val names = ExportFileNames.of(export.format, title, export.paths.size)
            val renamed = export.paths.zip(names).map { (path, name) -> files.rename(File(path), name).path }
            if (renamed != export.paths) documents.markExported(id, export.copy(paths = renamed))
        }
        return Outcome.Success(title)
    }
}

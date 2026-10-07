// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.usecase

import io.github.ukpratik.folio.core.domain.error.FolioError
import io.github.ukpratik.folio.core.domain.error.Outcome
import io.github.ukpratik.folio.core.domain.files.SaveDestinations
import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.domain.repository.PreferencesRepository
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.ExportFileNames
import io.github.ukpratik.folio.core.model.ExportFormat
import java.io.File
import javax.inject.Inject

/**
 * FR-22 "Save to device". A PDF goes into the document the user created with the system "Save to…" picker;
 * JPGs go into the folder they picked, which is remembered as the starting point next time.
 * Returns the folder name when known (the "Saved" snackbar shows it).
 */
class SaveExport @Inject constructor(
    private val documents: DocumentRepository,
    private val destinations: SaveDestinations,
    private val preferences: PreferencesRepository,
) {
    suspend operator fun invoke(id: DocumentId, destinationUri: String): Outcome<String?> {
        val export = documents.get(id)?.lastExport ?: return Outcome.Failure(FolioError.NothingToExport)
        val files = export.paths.map(::File)
        if (files.any { !it.exists() }) return Outcome.Failure(FolioError.NothingToExport)
        return when (export.format) {
            ExportFormat.PDF -> when (val outcome = destinations.copyToDocument(files.single(), destinationUri)) {
                is Outcome.Success -> Outcome.Success(null)
                is Outcome.Failure -> outcome
            }
            ExportFormat.JPG -> destinations.copyToFolder(files, destinationUri, ExportFileNames.mimeType(export.format)).also {
                if (it is Outcome.Success) preferences.update { prefs -> prefs.copy(lastSaveFolderUri = destinationUri) }
            }
        }
    }
}

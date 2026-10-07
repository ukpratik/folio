// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.testing

import io.github.ukpratik.folio.core.domain.error.FolioError
import io.github.ukpratik.folio.core.domain.error.Outcome
import io.github.ukpratik.folio.core.domain.files.SaveDestinations
import java.io.File

/** Records what would be copied where; [unavailable] simulates a removed SD card. */
class FakeSaveDestinations(var folderName: String? = "Downloads") : SaveDestinations {
    val copies = mutableListOf<Pair<String, List<String>>>()
    var unavailable = false

    override suspend fun copyToDocument(source: File, documentUri: String): Outcome<Unit> {
        if (unavailable) return Outcome.Failure(FolioError.SaveTargetUnavailable)
        copies += documentUri to listOf(source.name)
        return Outcome.Success(Unit)
    }

    override suspend fun copyToFolder(sources: List<File>, folderUri: String, mimeType: String): Outcome<String?> {
        if (unavailable) return Outcome.Failure(FolioError.SaveTargetUnavailable)
        copies += folderUri to sources.map { it.name }
        return Outcome.Success(folderName)
    }
}

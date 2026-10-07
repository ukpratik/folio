// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.files

import io.github.ukpratik.folio.core.domain.error.Outcome
import java.io.File

/**
 * Places the user picked through the system pickers (FR-22, ADR-0014). URIs are opaque strings here;
 * the Android implementation lives in :core:data.
 */
interface SaveDestinations {
    /** Copies [source] into a document the user just created with "Save to…". */
    suspend fun copyToDocument(source: File, documentUri: String): Outcome<Unit>

    /** Creates one file per source in a folder the user picked. Returns the folder's name when the provider exposes it. */
    suspend fun copyToFolder(sources: List<File>, folderUri: String, mimeType: String): Outcome<String?>
}

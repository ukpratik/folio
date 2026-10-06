// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.files

import io.github.ukpratik.folio.core.model.DocumentId
import java.io.File
import java.io.OutputStream

/** App-private file layout (HLD §7). Implemented in :core:data; used by processing and use cases (DIP). */
interface DocumentFiles {
    fun sourceFile(doc: DocumentId, sourceId: String): File
    fun outputFile(doc: DocumentId, fileName: String): File
    fun newWorkFile(prefix: String, extension: String): File

    /** Writes to a temp file, fsyncs, then renames over [target]. A failure never leaves a partial [target]. */
    suspend fun <T> writeAtomically(target: File, block: suspend (OutputStream) -> T): T

    suspend fun deleteSource(doc: DocumentId, sourceId: String)
    suspend fun deleteDocument(doc: DocumentId)
    suspend fun wipeWorkDir()
    fun freeBytes(): Long
}

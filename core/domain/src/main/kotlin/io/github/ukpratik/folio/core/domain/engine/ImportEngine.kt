// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.engine

import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.PageId
import kotlinx.coroutines.flow.StateFlow

/** Where an image comes from. [uri] is opaque to the domain (a content:// URI on Android). */
data class ImportSource(val uri: String)

/** One page to fill: the page row already exists with status IMPORTING. */
data class ImportJob(val documentId: DocumentId, val pageId: PageId, val sourceId: String, val source: ImportSource)

data class ImportProgress(val total: Int, val done: Int = 0, val failed: Int = 0) {
    val finished: Int get() = done + failed
    val isRunning: Boolean get() = finished < total
}

/**
 * Copies, normalises and analyses imported images in the background (LLD §6.3).
 * Work survives navigation; it does not survive process death (startup recovery cleans up).
 */
interface ImportEngine {
    val progress: StateFlow<Map<DocumentId, ImportProgress>>

    fun enqueue(jobs: List<ImportJob>)

    /** Clears a finished entry once the UI has shown its summary. */
    fun acknowledge(documentId: DocumentId)
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.testing

import io.github.ukpratik.folio.core.domain.engine.ImportEngine
import io.github.ukpratik.folio.core.domain.engine.ImportJob
import io.github.ukpratik.folio.core.domain.engine.ImportProgress
import io.github.ukpratik.folio.core.model.DocumentId
import kotlinx.coroutines.flow.MutableStateFlow

class FakeImportEngine : ImportEngine {
    val enqueued = mutableListOf<ImportJob>()
    override val progress = MutableStateFlow<Map<DocumentId, ImportProgress>>(emptyMap())

    override fun enqueue(jobs: List<ImportJob>) {
        enqueued += jobs
    }

    override fun acknowledge(documentId: DocumentId) {
        progress.value = progress.value - documentId
    }
}

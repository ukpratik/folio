// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.testing

import io.github.ukpratik.folio.core.domain.engine.ExportEngine
import io.github.ukpratik.folio.core.domain.engine.ExportPhase
import io.github.ukpratik.folio.core.domain.engine.ExportState
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.ExportSettings
import kotlinx.coroutines.flow.MutableStateFlow

/** Records calls; tests drive [states] directly. Starting shows Running like the real engine. */
class FakeExportEngine : ExportEngine {
    val started = mutableListOf<Pair<DocumentId, ExportSettings>>()
    val cancelled = mutableListOf<DocumentId>()
    override val states = MutableStateFlow<Map<DocumentId, ExportState>>(emptyMap())

    override fun start(documentId: DocumentId, settings: ExportSettings) {
        started += documentId to settings
        set(documentId, ExportState.Running(ExportPhase.PREPARING, 0, 0))
    }

    override fun cancel(documentId: DocumentId) {
        cancelled += documentId
        if (states.value[documentId] is ExportState.Running) set(documentId, ExportState.Cancelled)
    }

    override fun acknowledge(documentId: DocumentId) {
        if (states.value[documentId]?.isFinished == true) states.value = states.value - documentId
    }

    fun set(documentId: DocumentId, state: ExportState) {
        states.value = states.value + (documentId to state)
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.engine

import io.github.ukpratik.folio.core.domain.error.FolioError
import io.github.ukpratik.folio.core.model.ByteSize
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.ExportResult
import io.github.ukpratik.folio.core.model.ExportSettings
import kotlinx.coroutines.flow.StateFlow

enum class ExportPhase { PREPARING, ENCODING, SHRINKING, WRITING }

/** LLD §6.1 state machine, as seen by the UI. */
sealed interface ExportState {
    data class Running(val phase: ExportPhase, val pagesDone: Int, val pageCount: Int) : ExportState
    data class Succeeded(val result: ExportResult) : ExportState

    /** Written at the smallest readable size, which is still above [ExportResult.target] (FR-18). */
    data class TargetMissed(val result: ExportResult, val smallest: ByteSize) : ExportState
    data class Failed(val error: FolioError) : ExportState
    data object Cancelled : ExportState

    val isFinished: Boolean get() = this !is Running
}

/** Creates PDFs/JPGs on device (FR-19, FR-24). One export per document at a time. */
interface ExportEngine {
    val states: StateFlow<Map<DocumentId, ExportState>>

    /** Starts an export, or returns quietly if one is already running for [documentId]. */
    fun start(documentId: DocumentId, settings: ExportSettings)

    fun cancel(documentId: DocumentId)

    /** Clears a finished state once the UI has handled it. */
    fun acknowledge(documentId: DocumentId)
}

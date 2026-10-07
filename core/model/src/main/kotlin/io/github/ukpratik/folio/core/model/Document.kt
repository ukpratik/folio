// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.model

import java.time.Instant

enum class DocumentStatus { DRAFT, EXPORTED }

data class Document(
    val id: DocumentId,
    val title: String,
    val status: DocumentStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val exportSettings: ExportSettings,
    val lastExport: ExportResult? = null,
    /** An export started and never finished (process death) — or is running right now. */
    val exportInterrupted: Boolean = false,
)

/** The latest export of a document. [paths] is one PDF, or one JPG per page (FR-24). */
data class ExportResult(
    val format: ExportFormat,
    val paths: List<String>,
    val size: ByteSize,
    val pageCount: Int,
    val target: ByteSize?,
    val targetMet: Boolean?,
    val at: Instant,
)

/** What a Recents row shows for a document: its live page count and first page (FR-25). */
data class DocumentCover(val pageCount: Int, val firstPage: Page)

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
)

data class ExportResult(
    val path: String,
    val size: ByteSize,
    val pageCount: Int,
    val target: ByteSize?,
    val targetMet: Boolean?,
    val at: Instant,
    val interrupted: Boolean = false,
)

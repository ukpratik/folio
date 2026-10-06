// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.data.db

import io.github.ukpratik.folio.core.model.Adjustments
import io.github.ukpratik.folio.core.model.ByteSize
import io.github.ukpratik.folio.core.model.Document
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.DocumentStatus
import io.github.ukpratik.folio.core.model.EnhancementMode
import io.github.ukpratik.folio.core.model.ExportFormat
import io.github.ukpratik.folio.core.model.ExportResult
import io.github.ukpratik.folio.core.model.ExportSettings
import io.github.ukpratik.folio.core.model.Margin
import io.github.ukpratik.folio.core.model.Orientation
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.PageSize
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.model.PointF01
import io.github.ukpratik.folio.core.model.QualityPreset
import io.github.ukpratik.folio.core.model.Quad
import io.github.ukpratik.folio.core.model.Rotation
import java.time.Instant

internal fun DocumentEntity.toModel() = Document(
    id = DocumentId(id),
    title = title,
    status = DocumentStatus.valueOf(status),
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    exportSettings = ExportSettings(
        format = ExportFormat.valueOf(expFormat),
        pageSize = PageSize.valueOf(expPageSize),
        orientation = Orientation.valueOf(expOrientation),
        margin = Margin.valueOf(expMargin),
        quality = QualityPreset.valueOf(expQuality),
        target = expTargetBytes?.let(::ByteSize),
    ),
    lastExport = if (outPath != null && outSize != null && outPages != null && outAt != null) {
        ExportResult(
            path = outPath,
            size = ByteSize(outSize),
            pageCount = outPages,
            target = outTarget?.let(::ByteSize),
            targetMet = outTargetMet,
            at = Instant.ofEpochMilli(outAt),
            interrupted = exportInterrupted,
        )
    } else {
        null
    },
)

internal fun newDocumentEntity(id: DocumentId, title: String, settings: ExportSettings, now: Long) = DocumentEntity(
    id = id.value,
    title = title,
    status = DocumentStatus.DRAFT.name,
    createdAt = now,
    updatedAt = now,
    expFormat = settings.format.name,
    expPageSize = settings.pageSize.name,
    expOrientation = settings.orientation.name,
    expMargin = settings.margin.name,
    expQuality = settings.quality.name,
    expTargetBytes = settings.target?.bytes,
)

internal fun PageEntity.toModel() = Page(
    id = PageId(id),
    documentId = DocumentId(documentId),
    order = orderIndex,
    sourceId = sourceId,
    corners = corners?.toQuad(),
    autoCorners = autoCorners?.toQuad(),
    rotation = Rotation.entries.first { it.degrees == rotation },
    mode = EnhancementMode.valueOf(mode),
    adjustments = Adjustments(brightness, contrast),
    status = PageStatus.valueOf(status),
    editVersion = editVersion,
)

internal fun Page.toEntity() = PageEntity(
    id = id.value,
    documentId = documentId.value,
    orderIndex = order,
    sourceId = sourceId,
    corners = corners?.encode(),
    autoCorners = autoCorners?.encode(),
    rotation = rotation.degrees,
    mode = mode.name,
    brightness = adjustments.brightness,
    contrast = adjustments.contrast,
    status = status.name,
    editVersion = editVersion,
)

internal fun Quad.encode(): String = listOf(tl, tr, br, bl).joinToString(";") { "${it.x},${it.y}" }

internal fun String.toQuad(): Quad {
    val p = split(";").map { pair -> pair.split(",").let { PointF01(it[0].toFloat(), it[1].toFloat()) } }
    require(p.size == 4) { "Quad needs 4 points" }
    return Quad(p[0], p[1], p[2], p[3])
}

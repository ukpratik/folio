// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Schema per LLD §3.1. */
@Entity(tableName = "document", indices = [Index(value = ["updated_at"])])
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val status: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "exp_format") val expFormat: String,
    @ColumnInfo(name = "exp_page_size") val expPageSize: String,
    @ColumnInfo(name = "exp_orientation") val expOrientation: String,
    @ColumnInfo(name = "exp_margin") val expMargin: String,
    @ColumnInfo(name = "exp_quality") val expQuality: String,
    @ColumnInfo(name = "exp_target_bytes") val expTargetBytes: Long?,
    @ColumnInfo(name = "out_path") val outPath: String? = null,
    @ColumnInfo(name = "out_size") val outSize: Long? = null,
    @ColumnInfo(name = "out_pages") val outPages: Int? = null,
    @ColumnInfo(name = "out_target") val outTarget: Long? = null,
    @ColumnInfo(name = "out_target_met") val outTargetMet: Boolean? = null,
    @ColumnInfo(name = "out_at") val outAt: Long? = null,
    @ColumnInfo(name = "export_interrupted") val exportInterrupted: Boolean = false,
)

@Entity(
    tableName = "page",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["document_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["document_id", "order_index"]), Index(value = ["document_id", "source_id"]), Index(value = ["status"])],
)
data class PageEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "document_id") val documentId: String,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "source_id") val sourceId: String,
    /** "x,y;x,y;x,y;x,y" in normalised source space, or null for the full image. */
    val corners: String?,
    @ColumnInfo(name = "auto_corners") val autoCorners: String?,
    val rotation: Int,
    val mode: String,
    val brightness: Float,
    val contrast: Float,
    val status: String,
    @ColumnInfo(name = "edit_version") val editVersion: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

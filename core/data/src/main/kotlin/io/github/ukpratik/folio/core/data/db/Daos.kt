// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM document ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM document WHERE id = :id")
    fun observe(id: String): Flow<DocumentEntity?>

    @Upsert
    suspend fun upsert(document: DocumentEntity)

    @Query("UPDATE document SET title = :title, updated_at = :now WHERE id = :id")
    suspend fun rename(id: String, title: String, now: Long)

    @Query("UPDATE document SET updated_at = :now WHERE id = :id")
    suspend fun touch(id: String, now: Long)

    @Query("DELETE FROM document WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM document WHERE id = :id")
    suspend fun get(id: String): DocumentEntity?

    @Query(
        "UPDATE document SET exp_format = :format, exp_page_size = :pageSize, exp_orientation = :orientation, " +
            "exp_margin = :margin, exp_quality = :quality, exp_target_bytes = :target WHERE id = :id",
    )
    suspend fun saveExportSettings(
        id: String, format: String, pageSize: String, orientation: String, margin: String, quality: String, target: Long?,
    )

    @Query("UPDATE document SET export_interrupted = :running WHERE id = :id")
    suspend fun setExportRunning(id: String, running: Boolean)

    @Query(
        "UPDATE document SET status = 'EXPORTED', out_format = :format, out_path = :paths, out_size = :size, " +
            "out_pages = :pages, out_target = :target, out_target_met = :targetMet, out_at = :at, " +
            "export_interrupted = 0, updated_at = :at WHERE id = :id",
    )
    suspend fun markExported(
        id: String, format: String, paths: String, size: Long, pages: Int, target: Long?, targetMet: Boolean?, at: Long,
    )
}

@Dao
interface PageDao {
    @Query("SELECT * FROM page WHERE document_id = :documentId AND deleted_at IS NULL ORDER BY order_index")
    fun observe(documentId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM page WHERE id = :id")
    suspend fun get(id: String): PageEntity?

    /** First live page of every document, with the document's live page count (Recents). */
    @Query(
        "SELECT p.*, (SELECT COUNT(*) FROM page c WHERE c.document_id = p.document_id AND c.deleted_at IS NULL) " +
            "AS page_count FROM page p WHERE p.deleted_at IS NULL AND p.order_index = " +
            "(SELECT MIN(m.order_index) FROM page m WHERE m.document_id = p.document_id AND m.deleted_at IS NULL)",
    )
    fun observeCovers(): Flow<List<CoverRow>>

    @Query("SELECT COUNT(*) FROM page WHERE document_id = :documentId AND deleted_at IS NULL")
    suspend fun count(documentId: String): Int

    @Query("SELECT COALESCE(MAX(order_index), -1) FROM page WHERE document_id = :documentId AND deleted_at IS NULL")
    suspend fun maxOrder(documentId: String): Int

    @Insert
    suspend fun insertAll(pages: List<PageEntity>)

    @Query(
        "UPDATE page SET status = 'READY', auto_corners = :autoCorners, corners = :corners, " +
            "edit_version = edit_version + 1 WHERE id = :id",
    )
    suspend fun completeImport(id: String, autoCorners: String?, corners: String?)

    @Query("UPDATE page SET rotation = :degrees, edit_version = edit_version + 1 WHERE id = :id")
    suspend fun setRotation(id: String, degrees: Int)

    @Query("UPDATE page SET corners = :corners, edit_version = edit_version + 1 WHERE id = :id")
    suspend fun setCorners(id: String, corners: String?)

    @Query(
        "UPDATE page SET mode = :mode, brightness = :brightness, contrast = :contrast, " +
            "edit_version = edit_version + 1 WHERE id = :id",
    )
    suspend fun setEnhancement(id: String, mode: String, brightness: Float, contrast: Float)

    /** Apply-to-all and its Undo write every page in one transaction. */
    @Transaction
    suspend fun setEnhancements(changes: List<Triple<String, String, Pair<Float, Float>>>) {
        changes.forEach { (id, mode, adj) -> setEnhancement(id, mode, adj.first, adj.second) }
    }

    @Query(
        "UPDATE page SET corners = auto_corners, rotation = 0, mode = 'AUTO', brightness = 0, contrast = 0, " +
            "edit_version = edit_version + 1 WHERE id = :id",
    )
    suspend fun reset(id: String)

    @Transaction
    suspend fun insertAndReorder(page: PageEntity, orderedIds: List<String>) {
        insertAll(listOf(page))
        reorder(orderedIds)
    }

    @Query("UPDATE page SET status = :status WHERE id = :id")
    suspend fun setStatus(id: String, status: String)

    @Query("UPDATE page SET order_index = :order WHERE id = :id")
    suspend fun setOrder(id: String, order: Int)

    /** Rewrites dense order indices in one transaction (LLD §3.1). */
    @Transaction
    suspend fun reorder(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id -> setOrder(id, index) }
    }

    @Query("UPDATE page SET deleted_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE page SET deleted_at = NULL WHERE id = :id")
    suspend fun restore(id: String)

    @Query("DELETE FROM page WHERE id IN (:ids)")
    suspend fun deleteHard(ids: List<String>)

    @Query("SELECT COUNT(*) FROM page WHERE document_id = :documentId AND source_id = :sourceId")
    suspend fun countSourceReferences(documentId: String, sourceId: String): Int

    @Query("SELECT * FROM page WHERE status = :status AND created_at < :before AND deleted_at IS NULL")
    suspend fun findWithStatusCreatedBefore(status: String, before: Long): List<PageEntity>

    @Query("SELECT * FROM page WHERE deleted_at IS NOT NULL AND deleted_at < :before")
    suspend fun findSoftDeletedBefore(before: Long): List<PageEntity>
}

data class CoverRow(
    @Embedded val page: PageEntity,
    @ColumnInfo(name = "page_count") val pageCount: Int,
)

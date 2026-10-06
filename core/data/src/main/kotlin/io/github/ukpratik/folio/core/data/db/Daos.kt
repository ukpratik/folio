// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.data.db

import androidx.room.Dao
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
}

@Dao
interface PageDao {
    @Query("SELECT * FROM page WHERE document_id = :documentId AND deleted_at IS NULL ORDER BY order_index")
    fun observe(documentId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM page WHERE id = :id")
    suspend fun get(id: String): PageEntity?

    @Query("SELECT COUNT(*) FROM page WHERE document_id = :documentId AND deleted_at IS NULL")
    suspend fun count(documentId: String): Int

    @Query("SELECT COALESCE(MAX(order_index), -1) FROM page WHERE document_id = :documentId AND deleted_at IS NULL")
    suspend fun maxOrder(documentId: String): Int

    @Insert
    suspend fun insertAll(pages: List<PageEntity>)

    @Upsert
    suspend fun upsert(page: PageEntity)

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

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.repository

import io.github.ukpratik.folio.core.model.Document
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.Enhancement
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.model.Quad
import io.github.ukpratik.folio.core.model.Rotation
import io.github.ukpratik.folio.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun observeRecents(): Flow<List<Document>>
    fun observe(id: DocumentId): Flow<Document?>
    suspend fun create(title: String): Document
    suspend fun rename(id: DocumentId, title: String)
    suspend fun touch(id: DocumentId)
    suspend fun delete(id: DocumentId)
}

interface PageRepository {
    fun observePages(documentId: DocumentId): Flow<List<Page>>
    suspend fun get(id: PageId): Page?
    suspend fun count(documentId: DocumentId): Int

    /** Highest order index in use, or -1 for an empty document. */
    suspend fun maxOrder(documentId: DocumentId): Int
    suspend fun insertAll(pages: List<Page>)
    suspend fun setStatus(id: PageId, status: PageStatus)

    /** Sets the rotation and bumps the edit version (thumbnail cache key). */
    suspend fun setRotation(id: PageId, rotation: Rotation)

    /** Each of these targeted updates bumps the edit version (thumbnail cache, LLD §5.4). */
    suspend fun setCorners(id: PageId, corners: Quad?)
    suspend fun setEnhancements(changes: Map<PageId, Enhancement>)

    /** FR-11: back to the imported state — auto-detected crop, no rotation, Auto mode, no adjustments. */
    suspend fun reset(id: PageId)

    /** Inserts [page] and rewrites the document's order to [orderedIds] in one transaction. */
    suspend fun insertAndReorder(page: Page, orderedIds: List<PageId>)

    /** Marks an imported page READY with its detected corners. Never resurrects a deleted row. */
    suspend fun completeImport(id: PageId, autoCorners: Quad?, corners: Quad?)
    suspend fun reorder(documentId: DocumentId, orderedIds: List<PageId>)
    suspend fun softDelete(id: PageId)
    suspend fun restore(id: PageId)

    /** Permanently removes rows. Callers are responsible for source-file reference counting. */
    suspend fun deleteHard(ids: List<PageId>)

    /** Rows (including soft-deleted ones) that still use [sourceId]. */
    suspend fun countSourceReferences(documentId: DocumentId, sourceId: String): Int
    suspend fun findWithStatusCreatedBefore(status: PageStatus, before: Long): List<Page>
    suspend fun findSoftDeletedBefore(before: Long): List<Page>
}

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun update(transform: (UserPreferences) -> UserPreferences)
}

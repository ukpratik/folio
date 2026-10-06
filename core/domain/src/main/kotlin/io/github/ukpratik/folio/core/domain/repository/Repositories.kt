// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.repository

import io.github.ukpratik.folio.core.model.Document
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun observeRecents(): Flow<List<Document>>
    fun observe(id: DocumentId): Flow<Document?>
    suspend fun create(title: String): Document
    suspend fun rename(id: DocumentId, title: String)
    suspend fun delete(id: DocumentId)
}

interface PageRepository {
    fun observePages(documentId: DocumentId): Flow<List<Page>>
    suspend fun upsert(page: Page)
    suspend fun reorder(documentId: DocumentId, orderedIds: List<PageId>)
    suspend fun softDelete(id: PageId)
    suspend fun restore(id: PageId)
}

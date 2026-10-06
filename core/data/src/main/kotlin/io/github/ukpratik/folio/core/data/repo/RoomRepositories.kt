// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.data.repo

import io.github.ukpratik.folio.core.data.db.DocumentDao
import io.github.ukpratik.folio.core.data.db.PageDao
import io.github.ukpratik.folio.core.data.db.newDocumentEntity
import io.github.ukpratik.folio.core.data.db.toEntity
import io.github.ukpratik.folio.core.data.db.toModel
import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.domain.repository.PageRepository
import io.github.ukpratik.folio.core.model.Document
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.ExportSettings
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomDocumentRepository @Inject constructor(
    private val dao: DocumentDao,
) : DocumentRepository {
    override fun observeRecents(): Flow<List<Document>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observe(id: DocumentId): Flow<Document?> = dao.observe(id.value).map { it?.toModel() }

    override suspend fun create(title: String): Document {
        val now = System.currentTimeMillis()
        val settings = ExportSettings(pageSize = ExportSettings.defaultPageSizeFor(Locale.getDefault().country))
        val entity = newDocumentEntity(DocumentId.new(), title, settings, now)
        dao.upsert(entity)
        return entity.toModel()
    }

    override suspend fun rename(id: DocumentId, title: String) = dao.rename(id.value, title, System.currentTimeMillis())

    // TODO(E9): delete documents/{id}/ via FileStore and garbage-collect orphaned sources (LLD §3.4).
    override suspend fun delete(id: DocumentId) = dao.delete(id.value)
}

internal class RoomPageRepository @Inject constructor(
    private val dao: PageDao,
) : PageRepository {
    override fun observePages(documentId: DocumentId): Flow<List<Page>> =
        dao.observe(documentId.value).map { list -> list.map { it.toModel() } }

    override suspend fun upsert(page: Page) = dao.upsert(page.toEntity())

    override suspend fun reorder(documentId: DocumentId, orderedIds: List<PageId>) =
        dao.reorder(orderedIds.map { it.value })

    override suspend fun softDelete(id: PageId) = dao.softDelete(id.value, System.currentTimeMillis())

    override suspend fun restore(id: PageId) = dao.restore(id.value)
}

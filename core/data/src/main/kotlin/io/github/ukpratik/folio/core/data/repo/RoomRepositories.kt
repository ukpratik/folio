// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.data.repo

import io.github.ukpratik.folio.core.data.db.DocumentDao
import io.github.ukpratik.folio.core.data.db.PageDao
import io.github.ukpratik.folio.core.data.db.encode
import io.github.ukpratik.folio.core.data.db.newDocumentEntity
import io.github.ukpratik.folio.core.data.db.toEntity
import io.github.ukpratik.folio.core.data.db.toModel
import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.domain.repository.PageRepository
import io.github.ukpratik.folio.core.domain.time.Clock
import io.github.ukpratik.folio.core.model.Document
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.Enhancement
import io.github.ukpratik.folio.core.model.ExportSettings
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.model.Quad
import io.github.ukpratik.folio.core.model.Rotation
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomDocumentRepository @Inject constructor(
    private val dao: DocumentDao,
    private val clock: Clock,
) : DocumentRepository {
    override fun observeRecents(): Flow<List<Document>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observe(id: DocumentId): Flow<Document?> = dao.observe(id.value).map { it?.toModel() }

    override suspend fun create(title: String): Document {
        val now = clock.nowMillis()
        val settings = ExportSettings(pageSize = ExportSettings.defaultPageSizeFor(Locale.getDefault().country))
        val entity = newDocumentEntity(DocumentId.new(), title, settings, now)
        dao.upsert(entity)
        return entity.toModel()
    }

    override suspend fun rename(id: DocumentId, title: String) = dao.rename(id.value, title, clock.nowMillis())

    override suspend fun touch(id: DocumentId) = dao.touch(id.value, clock.nowMillis())

    override suspend fun delete(id: DocumentId) = dao.delete(id.value)
}

internal class RoomPageRepository @Inject constructor(
    private val dao: PageDao,
    private val clock: Clock,
) : PageRepository {
    override fun observePages(documentId: DocumentId): Flow<List<Page>> =
        dao.observe(documentId.value).map { list -> list.map { it.toModel() } }

    override suspend fun get(id: PageId): Page? = dao.get(id.value)?.toModel()

    override suspend fun count(documentId: DocumentId): Int = dao.count(documentId.value)

    override suspend fun maxOrder(documentId: DocumentId): Int = dao.maxOrder(documentId.value)

    override suspend fun insertAll(pages: List<Page>) = dao.insertAll(pages.map { it.toEntity() })

    override suspend fun setRotation(id: PageId, rotation: Rotation) = dao.setRotation(id.value, rotation.degrees)

    override suspend fun setCorners(id: PageId, corners: Quad?) = dao.setCorners(id.value, corners?.encode())

    override suspend fun setEnhancements(changes: Map<PageId, Enhancement>) = dao.setEnhancements(
        changes.map { (id, e) -> Triple(id.value, e.mode.name, e.adjustments.brightness to e.adjustments.contrast) },
    )

    override suspend fun reset(id: PageId) = dao.reset(id.value)

    override suspend fun insertAndReorder(page: Page, orderedIds: List<PageId>) =
        dao.insertAndReorder(page.toEntity(), orderedIds.map { it.value })

    override suspend fun completeImport(id: PageId, autoCorners: Quad?, corners: Quad?) =
        dao.completeImport(id.value, autoCorners?.encode(), corners?.encode())

    override suspend fun setStatus(id: PageId, status: PageStatus) = dao.setStatus(id.value, status.name)

    override suspend fun reorder(documentId: DocumentId, orderedIds: List<PageId>) =
        dao.reorder(orderedIds.map { it.value })

    override suspend fun softDelete(id: PageId) = dao.softDelete(id.value, clock.nowMillis())

    override suspend fun restore(id: PageId) = dao.restore(id.value)

    override suspend fun deleteHard(ids: List<PageId>) {
        // SQLite caps bound parameters; chunk to stay well below the limit.
        ids.map { it.value }.chunked(500).forEach { dao.deleteHard(it) }
    }

    override suspend fun countSourceReferences(documentId: DocumentId, sourceId: String): Int =
        dao.countSourceReferences(documentId.value, sourceId)

    override suspend fun findWithStatusCreatedBefore(status: PageStatus, before: Long): List<Page> =
        dao.findWithStatusCreatedBefore(status.name, before).map { it.toModel() }

    override suspend fun findSoftDeletedBefore(before: Long): List<Page> =
        dao.findSoftDeletedBefore(before).map { it.toModel() }
}

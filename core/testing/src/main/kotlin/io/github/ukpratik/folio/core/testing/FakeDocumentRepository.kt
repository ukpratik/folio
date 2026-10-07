// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.testing

import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.model.Document
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.DocumentStatus
import io.github.ukpratik.folio.core.model.ExportResult
import io.github.ukpratik.folio.core.model.ExportSettings
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeDocumentRepository(private val clock: FakeClock = FakeClock()) : DocumentRepository {
    private val docs = MutableStateFlow<Map<DocumentId, Document>>(emptyMap())
    val all: List<Document> get() = docs.value.values.toList()

    override fun observeRecents(): Flow<List<Document>> = docs.map { it.values.sortedByDescending(Document::updatedAt) }

    override fun observe(id: DocumentId): Flow<Document?> = docs.map { it[id] }

    suspend fun create(title: String): Document = create(title, ExportSettings())

    override suspend fun create(title: String, settings: ExportSettings): Document {
        val now = Instant.ofEpochMilli(clock.nowMillis())
        val doc = Document(DocumentId.new(), title, DocumentStatus.DRAFT, now, now, settings)
        docs.value = docs.value + (doc.id to doc)
        return doc
    }

    override suspend fun rename(id: DocumentId, title: String) = update(id) { it.copy(title = title) }

    override suspend fun touch(id: DocumentId) = update(id) { it.copy(updatedAt = Instant.ofEpochMilli(clock.nowMillis())) }

    override suspend fun delete(id: DocumentId) {
        docs.value = docs.value - id
    }

    override suspend fun get(id: DocumentId): Document? = docs.value[id]

    override suspend fun saveExportSettings(id: DocumentId, settings: ExportSettings) = update(id) { it.copy(exportSettings = settings) }

    override suspend fun setExportRunning(id: DocumentId, running: Boolean) = update(id) { it.copy(exportInterrupted = running) }

    override suspend fun markExported(id: DocumentId, result: ExportResult) = update(id) {
        it.copy(status = DocumentStatus.EXPORTED, lastExport = result, updatedAt = result.at, exportInterrupted = false)
    }

    private fun update(id: DocumentId, change: (Document) -> Document) {
        docs.value[id]?.let { docs.value = docs.value + (id to change(it)) }
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.testing

import io.github.ukpratik.folio.core.domain.files.DocumentFiles
import io.github.ukpratik.folio.core.model.DocumentId
import java.io.File
import java.io.OutputStream
import java.nio.file.Files

/** Real files in a temp directory, same layout as production. */
class FakeDocumentFiles(val root: File = Files.createTempDirectory("folio-test").toFile()) : DocumentFiles {
    private val work get() = File(root, "work")
    var workWipes = 0
        private set

    override fun sourceFile(doc: DocumentId, sourceId: String) = File(root, "documents/${doc.value}/src/$sourceId.jpg")

    override fun outputFile(doc: DocumentId, fileName: String) = File(root, "documents/${doc.value}/out/$fileName")

    override fun newWorkFile(prefix: String, extension: String): File {
        work.mkdirs()
        return File.createTempFile(prefix, ".$extension", work)
    }

    override suspend fun <T> writeAtomically(target: File, block: suspend (OutputStream) -> T): T {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, target.name + ".tmp")
        return try {
            tmp.outputStream().use { block(it) }.also { check(tmp.renameTo(target)) }
        } finally {
            tmp.delete()
        }
    }

    override suspend fun rename(file: File, newName: String): File {
        val target = File(file.parentFile, newName)
        return if (target != file && file.renameTo(target)) target else file
    }

    override suspend fun deleteSource(doc: DocumentId, sourceId: String) {
        sourceFile(doc, sourceId).delete()
    }

    override suspend fun deleteDocument(doc: DocumentId) {
        File(root, "documents/${doc.value}").deleteRecursively()
    }

    override suspend fun wipeWorkDir() {
        workWipes++
        work.deleteRecursively()
    }

    var free: Long = Long.MAX_VALUE

    override fun freeBytes(): Long = free
}

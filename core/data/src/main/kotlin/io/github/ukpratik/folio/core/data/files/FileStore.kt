// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.data.files

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.ukpratik.folio.core.domain.concurrency.IoDispatcher
import io.github.ukpratik.folio.core.domain.files.DocumentFiles
import io.github.ukpratik.folio.core.model.DocumentId
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** App-private file layout (HLD §7). Every write is temp file → fsync → rename (ADR-0007). */
internal class FileStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : DocumentFiles {
    private val documentsDir get() = File(context.filesDir, "documents")
    private val workDir get() = File(context.cacheDir, "work")

    override fun sourceFile(doc: DocumentId, sourceId: String) = File(documentsDir, "${doc.value}/src/$sourceId.jpg")

    override fun outputFile(doc: DocumentId, fileName: String) = File(documentsDir, "${doc.value}/out/$fileName")

    override fun newWorkFile(prefix: String, extension: String): File {
        workDir.mkdirs()
        return File.createTempFile(prefix, ".$extension", workDir)
    }

    override suspend fun <T> writeAtomically(target: File, block: suspend (OutputStream) -> T): T = withContext(io) {
        val dir = checkNotNull(target.parentFile) { "target has no parent" }
        dir.mkdirs()
        val tmp = File(dir, target.name + ".tmp")
        try {
            val result = FileOutputStream(tmp).use { out ->
                block(out).also {
                    out.flush()
                    out.fd.sync()
                }
            }
            check(tmp.renameTo(target)) { "rename failed" }
            result
        } finally {
            tmp.delete()
        }
    }

    override suspend fun rename(file: File, newName: String): File = withContext(io) {
        val target = File(file.parentFile, newName)
        if (target != file && file.renameTo(target)) target else file
    }

    override suspend fun deleteSource(doc: DocumentId, sourceId: String) {
        withContext(io) { sourceFile(doc, sourceId).delete() }
    }

    override suspend fun deleteDocument(doc: DocumentId) {
        withContext(io) { File(documentsDir, doc.value).deleteRecursively() }
    }

    override suspend fun wipeWorkDir() {
        withContext(io) { workDir.deleteRecursively() }
    }

    override fun freeBytes(): Long = context.filesDir.usableSpace
}

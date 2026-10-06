// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.data.files

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.ukpratik.folio.core.model.DocumentId
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** App-private file layout (HLD §7). Every write is temp-file → fsync → rename (ADR-0007). */
@Singleton
class FileStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val documentsDir get() = File(context.filesDir, "documents")
    private val workDir get() = File(context.cacheDir, "work")

    fun sourceFile(doc: DocumentId, sourceId: String) = File(documentsDir, "${doc.value}/src/$sourceId.jpg")

    fun outputFile(doc: DocumentId, fileName: String) = File(documentsDir, "${doc.value}/out/$fileName")

    fun newWorkFile(prefix: String, ext: String): File {
        workDir.mkdirs()
        return File.createTempFile(prefix, ".$ext", workDir)
    }

    suspend fun <T> writeAtomically(target: File, block: suspend (OutputStream) -> T): T = withContext(Dispatchers.IO) {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, target.name + ".tmp")
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

    suspend fun deleteDocumentDir(doc: DocumentId) = withContext(Dispatchers.IO) {
        File(documentsDir, doc.value).deleteRecursively()
    }

    suspend fun wipeWorkDir() = withContext(Dispatchers.IO) { workDir.deleteRecursively() }

    fun freeBytes(): Long = context.filesDir.usableSpace
}

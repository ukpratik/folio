// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.data.files

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.ukpratik.folio.core.domain.concurrency.IoDispatcher
import io.github.ukpratik.folio.core.domain.error.FolioError
import io.github.ukpratik.folio.core.domain.error.Outcome
import io.github.ukpratik.folio.core.domain.files.SaveDestinations
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Storage Access Framework copies (ADR-0014): no storage permission, the user picks every destination.
 * A destination that has gone away (SD card removed, provider uninstalled) becomes [FolioError.SaveTargetUnavailable].
 */
internal class ContentResolverSaveDestinations @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SaveDestinations {
    private val resolver get() = context.contentResolver

    override suspend fun copyToDocument(source: File, documentUri: String): Outcome<Unit> = guarded {
        copy(source, Uri.parse(documentUri))
    }

    override suspend fun copyToFolder(sources: List<File>, folderUri: String, mimeType: String): Outcome<String?> = guarded {
        val tree = Uri.parse(folderUri)
        val folder = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        sources.forEach { source ->
            // The provider de-duplicates names itself ("name (1).jpg").
            val created = DocumentsContract.createDocument(resolver, folder, mimeType, source.name)
                ?: throw FileNotFoundException("Couldn't create ${source.name}")
            copy(source, created)
        }
        displayName(folder)
    }

    private fun copy(source: File, target: Uri) {
        // "wt" truncates: re-saving over an existing document must not leave old bytes at the end.
        val out = resolver.openOutputStream(target, "wt") ?: throw FileNotFoundException("No stream for $target")
        out.use { stream -> source.inputStream().use { it.copyTo(stream) } }
    }

    private fun displayName(uri: Uri): String? = runCatching {
        resolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    private suspend fun <T> guarded(block: () -> T): Outcome<T> = withContext(io) {
        try {
            Outcome.Success(block())
        } catch (e: FileNotFoundException) {
            Timber.w(e, "Save target unavailable")
            Outcome.Failure(FolioError.SaveTargetUnavailable)
        } catch (e: SecurityException) {
            Timber.w(e, "Save target permission lost")
            Outcome.Failure(FolioError.SaveTargetUnavailable)
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "Save target invalid")
            Outcome.Failure(FolioError.SaveTargetUnavailable)
        } catch (e: java.io.IOException) {
            Timber.w(e, "Save failed")
            Outcome.Failure(FolioError.Unexpected(e))
        }
    }
}

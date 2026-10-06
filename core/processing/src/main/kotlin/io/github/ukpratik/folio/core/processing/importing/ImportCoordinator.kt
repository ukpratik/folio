// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.importing

import io.github.ukpratik.folio.core.domain.concurrency.ApplicationScope
import io.github.ukpratik.folio.core.domain.concurrency.IoDispatcher
import io.github.ukpratik.folio.core.domain.concurrency.ProcessingDispatcher
import io.github.ukpratik.folio.core.domain.engine.ImportEngine
import io.github.ukpratik.folio.core.domain.engine.ImportJob
import io.github.ukpratik.folio.core.domain.engine.ImportProgress
import io.github.ukpratik.folio.core.domain.files.DocumentFiles
import io.github.ukpratik.folio.core.domain.repository.PageRepository
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.processing.ProcessingConfig
import io.github.ukpratik.folio.core.processing.image.ImageNormalizer
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Copies → normalises each imported image into app storage (LLD §6.3).
 * Runs in the application scope so imports continue when the user leaves the editor (ADR-0012).
 */
@Singleton
internal class ImportCoordinator @Inject constructor(
    private val opener: ContentOpener,
    private val normalizer: ImageNormalizer,
    private val files: DocumentFiles,
    private val pages: PageRepository,
    config: ProcessingConfig,
    @ApplicationScope scope: CoroutineScope,
    @ProcessingDispatcher processing: CoroutineDispatcher,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ImportEngine {

    private val _progress = MutableStateFlow<Map<DocumentId, ImportProgress>>(emptyMap())
    override val progress: StateFlow<Map<DocumentId, ImportProgress>> = _progress.asStateFlow()

    private val queue = Channel<ImportJob>(Channel.UNLIMITED)

    init {
        repeat(config.parallelism) {
            scope.launch(processing) {
                for (job in queue) process(job)
            }
        }
    }

    override fun enqueue(jobs: List<ImportJob>) {
        if (jobs.isEmpty()) return
        _progress.update { current ->
            current + jobs.groupingBy { it.documentId }.eachCount().map { (doc, count) ->
                val previous = current[doc]?.takeIf { it.isRunning } ?: ImportProgress(total = 0)
                doc to previous.copy(total = previous.total + count)
            }
        }
        jobs.forEach { queue.trySend(it) }
    }

    override fun acknowledge(documentId: DocumentId) {
        _progress.update { current -> if (current[documentId]?.isRunning == true) current else current - documentId }
    }

    private suspend fun process(job: ImportJob) {
        val succeeded = try {
            importOne(job)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Import failed")
            false
        } catch (e: OutOfMemoryError) {
            Timber.w(e, "Import ran out of memory")
            false
        }

        if (succeeded && pages.get(job.pageId) != null) {
            pages.setStatus(job.pageId, PageStatus.READY)
        } else {
            // Failed, or the page/document was deleted while importing: leave nothing behind.
            pages.deleteHard(listOf(job.pageId))
            files.deleteSource(job.documentId, job.sourceId)
        }
        _progress.update { current ->
            val entry = current[job.documentId] ?: return@update current
            val next = if (succeeded) entry.copy(done = entry.done + 1) else entry.copy(failed = entry.failed + 1)
            current + (job.documentId to next)
        }
    }

    private suspend fun importOne(job: ImportJob) {
        val copy = files.newWorkFile("import", "bin")
        try {
            withContext(io) { opener.open(job.source.uri).use { it.copyLimitedTo(copy) } }
            files.writeAtomically(files.sourceFile(job.documentId, job.sourceId)) { out: OutputStream ->
                normalizer.normalize(copy, out)
            }
        } finally {
            copy.delete()
        }
    }

    private fun InputStream.copyLimitedTo(target: File) {
        target.outputStream().use { out ->
            val buffer = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val read = read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_INPUT_BYTES) throw IOException("Image larger than $MAX_INPUT_BYTES bytes")
                out.write(buffer, 0, read)
            }
        }
    }

    private companion object {
        /** Reject absurd inputs before decoding (LLD §10). */
        const val MAX_INPUT_BYTES = 100L * 1024 * 1024
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.export

import io.github.ukpratik.folio.core.domain.concurrency.ApplicationScope
import io.github.ukpratik.folio.core.domain.concurrency.ProcessingDispatcher
import io.github.ukpratik.folio.core.domain.engine.ExportEngine
import io.github.ukpratik.folio.core.domain.engine.ExportPhase
import io.github.ukpratik.folio.core.domain.engine.ExportState
import io.github.ukpratik.folio.core.domain.error.FolioError
import io.github.ukpratik.folio.core.domain.files.DocumentFiles
import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.domain.repository.PageRepository
import io.github.ukpratik.folio.core.domain.time.Clock
import io.github.ukpratik.folio.core.model.ByteSize
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.ExportFileNames
import io.github.ukpratik.folio.core.model.ExportFormat
import io.github.ukpratik.folio.core.model.ExportResult
import io.github.ukpratik.folio.core.model.ExportSettings
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.processing.encode.EncodedPage
import io.github.ukpratik.folio.core.processing.encode.OptimizerPhase
import io.github.ukpratik.folio.core.processing.encode.PageRasterizer
import io.github.ukpratik.folio.core.processing.encode.SizeOptimizer
import io.github.ukpratik.folio.core.processing.pdf.PageGeometry
import io.github.ukpratik.folio.core.processing.pdf.PdfPage
import io.github.ukpratik.folio.core.processing.pdf.StreamingPdfWriter
import io.github.ukpratik.folio.core.processing.render.PageRenderer
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Builds the rasterizer for an export; swapped for a fake in tests (DIP). */
fun interface RasterizerFactory {
    fun create(settings: ExportSettings): PageRasterizer
}

internal class RendererRasterizerFactory @Inject constructor(
    private val renderer: PageRenderer,
    private val files: DocumentFiles,
) : RasterizerFactory {
    override fun create(settings: ExportSettings): PageRasterizer = ExportRasterizer(renderer, files, settings)
}

/**
 * LLD §6.1. Runs in the application scope, one export per document. Encoded pages go to work files as they're
 * produced; the final PDF/JPGs are written atomically into the document's out/ folder, replacing the previous
 * export only on success. Any failure or cancel leaves the draft and the previous export untouched.
 */
@Singleton
internal class ExportCoordinator @Inject constructor(
    private val rasterizers: RasterizerFactory,
    private val files: DocumentFiles,
    private val documents: DocumentRepository,
    private val pages: PageRepository,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
    @ProcessingDispatcher private val processing: CoroutineDispatcher,
) : ExportEngine {

    private val _states = MutableStateFlow<Map<DocumentId, ExportState>>(emptyMap())
    override val states: StateFlow<Map<DocumentId, ExportState>> = _states.asStateFlow()
    private val jobs = mutableMapOf<DocumentId, Job>()

    @Synchronized
    override fun start(documentId: DocumentId, settings: ExportSettings) {
        if (jobs[documentId]?.isActive == true) return // single flight
        set(documentId, ExportState.Running(ExportPhase.PREPARING, 0, 0))
        jobs[documentId] = scope.launch(processing) { run(documentId, settings) }
    }

    @Synchronized
    override fun cancel(documentId: DocumentId) {
        jobs[documentId]?.cancel()
    }

    override fun acknowledge(documentId: DocumentId) {
        _states.update { current -> if (current[documentId]?.isFinished == true) current - documentId else current }
    }

    private suspend fun run(documentId: DocumentId, settings: ExportSettings) {
        val workFiles = mutableListOf<File>()
        try {
            val document = documents.get(documentId) ?: return fail(documentId, FolioError.NothingToExport)
            val ready = pages.observePages(documentId).first().filter { it.status == PageStatus.READY }
            if (ready.isEmpty()) return fail(documentId, FolioError.NothingToExport)
            val shortBy = requiredBytes(ready.size, settings) - files.freeBytes()
            if (shortBy > 0) return fail(documentId, FolioError.LowStorage(ByteSize(shortBy)))

            documents.setExportRunning(documentId, true)
            val encoded = encodePages(documentId, ready, settings, workFiles)
            set(documentId, ExportState.Running(ExportPhase.WRITING, ready.size, ready.size))
            currentCoroutineContext().ensureActive()

            val outputs = when (settings.format) {
                ExportFormat.PDF -> listOf(writePdf(documentId, document.title, settings, encoded.pages))
                ExportFormat.JPG -> writeJpgs(documentId, document.title, encoded.pages)
            }
            removeStaleOutputs(documentId, keep = outputs)
            val size = outputs.sumOf { it.length() }
            val target = settings.target
            val met = target?.let { size <= it.bytes }
            val result = ExportResult(
                format = settings.format,
                paths = outputs.map { it.path },
                size = ByteSize(size),
                pageCount = ready.size,
                target = target,
                targetMet = met,
                at = Instant.ofEpochMilli(clock.nowMillis()),
            )
            documents.markExported(documentId, result)
            set(documentId, if (met == false) ExportState.TargetMissed(result, ByteSize(size)) else ExportState.Succeeded(result))
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                documents.setExportRunning(documentId, false)
                set(documentId, ExportState.Cancelled)
            }
            throw e
        } catch (e: OutOfMemoryError) {
            Timber.w(e, "Export ran out of memory")
            fail(documentId, FolioError.Unexpected(e))
        } catch (e: Exception) {
            Timber.w(e, "Export failed")
            fail(documentId, FolioError.Unexpected(e))
        } finally {
            withContext(NonCancellable) { workFiles.forEach(File::delete) }
            synchronized(this) { jobs.remove(documentId) }
        }
    }

    private data class Encoded(val pages: List<EncodedFile>)

    private data class EncodedFile(val file: File, val page: EncodedPage)

    private suspend fun encodePages(id: DocumentId, ready: List<Page>, settings: ExportSettings, workFiles: MutableList<File>): Encoded {
        val target = settings.target?.bytes
        val budget = target?.let {
            if (settings.format == ExportFormat.PDF) it - StreamingPdfWriter.overheadBytes(ready.size, TITLE_ALLOWANCE) else it
        }
        val out = mutableListOf<EncodedFile>()
        SizeOptimizer(rasterizers.create(settings)).encode(
            pages = ready,
            preset = settings.quality,
            targetBytes = budget,
            perImage = settings.format == ExportFormat.JPG,
            sink = { page ->
                val file = files.newWorkFile("page", "jpg").also(workFiles::add)
                file.writeBytes(page.jpeg)
                out += EncodedFile(file, page.copy(jpeg = ByteArray(0))) // drop bytes: they're on disk now
            },
            progress = { phase, done ->
                currentCoroutineContext().ensureActive()
                val exportPhase = when (phase) {
                    OptimizerPhase.PLANNING -> ExportPhase.PREPARING
                    OptimizerPhase.ENCODING -> ExportPhase.ENCODING
                    OptimizerPhase.SHRINKING -> ExportPhase.SHRINKING
                }
                set(id, ExportState.Running(exportPhase, done, ready.size))
            },
        )
        return Encoded(out.sortedBy { it.page.index })
    }

    private suspend fun writePdf(id: DocumentId, title: String, settings: ExportSettings, encoded: List<EncodedFile>): File {
        val pdfPages = encoded.map { (file, page) ->
            PdfPage(PageGeometry.layout(page.widthPx, page.heightPx, page.dpi, settings.pageSize, settings.orientation, settings.margin), file)
        }
        val target = files.outputFile(id, ExportFileNames.pdf(title))
        files.writeAtomically(target) { out -> StreamingPdfWriter().write(out, title, pdfPages) }
        return target
    }

    private suspend fun writeJpgs(id: DocumentId, title: String, encoded: List<EncodedFile>): List<File> =
        encoded.mapIndexed { i, (file, _) ->
            val target = files.outputFile(id, ExportFileNames.jpg(title, i))
            files.writeAtomically(target) { out -> file.inputStream().use { it.copyTo(out) } }
            target
        }

    /** Folio keeps only the latest export per document (FR-27). */
    private fun removeStaleOutputs(id: DocumentId, keep: List<File>) {
        val dir = files.outputFile(id, "x").parentFile ?: return
        val keepPaths = keep.map { it.absolutePath }.toSet()
        dir.listFiles()?.filter { it.isFile && it.absolutePath !in keepPaths }?.forEach(File::delete)
    }

    private suspend fun fail(id: DocumentId, error: FolioError) {
        withContext(NonCancellable) {
            documents.setExportRunning(id, false)
            set(id, ExportState.Failed(error))
        }
    }

    private fun set(id: DocumentId, state: ExportState) = _states.update { it + (id to state) }

    private companion object {
        /** Typical title length for the overhead estimate; long titles only cost a few bytes more. */
        const val TITLE_ALLOWANCE = 40
        private const val MB = 1_000_000L

        /** Work files + output, twice over, plus headroom (LLD §7.7). */
        fun requiredBytes(pageCount: Int, settings: ExportSettings): Long =
            (settings.target?.bytes ?: (pageCount * 2 * MB)) * 2 + 20 * MB
    }
}

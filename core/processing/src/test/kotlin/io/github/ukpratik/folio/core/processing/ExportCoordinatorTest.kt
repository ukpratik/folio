// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing

import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.domain.engine.ExportState
import io.github.ukpratik.folio.core.domain.error.FolioError
import io.github.ukpratik.folio.core.model.ByteSize
import io.github.ukpratik.folio.core.model.DocumentId
import io.github.ukpratik.folio.core.model.ExportFormat
import io.github.ukpratik.folio.core.model.ExportSettings
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageId
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.processing.encode.PageRasterizer
import io.github.ukpratik.folio.core.processing.encode.RasterPage
import io.github.ukpratik.folio.core.processing.export.ExportCoordinator
import io.github.ukpratik.folio.core.testing.FakeClock
import io.github.ukpratik.folio.core.testing.FakeDocumentFiles
import io.github.ukpratik.folio.core.testing.FakeDocumentRepository
import io.github.ukpratik.folio.core.testing.FakePageRepository
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.apache.pdfbox.Loader
import org.junit.Test

class ExportCoordinatorTest {
    private val clock = FakeClock()
    private val documents = FakeDocumentRepository(clock)
    private val pages = FakePageRepository(clock)
    private val files = FakeDocumentFiles()

    /** Real JPEGs (so the real PDF writer and PDFBox can check them), small enough to keep tests fast. */
    private class ImageIoRaster(dpi: Int) : RasterPage {
        override val widthPx = dpi * 2
        override val heightPx = dpi * 3
        private val image = BufferedImage(widthPx, heightPx, BufferedImage.TYPE_INT_RGB).apply {
            val g = createGraphics()
            g.color = java.awt.Color.WHITE; g.fillRect(0, 0, widthPx, heightPx)
            g.color = java.awt.Color.BLACK
            for (y in 0 until heightPx step 9) g.drawLine(10, y, widthPx - 10, y)
            g.dispose()
        }

        override fun encode(quality: Int): ByteArray {
            val writer = ImageIO.getImageWritersByFormatName("jpg").next()
            val out = ByteArrayOutputStream()
            ImageIO.createImageOutputStream(out).use { stream ->
                writer.output = stream
                val params = writer.defaultWriteParam.apply { compressionMode = ImageWriteParam.MODE_EXPLICIT; compressionQuality = quality / 100f }
                writer.write(null, IIOImage(image, null, null), params)
            }
            writer.dispose()
            return out.toByteArray()
        }

        override fun close() = Unit
    }

    /** Lets a test pause the export mid-way (to cancel it). */
    private var gate: CompletableDeferred<Unit>? = null

    private fun TestScope.coordinator() = ExportCoordinator(
        rasterizers = { PageRasterizer { _, dpi -> gate?.await(); ImageIoRaster(dpi) } },
        files = files,
        documents = documents,
        pages = pages,
        clock = clock,
        scope = backgroundScope,
        processing = StandardTestDispatcher(testScheduler),
    )

    private suspend fun document(title: String = "Marksheet", ready: Int = 3, importing: Int = 0): DocumentId {
        val doc = documents.create(title)
        val all = (0 until ready + importing).map {
            Page(PageId("p$it"), doc.id, it, "s$it", status = if (it < ready) PageStatus.READY else PageStatus.IMPORTING)
        }
        pages.insertAll(all)
        return doc.id
    }

    private fun outDir(id: DocumentId) = files.outputFile(id, "x").parentFile!!

    @Test fun exportsReadyPagesToAPdfAndCleansUp() = runTest {
        val engine = coordinator()
        val id = document(ready = 3, importing = 1)

        engine.start(id, ExportSettings())
        runCurrent()

        val state = engine.states.value[id] as ExportState.Succeeded
        val pdf = File(state.result.paths.single())
        assertThat(pdf.name).isEqualTo("Marksheet.pdf")
        Loader.loadPDF(pdf).use { assertThat(it.numberOfPages).isEqualTo(3) }
        assertThat(state.result.size.bytes).isEqualTo(pdf.length())
        assertThat(documents.get(id)!!.lastExport).isEqualTo(state.result)
        assertThat(documents.get(id)!!.exportInterrupted).isFalse()
        assertThat(File(files.root, "work").listFiles().orEmpty().filter { it.isFile }).isEmpty()
    }

    @Test fun meetsATargetSize() = runTest {
        val engine = coordinator()
        val id = document(ready = 4)
        engine.start(id, ExportSettings(target = ByteSize.kb(150)))
        runCurrent()
        val state = engine.states.value[id] as ExportState.Succeeded
        assertThat(state.result.size.bytes).isAtMost(150_000)
        assertThat(state.result.targetMet).isTrue()
    }

    @Test fun impossibleTargetIsReportedNotHidden() = runTest {
        val engine = coordinator()
        val id = document(ready = 4)
        engine.start(id, ExportSettings(target = ByteSize(2_000)))
        runCurrent()
        val state = engine.states.value[id] as ExportState.TargetMissed
        assertThat(state.smallest.bytes).isGreaterThan(2_000)
        assertThat(File(state.result.paths.single()).exists()).isTrue() // "Keep this file" stays possible
    }

    @Test fun jpgExportNamesFilesPerPageAndReplacesThePreviousPdf() = runTest {
        val engine = coordinator()
        val id = document(title = "Fees", ready = 2)
        engine.start(id, ExportSettings())
        runCurrent()
        engine.acknowledge(id)

        engine.start(id, ExportSettings(format = ExportFormat.JPG))
        runCurrent()

        val state = engine.states.value[id] as ExportState.Succeeded
        assertThat(state.result.paths.map { File(it).name }).containsExactly("Fees_01.jpg", "Fees_02.jpg").inOrder()
        assertThat(outDir(id).list()!!.toList()).containsExactly("Fees_01.jpg", "Fees_02.jpg")
    }

    /** F-Droid review of 1.0.0: the JPG limit is per image; the total of all images must not be compared with it. */
    @Test fun jpgTargetIsPerImage() = runTest {
        val engine = coordinator()
        val id = document(ready = 4)
        val target = ByteSize.kb(60)
        engine.start(id, ExportSettings(format = ExportFormat.JPG, target = target))
        runCurrent()

        val state = engine.states.value[id] as ExportState.Succeeded
        val sizes = state.result.paths.map { File(it).length() }
        assertThat(sizes.all { it <= target.bytes }).isTrue()
        assertThat(sizes.sum()).isGreaterThan(target.bytes) // the case that used to be reported as missed
        assertThat(state.result.targetMet).isTrue()
    }

    @Test fun jpgMissReportsTheLargestImage() = runTest {
        val engine = coordinator()
        val id = document(ready = 3)
        engine.start(id, ExportSettings(format = ExportFormat.JPG, target = ByteSize(2_000)))
        runCurrent()

        val state = engine.states.value[id] as ExportState.TargetMissed
        assertThat(state.smallest.bytes).isEqualTo(state.result.paths.maxOf { File(it).length() })
        assertThat(state.result.targetMet).isFalse()
    }

    @Test fun cancelKeepsThePreviousExportAndClearsTheRunningFlag() = runTest {
        val engine = coordinator()
        val id = document(ready = 2)
        engine.start(id, ExportSettings())
        runCurrent()
        val previous = File((engine.states.value[id] as ExportState.Succeeded).result.paths.single())
        engine.acknowledge(id)

        gate = CompletableDeferred()
        engine.start(id, ExportSettings())
        runCurrent()
        assertThat(documents.get(id)!!.exportInterrupted).isTrue() // in progress
        engine.cancel(id)
        runCurrent()

        assertThat(engine.states.value[id]).isEqualTo(ExportState.Cancelled)
        assertThat(previous.exists()).isTrue()
        assertThat(documents.get(id)!!.exportInterrupted).isFalse()
    }

    @Test fun lowStorageFailsBeforeDoingAnyWork() = runTest {
        val engine = coordinator()
        val id = document()
        files.free = 1_000
        engine.start(id, ExportSettings())
        runCurrent()
        val failed = engine.states.value[id] as ExportState.Failed
        assertThat(failed.error).isInstanceOf(FolioError.LowStorage::class.java)
        assertThat((failed.error as FolioError.LowStorage).shortBy.bytes).isGreaterThan(0L)
        assertThat(outDir(id).exists()).isFalse()
    }

    @Test fun nothingReadyToExportFails() = runTest {
        val engine = coordinator()
        val id = document(ready = 0, importing = 2)
        engine.start(id, ExportSettings())
        runCurrent()
        assertThat(engine.states.value[id]).isEqualTo(ExportState.Failed(FolioError.NothingToExport))
    }
}

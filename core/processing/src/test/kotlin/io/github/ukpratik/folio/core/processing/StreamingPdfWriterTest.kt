// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing

import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.model.Margin
import io.github.ukpratik.folio.core.model.Orientation
import io.github.ukpratik.folio.core.model.PageSize
import io.github.ukpratik.folio.core.processing.pdf.JpegInfo
import io.github.ukpratik.folio.core.processing.pdf.PageGeometry
import io.github.ukpratik.folio.core.processing.pdf.PdfPage
import io.github.ukpratik.folio.core.processing.pdf.StreamingPdfWriter
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.ZoneOffset
import java.time.ZonedDateTime
import javax.imageio.ImageIO
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Golden checks: our PDFs parsed by Apache PDFBox (ADR-0010). */
class StreamingPdfWriterTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun jpeg(width: Int, height: Int, gray: Boolean = false): File {
        val image = BufferedImage(width, height, if (gray) BufferedImage.TYPE_BYTE_GRAY else BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = java.awt.Color.WHITE
        g.fillRect(0, 0, width, height)
        g.color = java.awt.Color.DARK_GRAY
        g.fillRect(width / 4, height / 4, width / 2, height / 8)
        g.dispose()
        return tmp.newFile().apply { ImageIO.write(image, "jpg", this) }
    }

    private fun page(file: File, size: PageSize = PageSize.A4, orientation: Orientation = Orientation.AUTO): PdfPage {
        val info = JpegInfo.parse(file.readBytes())
        return PdfPage(PageGeometry.layout(info.width, info.height, 200, size, orientation, Margin.NONE), file)
    }

    private fun write(title: String, pages: List<PdfPage>): ByteArray = ByteArrayOutputStream().also {
        StreamingPdfWriter().write(it, title, pages, ZonedDateTime.of(2026, 10, 7, 14, 30, 0, 0, ZoneOffset.ofHoursMinutes(5, 30)))
    }.toByteArray().also(::keepForQpdf)

    /** CI runs `qpdf --check` over everything written here (a second, independent PDF parser). */
    private fun keepForQpdf(pdf: ByteArray) {
        File("build/golden-pdfs").apply { mkdirs() }.resolve("pdf-${System.nanoTime()}.pdf").writeBytes(pdf)
    }

    @Test fun jpegInfoReadsFrameHeader() {
        assertThat(JpegInfo.parse(jpeg(320, 200).readBytes())).isEqualTo(JpegInfo(320, 200, 3))
        assertThat(JpegInfo.parse(jpeg(64, 48, gray = true).readBytes()).components).isEqualTo(1)
    }

    @Test fun pagesSizesOrderAndImagesSurviveARoundTrip() {
        val portrait = jpeg(1654, 2339)
        val landscape = jpeg(2339, 1654)
        val bytes = write("Folio_20261007_1430", listOf(page(portrait), page(landscape), page(portrait, PageSize.LETTER)))

        Loader.loadPDF(bytes).use { doc ->
            assertThat(doc.numberOfPages).isEqualTo(3)
            val a4 = doc.getPage(0).mediaBox
            assertThat(a4.width).isWithin(0.1f).of(595.28f)
            assertThat(a4.height).isWithin(0.1f).of(841.89f)
            val turned = doc.getPage(1).mediaBox
            assertThat(turned.width).isGreaterThan(turned.height) // Auto orientation → landscape page
            assertThat(doc.getPage(2).mediaBox.width).isWithin(0.1f).of(612f)

            val image = doc.getPage(1).resources.getXObject(org.apache.pdfbox.cos.COSName.getPDFName("Im0")) as PDImageXObject
            assertThat(image.width).isEqualTo(2339)
            assertThat(image.colorSpace.name).isEqualTo("DeviceRGB")

            assertThat(doc.documentInformation.title).isEqualTo("Folio_20261007_1430")
            assertThat(doc.documentInformation.producer).isEqualTo("Folio: Doc Maker")
            assertThat(doc.documentInformation.author).isNull() // FR-32: nothing else
        }
    }

    @Test fun grayscaleJpegUsesDeviceGray() {
        Loader.loadPDF(write("g", listOf(page(jpeg(100, 140, gray = true))))).use { doc ->
            val image = doc.getPage(0).resources.getXObject(org.apache.pdfbox.cos.COSName.getPDFName("Im0")) as PDImageXObject
            assertThat(image.colorSpace.name).isEqualTo("DeviceGray")
        }
    }

    @Test fun nonAsciiTitlesAreEncodedAsUtf16() {
        Loader.loadPDF(write("मार्कशीट (2026)", listOf(page(jpeg(100, 140))))).use { doc ->
            assertThat(doc.documentInformation.title).isEqualTo("मार्कशीट (2026)")
        }
    }

    @Test fun sizeIsJpegsPlusSmallOverhead() {
        val files = List(5) { jpeg(800, 1100) }
        val bytes = write("Folio_20261007_1430", files.map(::page))
        val jpegs = files.sumOf { it.length() }
        assertThat(bytes.size - jpegs).isAtMost(StreamingPdfWriter.overheadBytes(5, 20))
    }
}

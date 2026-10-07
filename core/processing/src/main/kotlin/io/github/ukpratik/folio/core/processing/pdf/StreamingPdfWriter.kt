// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.pdf

import java.io.File
import java.io.FilterOutputStream
import java.io.OutputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** One page: where to draw, and the JPEG to draw (embedded as-is with /DCTDecode — no re-encode). */
data class PdfPage(val spec: PdfPageSpec, val jpeg: File)

/**
 * A minimal PDF 1.4 writer (ADR-0010): one image XObject + one content stream per page, written straight to
 * the output stream, so memory stays flat for 100 pages and the final size ≈ Σ JPEG sizes + a small overhead.
 * Metadata is limited to Title, Producer and CreationDate (FR-32).
 */
class StreamingPdfWriter {

    fun write(out: OutputStream, title: String, pages: List<PdfPage>, created: ZonedDateTime = ZonedDateTime.now()) {
        require(pages.isNotEmpty()) { "A PDF needs at least one page" }
        val pdf = CountingStream(out)
        // Object numbers: 1 catalog, 2 page tree, 3 info, then 3 per page (image, content, page).
        val objectCount = 3 + pages.size * 3
        val offsets = LongArray(objectCount + 1)
        fun pageObjects(i: Int) = Triple(4 + i * 3, 5 + i * 3, 6 + i * 3)

        pdf.ascii("%PDF-1.4\n")
        pdf.write(byteArrayOf('%'.code.toByte(), 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), '\n'.code.toByte()))

        pages.forEachIndexed { i, page ->
            val (imageObj, contentObj, pageObj) = pageObjects(i)
            val jpeg = page.jpeg.readBytes()
            val info = JpegInfo.parse(jpeg)
            val colorSpace = when (info.components) {
                1 -> "/DeviceGray"
                4 -> "/DeviceCMYK"
                else -> "/DeviceRGB"
            }

            offsets[imageObj] = pdf.count
            pdf.ascii(
                "$imageObj 0 obj\n<< /Type /XObject /Subtype /Image /Width ${info.width} /Height ${info.height} " +
                    "/ColorSpace $colorSpace /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpeg.size} >>\nstream\n",
            )
            pdf.write(jpeg)
            pdf.ascii("\nendstream\nendobj\n")

            val s = page.spec
            val content = "q\n${num(s.drawWidth)} 0 0 ${num(s.drawHeight)} ${num(s.drawX)} ${num(s.drawY)} cm\n/Im0 Do\nQ\n"
            offsets[contentObj] = pdf.count
            pdf.ascii("$contentObj 0 obj\n<< /Length ${content.length} >>\nstream\n${content}endstream\nendobj\n")

            offsets[pageObj] = pdf.count
            pdf.ascii(
                "$pageObj 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${num(s.pageWidthPt)} ${num(s.pageHeightPt)}] " +
                    "/Resources << /XObject << /Im0 $imageObj 0 R >> /ProcSet [/PDF /ImageC /ImageB] >> " +
                    "/Contents $contentObj 0 R >>\nendobj\n",
            )
        }

        offsets[2] = pdf.count
        val kids = pages.indices.joinToString(" ") { "${pageObjects(it).third} 0 R" }
        pdf.ascii("2 0 obj\n<< /Type /Pages /Kids [$kids] /Count ${pages.size} >>\nendobj\n")

        offsets[1] = pdf.count
        pdf.ascii("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

        offsets[3] = pdf.count
        pdf.ascii("3 0 obj\n<< /Title ${pdfString(title)} /Producer (Folio: Doc Maker) /CreationDate (${pdfDate(created)}) >>\nendobj\n")

        val xref = pdf.count
        pdf.ascii("xref\n0 ${objectCount + 1}\n0000000000 65535 f \n")
        for (n in 1..objectCount) pdf.ascii(String.format(Locale.ROOT, "%010d 00000 n \n", offsets[n]))
        pdf.ascii("trailer\n<< /Size ${objectCount + 1} /Root 1 0 R /Info 3 0 R >>\nstartxref\n$xref\n%%EOF\n")
        pdf.flush()
    }

    private class CountingStream(out: OutputStream) : FilterOutputStream(out) {
        var count = 0L
            private set

        override fun write(b: Int) {
            out.write(b)
            count++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            out.write(b, off, len)
            count += len
        }

        fun ascii(text: String) = write(text.toByteArray(Charsets.US_ASCII))
    }

    companion object {
        /** Bytes the writer adds around the JPEGs; used by the size optimiser's budget (ADR-0011). */
        fun overheadBytes(pageCount: Int, titleLength: Int): Long = 600L + titleLength * 4 + pageCount * 520L

        internal fun num(v: Float): String = String.format(Locale.ROOT, "%.2f", v).trimEnd('0').trimEnd('.')

        /** ASCII titles as literal strings; anything else (e.g. Hindi) as UTF-16BE hex with a BOM. */
        internal fun pdfString(text: String): String =
            if (text.all { it.code in 32..126 }) {
                "(" + text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)") + ")"
            } else {
                "<FEFF" + text.toByteArray(Charsets.UTF_16BE).joinToString("") { String.format(Locale.ROOT, "%02X", it) } + ">"
            }

        internal fun pdfDate(t: ZonedDateTime): String {
            val base = t.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT))
            val offset = t.offset.totalSeconds / 60
            if (offset == 0) return "D:${base}Z"
            val sign = if (offset > 0) '+' else '-'
            return String.format(Locale.ROOT, "D:%s%c%02d'%02d'", base, sign, abs(offset) / 60, abs(offset) % 60)
        }
    }
}

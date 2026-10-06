// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.image

/** Formats Folio accepts (PRD §8). Detected from file bytes, never from the claimed MIME type (LLD §10). */
enum class ImageFormat { JPEG, PNG, WEBP, BMP, GIF, HEIF }

object ImageFormatSniffer {
    const val HEADER_BYTES = 16
    private val heifBrands = setOf("heic", "heix", "hevc", "hevx", "heim", "heis", "hevm", "hevs", "mif1", "msf1")

    fun sniff(header: ByteArray): ImageFormat? {
        fun at(i: Int) = if (i < header.size) header[i].toInt() and 0xFF else -1
        fun ascii(from: Int, len: Int) =
            if (from + len <= header.size) String(header, from, len, Charsets.US_ASCII) else ""
        return when {
            at(0) == 0xFF && at(1) == 0xD8 && at(2) == 0xFF -> ImageFormat.JPEG
            at(0) == 0x89 && ascii(1, 3) == "PNG" -> ImageFormat.PNG
            ascii(0, 4) == "RIFF" && ascii(8, 4) == "WEBP" -> ImageFormat.WEBP
            ascii(0, 2) == "BM" -> ImageFormat.BMP
            ascii(0, 4) == "GIF8" -> ImageFormat.GIF
            ascii(4, 4) == "ftyp" && ascii(8, 4) in heifBrands -> ImageFormat.HEIF
            else -> null
        }
    }
}

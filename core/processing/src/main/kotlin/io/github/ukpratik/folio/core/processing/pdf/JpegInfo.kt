// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.pdf

import java.io.IOException

/** What a PDF image XObject needs to know about an embedded JPEG (read from its SOF marker). */
data class JpegInfo(val width: Int, val height: Int, val components: Int) {
    companion object {
        private val SOF_MARKERS = setOf(0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF)

        fun parse(bytes: ByteArray): JpegInfo {
            fun u8(i: Int) = bytes[i].toInt() and 0xFF
            fun u16(i: Int) = (u8(i) shl 8) or u8(i + 1)
            if (bytes.size < 4 || u8(0) != 0xFF || u8(1) != 0xD8) throw IOException("Not a JPEG")
            var i = 2
            while (i + 4 < bytes.size) {
                if (u8(i) != 0xFF) throw IOException("Corrupt JPEG marker")
                val marker = u8(i + 1)
                if (marker == 0xFF) { i++; continue } // fill byte
                val length = u16(i + 2)
                if (marker in SOF_MARKERS) {
                    if (i + 9 >= bytes.size) break
                    return JpegInfo(width = u16(i + 7), height = u16(i + 5), components = u8(i + 9))
                }
                i += 2 + length
            }
            throw IOException("JPEG has no frame header")
        }
    }
}

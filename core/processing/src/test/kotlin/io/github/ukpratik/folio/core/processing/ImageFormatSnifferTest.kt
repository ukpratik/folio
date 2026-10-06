// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing

import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.processing.image.ImageFormat
import io.github.ukpratik.folio.core.processing.image.ImageFormatSniffer
import io.github.ukpratik.folio.core.processing.image.ImageNormalizer
import org.junit.Test

class ImageFormatSnifferTest {
    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }
    private fun ascii(text: String) = text.toByteArray(Charsets.US_ASCII)

    @Test fun detectsSupportedFormats() {
        assertThat(ImageFormatSniffer.sniff(bytes(0xFF, 0xD8, 0xFF, 0xE0))).isEqualTo(ImageFormat.JPEG)
        assertThat(ImageFormatSniffer.sniff(bytes(0x89) + ascii("PNG\r\n"))).isEqualTo(ImageFormat.PNG)
        assertThat(ImageFormatSniffer.sniff(ascii("RIFF") + bytes(0, 0, 0, 0) + ascii("WEBPVP8 "))).isEqualTo(ImageFormat.WEBP)
        assertThat(ImageFormatSniffer.sniff(ascii("BM") + bytes(0, 0))).isEqualTo(ImageFormat.BMP)
        assertThat(ImageFormatSniffer.sniff(ascii("GIF89a"))).isEqualTo(ImageFormat.GIF)
        assertThat(ImageFormatSniffer.sniff(bytes(0, 0, 0, 24) + ascii("ftypheic"))).isEqualTo(ImageFormat.HEIF)
    }

    @Test fun rejectsEverythingElse() {
        assertThat(ImageFormatSniffer.sniff(ascii("%PDF-1.4"))).isNull()
        assertThat(ImageFormatSniffer.sniff(ascii("hello world"))).isNull()
        assertThat(ImageFormatSniffer.sniff(ByteArray(0))).isNull()
        assertThat(ImageFormatSniffer.sniff(bytes(0, 0, 0, 24) + ascii("ftypavif"))).isNull()
    }

    @Test fun sampleSizeKeepsLongEdgeWithinCap() {
        assertThat(ImageNormalizer.sampleSizeFor(3000, 2000)).isEqualTo(1)
        assertThat(ImageNormalizer.sampleSizeFor(4000, 3000)).isEqualTo(2)
        assertThat(ImageNormalizer.sampleSizeFor(12000, 9000)).isEqualTo(4) // 108 MP → 3000×2250
    }
}

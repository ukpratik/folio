// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.processing.image.BitmapImageNormalizer
import io.github.ukpratik.folio.core.processing.image.UnsupportedImageException
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class BitmapImageNormalizerTest {
    @get:Rule val tmp = TemporaryFolder()
    private val normalizer = BitmapImageNormalizer(Dispatchers.Unconfined)

    private fun write(bitmap: Bitmap, format: Bitmap.CompressFormat, name: String): File =
        tmp.newFile(name).apply { outputStream().use { bitmap.compress(format, 100, it) } }

    private fun decode(bytes: ByteArray) = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    @Test fun writesJpegWithoutExifAndKeepsSize() = runTest {
        val input = write(Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }, Bitmap.CompressFormat.JPEG, "a.jpg")
        val out = ByteArrayOutputStream()

        val result = normalizer.normalize(input, out)

        assertThat(result.widthPx to result.heightPx).isEqualTo(400 to 300)
        val bytes = out.toByteArray()
        assertThat(bytes.take(3).map { it.toInt() and 0xFF }).containsExactly(0xFF, 0xD8, 0xFF).inOrder()
        val exif = ExifInterface(bytes.inputStream())
        assertThat(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED))
            .isAnyOf(ExifInterface.ORIENTATION_UNDEFINED, ExifInterface.ORIENTATION_NORMAL)
        assertThat(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)).isNull()
    }

    @Test fun appliesExifRotation() = runTest {
        val input = write(Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888), Bitmap.CompressFormat.JPEG, "r.jpg")
        ExifInterface(input).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            setAttribute(ExifInterface.TAG_GPS_LATITUDE, "12/1,58/1,0/1")
            saveAttributes()
        }
        val out = ByteArrayOutputStream()

        val result = normalizer.normalize(input, out)

        assertThat(result.widthPx to result.heightPx).isEqualTo(300 to 400)
        assertThat(ExifInterface(out.toByteArray().inputStream()).getAttribute(ExifInterface.TAG_GPS_LATITUDE)).isNull()
    }

    @Test fun flattensTransparentPngOntoWhite() = runTest {
        val input = write(Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.TRANSPARENT) }, Bitmap.CompressFormat.PNG, "t.png")
        val out = ByteArrayOutputStream()

        normalizer.normalize(input, out)

        val pixel = decode(out.toByteArray()).getPixel(25, 25)
        assertThat(Color.red(pixel)).isGreaterThan(245)
        assertThat(Color.green(pixel)).isGreaterThan(245)
        assertThat(Color.blue(pixel)).isGreaterThan(245)
    }

    @Test fun downsamplesHugeImages() = runTest {
        val input = write(Bitmap.createBitmap(7200, 100, Bitmap.Config.ARGB_8888), Bitmap.CompressFormat.PNG, "w.png")
        val result = normalizer.normalize(input, ByteArrayOutputStream())
        assertThat(result.widthPx).isAtMost(3508)
    }

    @Test fun rejectsNonImagesAndDamagedFiles() {
        val text = tmp.newFile("x.jpg").apply { writeText("not an image") }
        assertThrows(UnsupportedImageException::class.java) { runTest { normalizer.normalize(text, ByteArrayOutputStream()) } }

        val truncated = tmp.newFile("bad.jpg").apply { writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x00)) }
        assertThrows(UnsupportedImageException::class.java) { runTest { normalizer.normalize(truncated, ByteArrayOutputStream()) } }
    }
}

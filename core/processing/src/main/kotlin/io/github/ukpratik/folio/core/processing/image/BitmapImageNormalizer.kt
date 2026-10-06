// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import io.github.ukpratik.folio.core.domain.concurrency.ProcessingDispatcher
import java.io.File
import java.io.OutputStream
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal class BitmapImageNormalizer @Inject constructor(
    @ProcessingDispatcher private val dispatcher: CoroutineDispatcher,
) : ImageNormalizer {

    override suspend fun normalize(input: File, output: OutputStream): NormalizedImage = withContext(dispatcher) {
        val format = ImageFormatSniffer.sniff(input.readHeader()) ?: throw UnsupportedImageException("Unknown image format")

        val decoded = if (format == ImageFormat.HEIF) decodeHeif(input) else decodeWithExif(input, format)
        val flat = flattenOntoWhite(decoded)
        try {
            check(flat.compress(Bitmap.CompressFormat.JPEG, ImageNormalizer.SOURCE_JPEG_QUALITY, output)) { "JPEG encode failed" }
            NormalizedImage(flat.width, flat.height)
        } finally {
            flat.recycle()
        }
    }

    private fun decodeWithExif(input: File, format: ImageFormat): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(input.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw UnsupportedImageException("Damaged $format")

        val options = BitmapFactory.Options().apply {
            inSampleSize = ImageNormalizer.sampleSizeFor(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeFile(input.path, options) ?: throw UnsupportedImageException("Damaged $format")
        val orientation = if (format == ImageFormat.JPEG || format == ImageFormat.WEBP) {
            ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } else {
            ExifInterface.ORIENTATION_NORMAL
        }
        return applyOrientation(bitmap, orientation)
    }

    /** HEIC/HEIF needs ImageDecoder (Android 9+), which also applies the image's own rotation. */
    private fun decodeHeif(input: File): Bitmap {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) throw UnsupportedImageException("HEIC needs Android 9")
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(input)) { decoder, info, _ ->
            val longEdge = maxOf(info.size.width, info.size.height)
            if (longEdge > ImageNormalizer.MAX_LONG_EDGE_PX) {
                val scale = ImageNormalizer.MAX_LONG_EDGE_PX.toFloat() / longEdge
                decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = orientationMatrix(orientation) ?: return bitmap
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun flattenOntoWhite(bitmap: Bitmap): Bitmap {
        if (!bitmap.hasAlpha()) return bitmap
        val flat = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Canvas(flat).apply {
            drawColor(Color.WHITE)
            drawBitmap(bitmap, 0f, 0f, null)
        }
        bitmap.recycle()
        return flat
    }

    /** Reads up to [ImageFormatSniffer.HEADER_BYTES] bytes (InputStream.readNBytes needs API 33). */
    private fun File.readHeader(): ByteArray = inputStream().use { stream ->
        val header = ByteArray(ImageFormatSniffer.HEADER_BYTES)
        var filled = 0
        while (filled < header.size) {
            val read = stream.read(header, filled, header.size - filled)
            if (read < 0) break
            filled += read
        }
        header.copyOf(filled)
    }

    companion object {
        /** EXIF orientation → transform (same table as the EXIF spec; null = no change). */
        internal fun orientationMatrix(orientation: Int): Matrix? = when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Matrix().apply { setScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_180 -> Matrix().apply { setRotate(180f) }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> Matrix().apply { setRotate(180f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSPOSE -> Matrix().apply { setRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> Matrix().apply { setRotate(90f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> Matrix().apply { setRotate(-90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> Matrix().apply { setRotate(-90f) }
            else -> null
        }
    }
}

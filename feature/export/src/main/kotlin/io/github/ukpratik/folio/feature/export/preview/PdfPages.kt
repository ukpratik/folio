// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.export.preview

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Renders pages of an exported PDF with the platform [PdfRenderer] (S7d). PdfRenderer allows one open page
 * at a time and isn't thread-safe, so every call holds [lock]; renders run on the IO dispatcher.
 */
internal class PdfPages private constructor(private val fd: ParcelFileDescriptor, private val renderer: PdfRenderer) {
    private val lock = Any()
    private var closed = false

    val pageCount: Int = renderer.pageCount

    /** Width / height of each page, read once up front so the list can lay out before anything renders. */
    val aspectRatios: List<Float> = List(pageCount) { i -> renderer.openPage(i).use { it.width.toFloat() / it.height } }

    suspend fun render(index: Int, widthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        synchronized(lock) {
            if (closed) return@withContext null
            renderer.openPage(index).use { page ->
                val width = widthPx.coerceIn(1, MAX_WIDTH_PX)
                val height = (width.toLong() * page.height / page.width).toInt().coerceAtLeast(1)
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(Color.WHITE) // PDF pages render onto transparency
                    page.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
            }
        }
    }

    fun close() = synchronized(lock) {
        if (!closed) {
            closed = true
            renderer.close()
            fd.close()
        }
    }

    companion object {
        /** Keeps one rendered page well under 10 MB. */
        const val MAX_WIDTH_PX = 1440

        suspend fun open(file: File): PdfPages? = withContext(Dispatchers.IO) {
            try {
                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                PdfPages(fd, PdfRenderer(fd))
            } catch (e: IOException) {
                Timber.w(e, "Can't open PDF for preview")
                null
            } catch (e: SecurityException) {
                Timber.w(e, "Can't open PDF for preview")
                null
            }
        }
    }
}

/** Opens [path] for this composition and closes it when it leaves. Null while opening or if it can't be read. */
@Composable
internal fun rememberPdfPages(path: String?): PdfPages? {
    val pages by produceState<PdfPages?>(null, path) {
        value = path?.let { PdfPages.open(File(it)) }
    }
    DisposableEffect(pages) {
        val opened = pages
        onDispose { opened?.close() }
    }
    return pages
}

/** One page, laid out at its own aspect ratio; white until rendered. */
@Composable
internal fun PdfPageImage(pages: PdfPages, index: Int, widthPx: Int, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val bitmap by produceState<Bitmap?>(null, pages, index, widthPx) { value = pages.render(index, widthPx) }
    Box(modifier.aspectRatio(pages.aspectRatios[index]).background(androidx.compose.ui.graphics.Color.White)) {
        bitmap?.let {
            Image(it.asImageBitmap(), contentDescription, contentScale = ContentScale.Fit)
        }
    }
}

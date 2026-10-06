// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import io.github.ukpratik.folio.core.processing.detect.Px

/** Test fixtures drawn in code: no binary files, exact known geometry. */
object SyntheticImages {
    /** A tilted white page with "text" lines on a dark table, like T01/T10 in the QA set. */
    fun documentPhoto(width: Int, height: Int, corners: List<Px>, table: Int = Color.rgb(59, 52, 48)): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(table)
        val page = Path().apply {
            moveTo(corners[0].x.toFloat(), corners[0].y.toFloat())
            corners.drop(1).forEach { lineTo(it.x.toFloat(), it.y.toFloat()) }
            close()
        }
        canvas.drawPath(page, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(244, 242, 236) })
        // Text lines with margins, like a real page: inset from the page's own left/right edges at each row.
        val ink = Paint().apply { color = Color.rgb(60, 60, 60); strokeWidth = 6f }
        fun edgeX(a: Px, b: Px, y: Double) = a.x + (b.x - a.x) * (y - a.y) / (b.y - a.y)
        val top = maxOf(corners[0].y, corners[1].y) + 80
        val bottom = minOf(corners[2].y, corners[3].y) - 80
        var y = top
        while (y < bottom) {
            val left = edgeX(corners[0], corners[3], y) + 60
            val right = edgeX(corners[1], corners[2], y) - 60
            canvas.drawLine(left.toFloat(), y.toFloat(), right.toFloat(), y.toFloat(), ink)
            y += 40
        }
        return bmp
    }

    /** A flat screenshot-like image: content right up to the edges, no document border. */
    fun screenshot(width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        val ink = Paint().apply { color = Color.DKGRAY; strokeWidth = 5f }
        for (y in 20 until height step 36) canvas.drawLine(10f, y.toFloat(), width - 10f, y.toFloat(), ink)
        return bmp
    }

    /** A soft gradient with no edges at all (like a blurry non-document photo). */
    fun gradient(width: Int, height: Int): Bitmap {
        val pixels = IntArray(width * height) { i ->
            val v = (i % width) * 255 / width
            Color.rgb(v, 120, 255 - v)
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    /** Low-contrast grey page: everything between 100 and 150. */
    fun washedOut(width: Int, height: Int): Bitmap {
        val pixels = IntArray(width * height) { i -> if ((i / width) % 20 < 4) Color.rgb(100, 100, 100) else Color.rgb(150, 150, 150) }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
}

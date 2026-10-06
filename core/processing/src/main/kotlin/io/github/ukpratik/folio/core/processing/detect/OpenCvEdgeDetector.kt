// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.detect

import android.graphics.Bitmap
import io.github.ukpratik.folio.core.processing.opencv.MatScope
import io.github.ukpratik.folio.core.processing.opencv.OpenCvRuntime
import io.github.ukpratik.folio.core.processing.opencv.withMats
import javax.inject.Inject
import kotlin.math.max
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * Contour-based document detection (LLD §5.3): blur → Canny (median thresholds) → dilate → external contours
 * → the largest convex 4-point polygon that passes [QuadGeometry.isConfident]. Falls back to an Otsu threshold
 * when edges are weak (light page on a light surface).
 */
internal class OpenCvEdgeDetector @Inject constructor(private val runtime: OpenCvRuntime) : EdgeDetector {

    override fun detect(gray: GrayImage): Detection? {
        runtime.ensureLoaded()
        return withMats {
            val mat = track(Mat(gray.height, gray.width, CvType.CV_8UC1))
            if (gray.rowStride == gray.width) {
                mat.put(0, 0, gray.pixels)
            } else {
                for (row in 0 until gray.height) mat.put(row, 0, gray.pixels.copyOfRange(row * gray.rowStride, row * gray.rowStride + gray.width))
            }
            detectGray(mat)
        }
    }

    override fun detect(bitmap: Bitmap): Detection? {
        runtime.ensureLoaded()
        return withMats {
            val rgba = track(Mat())
            Utils.bitmapToMat(bitmap, rgba)
            val gray = track(Mat())
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            detectGray(gray)
        }
    }

    private fun MatScope.detectGray(input: Mat): Detection? {
        val scale = MAX_EDGE.toDouble() / max(input.cols(), input.rows())
        val gray = if (scale < 1.0) {
            track(Mat()).also { Imgproc.resize(input, it, Size(input.cols() * scale, input.rows() * scale), 0.0, 0.0, Imgproc.INTER_AREA) }
        } else {
            input
        }
        val blurred = track(Mat())
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)

        val median = median(blurred)
        val kernel = track(Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0)))
        fun edges(low: Double, high: Double): Mat = track(Mat()).also {
            Imgproc.Canny(blurred, it, low, high)
            Imgproc.dilate(it, it, kernel)
        }
        // Cheapest first: adaptive Canny, then a low fixed Canny (light page on a light surface), then Otsu.
        val passes = sequence<() -> Mat> {
            yield { edges(0.66 * median, 1.33 * median) }
            yield { edges(LOW_CANNY, 3 * LOW_CANNY) }
            yield {
                track(Mat()).also { Imgproc.threshold(blurred, it, 0.0, 255.0, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU) }
            }
        }
        val corners = passes.firstNotNullOfOrNull { pass -> bestQuad(pass(), gray.cols(), gray.rows()) } ?: return null

        val confidence = (QuadGeometry.area(corners) / (gray.cols().toDouble() * gray.rows())).toFloat()
        return Detection(QuadGeometry.toQuad(corners, gray.cols(), gray.rows()), confidence)
    }

    private fun MatScope.bestQuad(binary: Mat, width: Int, height: Int): List<Px>? {
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = track(Mat())
        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        contours.forEach { track(it) }

        for (contour in contours.sortedByDescending { Imgproc.contourArea(it) }.take(CANDIDATES)) {
            val curve = track(MatOfPoint2f(*contour.toArray()))
            val approx = track(MatOfPoint2f())
            Imgproc.approxPolyDP(curve, approx, APPROX_EPSILON * Imgproc.arcLength(curve, true), true)
            val points = approx.toArray()
            if (points.size != 4 || !Imgproc.isContourConvex(track(MatOfPoint(*points)))) continue
            val ordered = QuadGeometry.order(points.map { Px(it.x, it.y) })
            if (QuadGeometry.isConfident(ordered, width, height)) return ordered
        }
        return null
    }

    private fun MatScope.median(gray: Mat): Double {
        val hist = track(Mat())
        Imgproc.calcHist(listOf(gray), MatOfInt(0), track(Mat()), hist, MatOfInt(256), MatOfFloat(0f, 256f), false)
        val half = gray.total() / 2.0
        var cumulative = 0.0
        for (bin in 0 until 256) {
            cumulative += hist.get(bin, 0)[0]
            if (cumulative >= half) return bin.toDouble().coerceAtLeast(MIN_MEDIAN)
        }
        return 128.0
    }

    private companion object {
        const val MAX_EDGE = 1024
        const val CANDIDATES = 5
        const val APPROX_EPSILON = 0.02
        /** Keeps Canny thresholds sensible on very dark frames. */
        const val MIN_MEDIAN = 20.0
        const val LOW_CANNY = 30.0
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import io.github.ukpratik.folio.core.domain.concurrency.ProcessingDispatcher
import io.github.ukpratik.folio.core.model.Quad
import io.github.ukpratik.folio.core.model.Rotation
import io.github.ukpratik.folio.core.processing.detect.QuadGeometry
import io.github.ukpratik.folio.core.processing.enhance.ImageEnhancer
import io.github.ukpratik.folio.core.processing.opencv.MatScope
import io.github.ukpratik.folio.core.processing.opencv.OpenCvRuntime
import io.github.ukpratik.folio.core.processing.opencv.withMats
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * LLD §5.4. Holds at most one decoded base bitmap plus a few OpenCV Mats at a time.
 * Previews reuse decoded bases from a small LRU cache; export renders always decode fresh.
 */
@Singleton
internal class OpenCvPageRenderer @Inject constructor(
    private val runtime: OpenCvRuntime,
    private val enhancer: ImageEnhancer,
    @ProcessingDispatcher private val dispatcher: CoroutineDispatcher,
) : PageRenderer {

    private val previewBases = object : LruCache<String, Bitmap>(PREVIEW_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    override suspend fun render(request: RenderRequest): Bitmap = withContext(dispatcher) {
        runtime.ensureLoaded()
        require(request.targetLongEdgePx > 0)
        val (base, cached) = decodeBase(request)
        withMats {
            val rgba = track(Mat())
            Utils.bitmapToMat(base, rgba)
            if (!cached) base.recycle()
            var image = track(Mat())
            Imgproc.cvtColor(rgba, image, Imgproc.COLOR_RGBA2RGB)

            request.page.corners?.let { image = warp(image, it) }
            image = rotate(image, request.page.rotation)
            image = track(enhancer.enhance(image, request.page.mode, request.page.adjustments))
            image = scaleDown(image, request.targetLongEdgePx)
            toBitmap(image)
        }
    }

    /**
     * Smallest decode whose *cropped* area still has at least [RenderRequest.targetLongEdgePx] pixels on its long
     * edge, so cropped pages aren't under-resolved. Previews check the cache before touching the file.
     */
    private fun decodeBase(request: RenderRequest): Pair<Bitmap, Boolean> {
        val file = request.sourceFile
        val key = "${file.path}@${request.page.corners}@${request.targetLongEdgePx}"
        if (!request.forExport) previewBases.get(key)?.let { return it to true }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) throw FileNotFoundException("Unreadable source")
        val usefulLongEdge = request.page.corners
            ?.let { QuadGeometry.outputSize(QuadGeometry.fromQuad(it, bounds.outWidth, bounds.outHeight)) }
            ?.let { (w, h) -> max(w, h) }
            ?: max(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (usefulLongEdge / (sample * 2) >= request.targetLongEdgePx) sample *= 2

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeFile(file.path, options) ?: throw FileNotFoundException("Unreadable source")
        if (request.forExport) return bitmap to false
        previewBases.put(key, bitmap)
        return bitmap to true
    }

    private fun MatScope.warp(src: Mat, quad: Quad): Mat {
        val corners = QuadGeometry.fromQuad(quad, src.cols(), src.rows())
        val (w, h) = QuadGeometry.outputSize(corners)
        val from = track(MatOfPoint2f(*corners.map { Point(it.x, it.y) }.toTypedArray()))
        val to = track(MatOfPoint2f(Point(0.0, 0.0), Point(w - 1.0, 0.0), Point(w - 1.0, h - 1.0), Point(0.0, h - 1.0)))
        val transform = track(Imgproc.getPerspectiveTransform(from, to))
        val out = track(Mat())
        Imgproc.warpPerspective(src, out, transform, Size(w.toDouble(), h.toDouble()), Imgproc.INTER_LINEAR, Core.BORDER_REPLICATE, Scalar(0.0))
        return out
    }

    private fun MatScope.rotate(src: Mat, rotation: Rotation): Mat {
        val code = when (rotation) {
            Rotation.R0 -> return src
            Rotation.R90 -> Core.ROTATE_90_CLOCKWISE
            Rotation.R180 -> Core.ROTATE_180
            Rotation.R270 -> Core.ROTATE_90_COUNTERCLOCKWISE
        }
        return track(Mat()).also { Core.rotate(src, it, code) }
    }

    private fun MatScope.scaleDown(src: Mat, targetLongEdge: Int): Mat {
        val longEdge = max(src.cols(), src.rows())
        if (longEdge <= targetLongEdge) return src
        val scale = targetLongEdge.toDouble() / longEdge
        return track(Mat()).also {
            Imgproc.resize(src, it, Size(src.cols() * scale, src.rows() * scale), 0.0, 0.0, Imgproc.INTER_AREA)
        }
    }

    private fun toBitmap(mat: Mat): Bitmap {
        check(mat.type() == CvType.CV_8UC1 || mat.type() == CvType.CV_8UC3) { "Unexpected Mat type ${mat.type()}" }
        return Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888).also { Utils.matToBitmap(mat, it) }
    }

    private companion object {
        /** About 8 decoded previews at ~1080 px (LLD §9). */
        const val PREVIEW_CACHE_BYTES = 48 * 1024 * 1024
    }
}

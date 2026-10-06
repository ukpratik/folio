// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.enhance

import io.github.ukpratik.folio.core.model.Adjustments
import io.github.ukpratik.folio.core.model.EnhancementMode
import io.github.ukpratik.folio.core.processing.opencv.MatScope
import io.github.ukpratik.folio.core.processing.opencv.OpenCvRuntime
import io.github.ukpratik.folio.core.processing.opencv.withMats
import javax.inject.Inject
import kotlin.math.max
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfInt
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/** Enhancement modes per LLD §5.5. AUTO must keep colours natural (certificates, stamps). */
internal class OpenCvEnhancer @Inject constructor(private val runtime: OpenCvRuntime) : ImageEnhancer {

    override fun enhance(rgb: Mat, mode: EnhancementMode, adjustments: Adjustments): Mat {
        runtime.ensureLoaded()
        return withMats {
            val base = when (mode) {
                EnhancementMode.ORIGINAL -> track(rgb.clone())
                EnhancementMode.AUTO -> auto(rgb)
                EnhancementMode.GRAYSCALE -> grayscale(rgb)
                EnhancementMode.BW -> blackAndWhite(rgb)
            }
            if (!LinearAdjust.isIdentity(adjustments)) {
                base.convertTo(base, -1, LinearAdjust.alpha(adjustments), LinearAdjust.beta(adjustments))
            }
            keep(base)
        }
    }

    /** Gray-world white balance → per-channel 1–99 % stretch → mild unsharp mask. */
    private fun MatScope.auto(rgb: Mat): Mat {
        val channels = mutableListOf<Mat>()
        Core.split(rgb, channels)
        channels.forEach { track(it) }
        val means = channels.map { Core.mean(it).`val`[0].coerceAtLeast(1.0) }
        val gray = means.average()
        channels.forEachIndexed { i, ch ->
            ch.convertTo(ch, -1, (gray / means[i]).coerceIn(0.7, 1.4), 0.0)
            stretch(ch)
        }
        val balanced = track(Mat())
        Core.merge(channels, balanced)
        return sharpen(balanced)
    }

    private fun MatScope.grayscale(rgb: Mat): Mat {
        val gray = track(Mat())
        Imgproc.cvtColor(rgb, gray, Imgproc.COLOR_RGB2GRAY)
        stretch(gray)
        return gray
    }

    /** Adaptive threshold handles uneven lighting across a photographed page. */
    private fun MatScope.blackAndWhite(rgb: Mat): Mat {
        val gray = track(Mat())
        Imgproc.cvtColor(rgb, gray, Imgproc.COLOR_RGB2GRAY)
        Imgproc.medianBlur(gray, gray, 3)
        val block = max(15, gray.cols() / 30) or 1 // odd
        val bw = track(Mat())
        Imgproc.adaptiveThreshold(gray, bw, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, block, 10.0)
        return bw
    }

    /** Linear stretch so the 1st percentile → 0 and the 99th → 255. Histogram from a 4× downsample for speed. */
    private fun MatScope.stretch(channel: Mat) {
        val small = track(Mat())
        Imgproc.resize(channel, small, Size(channel.cols() / 4.0 + 1, channel.rows() / 4.0 + 1), 0.0, 0.0, Imgproc.INTER_AREA)
        val hist = track(Mat())
        Imgproc.calcHist(listOf(small), MatOfInt(0), track(Mat()), hist, MatOfInt(256), MatOfFloat(0f, 256f), false)
        val total = small.total().toDouble()
        val low = percentile(hist, total, 0.01)
        val high = percentile(hist, total, 0.99)
        if (high - low < MIN_RANGE) return
        val alpha = 255.0 / (high - low)
        channel.convertTo(channel, -1, alpha, -low * alpha)
    }

    private fun percentile(hist: Mat, total: Double, fraction: Double): Double {
        var cumulative = 0.0
        for (bin in 0 until 256) {
            cumulative += hist.get(bin, 0)[0]
            if (cumulative >= fraction * total) return bin.toDouble()
        }
        return 255.0
    }

    private fun MatScope.sharpen(src: Mat): Mat {
        val blur = track(Mat())
        Imgproc.GaussianBlur(src, blur, Size(0.0, 0.0), 1.0)
        val out = track(Mat())
        Core.addWeighted(src, 1.5, blur, -0.5, 0.0, out)
        return out
    }

    private companion object {
        /** Skip the stretch for near-uniform channels (it would amplify noise). */
        const val MIN_RANGE = 16.0
    }
}

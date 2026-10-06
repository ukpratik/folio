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

    /**
     * Document-aware auto (FR-12, "keep colours natural"):
     * 1. White balance from the *paper*: the brightest 10 % of pixels should be neutral. Gains are clamped so a
     *    genuinely cream page or a coloured certificate is only nudged, never recoloured.
     * 2. One contrast stretch (1st–99th luminance percentile) applied identically to all channels, which
     *    preserves hue. A per-channel stretch would turn a blue stamp black.
     * 3. A mild unsharp mask.
     */
    private fun MatScope.auto(rgb: Mat): Mat {
        val gray = track(Mat())
        Imgproc.cvtColor(rgb, gray, Imgproc.COLOR_RGB2GRAY)
        val (low, high) = percentiles(gray, 0.01, 0.99)
        val paperThreshold = percentiles(gray, PAPER_PERCENTILE, PAPER_PERCENTILE).first

        val paperMask = track(Mat())
        Imgproc.threshold(gray, paperMask, paperThreshold - 1, 255.0, Imgproc.THRESH_BINARY)
        val channels = mutableListOf<Mat>()
        Core.split(rgb, channels)
        channels.forEach { track(it) }
        val paper = channels.map { Core.mean(it, paperMask).`val`[0].coerceAtLeast(1.0) }
        val neutral = paper.average()
        channels.forEachIndexed { i, ch -> ch.convertTo(ch, -1, (neutral / paper[i]).coerceIn(MIN_GAIN, MAX_GAIN), 0.0) }
        val balanced = track(Mat())
        Core.merge(channels, balanced)

        if (high - low >= MIN_RANGE) {
            val alpha = 255.0 / (high - low)
            balanced.convertTo(balanced, -1, alpha, -low * alpha)
        }
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

    /** Linear stretch so the 1st percentile → 0 and the 99th → 255 (single-channel images). */
    private fun MatScope.stretch(channel: Mat) {
        val (low, high) = percentiles(channel, 0.01, 0.99)
        if (high - low < MIN_RANGE) return
        val alpha = 255.0 / (high - low)
        channel.convertTo(channel, -1, alpha, -low * alpha)
    }

    /** Two percentiles of a single-channel image, from a 4× downsampled histogram for speed. */
    private fun MatScope.percentiles(channel: Mat, lowFraction: Double, highFraction: Double): Pair<Double, Double> {
        val small = track(Mat())
        Imgproc.resize(channel, small, Size(channel.cols() / 4.0 + 1, channel.rows() / 4.0 + 1), 0.0, 0.0, Imgproc.INTER_AREA)
        val hist = track(Mat())
        Imgproc.calcHist(listOf(small), MatOfInt(0), track(Mat()), hist, MatOfInt(256), MatOfFloat(0f, 256f), false)
        val total = small.total().toDouble()
        return percentile(hist, total, lowFraction) to percentile(hist, total, highFraction)
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
        /** Pixels at or above this luminance percentile are treated as paper for white balance. */
        const val PAPER_PERCENTILE = 0.90
        const val MIN_GAIN = 0.85
        const val MAX_GAIN = 1.2
    }
}

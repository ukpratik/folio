// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.processing.opencv

import javax.inject.Inject
import javax.inject.Singleton
import org.opencv.android.OpenCVLoader

/** Loads the OpenCV native library on first use, never at startup (NFR-03, ADR-0009). */
@Singleton
class OpenCvRuntime @Inject constructor() {
    private val loaded by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { OpenCVLoader.initLocal() }

    fun ensureLoaded() {
        check(loaded) { "OpenCV native library failed to load" }
    }
}

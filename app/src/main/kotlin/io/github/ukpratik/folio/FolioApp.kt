// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/** Keep onCreate light: cold start ≤ 1.5 s (NFR-03). OpenCV and the DB load lazily. */
@HiltAndroidApp
class FolioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}

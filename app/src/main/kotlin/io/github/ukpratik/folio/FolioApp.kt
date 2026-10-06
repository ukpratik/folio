// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.ukpratik.folio.core.domain.concurrency.ApplicationScope
import io.github.ukpratik.folio.core.domain.concurrency.IoDispatcher
import io.github.ukpratik.folio.core.domain.time.SessionInfo
import io.github.ukpratik.folio.core.domain.usecase.RecoverOnStartup
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/** Keep onCreate light: cold start ≤ 1.5 s (NFR-03). Heavy objects are created lazily. */
@HiltAndroidApp
class FolioApp : Application() {
    /** Injected eagerly so the session start is fixed before any import can begin. */
    @Inject lateinit var session: SessionInfo
    @Inject lateinit var recoverOnStartup: Provider<RecoverOnStartup>
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope
    @Inject @IoDispatcher lateinit var io: CoroutineDispatcher

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        // Off the main thread; only touches data from before this session (LLD §3.4).
        appScope.launch(io) {
            runCatching { recoverOnStartup.get()() }
                .onSuccess { Timber.d("Startup recovery: %s", it) }
                .onFailure { Timber.w(it, "Startup recovery failed") }
        }
    }
}

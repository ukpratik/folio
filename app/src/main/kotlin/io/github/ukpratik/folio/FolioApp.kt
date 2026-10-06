// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import io.github.ukpratik.folio.core.processing.thumbnail.PageThumbnailFetcherFactory
import io.github.ukpratik.folio.core.processing.thumbnail.PageThumbnailKeyer
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
class FolioApp : Application(), SingletonImageLoader.Factory {
    @Inject lateinit var thumbnailFetcher: PageThumbnailFetcherFactory
    @Inject lateinit var thumbnailKeyer: PageThumbnailKeyer

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

    /** Coil renders page thumbnails through our engine. No network components are registered (ADR-0013). */
    override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(thumbnailKeyer)
            add(thumbnailFetcher)
        }
        .memoryCache { MemoryCache.Builder().maxSizePercent(context, THUMBNAIL_MEMORY_FRACTION).build() }
        .build()

    private companion object {
        /** LLD §9: 15 % of the app's memory class. */
        const val THUMBNAIL_MEMORY_FRACTION = 0.15
    }
}

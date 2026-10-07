// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.ukpratik.folio.core.domain.concurrency.ApplicationScope
import io.github.ukpratik.folio.core.domain.concurrency.DefaultDispatcher
import io.github.ukpratik.folio.core.domain.concurrency.IoDispatcher
import io.github.ukpratik.folio.core.domain.time.Clock
import io.github.ukpratik.folio.core.domain.time.SessionInfo
import io.github.ukpratik.folio.BuildConfig
import io.github.ukpratik.folio.R
import io.github.ukpratik.folio.core.model.AppInfo
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides @DefaultDispatcher
    fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /** Coordinators run here so work outlives screens (ADR-0012). SupervisorJob: one failure doesn't cancel the rest. */
    @Provides @Singleton @ApplicationScope
    fun applicationScope(@DefaultDispatcher dispatcher: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatcher)

    @Provides @Singleton
    fun clock(): Clock = object : Clock {
        override fun nowMillis(): Long = System.currentTimeMillis()
    }

    @Provides @Singleton
    fun appInfo(): AppInfo = AppInfo(
        versionName = BuildConfig.VERSION_NAME,
        rateUrl = BuildConfig.RATE_URL,
        feedbackEmail = BuildConfig.FEEDBACK_EMAIL,
        licencesResId = R.raw.aboutlibraries,
        supportsUpdatePrompts = BuildConfig.UPDATE_PROMPTS,
    )

    @Provides @Singleton
    fun session(clock: Clock): SessionInfo = SessionInfo(startedAtMillis = clock.nowMillis())
}

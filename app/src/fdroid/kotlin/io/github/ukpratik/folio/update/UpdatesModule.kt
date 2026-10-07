// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.ukpratik.folio.core.domain.update.AppUpdates
import io.github.ukpratik.folio.core.domain.update.AvailableUpdate
import javax.inject.Singleton

/** F-Droid build: the F-Droid client notifies about and installs updates; Folio stays fully open source (D-48). */
private object NoAppUpdates : AppUpdates, UpdateLauncher {
    override suspend fun available(): AvailableUpdate? = null

    override fun launch(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean = false
}

@Module
@InstallIn(SingletonComponent::class)
internal object UpdatesModule {
    @Provides @Singleton fun appUpdates(): AppUpdates = NoAppUpdates

    @Provides @Singleton fun launcher(): UpdateLauncher = NoAppUpdates
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.update

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.ukpratik.folio.core.domain.update.AppUpdates

@Module
@InstallIn(SingletonComponent::class)
internal abstract class UpdatesModule {
    @Binds abstract fun appUpdates(impl: PlayAppUpdates): AppUpdates

    @Binds abstract fun launcher(impl: PlayAppUpdates): UpdateLauncher
}

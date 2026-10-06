// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.data.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.ukpratik.folio.core.data.db.DocumentDao
import io.github.ukpratik.folio.core.data.db.FolioDatabase
import io.github.ukpratik.folio.core.data.db.PageDao
import io.github.ukpratik.folio.core.data.files.FileStore
import io.github.ukpratik.folio.core.data.prefs.DataStorePreferencesRepository
import io.github.ukpratik.folio.core.data.repo.RoomDocumentRepository
import io.github.ukpratik.folio.core.data.repo.RoomPageRepository
import io.github.ukpratik.folio.core.domain.repository.DocumentRepository
import io.github.ukpratik.folio.core.domain.files.DocumentFiles
import io.github.ukpratik.folio.core.domain.repository.PageRepository
import io.github.ukpratik.folio.core.domain.repository.PreferencesRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): FolioDatabase =
        Room.databaseBuilder(context, FolioDatabase::class.java, "folio.db").build()

    @Provides fun documentDao(db: FolioDatabase): DocumentDao = db.documentDao()

    @Provides fun pageDao(db: FolioDatabase): PageDao = db.pageDao()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    @Binds abstract fun documents(impl: RoomDocumentRepository): DocumentRepository

    @Binds abstract fun pages(impl: RoomPageRepository): PageRepository

    @Binds abstract fun preferences(impl: DataStorePreferencesRepository): PreferencesRepository

    @Binds @Singleton abstract fun files(impl: FileStore): DocumentFiles
}

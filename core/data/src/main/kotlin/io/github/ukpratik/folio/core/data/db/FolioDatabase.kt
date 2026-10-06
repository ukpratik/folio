// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DocumentEntity::class, PageEntity::class], version = 1, exportSchema = true)
abstract class FolioDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun pageDao(): PageDao
}

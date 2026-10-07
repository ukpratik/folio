// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.ukpratik.folio.core.domain.repository.PreferencesRepository
import io.github.ukpratik.folio.core.model.PageSize
import io.github.ukpratik.folio.core.model.QualityPreset
import io.github.ukpratik.folio.core.model.UpdateReminder
import io.github.ukpratik.folio.core.model.UserPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "folio_prefs")

@Singleton
internal class DataStorePreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
) : PreferencesRepository {
    private val store = context.dataStore

    override val preferences: Flow<UserPreferences> = store.data.map { it.toModel() }

    override suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        store.edit { prefs ->
            val next = transform(prefs.toModel())
            prefs.putOrRemove(PAGE_SIZE, next.defaultPageSize?.name)
            prefs[QUALITY] = next.defaultQuality.name
            prefs[CAMERA_ASKED] = next.cameraPermissionRequested
            prefs.putOrRemove(SAVE_FOLDER, next.lastSaveFolderUri)
            prefs[UPDATE_REMINDER] = next.updateReminder.name
            next.lastUpdatePromptAt.let { at -> if (at == null) prefs.remove(UPDATE_PROMPTED_AT) else prefs[UPDATE_PROMPTED_AT] = at }
        }
    }

    private fun Preferences.toModel() = UserPreferences(
        defaultPageSize = this[PAGE_SIZE]?.let { name -> PageSize.entries.firstOrNull { it.name == name } },
        defaultQuality = this[QUALITY]?.let { name -> QualityPreset.entries.firstOrNull { it.name == name } } ?: QualityPreset.BALANCED,
        cameraPermissionRequested = this[CAMERA_ASKED] ?: false,
        lastSaveFolderUri = this[SAVE_FOLDER],
        updateReminder = this[UPDATE_REMINDER]?.let { name -> UpdateReminder.entries.firstOrNull { it.name == name } }
            ?: UpdateReminder.EVERY_LAUNCH,
        lastUpdatePromptAt = this[UPDATE_PROMPTED_AT],
    )

    private fun androidx.datastore.preferences.core.MutablePreferences.putOrRemove(key: Preferences.Key<String>, value: String?) {
        if (value == null) remove(key) else this[key] = value
    }

    private companion object {
        val PAGE_SIZE = stringPreferencesKey("default_page_size")
        val QUALITY = stringPreferencesKey("default_quality")
        val CAMERA_ASKED = booleanPreferencesKey("camera_permission_requested")
        val SAVE_FOLDER = stringPreferencesKey("last_save_folder")
        val UPDATE_REMINDER = stringPreferencesKey("update_reminder")
        val UPDATE_PROMPTED_AT = longPreferencesKey("update_prompted_at")
    }
}

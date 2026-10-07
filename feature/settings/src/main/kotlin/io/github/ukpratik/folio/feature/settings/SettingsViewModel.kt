// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ukpratik.folio.core.domain.repository.PreferencesRepository
import io.github.ukpratik.folio.core.model.AppInfo
import io.github.ukpratik.folio.core.model.ExportSettings
import io.github.ukpratik.folio.core.model.PageSize
import io.github.ukpratik.folio.core.model.QualityPreset
import io.github.ukpratik.folio.core.model.UpdateReminder
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsState(
    val loaded: Boolean = false,
    /** What new documents get: the saved choice, or the region default (PRD §8). */
    val pageSize: PageSize = PageSize.A4,
    val quality: QualityPreset = QualityPreset.BALANCED,
    val updateReminder: UpdateReminder = UpdateReminder.EVERY_LAUNCH,
    val appInfo: AppInfo,
)

sealed interface SettingsIntent {
    data class SetPageSize(val pageSize: PageSize) : SettingsIntent
    data class SetQuality(val quality: QualityPreset) : SettingsIntent
    data class SetUpdateReminder(val reminder: UpdateReminder) : SettingsIntent
}

/** S8 (FR-34): defaults for new documents. Existing documents keep their own remembered settings. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    appInfo: AppInfo,
) : ViewModel() {
    private val region: String = Locale.getDefault().country

    val state: StateFlow<SettingsState> = preferences.preferences.map { prefs ->
        SettingsState(
            loaded = true,
            pageSize = prefs.defaultPageSize ?: ExportSettings.defaultPageSizeFor(region),
            quality = prefs.defaultQuality,
            updateReminder = prefs.updateReminder,
            appInfo = appInfo,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState(appInfo = appInfo))

    fun onIntent(intent: SettingsIntent) {
        viewModelScope.launch {
            when (intent) {
                is SettingsIntent.SetPageSize -> preferences.update { it.copy(defaultPageSize = intent.pageSize) }
                is SettingsIntent.SetQuality -> preferences.update { it.copy(defaultQuality = intent.quality) }
                // A new choice starts fresh, so the next update is offered on the next launch.
                is SettingsIntent.SetUpdateReminder -> preferences.update { it.copy(updateReminder = intent.reminder, lastUpdatePromptAt = null) }
            }
        }
    }
}

/** The licence screen only needs the generated resource id. */
@HiltViewModel
class LicencesViewModel @Inject constructor(appInfo: AppInfo) : ViewModel() {
    val licencesResId: Int = appInfo.licencesResId
}

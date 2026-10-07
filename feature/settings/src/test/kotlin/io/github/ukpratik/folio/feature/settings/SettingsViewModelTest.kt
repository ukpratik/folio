// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.settings

import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.model.AppInfo
import io.github.ukpratik.folio.core.model.PageSize
import io.github.ukpratik.folio.core.model.QualityPreset
import io.github.ukpratik.folio.core.testing.FakePreferencesRepository
import io.github.ukpratik.folio.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val preferences = FakePreferencesRepository()
    private val info = AppInfo("1.0.0", "market://details?id=x", "", 0)
    private val vm by lazy { SettingsViewModel(preferences, info) }

    @Test fun choicesAreSavedAsDefaults() = runTest {
        vm.onIntent(SettingsIntent.SetPageSize(PageSize.LEGAL))
        vm.onIntent(SettingsIntent.SetQuality(QualityPreset.SMALL))
        val state = vm.state.first { it.loaded && it.quality == QualityPreset.SMALL }
        assertThat(state.pageSize).isEqualTo(PageSize.LEGAL)
        assertThat(preferences.preferences.value.defaultPageSize).isEqualTo(PageSize.LEGAL)
        assertThat(preferences.preferences.value.defaultQuality).isEqualTo(QualityPreset.SMALL)
    }

    @Test fun showsTheVersion() = runTest {
        assertThat(vm.state.first { it.loaded }.appInfo.versionName).isEqualTo("1.0.0")
    }
}

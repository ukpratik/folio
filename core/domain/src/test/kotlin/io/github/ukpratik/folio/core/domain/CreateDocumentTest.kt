// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain

import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.domain.usecase.CreateDocument
import io.github.ukpratik.folio.core.model.PageSize
import io.github.ukpratik.folio.core.model.QualityPreset
import io.github.ukpratik.folio.core.model.UserPreferences
import io.github.ukpratik.folio.core.testing.FakeClock
import io.github.ukpratik.folio.core.testing.FakeDocumentRepository
import io.github.ukpratik.folio.core.testing.FakePreferencesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CreateDocumentTest {
    private val clock = FakeClock()
    private val documents = FakeDocumentRepository(clock)

    @Test fun usesTheRegionDefaultUntilTheUserPicksOne() = runTest {
        val create = CreateDocument(documents, FakePreferencesRepository(), clock)
        assertThat(create(region = "IN").exportSettings.pageSize).isEqualTo(PageSize.A4)
        assertThat(create(region = "US").exportSettings.pageSize).isEqualTo(PageSize.LETTER)
    }

    @Test fun usesTheSettingsDefaults() = runTest {
        val prefs = FakePreferencesRepository(UserPreferences(defaultPageSize = PageSize.LEGAL, defaultQuality = QualityPreset.HIGH))
        val settings = CreateDocument(documents, prefs, clock)(region = "IN").exportSettings
        assertThat(settings.pageSize).isEqualTo(PageSize.LEGAL)
        assertThat(settings.quality).isEqualTo(QualityPreset.HIGH)
    }
}

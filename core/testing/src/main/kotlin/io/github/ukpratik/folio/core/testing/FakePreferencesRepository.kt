// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.testing

import io.github.ukpratik.folio.core.domain.repository.PreferencesRepository
import io.github.ukpratik.folio.core.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow

class FakePreferencesRepository(initial: UserPreferences = UserPreferences()) : PreferencesRepository {
    override val preferences = MutableStateFlow(initial)

    override suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        preferences.value = transform(preferences.value)
    }
}

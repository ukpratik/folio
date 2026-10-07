// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain

import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.domain.update.AvailableUpdate
import io.github.ukpratik.folio.core.domain.update.DecideUpdatePrompt
import io.github.ukpratik.folio.core.domain.update.UpdatePrompt
import io.github.ukpratik.folio.core.model.UpdateReminder
import io.github.ukpratik.folio.core.model.UserPreferences
import io.github.ukpratik.folio.core.testing.FakeClock
import io.github.ukpratik.folio.core.testing.FakePreferencesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DecideUpdatePromptTest {
    private val hour = 60 * 60 * 1000L
    private val clock = FakeClock(now = 1_000 * hour)
    private val normal = AvailableUpdate(versionCode = 2, priority = 0)

    private fun decide(prefs: UserPreferences = UserPreferences()) = DecideUpdatePrompt(FakePreferencesRepository(prefs), clock)

    @Test fun noUpdateNoPrompt() = runTest { assertThat(decide()(null)).isNull() }

    @Test fun firstTimeIsOffered() = runTest { assertThat(decide()(normal)).isEqualTo(UpdatePrompt.OFFER) }

    @Test fun criticalIsAlwaysRequiredEvenRightAfterLater() = runTest {
        val prefs = UserPreferences(updateReminder = UpdateReminder.WEEKLY, lastUpdatePromptAt = clock.nowMillis())
        assertThat(decide(prefs)(AvailableUpdate(2, priority = 4))).isEqualTo(UpdatePrompt.REQUIRE)
    }

    @Test fun everyLaunchAsksAgainImmediately() = runTest {
        val prefs = UserPreferences(updateReminder = UpdateReminder.EVERY_LAUNCH, lastUpdatePromptAt = clock.nowMillis())
        assertThat(decide(prefs)(normal)).isEqualTo(UpdatePrompt.OFFER)
    }

    @Test fun dailyWaitsADayAfterLater() = runTest {
        val prefs = UserPreferences(updateReminder = UpdateReminder.DAILY, lastUpdatePromptAt = clock.nowMillis() - 23 * hour)
        assertThat(decide(prefs)(normal)).isNull()
        val dayLater = prefs.copy(lastUpdatePromptAt = clock.nowMillis() - 24 * hour)
        assertThat(decide(dayLater)(normal)).isEqualTo(UpdatePrompt.OFFER)
    }

    @Test fun laterRecordsTheTime() = runTest {
        val repo = FakePreferencesRepository()
        DecideUpdatePrompt(repo, clock).snooze()
        assertThat(repo.preferences.value.lastUpdatePromptAt).isEqualTo(clock.nowMillis())
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.domain.update

import io.github.ukpratik.folio.core.domain.repository.PreferencesRepository
import io.github.ukpratik.folio.core.domain.time.Clock
import io.github.ukpratik.folio.core.model.UpdateReminder
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * A newer version waiting in the store. [priority] is set per release in the Play Console (0–5, D-48);
 * 4 and 5 mark critical fixes.
 */
data class AvailableUpdate(val versionCode: Int, val priority: Int) {
    val isCritical: Boolean get() = priority >= CRITICAL_PRIORITY

    companion object {
        const val CRITICAL_PRIORITY = 4
    }
}

/** Asks the store whether an update is waiting. Folio has no internet: on Play this goes through the Play Store app. */
interface AppUpdates {
    suspend fun available(): AvailableUpdate?
}

enum class UpdatePrompt {
    /** "A new version is ready" with Update and Later. */
    OFFER,

    /** Critical update: Update only, no Later. */
    REQUIRE,
}

/** D-48: critical updates are always required; others are offered no more often than the user's reminder setting. */
class DecideUpdatePrompt @Inject constructor(
    private val preferences: PreferencesRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(update: AvailableUpdate?): UpdatePrompt? {
        update ?: return null
        if (update.isCritical) return UpdatePrompt.REQUIRE
        val prefs = preferences.preferences.first()
        val last = prefs.lastUpdatePromptAt ?: return UpdatePrompt.OFFER
        val due = prefs.updateReminder == UpdateReminder.EVERY_LAUNCH || clock.nowMillis() - last >= prefs.updateReminder.intervalMillis
        return if (due) UpdatePrompt.OFFER else null
    }

    /** "Later": remember when, so the next reminder waits for the chosen interval. */
    suspend fun snooze() = preferences.update { it.copy(lastUpdatePromptAt = clock.nowMillis()) }
}

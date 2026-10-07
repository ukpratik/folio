// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.update

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.ukpratik.folio.core.domain.update.AppUpdates
import io.github.ukpratik.folio.core.domain.update.AvailableUpdate
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * D-48: asks the Play Store app (over IPC; Folio has no internet) whether a newer version is published, and
 * starts Play's own full-screen update. Only immediate updates are used: the user taps Update and Play
 * installs and restarts Folio, so nobody is left half-updated.
 */
@Singleton
internal class PlayAppUpdates @Inject constructor(@ApplicationContext context: Context) : AppUpdates, UpdateLauncher {
    private val manager = AppUpdateManagerFactory.create(context)
    private var latest: AppUpdateInfo? = null

    override suspend fun available(): AvailableUpdate? = try {
        val info = manager.requestAppUpdateInfo().also { latest = it }
        when (info.updateAvailability()) {
            UpdateAvailability.UPDATE_AVAILABLE ->
                if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) AvailableUpdate(info.availableVersionCode(), info.updatePriority()) else null
            // An update the user started and left: finish it.
            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                AvailableUpdate(info.availableVersionCode(), AvailableUpdate.CRITICAL_PRIORITY)
            else -> null
        }
    } catch (e: Exception) {
        // Not installed from Play (sideloaded, dev builds), Play Store missing or offline: just no prompt.
        Timber.d(e, "Update check unavailable")
        null
    }

    override fun launch(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean {
        val info = latest ?: return false
        return manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE))
    }
}

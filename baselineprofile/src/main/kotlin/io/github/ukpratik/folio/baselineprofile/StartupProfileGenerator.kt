// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the code Folio runs from launch to an interactive Home, plus opening Settings, into the
 * Baseline Profile shipped in the APK (ADR-0019). Run: `./gradlew :app:generatePlayReleaseBaselineProfile`.
 */
@RunWith(AndroidJUnit4::class)
class StartupProfileGenerator {
    @get:Rule val rule = BaselineProfileRule()

    @Test fun generate() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.text("Scan")), TIMEOUT_MS)
        device.findObject(By.desc("Settings"))?.click()
        device.wait(Until.hasObject(By.text("Settings")), TIMEOUT_MS)
        device.pressBack()
    }
}

internal const val PACKAGE = "io.github.ukpratik.folio"
internal const val TIMEOUT_MS = 5_000L

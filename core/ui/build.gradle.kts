// SPDX-License-Identifier: GPL-3.0-or-later
plugins {
    id("folio.android.library")
    id("folio.android.compose")
}

dependencies {
    api(project(":core:model"))
    api(libs.coil.compose)
    api(libs.compose.material.icons.extended)

    testImplementation(libs.robolectric)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}

android {
    testOptions.unitTests.isIncludeAndroidResources = true
}

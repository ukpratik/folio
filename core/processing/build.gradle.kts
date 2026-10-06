// SPDX-License-Identifier: GPL-3.0-or-later
plugins {
    id("folio.android.library")
    id("folio.hilt")
}

android {
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    api(project(":core:domain"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.exifinterface)
    implementation(libs.timber)
    // OpenCV (core + imgproc) arrives in M2 (ADR-0009).

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
}

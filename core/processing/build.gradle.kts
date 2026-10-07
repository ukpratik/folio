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
    // Maven AAR for now; a slim core+imgproc source build replaces it before F-Droid (ADR-0009, ADR-0021).
    implementation(libs.opencv)
    implementation(libs.coil.core)

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
    testImplementation(libs.pdfbox) // parse our PDFs in tests (test-only, Apache-2.0)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

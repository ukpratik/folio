// SPDX-License-Identifier: GPL-3.0-or-later
plugins {
    id("folio.android.feature")
}

dependencies {
    // Android-only adapters only: the edge detector and quad smoother (ADR-0005 note in the M5 plan).
    implementation(project(":core:processing"))
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.timber)
}

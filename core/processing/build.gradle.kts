// SPDX-License-Identifier: GPL-3.0-or-later
plugins {
    id("folio.android.library")
}

dependencies {
    api(project(":core:domain"))
    implementation(libs.kotlinx.coroutines.android)
    // OpenCV (core + imgproc) is added after spikes S1/S4 decide the slim build (ADR-0009, ADR-0021).
}

// SPDX-License-Identifier: GPL-3.0-or-later
plugins {
    id("folio.jvm.library")
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:domain"))
    api(libs.kotlinx.coroutines.test)
    api(libs.junit)
    api(libs.truth)
}

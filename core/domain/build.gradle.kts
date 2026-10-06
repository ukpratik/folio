// SPDX-License-Identifier: GPL-3.0-or-later
plugins {
    id("folio.jvm.library")
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
    api(libs.javax.inject)
    testImplementation(project(":core:testing"))
}

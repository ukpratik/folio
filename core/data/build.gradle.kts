// SPDX-License-Identifier: GPL-3.0-or-later
plugins {
    id("folio.android.library")
    id("folio.hilt")
}

android {
    testOptions.unitTests.isIncludeAndroidResources = true
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    api(project(":core:domain"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.junit)
}

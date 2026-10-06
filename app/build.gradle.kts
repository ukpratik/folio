// SPDX-License-Identifier: GPL-3.0-or-later
plugins {
    id("folio.android.application")
    id("folio.android.compose")
    id("folio.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.ukpratik.folio"

    defaultConfig {
        applicationId = "io.github.ukpratik.folio" // permanent after first upload (ADR-0022)
        versionCode = 1
        versionName = "0.1.0"
        // Phones are ARM. Dropping x86/x86_64 keeps OpenCV's native code out of the APK (NFR-08).
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signing is configured in CI from secrets; local release builds are unsigned.
        }
    }

    // ADR-0021: identical apps except the "Rate" link and store metadata.
    flavorDimensions += "store"
    productFlavors {
        create("play") {
            dimension = "store"
            buildConfigField("String", "RATE_URL", "\"market://details?id=io.github.ukpratik.folio\"")
        }
        create("fdroid") {
            dimension = "store"
            buildConfigField("String", "RATE_URL", "\"https://f-droid.org/packages/io.github.ukpratik.folio/\"")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    // ADR-0021: keep builds reproducible for F-Droid.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:processing"))
    implementation(project(":core:ui"))
    implementation(project(":feature:home"))
    implementation(project(":feature:capture"))
    implementation(project(":feature:editor"))
    implementation(project(":feature:export"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
}

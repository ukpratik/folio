// SPDX-License-Identifier: GPL-3.0-or-later
plugins {
    id("folio.android.application")
    id("folio.android.compose")
    id("folio.hilt")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.aboutlibraries.android)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "io.github.ukpratik.folio"

    defaultConfig {
        applicationId = "io.github.ukpratik.folio" // permanent after first upload (ADR-0022)
        versionCode = 1
        versionName = "0.1.0"
        // Settings → Send feedback recipient. Set `folio.feedbackEmail` in gradle.properties; blank lets the user choose.
        buildConfigField("String", "FEEDBACK_EMAIL", "\"${providers.gradleProperty("folio.feedbackEmail").getOrElse("")}\"")
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
            buildConfigField("boolean", "UPDATE_PROMPTS", "true")
        }
        create("fdroid") {
            dimension = "store"
            buildConfigField("String", "RATE_URL", "\"https://f-droid.org/packages/io.github.ukpratik.folio/\"")
            buildConfigField("boolean", "UPDATE_PROMPTS", "false")
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

// One profile for both store flavours (they run identical code).
baselineProfile {
    mergeIntoMain = true
}

// FR-34 licence screen + CI licence gate (ADR-0023): generated at build time, fully offline.
aboutLibraries {
    offlineMode = true
    collect {
        // Licence texts are committed here (SPDX) because the plugin runs offline.
        configPath = file("aboutlibraries")
        fetchRemoteLicense = false
        fetchRemoteFunding = false
        filterVariants.addAll("playRelease", "fdroidRelease")
    }
    export {
        // No timestamps: keeps builds reproducible for F-Droid (ADR-0021).
        excludeFields.addAll("generated", "funding", "developers")
    }
    license {
        // Everything shipped must be GPL-3.0-compatible. A new licence fails the build until reviewed.
        strictMode = com.mikepenz.aboutlibraries.plugin.StrictMode.FAIL
        allowedLicenses.addAll("Apache-2.0", "MIT", "BSD-2-Clause", "BSD-3-Clause", "OFL-1.1")
        // D-48: Google's in-app update library, Play build only. Allowed for these Google groups and nothing else.
        allowedLicensesMap = mapOf(
            "PCSDKToS" to listOf("com.google.android.play"),
            "ASDKL" to listOf("com.google.android.gms"),
        )
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
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
    implementation(libs.coil.core)

    // Play build only (D-48): update prompts via the Play Store app. Closed source, so never in the F-Droid build.
    "playImplementation"(libs.play.app.update.ktx)

    // Debug only: finds leaked screens/ViewModels. Never in release (permission gate checks release APKs).
    debugImplementation(libs.leakcanary)
}

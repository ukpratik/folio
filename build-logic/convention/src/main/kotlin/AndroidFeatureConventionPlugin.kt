// SPDX-License-Identifier: GPL-3.0-or-later
import org.gradle.api.Plugin
import org.gradle.api.Project
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/** Feature modules: Android library + Compose + Hilt + core UI/domain. Features never depend on each other (ADR-0005). */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("folio.android.library")
        pluginManager.apply("folio.android.compose")
        pluginManager.apply("folio.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
        pluginManager.apply("io.github.takahirom.roborazzi")
        extensions.configure<LibraryExtension> {
            // Screenshot tests render Compose on the JVM with Robolectric (ADR-0018).
            testOptions.unitTests.isIncludeAndroidResources = true
        }
        // Placeholder features (camera, export) gain tests in their milestones; until then an empty run is fine.
        tasks.withType<Test>().configureEach { failOnNoDiscoveredTests.set(false) }
        dependencies {
            add("implementation", project(":core:model"))
            add("implementation", project(":core:domain"))
            add("implementation", project(":core:ui"))
            add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.lib("hilt-navigation-compose"))
            add("implementation", libs.lib("androidx-navigation-compose"))
            add("implementation", libs.lib("kotlinx-serialization-json"))
            add("testImplementation", libs.lib("turbine"))
            add("testImplementation", project(":core:testing"))
            add("testImplementation", libs.lib("robolectric"))
            add("testImplementation", libs.lib("roborazzi"))
            add("testImplementation", libs.lib("roborazzi-compose"))
            add("testImplementation", libs.lib("roborazzi-junit-rule"))
            add("testImplementation", libs.lib("compose-ui-test-junit4"))
            add("debugImplementation", libs.lib("compose-ui-test-manifest"))
        }
    }
}

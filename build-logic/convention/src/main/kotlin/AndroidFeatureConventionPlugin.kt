// SPDX-License-Identifier: GPL-3.0-or-later
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/** Feature modules: Android library + Compose + Hilt + core UI/domain. Features never depend on each other (ADR-0005). */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("folio.android.library")
        pluginManager.apply("folio.android.compose")
        pluginManager.apply("folio.hilt")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
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
        }
    }
}

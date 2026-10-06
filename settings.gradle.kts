// SPDX-License-Identifier: GPL-3.0-or-later
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "folio"

include(":app")
include(":core:model")
include(":core:domain")
include(":core:data")
include(":core:processing")
include(":core:ui")
include(":core:testing")
include(":feature:home")
include(":feature:capture")
include(":feature:editor")
include(":feature:export")

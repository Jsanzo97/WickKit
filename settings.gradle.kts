pluginManagement {
    includeBuild("build-logic")
    includeBuild("wickkit-gradle-plugin")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "WickKit"
include(":app")
include(":wickkit-core")
include(":wickkit-network")
include(":wickkit-flags")
include(":wickkit-no-op")
include(":wickkit-compose")
include(":wickkit-compose-no-op")
 
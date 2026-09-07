plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    id("detekt-convention")
    id("spotless-convention")
    id("publish-convention")
    alias(libs.plugins.gradle.plugin.publish)
}

group = "io.github.jsanzo97"
version = run {
    val tags = providers.exec {
        commandLine("git", "tag", "--list", "v*.*.*", "--sort=-v:refname")
    }.standardOutput.asText.getOrElse("")
    tags.trim().lines().firstOrNull { it.isNotBlank() }?.removePrefix("v") ?: "0.1.0"
}

description = "WickKit Gradle plugin for Compose composable instrumentation"

gradlePlugin {
    website = "https://github.com/Jsanzo97/WickKit"
    vcsUrl = "https://github.com/Jsanzo97/WickKit.git"
    plugins {
        create("wickKitPlugin") {
            id = "io.github.jsanzo97.wickkit"
            displayName = "WickKit"
            description = "Instruments @Composable functions to track recompositions in the WickKit debug overlay"
            tags = listOf("android", "compose", "debug", "instrumentation", "recomposition")
            implementationClass = "io.wickkit.gradle.WickKitPlugin"
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
}

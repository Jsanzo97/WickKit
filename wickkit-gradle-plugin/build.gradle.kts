plugins {
    `java-gradle-plugin`
    kotlin("jvm")
    id("detekt-convention")
    id("spotless-convention")
    id("publish-convention")
}

description = "WickKit Gradle plugin for Compose composable instrumentation"

gradlePlugin {
    plugins {
        create("wickKitPlugin") {
            id = "io.github.jsanzo97.wickkit"
            implementationClass = "io.wickkit.gradle.WickKitPlugin"
        }
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
}

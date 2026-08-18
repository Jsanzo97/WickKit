plugins {
    `java-gradle-plugin`
    kotlin("jvm")
    id("detekt-convention")
    id("spotless-convention")
}

gradlePlugin {
    plugins {
        create("wickKitPlugin") {
            id = "io.wickkit"
            implementationClass = "io.wickkit.gradle.WickKitPlugin"
        }
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
}

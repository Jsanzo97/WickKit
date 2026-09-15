plugins {
    id("android-library-convention")
    id("detekt-convention")
    id("spotless-convention")
    id("jacoco-convention")
    id("publish-convention")
}

description = "WickKit debug overlay — network inspection module"

android {
    namespace = "io.wickkit.network"
}

dependencies {
    implementation(project(":wickkit-core"))
    implementation(libs.kotlinx.collections.immutable)
    compileOnly(libs.okhttp)
    compileOnly(libs.ktor.client.core)
    testImplementation(project(":wickkit-core"))
    testImplementation(libs.okhttp)
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.mock)
}

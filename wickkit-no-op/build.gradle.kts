plugins {
    id("android-library-convention")
    id("detekt-convention")
    id("spotless-convention")
    id("jacoco-convention")
    id("publish-convention")
}

description = "WickKit no-op stub for release builds"

android {
    namespace = "io.wickkit.noop"
}

dependencies {
    implementation(libs.kotlinx.collections.immutable)
    compileOnly(libs.okhttp)
    compileOnly(libs.ktor.client.core)
}

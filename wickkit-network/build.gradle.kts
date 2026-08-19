plugins {
    id("android-library-convention")
    id("detekt-convention")
    id("spotless-convention")
    id("jacoco-convention")
}

android {
    namespace = "io.wickkit.network"
}

dependencies {
    implementation(project(":wickkit-core"))
    compileOnly(libs.okhttp)
    compileOnly(libs.ktor.client.core)
    testImplementation(libs.okhttp)
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.mock)
}

plugins {
    id("android-library-convention")
    id("detekt-convention")
    id("spotless-convention")
    id("jacoco-convention")
    id("publish-convention")
    alias(libs.plugins.kotlin.compose)
}

description = "WickKit Compose integration for recomposition tracking"

android {
    namespace = "io.wickkit.compose"
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.kotlinx.collections.immutable)
    testImplementation(libs.junit)
}

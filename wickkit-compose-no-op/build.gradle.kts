plugins {
    id("android-library-convention")
    id("detekt-convention")
    id("spotless-convention")
    id("jacoco-convention")
    id("publish-convention")
}

description = "WickKit Compose no-op stub for release builds"

android {
    namespace = "io.wickkit.compose.noop"
}

plugins {
    id("android-library-convention")
    id("detekt-convention")
    id("spotless-convention")
    id("jacoco-convention")
    id("publish-convention")
}

description = "WickKit debug overlay — feature flags and SharedPreferences module"

android {
    namespace = "io.wickkit.flags"
}

dependencies {
    compileOnly(project(":wickkit-core"))
}

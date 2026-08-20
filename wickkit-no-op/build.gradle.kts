plugins {
    id("android-library-convention")
    id("detekt-convention")
    id("spotless-convention")
    id("jacoco-convention")
}

android {
    namespace = "io.wickkit.noop"
}

dependencies {
    compileOnly(libs.okhttp)
}

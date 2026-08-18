plugins {
    id("android-library-convention")
    id("detekt-convention")
    id("spotless-convention")
    id("jacoco-convention")
}

android {
    namespace = "io.wickkit.flags"
}

dependencies {
    implementation(project(":wickkit-core"))
}

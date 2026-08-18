package io.wickkit.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class WickKitPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // Bytecode transformation (OkHttp) and Kotlin Compiler Plugin (Compose)
        // will be implemented in a later milestone
    }
}

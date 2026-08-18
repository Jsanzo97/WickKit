plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless) apply false
}

val appVersionName: String by lazy {
    val tags = providers.exec {
        commandLine("git", "tag", "--list", "v*.*.*", "--sort=-v:refname")
    }.standardOutput.asText.getOrElse("")
    tags.trim().lines().firstOrNull { it.isNotBlank() }?.removePrefix("v") ?: "0.1.0"
}

val appVersionCode: Int by lazy {
    val parts = appVersionName.split(".").mapNotNull { it.toIntOrNull() }
    if (parts.size == 3) parts[0] * 10000 + parts[1] * 100 + parts[2] else 1
}

tasks.register<Copy>("installGitHooks") {
    from(layout.projectDirectory.dir("config/git-hooks"))
    into(layout.projectDirectory.dir(".git/hooks"))
    filePermissions {
        user { read = true; write = true; execute = true }
        group { read = true; execute = true }
        other { read = true; execute = true }
    }
}

allprojects {
    tasks.matching { it.name == "preBuild" }.configureEach {
        dependsOn(":installGitHooks")
    }
}

tasks.register<JacocoReport>("jacocoRootReport") {
    dependsOn(subprojects.mapNotNull { it.tasks.findByName("jacocoTestReport") })

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val allClassDirs = subprojects.flatMap { sub ->
        sub.tasks.withType<JacocoReport>().flatMap { it.classDirectories.files }
    }
    val allSourceDirs = subprojects.flatMap { sub ->
        sub.tasks.withType<JacocoReport>().flatMap { it.sourceDirectories.files }
    }
    val allExecData = subprojects.flatMap { sub ->
        sub.tasks.withType<JacocoReport>().flatMap { it.executionData.files }.filter { it.exists() }
    }

    classDirectories.setFrom(allClassDirs)
    sourceDirectories.setFrom(allSourceDirs)
    executionData.setFrom(allExecData)
}

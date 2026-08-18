plugins {
    jacoco
}

tasks.withType<Test> {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

private val classExcludes = listOf(
    "**/R.class", "**/R$*.class", "**/BuildConfig.*",
    "**/Manifest*.*", "**/*Test*.*",
    "**/Activity*.*", "**/Fragment*.*",
    "**/Screen*.*", "**/Preview*.*",
    "**/Navigation*.*", "**/NavHost*.*", "**/Route*.*",
    "**/*Composable*.*", "**/*Compose*.*", "**/*Component*.*",
    "**/*Module*.*", "**/*_Factory*.*", "**/*Lambda*.*",
    "**/*Companion*.*",
)

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val buildDir = layout.buildDirectory.get().asFile

    classDirectories.setFrom(
        fileTree("$buildDir/tmp/kotlin-classes/debug") { exclude(classExcludes) }
    )
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(
        fileTree(buildDir) {
            include(
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                "jacoco/testDebugUnitTest.exec",
            )
        }
    )
}

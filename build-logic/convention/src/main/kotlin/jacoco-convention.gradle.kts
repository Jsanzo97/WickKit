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
    // Android generated
    "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
    // Tests
    "**/*Test*.*",
    // Android-specific — not unit-testable
    "io/wickkit/overlay/**",
    "io/wickkit/core/**",
    "io/wickkit/WickKit*",
    "io/wickkit/logs/WickKitLogcat*",
    "**/*Activity*.*", "**/*Fragment*.*",
    "**/*Screen*.*", "**/*Preview*.*",
    "**/*Module*.*", "**/*_Factory*.*", "**/*Lambda*.*", "**/*Companion*.*",
    // Data / value classes — auto-generated getters, copy, equals, hashCode
    "io/wickkit/network/NetworkEntry*",
    "io/wickkit/network/MockRule*",
    "io/wickkit/logs/LogEntry*",
    "io/wickkit/database/DatabaseEntry*",
    "io/wickkit/database/DatabaseStatus*",
    "io/wickkit/database/ColumnInfo*",
)

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val buildDir = layout.buildDirectory.get().asFile

    classDirectories.setFrom(
        fileTree("$buildDir/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes") {
            exclude(classExcludes)
        }
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

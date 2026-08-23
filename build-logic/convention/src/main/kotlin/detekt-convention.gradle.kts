plugins {
    id("io.gitlab.arturbosch.detekt")
}

detekt {
    toolVersion = "1.23.8"
    config.setFrom(files("$rootDir/config/detekt.yml"))
    buildUponDefaultConfig = true
    source.setFrom(fileTree("src") { include("**/*.kt") })
}

dependencies {
    add("detektPlugins", "io.nlopez.compose.rules:detekt:0.6.4")
}

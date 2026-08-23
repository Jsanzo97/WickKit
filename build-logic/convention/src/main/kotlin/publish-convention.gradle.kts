plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set(project.name)
        description.set(project.description ?: "WickKit Android debug overlay SDK")
        inceptionYear.set("2025")
        url.set("https://github.com/Jsanzo97/WickKit")
        licenses {
            license {
                name.set("Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("jsanzo97")
                name.set("Jorge Sanzo Hernando")
                url.set("https://github.com/Jsanzo97")
            }
        }
        scm {
            url.set("https://github.com/Jsanzo97/WickKit")
            connection.set("scm:git:git://github.com/Jsanzo97/WickKit.git")
            developerConnection.set("scm:git:ssh://git@github.com/Jsanzo97/WickKit.git")
        }
    }
}

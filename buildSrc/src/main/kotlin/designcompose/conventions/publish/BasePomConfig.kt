package designcompose.conventions.publish

import org.gradle.api.publish.maven.MavenPom

fun MavenPom.basePom() {
    url.set("https://github.com/google/automotive-design-compose")
    licenses {
        license {
            name.set("The Apache License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0")
        }
    }
    developers {
        developer {
            id.set("designCompose")
            name.set("The Automotive Design for Compose team")
            url.set("aae-design-compose@google.com")
        }
    }
    scm {
        connection.set(
            "scm:git:https://github.com/google/automotive-design-compose.git"
        )
        developerConnection.set(
            "scm:git:git@github.com:google/automotive-design-compose.git"
        )
        url.set("https://github.com/google/automotive-design-compose")
    }
}

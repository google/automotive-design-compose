/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

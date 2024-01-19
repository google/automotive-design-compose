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

rootProject.name = "DesignCompose Tutorial App"

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    // Use the same version catalog that we use for the core SDK
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
            // Use the latest published version of the SDK
            version("designcompose", "+")
        }
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
    }
}

val unbundledAAOSDir: String? by settings
val unbundledRepo = File(unbundledAAOSDir, "out/aaos-apps-gradle-build/unbundled_m2repo")

if (unbundledRepo.exists()) {
    dependencyResolutionManagement { repositories { maven(uri(unbundledRepo)) } }
} else {
    throw GradleException(
        "Cannot find compiled Unbundled libraries, cannot proceed with build.\n" +
            "Go to $unbundledAAOSDir/packages/apps/Car/libs/aaos-apps-gradle-project \n" +
            "and run:\n" +
            "./gradlew publishAllPublicationsToLocalRepository"
    )
}

include(":media-lib")

project(":media-lib").projectDir = File("media")

include(":mediacompose-app")

project(":mediacompose-app").projectDir = File("mediacompose")

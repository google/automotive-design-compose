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

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        gradlePluginPortal()
    }
    includeBuild("plugins")
    includeBuild("build-logic")
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    // Downloads the required Java Toolchain, if needed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "DesignCompose"

// Published modules
include("designcompose")

include("common")

include("annotation")

include("codegen")

include("test")

include("test:test-native")

// Internal support
include("test:internal")

// Integration (and benchmark) tests
include("integration-tests:app-common")

include("validation-app")

project(":validation-app").projectDir = File("integration-tests/validation")

include("battleship-app")

project(":battleship-app").projectDir =
    File("integration-tests/benchmarks/battleship/battleship-app")

include("integration-tests:benchmarks:battleship:lib")

include("integration-tests:benchmarks:battleship:benchmark")

// Reference apps (Can only use published libraries)
include("helloworld-app")

project(":helloworld-app").projectDir = File("reference-apps/helloworld/helloworld-app")

include("tutorial-app")

project(":tutorial-app").projectDir = File("reference-apps/tutorial/app")

// Optionally include the AAOS Unbundled projects if `unbundledAAOSDir` is set.

val unbundledAAOSDir: String? by settings

if (unbundledAAOSDir.isNullOrBlank()) {
    logger.warn("unbundledAAOSDir not set, cannot include MediaCompose in the project")
} else {
    logger.warn("unbundledAAOSDir set, including MediaCompose in the project")
    logger.warn("See reference-apps/aaos-unbundled/README.md for set-up")

    val unbundledRepo = File(unbundledAAOSDir, "out/aaos-apps-gradle-build/unbundled_m2repo")
    if (unbundledRepo.exists()) {

        // DO NOT SUBMIT Disabled for now
        //        include(":media-lib")
        //        project(":media-lib").projectDir = File("reference-apps/aaos-unbundled/media")
        //        include(":mediacompose-app")
        //        project(":mediacompose-app").projectDir =
        // File("reference-apps/aaos-unbundled/mediacompose")

        dependencyResolutionManagement { repositories { maven(uri(unbundledRepo)) } }
    } else {
        throw GradleException(
            "Cannot find compiled Unbundled libraries, cannot proceed with build.\n" +
                "Make sure your Unbundled repo is up to date, then \n" +
                "go to $unbundledAAOSDir/packages/apps/Car/libs/aaos-apps-gradle-project \n" +
                "and run:\n" +
                "./gradlew publishAllPublicationsToLocalRepository"
        )
    }
}

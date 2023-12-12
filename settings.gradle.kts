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

rootProject.name = "DesignCompose"

// Published modules
include("designcompose")

include("common")

include("annotation")

include("codegen")

include("test")

// Internal support
include("test:internal")

// Integration (and benchmark) tests
include("integration-tests:app-common")

include("validation-app")

project(":validation-app").projectDir = File("integration-tests/validation")

include("battleship-app")

project(":battleship-app").projectDir = File("integration-tests/benchmarks/battleship/app")

include("integration-tests:benchmarks:battleship:lib")

include("integration-tests:benchmarks:battleship:benchmark")

// Reference apps (Can only use published libraries)
include("helloworld-app")

project(":helloworld-app").projectDir = File("reference-apps/helloworld/app")

include("tutorial-app")

project(":tutorial-app").projectDir = File("reference-apps/tutorial/app")

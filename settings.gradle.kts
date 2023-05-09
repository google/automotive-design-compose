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

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
    includeBuild("plugins")
    includeBuild("build-logic")
}

rootProject.name = "DesignCompose"

include(":designcompose")

include(":annotation")

include(":codegen")

include(":common")

include(":validation-app")

project(":validation-app").projectDir = File("integration-tests/validation")

include(":helloworld-app")

project(":helloworld-app").projectDir = File("reference-apps/helloworld")

include(":tutorial-app")

project(":tutorial-app").projectDir =
    File(
        "reference-apps/tutorial/app"
    )

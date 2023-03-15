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

rootProject.name = "DC Unbundled Apps"

apply("unbundled-settings.gradle.kts")

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// Reference apps
include(":media")

include(":mediacompose")

include(":cluster")

// Re-imports of projects needed for our reference apps
fun includeDir(settings: Settings, gradlePath: String, projectDir: File) {
    settings.include(gradlePath)
    settings.project(gradlePath).projectDir = projectDir
}

includeDir(settings, ":common", File("../../common"))

includeDir(settings, ":annotation", File("../../annotation"))

includeDir(settings, ":designcompose", File("../../designcompose"))

includeDir(settings, ":codegen", File("../../codegen"))

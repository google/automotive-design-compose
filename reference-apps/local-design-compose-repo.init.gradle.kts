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

settingsEvaluated {
    logger.warn(
        "Init script is overwriting declared repositories to use a local DesignCompose maven repo"
    )
    val DesignComposeMavenRepo: String by settings
    dependencyResolutionManagement {
        repositories.clear()
        repositories {
            maven(uri(DesignComposeMavenRepo)) {
                content { includeGroup("com.android.designcompose") }
            }

            google() { content { excludeGroupByRegex("com\\.android\\.designcompose.*") } }

            mavenCentral()
        }
    }
    pluginManagement {
        repositories.clear()
        repositories {
            maven(uri(DesignComposeMavenRepo)) {
                content { includeGroup("com.android.designcompose") }
            }

            google() { content { excludeGroupByRegex("com\\.android\\.designcompose.*") } }

            gradlePluginPortal()
        }
    }
}

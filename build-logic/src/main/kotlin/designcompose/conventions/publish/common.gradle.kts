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

import org.gradle.accessors.dm.LibrariesForLibs

plugins { `maven-publish` }

group = "com.android.designcompose"

// This serves as a proxy for the regular way to fetch Gradle version catalogs, which
// aren't available inside a pre-built plugin
val libs = the<LibrariesForLibs>()

// LINT.IfChange
val dcRootProjName = "DesignCompose"
// LINT.ThenChange(settings.gradle.kts)

// LINT.IfChange
val dcPluginsRootProjName = "DesignCompose Plugins"
// LINT.ThenChange(plugins/settings.gradle.kts)

version =
    (project.findProperty("designComposeReleaseVersion") ?: libs.versions.designcompose.get())
        .toString()
        .trimStart('v')

publishing {
    repositories {
        // Configurable location to output a flat-file repository
        // Set the DesignComposeMavenRepo Gradle Property to a directory to publish the library
        // there.
        // The standalone sample projects will also use the property to find the libraries.
        // See `dev-scripts/test-standalone-projects.sh` for an example.
        val DesignComposeMavenRepo: String? by project

        // Check if this project is an includedBuild (i.e., it has a parent project).
        if (rootProject.name == dcRootProjName) {
            // This block will execute if the current project is part of the root project (it has no
            // parent).
            // Configure a Maven repository named "localDir" for the root project.
            maven {
                name = "localDir"
                // Set the URL of the repository.
                // It prioritizes the 'DesignComposeMavenRepo' property if it's set.
                // If not set, it defaults to a directory named 'designcompose_m2repo' within the
                // root project's settings directory.
                // Using `project.layout.settingsDirectory` here ensures the path is relative to the
                // settings file location for the root project.
                url =
                    uri(
                        DesignComposeMavenRepo
                            ?: rootProject.layout.buildDirectory.dir("build/designcompose_m2repo")
                    )
            }
        } else if (rootProject.name == dcPluginsRootProjName && project.gradle.parent != null) {
            // It's an includedBuild, so place the repo inside the parent project's build dir
            val parent = project.gradle.parent!!
            // An included build is being configured before it's parent's settings has finished
            // being evaluated. Therefore we need to use the `parent.settingsEvaluated` callback to
            // only configure maven block after the root project's properties are available.
            parent.settingsEvaluated {
                // Configure a Maven repository named "localDir" for this subproject.
                maven {
                    name = "localDir"
                    // Set the URL of the repository.
                    // It prioritizes the 'DesignComposeMavenRepo' property if it's set.
                    // If not set, it defaults to a directory named 'designcompose_m2repo' within
                    // the root project's build directory.
                    url =
                        uri(DesignComposeMavenRepo ?: rootDir.resolve("build/designcompose_m2repo"))
                }
            }
        }
        // The 'maven' repository configuration will automatically create Gradle tasks
        // like `publishAllPublicationsToLocalDirRepository`
        // These tasks will publish the project's artifacts to the configured local Maven
        // repository.
    }
}

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

        // The default publish dir is meant to be within the main build's buildDir.
        // If the publishing project (like the plugin) is an included build then put the output in the parent's buildDir
        val defaultPublishDir=gradle.parent?.rootProject?.buildDir?: rootProject.buildDir

        // This will create the `publish*ToLocalDirRepository` tasks
        maven {
            name = "localDir"
            url = uri(DesignComposeMavenRepo ?: File(defaultPublishDir, "designcompose_m2repo"))
        }
    }
}

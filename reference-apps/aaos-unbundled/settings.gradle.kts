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

val unbundledAAOSDir: String by settings
val DesignComposeMavenRepo: String? by settings
val unbundledAAOSAndroidGradlePluginVer = "7.1.2"
val aaosLatestSDK = "32"

includeBuild("$unbundledAAOSDir/packages/apps/Car/libs/aaos-apps-gradle-project") {
    dependencySubstitution {
        substitute(module("com.android.car-ui-lib:car-ui-lib")).using(project(":car-ui-lib"))
        substitute(module("com.android.car-apps-common:car-apps-common"))
            .using(project(":car-apps-common"))
        substitute(module("com.android.car-media-common:car-media-common"))
            .using(project(":car-media-common"))
    }
}

/* Note about how this project finds the DesignCompose SDK:

By default: The SDK is fetched from Google's Maven repository
To use a pre-built DesignCompose SDK: Set the DesignComposeMavenRepo Gradle Property to the path to the pre-built Maven directory
To use the SDK from the local source, Set DesignComposeMavenRepo like above and also set alwaysRebuildDesignComposeSdk (to anything). The SDK will be built by a task in the buildSrc project, which ensures the repository exists before the root project checks for it.
 */

@Suppress("UnstableApiUsage") // For versionCatalogs and repositories
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {

        if (!DesignComposeMavenRepo.isNullOrBlank()) {
            logger.lifecycle("Using DesignCompose SDK from $DesignComposeMavenRepo")
            maven(uri(DesignComposeMavenRepo!!)) {
                content { includeGroup("com.android.designcompose") }
            }
            google() { content { excludeGroupByRegex("com\\.android\\.designcompose.*") } }
        } else {
            google()
        }
        mavenCentral()
    }
    versionCatalogs {
        create("unbundledLibs") {
            from(files("gradle/libs.versions.toml"))
            // Version overrides used for the unbundled apps, which include the Unbundled AAOS repo
            // and must match certain key versions These versions must match the version of the
            // Android Gradle Plugin used in the AAOS Unbundled repo Version can be found in
            // `packages/apps/Car/libs/aaos-apps-gradle-project/build.gradle` of the repo TODO:
            // parse out the version the version from that file
            println(
                "Reminder! Overriding Android Gradle Plugin version to $unbundledAAOSAndroidGradlePluginVer to match the Unbundled AAOS project!"
            )
            version("android.gradlePlugin", unbundledAAOSAndroidGradlePluginVer)
            version("aaosLatestSDK", aaosLatestSDK)
            // Use the latest published version of the SDK
            version("designcompose", "+")
        }
    }
}


// Reference apps
include(":media")

include(":mediacompose")

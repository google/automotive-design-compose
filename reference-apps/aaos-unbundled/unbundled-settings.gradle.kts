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

val unbundledAAOSDir: String by settings

includeBuild("$unbundledAAOSDir/packages/apps/Car/libs/aaos-apps-gradle-project") {
    dependencySubstitution {
        substitute(module("com.android.car-ui-lib:car-ui-lib")).using(project(":car-ui-lib"))
        substitute(module("com.android.car-apps-common:car-apps-common"))
            .using(project(":car-apps-common"))
        substitute(module("com.android.car-media-common:car-media-common"))
            .using(project(":car-media-common"))
    }
}

// TODO: Detect these from the unbundled project
val unbundledAAOSAndroidGradlePluginVer = "7.1.2"
val aaosLatestSDK = "32"

@Suppress("UnstableApiUsage") // For versionCatalogs and repositories
dependencyResolutionManagement {
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
        }
    }
}

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

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // These are plugins that are published as external jars, integrating directly into the
        // build scripts
        classpath(libs.dokka.gradlePlugin)
        classpath(libs.android.gms.strictVersionMatcher)
    }
}

plugins {
    id("designcompose.conventions.base")
    id("designcompose.conventions.roborazzi") apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidTest) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.roborazzi) apply false
}

// Apply some of our convention plugins to the reference apps

for (projectName in listOf("tutorial-app", "helloworld-app", "cluster-demo", "mediacompose-app")) {
    if (projectName in childProjects) {
        project(projectName).plugins.apply("designcompose.conventions.base")
        project(projectName).plugins.apply("designcompose.conventions.roborazzi")
    }
}

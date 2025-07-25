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

plugins {
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.ksp)
    alias(libs.plugins.designcompose)
    alias(libs.plugins.jetbrains.compose)
    id("designcompose.conventions.base")
    id("designcompose.conventions.roborazzi")
    id("com.android.designcompose.internal")
    jacoco
}

var applicationID = "com.android.designcompose.testapp.validation"

android {
    namespace = applicationID
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = applicationID
        minSdk = libs.versions.appMinSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        designcompose.figmaToken.orNull?.let {
            testInstrumentationRunnerArguments["FIGMA_ACCESS_TOKEN"] = it
        }
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        // We use a bundled debug keystore, to allow debug builds from CI to be upgradable
        named("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.getByName("debug") }

        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    packaging { resources { excludes.add("/META-INF/{AL2.0,LGPL2.1}") } }
}

dependencies {
    implementation(project(":designcompose"))
    implementation(project(":integration-tests:benchmarks:battleship:lib"))
    implementation(project(":integration-tests:app-common"))
    ksp(project(":codegen"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.material)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(testFixtures(project(":designcompose")))
    testImplementation(testFixtures(project(":integration-tests:app-common")))
    testImplementation(project(":test"))
    testImplementation(project(":test:internal"))
    testImplementation(kotlin("test"))
    testImplementation(libs.google.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit)
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.androidx.compose.ui.test.junit4)

    androidTestImplementation(testFixtures(project(":integration-tests:app-common")))
    androidTestImplementation(testFixtures(project(":designcompose")))
    androidTestImplementation(project(":test"))
    androidTestImplementation(kotlin("test"))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.google.truth)
    androidTestImplementation(libs.androidx.compose.ui.tooling)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.espresso.intents)
    androidTestImplementation(libs.androidx.rules)
}

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
    kotlin("android")
    id("com.android.application")
    @Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
    alias(libs.plugins.ksp)
    id("designcompose.conventions.base")
    alias(libs.plugins.designcompose)
    alias(libs.plugins.roborazzi)
}

var applicationID = "com.android.designcompose.testapp.helloworld"

@Suppress("UnstableApiUsage")
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
        if (designcompose.figmaToken.isPresent) {
            testInstrumentationRunnerArguments["FIGMA_ACCESS_TOKEN"] =
                designcompose.figmaToken.get()
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
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures { compose = true }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidx.compose.compiler.get()
    }

    packaging.resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    testOptions.unitTests.isIncludeAndroidResources = true // For Roborazzi
}

dependencies {
    implementation(project(":designcompose"))
    androidTestImplementation(testFixtures(project(":designcompose")))
    ksp(project(":codegen"))

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.material)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit)
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.androidx.compose.ui.test.junit4)

    androidTestImplementation(kotlin("test"))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.google.truth)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

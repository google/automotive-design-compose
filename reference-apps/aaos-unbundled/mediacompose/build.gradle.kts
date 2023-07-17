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
    alias(libs.plugins.designcompose)

}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(11)) } }

var applicationID = "com.android.designcompose.reference.mediacompose"

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
        vectorDrawables.useSupportLibrary = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    signingConfigs {
        register("platform_UNSECURE") {
            // Use AOSP's platform key for signing. This is a well-known key which must not be
            // used for production. We use it here to make it easier to integrate with AOSP
            // emulator builds.
            //
            // For a production project, you should replace this with your own platform key.
            storeFile = project.layout.projectDirectory.file("../aosp_platform.keystore").asFile
            storePassword = "UNSECURE"
            keyAlias = "platform"
            keyPassword = "UNSECURE"
        }
    }
    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.getByName("platform_UNSECURE") }

        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("platform_UNSECURE")
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

    // Deprecated in AGP 8+, replaced by `packaging`
    @Suppress("DEPRECATION")
    packagingOptions { resources { excludes.add("/META-INF/{AL2.0,LGPL2.1}") } }
}

dependencies {
    implementation(libs.designcompose)
    ksp(libs.designcompose.codegen)
    implementation(project(":media-lib"))

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.material)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

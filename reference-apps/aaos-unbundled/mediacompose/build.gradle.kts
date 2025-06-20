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
    id("com.android.application")
    alias(libs.plugins.ksp)
    alias(libs.plugins.designcompose)
    alias(libs.plugins.jetbrains.compose)
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

var applicationID = "com.android.designcompose.reference.mediacompose"

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
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    // Deprecated in AGP 8+, replaced by `packaging`
    @Suppress("DEPRECATION")
    packagingOptions { resources { excludes.add("/META-INF/{AL2.0,LGPL2.1}") } }

    useLibrary("android.car")
}

val unbundledAAOSDir: String? by project

if (unbundledAAOSDir == null) throw GradleException("unbundledAAOSDir is not set")

dependencies {
    implementation(libs.designcompose)
    ksp(libs.designcompose.codegen)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.material)

    implementation("com.android.car:car-apps-common:UNBUNDLED")
    api("com.android.car:car-media-common:UNBUNDLED")
    compileOnly(
        files(
            "$unbundledAAOSDir/prebuilts/sdk/${ libs.versions.unbundledStubsSdk.get().toInt()}/system/android.car-system-stubs.jar"
        )
    )

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

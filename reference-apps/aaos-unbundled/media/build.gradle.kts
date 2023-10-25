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
    id("com.android.library")
    @Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
    alias(libs.plugins.ksp)
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(11)) } }

@Suppress("UnstableApiUsage")
android {
    namespace = "com.android.designcompose.reference.media"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.appMinSdk.get().toInt()
        consumerProguardFiles("consumer-proguard-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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

    implementation(platform(libs.androidx.compose.bom))

    api(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.legacy.support.v4)

    // The following dependencies are provided by the unbundled aaos repository
//    val unbundledAAOSDir: String by project
//    implementation(
//        files(
//            "$unbundledAAOSDir/prebuilts/sdk/${unbundledLibs.versions.aaosLatestSDK.get()}/system/android.car-system-stubs.jar"
//        )
//    )
    implementation("com.android.car:car-apps-common:UNBUNDLED")
    api("com.android.car:car-media-common:UNBUNDLED")

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

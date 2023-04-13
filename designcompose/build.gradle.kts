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
    id("org.mozilla.rust-android-gradle.rust-android")
    // Plugins from our buildSrc
    id("designcompose.conventions.publish.android")
    id("designcompose.conventions.android-test-devices")
}

@Suppress("UnstableApiUsage")
android {
    namespace = "com.android.designcompose"
    compileSdk = libs.versions.compileSdk.get().toInt()
    ndkVersion = "25.2.9519653"

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-proguard-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { compose = true }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidx.compose.compiler.get()
    }
    packagingOptions {
        resources.excludes.apply {
            add("META-INF/LICENSE*")
            add("META-INF/AL2.0")
            add("META-INF/LGPL2.1")
        }
    }
}

// Syntax: https://github.com/mozilla/rust-android-gradle
// Defines the configuation for the Rust JNI build
cargo {
    module = "../crates/live_update"
    targetDirectory = "../target"
    targetIncludes = arrayOf("liblive_update.so")
    libname = "figma_import"
    targets =
        listOf(
            "arm", // Older Physical Android Devices
            "arm64", // Recent Physical Android Devices
            "x86", // Older Emulated devices, including the ATD Android Test device
            "x86_64", // Most Emulated Android Devices
        )

    if (hasProperty("liveUpdateJNIReleaseBuild")) {
        println("Building the release profile of the Live Update JNI")
        profile = "release"
    }
}

android.sourceSets.getByName("androidTest") { jniLibs.srcDir("$buildDir/rustJniLibs/android") }

// Must manually configure that Android build to depend on the JNI artifacts
tasks.withType<com.android.build.gradle.tasks.MergeSourceSetFolders>().configureEach {
    if (this.name.contains("Jni")) {
        this.dependsOn(tasks.named("cargoBuild"))
    }
}

dependencies {
    api(project(":common"))
    api(project(":annotation"))
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.foundation.layout)
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.runtime.livedata)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.text)
    api(libs.androidx.lifecycle.runtime)
    api(libs.androidx.lifecycle.livedata.core)
    implementation(libs.accompanist.flowlayout)
    implementation(libs.grpc.auth)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.okhttp)
    implementation(libs.grpc.protobufLite)
    implementation(libs.guavaAndroid)
    implementation(libs.javax.annotationApi)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.compose)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.mockk.agent)
    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

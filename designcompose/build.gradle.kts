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
    id("com.android.designcompose.rust-in-android")
    // Plugins from our buildSrc
    id("designcompose.conventions.base")
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
        System.getenv("FIGMA_ACCESS_TOKEN")?.let {
            testInstrumentationRunnerArguments["FIGMA_ACCESS_TOKEN"] = it
        }
    }

    buildFeatures { compose = true }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidx.compose.compiler.get()
    }
    packaging {
        resources.excludes.apply {
            add("META-INF/LICENSE*")
            add("META-INF/AL2.0")
            add("META-INF/LGPL2.1")
        }
    }
}

// To simplify publishing of the entire SDK, make the DesignCompose publish tasks depend on the
// Gradle Plugin's publish tasks
// Necessary because the plugin must be in a separate Gradle build
listOf("publish", "publishToMavenLocal", "publishAllPublicationsToLocalDirRepository").forEach {
    tasks.named(it) { dependsOn(gradle.includedBuild("plugins").task(":gradle-plugin:${it}")) }
}

// Defines the configuration for the Rust JNI build
cargo {
    crateDir.set(File(rootProject.relativePath("../crates/live_update")))
    abi.add("x86") // Older Emulated devices, including the ATD Android Test device
    abi.add("x86_64") // Most Emulated Android Devices
    abi.add("armeabi-v7a")
    abi.add("arm64-v8a")
}

dependencies {
    api(project(":common"))
    api(project(":annotation"))
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.foundation.layout)
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.runtime.livedata)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.text)
    api(libs.androidx.lifecycle.runtime)
    api(libs.androidx.lifecycle.livedata.core)
    implementation(libs.accompanist.flowlayout)
    implementation(libs.guavaAndroid)
    implementation(libs.grpc.stub)
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
    implementation(kotlin("test"))
}

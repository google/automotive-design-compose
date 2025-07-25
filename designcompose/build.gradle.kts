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
    alias(libs.plugins.ksp) // For testing a fetch of HelloWorld
    alias(libs.plugins.designcompose) // Allows us to get the Figma Token
    alias(libs.plugins.jetbrains.compose)

    // Plugins from our buildSrc
    id("designcompose.conventions.base")
    id("designcompose.conventions.publish.android")
    id("designcompose.conventions.roborazzi")
    id("com.android.designcompose.internal")
    jacoco
}

@Suppress("UnstableApiUsage")
android {
    namespace = "com.android.designcompose"
    compileSdk = libs.versions.compileSdk.get().toInt()
    ndkVersion = libs.versions.ndk.get()

    testFixtures { enable = true }

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-proguard-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        if (designcompose.figmaToken.isPresent) {
            testInstrumentationRunnerArguments["FIGMA_ACCESS_TOKEN"] =
                designcompose.figmaToken.get()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            matchingFallbacks.add("release")
        }
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
    crateDir.set(File(rootProject.relativePath("../crates/dc_jni")))
    abi.add("x86") // Older Emulated devices, including the ATD Android Test device
    abi.add("x86_64") // Most Emulated Android Devices
    abi.add("armeabi-v7a")
    abi.add("arm64-v8a")
}

dependencies {
    // Our code
    api(project(":common"))
    api(project(":annotation"))

    ksp(libs.designcompose.codegen)

    // The following dependencies are required to support the code generated by
    // the codegen library, and so are included in the `api` configuration. Meaning they are
    // included in the POM for DesignCompose as transitive dependencies
    // (Ideally these would be dependencies of codegen but it does not build an android library)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.text)

    // Dependencies that
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.guavaAndroid)
    implementation(libs.androidx.tracing.ktx)
    implementation(libs.androidx.compose.runtime.tracing)
    implementation(libs.protobuf.kotlin.lite)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(testFixtures(project(":designcompose")))
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

    androidTestImplementation(project(":test"))
    androidTestImplementation(kotlin("test"))
    androidTestImplementation(libs.google.truth)
    androidTestImplementation(libs.mockk.agent)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    testFixturesImplementation(libs.androidx.test.ext.junit)
}

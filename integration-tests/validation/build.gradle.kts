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

// Hacky: GH-502
import com.android.designcompose.cargoplugin.CargoBuildType
import com.android.designcompose.cargoplugin.getHostCargoOutputDir

evaluationDependsOn(":designcompose")
// End Hacky
plugins {
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.ksp)
    alias(libs.plugins.designcompose)
    id("designcompose.conventions.base")
    id("designcompose.conventions.android-test-devices")
    id("designcompose.conventions.roborazzi")
    id("com.android.designcompose.internal")
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
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures { compose = true }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidx.compose.compiler.get()
    }

    packaging { resources { excludes.add("/META-INF/{AL2.0,LGPL2.1}") } }

    // Hacky: GH-502
    testOptions {
        unitTests {
            all { test ->
                // hacky
                val dcProject = project(":designcompose")
                test.dependsOn(dcProject.tasks.named("cargoBuildHostDebug").get())
                test.systemProperty(
                    "java.library.path",
                    dcProject.getHostCargoOutputDir(CargoBuildType.DEBUG).get().asFile.absolutePath
                )
            }
        }
    }
    // End Hacky

}

dependencies {
    implementation(project(":designcompose"))
    implementation(project(":test:internal"))
    implementation(project(":benchmarks:battleship:lib"))
    testImplementation(project(":designcompose"))
    ksp(project(":codegen"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.material)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(testFixtures(project(":designcompose")))
    testImplementation(kotlin("test"))
    testImplementation(libs.google.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit)
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.androidx.compose.ui.test.junit4)

    androidTestImplementation(testFixtures(project(":designcompose")))
    androidTestImplementation(kotlin("test"))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.google.truth)
    androidTestImplementation(libs.androidx.compose.ui.tooling)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.rules)
}

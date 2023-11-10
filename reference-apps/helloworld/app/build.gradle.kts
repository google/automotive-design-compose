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
// import com.android.designcompose.cargoplugin.CargoBuildType
// import com.android.designcompose.cargoplugin.getHostCargoOutputDir

// evaluationDependsOn(":designcompose")
// End Hacky
plugins {
    kotlin("android")
    id("com.android.application")
    alias(libs.plugins.ksp)
    alias(libs.plugins.designcompose)
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

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

        vectorDrawables.useSupportLibrary = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
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

    testOptions.unitTests {
        isIncludeAndroidResources = true // For Roborazzi
    }
    /* Disabled for now, need to make a designcompsoe test library
        // Hacky: GH-502
        testOptions {
            unitTests {
                all { test ->
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
    */
}

afterEvaluate {
    tasks.withType<Test>().configureEach {
        logger.warn(
            "Disabling ${project.name}:$name. Cannot support unit tests in standalone apps at this time."
        )
        enabled = false
    }
}

dependencies {
    implementation(libs.designcompose)
    ksp(libs.designcompose.codegen)

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
}

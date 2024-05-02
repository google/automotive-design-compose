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
    `kotlin-dsl`
    id("com.diffplug.spotless") version "6.25.0"
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktfmt(libs.versions.ktfmt.get()).kotlinlangStyle()
    }
    kotlinGradle { ktfmt(libs.versions.ktfmt.get()).kotlinlangStyle() }
}
kotlin { jvmToolchain(libs.versions.jvmToolchain.get().toInt())}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.dokka.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.ksp.gradlePlugin)
    implementation(libs.roborazzi.gradlePlugin)
    implementation(libs.android.gms.strictVersionMatcher)
    implementation(libs.spotless.plugin.gradle)
    // Allows the precompiled scripts to access our local directory, specifically to
    // Access the version catalog
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

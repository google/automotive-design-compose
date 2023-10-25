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
import com.ncorti.ktfmt.gradle.tasks.KtfmtBaseTask

plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktfmt)
}

ktfmt { kotlinLangStyle() }

tasks.withType<KtfmtBaseTask> {
    // For some reason Ktfmt has problems filtering out generated kotlin files from the build dir
    exclude { it.file.path.contains("/build/") }
}

repositories {
    google()
    // for kotlin-dsl plugin
    gradlePluginPortal()
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.dokka.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.ktfmt.gradlePlugin)
    implementation(libs.ksp.gradlePlugin)
    implementation(libs.roborazzi.gradlePlugin)
    implementation(libs.android.gms.strictVersionMatcher)
    // Allows the precompiled scripts to access our local directory, specifically to
    // Access the version catalog
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

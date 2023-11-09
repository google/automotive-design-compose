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

import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // These are plugins that are published as external jars, integrating directly into the
        // build scripts
        classpath(libs.dokka.gradlePlugin)
        classpath(libs.android.gms.strictVersionMatcher)
    }
}

plugins {
    id("designcompose.conventions.base")
    id("designcompose.conventions.android-test-devices") apply false
    id("designcompose.conventions.roborazzi") apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.roborazzi) apply false
}

// Format all *.gradle.kts files in the repository. This should catch all buildscripts.
// This task must be run on it's own, since it modifies the build scripts for everything else and
// Gradle throws an error.
tasks.register<KtfmtFormatTask>("ktfmtFormatBuildScripts") {
    source = project.layout.projectDirectory.asFileTree
    exclude { it.path.contains("/build/") }
    include("**/*.gradle.kts")
    notCompatibleWithConfigurationCache(
        "Doesn't seem to read the codestyle set in the ktfmt extension"
    )
    doFirst {
        @Suppress("UnstableApiUsage")
        if (this.project.gradle.startParameter.isConfigurationCacheRequested) {
            throw GradleException(
                "This task will not run properly with the Configuration Cache. " +
                    "You must rerun with '--no-configuration-cache'"
            )
        }
    }
}

tasks.named("ktfmtCheck") { dependsOn(gradle.includedBuilds.map { it.task(":ktfmtCheck") }) }

tasks.named("ktfmtFormat") { dependsOn(gradle.includedBuilds.map { it.task(":ktfmtFormat") }) }

// Apply some of our convention plugins to the tutorial app
subprojects {
    if (listOf("tutorial-app", "helloworld-app").contains(name)) {
        plugins.apply("designcompose.conventions.base")
        plugins.apply("designcompose.conventions.android-test-devices")
        plugins.apply("designcompose.conventions.roborazzi")
    }
}

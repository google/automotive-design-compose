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

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // These are plugins that are published as external jars, integrating directly into the
        // build scripts
        classpath(unbundledLibs.android.gradlePlugin)
        classpath(libs.kotlin.gradlePlugin)
        classpath(libs.dokka.gradlePlugin)
        classpath(libs.android.gms.strictVersionMatcher)
    }
}

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.ktfmt) apply false
    alias(libs.plugins.versions)
    alias(libs.plugins.versionCatalogUpdate)
}

versionCatalogUpdate {
    keep {
        // The version catalog plugin seems to be very aggressive in removing versions it things are
        // unused.
        keepUnusedVersions.set(true)
        keepUnusedLibraries.set(true)
    }
}

// Function to determine whether a release is final or not.
fun String.isNonStable(): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(this)
    return isStable.not()
}

// https://github.com/ben-manes/gradle-versions-plugin
// Prevent the verisons plugin from updating versions to non-stable releases,
// unless the dependency is already using a non-stable release
tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf { candidate.version.isNonStable() && !currentVersion.isNonStable() }
}

subprojects {
    apply {
        plugin(rootProject.libs.plugins.ktfmt.get().pluginId)
        plugin(rootProject.libs.plugins.strictVersionMatcher.get().pluginId)
    }
    configure<com.ncorti.ktfmt.gradle.KtfmtExtension> { kotlinLangStyle() }

    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
    }
}

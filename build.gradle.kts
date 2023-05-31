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
import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // These are plugins that are published as external jars, integrating directly into the
        // build scripts
        classpath(libs.kotlin.gradlePlugin)
        classpath(libs.android.gradlePlugin)
        classpath(libs.android.gms.strictVersionMatcher)
    }
}

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("designcompose.conventions.base")
    alias(libs.plugins.dokka)
    alias(libs.plugins.versions)
    alias(libs.plugins.versionCatalogUpdate)
    alias(libs.plugins.ksp) apply false
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
fun isNonStable(version: String) = "^[0-9,.v-]+(-r)?$".toRegex().matches(version).not()

// https://github.com/ben-manes/gradle-versions-plugin
// Prevent the versions plugin from updating versions to non-stable releases,
// unless the dependency is already using a non-stable release
tasks.withType<DependencyUpdatesTask> { rejectVersionIf { isNonStable(candidate.version) } }

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

tasks.dokkaGfmMultiModule { outputDirectory.set(projectDir.resolve("docs/dokka")) }

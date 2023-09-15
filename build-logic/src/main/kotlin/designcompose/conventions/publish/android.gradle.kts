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

package designcompose.conventions.publish

plugins {
    kotlin("android")
    id("com.android.library")
    id("org.jetbrains.dokka")
    id("designcompose.conventions.publish.common")
}

android {
    publishing {
        multipleVariants {
            allVariants()
            withJavadocJar()
            withSourcesJar()
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            pom { basePom() }
            afterEvaluate { from(components["default"]) }
            // Gradle complains that Test Fixtures aren't compatible with older versions of maven,
            // which we don't care about
            suppressPomMetadataWarningsFor("debugTestFixturesVariantDefaultApiPublication")
            suppressPomMetadataWarningsFor("debugTestFixturesVariantDefaultRuntimePublication")
            suppressPomMetadataWarningsFor("releaseTestFixturesVariantDefaultApiPublication")
            suppressPomMetadataWarningsFor("releaseTestFixturesVariantDefaultRuntimePublication")
        }
    }
}

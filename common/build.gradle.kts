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

import com.google.protobuf.gradle.id

plugins {
    id("java-library")
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.protobuf)

    id("designcompose.conventions.base")
    id("designcompose.conventions.publish.jvm")
    idea // required so that Android Studio will detect the protobuf files correctly
}

kotlin { jvmToolchain(11) }

publishing {
    publications.named<MavenPublication>("release") {
        pom {
            name.set("Automotive Design for Compose Common Library")
            description.set("Common code used by other DesignCompose libraries")
        }
    }
}

sourceSets {
    test {
        resources.srcDirs(rootProject.rootDir.resolve("designcompose/src/main/assets"))
        resources.srcDirs(
            rootProject.rootDir.resolve("integration-tests/validation/src/main/assets")
        )
    }
}

// Protobuf configuration
project.sourceSets.main {
    proto { srcDir(rootProject.layout.projectDirectory.dir("crates/dc_bundle/src/proto")) }
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:${libs.versions.protoc.get()}" }
    generateProtoTasks {
        all().forEach {
            it.builtins {
                get("java").option("lite")
                id("kotlin") { option("lite") }
            }
        }
    }
}

dependencies {
    api(libs.javax.annotationApi)
    implementation(libs.kotlin.stdlib)
    implementation(libs.protobuf.kotlin.lite)
    testImplementation(libs.junit.junit)
    testImplementation(libs.google.truth)
    testImplementation(kotlin("test"))
    testImplementation(libs.mockito.core)
}

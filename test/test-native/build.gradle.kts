/*
 * Copyright 2024 Google LLC
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
    kotlin("jvm")
    `java-library`
    id("com.android.designcompose.rust-in-android")
    id("designcompose.conventions.base")
    id("designcompose.conventions.publish.jvm")
}

// Note: There is currently no Java code in this artifact - only the
// JNi Libraries

publishing {
    publications.named<MavenPublication>("release") {
        pom {
            name.set("Automotive Design for Compose Native Libraries")
            description.set("Prebuilt Rust JNI libraries for DesignCompose")
        }
    }
}

// Defines the configuration for the Rust JNI build
cargo {
    crateDir.set(File(rootProject.relativePath("../../crates/dc_jni")))
    buildHost.set(true)
}

sourceSets {
    main {
        resources {
            // Add the built JNI libraries to our resources
            tasks.named { it.contains("cargoBuildHostDebug") }.forEach { this.srcDir(it.outputs) }
        }
    }
}

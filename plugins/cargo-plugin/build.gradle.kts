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
    groovy
    `java-gradle-plugin`
    id("designcompose.conventions.base")
    kotlin("jvm")
}

gradlePlugin {
    plugins {
        create("cargoPlugin") {
            id = "com.android.designcompose.rust-in-android"
            implementationClass = "com.android.designcompose.cargoplugin.CargoPlugin"
        }
    }
}

tasks.withType(Test::class.java).configureEach {
    useJUnitPlatform()
    testLogging.events("passed")
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.google.truth)
}

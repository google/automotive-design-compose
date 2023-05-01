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

sourceSets {
    test {
        resources {
            srcDir("src/test/gradle")
            exclude(
                "**/build/",
                "**/.gradle/",
                "**/.idea/",
                "**/rust/**/target/",
                "**/gradle/",
                "**/gradlew*",
                "../local.properties"
            )
        }
    }
}

tasks.register<Test>("EmulatorTest") {
    systemProperty("org.gradle.testing.runSlow", "true")
    failFast = true
}

tasks.withType<Test> {
    group = "verification"
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    useJUnitPlatform()
    systemProperty("org.gradle.testing.underTest", "true")
    testLogging { showStandardStreams = true }
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)

    testImplementation(libs.junit)
    testImplementation("com.google.truth:truth:1.1.3")

    //    "integrationTestImplementation"(project)
    testImplementation("org.spockframework:spock-core:2.3-groovy-3.0") {
        exclude(group = "org.codehaus.groovy")
    }
}

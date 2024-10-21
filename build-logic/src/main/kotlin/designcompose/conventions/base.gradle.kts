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

package designcompose.conventions

import com.google.devtools.ksp.gradle.KspTask
import org.gradle.api.internal.artifacts.dsl.dependencies.DependenciesExtensionModule.module
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin

plugins {
    id("com.diffplug.spotless")
    id("com.google.android.gms.strict-version-matcher-plugin")
}

// Keep in sync with gradle/libs.versions.toml
val ktfmtVersion = "0.52"

// Apply Spotless Kotlin configuration to projects with Kotlin
project.plugins.withType(KotlinBasePlugin::class.java) {
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            ktfmt(ktfmtVersion).kotlinlangStyle()
        }
    }
}

// Apply Spotless KotlinScript configuration globally to cover our build scripts
configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlinGradle { ktfmt(ktfmtVersion).kotlinlangStyle() }
}

project.plugins.withType(JavaBasePlugin::class.java) {
    project.extensions.getByType(JavaPluginExtension::class.java).toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

project.plugins.withType(com.android.build.gradle.BasePlugin::class.java) {
    // Replace dependencies on DesignCompose with our project. Because of the way we include our
    // reference apps, we need to only do so the gradle project being run actually includes
    // DesignCompose
    if (findProject(":designcompose") != null) {
        configurations.all {
            resolutionStrategy.dependencySubstitution {
                substitute(module("com.android.designcompose:designcompose"))
                    .using(project(":designcompose"))
                substitute(module("com.android.designcompose:codegen")).using(project(":codegen"))
                substitute(module("com.android.designcompose:test")).using(project(":test"))
            }
        }
    }
}

tasks.withType<KspTask>() { group = "DesignCompose Developer" }

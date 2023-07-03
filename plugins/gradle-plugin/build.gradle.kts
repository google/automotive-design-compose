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

import designcompose.conventions.publish.basePom

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    id("designcompose.conventions.base")
    id("designcompose.conventions.publish.common")
}

gradlePlugin {
    plugins {
        create("designcompose-gradle-plugin") {
            id = "com.android.designcompose"
            displayName = "com.android.designcompose.gradle.plugin"
            implementationClass = "com.android.designcompose.gradle.Plugin"
        }
    }
}

publishing {
    afterEvaluate {
        // The java-gradle-plugin creates two publications, the "main" publication for
        // the plugin's code and the "marker" publication that declares the plugin. Both need to
        // have their POM metadata set.
        // https://docs.gradle.org/current/userguide/java_gradle_plugin.html#maven_publish_plugin
        val configuration: MavenPublication.() -> Unit = {
            pom {
                basePom()
                name.set("Automotive Design for Compose Plugin")
                description.set(
                    "Plugin that adds base configuration and assisting tasks to DesignCompose-enabled apps"
                )
            }
        }
        publications.named("pluginMaven", configuration)
        publications.named("designcompose-gradle-pluginPluginMarkerMaven", configuration)
    }
}

dependencies { compileOnly(libs.android.gradlePlugin.minimumSupportedVersion) }

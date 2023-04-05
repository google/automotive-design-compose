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

@file:Suppress("UnstableApiUsage")

package designcompose.conventions

import com.android.build.api.dsl.ManagedDevices
import com.android.build.api.dsl.ManagedVirtualDevice
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.BasePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.maybeCreate

// We want to apply this to both android libraries and applications, so we can't
// just use the DLS here. Instead we ...
class ATDPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Hook into the project when the base Android plugin is applied
        project.plugins.withType(BasePlugin::class.java) {
            // Then get at the Android Components Extension
            // https://developer.android.com/reference/tools/gradle-api/7.3/com/android/build/api/variant/AndroidComponentsExtension
            val extension = project.extensions.getByType(AndroidComponentsExtension::class.java)
            // And hook into when it's finalizing what it's read from the Android DSL
            // https://developer.android.com/studio/build/extend-agp#finalize-dsl
            extension.finalizeDsl {
                // Configure the managed Devices
                it.testOptions.managedDevices.apply(createTestDevices())
            }
        }
    }

    fun createTestDevices(): ManagedDevices.() -> Unit = {
        // Add a group to hold our new devices
        val tabletAllApisGroup = groups.maybeCreate("tabletAllApis")
        // Configure an ATD:
        // https://developer.android.com/studio/test/gradle-managed-devices
        devices.apply {
            maybeCreate<ManagedVirtualDevice>(
                "tabletAtdApi30"
            )
                .apply {
                    device = "Nexus 10"
                    // ATDs currently support only API level 30.
                    apiLevel = 30
                    systemImageSource = "aosp-atd"
                }
        }
        tabletAllApisGroup.targetDevices.add(devices["tabletAtdApi30"])
        arrayOf(31, 32, 33).forEach { thisApiLevel ->
            val device =
                devices
                    .maybeCreate<ManagedVirtualDevice>(
                        "tabletApi${thisApiLevel}"
                    )
                    .apply {
                        device = "Nexus 10"
                        apiLevel = thisApiLevel
                        systemImageSource = "google"
                    }
            tabletAllApisGroup.targetDevices.add(device)
        }
    }
}

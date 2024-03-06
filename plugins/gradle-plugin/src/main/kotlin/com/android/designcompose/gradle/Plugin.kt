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

package com.android.designcompose.gradle

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
    import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.configurationcache.extensions.capitalized

/**
 * Figma token plugin
 *
 * Registers and configures FigmaTokenTasks for the project it is being applied to. Will only apply
 * to projects that also have the AGP App plugin applied. Configure your Figma token to the
 * environment variable `FIGMA_ACCESS_TOKEN` and call the task for the app. For example:
 *
 * `FIGMA_ACCESS_TOKEN=XXXXXX-XXXXXXXXXX-XXXX ./gradlew ref:helloworld:setFigmaTokenDebug`
 *
 * Calling the `Debug` or `Release` versions only matter if there's a difference in the app's ID.
 *
 * The task will use adb to check whether the app is installed and skip execution if it's not. This
 * allows you to just run `./gradlew setFigmaToken` from the root of the project to configure all
 * installed apps.
 *
 * If you have multiple emulators or devices connected you can run `adb devices` to check their
 * addresses (such as `emulator-5444`) and set the address to the `ANDROID_SERIAL` environment
 * variable.
 *
 * @constructor Create empty Figma token plugin
 */
class Plugin : Plugin<Project> {

    private lateinit var pluginExtension: PluginExtension

    override fun apply(project: Project) {
        pluginExtension = project.extensions.create("designcompose", PluginExtension::class.java)
        project.initializeExtension(pluginExtension)

        project.pluginManager.withPlugin("com.android.application") {
            project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java).let {
                ace ->
                @Suppress("UnstableApiUsage") val adb = ace.sdkComponents.adb
                // Create one task per variant of the app
                ace.onVariants { variant ->
                    project.createSetFigmaTokenTask(variant.name, variant.applicationId, adb)
                }
            }
        }
    }

    /**
     * Create a setFigmaToken task for the given [variant][variantId]
     *
     * @param variantName The AGP-generated name for the application variant
     * @param variantId The Application ID for the variant
     * @param adb Provided by AGP, the path to adb on the system
     */
    private fun Project.createSetFigmaTokenTask(
        variantName: String,
        variantId: Property<String>,
        adb: Provider<RegularFile>
    ) {
        tasks.register("setFigmaToken${variantName.capitalized()}", SetFigmaTokenTask::class.java) {
            it.adbPath.set(adb)
            it.appID.set(variantId)
            it.figmaToken.set(pluginExtension.figmaToken.also { token -> token.finalizeValue() })
            it.group = "DesignCompose"
        }
    }
}

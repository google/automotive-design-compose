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

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
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

        // Create the "umbrella" tasks for the fetch and update tasks
        val umbrellaFetchTask: TaskProvider<Task> =
            project.tasks.register("fetchFigmaFiles") { it.group = "designcompose" }
        val umbrellaFetchAndUpdateTask: TaskProvider<Task> =
            project.tasks.register("fetchAndUpdateFigmaFiles") { it.group = "designcompose" }

        // Configure the fetch tasks for each variant of the android app and library with
        // roborazzi test setup.
        project.pluginManager.withPlugin("designcompose.conventions.roborazzi") {
            project.extensions.getByType(AndroidComponentsExtension::class.java).onVariants {
                variant ->
                val unitTest = variant.unitTest ?: return@onVariants

                project.configureFetchTasks(
                    variant.name.capitalized(),
                    "test${unitTest.name.capitalized()}",
                    umbrellaFetchTask,
                    umbrellaFetchAndUpdateTask,
                )
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
        adb: Provider<RegularFile>,
    ) {
        tasks.register("setFigmaToken${variantName.capitalized()}", SetFigmaTokenTask::class.java) {
            it.adbPath.set(adb)
            it.appID.set(variantId)
            it.figmaToken.set(pluginExtension.figmaToken.also { token -> token.finalizeValue() })
            it.group = "DesignCompose"
        }
    }

    /**
     * Configure fetch tasks
     *
     * (Scoped function) Configure the fetch tasks for the given Android variant
     *
     * @param variantName
     * @param testTaskName
     */
    fun Project.configureFetchTasks(
        variantName: String,
        testTaskName: String,
        umbrellaFetchTask: TaskProvider<Task>,
        umbrellaFetchAndUpdateTask: TaskProvider<Task>,
    ) {
        val figmaTokenProvider = project.objects.property(String::class.java)
        figmaTokenProvider.set(pluginExtension.figmaToken.also { token -> token.finalizeValue() })

        val isFetch = objects.property(Boolean::class.java)
        val isFetchAndSave = objects.property(Boolean::class.java)

        val dcfOutDir = layout.buildDirectory.dir("outputs/designComposeFiles/$variantName/figma")

        // Hard-coded, not ideal, but a cleaner solution would take more work
        val assetsDir = project.layout.projectDirectory.dir("src/main/assets/figma")

        // Reverse engineer the unit test's name
        val testTaskProvider =
            project.tasks.withType(Test::class.java).matching { it.name == testTaskName }

        // Register the fetch tasks.
        // FetchAndUpdate depends on Fetch which depends on the actual test task.
        // These tasks are essentially flags! They don't do anything on their own. But if
        // they're in the task graph for an execution then we'll set some SystemProperties for
        // the test tasks
        val variantFetchTask =
            tasks.register("fetchFigmaFiles$variantName") { it.dependsOn(testTaskProvider) }
        umbrellaFetchTask.dependsOn(variantFetchTask)

        val variantFetchAndUpdateTask =
            tasks.register("fetchAndUpdateFigmaFiles$variantName") {
                it.dependsOn(variantFetchTask)
            }
        umbrellaFetchAndUpdateTask.dependsOn(variantFetchAndUpdateTask)

        // Once Gradle has read the task graph, set the flags based on whether the fetch tasks
        // were requested
        gradle.taskGraph.whenReady { graph ->
            isFetch.set(variantFetchTask.map { graph.hasTask(it) })
            isFetchAndSave.set(variantFetchAndUpdateTask.map { graph.hasTask(it) })
        }

        // Configure the unit test task to pass in the flags to the test
        testTaskProvider.configureEach { test ->
            // Help with caching. We don't want to cache the unit test results if we fetch
            // so add an input property to mark whether we're fetching
            test.inputs.properties(mapOf("isFetch" to isFetch))
            test.outputs.doNotCacheIf("Always fetch DCF files") { isFetch.get() }

            test.doFirst {
                if (isFetch.get()) {
                    test.useJUnit {
                        it.includeCategories("com.android.designcompose.test.Fetchable")
                    }

                    // Make sure we have a figmaToken set
                    val figmaToken =
                        figmaTokenProvider.orNull
                            ?: throw GradleException(
                                """FigmaToken must be set to run a fetch.
                                https://google.github.io/automotive-design-compose/docs/live-update/setup#GetFigmaToken"""
                            )

                    // Clear the results of the previous test run
                    val outDir = dcfOutDir.get().asFile
                    outDir.deleteRecursively()
                    dcfOutDir.get().asFile.mkdirs()

                    test.systemProperty("designcompose.test.fetchFigma", true)
                    test.systemProperty("designcompose.test.figmaToken", figmaToken)
                    test.systemProperty("designcompose.test.dcfOutPath", outDir.absolutePath)
                }
            }
            test.doLast {
                // If we're saving the fetched files, copy them to the assets dir
                if (isFetchAndSave.get()) {
                    dcfOutDir.get().asFile.copyRecursively(assetsDir.asFile, overwrite = true)
                }
            }
        }
    }
}

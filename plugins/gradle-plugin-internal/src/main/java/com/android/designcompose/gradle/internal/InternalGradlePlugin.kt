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

package com.android.designcompose.gradle.internal

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.android.designcompose.gradle.PluginExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.configurationcache.extensions.capitalized

class InternalGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {

        // Create the "umbrella" tasks for the fetch and update tasks
        val umbrellaFetchTask: TaskProvider<Task> =
            project.tasks.register("fetchFigmaFiles") { it.group = "designcompose" }
        val umbrellaFetchAndUpdateTask: TaskProvider<Task> =
            project.tasks.register("fetchAndUpdateFigmaFiles") { it.group = "designcompose" }

        val figmaTokenProvider = project.objects.property(String::class.java)

        /**
         * Configure fetch tasks
         *
         * (Scoped function) Configure the fetch tasks for the given Android variant
         *
         * @param variantName
         * @param testTaskName
         */
        fun Project.configureFetchTasks(variantName: String, testTaskName: String) {
            val isFetch = objects.property(Boolean::class.java)
            val isFetchAndSave = objects.property(Boolean::class.java)

            val dcfOutDir =
                layout.buildDirectory.dir("outputs/designComposeFiles/$variantName/figma")

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
                            it.includeCategories(
                                "com.android.designcompose.testapp.common.Fetchable"
                            )
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

        // Get the FigmaToken out of the DesignCompose plugin
        project.pluginManager.withPlugin("com.android.designcompose") {
            val extension = project.extensions.getByType(PluginExtension::class.java)
            figmaTokenProvider.set(extension.figmaToken.map { it })
        }

        // Configure the fetch tasks for each variant of the android app
        project.pluginManager.withPlugin("com.android.application") {
            project.extensions
                .getByType(ApplicationAndroidComponentsExtension::class.java)
                .onVariants { variant ->
                    val unitTest = variant.unitTest ?: return@onVariants

                    project.configureFetchTasks(
                        variant.name.capitalized(),
                        "test${unitTest.name.capitalized()}",
                    )
                }
        }
    }
}

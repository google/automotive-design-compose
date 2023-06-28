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

package designcompose.features

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.process.internal.DefaultExecOperations

/**
 * Figma token task
 *
 * Base task for setting a user's Figma Access Token in a DesignCompose LiveUpdate-enabled
 * development app
 *
 * @constructor Create empty Figma token task
 * @property executor The Gradle ExecOperations service which provides methods for running other
 *   binaries. Injected by Gradle, used here to run `adb`
 * @property adbPath Path to the adb executable on the system. Should be configured using via AGP's
 *   Android Components Extension
 * @property appID The applicationID of the target app.
 * @property figmaToken The token to set. Expected to be set using a system environment variable
 *   provider
 */
abstract class FigmaTokenTask @Inject constructor(private val executor: DefaultExecOperations) :
    DefaultTask() {

    @get:InputFile abstract val adbPath: RegularFileProperty

    @get:Input abstract val appID: Property<String>

    @get:Input @get:Optional abstract val figmaToken: Property<String>

    /**
     * Check if an app is installed
     *
     * First checks if adb can target a device and returns an exception with a more useful error if
     * not. Check to see whether the [appID] for this task has been installed.
     *
     * Uses adb to run the
     * [package manager](https://developer.android.com/studio/command-line/adb#pm) on the device
     *
     * @return True if the checks pass and adb has a valid device and package target
     */
    private fun isAppInstalled(): Boolean {
        val stdOut = ByteArrayOutputStream()
        val stdErr = ByteArrayOutputStream()
        val execResult =
            executor.exec {
                executable = adbPath.get().toString()
                args("shell", "pm", "list", "packages", appID.get())
                standardOutput = stdOut
                errorOutput = stdErr
                isIgnoreExitValue = true // Handle it ourselves
            }
        if (
            execResult.exitValue != 0 &&
                stdErr.toString().contains("adb: more than one device/emulator")
        ) {
            throw GradleException(
                "adb found multiple devices.\nSet '\$ANDROID_SERIAL' environment variable to specify the target device"
            )
        }
        execResult.assertNormalExitValue()

        if (!stdOut.toString().contains(appID.get())) return false
        return true
    }

    /**
     * Run the adb command
     *
     * Essentially the entry point for the
     * [task](https://docs.gradle.org/current/userguide/custom_tasks.html#sec:writing_a_simple_task_class)
     *
     * Uses adb to send a setApiKey
     * [intent](https://developer.android.com/studio/command-line/adb#IntentSpec) to the [appID] if
     * that appId. Checks to see if the app is installed first, if not, will note that it's not and
     * exit early.
     */
    @TaskAction
    fun runCommand() {
        val token =
            figmaToken.orNull
                ?: throw GradleException(
                    "No key set. Set it in the '\$FIGMA_ACCESS_TOKEN' environment variable"
                )

        if (!isAppInstalled()) {
            logger.lifecycle("Skipping ${this.name}, ${appID.get()} not found on device")
            // Exit early, rather than throwing an error, to allow the task to be called from the
            // command line without a specific app target in mind.
            return
        }

        val execResult =
            executor.exec {
                executable = adbPath.get().toString()
                isIgnoreExitValue = true
                args(
                    "shell",
                    "am",
                    "startservice",
                    "-n",
                    "${appID.get()}/com.android.designcompose.ApiKeyService",
                    "-a",
                    "setApiKey",
                    "-e",
                    "ApiKey",
                    token
                )
            }
        if (execResult.exitValue != 0) {
            logger.lifecycle("Failed to set token for ${appID.get()}")
        }
    }
}

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
class FigmaTokenPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.withType(com.android.build.gradle.AppPlugin::class.java) {
            project.extensions.getByType<ApplicationAndroidComponentsExtension>().let { ace ->
                @Suppress("UnstableApiUsage") val adb = ace.sdkComponents.adb
                // Create one task per variant of the app
                ace.onVariants() { variant ->
                    createTokenTask(project, variant.name, variant.applicationId, adb)
                }
            }
        }
    }

    /**
     * Create token task for the given [project] and [variant][variantId]
     *
     * @param project The Gradle project we're applying to
     * @param variantName The AGP-generated name for the application variant
     * @param variantId The Application ID for the variant
     * @param adb Provided by AGP, the path to adb on the system
     */
    private fun createTokenTask(
        project: Project,
        variantName: String,
        variantId: Property<String>,
        adb: Provider<RegularFile>
    ) {
        project.tasks.register<FigmaTokenTask>("setFigmaToken${variantName.capitalized()}") {
            adbPath.set(adb)
            appID.set(variantId)
            figmaToken.set(project.providers.environmentVariable("FIGMA_ACCESS_TOKEN"))
            group = "DesignCompose"
        }
    }
}

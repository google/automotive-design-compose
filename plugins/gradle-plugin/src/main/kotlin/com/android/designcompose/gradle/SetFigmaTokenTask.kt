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

import java.io.ByteArrayOutputStream
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

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
abstract class SetFigmaTokenTask @Inject constructor(private val executor: ExecOperations) :
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
                it.executable = adbPath.get().toString()
                it.args("shell", "pm", "list", "packages", "--user", "current", appID.get())
                it.standardOutput = stdOut
                it.errorOutput = stdErr
                it.isIgnoreExitValue = true // Handle it ourselves
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

        return stdOut.toString().contains(appID.get())
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
                    "No Figma token set. See https://google.github.io/automotive-design-compose/docs/live-update/setup"
                )

        if (!isAppInstalled()) {
            logger.lifecycle("Skipping ${this.name}, ${appID.get()} not found on device")
            // Exit early, rather than throwing an error, to allow the task to be called from the
            // command line without a specific app target in mind.
            return
        }

        val execResult =
            executor.exec {
                it.executable = adbPath.get().toString()
                it.isIgnoreExitValue = true
                it.args(
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

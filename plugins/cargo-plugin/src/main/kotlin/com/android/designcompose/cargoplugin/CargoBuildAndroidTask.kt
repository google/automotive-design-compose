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

package com.android.designcompose.cargoplugin

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.process.ExecOperations

/**
 * Cargo build task
 *
 * Builds the crate at the root of @rustSrcs, using the below properties
 *
 * @constructor Create empty Cargo build task
 * @property executor The Gradle ExecOperations service which provides methods for running other
 *   binaries.
 * @property ndkDirectory The directory containing the NDK to build with
 * @property androidAbi The ABI to build
 * @property compileApi The API version to build
 */
abstract class CargoBuildAndroidTask @Inject constructor(private val executor: ExecOperations) :
    CargoBuildTask() {

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputDirectory
    abstract val ndkDirectory: DirectoryProperty

    @get:Input abstract val androidAbi: Property<String>

    @get:Input abstract val compileApi: Property<Int>

    @TaskAction
    fun runCommand() {
        val toolchain =
            Toolchain(androidAbi.get(), compileApi.get(), ndkDirectory.get().asFile, hostOS.get())
        // The path (within the root Cargo target dir) that will contain the compiled .so file
        val targetOutputDir =
            cargoTargetDir.get().dir(toolchain.cargoTriple).dir(buildType.get().toString())

        cargoTargetDir.get().asFile.mkdirs()

        executor.exec {
            it.applyCommonCargoConfig()
            it.args("--target=${toolchain.cargoTriple}")

            it.environment("TARGET_CC", "${toolchain.cc}")
            it.environment(toolchain.linkerEnvName, "${toolchain.cc}")
            it.environment("TARGET_AR", "${toolchain.ar}")
        }

        // Copy the final compiled library to the correct location in the output dir.
        val finalOutLibDir = outLibDir.get().dir(androidAbi.get())
        fs.copy {
            it.from(targetOutputDir)
            it.include("*.so")
            it.into(finalOutLibDir)
        }
    }
}
/**
 * Create cargo task
 *
 * @param this@createAndroidCargoTask The project to create the task in
 * @param cargoExtension Configuration for this plugin
 * @param variant Android build variant that this task will build for
 * @param abi The Android ABI to compile
 * @param ndkDir The directory containing the NDK tools
 */
fun Project.registerAndroidCargoTask(
    cargoExtension: CargoPluginExtension,
    buildType: CargoBuildType,
    compileApi: Int,
    abi: String,
    ndkDir: Provider<Directory>
): TaskProvider<CargoBuildAndroidTask> =
    tasks.register(
        "cargoBuild${abi.capitalized()}${buildType.toString().capitalized()}",
        CargoBuildAndroidTask::class.java
    ) { task ->
        task.applyCommonConfig(cargoExtension, this, buildType)
        task.androidAbi.set(abi)
        task.ndkDirectory.set(ndkDir)
        task.compileApi.set(compileApi)
        // Don't set the outLibDir, it's set by the task's consumer
    }

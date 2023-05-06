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

import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations

/**
 * Cargo build task
 *
 * Builds the crate at the root of @rustSrcs, using the below properties
 *
 * @constructor Create empty Cargo build task
 * @property executor The Gradle ExecOperations service which provides methods for running other
 *   binaries.
 * @property rustSrcs The collection of files that will be compiled. Provided as a collection to aid
 *   Gradle's build cache, but only the root is passed to Cargo
 * @property ndkDirectory The directory containing the NDK to build with
 * @property cargoBin The cargo binary
 * @property androidAbi The ABI to build
 * @property compileApi The API version to build
 * @property useReleaseProfile If set, compile a debug build. Otherwise compile release
 * @property outLibDir Where the libraries will be copied to. Actually set by the Android plugin
 */
@UntrackedTask(
    because =
        "Cargo has it's own up-to-date checks. Trying to reproduce them so that we don't need to run Cargo is infeasible, and any errors will cause out-of-date code to be included"
)
abstract class CargoBuildTask @Inject constructor(private val executor: ExecOperations) :
    DefaultTask() {

    @get:Inject abstract val fs: FileSystemOperations

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    abstract val rustSrcs: ConfigurableFileCollection

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputDirectory
    abstract val ndkDirectory: DirectoryProperty

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputFile
    abstract val cargoBin: Property<File>

    @get:Input abstract val hostOS: Property<String>

    @get:Input abstract val androidAbi: Property<String>

    @get:Input abstract val compileApi: Property<Int>

    @get:Input abstract val useReleaseProfile: Property<Boolean>

    @get:OutputDirectory abstract val outLibDir: DirectoryProperty

    @get:Internal abstract val cargoTargetDir: DirectoryProperty

    @TaskAction
    fun runCommand() {
        val toolchain =
            Toolchain(androidAbi.get(), compileApi.get(), ndkDirectory.get().asFile, hostOS.get())
        val debugOrRelease = if (useReleaseProfile.get()) "release" else "debug"
        // The path (within the root Cargo target dir) that will contain the compiled .so file
        val targetOutputDir = cargoTargetDir.get().dir(toolchain.cargoTriple).dir(debugOrRelease)

        cargoTargetDir.get().asFile.mkdirs()

        executor.exec {
            it.executable(cargoBin.get().absolutePath)
            it.workingDir(rustSrcs.asPath)

            it.args("build")
            it.args("--target-dir=${cargoTargetDir.get().asFile.absolutePath}")
            it.args("--target=${toolchain.cargoTriple}")
            it.args("--quiet")
            if (useReleaseProfile.get()) {
                it.args("--release")
            }
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

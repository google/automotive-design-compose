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
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecSpec

@UntrackedTask(
    because =
        "Cargo has it's own up-to-date checks. Trying to reproduce them so that we don't need to run Cargo is infeasible, and any errors will cause out-of-date code to be included"
)

/**
 * Cargo build task
 *
 * @constructor Create empty Cargo build task
 * @property rustSrcs The collection of files that will be compiled. Provided as a collection to aid
 *   Gradle's build cache, but only the root is passed to Cargo
 * @property cargoBin The cargo binary
 * @property outLibDir Where the libraries will be copied to. Actually set by the Android plugin
 */
abstract class CargoBuildTask : DefaultTask() {
    @get:Inject abstract val fs: FileSystemOperations

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    abstract val rustSrcs: ConfigurableFileCollection

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputFile
    abstract val cargoBin: Property<File>

    @get:Input abstract val hostOS: Property<String>

    @get:Input abstract val buildType: Property<CargoBuildType>

    @get:OutputDirectory abstract val outLibDir: DirectoryProperty

    @get:Internal abstract val cargoTargetDir: DirectoryProperty

    /**
     * Apply common configuration for the task
     *
     * All Cargo tasks will use this configuration
     *
     * @param cargoExtension
     * @param project
     * @param theBuildType
     */
    fun applyCommonConfig(
        cargoExtension: CargoPluginExtension,
        project: Project,
        theBuildType: CargoBuildType
    ) {
        // Set the cargoBinary location from the configured plugin extension, or default to
        // the standard install location
        cargoBin.set(
            cargoExtension.cargoBin.orElse(
                project.providers.systemProperty("user.home").map { File(it, ".cargo/bin/cargo") }
            )
        )
        rustSrcs.setFrom(cargoExtension.crateDir.filter { !it.asFile.path.startsWith("target") })

        hostOS.set(
            if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                if (Os.isArch("x86_64") || Os.isArch("amd64")) {
                    "windows-x86_64"
                } else {
                    "windows"
                }
            } else if (Os.isFamily(Os.FAMILY_MAC)) {
                "darwin-x86_64"
            } else {
                "linux-x86_64"
            }
        )

        buildType.set(theBuildType)

        cargoTargetDir.set(
            project.layout.buildDirectory.map { it.dir("intermediates/cargoTarget") }
        )

        group = "build"
    }

    /**
     * Apply Common Cargo Config to an ExecSpec
     *
     * This is configuration of a Gradle ExecSpec that is common for all Cargo build tasks
     *
     * @param this@baseExecOptions The
     */
    fun ExecSpec.applyCommonCargoConfig() {
        executable(cargoBin.get().absolutePath)
        workingDir(rustSrcs.asPath)

        args("build")
        args("--target-dir=${cargoTargetDir.get().asFile.absolutePath}")
        args("--quiet")
        if (buildType.get() == CargoBuildType.RELEASE) args("--release")
    }
}

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
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.UntrackedTask
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.process.ExecOperations

@UntrackedTask(
    because =
        "Cargo has it's own up-to-date checks. Trying to reproduce them so that we don't need " +
            "to run Cargo is infeasible, and any errors will cause out-of-date code to be included"
)
abstract class CargoBuildHostTask @Inject constructor(private val executor: ExecOperations) :
    CargoBuildTask() {

    @TaskAction
    fun runCommand() {
        cargoTargetDir.get().asFile.mkdirs()

        executor.exec { it.applyCommonCargoConfig() }

        fs.copy {
            it.from(cargoTargetDir.get().dir(buildType.get().toString()))
            it.include("*.so") // Linux
            it.include("*.dylib") // Mac
            it.include("*.dll") // Windows
            it.into(outLibDir)
        }
    }
}

/**
 * Get host cargo output dir
 *
 * Provides a method for other projects to get the directory that will contain the compiled
 * libraries without triggering generation of the task.
 *
 * @param buildType
 * @return
 */
fun Project.getHostCargoOutputDir(buildType: CargoBuildType): Provider<Directory> =
    layout.buildDirectory.dir("intermediates/host_rust_libs/$buildType")

/**
 * Register host cargo task
 *
 * @param cargoExtension
 * @param buildType
 * @return
 */
fun Project.registerHostCargoTask(
    cargoExtension: CargoPluginExtension,
    buildType: CargoBuildType,
): TaskProvider<CargoBuildHostTask> {

    return tasks.register(
        "cargoBuildHost${buildType.toString().capitalized()}",
        CargoBuildHostTask::class.java,
    ) { task ->
        task.applyCommonConfig(cargoExtension, this, buildType)
        task.outLibDir.set(getHostCargoOutputDir(buildType))
        task.enabled = cargoExtension.buildHost.getOrElse(false)
    }
}

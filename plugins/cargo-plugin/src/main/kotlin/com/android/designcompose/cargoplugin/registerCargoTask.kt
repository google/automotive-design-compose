package com.android.designcompose.cargoplugin

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.configurationcache.extensions.capitalized

const val cargoBuildHostTaskBaseName = "cargoBuildHost"

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
        task.applyCommonCargoConfig(cargoExtension, this, buildType)
        task.androidAbi.set(abi)
        task.ndkDirectory.set(ndkDir)
        task.compileApi.set(compileApi)
    }

fun makeHostCargoBuildTaskName(buildType: CargoBuildType) ="$cargoBuildHostTaskBaseName${buildType.toString().capitalized()}"
fun makeHostCargoOutputDir(
    cargoExtension: CargoPluginExtension,
    buildType: CargoBuildType
): Provider<Directory> =
    cargoExtension.hostLibsOut.dir(buildType.toString())

fun Project.registerHostCargoTask(
    cargoExtension: CargoPluginExtension,
    buildType: CargoBuildType
): TaskProvider<CargoBuildHostTask> {

    return tasks.register(makeHostCargoBuildTaskName(buildType), CargoBuildHostTask::class.java) { task ->
        task.applyCommonCargoConfig(cargoExtension, this, buildType)
        task.outLibDir.set(makeHostCargoOutputDir(cargoExtension, buildType))
    }
}


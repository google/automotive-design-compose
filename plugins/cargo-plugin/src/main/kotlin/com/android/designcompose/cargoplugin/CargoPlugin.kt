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

import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.LibraryVariant
import com.android.build.api.variant.Variant
import com.android.builder.model.PROPERTY_BUILD_ABI
import java.io.File
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.configurationcache.extensions.capitalized

/**
 * Cargo plugin
 *
 * Creates and wires up tasks for compiling Rust code with the NDK. Currently only supports Android
 * libraries.
 */
@Suppress("unused")
class CargoPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val cargoExtension = project.extensions.create("cargo", CargoPluginExtension::class.java)

        // Filter the ABIs using configurable Gradle properties
        val activeAbis = getActiveAbis(cargoExtension.abi, project)

        // withPlugin(String) will do the action once the plugin is applied, or immediately
        // if the plugin is already applied
        project.pluginManager.withPlugin("com.android.library") {
            project.extensions.getByType(LibraryAndroidComponentsExtension::class.java).let { ace ->
                val ndkDir = findNdkDirectory(project, ace)

                // Create one task per variant and ABI
                ace.onVariants { variant ->
                    cargoExtension.abi.get().forEach { abi ->
                        val cargoTask =
                            createCargoTask(project, cargoExtension, variant, abi, ndkDir)

                        // If building a release or the ABI is active, add the task to the build
                        if (variant.buildType == "release" || activeAbis.get().contains(abi)) {
                            addDependencyOnTask(variant, cargoTask, project)
                        }
                    }
                }
            }
        }
    }

    /**
     * Add dependency on task
     *
     * @param variant The build variant
     * @param cargoTask The task to add
     * @param project The full project
     */
    private fun addDependencyOnTask(
        variant: LibraryVariant,
        cargoTask: TaskProvider<CargoBuildTask>,
        project: Project
    ) {
        with(variant.sources.jniLibs) {
            if (this != null) {
                // Add the result to the variant's JNILibs sources. This is all we need to
                // do to make sure the JNILibs are compiled and included in the library
                this.addGeneratedSourceDirectory(cargoTask, CargoBuildTask::outLibDir)
            } else
                project.logger.error(
                    "No JniLibs configured by Android Gradle Plugin, Cargo tasks may not run"
                )
        }
    }

    /**
     * Get active abis
     *
     * @param configuredAbis The list of ABIs configured for the plugin
     * @param project The project to work on
     * @return
     */
    private fun getActiveAbis(
        configuredAbis: Provider<MutableSet<String>>,
        project: Project
    ): Provider<Set<String>> =
        configuredAbis.map {
            if (project.findProperty(PROPERTY_ALLOW_ABI_OVERRIDE)?.toString() == "true") {
                selectActiveAbis(
                    configuredAbis.get(),
                    project.findProperty(PROPERTY_BUILD_ABI)?.toString(),
                    project.findProperty(PROPERTY_ABI_FILTER)?.toString()
                )
            } else {
                it
            }
        }

    /**
     * Find ndk directory for a given Android configuration
     *
     * Given the configured android.ndkVersion, will return the path to the directory containing it.
     * We should be able to remove this once b/278740309 is fixed.
     *
     * @param project
     * @param ace Android Components Extension
     * @return
     */
    private fun findNdkDirectory(
        project: Project,
        ace: LibraryAndroidComponentsExtension
    ): DirectoryProperty {
        val ndkDir = project.objects.directoryProperty()

        ace.finalizeDsl { androidDsl ->
            @Suppress("UnstableApiUsage")
            // For now the ndkVersion must be specified in the android block of the project.
            //
            val ndkVersion =
                androidDsl.ndkVersion ?: throw GradleException("android.ndkVersion must be set!")
            // https://blog.rust-lang.org/2023/01/09/android-ndk-update-r25.html
            if (ndkVersion.substringBefore(".").toInt() < 25)
                throw GradleException("ndkVersion must be at least r25")
            @Suppress("UnstableApiUsage")
            ndkDir.set(ace.sdkComponents.sdkDirectory.map { it.dir("ndk/$ndkVersion") })
        }
        return ndkDir
    }

    /**
     * Create cargo task
     *
     * @param project The project to create the task in
     * @param cargoExtension Configuration for this plugin
     * @param variant Android build variant that this task will build for
     * @param abi The Android ABI to compile
     * @param ndkDir The directory containing the NDK tools
     */
    private fun createCargoTask(
        project: Project,
        cargoExtension: CargoPluginExtension,
        variant: Variant,
        abi: String,
        ndkDir: Provider<Directory>
    ): TaskProvider<CargoBuildTask> {
        val cargoTask =
            project.tasks.register(
                "cargoBuild${abi.capitalized()}${variant.name.capitalized()}",
                CargoBuildTask::class.java,
            ) { task ->
                // Set the cargoBinary location from the configured plugin extension, or default to
                // the standard install location
                task.cargoBin.set(
                    cargoExtension.cargoBin.orElse(
                        project.providers.systemProperty("user.home").map {
                            File(it, ".cargo/bin/cargo")
                        }
                    )
                )

                task.rustSrcs.from(cargoExtension.crateDir).filterNot { file ->
                    file.name == "target"
                }

                task.hostOS.set(
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

                task.androidAbi.set(abi)
                task.useReleaseProfile.set(variant.buildType != "debug")
                task.ndkDirectory.set(ndkDir)
                task.compileApi.set(variant.minSdk.apiLevel)
                task.cargoTargetDir.set(
                    project.layout.buildDirectory.map { it.dir("intermediates/cargoTarget") }
                )

                task.group = "build"
                // Try to get the cargo build started earlier in the build execution.
                task.shouldRunAfter(project.tasks.named("preBuild"))
            }

        return cargoTask
    }
}

/**
 * Select active abis
 *
 * @param configuredAbis The Abis configured in the plugin
 * @param androidInjectedAbis The Abis injected by an Android Studio Build
 * @param abiFilter The Abi filter set via gradle property
 * @return
 */
internal fun selectActiveAbis(
    configuredAbis: Set<String>,
    androidInjectedAbis: String?,
    abiFilter: String?
): Set<String> {
    return if (androidInjectedAbis != null) {
        // Android injects two ABIs for emulators. The first is the architecture of the host, the
        // second is the architecture of the device that the AVD is emulating. We just want the
        // host's architecture
        val abi = androidInjectedAbis.split(",").first()
        if (configuredAbis.contains(abi)) setOf(abi)
        else throw GradleException("Unknown injected build ABI: $abi")
    } else if (abiFilter != null) {
        abiFilter
            .split(",")
            .map {
                if (!configuredAbis.contains(it)) throw GradleException("Unknown abiOverride: $it")
                else it
            }
            .toSet()
    } else configuredAbis
}

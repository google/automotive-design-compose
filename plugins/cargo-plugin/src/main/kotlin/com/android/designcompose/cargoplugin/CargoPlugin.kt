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
import com.android.builder.model.PROPERTY_BUILD_ABI
import org.gradle.api.GradleException
import org.gradle.api.Named
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

inline fun <reified T : Named> Project.namedAttribute(value: String) =
    objects.named(T::class.java, value)

/**
 * Cargo plugin
 *
 * Creates and wires up tasks for compiling Rust code with the NDK. Currently only supports Android
 * libraries.
 */
@Suppress("unused")
class CargoPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val cargoExtension = project.initializeExtension()
        // Filter the ABIs using configurable Gradle properties
        val activeAbis = getActiveAbis(cargoExtension.abi, project)

        // Hacky: GH-502
        // Register the tasks, which is fine, but they need to be hooked up to an outgoing Gradle
        // ConfigurationF
        project.registerHostCargoTask(cargoExtension, CargoBuildType.DEBUG)
        project.registerHostCargoTask(cargoExtension, CargoBuildType.RELEASE)

        // withPlugin(String) will do the action once the plugin is applied, or immediately
        // if the plugin is already applied
        project.pluginManager.withPlugin("com.android.library") {
            project.extensions
                .getByType(LibraryAndroidComponentsExtension::class.java)
                .configureCargoPlugin(project, cargoExtension, activeAbis)
        }
    }

    private fun LibraryAndroidComponentsExtension.configureCargoPlugin(
        project: Project,
        cargoExtension: CargoPluginExtension,
        activeAbis: Provider<Set<String>>,
    ) {
        val ndkDir = this.findNdkDirectory(project)

        var androidCargoTasks:
            Map<Pair<CargoBuildType, String>, TaskProvider<CargoBuildAndroidTask>> =
            mapOf()

        finalizeDsl { androidExtension ->
            cargoExtension.abi.finalizeValue()

            // Register the cargo tasks for all abi's and build types.
            val newTasks:
                MutableMap<Pair<CargoBuildType, String>, TaskProvider<CargoBuildAndroidTask>> =
                mutableMapOf()

            for (buildType in CargoBuildType.entries) for (abi in cargoExtension.abi.get()) {
                newTasks[Pair(buildType, abi)] =
                    project.registerAndroidCargoTask(
                        cargoExtension,
                        buildType,
                        // Not ideal: Technically, each variant can set it's own minSdk, but because
                        // we register the tasks outside of the variants we only have access to the
                        // defaultConfig minSdk.
                        androidExtension.defaultConfig.minSdk!!,
                        abi,
                        ndkDir,
                    )
            }
            androidCargoTasks = newTasks
        }

        // For each variant, add dependencies on the appropriate Cargo tasks.
        onVariants { variant ->
            variant.sources.jniLibs?.let { sourceDirs ->
                val tasks =
                    // The main release variant must include all abi's.
                    // Clunky: A better solution here would be to add a DSL setting to specify which
                    // ABIs to include in each variant.
                    // (something like com.android.build.api.dsl.Split)
                    if (variant.name == "release") {
                        androidCargoTasks.filter { it.key.first == CargoBuildType.RELEASE }
                    }
                    // All other variants only include the active ABIs (specified by
                    // designcompose.cargoPlugin.abiOverride)
                    else {
                        val buildType =
                            if (variant.debuggable) CargoBuildType.DEBUG else CargoBuildType.RELEASE
                        androidCargoTasks.filter {
                            it.key.first == buildType && activeAbis.get().contains(it.key.second)
                        }
                    }
                assert(tasks.isNotEmpty())

                for (task in tasks.values) {
                    // Todo: This does not handle variants with minSdks that differ from the
                    // DefaultConfig
                    sourceDirs.addGeneratedSourceDirectory(task, CargoBuildAndroidTask::outLibDir)
                }
            }
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
        project: Project,
    ): Provider<Set<String>> =
        configuredAbis.map {
            if (project.findProperty(PROPERTY_ALLOW_ABI_OVERRIDE)?.toString() == "true") {
                selectActiveAbis(
                    configuredAbis.get(),
                    project.findProperty(PROPERTY_BUILD_ABI)?.toString(),
                    project.findProperty(PROPERTY_ABI_FILTER)?.toString(),
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
     * @return
     */
    @Suppress("UnstableApiUsage")
    private fun LibraryAndroidComponentsExtension.findNdkDirectory(
        project: Project
    ): DirectoryProperty {
        val ndkDir = project.objects.directoryProperty()

        finalizeDsl { androidDsl ->
            val ndkVersion = androidDsl.ndkVersion
            if (ndkVersion.substringBefore(".").toInt() < 25)
            // https://blog.rust-lang.org/2023/01/09/android-ndk-update-r25.html
            throw GradleException("ndkVersion must be at least r25")

            ndkDir.set(sdkComponents.sdkDirectory.map { it.dir("ndk/$ndkVersion") })
        }
        return ndkDir
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
    abiFilter: String?,
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

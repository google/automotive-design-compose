/*
 * Copyright 2024 Google LLC
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

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

// We put all the logic that depends on the Android Gradle Plugin in a separate file, so that this
// plugin can be applied to non-Android projects without crashing. This is useful for testing.
internal fun configureAndroidPlugin(project: Project, pluginExtension: PluginExtension) {
    val ace = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
    @Suppress("UnstableApiUsage") val adb = ace.sdkComponents.adb
    // Create one task per variant of the app
    ace.onVariants { variant ->
        project.createSetFigmaTokenTask(variant.name, variant.applicationId, adb, pluginExtension)
    }
}

/**
 * Create a setFigmaToken task for the given [variant][variantId]
 *
 * @param variantName The AGP-generated name for the application variant
 * @param variantId The Application ID for the variant
 * @param adb Provided by AGP, the path to adb on the system
 */
private fun Project.createSetFigmaTokenTask(
    variantName: String,
    variantId: Property<String>,
    adb: Provider<RegularFile>,
    pluginExtension: PluginExtension,
) {
    tasks.register(
        "setFigmaToken${variantName.replaceFirstChar { it.uppercase() }}",
        SetFigmaTokenTask::class.java,
    ) {
        it.adbPath.set(adb)
        it.appID.set(variantId)
        it.figmaToken.set(pluginExtension.figmaToken.also { token -> token.finalizeValue() })
        it.group = "DesignCompose"
    }
}

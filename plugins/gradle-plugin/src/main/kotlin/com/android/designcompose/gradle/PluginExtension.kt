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

import java.awt.SystemColor.text
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

private const val tokenGradlePropertyName = "figmaAccessToken"
private const val tokenFileGradlePropertyName = "figmaAccessTokenFilePath"

/**
 * Plugin extension for the DesignCompose plugin
 *
 * Holds persistent data for the plugin including the path to the user's Figma Token File and the
 * user's token itself.
 */
abstract class PluginExtension() {
    abstract val figmaTokenFile: RegularFileProperty
    abstract val figmaToken: Property<String>
}

/**
 * Initialize the plugin extension to set conventions (default values) for the properties
 *
 * @param extension: The instance of the extension that was created with project.extensions.create
 */
fun Project.initializeExtension(extension: PluginExtension) {

    // Determine the directory where the token file should live, based on OS
    val tokenFileDir: Provider<Directory> =
        when {
            Os.isFamily(Os.FAMILY_WINDOWS) ->
                providers.systemProperty("APPDATA").map { layout.projectDirectory.dir(it) }
            Os.isFamily(Os.FAMILY_UNIX) ->
                providers.systemProperty("user.home").map {
                    layout.projectDirectory.dir(it).dir(".config")
                }
            else -> provider { rootProject.layout.projectDirectory }
        }

    // Set the path to the file, but allow it to be overriden by a Gradle property
    // (figmaAccessTokenFilePath
    extension.figmaTokenFile.convention(
        providers
            .gradleProperty(tokenFileGradlePropertyName)
            .map { layout.projectDirectory.file(it) }
            .orElse(tokenFileDir.map { it.file("figma_access_token") })
    )

    // Set the Figma token from the environment variable, falling back on a Gradle property and
    // falling back on the contents of the figma token file
    extension.figmaToken.convention(
        providers
            .environmentVariable("FIGMA_ACCESS_TOKEN")
            .orElse(providers.gradleProperty(tokenGradlePropertyName))
            .orElse(
                providers.fileContents(extension.figmaTokenFile).asText.map { text ->
                    // Check file permissions when reading the token from a file (Issue #249).
                    // Warn if the file is world-readable, as this could expose the Figma
                    // access token to other users on shared systems.
                    checkTokenFilePermissions(extension.figmaTokenFile)
                    text.trim()
                }
            )
    )
    // Avoid odd behavior by only allowing changes to be made to the token before it's read
    extension.figmaToken.finalizeValueOnRead()
}

/**
 * Check that the Figma token file is not world-readable. On Unix/macOS systems, a world-readable
 * token file could expose the Figma access token to other users. This function emits a warning
 * if the file has overly permissive permissions.
 *
 * @param tokenFile The RegularFileProperty pointing to the token file
 */
private fun Project.checkTokenFilePermissions(tokenFile: RegularFileProperty) {
    if (!Os.isFamily(Os.FAMILY_UNIX)) return

    try {
        val file = tokenFile.get().asFile
        if (!file.exists()) return

        val permissions = Files.getPosixFilePermissions(file.toPath())
        val worldReadable = PosixFilePermission.OTHERS_READ in permissions
        val worldWritable = PosixFilePermission.OTHERS_WRITE in permissions
        val groupReadable = PosixFilePermission.GROUP_READ in permissions

        if (worldReadable || worldWritable) {
            logger.warn(
                "WARNING: Figma access token file '${file.absolutePath}' is world-" +
                    "${if (worldReadable && worldWritable) "readable and writable" else if (worldReadable) "readable" else "writable"}. " +
                    "This could expose your Figma access token to other users. " +
                    "Run 'chmod 600 ${file.absolutePath}' to restrict access to owner only."
            )
        } else if (groupReadable) {
            logger.warn(
                "WARNING: Figma access token file '${file.absolutePath}' is group-readable. " +
                    "Consider running 'chmod 600 ${file.absolutePath}' to restrict access to owner only."
            )
        }
    } catch (e: Exception) {
        // Don't fail the build if we can't check permissions (e.g. on unsupported filesystems)
        logger.debug("Could not check Figma token file permissions: ${e.message}")
    }
}


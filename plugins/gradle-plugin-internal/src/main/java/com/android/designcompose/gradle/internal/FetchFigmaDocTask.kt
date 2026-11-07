/*
 * Copyright 2026 Google LLC
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

package com.android.designcompose.gradle.internal

import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

/**
 * Gradle task that wraps the `fetch` CLI binary, auto-extracting node queries
 * from @DesignComponent and @DesignVariant annotations in Kotlin source files.
 *
 * This produces the same node list that the codegen-generated `queries()` function
 * returns, so CLI-fetched .dcf files match what the live update system gets.
 *
 * Register in your build script:
 * ```
 * tasks.register<FetchFigmaDocTask>("fetchMyDoc") {
 *     sourceDir.set(file("src/main/java"))
 *     outputFile.set(file("src/main/assets/figma/MyDoc.dcf"))
 *     // optional: override doc ID if annotation uses a constant reference
 *     // docId.set("abc123")
 * }
 * ```
 */
abstract class FetchFigmaDocTask @Inject constructor(
    private val executor: ExecOperations
) : DefaultTask() {

    /** Kotlin source directory to scan for annotations. */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDir: DirectoryProperty

    /** Output .dcf file path. */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    /**
     * Figma document ID. If not set, extracted from @DesignDoc(id = "...") in source.
     * Must be set explicitly if the annotation uses a constant reference.
     */
    @get:Input
    @get:Optional
    abstract val docId: Property<String>

    /** Figma API token. Falls back to FIGMA_ACCESS_TOKEN env var. */
    @get:Input
    @get:Optional
    abstract val figmaToken: Property<String>

    init {
        group = "designcompose"
        description = "Fetch a Figma doc with auto-extracted node queries from annotations"
    }

    @TaskAction
    fun run() {
        val srcDir = sourceDir.get().asFile
        val ktFiles = srcDir.walkTopDown().filter { it.extension == "kt" }.toList()

        if (ktFiles.isEmpty()) {
            throw GradleException("No Kotlin files found in ${srcDir.absolutePath}")
        }

        val allSource = ktFiles.joinToString("\n") { it.readText() }

        // --- Extract doc ID ---
        val resolvedDocId = docId.orNull ?: extractDocId(allSource)
            ?: throw GradleException(
                "Could not extract doc ID from @DesignDoc in $srcDir. " +
                    "The annotation may use a constant reference — set docId explicitly."
            )

        // --- Extract node queries (same logic as codegen queries()) ---
        val nodes = extractNodeQueries(allSource)

        if (nodes.isEmpty()) {
            throw GradleException(
                "No @DesignComponent or @DesignVariant annotations found in $srcDir"
            )
        }

        logger.lifecycle("Fetching doc $resolvedDocId with ${nodes.size} nodes:")
        nodes.forEach { logger.lifecycle("  $it") }

        // --- Resolve Figma token ---
        val token = figmaToken.orNull
            ?: System.getenv("FIGMA_ACCESS_TOKEN")
            ?: loadTokenFromFile()
            ?: throw GradleException(
                "Figma token not found. Set figmaToken, FIGMA_ACCESS_TOKEN env var, " +
                    "or create ~/.config/figma_access_token"
            )

        // --- Run cargo fetch ---
        val nodeArgs = nodes.flatMap { listOf("--nodes", it) }

        executor.exec {
            it.workingDir(project.rootDir)
            it.commandLine(
                "cargo", "run", "--bin", "fetch", "--features=fetch", "--",
                "--doc-id=$resolvedDocId",
                *nodeArgs.toTypedArray(),
                "--output=${outputFile.get().asFile.absolutePath}",
                "--api-key=$token"
            )
        }

        logger.lifecycle("Wrote ${outputFile.get().asFile.absolutePath}")
    }

    companion object {

        /** Regex patterns matching what codegen collects into queries(). */
        private val DESIGN_COMPONENT_NODE =
            Regex("""@DesignComponent\s*\([^)]*node\s*=\s*"([^"]+)"""")
        private val DESIGN_VARIANT_PROPERTY =
            Regex("""@DesignVariant\s*\([^)]*property\s*=\s*"([^"]+)"""")
        private val DESIGN_DOC_ID =
            Regex("""@DesignDoc\s*\([^)]*id\s*=\s*"([^"]+)"""")

        /**
         * Extract node queries from source text.
         *
         * Mirrors [BuilderProcessor.visitFunctionQueries]: collects
         * - @DesignComponent(node = "X")
         * - @DesignVariant(property = "Y")
         */
        fun extractNodeQueries(source: String): List<String> {
            val nodes = mutableSetOf<String>()
            DESIGN_COMPONENT_NODE.findAll(source).forEach { nodes.add(it.groupValues[1]) }
            DESIGN_VARIANT_PROPERTY.findAll(source).forEach { nodes.add(it.groupValues[1]) }
            return nodes.sorted()
        }

        /** Extract doc ID from @DesignDoc(id = "...") literal. */
        fun extractDocId(source: String): String? =
            DESIGN_DOC_ID.find(source)?.groupValues?.get(1)

        private fun loadTokenFromFile(): String? {
            val home = System.getenv("HOME") ?: return null
            val tokenFile = File(home, ".config/figma_access_token")
            return if (tokenFile.isFile) tokenFile.readText().trim().ifEmpty { null } else null
        }
    }
}

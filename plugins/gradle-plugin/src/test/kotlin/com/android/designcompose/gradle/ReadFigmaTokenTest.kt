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

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Read figma token test
 *
 * Uses the Gradle TestKit's GradleRunner to create and run Gradle projects with different
 * environments.
 *
 * Test that each environment results in the correct token being read by the project.
 */
class ReadFigmaTokenTest {

    @field:TempDir lateinit var testProjectDir: File

    private val envToken = "envToken"
    private val envTokenEnvironment = mapOf("FIGMA_ACCESS_TOKEN" to envToken)
    private val propertyToken = "propertyToken"
    private val propertyTokenEnvironment =
        mapOf("ORG_GRADLE_PROJECT_figmaAccessToken" to propertyToken)
    private val fileToken = "fileToken"
    private lateinit var tokenFile: File
    private lateinit var testProject: GradleRunner

    @BeforeEach
    fun setup() {
        tokenFile = testProjectDir.resolve("figma_access_token").also { it.writeText(fileToken) }

        testProject =
            GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withPluginClasspath()
                .withArguments("checkFigmaToken")
    }

    /**
     * Write build script for the project being used in the test. This includes a task in this
     * project to check that the token read by the project matches the expected one per test
     *
     * @param expectedToken: The token that the test expects to find
     */
    private fun writeBuildScript(expectedToken: String) {
        testProjectDir
            .resolve("build.gradle.kts")
            .writeText(
                """
                plugins {
                    id("com.android.designcompose") 
                }
                designcompose {
                    figmaTokenFile.set(File("${tokenFile.absolutePath}"))
                }
                tasks.register("checkFigmaToken") {
                    doLast {
                        val dcExtension = project.extensions.getByType(com.android.designcompose.gradle.PluginExtension::class.java)
                        if( dcExtension.figmaToken.get() != "$expectedToken") { 
                            throw GradleException("Token does not match")
                        }
                    }
                } 
            """
                    .trimIndent()
            )
    }

    @Test
    fun environmentVarOverridesAll() {
        writeBuildScript(envToken)
        val result =
            testProject.withEnvironment(envTokenEnvironment + propertyTokenEnvironment).build()
        assertThat(result.task(":checkFigmaToken")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun environmentVarOverridesFile() {
        writeBuildScript(envToken)
        val result = testProject.withEnvironment(envTokenEnvironment).build()
        assertThat(result.task(":checkFigmaToken")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun gradlePropertyOverridesFile() {
        writeBuildScript(propertyToken)
        val result = testProject.withEnvironment(propertyTokenEnvironment).build()
        assertThat(result.task(":checkFigmaToken")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun tokenIsReadFromFile() {
        writeBuildScript(fileToken)
        // Test runs with an empty set of environment variables, to avoid being contaminated by the
        // user's environment
        val result = testProject.withEnvironment(mapOf()).build()
        assertThat(result.task(":checkFigmaToken")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
}

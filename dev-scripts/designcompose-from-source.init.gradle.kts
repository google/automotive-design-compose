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

/**
 * README:
 *
 * The below builds the DesignCompose dependency from source and configured the build to use it. The
 * standard way to have a Gradle build include the build of a sepearate project is to use
 * `includeBuild`. Unfortunately this is not available to us because the Android Gradle Plugin
 * version needs to match for both builds and that isn't feasible. Instead we directly build
 * DesignCompose via a subprocess and include it's maven artifacts into this build
 *
 * CONFIGURATION: Set the Gradle property `buildDesignComposeFromSource` to `true` (no quotation
 * marks) Set the Gradle property `designComposeSourceDir` to the absolute path of the DesignCompose
 * source checkout. Set these in your ~/.gradle/gradle.properties file
 *
 * BE AWARE: Android Studio doesn't have a clean way to use init scripts so you'll need to symlink or
 * copy this file into your $GRADLE_USER_HOME (normally ~/.gradle)
 *
 * The best way to do so is to copy the script into ~/.gradle/init.d. Best way to disable it is to
 * delete the file or change the file extension. You can also always leave it enabled and disable it
 * by setting the Gradle property `buildDesignComposeFromSource` to false.
 *
 */
settingsEvaluated {
    // Only evaluate this from the root project, not from includedBuilds
    if (gradle.parent != null) return@settingsEvaluated
    logger.lifecycle("Using designcompose-from-source.init.gradle.kts")

    val buildDesignComposeFromSource: String? by settings
    if (buildDesignComposeFromSource?.contentEquals("true") != true) {
        logger.lifecycle("SKIPPING: buildDesignComposeFromSource unset")
        return@settingsEvaluated
    }

    // Don't recursively apply to the DesignCompose project
    if (rootProject.name == "DesignCompose") return@settingsEvaluated

    if (!rootProject.name.contains("AAOS Apps")) {
        logger.lifecycle(
            "SKIPPING: This init script is only intended for the AAOS Apps projects. This project doesn't seem to be one."
        )
        return@settingsEvaluated
    }

    val designComposeSourceDir: String? by settings
    if (designComposeSourceDir == null) {
        throw GradleException("designComposeSourceDir not set")
    }

    val actualDCDir = File(designComposeSourceDir)
    if (!actualDCDir.canRead()) {
        throw GradleException("Unable to resolve designComposeSourceDir $designComposeSourceDir")
    }

    logger.lifecycle("Building DesignCompose")
    val pb =
        ProcessBuilder(
            System.getenv("SHELL"),
            "gradlew",
            "publishAllPublicationsToLocalDirRepository",
        )

    val m2repoDir = actualDCDir.resolve("build/designcompose_m2repo_for_init_script").absolutePath

    pb.directory(actualDCDir)
    pb.environment()["ORG_GRADLE_PROJECT_DesignComposeMavenRepo"] = m2repoDir

    val dcBuildProcess: Process = pb.start()
    // Print the stdout of the build
    java.io.BufferedReader(dcBuildProcess.inputReader()).lines().forEach { line ->
        logger.lifecycle("DC: $line")
    }

    // Wait for the build to finish
    val result = dcBuildProcess.waitFor()
    if (result != 0) {
        throw GradleException("DesignCompose build failed")
    }

    logger.lifecycle("DesignCompose build complete")

    // Set the build to use the newly built DesignCompose
    pluginManagement {
        repositories {
            exclusiveContent {
                forRepository { maven { url = uri(m2repoDir) } }
                filter { includeGroup("com.android.designcompose") }
            }
        }
    }

    dependencyResolutionManagement {
        versionCatalogs { create("libs") { version("designcompose", "+") } }

        @Suppress("UnstableApiUsage")
        repositories {
            exclusiveContent {
                forRepository { maven { url = uri(m2repoDir) } }
                filter { includeGroup("com.android.designcompose") }
            }
        }
    }
}

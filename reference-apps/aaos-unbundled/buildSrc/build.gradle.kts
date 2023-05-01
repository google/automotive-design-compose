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

// Temporary (I hope) hacks:
// MediaCompose needs AAOS Unbundled libraries, but we can't do an "includedBuild" of AAOS Unbundled
// and of DesignCompose, because they use incompatible versions of AGP. So instead we build and use
// DesignCompose's maven artifacts.
// We were using a GradleBuild task to build those artifacts, but GradleBuild doesn't support
// builds that use other IncludedBuilds (like DesignCompose's build-logic and plugins)
// So we just directly exec gradle.

// This should all be cleaned up once the AAOS Unbundled repo implements publishing of it's
// libraries
val designComposeRepoBuild =
    tasks.register<Exec>("buildDesignComposeRepo") {
        workingDir = layout.projectDirectory.dir("../../../").asFile
        commandLine =
            listOf(
                "./gradlew",
                "-PDesignComposeMavenRepo=reference-apps/aaos-unbundled/build/designcompose_m2repo",
                "publishAllPublicationsToLocalDirRepository"
            )
    }

tasks.named("build") { dependsOn(designComposeRepoBuild) }

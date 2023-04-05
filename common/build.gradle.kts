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

import com.ncorti.ktfmt.gradle.tasks.KtfmtCheckTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-library`
    id("designcompose.conventions.publish.jvm")
}

// Serdegen related code
val serdeGenDir: Provider<Directory> = layout.buildDirectory.dir("generated/serdegen/java")

val genCodeTask =
    tasks.register<Exec>("generateSerdegenCode") {
        group = "DesignCompose Developer"
        outputs.dir(serdeGenDir)
        doFirst { delete(serdeGenDir) }
        workingDir(File(projectDir, "../crates/figma_import"))
        environment(
            "CARGO_TARGET_DIR",
            layout.buildDirectory.dir("serdegenCargoTarget").get().toString()
        )
        commandLine =
            listOf(
                "cargo",
                "run",
                "-q",
                "--release",
                "--features=reflection",
                "--bin=reflection",
                "--",
                "--out-dir",
                serdeGenDir.get().toString()
            )
    }

tasks.withType<JavaCompile> { dependsOn(genCodeTask) }

tasks.withType<KotlinCompile> { dependsOn(genCodeTask) }

tasks.withType<KtfmtCheckTask> { dependsOn(genCodeTask) }

project.afterEvaluate { serdeGenDir.get().asFile.mkdirs() }

sourceSets { main { java { srcDir(serdeGenDir) } } }

dependencies {
    api(libs.javax.annotationApi)
    implementation(libs.kotlin.stdlib)
}

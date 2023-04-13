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

import org.gradle.process.internal.DefaultExecOperations

plugins {
    kotlin("jvm")
    `java-library`
    id("designcompose.conventions.publish.jvm")
}

/**
 * Serde gen task
 *
 * Generates the Java files from our Rust code
 *
 * @constructor Create empty Serde gen task
 * @property executor: ExecOperations class
 * @property rustSrcs The files to watch to see if we should rebuild (should be filtered to not
 *   include the target dir
 * @property generatedCodeDir Where the generated code will be output
 */
@CacheableTask
abstract class SerdeGenTask @Inject constructor(private val executor: DefaultExecOperations) :
    DefaultTask() {

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    abstract val rustSrcs: ConfigurableFileCollection

    @get:OutputDirectory abstract val generatedCodeDir: DirectoryProperty

    init {
        group = "DesignCompose Developer"
    }

    @TaskAction
    fun run() {
        generatedCodeDir.get().asFileTree.forEach { it.delete() }
        executor.exec {
            executable = "cargo"
            environment("CARGO_TARGET_DIR", "${project.buildDir}/serdeGenCargoTarget")
            workingDir(rustSrcs.asPath)
            args(
                listOf(
                    "run",
                    "-q",
                    "--release",
                    "--features=reflection",
                    "--bin=reflection",
                    "--",
                    "--out-dir",
                    generatedCodeDir.get().toString()
                )
            )
        }
    }
}

// Configure the task, setting the locations of the source and outputs
val serdeGenTask =
    tasks.register<SerdeGenTask>("generateSerdegenCode") {
        rustSrcs.from(
            layout.projectDirectory.files("../crates/figma_import").filterNot { name == "target" }
        )
        generatedCodeDir.set(layout.buildDirectory.dir("generated/serdegen/java"))
    }

// Connect the outputs to the java source set, so it'll automatically be compiled
project.sourceSets.main { java { srcDir(serdeGenTask.flatMap { it.generatedCodeDir }) } }

dependencies {
    api(libs.javax.annotationApi)
    implementation(libs.kotlin.stdlib)
}

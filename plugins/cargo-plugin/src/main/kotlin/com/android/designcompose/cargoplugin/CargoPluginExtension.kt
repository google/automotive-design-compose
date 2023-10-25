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

import org.gradle.api.Project
import java.io.File
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

interface CargoPluginExtension {
    val cargoBin: Property<File> // Path to the cargo Binary, defaults to ~/.cargo/bin/cargo
    val crateDir: DirectoryProperty // The cargo workspace to compile
    val abi:
        SetProperty<String> // The ABI's to compile https://developer.android.com/ndk/guides/abis
    val hostLibsOut: DirectoryProperty
}

fun Project.initializeExtension(): CargoPluginExtension {
    val cargoExtension = extensions.create("cargo", CargoPluginExtension::class.java)
    cargoExtension.hostLibsOut.convention(layout.buildDirectory.dir("intermediates/host_rust_libs"))
    return cargoExtension
}
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

import java.io.File

val cargoTriples =
    mapOf(
        "x86" to "i686-linux-android",
        "x86_64" to "x86_64-linux-android",
        "armeabi-v7a" to "armv7-linux-androideabi",
        "arm64-v8a" to "aarch64-linux-android"
    )

/**
 * Toolchain data class
 *
 * Provides convenience functions for determing Csrgo targets, compilers and other things required
 * for cross compiling
 *
 * @constructor Create empty Toolchain
 * @property androidAbi The ABI
 * @property compileApi The API version of the tools to use (set by minSDK)
 * @property ndkDirectory The path to the directory containing the NDK
 */
data class Toolchain(
    val androidAbi: String,
    val compileApi: Int,
    val ndkDirectory: File,
    val hostOS: String
) {

    init {
        require(cargoTriples.containsKey(androidAbi)) { "Unknown Abi $androidAbi" }
    }

    val cargoTriple
        get() = cargoTriples[androidAbi]!!

    // The string used for the ndk compiler
    // The "armeabi" target is the one odd one whose target names don't match
    private val compilerTriple: String
        get() = if (androidAbi == "armeabi-v7a") "armv7a-linux-androideabi" else cargoTriple

    // The binary dir of the NDK
    private val binDir
        get() = File(ndkDirectory, "toolchains/llvm/prebuilt/$hostOS/bin")
    // The C compiler for this toolchain (also the linker)
    val cc
        get() = File(binDir, "${compilerTriple}$compileApi-clang")
    // The archiver for this toolchain
    val ar
        get() = File(binDir, "llvm-ar")

    // The name of the environment variable that sets the path to the Linker
    val linkerEnvName
        get() = "CARGO_TARGET_${cargoTriple.uppercase().replace('-', '_')}_LINKER"
}

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

package com.android.designcompose.benchmark.battleship.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

const val PACKAGE_NAME = "com.android.designcompose.benchmarks.battleship"

@RunWith(AndroidJUnit4::class)
class StartupBenchmarks {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartup() =
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(StartupTimingMetric()),
            iterations = 5,
            startupMode = StartupMode.COLD
        ) {
            pressHome()
            startActivityAndWait()
        }

    @Test
    fun hotStartup() =
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(StartupTimingMetric()),
            iterations = 5,
            startupMode = StartupMode.HOT
        ) {
            pressHome()
            startActivityAndWait()
        }

    @Test
    fun noPrecompileStartup() =
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(StartupTimingMetric()),
            iterations = 5,
            compilationMode = CompilationMode.None(),
            startupMode = StartupMode.COLD
        ) {
            pressHome()
            startActivityAndWait()
        }

    @Test
    fun fullPrecompileStartup() =
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(StartupTimingMetric()),
            iterations = 5,
            compilationMode = CompilationMode.Full(),
            startupMode = StartupMode.COLD
        ) {
            pressHome()
            startActivityAndWait()
        }
}

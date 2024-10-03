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

package com.android.designcompose.benchmarks.battleship.benchmark

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.TraceSectionMetric.Mode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.android.designcompose.DCTraces
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This is a rough example of a benchmark, though one that doesn't focus on any behavior well.
 * Essentially it benchmarks a large recomposition, by clicking the Opponent's Playfield button,
 * which causes the full screen to re-draw.
 */
@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class ClickPlayfield {

    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun changePlayers() =
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics =
                listOf(
                    // This is how you capture a trace for a specific Composable. The "%" a wildcard
                    // and is required at the end because the trace string includes the line number
                    // of the composable.
                    TraceSectionMetric(
                        "com.android.designcompose.benchmarks.battleship.lib.BattleshipGen.MainFrame %",
                        Mode.Sum,
                    ),
                    TraceSectionMetric(DCTraces.DESIGNDOCINTERNAL, Mode.Sum),
                    TraceSectionMetric(DCTraces.DESIGNVIEW),
                    TraceSectionMetric(DCTraces.DESIGNFRAME_DE_SUBSCRIBE, Mode.Sum),
                    TraceSectionMetric(DCTraces.DESIGNFRAME_FINISHLAYOUT, Mode.Sum),
                    TraceSectionMetric(DCTraces.DESIGNTEXT_DE_SUBSCRIBE, Mode.Sum),
                    TraceSectionMetric(DCTraces.JNIADDNODES, Mode.Sum),
                    TraceSectionMetric(DCTraces.DOCSERVER_DOC, Mode.Sum),
                    TraceSectionMetric(DCTraces.DESIGNVIEW_INTERACTIONSCOPE, Mode.Sum),
                ),
            iterations = 10,
            /**
             * We want to measure a fresh start of the app, but don't want to capture the startup,
             * so we manually kill and restart the app in the setupBlock.
             */
            startupMode = null,
            setupBlock = {
                pressHome()
                killProcess()
                startActivityAndWait()
            },
        ) {
            val opponentBoardButton = device.findObject(By.textContains("Your Opponent"))
            opponentBoardButton.click()
            // Bug: GH-553 Shouldn't need to click twice
            opponentBoardButton.click()

            device.wait(Until.findObject(By.text("<- Your Board")), 4000)
            device.waitForIdle()
        }
}

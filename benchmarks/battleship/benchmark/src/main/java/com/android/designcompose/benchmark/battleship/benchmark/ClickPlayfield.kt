package com.android.designcompose.benchmark.battleship.benchmark

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
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
                    TraceSectionMetric(DCTraces.DESIGNFRAME_DE_SUBSCRIBE, Mode.Sum),
                    TraceSectionMetric(DCTraces.DESIGNFRAME_FINISHLAYOUT, Mode.Sum),
                    TraceSectionMetric(DCTraces.DESIGNTEXT_DE, Mode.Sum),
                    TraceSectionMetric(DCTraces.DESIGNDOCINTERNAL),
                    TraceSectionMetric(DCTraces.JNIADDNODES, Mode.Sum),
                    TraceSectionMetric(DCTraces.DESIGNVIEW, Mode.Sum),
                    TraceSectionMetric(DCTraces.DOCSERVER_DOC, Mode.Sum),
                    TraceSectionMetric(DCTraces.DESIGNVIEW_INTERACTIONSCOPE, Mode.Sum),
                    ),
            iterations = 2,
            startupMode = StartupMode.WARM,
            setupBlock = {
                pressHome()
                killProcess()
                startActivityAndWait()
            }
        ) {
            val opponentBoardButton = device.findObject(By.textContains("Your Opponent"))
            opponentBoardButton.click()
            opponentBoardButton.click()

            device.wait(Until.findObject(By.text("<- Your Board")), 4000)
            device.waitForIdle()
        }
}

package com.android.designcompose.benchmark.battleship.benchmark

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class ClickPlayfield {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun clickOnce() =
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics =
                listOf(
                    TraceSectionMetric("DesignDocInternal"),
                    TraceSectionMetric("DesignView"),
                    TraceSectionMetric("DocServer.doc"),
                    TraceSectionMetric("DecodeDiskDoc"),
                    StartupTimingMetric(),
                    TraceSectionMetric("DCLayout")
                ),
            iterations = 2,
            startupMode = StartupMode.COLD,
            setupBlock = { pressHome() }
        ) {
            startActivityAndWait()
            //            with(device) {
            //                click(256, 437)
            //                click(256, 437)
            //            }
        }

    @Test
    fun changePlayers() =
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics =
                listOf(
                    TraceSectionMetric("DesignDocInternal"),
                    TraceSectionMetric("DesignView"),
                    TraceSectionMetric("DocServer.doc"),
                    TraceSectionMetric("DecodeDiskDoc"),
                    TraceSectionMetric("DCLayout")
                ),
            iterations = 2,
            startupMode = StartupMode.COLD,
            setupBlock = {
                pressHome()
                startActivityAndWait()
            }
        ) {

               device. click(700, 120)
                device. click(700, 120)

                device.wait(Until.findObject(By.text("<- Your Basdfsadf`oard")), 4000 )
//                wait(Until.textContains("<- Your Board"))
//                opponentButton.click()
//                opponentButton.clickAndWait(Until.newWindow(), 1000)
//                assertNotNull(findObject(By.text("<- Your Board")))

        }
}

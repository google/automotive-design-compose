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

package com.android.designcompose.testapp.helloworld

import android.content.Context
import android.util.Log
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.DocRenderStatus
import com.android.designcompose.test.assertHasText
import com.android.designcompose.test.assertRenderStatus
import com.android.designcompose.test.onDCDoc
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziRule
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode


/**
 * Render hello world
 *
 * Basic test that uses Robolectric's native graphics to test that HelloWorld renders.
 *
 * Includes Roborazzi for Screenshot tests,
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet, sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RenderHelloWorld {
    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule
    val roborazziRule =
        RoborazziRule(
            composeRule = composeTestRule,
            // Specify the node to capture for the last image
            captureRoot = composeTestRule.onRoot(),
            options =
                RoborazziRule.Options(
                    outputDirectoryPath = "src/testDebug/roborazzi",
                    // Always capture the last image of the test
                    captureType = RoborazziRule.CaptureType.LastImage(),
                ),
        )

    @Test
    fun testHello() {
        with(composeTestRule) {
            setContent { HelloWorldDoc.mainFrame(name = "Testers!") }
            onDCDoc(HelloWorldDoc).assertRenderStatus(DocRenderStatus.Rendered)
            onDCDoc(HelloWorldDoc).assertHasText("Testers!", substring = true)
        }
    }

    @Test
    fun testLoadScalableDcf() {
        val filePath = "ScalableUiTest_1OqNt1MCCc4E8KXS0OVcxS.dcf"
        val dcfFile = this.javaClass.classLoader?.getResource(filePath)
        assertNotNull(dcfFile)

        try {
            val stream = dcfFile?.openStream()
            assertNotNull(stream)

            val context = ApplicationProvider.getApplicationContext<Context>()
            assertNotNull(context)

            // Check that panels can be loaded from the document
            val loader = PanelStateDocLoader(context)
            val docId = "1OqNt1MCCc4E8KXS0OVcxS"
            val states = loader.loadPanelStates(stream, docId)
            assertNotNull(states)
            assertEquals(states.size, 11)

            // Check that the PanelApp exists, and it has the appropriate variants
            val panelApp = states.find { it.id == "PanelApp" }
            assertNotNull(panelApp)
            assertNotNull(panelApp?.getVariant("#panel-app=open1"))
            assertNotNull(panelApp?.getVariant("#panel-app=open2"))
            assertNotNull(panelApp?.getVariant("#panel-app=open3"))
            assertNotNull(panelApp?.getVariant("#panel-app=full"))

            // Check that the current variant is the open3 variant
            val open3 = panelApp?.getVariant("#panel-app=open3")
            assertEquals(open3, panelApp?.currentVariant)

            // Check the bounds of one of the variants
            val density = context.resources.displayMetrics.density
            assertEquals(500.0f, (open3?.bounds?.width() ?: 0) / density)
            assertEquals(690.0f, (open3?.bounds?.height() ?: 0) / density)
            assertEquals(150.0f, (open3?.bounds?.left ?: 0) / density)
            assertEquals(0.0f, (open3?.bounds?.top ?: 0) / density)

            // Check other properties of the variant
            assertEquals(true, open3?.isVisible)
            assertEquals(1.0f, open3?.alpha)
            assertEquals(0, open3?.layer)

        } catch(e: IOException) {
            assert(false, { "Exception in test: $e" })
        }
    }
}

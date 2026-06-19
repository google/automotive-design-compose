/*
 * Copyright 2026 Google LLC
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

package com.android.designcompose.testapp.validation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.DocRenderStatus
import com.android.designcompose.TestUtils
import com.android.designcompose.test.assertRenderStatus
import com.android.designcompose.test.internal.captureRootRoboImage
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.test.onDCDoc
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.ComponentTapCallbackDoc
import com.android.designcompose.testapp.validation.examples.NestedComponentModifierTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Regression test for nested-component Modifier propagation (PR #2466).
 *
 * A `Modifier.rotate(45f)` is registered on the nested "CompTest" component (see
 * [NestedComponentModifierTest]). The captured snapshot must show CompTest rendered with the
 * rotation applied. Reverting the sub-SquooshRoot modifier-wrap (e.g. removing the `renderSubtree`
 * branch in SquooshTreeBuilder) drops the Modifier, so the nested component renders unrotated and
 * this snapshot changes -- failing `verifyRoborazziDebug`.
 *
 * Record the golden image with: ./gradlew :validation-app:recordRoborazziDebug --tests
 * "*NestedComponentModifierValidation*"
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w1920dp-h1500dp-xlarge-long-notround-any-xhdpi-keyshidden-nonav")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NestedComponentModifierValidation {
    @get:Rule val clearStateTestRule = TestUtils.ClearStateTestRule()
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)
    @get:Rule val interFontTestRule = InterFontTestRule()

    @Test
    fun nestedComponentModifierIsApplied() {
        with(composeTestRule) {
            setContent { NestedComponentModifierTest() }
            waitForIdle()
            // Fail loudly if the doc failed to load, rather than silently capturing a blank.
            onDCDoc(ComponentTapCallbackDoc).assertRenderStatus(DocRenderStatus.Rendered)
            captureRootRoboImage("NestedComponentModifier-rotated")
        }
    }
}

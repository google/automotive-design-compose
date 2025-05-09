/*
 * Copyright 2025 Google LLC
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.TestUtils
import com.android.designcompose.common.FSAAS_DOC_VERSION
import com.android.designcompose.test.assertHasText
import com.android.designcompose.test.internal.captureRootRoboImage
import com.android.designcompose.test.internal.designComposeRoborazziRule
import com.android.designcompose.test.onDCDoc
import com.android.designcompose.testapp.common.InterFontTestRule
import com.android.designcompose.testapp.validation.examples.ProtoChangeDoc
import com.android.designcompose.testapp.validation.examples.ProtoChangeTest
import com.android.designcompose.testapp.validation.examples.ProtoChangeTestV27
import com.android.designcompose.testapp.validation.examples.ProtoChangeV27Doc
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
class ProtoVersions {
    @get:Rule val clearStateTestRule = TestUtils.ClearStateTestRule()
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    @get:Rule val roborazziRule = designComposeRoborazziRule(javaClass.simpleName)
    @get:Rule val interFontRule = InterFontTestRule()

    // Test that the latest version renders and that the version matches.
    @Test
    fun protoLatestRenders() {
        with(composeTestRule) {
            val dcVersion = mutableIntStateOf(0)
            setContent { ProtoChangeTest { docDcVersion -> dcVersion.intValue = docDcVersion } }
            with(onDCDoc(ProtoChangeDoc)) { assertHasText("Hello") }
            assert(dcVersion.intValue == FSAAS_DOC_VERSION)
            captureRootRoboImage("proto-latest")
        }
    }

    // Add new tests here when protobuf changes are made.

    // Test that V27 renders and that the version matches.
    @Test
    fun proto27Renders() {
        with(composeTestRule) {
            val dcVersion = mutableIntStateOf(0)
            setContent { ProtoChangeTestV27 { docDcVersion -> dcVersion.intValue = docDcVersion } }
            with(onDCDoc(ProtoChangeV27Doc)) { assertHasText("Hello") }
            assert(dcVersion.intValue == 27)
            captureRootRoboImage("proto-v27")
        }
    }
}

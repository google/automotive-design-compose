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
 * distributed under the License is distributed on an "AS IS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.designcompose

import androidx.compose.ui.text.font.FontFamily
import com.android.designcompose.common.DesignDocId
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DesignSettingsTest {

    @Test
    fun testSetRawResourceId() {
        val docId = DesignDocId("test")
        DesignSettings.setRawResourceId(docId, 123)
        assertThat(DesignSettings.rawResourceId).containsKey(docId)
        assertThat(DesignSettings.rawResourceId[docId]).isEqualTo(123)
    }

    @Test
    fun testClearRawResources() {
        val docId = DesignDocId("test")
        DesignSettings.setRawResourceId(docId, 123)
        DesignSettings.clearRawResources()
        assertThat(DesignSettings.rawResourceId).isEmpty()
    }

    @Test
    fun testAddFontFamily() {
        val fontFamily = FontFamily.Default
        DesignSettings.addFontFamily("testFont", fontFamily)
        assertThat(DesignSettings.fontFamily("testFont")).isEqualTo(fontFamily)
    }
}

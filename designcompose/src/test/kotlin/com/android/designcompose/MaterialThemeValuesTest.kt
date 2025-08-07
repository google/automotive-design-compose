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

package com.android.designcompose

import androidx.compose.material3.lightColorScheme
import com.android.designcompose.common.DesignDocId
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MaterialThemeValuesTest {

    @Before
    fun setUp() {
        val collection =
            com.android.designcompose.definition.element.Collection.newBuilder()
                .setName(MATERIAL_THEME_COLLECTION_NAME)
                .build()
        val variableMap =
            com.android.designcompose.definition.element.VariableMap.newBuilder()
                .putCollectionsById("collection1", collection)
                .build()
        VariableManager.init(DesignDocId("doc1"), variableMap)
    }

    @Test
    fun testGetColor() {
        val colorScheme = lightColorScheme()
        val state = VariableState(useMaterialTheme = true, materialColorScheme = colorScheme)

        assertThat(
                MaterialThemeValues.getColor(MaterialThemeConstants.PRIMARY, "collection1", state)
            )
            .isEqualTo(colorScheme.primary)
        assertThat(MaterialThemeValues.getColor("InvalidName", "collection1", state)).isNull()
    }

    @Test
    fun testGetColorWrongCollection() {
        val colorScheme = lightColorScheme()
        val state = VariableState(useMaterialTheme = true, materialColorScheme = colorScheme)

        assertThat(
                MaterialThemeValues.getColor(
                    MaterialThemeConstants.PRIMARY,
                    "wrong_collection",
                    state,
                )
            )
            .isNull()
    }

    @Test
    fun testGetColorNoMaterialTheme() {
        val colorScheme = lightColorScheme()
        val state = VariableState(useMaterialTheme = false, materialColorScheme = colorScheme)

        assertThat(
                MaterialThemeValues.getColor(MaterialThemeConstants.PRIMARY, "collection1", state)
            )
            .isNull()
    }
}

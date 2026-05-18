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

package com.android.designcompose.common

import com.android.designcompose.definition.view.View
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VariantPropertyMapTest {
    @Test
    fun testAddProperty() {
        val variantPropertyMap = VariantPropertyMap()
        variantPropertyMap.addProperty("parent", "prop", "value")
        assertThat(variantPropertyMap.propertyToParentNodes["prop"]).containsExactly("parent")
        assertThat(variantPropertyMap.propertyMap["parent"]?.get("prop")).containsExactly("value")
    }

    @Test
    fun resolveVariantNameToView_directMatch() {
        val variantPropertyMap = VariantPropertyMap()
        variantPropertyMap.addProperty("parent", "prop1", "value1")
        variantPropertyMap.addProperty("parent", "prop2", "value2")

        val view = View.newBuilder().setName("testView").build()
        val variantViewMap = hashMapOf("prop1=value1,prop2=value2" to view)

        val result =
            variantPropertyMap.resolveVariantNameToView(
                "prop1=value1,prop2=value2",
                "parent",
                variantViewMap,
            )
        assertThat(result).isEqualTo(view)
    }

    @Test
    fun resolveVariantNameToView_wildcardMatch() {
        val variantPropertyMap = VariantPropertyMap()
        variantPropertyMap.addProperty("parent", "prop1", "value1")
        variantPropertyMap.addProperty("parent", "prop2", "*")

        val view = View.newBuilder().setName("testView").build()
        val variantViewMap = hashMapOf("prop1=value1,prop2=*" to view)

        val result =
            variantPropertyMap.resolveVariantNameToView(
                "prop1=value1,prop2=value2",
                "parent",
                variantViewMap,
            )
        assertThat(result).isEqualTo(view)
    }

    @Test
    fun resolveVariantNameToView_noMatch() {
        val variantPropertyMap = VariantPropertyMap()
        variantPropertyMap.addProperty("parent", "prop1", "value1")
        variantPropertyMap.addProperty("parent", "prop2", "value2")

        val view = View.newBuilder().setName("testView").build()
        val variantViewMap = hashMapOf("prop1=value1,prop2=value2" to view)

        val result =
            variantPropertyMap.resolveVariantNameToView(
                "prop1=value1,prop2=value3",
                "parent",
                variantViewMap,
            )
        assertThat(result).isNull()
    }

    @Test
    fun resolveVariantNameToView_extraProperties() {
        val variantPropertyMap = VariantPropertyMap()
        variantPropertyMap.addProperty("parent", "prop1", "value1")
        variantPropertyMap.addProperty("parent", "prop2", "value2")
        variantPropertyMap.addProperty("parent", "prop3", "value3")

        val view = View.newBuilder().setName("testView").build()
        val variantViewMap = hashMapOf("prop1=value1,prop2=value2,prop3=value3" to view)

        val result =
            variantPropertyMap.resolveVariantNameToView(
                "prop1=value1,prop2=value2",
                "parent",
                variantViewMap,
            )
        assertThat(result).isEqualTo(view)
    }

    @Test
    fun resolveVariantNameToView_wildcardAndExtraProperties() {
        val variantPropertyMap = VariantPropertyMap()
        variantPropertyMap.addProperty("parent", "prop1", "value1")
        variantPropertyMap.addProperty("parent", "prop2", "*")
        variantPropertyMap.addProperty("parent", "prop3", "value3")

        val view = View.newBuilder().setName("testView").build()
        val variantViewMap = hashMapOf("prop1=value1,prop2=*,prop3=value3" to view)

        val result =
            variantPropertyMap.resolveVariantNameToView(
                "prop1=value1,prop2=value2",
                "parent",
                variantViewMap,
            )
        assertThat(result).isEqualTo(view)
    }

    @Test
    fun resolveVariantNameToView_emptyNodeName() {
        val variantPropertyMap = VariantPropertyMap()
        variantPropertyMap.addProperty("parent", "prop1", "value1")

        val view = View.newBuilder().setName("testView").build()
        val variantViewMap = hashMapOf("prop1=value1" to view)

        val result = variantPropertyMap.resolveVariantNameToView("", "parent", variantViewMap)
        assertThat(result).isNull()
    }
}

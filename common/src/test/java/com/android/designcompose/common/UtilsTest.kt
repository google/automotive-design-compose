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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UtilsTest {
    @Test
    fun testCreateSortedVariantName() {
        val unsorted = "c=d, a=b, e=f"
        val sorted = "a=b,c=d,e=f"
        assertThat(createSortedVariantName(unsorted)).isEqualTo(sorted)
    }

    @Test
    fun testNodeNameToPropertyValueList() {
        val nodeName = "a=b, c=d, e=f"
        val propertyValueList = nodeNameToPropertyValueList(nodeName)
        assertThat(propertyValueList)
            .containsExactly(Pair("a", "b"), Pair("c", "d"), Pair("e", "f"))

        val nodeName2 = "a=b"
        val propertyValueList2 = nodeNameToPropertyValueList(nodeName2)
        assertThat(propertyValueList2).containsExactly(Pair("a", "b"))

        val nodeName3 = ""
        val propertyValueList3 = nodeNameToPropertyValueList(nodeName3)
        assertThat(propertyValueList3).isEmpty()
    }

    @Test
    fun testAddProperty() {
        val variantPropertyMap = VariantPropertyMap()
        variantPropertyMap.addProperty("parent", "prop", "variant")
        assertThat(variantPropertyMap.propertyToParentNodes["prop"]).containsExactly("parent")
        assertThat(variantPropertyMap.propertyMap["parent"]?.get("prop")).containsExactly("variant")
    }
}

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

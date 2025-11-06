package com.android.designcompose.common

import com.android.designcompose.definition.view.View
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VariantPropertyMapTest {
    private lateinit var variantPropertyMap: VariantPropertyMap
    private lateinit var variantViewMap: HashMap<String, View>
    private val view1 = View.newBuilder().setName("view1").build()
    private val view2 = View.newBuilder().setName("view2").build()
    private val view3 = View.newBuilder().setName("view3").build()
    private val view4 = View.newBuilder().setName("view4").build()
    private val view5 = View.newBuilder().setName("view5").build()

    @Before
    fun setup() {
        variantPropertyMap = VariantPropertyMap()
        variantViewMap = HashMap()

        // Populate variantPropertyMap
        variantPropertyMap.addProperty("componentSet", "prop1", "val1")
        variantPropertyMap.addProperty("componentSet", "prop1", "val2")
        variantPropertyMap.addProperty("componentSet", "prop2", "valA")
        variantPropertyMap.addProperty("componentSet", "prop2", "valB")
        variantPropertyMap.addProperty("componentSet", "prop3", "valX")
        variantPropertyMap.addProperty("componentSet", "prop3", "*")

        // Populate variantViewMap
        variantViewMap["prop1=val1,prop2=valA"] = view1
        variantViewMap["prop1=val2,prop2=valB"] = view2
        variantViewMap["prop1=val1,prop2=valB"] = view3
        variantViewMap["prop1=val1,prop3=valX"] = view4
        variantViewMap["prop1=val1,prop3=*"] = view5
    }

    @Test
    fun resolveVariantNameToView_exactMatch() {
        val customVariantPropertyMap = VariantPropertyMap()
        customVariantPropertyMap.addProperty("componentSet", "prop1", "val1")
        customVariantPropertyMap.addProperty("componentSet", "prop2", "valA")
        val customVariantViewMap = HashMap<String, View>()
        customVariantViewMap["prop1=val1,prop2=valA"] = view1
        val result =
            customVariantPropertyMap.resolveVariantNameToView(
                "prop1=val1,prop2=valA",
                "componentSet",
                customVariantViewMap,
            )
        assertThat(result).isEqualTo(view1)
    }

    @Test
    fun resolveVariantNameToView_wildcardMatch() {
        val customVariantPropertyMap = VariantPropertyMap()
        customVariantPropertyMap.addProperty("componentSet", "prop1", "val1")
        customVariantPropertyMap.addProperty("componentSet", "prop3", "valY")
        customVariantPropertyMap.addProperty("componentSet", "prop3", "*")
        val customVariantViewMap = HashMap<String, View>()
        customVariantViewMap["prop1=val1,prop3=*"] = view5
        // This should match "prop1=val1,prop3=*"
        val result =
            customVariantPropertyMap.resolveVariantNameToView(
                "prop1=val1,prop3=valY", // valY doesn't exist, should match *
                "componentSet",
                customVariantViewMap,
            )
        assertThat(result).isEqualTo(view5)
    }

    @Test
    fun resolveVariantNameToView_noMatch() {
        val result =
            variantPropertyMap.resolveVariantNameToView(
                "prop1=val3,prop2=valC",
                "componentSet",
                variantViewMap,
            )
        assertThat(result).isNull()
    }

    @Test
    fun resolveVariantNameToView_extraPropertiesInParent() {
        // This test needs a specific setup, so let's create a new map.
        val customVariantPropertyMap = VariantPropertyMap()
        customVariantPropertyMap.addProperty("componentSet", "prop1", "val1")
        customVariantPropertyMap.addProperty("componentSet", "prop2", "valA")
        customVariantPropertyMap.addProperty("componentSet", "prop4", "valZ")

        val customVariantViewMap = HashMap<String, View>()
        customVariantViewMap["prop1=val1,prop2=valA,prop4=valZ"] = view1

        val result =
            customVariantPropertyMap.resolveVariantNameToView(
                "prop1=val1,prop2=valA",
                "componentSet",
                customVariantViewMap,
            )
        assertThat(result).isEqualTo(view1)
    }

    @Test
    fun resolveVariantNameToView_emptyNodeName() {
        val result = variantPropertyMap.resolveVariantNameToView("", "componentSet", variantViewMap)
        assertThat(result).isNull()
    }

    @Test
    fun resolveVariantNameToView_emptyComponentSet() {
        val result =
            variantPropertyMap.resolveVariantNameToView("prop1=val1,prop2=valA", "", variantViewMap)
        assertThat(result).isNull()
    }

    @Test
    fun resolveVariantNameToView_emptyMaps() {
        val result =
            VariantPropertyMap()
                .resolveVariantNameToView("prop1=val1,prop2=valA", "componentSet", HashMap())
        assertThat(result).isNull()
    }

    @Test
    fun resolveVariantNameToView_findViewFromPossibleNames() {
        // This test case is designed to exercise the findViewFromPossibleNodeNames function
        // We have a variant that only exists with a wildcard
        val customVariantPropertyMap = VariantPropertyMap()
        customVariantPropertyMap.addProperty("componentSet", "prop1", "val1")
        customVariantPropertyMap.addProperty("componentSet", "prop2", "valB")
        customVariantPropertyMap.addProperty("componentSet", "prop2", "*")
        val customVariantViewMap = HashMap<String, View>()
        customVariantViewMap["prop1=val1,prop2=*"] = view1
        val result =
            customVariantPropertyMap.resolveVariantNameToView(
                "prop1=val1,prop2=valB",
                "componentSet",
                customVariantViewMap,
            )
        assertThat(result).isEqualTo(view1)
    }
}

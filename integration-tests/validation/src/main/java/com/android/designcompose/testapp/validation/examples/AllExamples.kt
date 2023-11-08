package com.android.designcompose.testapp.validation.examples

import androidx.compose.runtime.Composable

val EXAMPLES: ArrayList<Triple<String, @Composable () -> Unit, String?>> =
    arrayListOf(
        Triple("Hello", { HelloWorld() }, HelloWorldDoc.javaClass.name),
        Triple("Image Update", { ImageUpdateTest() }, ImageUpdateTestDoc.javaClass.name),
        Triple("Telltales", { TelltaleTest() }, TelltaleTestDoc.javaClass.name),
        Triple("OpenLink", { OpenLinkTest() }, OpenLinkTestDoc.javaClass.name),
        Triple("Variant *", { VariantAsteriskTest() }, VariantAsteriskTestDoc.javaClass.name),
        Triple("Alignment", { AlignmentTest() }, AlignmentTestDoc.javaClass.name),
        Triple("Battleship", { BattleshipTest() }, BattleshipDoc.javaClass.name),
        Triple("H Constraints", { HConstraintsTest() }, ConstraintsDoc.javaClass.name),
        Triple("V Constraints", { VConstraintsTest() }, ConstraintsDoc.javaClass.name),
        Triple("Interaction", { InteractionTest() }, InteractionTestDoc.javaClass.name),
        Triple("Shadows", { ShadowsTest() }, ShadowsTestDoc.javaClass.name),
        Triple("Item Spacing", { ItemSpacingTest() }, ItemSpacingTestDoc.javaClass.name),
        Triple(
            "Recurse Customization",
            { RecursiveCustomizations() },
            RecursiveCustomizationsDoc.javaClass.name
        ),
        Triple("Color Tint", { ColorTintTest() }, ColorTintTestDoc.javaClass.name),
        Triple(
            "Variant Properties",
            { VariantPropertiesTest() },
            VariantPropertiesTestDoc.javaClass.name
        ),
        // Lazy Grid doesn't actually use a doc
        Triple("Lazy Grid", { LazyGridItemSpans() }, null),
        Triple("Grid Layout", { GridLayoutTest() }, GridLayoutTestDoc.javaClass.name),
        Triple("Grid Widget", { GridWidgetTest() }, GridWidgetTestDoc.javaClass.name),
        Triple("List Widget", { ListWidgetTest() }, ListWidgetTestDoc.javaClass.name),
        Triple("1px Separator", { OnePxSeparatorTest() }, OnePxSeparatorDoc.javaClass.name),
        Triple(
            "Variant Interactions",
            { VariantInteractionsTest() },
            VariantInteractionsTestDoc.javaClass.name
        ),
        Triple(
            "Layout Replacement",
            { LayoutReplacementTest() },
            LayoutReplacementTestDoc.javaClass.name
        ),
        Triple("Text Elide", { TextElideTest() }, TextElideTestDoc.javaClass.name),
        Triple("Fancy Fills", { FancyFillTest() }, FancyFillTestDoc.javaClass.name),
        Triple("Fill Container", { FillTest() }, FillTestDoc.javaClass.name),
        Triple("CrossAxis Fill", { CrossAxisFillTest() }, CrossAxisFillTestDoc.javaClass.name),
        Triple(
            "Grid Layout Documentation",
            { GridLayoutDocumentation() },
            GridLayoutDoc.javaClass.name
        ),
        Triple("Blend Modes", { BlendModeTest() }, BlendModeTestDoc.javaClass.name),
        Triple(
            "Vector Rendering",
            { VectorRenderingTest() },
            VectorRenderingTestDoc.javaClass.name
        ),
        Triple("Dials Gauges", { DialsGaugesTest() }, DialsGaugesTestDoc.javaClass.name),
        Triple("Masks", { MaskTest() }, MaskTestDoc.javaClass.name),
        Triple("Variable Borders", { VariableBorderTest() }, VariableBorderTestDoc.javaClass.name),
        Triple("Layout Tests", { LayoutTests() }, LayoutTestsDoc.javaClass.name)
    )

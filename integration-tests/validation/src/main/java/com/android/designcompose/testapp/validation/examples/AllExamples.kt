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

package com.android.designcompose.testapp.validation.examples

import androidx.compose.runtime.Composable
import com.android.designcompose.benchmarks.battleship.lib.BattleshipDoc
import com.android.designcompose.benchmarks.battleship.lib.BattleshipTest

val EXAMPLES: ArrayList<Triple<String, @Composable () -> Unit, String?>> =
    arrayListOf(
        Triple("Hello", { HelloWorld() }, HelloWorldDoc.javaClass.name),
        Triple("HelloBye", { HelloBye() }, HelloByeDoc.javaClass.name),
        Triple("HelloSquoosh", { HelloSquoosh() }, HelloWorldDoc.javaClass.name),
        Triple("HelloVersion", { HelloVersion() }, HelloVersionDoc.javaClass.name),
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
        Triple("Styled Text Runs", { StyledTextRunsTest() }, StyledTextRunsDoc.javaClass.name),
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
        Triple("Layout Tests", { LayoutTests() }, LayoutTestsDoc.javaClass.name),
        Triple("Custom Brush", { CustomBrushTest() }, CustomBrushTestDoc.javaClass.name),
        Triple("Component Replace", { ComponentReplaceTest() }, ComponentReplaceDoc.javaClass.name),
        Triple(
            "Squoosh Component Replace",
            { SquooshComponentReplaceTest() },
            ComponentReplaceDoc.javaClass.name
        ),
        Triple("SA", { SmartAnimateTest() }, SmartAnimateTestDoc.javaClass.name),
        Triple("SA Variant", { VariantAnimationTest() }, VariantAnimationTestDoc.javaClass.name),
        // Don't run in CI, need an annotation.
        // Triple("Compositing", { CompositingViewsTest() },
        // CompositingViewsTestDoc.javaClass.name),
        Triple("Text Inval", { TextResizingTest() }, TextResizingTestDoc.javaClass.name),
        Triple("Shared Customization", { ModuleExample() }, ModuleExampleDoc.javaClass.name),
        Triple("Variable Modes", { VariableModesTest() }, VariablesTestDoc.javaClass.name),
        // GH-636: Test takes too long to execute.
        // Triple("Very large File", { VeryLargeFile() }, VeryLargeFileDoc.javaClass.name)

        // Rendering parity tests. Largely static views of standard test document nodes.
        // TODO: should changes be here?
        // TODO: is it possible to specify the document and node name here? (rather than hard coding them elsewhere)?
        //Triple("Layout Test Stage Node", { StaticDocument() }, StaticDocumentDoc.javaClass.name),
        Triple("Layout Test Stage Node", { StaticDocument("Gv63fYTzpeH2ZtxP4go31E", "#stage") }, StaticDocumentDoc.javaClass.name),
        Triple("Layout Test Stuff Node", { StaticDocument("Gv63fYTzpeH2ZtxP4go31E", "#stage-stuff") }, StaticDocumentDoc.javaClass.name),
        Triple("Layout Test Child Node", { StaticDocument("Gv63fYTzpeH2ZtxP4go31E", "#stage-children") }, StaticDocumentDoc.javaClass.name),
        Triple("Layout Test Rotations Node", { StaticDocument("Gv63fYTzpeH2ZtxP4go31E", "#stage-rotations") }, StaticDocumentDoc.javaClass.name),
        Triple("Layout Test Constraints Node", { StaticDocument("Gv63fYTzpeH2ZtxP4go31E", "#stage-constraintes") }, StaticDocumentDoc.javaClass.name),
        Triple("Layout Test Line Node", { StaticDocument("Gv63fYTzpeH2ZtxP4go31E", "#stageline") }, StaticDocumentDoc.javaClass.name),
        Triple("Blendmode Test Stage Node", { StaticDocument("ZqX5i5g6inv9tANIwMMXUV", "#stage") }, StaticDocumentDoc.javaClass.name),
        //Triple("Layout Test AutoLayout Node", { StaticDocument("JIjE9oKQbq8ipi66ab5UaK", "#AutoLayout") }, StaticDocumentDoc.javaClass.name),
    )

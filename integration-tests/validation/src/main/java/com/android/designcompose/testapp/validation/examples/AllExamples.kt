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
import java.time.Clock

val EXAMPLES: ArrayList<Triple<String, @Composable () -> Unit, String?>> =
    arrayListOf(
        // First comes the "HelloWorld" examples.
        Triple("Hello", { HelloWorld() }, HelloWorldDoc.javaClass.name),
        Triple("HelloBye", { HelloBye() }, HelloByeDoc.javaClass.name),
        Triple("HelloVersion", { HelloVersion() }, HelloVersionDoc.javaClass.name),
        // Alphabetically ordered and trying to put similar tests together...
        Triple("Alignment", { AlignmentTest() }, AlignmentTestDoc.javaClass.name),
        Triple(
            "AutoLayout MinMax",
            { AutoLayoutMinMaxTest() },
            AutoLayoutMinMaxTestDoc.javaClass.name,
        ),
        Triple("Battleship", { BattleshipTest() }, BattleshipDoc.javaClass.name),
        Triple("Blend Modes", { BlendModeTest() }, BlendModeTestDoc.javaClass.name),
        Triple("Component Replace", { ComponentReplaceTest() }, ComponentReplaceDoc.javaClass.name),
        Triple(
            "ComponentTapCallback",
            { ComponentTapCallbackTest() },
            ComponentTapCallbackDoc.javaClass.name,
        ),
        Triple("Custom Brush", { CustomBrushTest() }, CustomBrushTestDoc.javaClass.name),
        // Dials gauges and progress vectors
        Triple("Dials Gauges", { DialsGaugesTest() }, DialsGaugesTestDoc.javaClass.name),
        Triple("Progress Vectors", { ProgressVectorTest() }, DialsGaugesTestDoc.javaClass.name),
        Triple("Fancy Fills", { FancyFillTest() }, FancyFillTestDoc.javaClass.name),
        Triple("Fill Container", { FillTest() }, FillTestDoc.javaClass.name),
        Triple(
            "Grid Layout Documentation",
            { GridLayoutDocumentation() },
            GridLayoutDoc.javaClass.name,
        ),
        // H and V constraints
        Triple("H Constraints", { HConstraintsTest() }, ConstraintsDoc.javaClass.name),
        Triple("V Constraints", { VConstraintsTest() }, ConstraintsDoc.javaClass.name),
        Triple("Image Update", { ImageUpdateTest() }, ImageUpdateTestDoc.javaClass.name),
        Triple("Interaction", { InteractionTest() }, InteractionTestDoc.javaClass.name),
        // Layout related tests
        Triple("CrossAxis Fill", { CrossAxisFillTest() }, CrossAxisFillTestDoc.javaClass.name),
        Triple(
            "Ignore Auto Layout",
            { IgnoreAutoLayoutTest() },
            IgnoreAutoLayoutTestDoc.javaClass.name,
        ),
        Triple("Item Spacing", { ItemSpacingTest() }, ItemSpacingTestDoc.javaClass.name),
        Triple(
            "Layout Replacement",
            { LayoutReplacementTest() },
            LayoutReplacementTestDoc.javaClass.name,
        ),
        Triple(
            "Recurse Customization",
            { RecursiveCustomizations() },
            RecursiveCustomizationsDoc.javaClass.name,
        ),
        Triple("Layout Tests", { LayoutTests() }, LayoutTestsDoc.javaClass.name),
        // Masks/shadows
        Triple("Masks", { MaskTest() }, MaskTestDoc.javaClass.name),
        Triple("Shadows", { ShadowsTest() }, ShadowsTestDoc.javaClass.name),
        // Text validations
        Triple("Text Elide", { TextElideTest() }, TextElideTestDoc.javaClass.name),
        Triple("Text Inval", { TextResizingTest() }, TextResizingTestDoc.javaClass.name),
        Triple("Styled Text Runs", { StyledTextRunsTest() }, StyledTextRunsDoc.javaClass.name),
        Triple("Shared Customization", { ModuleExample() }, ModuleExampleDoc.javaClass.name),
        Triple(
            "State Customizations",
            { StateCustomizationsTest(Clock.systemDefaultZone()) },
            StateCustomizationsDoc.javaClass.name,
        ),
        Triple("Telltales", { TelltaleTest() }, TelltaleTestDoc.javaClass.name),
        // Variant tests
        Triple("Variant *", { VariantAsteriskTest() }, VariantAsteriskTestDoc.javaClass.name),
        Triple(
            "Variant Interactions",
            { VariantInteractionsTest() },
            VariantInteractionsTestDoc.javaClass.name,
        ),
        Triple(
            "Variant Properties",
            { VariantPropertiesTest() },
            VariantPropertiesTestDoc.javaClass.name,
        ),
        Triple("Variable Borders", { VariableBorderTest() }, VariableBorderTestDoc.javaClass.name),
        Triple("Variable Modes", { VariableModesTest() }, VariablesTestDoc.javaClass.name),
        Triple(
            "Vector Rendering",
            { VectorRenderingTest() },
            VectorRenderingTestDoc.javaClass.name,
        ),
        Triple("1px Separator", { OnePxSeparatorTest() }, OnePxSeparatorDoc.javaClass.name),
        // Don't run in CI, need an annotation.
        // Triple("Compositing", { CompositingViewsTest() },
        // CompositingViewsTestDoc.javaClass.name),
        Triple(
            "Component Replace Relayout",
            { ComponentReplaceRelayoutTest() },
            ComponentReplaceRelayoutDoc.javaClass.name,
        ),
        Triple("OpenLink", { OpenLinkTest() }, OpenLinkTestDoc.javaClass.name),
        Triple("Color Tint", { ColorTintTest() }, ColorTintTestDoc.javaClass.name),
        Triple("Scrolling", { ScrollingTest() }, ScrollingTestDoc.javaClass.name),
        // GH-636: Test takes too long to execute.
        // Triple("Very large File", { VeryLargeFile() }, VeryLargeFileDoc.javaClass.name)
    )

// Default renderer doesn't support animations
val SQUOOSH_ONLY_EXAMPLES: ArrayList<Triple<String, @Composable () -> Unit, String?>> =
    arrayListOf(
        Triple("SA", { SmartAnimateTest() }, SmartAnimateTestDoc.javaClass.name),
        Triple("SA Variant", { VariantAnimationTest() }, VariantAnimationTestDoc.javaClass.name),
        Triple(
            "SA Variant Timelines",
            { VariantAnimationTimelineTest() },
            VariantAnimationTimelineTestDoc.javaClass.name,
        ),
    )

val DEFAULT_RENDERER_ONLY_EXAMPLES: ArrayList<Triple<String, @Composable () -> Unit, String?>> =
    arrayListOf(
        // No support for hyperlinks.
        Triple("Hyperlink", { HyperlinkTest() }, HyperlinkValidationDoc.javaClass.name),
        // Lazy Grid doesn't actually use a doc
        // This example is not using any of the renderers.
        Triple("Lazy Grid", { LazyGridItemSpans() }, null),
        // Squoosh doesn't work with ListContent
        Triple("Grid Layout", { GridLayoutTest() }, GridLayoutTestDoc.javaClass.name),
        Triple("Grid Widget", { GridWidgetTest() }, GridWidgetTestDoc.javaClass.name),
        Triple("List Widget", { ListWidgetTest() }, ListWidgetTestDoc.javaClass.name),
    )

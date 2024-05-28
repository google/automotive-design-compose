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
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// Layout "stage"
@DesignDoc(id = "Gv63fYTzpeH2ZtxP4go31E")
private interface LayoutStage {
    @DesignComponent(node = "#stage") fun MainFrame()
}
// Layout "stuff"
@DesignDoc(id = "Gv63fYTzpeH2ZtxP4go31E")
private interface LayoutStageStuff {
    @DesignComponent(node = "#stage-stuff") fun MainFrame()
}
// Layout "Children"
@DesignDoc(id = "Gv63fYTzpeH2ZtxP4go31E")
private interface LayoutStageChildren {
    @DesignComponent(node = "#stage-children") fun MainFrame()
}
// Layout "Rotations"
@DesignDoc(id = "Gv63fYTzpeH2ZtxP4go31E")
private interface LayoutStageRotations {
    @DesignComponent(node = "#stage-rotations") fun MainFrame()
}
// Layout "Constraints"
@DesignDoc(id = "Gv63fYTzpeH2ZtxP4go31E")
private interface LayoutStageConstraints {
    @DesignComponent(node = "#stage-constraints") fun MainFrame()
}
// Layout "Line"
@DesignDoc(id = "Gv63fYTzpeH2ZtxP4go31E")
private interface LayoutStageLine {
    @DesignComponent(node = "#stageline") fun MainFrame()
}

// "Absolute Layout Alignment Test",
@DesignDoc(id = "JIjE9oKQbq8ipi66ab5UaK")
private interface AbsoluteLayoutAlignmentTest {
    @DesignComponent(node = "#Test") fun MainFrame()
}

// "Absolute Layout Alignment AutoLayout Test"
@DesignDoc(id = "JIjE9oKQbq8ipi66ab5UaK")
private interface AbsoluteLayoutAutoLayoutTest {
    @DesignComponent(node = "#AutoLayout") fun MainFrame()
}

// "Absolute Layout Alignment Vector Alignment Test"
@DesignDoc(id = "JIjE9oKQbq8ipi66ab5UaK")
private interface AbsoluteLayoutVectorAlignmentTest {
    @DesignComponent(node = "#VectorAlignment") fun MainFrame()
}

// "Absolute Layout Alignment Text Alignment Test",
@DesignDoc(id = "JIjE9oKQbq8ipi66ab5UaK")
private interface AbsoluteLayoutTextAlignmentTest {
    @DesignComponent(node = "#TextAlignment") fun MainFrame()
}

// Blend mode test
@DesignDoc(id = "ZqX5i5g6inv9tANIwMMXUV")
private interface BlendModeStageTest {
    @DesignComponent(node = "#stage") fun MainFrame()
}

// "Custom Brush Test"
@DesignDoc(id = "oetCBVw8gCAxmCNllXx7zO")
private interface CustomBrushMainFrameTest {
    @DesignComponent(node = "#MainFrame") fun MainFrame()
}

// "Compositing Test"
@DesignDoc(id = "9g0jn7KXNloRmOZIGze2Rr")
private interface CompositingTest {
    @DesignComponent(node = "#MainFrame") fun MainFrame()
}

// "Constraints Test"
@DesignDoc(id = "KuHLbsKA23DjZPhhgHqt71")
private interface ConstraintsHorizontalTest {
    @DesignComponent(node = "#Horizontal") fun MainFrame()
}

// "Constraints Test"
@DesignDoc(id = "KuHLbsKA23DjZPhhgHqt71")
private interface ConstraintsVerticalTest {
    @DesignComponent(node = "#Vertical") fun MainFrame()
}

// "Cross Axis Fill"
@DesignDoc(id = "GPr1cx4n3zBPwLhqlSL1ba")
private interface CrossAxisFillStageTest {
    @DesignComponent(node = "#stage") fun MainFrame()
}

// "Dials and Gauges Test"
@DesignDoc(id = "lZj6E9GtIQQE4HNLpzgETw")
private interface DialsAndGaugesTest {
    @DesignComponent(node = "#stage") fun MainFrame()
}

// "Fancy Fills Test"
@DesignDoc(id = "xQ9cunHt8VUm6xqJJ2Pjb2")
private interface FancyFillsTest {
    @DesignComponent(node = "#stage") fun MainFrame()
}

// "Fill Container Test"
@DesignDoc(id = "dB3q96FkxkTO4czn5NqnxV")
private interface FillContainerTest {
    @DesignComponent(node = "#stage") fun MainFrame()
}

// "Grid Layout Test"
@DesignDoc(id = "MBNjjSbzzKeN7nBjVoewsl")
private interface GridLayoutStageTest {
    @DesignComponent(node = "#stage") fun MainFrame()
}

// "Widget Grid Test"
@DesignDoc(id = "OBhNItd9i9J2LwVYuLxEIx")
private interface WidgetGridTest {
    @DesignComponent(node = "#Main") fun MainFrame()
}

// "Hello World Test"
@DesignDoc(id = "pxVlixodJqZL95zo2RzTHl")
private interface HelloWorldTest {
    @DesignComponent(node = "#MainFrame") fun MainFrame()
}

// "Image Update Test"
@DesignDoc(id = "oQw7kiy94fvdVouCYBC9T0")
private interface ImageUpdateStageTest {
    @DesignComponent(node = "#Stage") fun MainFrame()
}

// "Image Update Test"
@DesignDoc(id = "oQw7kiy94fvdVouCYBC9T0")
private interface ImageUpdateStage2Test {
    @DesignComponent(node = "#Stage2") fun MainFrame()
}

// "Item Spacing Test"
@DesignDoc(id = "JIjE9oKQbq8ipi66ab5UaK")
private interface ItemSpacingMainTest {
    @DesignComponent(node = "#Main") fun MainFrame()
}

// "Mask Test"
@DesignDoc(id = "JIjE9oKQbq8ipi66ab5UaK")
private interface MaskMainFrameTest {
    @DesignComponent(node = "#MainFrame") fun MainFrame()
}

// "Shadows Test"
@DesignDoc(id = "JIjE9oKQbq8ipi66ab5UaK")
private interface ShadowsRootTest {
    @DesignComponent(node = "#Root") fun MainFrame()
}

// "Shadows Test Strokes"
@DesignDoc(id = "JIjE9oKQbq8ipi66ab5UaK")
private interface ShadowsStrokesTest {
    @DesignComponent(node = "Strokes") fun MainFrame()
}

// "Shadows Test With Opacity"
@DesignDoc(id = "JIjE9oKQbq8ipi66ab5UaK")
private interface ShadowsWithOpacityTest {
    @DesignComponent(node = "With Opacity") fun MainFrame()
}

// "Telltales Test"
@DesignDoc(id = "JIjE9oKQbq8ipi66ab5UaK")
private interface TelltalesTest {
    @DesignComponent(node = "#Main") fun MainFrame()
}

// "Text Elide Test"
@DesignDoc(id = "JIjE9oKQbq8ipi66ab5UaK")
private interface TextElideStageTest {
    @DesignComponent(node = "#stage") fun MainFrame()
}

// "Text Resizing Test"
@DesignDoc(id = "JIjE9oKQbq8ipi66ab5UaK")
private interface TextResizingMainFrameTest {
    @DesignComponent(node = "#MainFrame") fun MainFrame()
}

// "Variable Border Test"
@DesignDoc(id = "MWnVAfW3FupV4VMLNR1m67")
private interface VariableBorderMainFrameTest {
    @DesignComponent(node = "#MainFrame") fun MainFrame()
}

// "Vector Rendering Test"
@DesignDoc(id = "Z3ucY0wMAbIwZIa6mLEWIK")
private interface VectorRenderingStageTest {
    @DesignComponent(node = "#stage") fun MainFrame()
}

// To run these tests in a local development env, run `./gradlew val:fAUFF ; ./gradlew
// recordRoborazziDebug`.file
// Test outputs should be a series of images at:
// `integration-tests/validation/src/testDebug/roborazzi/RenderStaticExamples/*.png`
val STATIC_EXAMPLES: ArrayList<Triple<String, @Composable () -> Unit, String?>> =
    arrayListOf(
        Triple(
            "Layout Test Stage Node",
            { LayoutStageDoc.MainFrame() },
            LayoutStageDoc.javaClass.name
        ),
        Triple(
            "Layout Test Stuff Node",
            { LayoutStageStuffDoc.MainFrame() },
            LayoutStageStuffDoc.javaClass.name
        ),
        Triple(
            "Layout Test Children Node",
            { LayoutStageChildrenDoc.MainFrame() },
            LayoutStageChildrenDoc.javaClass.name
        ),
        Triple(
            "Layout Test Rotations Node",
            { LayoutStageRotationsDoc.MainFrame() },
            LayoutStageRotationsDoc.javaClass.name
        ),
        Triple(
            "Layout Test Constraints Node",
            { LayoutStageConstraintsDoc.MainFrame() },
            LayoutStageConstraintsDoc.javaClass.name
        ),
        Triple(
            "Layout Test Line Node",
            { LayoutStageLineDoc.MainFrame() },
            LayoutStageLineDoc.javaClass.name
        ),
        Triple(
            "Absolute Layout Alignment Test Node",
            { AbsoluteLayoutAlignmentTestDoc.MainFrame() },
            AbsoluteLayoutAlignmentTestDoc.javaClass.name
        ),
        // Triple("Absolute Layout Alignment AutoLayout Node", {
        // AbsoluteLayoutAutoLayoutTestDoc.MainFrame() },
        // AbsoluteLayoutAutoLayoutTestDoc.javaClass.name),
        // Triple("Absolute Layout Alignment VectorAlignment Node", {
        // AbsoluteLayoutVectorAlignmentTestDoc.MainFrame() },
        // AbsoluteLayoutVectorAlignmentTestDoc.javaClass.name),
        // Triple("Absolute Layout Alignment TextAlignment Node", {
        // AbsoluteLayoutTextAlignmentTestDoc.MainFrame() },
        // AbsoluteLayoutTextAlignmentTestDoc.javaClass.name),

        Triple(
            "Blendmode Test Stage Node",
            { BlendModeStageTestDoc.MainFrame() },
            BlendModeStageTestDoc.javaClass.name
        ),
        Triple(
            "Custom Brush Test",
            { CustomBrushMainFrameTestDoc.MainFrame() },
            CustomBrushMainFrameTestDoc.javaClass.name
        ),
        Triple(
            "Compositing Test",
            { CompositingTestDoc.MainFrame() },
            CompositingTestDoc.javaClass.name
        ),
        Triple(
            "Horizontal Constraints Test",
            { ConstraintsHorizontalTestDoc.MainFrame() },
            ConstraintsHorizontalTestDoc.javaClass.name
        ),

        // Triple("Vertical Constraints Test", { ConstraintsVerticalTestDoc.MainFrame() },
        // ConstraintsVerticalTestDoc.javaClass.name),
        Triple(
            "Cross Axis Fill",
            { CrossAxisFillStageTestDoc.MainFrame() },
            CrossAxisFillStageTestDoc.javaClass.name
        ),
        Triple(
            "Dials and Gauges Test",
            { DialsAndGaugesTestDoc.MainFrame() },
            DialsAndGaugesTestDoc.javaClass.name
        ),
        Triple(
            "Fancy Fills Test",
            { FancyFillsTestDoc.MainFrame() },
            FancyFillsTestDoc.javaClass.name
        ),
        Triple(
            "Fill Container Test",
            { FillContainerTestDoc.MainFrame() },
            FillContainerTestDoc.javaClass.name
        ),
        Triple(
            "Grid Layout Test",
            { BlendModeStageTestDoc.MainFrame() },
            BlendModeStageTestDoc.javaClass.name
        ),
        Triple(
            "Widget Grid Test",
            { WidgetGridTestDoc.MainFrame() },
            WidgetGridTestDoc.javaClass.name
        ),
        Triple(
            "Hello World Test",
            { HelloWorldTestDoc.MainFrame() },
            HelloWorldTestDoc.javaClass.name
        ),
        // Triple("Image Update Test", { ImageUpdateStageTestDoc.MainFrame() },
        // ImageUpdateStageTestDoc.javaClass.name),
        // Triple("Image Update 2 Test", { ImageUpdateStage2TestDoc.MainFrame() },
        // ImageUpdateStage2TestDoc.javaClass.name),
        // Triple("Item Spacing MainFrame Test", { ItemSpacingMainTestDoc.MainFrame() },
        // ItemSpacingMainTestDoc.javaClass.name),
        // Triple("Mask MainFrame Test", { MaskMainFrameTestDoc.MainFrame() },
        // MaskMainFrameTestDoc.javaClass.name),
        // Triple("Shadows Root Test", { ShadowsRootTestDoc.MainFrame() },
        // ShadowsRootTestDoc.javaClass.name),
        // Triple("Shadows Strokes Test", { ShadowsStrokesTestDoc.MainFrame() },
        // ShadowsStrokesTestDoc.javaClass.name),
        // Triple("Shadows Opacity Test", { ShadowsWithOpacityTestDoc.MainFrame() },
        // ShadowsWithOpacityTestDoc.javaClass.name),
        // Triple("Telltales MainFrame Test", { TelltalesTestDoc.MainFrame() },
        // TelltalesTestDoc.javaClass.name),
        // Triple("Text Elide Stage Test", { TextElideStageTestDoc.MainFrame() },
        // TextElideStageTestDoc.javaClass.name),
        // Triple("Text Resizing MainFrame Test", { TextResizingMainFrameTestDoc.MainFrame() },
        // TextResizingMainFrameTestDoc.javaClass.name),
        Triple(
            "Variable Border MainFrame Test",
            { VariableBorderMainFrameTestDoc.MainFrame() },
            VariableBorderMainFrameTestDoc.javaClass.name
        ),
        Triple(
            "Vector Rendering MainFrame Test",
            { VectorRenderingStageTestDoc.MainFrame() },
            VectorRenderingStageTestDoc.javaClass.name
        ),
    )

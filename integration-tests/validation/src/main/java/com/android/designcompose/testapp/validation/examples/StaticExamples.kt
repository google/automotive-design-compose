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

 import com.android.designcompose.DesignDocOverride
 import com.android.designcompose.annotation.Design
 import com.android.designcompose.annotation.DesignComponent
 import com.android.designcompose.annotation.DesignDoc
 
 // Layout "stage"
@DesignDoc(id = "Gv63fYTzpeH2ZtxP4go31E")
private interface LayoutStage {
    @DesignComponent(node = "#stage") fun MainFrame()
}
@Composable
private fun LayoutStage() {
    LayoutStageDoc.MainFrame()
}

// Layout "stuff"
@DesignDoc(id = "Gv63fYTzpeH2ZtxP4go31E")
private interface LayoutStageStuff {
    @DesignComponent(node = "#stage-stuff") fun MainFrame()
}
@Composable
private fun LayoutStageStuff() {
    LayoutStageStuffDoc.MainFrame()
}

// Blend mode test
@DesignDoc(id = "ZqX5i5g6inv9tANIwMMXUV")
private interface BlendModeStage {
    @DesignComponent(node = "#stage") fun MainFrame()
}
@Composable
private fun BlendModeStage() {
    BlendModeStageDoc.MainFrame()
}

// Absolute Layout Alignment Test node
@DesignDoc(id = "JIjE9oKQbq8ipi66ab5UaK")
private interface AbsoluteLayoutAlignmentTest {
    @DesignComponent(node = "#Test") fun MainFrame()
}
@Composable
private fun AbsoluteLayoutAlignmentTest() {
    AbsoluteLayoutAlignmentTestDoc.MainFrame()
}
 

 // To run these tests in a local development env, run `./gradlew recordRoborazziDebug`.file
 // Test outputs should be a series of images at:
 // `integration-tests/validation/src/testDebug/roborazzi/RenderStaticExamples/*.png`
 val STATIC_EXAMPLES: ArrayList<Triple<String, @Composable () -> Unit, String?>> =
     arrayListOf(
         // TODO: There's a problem with code generation so that I have to keep these tets in place or
         // nodes can't be found for subsequent tests.
         Triple("Layout Tests", { LayoutTests() }, LayoutTestsDoc.javaClass.name),
         Triple("Alignment", { AlignmentTest() }, AlignmentTestDoc.javaClass.name),
         Triple("Blend Modes", { BlendModeTest() }, BlendModeTestDoc.javaClass.name),

         Triple("Layout Test Stage Node", { LayoutStage() }, LayoutStageDoc.javaClass.name),
         Triple("Layout Test Stuff Node", { LayoutStageStuff() }, LayoutStageStuffDoc.javaClass.name),
        //  Triple("Layout Test Child Node", { StaticDocument("Gv63fYTzpeH2ZtxP4go31E", "#stage-children") }, StaticDocumentDoc.javaClass.name),
        //  Triple("Layout Test Rotations Node", { StaticDocument("Gv63fYTzpeH2ZtxP4go31E", "#stage-rotations") }, StaticDocumentDoc.javaClass.name),
        //  Triple("Layout Test Constraints Node", { StaticDocument("Gv63fYTzpeH2ZtxP4go31E", "#stage-constraintes") }, StaticDocumentDoc.javaClass.name),
        //  Triple("Layout Test Line Node", { StaticDocument("Gv63fYTzpeH2ZtxP4go31E", "#stageline") }, StaticDocumentDoc.javaClass.name),
         Triple("Blendmode Test Stage Node", { BlendModeStage() }, BlendModeStageDoc.javaClass.name),
         Triple("Absolute Layout Alignmen Test Node", { AbsoluteLayoutAlignmentTest() }, AbsoluteLayoutAlignmentTestDoc.javaClass.name),
     )
 

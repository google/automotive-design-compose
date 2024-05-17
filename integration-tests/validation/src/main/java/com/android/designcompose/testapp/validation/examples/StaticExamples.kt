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

// Blend mode test
@DesignDoc(id = "ZqX5i5g6inv9tANIwMMXUV")
private interface BlendModeStage {
    @DesignComponent(node = "#stage") fun MainFrame()
}

// Absolute Layout Alignment Test node
@DesignDoc(id = "JIjE9oKQbq8ipi66ab5UaK")
private interface AbsoluteLayoutAlignmentTest {
    @DesignComponent(node = "#Test") fun MainFrame()
}

 // To run these tests in a local development env, run `./gradlew val:fAUFF ; ./gradlew recordRoborazziDebug`.file
 // Test outputs should be a series of images at:
 // `integration-tests/validation/src/testDebug/roborazzi/RenderStaticExamples/*.png`
 val STATIC_EXAMPLES: ArrayList<Triple<String, @Composable () -> Unit, String?>> =
     arrayListOf(

         Triple("Layout Test Stage Node", { LayoutStageDoc.MainFrame() }, LayoutStageDoc.javaClass.name),
         Triple("Layout Test Stuff Node", { LayoutStageStuffDoc.MainFrame() }, LayoutStageStuffDoc.javaClass.name),
         Triple("Layout Test Children Node", { LayoutStageChildrenDoc.MainFrame() }, LayoutStageChildrenDoc.javaClass.name),
         Triple("Layout Test Rotations Node", { LayoutStageRotationsDoc.MainFrame() }, LayoutStageRotationsDoc.javaClass.name),
         Triple("Layout Test Constraints Node", { LayoutStageConstraintsDoc.MainFrame() }, LayoutStageConstraintsDoc.javaClass.name),
         Triple("Layout Test Line Node", { LayoutStageLineDoc.MainFrame() }, LayoutStageLineDoc.javaClass.name),
         Triple("Blendmode Test Stage Node", { BlendModeStageDoc.MainFrame() }, BlendModeStageDoc.javaClass.name),
         Triple("Absolute Layout Alignmen Test Node", { AbsoluteLayoutAlignmentTestDoc.MainFrame() }, AbsoluteLayoutAlignmentTestDoc.javaClass.name),
     )
 

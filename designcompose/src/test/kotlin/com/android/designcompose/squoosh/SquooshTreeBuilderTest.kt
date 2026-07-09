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

package com.android.designcompose.squoosh

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.android.designcompose.*
import com.android.designcompose.common.DesignDocId
import com.android.designcompose.common.GenericDocContent
import com.android.designcompose.common.VariantPropertyMap
import com.android.designcompose.definition.DesignComposeDefinition
import com.android.designcompose.definition.DesignComposeDefinitionHeader
import com.android.designcompose.definition.plugin.MeterData
import com.android.designcompose.definition.plugin.ProgressBarMeterData
import com.android.designcompose.definition.view.NodeStyle
import com.android.designcompose.definition.view.View
import com.android.designcompose.definition.view.ViewDataKt.container
import com.android.designcompose.definition.view.ViewStyle
import com.android.designcompose.definition.view.componentInfo
import com.android.designcompose.definition.view.nodeStyle
import com.android.designcompose.definition.view.view
import com.android.designcompose.definition.view.viewData
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SquooshTreeBuilderTest {

    private fun createDocContent(
        rootView: View,
        variantViewMap: HashMap<String, HashMap<String, View>> = HashMap(),
        nodeIdMap: HashMap<String, View> = HashMap(),
        variantPropertyMap: VariantPropertyMap = VariantPropertyMap(),
    ): DocContent {
        val docId = DesignDocId("dummy")
        val header = DesignComposeDefinitionHeader.getDefaultInstance()
        val document = DesignComposeDefinition.getDefaultInstance()
        val imageSession = ByteString.EMPTY

        val genericDocContent =
            GenericDocContent(
                docId,
                header,
                document,
                variantViewMap,
                variantPropertyMap,
                nodeIdMap,
                imageSession,
            )
        return DocContent(genericDocContent, null)
    }

    @Test
    fun testComponentReplacementPreservedOnVariantChange() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = Density(1f)
        val fontResolver = createFontFamilyResolver(context)

        // 1. Create a child slot component "DefaultIcon"
        val iconSlotComponentSetName = "DefaultIcon"
        val iconSlotInstanceName = "#icon-slot"

        // Child component views for DefaultIcon
        val iconLightView = view {
            uniqueId = 3
            id = "icon-light-id"
            name = "theme=Light"
            componentInfo = componentInfo {
                id = "icon-light-id"
                name = "theme=Light"
                componentSetName = iconSlotComponentSetName
            }
        }
        val iconDarkView = view {
            uniqueId = 4
            id = "icon-dark-id"
            name = "theme=Dark"
            componentInfo = componentInfo {
                id = "icon-dark-id"
                name = "theme=Dark"
                componentSetName = iconSlotComponentSetName
            }
        }

        // 2. Create parent component "MyParent" with variants Light and Dark
        val parentComponentSetName = "MyParent"
        val parentLightView = view {
            uniqueId = 1
            id = "parent-light-id"
            name = "theme=Light"
            componentInfo = componentInfo {
                id = "parent-light-id"
                name = "theme=Light"
                componentSetName = parentComponentSetName
            }
            data = viewData {
                container = container {
                    // It has child icon-slot-light
                    children.add(
                        view {
                            uniqueId = 5
                            id = "icon-slot-light-id"
                            name = iconSlotInstanceName
                            componentInfo = componentInfo {
                                id = "icon-slot-light-id"
                                name = "theme=Light"
                                componentSetName = iconSlotComponentSetName
                            }
                        }
                    )
                }
            }
        }

        val parentDarkView = view {
            uniqueId = 2
            id = "parent-dark-id"
            name = "theme=Dark"
            componentInfo = componentInfo {
                id = "parent-dark-id"
                name = "theme=Dark"
                componentSetName = parentComponentSetName
            }
            data = viewData {
                container = container {
                    // It has child icon-slot-dark
                    children.add(
                        view {
                            uniqueId = 6
                            id = "icon-slot-dark-id"
                            name = iconSlotInstanceName
                            componentInfo = componentInfo {
                                id = "icon-slot-dark-id"
                                name = "theme=Light" // Default variant
                                componentSetName = iconSlotComponentSetName
                            }
                        }
                    )
                }
            }
        }

        // Populate doc maps
        val variantViewMap = HashMap<String, HashMap<String, View>>()
        val parentMap = HashMap<String, View>()
        parentMap["theme=Light"] = parentLightView
        parentMap["theme=Dark"] = parentDarkView
        variantViewMap[parentComponentSetName] = parentMap

        val iconMap = HashMap<String, View>()
        iconMap["theme=Light"] = iconLightView
        iconMap["theme=Dark"] = iconDarkView
        variantViewMap[iconSlotComponentSetName] = iconMap

        val variantPropertyMap = VariantPropertyMap()
        variantPropertyMap.addProperty(parentComponentSetName, "theme", "Light")
        variantPropertyMap.addProperty(parentComponentSetName, "theme", "Dark")
        variantPropertyMap.addProperty(iconSlotComponentSetName, "theme", "Light")
        variantPropertyMap.addProperty(iconSlotComponentSetName, "theme", "Dark")

        val docContent =
            createDocContent(
                rootView = parentLightView,
                variantViewMap = variantViewMap,
                variantPropertyMap = variantPropertyMap,
            )

        // 3. Setup Customizations
        val customizations = CustomizationContext()
        customizations.setVariantProperties(hashMapOf("theme" to "Light"))

        var replacementInvoked = false
        val replacementComponent: @Composable (ComponentReplacementContext) -> Unit =
            @Composable { _ -> replacementInvoked = true }
        customizations.setComponent(iconSlotInstanceName, replacementComponent)

        val variantTransition = SquooshVariantTransition()
        val interactionState = InteractionState()
        val keyTracker = KeyEventTracker()
        val layoutIdAllocator = SquooshLayoutIdAllocator()

        // ==========================================
        // Phase 1: Render Light Variant (BasePhase)
        // ==========================================
        variantTransition.treeBuildPhase = TreeBuildPhase.BasePhase
        val composableList1 = ComposableList()
        val resolvedNode1 =
            resolveVariantsRecursively(
                viewFromTree = parentLightView,
                document = docContent,
                customizations = customizations,
                variantTransition = variantTransition,
                interactionState = interactionState,
                keyTracker = keyTracker,
                parentComponents = null,
                density = density,
                fontResolver = fontResolver,
                composableList = composableList1,
                layoutIdAllocator = layoutIdAllocator,
                isRoot = true,
                variableState = VariableState(),
                appContext = context,
                customVariantTransition = null,
                textMeasureCache = TextMeasureCache(),
                textHash = HashSet(),
            )

        assertThat(resolvedNode1).isNotNull()
        val child1 =
            composableList1.childComposables.find { it.node.unresolvedName == iconSlotInstanceName }
        assertThat(child1).isNotNull()
        assertThat(child1!!.component).isEqualTo(replacementComponent)

        // Cycle layout IDs
        layoutIdAllocator.removalNodes()

        // ==========================================
        // Phase 2: Start transition to Dark variant
        // ==========================================
        customizations.setVariantProperties(hashMapOf("theme" to "Dark"))

        // Run BasePhase pass of Phase 2 (this registers the transition)
        variantTransition.treeBuildPhase = TreeBuildPhase.BasePhase
        val composableList2Base = ComposableList()
        resolveVariantsRecursively(
            viewFromTree = parentLightView,
            document = docContent,
            customizations = customizations,
            variantTransition = variantTransition,
            interactionState = interactionState,
            keyTracker = keyTracker,
            parentComponents = null,
            density = density,
            fontResolver = fontResolver,
            composableList = composableList2Base,
            layoutIdAllocator = layoutIdAllocator,
            isRoot = true,
            variableState = VariableState(),
            appContext = context,
            customVariantTransition = null,
            textMeasureCache = TextMeasureCache(),
            textHash = HashSet(),
        )

        // Cycle layout IDs
        layoutIdAllocator.removalNodes()

        // Run TransitionTargetPhase of Phase 2
        variantTransition.treeBuildPhase = TreeBuildPhase.TransitionTargetPhase
        val composableList2Target = ComposableList()
        resolveVariantsRecursively(
            viewFromTree = parentLightView,
            document = docContent,
            customizations = customizations,
            variantTransition = variantTransition,
            interactionState = interactionState,
            keyTracker = keyTracker,
            parentComponents = null,
            density = density,
            fontResolver = fontResolver,
            composableList = composableList2Target,
            layoutIdAllocator = layoutIdAllocator,
            isRoot = true,
            variableState = VariableState(),
            appContext = context,
            customVariantTransition = null,
            textMeasureCache = TextMeasureCache(),
            textHash = HashSet(),
        )

        // Complete the transition
        variantTransition.afterRenderPhases()
        // Cycle layout IDs
        layoutIdAllocator.removalNodes()

        // ==========================================
        // Phase 3: Final Stable Dark variant (BasePhase)
        // ==========================================
        variantTransition.treeBuildPhase = TreeBuildPhase.BasePhase
        val composableList3 = ComposableList()
        val resolvedNode3 =
            resolveVariantsRecursively(
                viewFromTree = parentLightView,
                document = docContent,
                customizations = customizations,
                variantTransition = variantTransition,
                interactionState = interactionState,
                keyTracker = keyTracker,
                parentComponents = null,
                density = density,
                fontResolver = fontResolver,
                composableList = composableList3,
                layoutIdAllocator = layoutIdAllocator,
                isRoot = true,
                variableState = VariableState(),
                appContext = context,
                customVariantTransition = null,
                textMeasureCache = TextMeasureCache(),
                textHash = HashSet(),
            )

        assertThat(resolvedNode3).isNotNull()
        // Verify child replacement component is STILL added to composable list in Dark variant
        val child3 =
            composableList3.childComposables.find { it.node.unresolvedName == iconSlotInstanceName }
        assertThat(child3).isNotNull()
        assertThat(child3!!.component).isEqualTo(replacementComponent)
    }

    @Test
    fun testProgressBarDataTransferToIndicator() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = Density(1f)
        val fontResolver = createFontFamilyResolver(context)

        // 1. Create progress bar parent view style with progress bar meter data
        val pbMeterData = ProgressBarMeterData.newBuilder().setEnabled(true).build()
        val meterData = MeterData.newBuilder().setProgressBarData(pbMeterData).build()
        val parentNodeStyle = NodeStyle.newBuilder().setMeterData(meterData).build()
        val parentStyle = ViewStyle.newBuilder().setNodeStyle(parentNodeStyle).build()

        // 2. Create indicator child view
        val childView = view {
            uniqueId = 2
            id = "indicator-id"
            name = "#indicator"
        }

        // 3. Create parent view "#progress" with the indicator child
        val parentView = view {
            uniqueId = 1
            id = "progress-id"
            name = "#progress"
            style = parentStyle
            data = viewData { container = container { children.add(childView) } }
        }

        val docContent = createDocContent(rootView = parentView)
        val customizations = CustomizationContext()
        val variantTransition = SquooshVariantTransition()
        val interactionState = InteractionState()
        val keyTracker = KeyEventTracker()
        val layoutIdAllocator = SquooshLayoutIdAllocator()

        val resolvedNode =
            resolveVariantsRecursively(
                viewFromTree = parentView,
                document = docContent,
                customizations = customizations,
                variantTransition = variantTransition,
                interactionState = interactionState,
                keyTracker = keyTracker,
                parentComponents = null,
                density = density,
                fontResolver = fontResolver,
                composableList = null,
                layoutIdAllocator = layoutIdAllocator,
                isRoot = true,
                variableState = VariableState(),
                appContext = context,
                customVariantTransition = null,
                textMeasureCache = TextMeasureCache(),
                textHash = HashSet(),
            )

        assertThat(resolvedNode).isNotNull()
        // Parent "#progress" should NOT have progress bar data anymore
        assertThat(resolvedNode!!.style.nodeStyle.hasMeterData()).isFalse()

        // Child "#indicator" should have progress bar data transferred
        val indicatorNode = resolvedNode.firstChild
        assertThat(indicatorNode).isNotNull()
        assertThat(indicatorNode!!.unresolvedName).isEqualTo("#indicator")
        assertThat(indicatorNode.style.nodeStyle.hasMeterData()).isTrue()
        assertThat(indicatorNode.style.nodeStyle.meterData.hasProgressBarData()).isTrue()

        // Child customizationName should be set to parent's unresolved name "#progress"
        assertThat(indicatorNode.customizationName).isEqualTo("#progress")
    }
}

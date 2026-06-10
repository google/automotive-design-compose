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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.designcompose.CustomizationContext
import com.android.designcompose.DocContent
import com.android.designcompose.DocServer
import com.android.designcompose.setModifier
import com.android.designcompose.TestUtils.ClearStateTestRule
import com.android.designcompose.common.DesignDocId
import com.android.designcompose.common.GenericDocContent
import com.android.designcompose.common.NodeQuery
import com.android.designcompose.common.VariantPropertyMap
import com.android.designcompose.definition.DesignComposeDefinition
import com.android.designcompose.definition.DesignComposeDefinitionHeader
import com.android.designcompose.definition.view.View
import com.android.designcompose.definition.view.ViewDataKt.container
import com.android.designcompose.definition.view.view
import com.android.designcompose.definition.view.viewData
import com.android.designcompose.definition.element.DimensionProto
import com.android.designcompose.definition.element.DimensionRect
import com.android.designcompose.definition.element.Size
import com.android.designcompose.definition.layout.AlignContent
import com.android.designcompose.definition.layout.AlignItems
import com.android.designcompose.definition.layout.AlignSelf
import com.android.designcompose.definition.layout.FlexDirection
import com.android.designcompose.definition.layout.JustifyContent
import com.android.designcompose.definition.layout.PositionType
import com.android.designcompose.definition.layout.LayoutStyle
import com.android.designcompose.definition.layout.ItemSpacing
import com.android.designcompose.definition.view.ViewStyle
import com.google.protobuf.ByteString
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SquooshRenderTest {
    @get:Rule val clearStateTestRule = ClearStateTestRule()
    @get:Rule val composeTestRule = createComposeRule()

    private val docId = DesignDocId("mockdoc")
    private val CustomTestKey = SemanticsPropertyKey<String>("CustomTestKey")

    private fun createValidViewStyle(): ViewStyle {
        val undefinedDimension = DimensionProto.getDefaultInstance()
        val defaultRect = DimensionRect.newBuilder()
            .setStart(undefinedDimension)
            .setEnd(undefinedDimension)
            .setTop(undefinedDimension)
            .setBottom(undefinedDimension)
            .build()
        val defaultItemSpacing = ItemSpacing.newBuilder().setFixed(0).build()
        val defaultSize = Size.newBuilder().setWidth(0f).setHeight(0f).build()

        val layoutStyle = LayoutStyle.newBuilder()
            .setMargin(defaultRect)
            .setPadding(defaultRect)
            .setItemSpacing(defaultItemSpacing)
            .setTop(undefinedDimension)
            .setLeft(undefinedDimension)
            .setBottom(undefinedDimension)
            .setRight(undefinedDimension)
            .setWidth(undefinedDimension)
            .setHeight(undefinedDimension)
            .setMinWidth(undefinedDimension)
            .setMaxWidth(undefinedDimension)
            .setMinHeight(undefinedDimension)
            .setMaxHeight(undefinedDimension)
            .setFlexBasis(undefinedDimension)
            .setBoundingBox(defaultSize)
            .setAlignContent(AlignContent.ALIGN_CONTENT_FLEX_START)
            .setAlignItems(AlignItems.ALIGN_ITEMS_FLEX_START)
            .setAlignSelf(AlignSelf.ALIGN_SELF_AUTO)
            .setFlexDirection(FlexDirection.FLEX_DIRECTION_ROW)
            .setJustifyContent(JustifyContent.JUSTIFY_CONTENT_FLEX_START)
            .setPositionType(PositionType.POSITION_TYPE_RELATIVE)
            .build()
        return ViewStyle.newBuilder()
            .setLayoutStyle(layoutStyle)
            .build()
    }

    private fun createDocContent(
        rootView: View,
        nodeIdMap: HashMap<String, View> = HashMap(),
    ): DocContent {
        val header = DesignComposeDefinitionHeader.getDefaultInstance()
        val document = DesignComposeDefinition.getDefaultInstance()
        val imageSession = ByteString.EMPTY
        val genericDocContent =
            GenericDocContent(
                docId,
                header,
                document,
                HashMap(),
                VariantPropertyMap(),
                nodeIdMap,
                imageSession,
            )
        return DocContent(genericDocContent, null)
    }

    @Before
    fun setUp() {
        DocServer.testOnlyClearDocuments()
    }

    @Test
    fun testChildModifierAppliedInRendering() {
        val defaultStyle = createValidViewStyle()
        // 1. Create a parent component and a child node
        val parentView = view {
            uniqueId = 1
            id = "parent-id"
            name = "Parent"
            style = defaultStyle
            data = viewData {
                container = container {
                    children.add(
                        view {
                            uniqueId = 2
                            id = "child-id"
                            name = "#child-node"
                            style = defaultStyle
                        }
                    )
                }
            }
        }

        // Map child ID to its view so SquooshRoot can find it when rendering subtree
        val nodeIdMap = HashMap<String, View>()
        nodeIdMap["parent-id"] = parentView
        nodeIdMap["child-id"] = parentView.data.container.childrenList[0]

        val docContent = createDocContent(rootView = parentView, nodeIdMap = nodeIdMap)

        // Register the document in DocServer
        DocServer.documents[docId] = docContent

        // 2. Setup Customizations with a modifier on the child
        val customizations = CustomizationContext()
        customizations.setModifier("#child-node", Modifier.semantics { set(CustomTestKey, "custom-value") })

        // 3. Render SquooshRoot
        composeTestRule.setContent {
            SquooshRoot(
                docName = "mockdoc",
                incomingDocId = docId,
                rootNodeQuery = NodeQuery.NodeId("parent-id"),
                customizationContext = customizations,
            )
        }

        // 4. Verify that the modifier with testTag is applied and can be found in the compose tree
        composeTestRule.onNode(SemanticsMatcher.expectValue(CustomTestKey, "custom-value")).assertExists()
    }
}

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

package com.android.designcompose.annotation

/**
 * Generate an interface that contains functions to render various nodes in a Figma document
 *
 * @param id the id of the Figma document. This can be found in the url, e.g. figma.com/file/<id>
 * @param version a version string that gets written to a generated JSON file used for the Design
 * Compose Figma plugin
 */
@Target(AnnotationTarget.CLASS)
annotation class DesignDoc(val id: String, val version: String = "0")

/**
 * Generate a @Composable function that renders the given node
 *
 * @param node the name of the Figma node
 * @param override set to true if this function overrides a function. Defaulted to false
 * @param hideDesignSwitcher set to true if this is a root node and you do not want to show the
 * design switcher. Defaulted to false
 * @param isRoot set to true if this is the root node. All customizations should be set in a root
 * node to be passed down to child nodes. Defaulted to false. This is used in the generated JSON
 * file used for the Design Compose Figma plugin
 */
@Target(AnnotationTarget.FUNCTION)
annotation class DesignComponent(
    val node: String,
    val override: Boolean = false,
    val hideDesignSwitcher: Boolean = false,
    val isRoot: Boolean = false
)

/**
 * Specify a node customization parameter within a @DesignComponent function.
 * @param node the name of the Figma node
 */
@Target(AnnotationTarget.VALUE_PARAMETER) annotation class Design(val node: String)

/**
 * Specify a variant name for a component set that contains variant children.
 * @param property the name of the variant property
 */
@Target(AnnotationTarget.VALUE_PARAMETER) annotation class DesignVariant(val property: String)

/**
 * An optional annotation that goes with a @Design annotation of type @Composable() -> Unit, which
 * is used to replace the children of this frame with new data. Adding the @DesignContentTypes
 * annotation tells Design Compose what nodes can be used as children. This data is used in the
 * generated json file which is input for the DesignCompose Figma plugin.
 * @param nodes A comma delimited string of node names that can go into the associated content
 * replacement annotation
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class DesignContentTypes(val nodes: Array<String>)

annotation class PreviewNode(
    val count: Int,
    val node: String,
)

/**
 * An optional annotation that goes with a @Design annotation of type @Composable() -> Unit, which
 * is used to provide sample content for the List Preview Widget. This data is used in the generated
 * json file which is input for the List Preview Widget.
 * @param nodes A comma delimited string of node names that will be used as sample content
 */
@Repeatable
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class DesignPreviewContent(val name: String, val nodes: Array<PreviewNode>)

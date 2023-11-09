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

package com.android.designcompose

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.android.designcompose.common.nodeNameToPropertyValueList
import com.android.designcompose.serdegen.Background
import com.android.designcompose.serdegen.ComponentInfo
import com.android.designcompose.serdegen.Dimension
import com.android.designcompose.serdegen.NodeQuery
import java.util.Optional

// Node data associated with a Figma node that may be a variant. This is used for grid layouts to
// determine the span of an item in the grid layout.
data class DesignNodeData(
    var nodeName: String = "",
    var variantProperties: HashMap<String, String> = HashMap(),
)

typealias GetDesignNodeData = () -> DesignNodeData

typealias GridSpanFunc = ((GetDesignNodeData) -> LazyContentSpan)

data class LazyContentSpan(
    val span: Int = 1,
    val maxLineSpan: Boolean = false,
)

data class ListContentData(
    var count: Int = 0,
    var key: ((index: Int) -> Any)? = null,
    var span: ((index: Int) -> LazyContentSpan)? = null,
    var contentType: (index: Int) -> Any? = { null },
    var initialSpan: (() -> LazyContentSpan)? = null,
    var initialContent: @Composable (parentLayoutInfo: ParentLayoutInfo) -> Unit = {},
    var itemContent: @Composable (index: Int, parentLayoutInfo: ParentLayoutInfo) -> Unit
)

typealias ListContent = (GridSpanFunc) -> ListContentData

fun EmptyListContent(): ListContent {
    return { ListContentData { _, _ -> } }
}

data class ContentReplacementContext(
    val parentLayoutId: Int,
    val rootLayoutId: Int,
)

data class ReplacementContent(
    var count: Int = 0,
    var content: ((index: Int) -> @Composable (ContentReplacementContext) -> Unit),
)

typealias TapCallback = () -> Unit

typealias MeterFunction = @Composable () -> Meter

typealias Meter = Float

// A Customization changes the way a node is presented, or changes the content of a node.
data class Customization(
    // Text content customization
    var text: Optional<String> = Optional.empty(),
    // Text function customization
    var textFunction: Optional<@Composable () -> String> = Optional.empty(),
    // Image fill customization
    var image: Optional<Bitmap> = Optional.empty(),
    // Image fill with context customization
    var imageWithContext: Optional<@Composable (ImageReplacementContext) -> Bitmap?> =
        Optional.empty(),
    // Modifier customization
    var modifier: Optional<Modifier> = Optional.empty(),
    // Tap callback customization
    var tapCallback: Optional<TapCallback> = Optional.empty(),
    // Child content customization V2
    var content: Optional<ReplacementContent> = Optional.empty(),
    var listContent: Optional<ListContent> = Optional.empty(),
    // Node substitution customization
    var component: Optional<@Composable (ComponentReplacementContext) -> Unit> = Optional.empty(),
    // Visibility customization
    var visible: Boolean = true,
    // Font customizations
    var textStyle: Optional<TextStyle> = Optional.empty(),
    // Open link callback function
    var openLinkCallback: Optional<OpenLinkCallback> = Optional.empty(),
    // Meter (dial, gauge, progress bar) customization as a percentage 0-100
    var meterValue: Optional<Float> = Optional.empty(),
    // Meter (dial, gauge, progress bar) customization as a function that returns a percentage 0-100
    var meterFunction: Optional<@Composable () -> Float> = Optional.empty()
)

private fun Customization.clone(): Customization {
    val c = Customization()
    c.text = text
    c.image = image
    c.imageWithContext = imageWithContext
    c.modifier = modifier
    c.tapCallback = tapCallback
    c.content = content
    c.listContent = listContent
    c.component = component
    c.visible = visible
    c.textStyle = textStyle
    c.openLinkCallback = openLinkCallback
    c.meterValue = meterValue
    c.meterFunction = meterFunction

    return c
}

// This class tracks all of the customizations; currently any time a customization changes
// we invalidate the whole Composable tree, but in the future we should be able to make
// this class a singleton, query by "$DocId+$NodeName", and have nodes subscribe to their
// customizations -- then we'd only invalidate the correct node(s) when updating a customization.
data class CustomizationContext(
    val cs: HashMap<String, Customization> = HashMap(),
    var variantProperties: HashMap<String, String> = HashMap(),
    var customComposable:
        Optional<
            @Composable
            (Modifier, String, NodeQuery, List<ParentComponentInfo>, TapCallback?) -> Unit
        > =
        Optional.empty(),
    var key: String? = null,
)

private fun CustomizationContext.customize(nodeName: String, evolver: (Customization) -> Unit) {
    val x = cs[nodeName] ?: Customization()
    evolver(x)
    cs[nodeName] = x
}

fun CustomizationContext.setText(nodeName: String, text: String?) {
    customize(nodeName) { c -> c.text = Optional.ofNullable(text) }
}

fun CustomizationContext.setTextFunction(nodeName: String, text: @Composable (() -> String)?) {
    customize(nodeName) { c -> c.textFunction = Optional.ofNullable(text) }
}

fun CustomizationContext.setImage(nodeName: String, image: Bitmap?) {
    customize(nodeName) { c -> c.image = Optional.ofNullable(image) }
}

// ViewStyle elements that we expose in ImageReplacementContext
data class ImageContext(
    val background: List<Background>,
    val minWidth: Dimension,
    val maxWidth: Dimension,
    val width: Dimension,
    val minHeight: Dimension,
    val maxHeight: Dimension,
    val height: Dimension,
) {
    fun getBackgroundColor(): Int? {
        if (background.size == 1 && background[0] is Background.Solid) {
            val color = (background[0] as Background.Solid).value.color
            return ((color[3].toInt() shl 24) and 0xFF000000.toInt()) or
                ((color[0].toInt() shl 16) and 0x00FF0000) or
                ((color[1].toInt() shl 8) and 0x0000FF00) or
                (color[2].toInt() and 0x000000FF)
        }
        return null
    }

    fun getPixelWidth(): Int? {
        if (width is Dimension.Points) return width.value.toInt()
        if (minWidth is Dimension.Points) return minWidth.value.toInt()
        if (maxWidth is Dimension.Points) return maxWidth.value.toInt()
        return null
    }

    fun getPixelHeight(): Int? {
        if (height is Dimension.Points) return height.value.toInt()
        if (minHeight is Dimension.Points) return minHeight.value.toInt()
        if (maxHeight is Dimension.Points) return maxHeight.value.toInt()
        return null
    }
}

// Image replacements with context provide some extra data from the frame that can be used to
// influence the resulting image. For example, we expose the color and size of the frame so
// that the replacement image can be retrieved at the correct size, and optionally tinted by a
// color specified by the designer if the image is a vector image.
interface ImageReplacementContext {
    val imageContext: ImageContext
}

fun CustomizationContext.setImageWithContext(
    nodeName: String,
    imageWithContext: @Composable ((ImageReplacementContext) -> Bitmap?)?
) {
    customize(nodeName) { c -> c.imageWithContext = Optional.ofNullable(imageWithContext) }
}

fun CustomizationContext.setModifier(nodeName: String, modifier: Modifier?) {
    customize(nodeName) { c -> c.modifier = Optional.ofNullable(modifier) }
}

fun CustomizationContext.setTapCallback(nodeName: String, tapCallback: TapCallback) {
    customize(nodeName) { c -> c.tapCallback = Optional.ofNullable(tapCallback) }
}

fun CustomizationContext.setContent(nodeName: String, content: ReplacementContent?) {
    customize(nodeName) { c -> c.content = Optional.ofNullable(content) }
}

fun CustomizationContext.setListContent(nodeName: String, listContent: ListContent?) {
    customize(nodeName) { c -> c.listContent = Optional.ofNullable(listContent) }
}

fun CustomizationContext.setOpenLinkCallback(nodeName: String, callback: OpenLinkCallback) {
    customize(nodeName) { c -> c.openLinkCallback = Optional.ofNullable(callback) }
}

fun CustomizationContext.setKey(key: String?) {
    this.key = key
}

// Component Replacements are provided with information on the component they are replacing
// including the modifiers that would be applied for the layout style, visual style, and
// (if applicable) the text style.
//
// This allows a replacement component to take on the designer specified appearance while
// offering more behavioral changes than are permitted with simple Modifier customizations
// (for example, replacing a styled text node with a complete text field.
interface ComponentReplacementContext {
    // Return the layout modifier that would have been used to present this component.
    // This modifier doesn't contain any appearance information, and should be the first
    // in the modifier chain.
    val layoutModifier: Modifier

    // Return the appearance modifier. This causes the view to be presented as specified
    // by the designer. It also includes any modifier customizations that may have been
    // specified elsewhere in the program.
    val appearanceModifier: Modifier

    // Render the content of this replaced component, if any.
    @Composable fun Content(): Unit

    // Return the text style, if the component being replaced is a text node in the Figma
    // document.
    val textStyle: TextStyle?

    // Data needed to perform layout
    val parentLayout: ParentLayoutInfo?
}

fun CustomizationContext.setComponent(
    nodeName: String,
    component: @Composable ((ComponentReplacementContext) -> Unit)?
) {
    customize(nodeName) { c -> c.component = Optional.ofNullable(component) }
}

fun CustomizationContext.setVisible(nodeName: String, visible: Boolean) {
    customize(nodeName) { c -> c.visible = visible }
}

fun CustomizationContext.setTextStyle(nodeName: String, textStyle: TextStyle) {
    customize(nodeName) { c -> c.textStyle = Optional.ofNullable(textStyle) }
}

fun CustomizationContext.setMeterValue(nodeName: String, value: Float) {
    customize(nodeName) { c -> c.meterValue = Optional.ofNullable(value) }
}

fun CustomizationContext.setMeterFunction(nodeName: String, value: @Composable () -> Float) {
    customize(nodeName) { c -> c.meterFunction = Optional.ofNullable(value) }
}

fun CustomizationContext.setVariantProperties(vp: HashMap<String, String>) {
    variantProperties = vp
}

fun CustomizationContext.setCustomComposable(
    composable:
        @Composable
        (Modifier, String, NodeQuery, List<ParentComponentInfo>, TapCallback?) -> Unit
) {
    customComposable = Optional.ofNullable(composable)
}

fun CustomizationContext.get(nodeName: String): Optional<Customization> {
    return Optional.ofNullable(cs[nodeName])
}

fun CustomizationContext.getImage(nodeName: String): Bitmap? {
    val c = cs[nodeName] ?: return null
    if (c.image.isPresent) return c.image.get()
    return null
}

fun CustomizationContext.getImageWithContext(
    nodeName: String
): @Composable ((ImageReplacementContext) -> Bitmap?)? {
    val c = cs[nodeName] ?: return null
    if (c.imageWithContext.isPresent) return c.imageWithContext.get()
    return null
}

fun CustomizationContext.getText(nodeName: String): String? {
    val c = cs[nodeName] ?: return null
    if (c.text.isPresent) return c.text.get()
    return null
}

fun CustomizationContext.getTextFunction(nodeName: String): @Composable (() -> String)? {
    val c = cs[nodeName] ?: return null
    if (c.textFunction.isPresent) return c.textFunction.get()
    return null
}

fun CustomizationContext.getModifier(nodeName: String): Modifier? {
    val c = cs[nodeName] ?: return null
    if (c.modifier.isPresent) return c.modifier.get()
    return null
}

fun CustomizationContext.getTapCallback(nodeName: String): TapCallback? {
    val c = cs[nodeName] ?: return null
    if (c.tapCallback.isPresent) return c.tapCallback.get()
    return null
}

fun CustomizationContext.getContent(nodeName: String): ReplacementContent? {
    val c = cs[nodeName] ?: return null
    if (c.content.isPresent) return c.content.get()
    return null
}

fun CustomizationContext.getListContent(nodeName: String): ListContent? {
    val c = cs[nodeName] ?: return null
    if (c.listContent.isPresent) return c.listContent.get()
    return null
}

fun CustomizationContext.getComponent(
    nodeName: String
): @Composable ((ComponentReplacementContext) -> Unit)? {
    val c = cs[nodeName] ?: return null
    if (c.component.isPresent) return c.component.get()
    return null
}

fun CustomizationContext.getMatchingVariant(maybeComponentInfo: Optional<ComponentInfo>): String? {
    if (!maybeComponentInfo.isPresent) return null

    val componentInfo = maybeComponentInfo.get()
    val nodeVariants = parseNodeVariants(componentInfo.name)

    // Check to see if any of the variant properties set match the variant properties in this node.
    // If any match, update the values of the variant properties and return a new node name that
    // uses the specified variants
    var variantChanged = false
    nodeVariants.forEach {
        val value = variantProperties[it.key]
        if (value != null && value != it.value) {
            nodeVariants[it.key] = value
            variantChanged = true
        }
    }

    if (variantChanged) {
        var newVariantList: ArrayList<String> = ArrayList()
        val sortedKeys = nodeVariants.keys.sorted()
        sortedKeys.forEach { newVariantList.add(it + "=" + nodeVariants[it]) }
        return newVariantList.joinToString(",")
    }

    return null
}

private fun parseNodeVariants(nodeName: String): HashMap<String, String> {
    // Take a node name in the form of "property1=name1, property2=name2" and return a list
    // of the property-to-name bindings as string pairs
    val ret = HashMap<String, String>()
    val propertyValueList = nodeNameToPropertyValueList(nodeName)
    for (p in propertyValueList) ret[p.first] = p.second
    return ret
}

fun CustomizationContext.getVisible(nodeName: String): Boolean {
    val c = cs[nodeName] ?: return true
    return c.visible
}

fun CustomizationContext.getTextStyle(nodeName: String): TextStyle? {
    val c = cs[nodeName] ?: return null
    if (c.textStyle.isPresent) return c.textStyle.get()
    return null
}

fun CustomizationContext.getOpenLinkCallback(nodeName: String): OpenLinkCallback? {
    val c = cs[nodeName] ?: return null
    if (c.openLinkCallback.isPresent) return c.openLinkCallback.get()
    return null
}

fun CustomizationContext.getMeterValue(nodeName: String): Float? {
    val c = cs[nodeName] ?: return null
    var value = if (c.meterValue.isPresent) c.meterValue.get() else return null
    if (!value.isFinite()) value = 0F
    return value
}

fun CustomizationContext.getMeterFunction(nodeName: String): (@Composable () -> Float)? {
    val c = cs[nodeName] ?: return null
    if (c.meterFunction.isPresent) return c.meterFunction.get()
    return null
}

fun CustomizationContext.getKey(): String? {
    return key
}

fun CustomizationContext.getCustomComposable():
    @Composable ((Modifier, String, NodeQuery, List<ParentComponentInfo>, TapCallback?) -> Unit)? {
    if (customComposable.isPresent) return customComposable.get()
    return null
}

fun CustomizationContext.mergeFrom(other: CustomizationContext) {
    other.cs.forEach {
        // Make a copy of the customization so we don't use the same one, causing unexpected results
        cs[it.key] = it.value.clone()
    }
    other.variantProperties.forEach { variantProperties[it.key] = it.value }
    customComposable = other.customComposable
}

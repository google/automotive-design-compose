package com.android.designcompose.squoosh

import android.graphics.PointF
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawContext
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontLoader
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.CustomizationContext
import com.android.designcompose.DesignSettings
import com.android.designcompose.DocContent
import com.android.designcompose.DocServer
import com.android.designcompose.DocumentSwitcher
import com.android.designcompose.InteractionState
import com.android.designcompose.InteractionStateManager
import com.android.designcompose.Jni
import com.android.designcompose.LayoutManager
import com.android.designcompose.ParentComponentInfo
import com.android.designcompose.ParentLayoutInfo
import com.android.designcompose.ReplacementContent
import com.android.designcompose.TextLayoutData
import com.android.designcompose.TextMeasureData
import com.android.designcompose.asBrush
import com.android.designcompose.asComposeBlendMode
import com.android.designcompose.asComposeTransform
import com.android.designcompose.blurFudgeFactor
import com.android.designcompose.common.DocumentServerParams
import com.android.designcompose.convertColor
import com.android.designcompose.doc
import com.android.designcompose.getComponent
import com.android.designcompose.getContent
import com.android.designcompose.getKey
import com.android.designcompose.getMatchingVariant
import com.android.designcompose.getText
import com.android.designcompose.getTextStyle
import com.android.designcompose.isAutoHeightFillWidth
import com.android.designcompose.isMask
import com.android.designcompose.measureTextBounds
import com.android.designcompose.mergeStyles
import com.android.designcompose.pointsAsDp
import com.android.designcompose.rootNode
import com.android.designcompose.serdegen.Layout
import com.android.designcompose.serdegen.LayoutChangedResponse
import com.android.designcompose.serdegen.LayoutNode
import com.android.designcompose.serdegen.LayoutNodeList
import com.android.designcompose.serdegen.LineHeight
import com.android.designcompose.serdegen.NodeQuery
import com.android.designcompose.serdegen.TextAlignVertical
import com.android.designcompose.serdegen.TextOverflow
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewData
import com.android.designcompose.serdegen.ViewStyle
import com.android.designcompose.squooshNodeVariant
import com.android.designcompose.squooshRootNode
import com.android.designcompose.squooshShapeRender
import com.android.designcompose.stateForDoc
import com.android.designcompose.useLayer
import com.novi.bincode.BincodeDeserializer
import com.novi.bincode.BincodeSerializer
import kotlin.system.measureTimeMillis
import java.util.Optional
import kotlin.math.roundToInt

const val TAG: String = "DC_SQUOOSH"

// Notes on Layout impl:
//  - add some unique id to View, to make it easier to build a layout tree. [DONE]
//  - make a taffy instance creatable, rather than global; don't need to run layout on everything.
//  - make taffy take the style, instead of the View (or in addition to?)

internal object SquooshLayout {
    private var nextLayoutId: Int = 0
    var density: Float = 1f

    internal fun getNextLayoutId(): Int {
        return ++nextLayoutId
    }

    internal fun removeNode(rootLayoutId: Int, layoutId: Int) {
        Jni.jniRemoveNode(rootLayoutId, layoutId, false)
    }

    internal fun keepJniBits() {
        Jni.jniSetNodeSize(0, 0, 0, 0)
        Jni.jniRemoveNode(0, 0, false)
    }

    internal fun doLayout(rootLayoutId: Int, layoutNodes: ArrayList<LayoutNode>): Map<Int, Layout> {
        val serializedNodes = serialize(layoutNodes)
        var response: ByteArray? = null
        val performLayoutTime = measureTimeMillis {
            response = Jni.jniAddNodes(rootLayoutId, serializedNodes)
        }
        if (response == null) return emptyMap()
        var layoutChangedResponse: LayoutChangedResponse? = null
        val layoutDeserializeTime = measureTimeMillis {
            layoutChangedResponse =
                LayoutChangedResponse.deserialize(BincodeDeserializer(response))
        }
        Log.d(TAG, "doLayout: perform layout $performLayoutTime ms, response deser.: $layoutDeserializeTime ms")
        return layoutChangedResponse!!.changed_layouts
    }

    private fun serialize(ln: List<LayoutNode>): ByteArray {
        val layoutNodeList = LayoutNodeList(ln)
        val nodeListSerializer = BincodeSerializer()
        val serializeTime = measureTimeMillis {
            layoutNodeList.serialize(nodeListSerializer)
        }
        Log.d(TAG, "doLayout serialize time $serializeTime ms for ${ln.size} nodes")
        return nodeListSerializer._bytes
    }
}

internal class SquooshResolvedNode(
    val view: View,
    val style: ViewStyle,
    val textInfo: TextMeasureData?,
    var firstChild: SquooshResolvedNode? = null,
    var nextSibling: SquooshResolvedNode? = null,
    var parent: SquooshResolvedNode? = null,
    var computedLayout: Layout? = null
) {
    fun offsetFromAncestor(ancestor: SquooshResolvedNode? = null): PointF
    {
        var n: SquooshResolvedNode? = this
        var x = 0f
        var y = 0f
        while (n != ancestor && n != null) {
            val layout = n.computedLayout
            if (layout != null) {
                x += layout.left
                y += layout.top
            }
            n = n.parent
        }
        return PointF(x, y)
    }
}



// Remember if there's a child composable for a given node, and also we return an ordered
// list of all the child composables we need to render, along with transforms etc.
internal class SquooshChildComposable(
    // One of these should be populated...
    val component: @Composable ((ComponentReplacementContext) -> Unit)?,
    val content: ReplacementContent?,

    // We use this to look up the transform and layout translation.
    val node: SquooshResolvedNode
)

// We want to provide the node to a Compose layout customization, and we do that using
// the ParentDataModifier.
private data class SquooshParentData(val node: SquooshResolvedNode) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any? { return this@SquooshParentData }
}

// This is a holder for the current child composable that we want to draw. Compose doesn't
// let us draw children individually, so instead, any time we want to draw a child we set it
// in an instance of the holder, and draw all children, and have each child filter if it draws
// or not. Terrible hack, but it's not clear what the alternatives are.
internal class SquooshChildRenderSelector(
    var selectedRenderChild: SquooshResolvedNode? = null
)

val newlineRegex = Regex("\\R+")
val lineSeparator: String? = System.getProperty("line.separator")

private fun normalizeNewlines(text: String): String {
    if (lineSeparator != null)
        return text.replace(newlineRegex, lineSeparator)
    return text
}

internal fun computeTextInfo(
    v: View,
    density: Density,
    document: DocContent,
    customizations: CustomizationContext,
    fontResourceLoader: Font.ResourceLoader
): TextMeasureData? {
    val customizedText = customizations.getText(v.name)
    val customTextStyle = customizations.getTextStyle(v.name)
    val fontFamily = DesignSettings.fontFamily(v.style.font_family)

    val annotatedText = if (customizedText != null) {
        val builder = AnnotatedString.Builder()
        builder.append(normalizeNewlines(customizedText))
        builder.toAnnotatedString()
    } else when (v.data) {
        is ViewData.Text -> {
            val builder = AnnotatedString.Builder()
            builder.append(normalizeNewlines((v.data as ViewData.Text).content))
            builder.toAnnotatedString()
        }
        is ViewData.StyledText -> {
            val builder = AnnotatedString.Builder()
            for (run in (v.data as ViewData.StyledText).content) {
                val textBrushAndOpacity = run.style.text_color.asBrush(document, density.density)
                builder.pushStyle(
                    @OptIn(ExperimentalTextApi::class)
                    SpanStyle(
                        brush = textBrushAndOpacity?.first,
                        alpha = textBrushAndOpacity?.second ?: 1.0f,
                        fontSize = run.style.font_size.sp,
                        fontWeight =
                            FontWeight(
                                run.style.font_weight.value.roundToInt()
                            ),
                        fontStyle =
                            when (run.style.font_style) {
                                is com.android.designcompose.serdegen.FontStyle.Italic -> FontStyle.Italic
                                else -> FontStyle.Normal
                            },
                        fontFamily = DesignSettings.fontFamily(run.style.font_family, fontFamily),
                        fontFeatureSettings =
                            run.style.font_features.joinToString(", ") { feature ->
                                String(feature.tag.toByteArray())
                            },
                        //platformStyle = PlatformSpanStyle(includeFontPadding = false),
                    )
                )
                builder.append(run.text)
                builder.pop()
            }
            builder.toAnnotatedString()
        }
        else -> return null
    }

    val lineHeight =
        customTextStyle?.lineHeight
            ?: when (v.style.line_height) {
                is LineHeight.Pixels -> (v.style.line_height as LineHeight.Pixels).value.sp
                /*
                    when (v.data) {
                        is ViewData.StyledText -> ((v.style.line_height as LineHeight.Pixels).value / v.style.font_size).em
                        else -> (v.style.line_height as LineHeight.Pixels).value.sp
                    }*/
                else -> TextUnit.Unspecified
            }
    val fontWeight =
        customTextStyle?.fontWeight
            ?: FontWeight(v.style.font_weight.value.roundToInt())
    val fontStyle =
        customTextStyle?.fontStyle
            ?: when (v.style.font_style) {
                is com.android.designcompose.serdegen.FontStyle.Italic -> FontStyle.Italic
                else -> FontStyle.Normal
            }
    // Compose only supports a single outset shadow on text; we must use a canvas and perform
    // manual text layout (and editing, and accessibility) to do fancier text.
    val shadow =
        v.style.text_shadow.flatMap { textShadow ->
            Optional.of(
                Shadow(
                    // Ensure that blur radius is never zero, because Compose interprets that as no
                    // shadow (rather than as a hard-edged shadow).
                    blurRadius = textShadow.blur_radius * density.density * blurFudgeFactor + 0.1f,
                    offset =
                    Offset(
                        textShadow.offset[0] * density.density,
                        textShadow.offset[1] * density.density
                    ),
                    color = convertColor(textShadow.color)
                )
            )
        }
    val textBrushAndOpacity = v.style.text_color.asBrush(document, density.density)
    val textStyle =
        @OptIn(ExperimentalTextApi::class)
        TextStyle(
            brush = textBrushAndOpacity?.first,
            alpha = textBrushAndOpacity?.second ?: 1.0f,
            fontSize = customTextStyle?.fontSize ?: v.style.font_size.sp,
            fontFamily = fontFamily,
            fontFeatureSettings =
                v.style.font_features.joinToString(", ") { feature ->
                    String(feature.tag.toByteArray())
                },
            lineHeight = lineHeight,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            textAlign =
                customTextStyle?.textAlign
                    ?: when (v.style.text_align) {
                        is com.android.designcompose.serdegen.TextAlign.Center -> TextAlign.Center
                        is com.android.designcompose.serdegen.TextAlign.Right -> TextAlign.Right
                        else -> TextAlign.Left
                    },
            shadow = shadow.orElse(null),
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both
            )
        )

    val paragraph = ParagraphIntrinsics(
        text = annotatedText.text,
        style = textStyle,
        spanStyles = annotatedText.spanStyles,
        density = density,
        resourceLoader = fontResourceLoader
    )

    val textLayoutData =
        TextLayoutData(annotatedText, textStyle, fontResourceLoader, v.style.text_size, paragraph)
    val maxLines = if (v.style.line_count.isPresent) v.style.line_count.get().toInt() else Int.MAX_VALUE

    return TextMeasureData(
        textLayoutData,
        density,
        maxLines,
        v.style.min_width.pointsAsDp(density.density).value
    )
}

/// Iterate over a given view tree recursively, applying customizations that will select
/// variants or affect layout. The output of this function is a `SquooshResolvedNode` which
/// has links to siblings and children (but no layout value).
///
/// Subsequent passes of this generated tree can set up a native layout tree, and then populate
/// the computed layout values, and finally render the view tree.
internal fun resolveVariantsRecursively(
    v: View,
    document: DocContent,
    customizations: CustomizationContext,
    interactionState: InteractionState,
    parentComponents: List<ParentComponentInfo>,
    density: Density,
    fontResourceLoader: Font.ResourceLoader,
    // XXX: This probably won't show up in any profile, but I used linked lists everywhere
    //      else to reduce the number of objects we make (especially since we run this code
    //      every recompose.
    composableList: ArrayList<SquooshChildComposable>,
    variantParentName: String = ""): SquooshResolvedNode
{
    // XXX: This seems to do a lot of extra allocations. We'll realloc this list all the way
    //      down, when I think we could simply push and pop. It would be nice if there was a
    //      way to avoid even that, since the list is a static property of the View and not
    //      a dynamic thing at all.
    val parentComps =
        if (v.component_info.isPresent) {
            val pc = parentComponents.toMutableList()
            pc.add(ParentComponentInfo(v.id, v.component_info.get()))
            pc
        } else {
            parentComponents
        }

    // Do we have an override style? This is style data which we should apply to the final style
    // even if we're swapping out our view definition for a variant.
    var overrideStyle: ViewStyle? = null
    if (v.component_info.isPresent) {
        val componentInfo = v.component_info.get()
        if (componentInfo.overrides.isPresent) {
            val overrides = componentInfo.overrides.get()
            if (overrides.style.isPresent) {
                overrideStyle = overrides.style.get()
            }
        }
    }

    // See if we've got a replacement node from an interaction
    var view = interactionState.squooshNodeVariant(v.id, customizations.getKey(), document) ?: v
    var hasVariantReplacement = view.name != v.name
    var variantParentName = variantParentName
    if (!hasVariantReplacement) {
        // If an interaction has not changed the current variant, then check to see if this node
        // is part of a component set with variants and if any @DesignVariant annotations
        // set variant properties that match. If so, variantNodeName will be set to the
        // name of the node with all the variants set to the @DesignVariant parameters
        val variantNodeName = customizations.getMatchingVariant(view.component_info)
        if (variantNodeName != null) {
            // Find the view associated with the variant name
            val variantNodeQuery =
                NodeQuery.NodeVariant(variantNodeName, variantParentName.ifEmpty { view.name })
            val isRoot = true // XXX XXX RALPH
            val variantView = interactionState.squooshRootNode(variantNodeQuery, document, isRoot)
            if (variantView != null) {
                view = variantView
                hasVariantReplacement = true
                variantView.component_info.ifPresent { variantParentName = it.component_set_name }
            }
        }
    }

    // Calculate the style we're going to use. If we have an override style then we have to apply
    // that on top of the view (or variant) style.
    val style =
        if (overrideStyle == null) {
            view.style
        } else {
            mergeStyles(view.style, overrideStyle)
        }
    // XXX: Skip figuring out grid info, scrolling, interactions.

    // Now we know the view we want to render, the style we want to use, etc. We can create
    // a record of it. After this, another pass can be done to build a layout tree. Finally,
    // layout can be performed and rendering done.
    val textInfo = computeTextInfo(view, density, document, customizations, fontResourceLoader)
    val resolvedView = SquooshResolvedNode(view, style, textInfo)

    if (view.data is ViewData.Container) {
        val viewData = view.data as ViewData.Container
        var previousChild: SquooshResolvedNode? = null

        for (child in viewData.children) {
            val childResolvedNode = resolveVariantsRecursively(
                child,
                document,
                customizations,
                interactionState,
                parentComps,
                density,
                fontResourceLoader,
                composableList,
            )

            childResolvedNode.parent = resolvedView

            if (resolvedView.firstChild == null)
                resolvedView.firstChild = childResolvedNode
            if (previousChild != null)
                previousChild.nextSibling = childResolvedNode

            previousChild = childResolvedNode
        }
    }

    // If this node has a content customization, then we make a special record of it so that we can
    // zip through all of them after layout and render them in the right location.
    val replacementContent = customizations.getContent(view.name)
    val replacementComponent = customizations.getComponent(view.name)
    if (replacementContent != null || replacementComponent != null) {
        composableList.add(
            SquooshChildComposable(
                component = replacementComponent,
                content = replacementContent,
                node = resolvedView
            )
        )
    }

    return resolvedView
}

/// Takes a `SquooshResolvedNode` and recursively builds or updates a native layout tree via
/// the `SquooshLayout` wrapper of `JniLayout`.
internal fun updateLayoutTree(
    rootLayoutId: Int,
    resolvedNode: SquooshResolvedNode,
    layoutCache: HashMap<Int, Int>,
    layoutNodes: ArrayList<LayoutNode>,
    parentLayoutId: Int = 0,
    childIndex: Int = 0,
) {
    // Make a unique layout id for this node by taking the root's unique id and adding the
    // file specific unique id (which is a u16).
    val layoutId = rootLayoutId + resolvedNode.view.unique_id

    // Compute a cache key for the layout; we use this to determine if we need to update the
    // node with a new layout value or not.
    val layoutCacheKey = resolvedNode.style.hashCode()
    val needsLayoutUpdate = layoutCache[layoutId] != layoutCacheKey

    if (needsLayoutUpdate) {
        var useMeasureFunc = false
        var fixedWidth: Optional<Int> = Optional.empty()
        var fixedHeight: Optional<Int> = Optional.empty()

        // Text needs some additional work to measure, and to let layout measure interactively
        // to account for wrapping.
        if (resolvedNode.textInfo != null) {
            val density = resolvedNode.textInfo.density
            if (isAutoHeightFillWidth(resolvedNode.style) || true) {
                // We need layout to measure this text.
                useMeasureFunc = true

                // This is used by the callback logic in DesignText.kt to compute width-for-height
                // computations for the layout implementation in Rust.
                LayoutManager.squooshSetTextMeasureData(
                    layoutId,
                    resolvedNode.textInfo
                )

            } else {
                // We can measure the text now because it's constrained.
                val textBounds = measureTextBounds(
                    resolvedNode.style,
                    resolvedNode.textInfo.textLayout,
                    resolvedNode.textInfo.density
                )
                fixedWidth = Optional.of((textBounds.width / density.density).roundToInt())
                fixedHeight = Optional.of((textBounds.layoutHeight / density.density).roundToInt())
            }
        }

        layoutNodes.add(
            LayoutNode(
                layoutId,
                parentLayoutId,
                childIndex,
                resolvedNode.style,
                resolvedNode.view.name,
                useMeasureFunc,
                fixedWidth,
                fixedHeight
            )
        )
        layoutCache[layoutId] = layoutCacheKey
    }
    // XXX: We might want separate (cheaper) calls to assert the tree structure.
    // XXX XXX: This code doesn't ever update the tree structure.

    var childIdx = 0
    var child = resolvedNode.firstChild
    while (child != null) {
        updateLayoutTree(rootLayoutId, child, layoutCache, layoutNodes, layoutId, childIdx)
        childIdx++
        child = child.nextSibling
    }
}

/// Iterate over a `SquooshComputedNode` tree and populate the computed layout values
/// so that the nodes can be used for presentation or interaction (hit testing).
internal fun populateComputedLayout(
    rootLayoutId: Int,
    resolvedNode: SquooshResolvedNode,
    layoutValueCache: HashMap<Int, Layout>
)
{
    val layoutId = rootLayoutId + resolvedNode.view.unique_id
    val layoutValue = layoutValueCache[layoutId]
    if (layoutValue == null) {
        Log.d(TAG, "Unable to fetch computed layout for ${resolvedNode.view.name} and its children")
    }
    resolvedNode.computedLayout = layoutValue

    var child = resolvedNode.firstChild
    while (child != null) {
        populateComputedLayout(rootLayoutId, child, layoutValueCache)
        child = child.nextSibling
    }
}
// Experiment -- minimal DesignCompose root node; no switcher, no interactions, etc.
@Composable
fun SquooshRoot(
    docName: String,
    incomingDocId: String,
    rootNodeQuery: NodeQuery,
    customizationContext: CustomizationContext = CustomizationContext()
) {
    val docId = DocumentSwitcher.getSwitchedDocId(incomingDocId)
    val doc = DocServer.doc(docName, docId, DocumentServerParams(), null, false)

    if (doc == null) {
        Log.d(TAG, "No doc! $docName / $incomingDocId")
        return
    }

    val interactionState = InteractionStateManager.stateForDoc(docId)
    val startFrame = interactionState.rootNode(initialNode = rootNodeQuery, doc = doc, isRoot = true)

    if (startFrame == null) {
        Log.d(TAG, "No start frame $docName / $incomingDocId")
        SquooshLayout.keepJniBits() // XXX: Must call this from somewhere otherwise it gets stripped and the jni lib won't link.
        return
    }

    val density = LocalDensity.current
    LaunchedEffect(density.density) { LayoutManager.setDensity(density.density) }

    val variantParentName = when (rootNodeQuery) {
        is NodeQuery.NodeVariant -> rootNodeQuery.field1
        else -> ""
    }

    val rootLayoutId = remember { SquooshLayout.getNextLayoutId() * 1000000 }
    val layoutCache = remember { HashMap<Int, Int>() }
    val layoutValueCache = remember { HashMap<Int, Layout>() }

    // Ok, now we have done the dull stuff, we need to build a tree applying
    // the correct variants etc and then build/update the tree. How do we know
    // what's different from last time? Does the Rust side track

    var root: SquooshResolvedNode? = null
    val childComposables: ArrayList<SquooshChildComposable> = arrayListOf()
    val resolveVariantsTime = measureTimeMillis {
        root = resolveVariantsRecursively(
            startFrame,
            doc,
            customizationContext,
            interactionState,
            emptyList(),
            density,
            LocalFontLoader.current,
            childComposables,
            variantParentName
        )
    }
    val layoutNodes: ArrayList<LayoutNode> = arrayListOf()
    val buildLayoutTreeTime = measureTimeMillis {
        updateLayoutTree(rootLayoutId, root!!, layoutCache, layoutNodes)
    }
    val performLayoutTime = measureTimeMillis {
        val updatedLayouts = SquooshLayout.doLayout(
            rootLayoutId + root!!.view.unique_id,
            layoutNodes
        )
        layoutValueCache.putAll(updatedLayouts)
        // XXX: layoutValueCache doesn't remove nodes that are no longer in the tree.
        Log.d(TAG, "$docName layout invalidated ${updatedLayouts.size} nodes")
    }
    val populateLayoutTime = measureTimeMillis {
        populateComputedLayout(rootLayoutId, root!!, layoutValueCache)
    }

    Log.d(TAG, "$docName resolveVariants: $resolveVariantsTime ms, buildLayout: $buildLayoutTreeTime ms, eval. layout $performLayoutTime ms, populate layout values: $populateLayoutTime ms")

    // Now our tree of resolved nodes is ready to use!

    // Select which child to draw using this holder.
    val childRenderSelector = SquooshChildRenderSelector()

    androidx.compose.ui.layout.Layout(
        modifier = Modifier
            .size(
                width = root!!.computedLayout!!.width.dp,
                height = root!!.computedLayout!!.height.dp
            )
            .squooshRender(
                root!!,
                doc,
                docName,
                customizationContext,
                childRenderSelector
            ),
        measurePolicy = { measurables, constraints ->
            val placeables = measurables.map { measurable ->
                val squooshData = measurable.parentData as? SquooshParentData
                if (squooshData == null || squooshData.node.computedLayout == null) {
                    // Oops! No data, just lay it out however it wants.
                    Pair(measurable.measure(constraints), null)
                } else {
                    // Ok, we can get some layout data. This lets us determine a width
                    // and height from layout. We also need to extract a transform, then
                    // we can position this view appropriately, and create a layer for it
                    // if it has a rotation applied (unfortunately Compose doesn't seem to
                    // accept a full matrix transform, so we can't support shears).
                    val w = (squooshData.node.computedLayout!!.width * density.density).roundToInt()
                    val h = (squooshData.node.computedLayout!!.height * density.density).roundToInt()

                    Pair(measurable.measure(Constraints(
                        minWidth = w,
                        maxWidth = w,
                        minHeight = h,
                        maxHeight = h
                    )), squooshData.node)
                }
            }

            layout(constraints.maxWidth, constraints.maxHeight) {
                // Place children in the parent layout
                placeables.forEach { (placeable, node) ->
                    // If we don't have a node, then just place this and finish.
                    if (node == null) {
                        placeable.placeRelative(x = 0, y = 0)
                    } else {
                        // Ok, we can look up the position and transform by iterating over the
                        // parents. We don't support transforms here yet. Child composables will
                        // be rendered with transforms, but won't use them for input.
                        //
                        // We always take the offset from the root, but if there are layers of
                        // custom composables (containing each other) then this will give the
                        // wrong offset.
                        //
                        // XXX XXX: Create ticket to implement transformed input.
                        val offsetFromRoot = node.offsetFromAncestor()

                        placeable.placeRelative(
                            x = (offsetFromRoot.x * density.density).roundToInt(),
                            y = (offsetFromRoot.y * density.density).roundToInt()
                        )
                    }
                }
            }
        },
        content = {
            // Now render all of the children
            for (child in childComposables) {
                if (child.node.computedLayout == null) { continue }
                Box(
                    Modifier
                        .drawWithContent {
                            if (child.node == childRenderSelector.selectedRenderChild)
                                drawContent()
                        }
                        .then(SquooshParentData(node = child.node))
                )
                {
                    if (child.component != null) {
                        child.component!!(object : ComponentReplacementContext {
                            override val appearanceModifier: Modifier = Modifier
                            override val layoutModifier: Modifier = Modifier
                            @Composable override fun Content() {}
                            override val textStyle: TextStyle? = null
                            override val parentLayout: ParentLayoutInfo? = null
                        })
                    } else if (child.content != null) {
                        Log.d(TAG, "Unimplemented: child.content")
                    }
                }
            }
        }
    )
}

internal fun Modifier.squooshRender(
    node: SquooshResolvedNode,
    document: DocContent,
    docName: String,
    customizations: CustomizationContext,
    childRenderSelector: SquooshChildRenderSelector
): Modifier =
    this.then(
        Modifier.drawWithContent {
            var nodeRenderCount = 0
            val renderTime = measureTimeMillis {
                fun renderNode(node: SquooshResolvedNode) {
                    val computedLayout = node.computedLayout ?: return
                    val shape = when (node.view.data) {
                        is ViewData.Container -> (node.view.data as ViewData.Container).shape
                        else -> {
                            if (node.textInfo != null) {
                                squooshTextRender(
                                    drawContext,
                                    this,
                                    node.textInfo,
                                    node.style,
                                    computedLayout
                                )
                                nodeRenderCount++
                            }
                            return
                        }
                    }

                    if (customizations.getComponent(node.view.name) != null) {
                        // We need to offset the translation that we did to position the child
                        // Composable for Compose's layout phase. We lay the child out in the
                        // correct position so that hit testing works, but we've already got the
                        // full transform computed here... so we need to invert that.
                        val offsetFromRoot = node.offsetFromAncestor()
                        childRenderSelector.selectedRenderChild = node
                        drawContext.canvas.translate(-offsetFromRoot.x * density, -offsetFromRoot.y * density)
                        drawContent()
                        drawContext.canvas.translate(offsetFromRoot.x * density, offsetFromRoot.y * density)
                        childRenderSelector.selectedRenderChild = null
                    }

                    // If we have masked children, then we need to do create a layer for the parent
                    // and have the child draw into a layer that's blended with DstIn.
                    //
                    // XXX: We could take the smallest of the mask size and common parent size, and
                    //      then transform children appropriately.
                    val nodeSize = Size(computedLayout.width * density, computedLayout.height * density)

                    squooshShapeRender(
                        drawContext,
                        density,
                        nodeSize,
                        node.style,
                        shape,
                        null, // customImageWithContext
                        document,
                        node.view.name,
                        customizations
                    ) {
                        var child = node.firstChild
                        var pendingMask: SquooshResolvedNode? = null

                        while (child != null) {
                            if (child.view.isMask()) {
                                // We were already drawing a mask! Wrap it up...
                                if (pendingMask != null) {
                                    val dstInPaint = Paint()
                                    dstInPaint.blendMode = BlendMode.DstIn

                                    // Draw the mask as DstIn
                                    drawContext.canvas.saveLayer(
                                        nodeSize.toRect(),
                                        dstInPaint
                                    )
                                    translate(
                                        pendingMask.computedLayout!!.left * density,
                                        pendingMask.computedLayout!!.top * density)
                                    {
                                        renderNode(pendingMask!!)
                                    }

                                    drawContext.canvas.restore()

                                    // Restore the layer that got saved for the mask and content.
                                    drawContext.canvas.restore()
                                }

                                // We're starting a mask operation, so save a layer, and go on to
                                // render children. If we encounter another mask, or if we get to
                                // the end of the children, then we need to pop the mask.
                                pendingMask = child
                                child = child.nextSibling

                                drawContext.canvas.saveLayer(
                                    nodeSize.toRect(),
                                    Paint()
                                )
                            } else {
                                val childLayout = child.computedLayout
                                if (childLayout != null) {
                                    translate(childLayout.left * density, childLayout.top * density) {
                                        renderNode(child!!)
                                    }
                                }
                                child = child.nextSibling
                            }
                        }

                        // XXX: This logic is duplicated above; it needs to be factored out
                        //      somehow.
                        if (pendingMask != null) {
                            val dstInPaint = Paint()
                            dstInPaint.blendMode = BlendMode.DstIn

                            // Draw the mask as DstIn
                            drawContext.canvas.saveLayer(
                                nodeSize.toRect(),
                                dstInPaint
                            )
                            translate(
                                pendingMask.computedLayout!!.left * density,
                                pendingMask.computedLayout!!.top * density)
                            {
                                renderNode(pendingMask)
                            }

                            drawContext.canvas.restore()

                            // Restore the layer that got saved for the mask and content.
                            drawContext.canvas.restore()
                        }
                    }
                    nodeRenderCount++
                }
                renderNode(node)
            }
            Log.d(TAG, "$docName rendered $nodeRenderCount nodes in ${renderTime}ms")
        }
    )

internal fun squooshTextRender(
    drawContext: DrawContext,
    density: Density,
    textInfo: TextMeasureData,
    style: ViewStyle,
    computedLayout: Layout,
) {
    val paragraph = Paragraph(
        paragraphIntrinsics = textInfo.textLayout.paragraph,
        width = computedLayout.width * density.density,
        maxLines = textInfo.maxLines,
        ellipsis = style.text_overflow is TextOverflow.Ellipsis
    )

    // Apply any styled transform or blend mode.
    // XXX: transform customization?
    val transform = style.transform.asComposeTransform(density.density)
    val blendMode = style.blend_mode.asComposeBlendMode()
    val useBlendModeLayer = style.blend_mode.useLayer()

    if (useBlendModeLayer) {
        val paint = Paint()
        paint.blendMode = blendMode
        drawContext.canvas.saveLayer(
            Rect(
                0f,
                0f,
                computedLayout.width * density.density,
                computedLayout.height * density.density
            ),
            paint
        )
    } else if (transform != null) {
        drawContext.canvas.save()
    }

    if (transform != null)
        drawContext.transform.transform(transform)

    // Apply vertical centering; this would be better done in layout.
    val verticalCenterOffset = when (style.text_align_vertical) {
        is TextAlignVertical.Center -> (computedLayout.height * density.density - paragraph.height) / 2f
        is TextAlignVertical.Bottom -> computedLayout.height * density.density - paragraph.height
        else -> 0.0f
    }

    drawContext.canvas.translate(0.0f, verticalCenterOffset)
    paragraph.paint(drawContext.canvas)
    drawContext.canvas.translate(0.0f, -verticalCenterOffset)

    if (useBlendModeLayer || transform != null)
        drawContext.canvas.restore()
}

package com.android.designcompose.squoosh

import android.graphics.PointF
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Animation
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.input.pointer.pointerInput
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
import com.android.designcompose.AnimatedTransition
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
import com.android.designcompose.clonedWithTransitionsApplied
import com.android.designcompose.common.DocumentServerParams
import com.android.designcompose.convertColor
import com.android.designcompose.dispatch
import com.android.designcompose.doc
import com.android.designcompose.getComponent
import com.android.designcompose.getContent
import com.android.designcompose.getKey
import com.android.designcompose.getMatchingVariant
import com.android.designcompose.getText
import com.android.designcompose.getTextStyle
import com.android.designcompose.isMask
import com.android.designcompose.mergeStyles
import com.android.designcompose.pointsAsDp
import com.android.designcompose.rootNode
import com.android.designcompose.serdegen.Action
import com.android.designcompose.serdegen.Layout
import com.android.designcompose.serdegen.LayoutChangedResponse
import com.android.designcompose.serdegen.LayoutNode
import com.android.designcompose.serdegen.LayoutNodeList
import com.android.designcompose.serdegen.LayoutParentChildren
import com.android.designcompose.serdegen.LineHeight
import com.android.designcompose.serdegen.NodeQuery
import com.android.designcompose.serdegen.TextAlignVertical
import com.android.designcompose.serdegen.TextOverflow
import com.android.designcompose.serdegen.Trigger
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewData
import com.android.designcompose.serdegen.ViewStyle
import com.android.designcompose.squooshAnimatedTransitions
import com.android.designcompose.squooshCompleteTransition
import com.android.designcompose.squooshNodeVariant
import com.android.designcompose.squooshRootNode
import com.android.designcompose.squooshShapeRender
import com.android.designcompose.squooshVariantMemory
import com.android.designcompose.stateForDoc
import com.android.designcompose.undoDispatch
import com.android.designcompose.useLayer
import com.novi.bincode.BincodeDeserializer
import com.novi.bincode.BincodeSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
        Jni.jniRemoveNode(layoutId, rootLayoutId, false)
    }

    internal fun keepJniBits() {
        Jni.jniSetNodeSize(0, 0, 0, 0)
    }

    internal fun doLayout(rootLayoutId: Int, layoutNodeList: LayoutNodeList): Map<Int, Layout> {
        val serializedNodes = serialize(layoutNodeList)
        var response: ByteArray?
        val performLayoutTime = measureTimeMillis {
            response = Jni.jniAddNodes(rootLayoutId, serializedNodes)
        }
        if (response == null) return emptyMap()
        var layoutChangedResponse: LayoutChangedResponse?
        val layoutDeserializeTime = measureTimeMillis {
            layoutChangedResponse =
                LayoutChangedResponse.deserialize(BincodeDeserializer(response))
        }
        Log.d(TAG, "doLayout: perform layout $performLayoutTime ms, response deser.: $layoutDeserializeTime ms")
        return layoutChangedResponse!!.changed_layouts
    }

    private fun serialize(layoutNodeList: LayoutNodeList): ByteArray {
        val nodeListSerializer = BincodeSerializer()
        val serializeTime = measureTimeMillis {
            layoutNodeList.serialize(nodeListSerializer)
        }
        Log.d(TAG, "doLayout serialize time $serializeTime ms for ${layoutNodeList.layout_nodes.size} nodes")
        return nodeListSerializer._bytes
    }
}

internal class SquooshLayoutIdAllocator(
    private var lastAllocatedId: Int = 1,
    private val idMap: HashMap<List<ParentComponentInfo>, Int> = HashMap(),
    // We can also track referenced layout IDs from one generation to the next, which lets us
    // build the set of nodes to remove from the native layout tree.
    private var visitedSet: HashSet<Int> = HashSet(),
    private var remainingSet: HashSet<Int> = HashSet(),
)
{
    /// Return a new "root layout id" for a tree node that is an instance of a component. This
    /// ensures that component instance children get unique layout ids even though there might
    /// be many instances of the same component in one tree.
    fun componentLayoutId(component: List<ParentComponentInfo>): Int {
        val maybeId = idMap[component]
        if (maybeId != null) return maybeId
        val id = lastAllocatedId++
        idMap[component] = id
        return id
    }

    /// Note that we've visited a layout node; this protects it from removal and adds it to the
    /// set of nodes that might get removed next iteration.
    fun visitLayoutId(id: Int) {
        visitedSet.add(id)
        remainingSet.remove(id)
    }

    /// Get the set of layout nodes to remove.
    fun removalNodes(): Set<Int> {
        val removalSet = remainingSet
        remainingSet = visitedSet
        visitedSet = HashSet()
        return removalSet
    }
}


internal class SquooshResolvedNode(
    val view: View,
    var style: ViewStyle,
    val layoutId: Int,
    val textInfo: TextMeasureData?,
    val unresolvedNodeId: String, // The node id before we resolved variants; used for interactions
    var firstChild: SquooshResolvedNode? = null,
    var nextSibling: SquooshResolvedNode? = null,
    var parent: SquooshResolvedNode? = null,
    var computedLayout: Layout? = null,
    var needsChildRender: Boolean = false,
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

    // Used for node resolution for interactions
    val parentComponents: List<ParentComponentInfo>,

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
    rootLayoutId: Int,
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
    layoutIdAllocator: SquooshLayoutIdAllocator,
    variantParentName: String = ""): SquooshResolvedNode
{
    var rootLayoutId = rootLayoutId
    // XXX: This seems to do a lot of extra allocations. We'll realloc this list all the way
    //      down, when I think we could simply push and pop. It would be nice if there was a
    //      way to avoid even that, since the list is a static property of the View and not
    //      a dynamic thing at all.
    var parentComps = parentComponents
    if (v.component_info.isPresent) {
        val pc = parentComponents.toMutableList()
        pc.add(ParentComponentInfo(v.id, v.component_info.get()))
        parentComps = pc

        // Ensure that the children of this component get unique layout ids, even though there
        // may be multiple instances of the same component in one tree.
        rootLayoutId = layoutIdAllocator.componentLayoutId(pc) * 1000000
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

    // Now we know the view we want to render, the style we want to use, etc. We can create
    // a record of it. After this, another pass can be done to build a layout tree. Finally,
    // layout can be performed and rendering done.
    val layoutId = rootLayoutId + v.unique_id
    layoutIdAllocator.visitLayoutId(layoutId)

    val textInfo = computeTextInfo(view, density, document, customizations, fontResourceLoader)
    val resolvedView = SquooshResolvedNode(view, style, layoutId, textInfo, v.id)

    if (view.data is ViewData.Container) {
        val viewData = view.data as ViewData.Container
        var previousChild: SquooshResolvedNode? = null

        for (child in viewData.children) {
            val childResolvedNode = resolveVariantsRecursively(
                child,
                rootLayoutId,
                document,
                customizations,
                interactionState,
                parentComps,
                density,
                fontResourceLoader,
                composableList,
                layoutIdAllocator,
            )

            childResolvedNode.parent = resolvedView

            if (resolvedView.firstChild == null)
                resolvedView.firstChild = childResolvedNode
            if (previousChild != null)
                previousChild.nextSibling = childResolvedNode

            previousChild = childResolvedNode
        }
    }

    // Find out if we have some supported interactions; currently that's just on press and on click.
    // We'll add timeouts and the others later...
    var hasSupportedInteraction = false
    view.reactions.ifPresent { reactions ->
        reactions.forEach { r ->
            hasSupportedInteraction = hasSupportedInteraction || r.trigger is Trigger.OnClick || r.trigger is Trigger.OnPress
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
                node = resolvedView,
                parentComponents = parentComps
            )
        )
        // Make sure that the renderer knows that it needs to do an external render for this
        // node.
        resolvedView.needsChildRender = true
    } else if (hasSupportedInteraction) {
        // Add a SquooshChildComposable to handle the interaction.
        composableList.add(
            SquooshChildComposable(
                component = null,
                content = null,
                node = resolvedView,
                parentComponents = parentComps
            )
        )
    }

    return resolvedView
}

/// Takes a `SquooshResolvedNode` and recursively builds or updates a native layout tree via
/// the `SquooshLayout` wrapper of `JniLayout`.
internal fun updateLayoutTree(
    resolvedNode: SquooshResolvedNode,
    layoutCache: HashMap<Int, Int>,
    layoutNodes: ArrayList<LayoutNode>,
    layoutParentChildren: ArrayList<LayoutParentChildren>,
    parentLayoutId: Int = 0,
): Boolean {
    // Make a unique layout id for this node by taking the root's unique id and adding the
    // file specific unique id (which is a u16).
    val layoutId = resolvedNode.layoutId

    // Compute a cache key for the layout; we use this to determine if we need to update the
    // node with a new layout value or not.
    val layoutCacheKey = resolvedNode.style.hashCode()
    val needsLayoutUpdate = layoutCache[layoutId] != layoutCacheKey
    val layoutChildren: ArrayList<Int> = arrayListOf()

    if (needsLayoutUpdate) {
        var useMeasureFunc = false

        // Text needs some additional work to measure, and to let layout measure interactively
        // to account for wrapping.
        if (resolvedNode.textInfo != null) {
            // We need layout to measure this text.
            useMeasureFunc = true

            // This is used by the callback logic in DesignText.kt to compute width-for-height
            // computations for the layout implementation in Rust.
            LayoutManager.squooshSetTextMeasureData(
                layoutId,
                resolvedNode.textInfo
            )
        }

        layoutNodes.add(
            LayoutNode(
                layoutId,
                parentLayoutId,
                -1, // not childIdx!
                resolvedNode.style,
                resolvedNode.view.name,
                useMeasureFunc,
                Optional.empty(),
                Optional.empty()
            )
        )
        layoutCache[layoutId] = layoutCacheKey
    }
    // XXX: We might want separate (cheaper) calls to assert the tree structure.
    // XXX XXX: This code doesn't ever update the tree structure.

    var updateLayoutChildren = needsLayoutUpdate
    var child = resolvedNode.firstChild
    while (child != null) {
        layoutChildren.add(child.layoutId)
        updateLayoutChildren = updateLayoutTree(child, layoutCache, layoutNodes, layoutParentChildren, layoutId) || updateLayoutChildren
        child = child.nextSibling
    }

    if (updateLayoutChildren) {
        layoutParentChildren.add(LayoutParentChildren(layoutId, layoutChildren))
    }

    return needsLayoutUpdate
}

/// Iterate over a `SquooshComputedNode` tree and populate the computed layout values
/// so that the nodes can be used for presentation or interaction (hit testing).
internal fun populateComputedLayout(
    resolvedNode: SquooshResolvedNode,
    layoutValueCache: HashMap<Int, Layout>
)
{
    val layoutId = resolvedNode.layoutId
    val layoutValue = layoutValueCache[layoutId]
    if (layoutValue == null) {
        Log.d(TAG, "Unable to fetch computed layout for ${resolvedNode.view.name} and its children")
    }
    resolvedNode.computedLayout = layoutValue

    var child = resolvedNode.firstChild
    while (child != null) {
        populateComputedLayout(child, layoutValueCache)
        child = child.nextSibling
    }
}

internal fun findTargetInstanceId(
    document: DocContent,
    parentComponents: List<ParentComponentInfo>,
    action: Action): String?
{
    val destinationId =
        when (action) {
            is Action.Node -> action.destination_id.orElse(null)
            else -> null
        } ?: return null

    val componentSetId = document.c.document.component_sets[destinationId] ?: return null

    Log.d(TAG, "Looking for component set $componentSetId...")

    // Look up our list of parent components and try to find one that is a member of
    // this component set.
    for (parentComponentInfo in parentComponents.reversed()) {//(i in 0..parentComponents.size) {
        Log.d(TAG, "  inspecting ${parentComponentInfo.instanceId} / ${parentComponentInfo.componentInfo.id} / ${parentComponentInfo.componentInfo.component_set_name}")
        Log.d(TAG, "   doc has component set id: ${document.c.document.component_sets[parentComponentInfo.componentInfo.id]}")
        if (
            componentSetId ==
            document.c.document.component_sets[parentComponentInfo.componentInfo.id]
        ) {
            return parentComponentInfo.instanceId
        }
    }

    Log.d(TAG, "Looked for action dest ${destinationId} got component set id ${componentSetId} but didn't find it in ${parentComponents.size}")

    return null
}

internal fun squooshInteractionModifier(
    document: DocContent,
    interactionState: InteractionState,
    interactionScope: CoroutineScope,
    customizations: CustomizationContext,
    childComposable: SquooshChildComposable
): Modifier {
    val node = childComposable.node
    return Modifier.pointerInput(node.view.reactions) {
        interactionScope.launch {
            detectTapGestures(
                onPress = {
                    if (!node.view.reactions.isPresent) return@detectTapGestures
                    val reactions = node.view.reactions.get()

                    // Set the "pressed" state.
                    for (r in reactions.filter { r -> r.trigger is Trigger.OnPress }) {
                        interactionState.dispatch(
                            r.action,
                            findTargetInstanceId(document, childComposable.parentComponents, r.action),
                            customizations.getKey(),
                            node.unresolvedNodeId
                        )
                    }
                    // XXX XXX ralph: we need to remember that we're pressed and keep emitting
                    //                this pointerInput modifier.
                    val dispatchClickEvent = tryAwaitRelease()

                    // Clear the "pressed" state.
                    for (r in reactions.filter { r -> r.trigger is Trigger.OnPress }) {
                        interactionState.undoDispatch(
                            findTargetInstanceId(document, childComposable.parentComponents, r.action),
                            node.unresolvedNodeId,
                            customizations.getKey()
                        )
                    }

                    // If the tap wasn't cancelled (turned into a drag, a window opened on top of
                    // us, etc) then we can run the action.
                    if (dispatchClickEvent) {
                        for (r in reactions.filter { r -> r.trigger is Trigger.OnClick }) {
                            interactionState.dispatch(
                                r.action,
                                findTargetInstanceId(document, childComposable.parentComponents, r.action),
                                customizations.getKey(),
                                null // no undo
                            )
                        }
                    }
                }
            )
        }
    }
}

internal class SquooshRenderTransition(
    var control: SquooshAnimate,
    val animation: TargetBasedAnimation<Float, AnimationVector1D>,
    val source: AnimatedTransition,
    var startTimeNanos: Long = 0L,
)

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

    val interactionScope = rememberCoroutineScope()
    val interactionState = InteractionStateManager.stateForDoc(docId)

    // We're starting to support animated transitions
    interactionState.supportAnimations = true

    val startFrame = interactionState.rootNode(initialNode = rootNodeQuery, doc = doc, isRoot = true)

    if (startFrame == null) {
        Log.d(TAG, "No start frame $docName / $incomingDocId")
        SquooshLayout.keepJniBits() // XXX: Must call this from somewhere otherwise it gets stripped and the jni lib won't link.
        return
    }

    // Ensure we get invalidated when the variant memory is updated from an interaction.
    interactionState.squooshVariantMemory(doc)

    val density = LocalDensity.current
    LaunchedEffect(density.density) { LayoutManager.setDensity(density.density) }

    val variantParentName = when (rootNodeQuery) {
        is NodeQuery.NodeVariant -> rootNodeQuery.field1
        else -> ""
    }

    val rootLayoutId = remember { 0 /*SquooshLayout.getNextLayoutId() * 1000000*/ }
    val layoutIdAllocator = remember { SquooshLayoutIdAllocator() }
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
            rootLayoutId,
            doc,
            customizationContext,
            interactionState,
            emptyList(),
            density,
            LocalFontLoader.current,
            childComposables,
            layoutIdAllocator,
            variantParentName
        )
    }

    val removeOldLayoutNodesTime = measureTimeMillis {
        val removalNodes = layoutIdAllocator.removalNodes()
        for (layoutId in removalNodes) {
            SquooshLayout.removeNode(rootLayoutId, layoutId)
            layoutValueCache.remove(layoutId)
            layoutCache.remove(layoutId)
        }
        Log.d(TAG, "$docName remove ${removalNodes.size} nodes from layout value cache")
    }

    var layoutNodeList = LayoutNodeList(emptyList(), emptyList())
    val buildLayoutTreeTime = measureTimeMillis {
        val layoutNodes: ArrayList<LayoutNode> = arrayListOf()
        val layoutParentChildren: ArrayList<LayoutParentChildren> = arrayListOf()
        updateLayoutTree(root!!, layoutCache, layoutNodes, layoutParentChildren)
        layoutNodeList = LayoutNodeList(layoutNodes, layoutParentChildren)
    }

    val performLayoutTime = measureTimeMillis {
        val updatedLayouts = SquooshLayout.doLayout(
            root!!.layoutId,
            layoutNodeList
        )
        val priorLayoutCacheSize = layoutValueCache.size
        layoutValueCache.putAll(updatedLayouts)
        Log.d(TAG, "$docName layout invalidated ${updatedLayouts.size} nodes; layout cache size: ${layoutValueCache.size}; prior layout cache size: ${priorLayoutCacheSize}")
    }
    val populateLayoutTime = measureTimeMillis {
        populateComputedLayout(root!!, layoutValueCache)
    }

    Log.d(TAG, "$docName resolveVariants: $resolveVariantsTime ms, buildLayout: $buildLayoutTreeTime ms, remove old nodes: $removeOldLayoutNodesTime ms, eval. layout $performLayoutTime ms, populate layout values: $populateLayoutTime ms")

    // ok, now "root" is good. If we have a transition then we need to make another one with the
    // transition applied! omg!
    val transitions = interactionState.squooshAnimatedTransitions(doc)
    val animations = remember { HashMap<Int, SquooshRenderTransition>() }
    val animationValues: MutableState<Map<Int, Float>> = remember { mutableStateOf(mapOf()) }
    val transitionAnimationState = interactionState.clonedWithTransitionsApplied()

    if (transitionAnimationState != null && transitions.isNotEmpty()) {
        Log.d(TAG, "$docName: creating a new root with transitions applied...")
        // We need to make a new root with this interaction state applied, and then compute the
        // animation control between the trees.
        var transitionRoot: SquooshResolvedNode? = null
        val createTransitionTargetTime = measureTimeMillis {
            childComposables.clear()
            transitionRoot = resolveVariantsRecursively(
                startFrame,
                rootLayoutId,
                doc,
                customizationContext,
                transitionAnimationState,
                emptyList(),
                density,
                LocalFontLoader.current,
                childComposables,
                layoutIdAllocator,
                variantParentName
            )
            // Layout maintenance
            val removalNodes = layoutIdAllocator.removalNodes()
            for (layoutId in removalNodes) {
                SquooshLayout.removeNode(rootLayoutId, layoutId)
                layoutValueCache.remove(layoutId)
                layoutCache.remove(layoutId)
            }
            // Build layout tree
            val layoutNodes: ArrayList<LayoutNode> = arrayListOf()
            val layoutParentChildren: ArrayList<LayoutParentChildren> = arrayListOf()
            updateLayoutTree(transitionRoot!!, layoutCache, layoutNodes, layoutParentChildren)
            layoutNodeList = LayoutNodeList(layoutNodes, layoutParentChildren)
            // Perform layout
            val updatedLayouts = SquooshLayout.doLayout(
                transitionRoot!!.layoutId,
                layoutNodeList
            )
            val priorLayoutCacheSize = layoutValueCache.size
            layoutValueCache.putAll(updatedLayouts)
            // Populate layouts
            populateComputedLayout(transitionRoot!!, layoutValueCache)
        }
        Log.d(TAG, "Creating transition root took $createTransitionTargetTime ms")

        val nextAnimations = HashMap<Int, SquooshRenderTransition>()
        for (anim in interactionState.animations) {
            val animationControl =
                newSquooshAnimate(root!!, anim.instanceNodeId, transitionRoot!!, anim.newVariantId)
            if (animationControl == null) {
                Log.d(TAG, "Unable to animate ${anim.instanceNodeId} to ${anim.newVariantId}")
                // XXX: Should we just commit the action here with no transition as if the transition
                //      had ended?
                continue
            }
            val transition = animations.get(anim.id)
            if (transition == null) {
                val animatable = TargetBasedAnimation(
                    animationSpec = spring<Float>(Spring.DampingRatioNoBouncy, Spring.StiffnessVeryLow),
                    typeConverter = Float.VectorConverter,
                    initialValue = 0f,
                    targetValue = 1f
                )
                val rt = SquooshRenderTransition(animationControl, animatable, anim)
                nextAnimations.put(anim.id, rt)
            } else {
                // Update the control with one that knows about new nodes.
                transition.control = animationControl
                nextAnimations.put(anim.id, transition)
            }
        }
        // Make sure we draw from the target root
        root = transitionRoot

        // Evolve the animations list; it would be better to just remove everything that doesn't
        // have a key in transitions...
        animations.clear()
        animations.putAll(nextAnimations)
    }

    // XXX: We could maybe use the most recent unique id to avoid this?
    LaunchedEffect(transitions.map { tx -> tx.id }) {
        // While there are transitions to be run, we should run them; we just update the floats
        // in the mutable state. Those are then used by the render function, and we thus avoid
        // needing to recompose in order to propagate the animation state.
        //
        // We also complete transitions in this block, and that action does cause a recomposition
        // via the subscription that SquooshRoot makes to the InteractionState's list of transitions.
        while (interactionState.animations.isNotEmpty()) {
            withFrameNanos { frameTimeNanos ->
                val animState = HashMap(animationValues.value)
                for ((id, anim) in animations) {
                    // If we haven't started this animation yet, then start it now.
                    if (anim.startTimeNanos == 0L) {
                        anim.startTimeNanos = frameTimeNanos
                    }

                    val playTimeNanos = frameTimeNanos - anim.startTimeNanos

                    // Compute where it's meant to be, and update the value in animState.
                    val position =
                        anim.animation.getValueFromNanos(playTimeNanos)
                    animState[id] = position

                    // If the animation is complete, then we need to remove it from the transitions
                    // list, and apply it to the base interaction state.
                    if (anim.animation.isFinishedFromNanos(playTimeNanos)) {
                        animState.remove(id)
                        interactionState.squooshCompleteTransition(anim.source)
                    }
                }
                animationValues.value = animState
            }
        }
    }

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
                childRenderSelector,
                // Is there a nicer way of passing these two?
                animations,
                animationValues,
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
            val renderChildComposableDuration = measureTimeMillis {
                for (child in childComposables) {
                    if (child.node.computedLayout == null) {
                        continue
                    }
                    var composableChildModifier = Modifier
                        .drawWithContent {
                            if (child.node == childRenderSelector.selectedRenderChild)
                                drawContent()
                        }
                        .then(SquooshParentData(node = child.node))

                    if (child.component == null && child.content == null) {
                        val interactionModifier = squooshInteractionModifier(
                            doc,
                            interactionState,
                            interactionScope,
                            customizationContext,
                            child
                        )
                        composableChildModifier = composableChildModifier.then(interactionModifier)
                    }

                    Box(modifier = composableChildModifier) {
                        if (child.component != null) {
                            child.component!!(object : ComponentReplacementContext {
                                override val appearanceModifier: Modifier = Modifier
                                override val layoutModifier: Modifier = Modifier
                                @Composable
                                override fun Content() {
                                }

                                override val textStyle: TextStyle? = null
                                override val parentLayout: ParentLayoutInfo? = null
                            })
                        } else if (child.content != null) {
                            Log.d(TAG, "Unimplemented: child.content")
                        }
                    }
                }
            }
            Log.d(TAG, "$docName generate child composables took ${renderChildComposableDuration}ms")
        }
    )
}

internal fun Modifier.squooshRender(
    node: SquooshResolvedNode,
    document: DocContent,
    docName: String,
    customizations: CustomizationContext,
    childRenderSelector: SquooshChildRenderSelector,
    animations: Map<Int, SquooshRenderTransition>,
    animationValues: State<Map<Int, Float>>
): Modifier =
    this.then(
        Modifier.drawWithContent {
            val animValues = animationValues.value
            for ((id, transition) in animations) {
                val animationOffset = animValues[id]
                if (animationOffset == null) {
                    // This happens the first time through, because we manage to render before our
                    // LaunchedEffect has run. It doesn't seem OK to drop a frame at the start of
                    // each animation, so we should figure out how to run the effect immediately.
                    // (Compose's animation code seems to do this, but it's a bit complicated).
                    transition.control.apply(0.0f)
                    continue
                }
                transition.control.apply(animationOffset)
            }

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

                    if (node.needsChildRender) {
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

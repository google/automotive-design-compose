package com.android.designcompose.squoosh

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.android.designcompose.CustomizationContext
import com.android.designcompose.DocContent
import com.android.designcompose.DocServer
import com.android.designcompose.DocumentSwitcher
import com.android.designcompose.InteractionState
import com.android.designcompose.InteractionStateManager
import com.android.designcompose.Jni
import com.android.designcompose.LayoutManager
import com.android.designcompose.ParentComponentInfo
import com.android.designcompose.common.DocumentServerParams
import com.android.designcompose.doc
import com.android.designcompose.getKey
import com.android.designcompose.getMatchingVariant
import com.android.designcompose.mergeStyles
import com.android.designcompose.nodeVariant
import com.android.designcompose.render
import com.android.designcompose.rootNode
import com.android.designcompose.serdegen.Layout
import com.android.designcompose.serdegen.LayoutChangedResponse
import com.android.designcompose.serdegen.NodeQuery
import com.android.designcompose.serdegen.View
import com.android.designcompose.serdegen.ViewData
import com.android.designcompose.serdegen.ViewStyle
import com.android.designcompose.squooshNodeVariant
import com.android.designcompose.squooshRootNode
import com.android.designcompose.squooshShapeRender
import com.android.designcompose.stateForDoc
import com.android.designcompose.width
import com.novi.bincode.BincodeDeserializer
import com.novi.bincode.BincodeSerializer
import kotlin.system.measureTimeMillis

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

    internal fun addOrUpdateNode(
        layoutId: Int,
        parentLayoutId: Int,
        childIndex: Int,
        style: ViewStyle)
    {
        Jni.jniAddStyle(
            layoutId,
            parentLayoutId,
            childIndex,
            serialize(style))
    }

    internal fun removeNode(layoutId: Int) {
        Jni.jniRemoveNode(layoutId, false)
    }

    internal fun keepJniBits() {
        Jni.jniSetNodeSize(0, 0, 0)
        Jni.jniAddTextNode(0, 0, 0, emptyByteArray, false)
        Jni.jniAddNode(
            0,
            0,
            0,
            emptyByteArray,
            emptyByteArray,
            false)
        Jni.jniRemoveNode(0, false)
    }

    internal fun doLayout(): List<Int> {
        val response = Jni.jniComputeLayout() ?: return emptyList()
        val layoutChangedResponse: LayoutChangedResponse = LayoutChangedResponse.deserialize(BincodeDeserializer(response))
        return layoutChangedResponse.changed_layout_ids
    }
    internal fun getComputedLayout(layoutId: Int): Layout? {
        val layoutBytes = Jni.jniGetLayout(layoutId) ?: return null
        val deserializer = BincodeDeserializer(layoutBytes)
        return Layout.deserialize(deserializer)
    }

    private val emptyByteArray = ByteArray(0)
    private fun serialize(v: ViewStyle?): ByteArray {
        if (v == null) {
            return emptyByteArray
        }
        val serializer = BincodeSerializer()
        v.serialize(serializer)
        return serializer._bytes
    }
}

internal class SquooshResolvedNode(
    val view: View,
    val style: ViewStyle,
    var firstChild: SquooshResolvedNode? = null,
    var nextSibling: SquooshResolvedNode? = null,
    var computedLayout: Layout? = null
)

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
    val resolvedView = SquooshResolvedNode(view, style)

    if (view.data is ViewData.Container) {
        val viewData = view.data as ViewData.Container
        var previousChild: SquooshResolvedNode? = null
        for (child in viewData.children) {
            val childResolvedNode = resolveVariantsRecursively(child, document, customizations, interactionState, parentComps)
            if (resolvedView.firstChild == null)
                resolvedView.firstChild = childResolvedNode
            if (previousChild != null)
                previousChild.nextSibling = childResolvedNode
            previousChild = childResolvedNode
        }
    }

    return resolvedView
}

/// Takes a `SquooshResolvedNode` and recursively builds or updates a native layout tree via
/// the `SquooshLayout` wrapper of `JniLayout`.
internal fun updateLayoutTree(
    rootLayoutId: Int,
    resolvedNode: SquooshResolvedNode,
    layoutCache: HashMap<Int, Int>,
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
        SquooshLayout.addOrUpdateNode(
            layoutId = layoutId,
            parentLayoutId = parentLayoutId,
            childIndex = childIndex,
            style = resolvedNode.style
        )
        layoutCache[layoutId] = layoutCacheKey
    }
    // XXX: We might want separate (cheaper) calls to assert the tree structure.
    // XXX XXX: This code doesn't ever update the tree structure.

    var childIdx = 0
    var child = resolvedNode.firstChild
    while (child != null) {
        updateLayoutTree(rootLayoutId, child, layoutCache, layoutId, childIdx)
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
        val updatedLayoutValue = SquooshLayout.getComputedLayout(layoutId)
        if (updatedLayoutValue == null) {
            Log.d(TAG, "Unable to fetch computed layout for ${resolvedNode.view.name} and its children")
            return
        }
        layoutValueCache[layoutId] = updatedLayoutValue
        resolvedNode.computedLayout = updatedLayoutValue
    } else {
        resolvedNode.computedLayout = layoutValue
    }
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

    val density = LocalDensity.current.density
    LaunchedEffect(density) { LayoutManager.setDensity(density) }

    val variantParentName = when (rootNodeQuery) {
        is NodeQuery.NodeVariant -> rootNodeQuery.field1
        else -> ""
    }

    val rootLayoutId = remember { LayoutManager.getNextLayoutId() * 1000000 }
    val layoutCache = remember { HashMap<Int, Int>() }
    val layoutValueCache = remember { HashMap<Int, Layout>() }

    // Ok, now we have done the dull stuff, we need to build a tree applying
    // the correct variants etc and then build/update the tree. How do we know
    // what's different from last time? Does the Rust side track

    // I want to only call the addOrUpdateNode function when the view has actually
    // changed. That means I need to know if I'm visiting the same view as before.
    // I can assign layout ids sequentially.

    var root: SquooshResolvedNode? = null
    val resolveVariantsTime = measureTimeMillis {
        root = resolveVariantsRecursively(
            startFrame,
            doc,
            customizationContext,
            interactionState,
            emptyList(),
            variantParentName
        )
    }
    val buildLayoutTreeTime = measureTimeMillis {
        updateLayoutTree(rootLayoutId, root!!, layoutCache)
    }
    val performLayoutTime = measureTimeMillis {
        val invalidatedLayoutIds = SquooshLayout.doLayout()
        for (invalidatedLayoutId in invalidatedLayoutIds) {
            layoutValueCache.remove(invalidatedLayoutId)
        }
        Log.d(TAG, "$docName layout invalidated ${invalidatedLayoutIds.size} nodes")
    }
    val populateLayoutTime = measureTimeMillis {
        populateComputedLayout(rootLayoutId, root!!, layoutValueCache)
    }

    Log.d(TAG, "$docName resolveVariants: $resolveVariantsTime ms, buildLayout: $buildLayoutTreeTime ms, eval. layout $performLayoutTime ms, populate layout values: $populateLayoutTime ms")

    // Now our tree of resolved nodes is ready to use!

    if (root != null && root!!.computedLayout != null) {
        Log.d(TAG, "$docName root size: ${root!!.computedLayout!!.width}x${root!!.computedLayout!!.height}")
    }
    // Ok, good enough, we can make a composable that will:
    //  1. occupy the space of the root,
    //  2. place any child composables
    //  3. draw the content

    // We will also need a sort of "draw/interact" child that accepts derivative
    // layout

    Box(
        Modifier.size(
            width = root!!.computedLayout!!.width.dp,
            height = root!!.computedLayout!!.height.dp
        ).squooshRender(root!!, doc, customizationContext)
    )
}

internal fun Modifier.squooshRender(
    node: SquooshResolvedNode,
    document: DocContent,
    customizations: CustomizationContext
): Modifier =
    this.then(
        Modifier.drawWithContent {
            // no masking yet :(
            var frameRenderCount = 0
            val renderTime = measureTimeMillis {
                fun renderNode(node: SquooshResolvedNode) {
                    val computedLayout = node.computedLayout ?: return
                    val shape = when (node.view.data) {
                        is ViewData.Container -> (node.view.data as ViewData.Container).shape
                        else -> return
                    }
                    squooshShapeRender(
                        drawContext,
                        density,
                        Size(computedLayout.width * density, computedLayout.height * density),
                        node.style,
                        shape,
                        null, // customImageWithContext
                        document,
                        node.view.name,
                        customizations
                    ) {
                        var child = node.firstChild
                        while (child != null) {
                            val childLayout = child.computedLayout
                            if (childLayout != null) {
                                translate(childLayout.left * density, childLayout.top * density) {
                                    renderNode(child!!)
                                }
                            }
                            child = child.nextSibling
                        }
                    }
                    frameRenderCount++
                }
                renderNode(node)
            }
            Log.d(TAG, "render $frameRenderCount frames only recursively $renderTime ms")
        }
    )


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

package com.android.designcompose.testapp.validation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.designcompose.DesignSettings
import com.android.designcompose.testapp.common.interFont
import kotlin.math.roundToInt

// Surface the layout data to our parent container.
private class DesignChildData(val name: String, val layoutId: Int) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = this@DesignChildData

    override fun hashCode(): Int = layoutId // style.hashCode()

    override fun toString(): String = "DesignChildData($name, $layoutId)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? DesignChildData ?: return false
        return name == otherModifier.name && layoutId == otherModifier.layoutId
    }
}

private val Measurable.designChildData: DesignChildData?
    get() = parentData as? DesignChildData
private val Placeable.designChildData: DesignChildData?
    get() = parentData as? DesignChildData

internal fun Modifier.layoutStyle(name: String, layoutId: Int) =
    this.then(DesignChildData(name, layoutId))

data class LO(val width: Float, val height: Float, val top: Float, val left: Float)

fun EmptyLO(): LO {
    return LO(0F, 0F, 0F, 0F)
}

internal object LayoutMgr {
    private val data: HashMap<String, LO> = HashMap()
    private val layoutToName: HashMap<Int, String> = HashMap()
    private val subscribers: HashMap<Int, (Int) -> Unit> = HashMap()
    private var layoutState: Int = 0
    private var nextId: Int = 0
    private var greenState: Boolean = false

    init {
        data["stage"] = LO(500F, 500F, 0F, 0F)
        data["rect1"] = LO(100F, 100F, 20F, 20F)
        data["rect2"] = LO(150F, 150F, 140F, 20F)
        data["rect2-0"] = LO(50F, 50F, 10F, 120F)

        data["stage2"] = LO(600F, 400F, 0F, 0F)
        data["rect3"] = LO(200F, 200F, 50F, 50F)
        data["rect4"] = LO(200F, 200F, 100F, 300F)
    }

    fun getNextId(): Int {
        ++nextId
        return nextId
    }

    fun get(name: String): LO {
        return if (data.containsKey(name)) data[name]!! else LO(0F, 0F, 0F, 0F)
    }

    fun get(layoutId: Int): LO {
        val name = layoutToName[layoutId]
        if (name != null) {
            return if (data.containsKey(name)) data[name]!! else EmptyLO()
        }
        return EmptyLO()
    }

    fun subscribe(layoutId: Int, name: String, setLayoutState: (Int) -> Unit) {
        println("Subscribe $name $layoutId")
        layoutToName[layoutId] = name
        subscribers[layoutId] = setLayoutState
    }

    fun unsubscribe(layoutId: Int) {
        println("Unsubscribe $layoutId")
        layoutToName.remove(layoutId)
        subscribers.remove(layoutId)
    }

    fun notifySubscribers() {
        subscribers.values.forEach { it(layoutState) }
    }

    fun toggleGreen() {
        greenState = !greenState
        if (greenState) data["rect2-0"] = LO(50F, 50F, 10F, 120F)
        else data["rect2-0"] = LO(30F, 30F, 30F, 30F)
        ++layoutState
        notifySubscribers()
    }

    fun computeLayout() {}
}

internal object ChildrenMgr {
    fun get(name: String): List<String> {
        return if (name == "stage") arrayListOf("rect1", "rect2")
        else if (name == "stage2") arrayListOf("rect3", "rect4")
        else if (name == "rect2") arrayListOf("rect2-0") else arrayListOf()
    }
}

internal object ColorMgr {
    private val data: HashMap<String, Color> = HashMap()

    init {
        data["stage"] = Color.Black
        data["stage2"] = Color.Black
        data["rect1"] = Color.Blue
        data["rect2"] = Color.Red
        data["rect2-0"] = Color.Green
        data["rect3"] = Color.Cyan
        data["rect4"] = Color.Magenta
    }

    fun get(name: String): Color {
        return if (data.containsKey(name)) data[name]!! else Color.Yellow
    }
}

internal fun Modifier.myRender(name: String, color: Color): Modifier =
    this.then(
        Modifier.drawWithContent {
            println("Render $name ${size.width} x ${size.height}")
            drawContext.canvas.save()

            val paint = Paint()
            paint.style = PaintingStyle.Stroke
            paint.strokeWidth = 2F
            val brush = SolidColor(color)
            brush.applyTo(size, paint, 1F)

            val path = Path()
            path.addRect(Rect(0.0f, 0.0f, size.width, size.height))

            drawContext.canvas.drawPath(path, paint)
            drawContent()

            drawContext.canvas.restore()
        }
    )

@Composable
fun MyFrame(modifier: Modifier = Modifier, name: String, space: String = "", showRect1: Boolean) {
    val layoutId = remember { LayoutMgr.getNextId() }
    val (layoutState, setLayoutState) = remember { mutableStateOf(0) }

    println("$space MyFrame $name layoutId $layoutId layoutState $layoutState")
    if (!showRect1 && name == "rect1") return

    DisposableEffect(name) {
        LayoutMgr.subscribe(layoutId, name, setLayoutState)
        onDispose { LayoutMgr.unsubscribe(layoutId) }
    }

    val myContent =
        @Composable {
            Box(Modifier)
            val children = ChildrenMgr.get(name)
            children.forEach { MyFrame(Modifier, it, "$space  ", showRect1) }
        }

    val color = ColorMgr.get(name)
    val renderModifier = Modifier.myRender(name, color)
    val layoutModifier = Modifier.layoutStyle(name, layoutId)

    DesignFrameLayout(
        modifier.then(renderModifier).then(layoutModifier),
        name,
        layoutId,
        layoutState,
        space,
        myContent,
    )
}

@Composable
internal inline fun DesignFrameLayout(
    modifier: Modifier,
    name: String,
    layoutId: Int,
    layoutState: Int,
    space: String,
    content: @Composable () -> Unit,
) {
    val measurePolicy = remember(layoutState) { designMeasurePolicy(name, layoutId, space) }
    Layout(content = content, measurePolicy = measurePolicy, modifier = modifier)
}

internal fun designMeasurePolicy(name: String, layoutId: Int, space: String) =
    MeasurePolicy { measurables, constraints ->
        println("$space START Layout $name")

        val placeables =
            measurables.mapIndexed { index, measurable ->
                val layoutData = measurable.designChildData
                println("$space Measure $name Child $index layoutData $layoutData")
                measurable.measure(constraints)
            }

        val myLayout = LayoutMgr.get(layoutId)
        val result =
            layout(myLayout.width.roundToInt(), myLayout.height.roundToInt()) {
                // Place children in the parent layout
                placeables.forEachIndexed { index, placeable ->
                    val layoutData = placeable.designChildData
                    if (index == 0) {
                        println(
                            "$space Place $name layoutData ${layoutData?.name}--${layoutData?.layoutId} Child $index: ${myLayout.left}, ${myLayout.top}"
                        )
                        placeable.placeRelative(
                            x = myLayout.left.roundToInt(),
                            y = myLayout.top.roundToInt(),
                        )
                    } else {
                        val childLayout = LayoutMgr.get(layoutData?.layoutId ?: -1)
                        println(
                            "$space Place $name layoutData ${layoutData?.name}--${layoutData?.layoutId} Child $index: ${childLayout.left}, ${childLayout.top}"
                        )
                        placeable.place(
                            x = childLayout.left.roundToInt(),
                            y = childLayout.top.roundToInt(),
                        )
                    }
                }
            }
        println("$space END Layout $name")
        result
    }

@Composable
internal fun Button(name: String, selected: Boolean, select: () -> Unit) {
    val textColor = if (selected) Color.Black else Color.Gray
    val borderColor = if (selected) Color.Black else Color.Gray
    var modifier =
        Modifier.padding(10.dp)
            .clickable { select() }
            .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .absolutePadding(10.dp, 2.dp, 10.dp, 2.dp)

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Text(name, fontSize = 30.sp, color = textColor)
    }
}

@Composable
internal fun TestButton(name: String, tag: String, selected: Boolean, select: () -> Unit) {
    val textColor = if (selected) Color.Black else Color.Gray
    val borderColor = if (selected) Color.Black else Color.Gray
    var modifier =
        Modifier.padding(10.dp)
            .clickable { select() }
            .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .absolutePadding(10.dp, 2.dp, 10.dp, 2.dp)
            .testTag(tag)

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Text(name, fontSize = 30.sp, color = textColor)
    }
}

@Composable
fun LayoutComposable() {
    val (showRect1, setShowRect1) = remember { mutableStateOf(true) }
    val (stage, setStage) = remember { mutableStateOf("stage") }

    Column {
        MyFrame(Modifier, stage, "", showRect1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Visibility", fontSize = 30.sp, color = Color.Black)
            Button("Rect1", false) { setShowRect1(!showRect1) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Toggle Green", fontSize = 30.sp, color = Color.Black)
            Button("Toggle", false) { LayoutMgr.toggleGreen() }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Stage", fontSize = 30.sp, color = Color.Black)
            Button("Stage", false) { setStage("stage") }
            Button("Stage2", false) { setStage("stage2") }
        }
    }
}

// Main Activity class. Setup auth token and font, then build the UI with buttons for each test
// on the left and the currently selected test on the right.
class LayoutTest : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DesignSettings.addFontFamily("Inter", interFont)
        DesignSettings.enableLiveUpdates(this)

        setContent { LayoutComposable() }
    }
}

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

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.ContentReplacementContext
import com.android.designcompose.DesignComposeCallbacks
import com.android.designcompose.DesignSettings
import com.android.designcompose.GetDesignNodeData
import com.android.designcompose.ImageReplacementContext
import com.android.designcompose.LazyContentSpan
import com.android.designcompose.ListContent
import com.android.designcompose.ListContentData
import com.android.designcompose.Meter
import com.android.designcompose.OpenLinkCallback
import com.android.designcompose.ParentLayoutInfo
import com.android.designcompose.ReplacementContent
import com.android.designcompose.TapCallback
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignContentTypes
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignKeyAction
import com.android.designcompose.annotation.DesignMetaKey
import com.android.designcompose.annotation.DesignPreviewContent
import com.android.designcompose.annotation.DesignVariant
import com.android.designcompose.annotation.PreviewNode
import com.android.designcompose.widgetParent
import java.lang.Float.max
import java.lang.Float.min
import kotlin.math.roundToInt

val interFont =
    FontFamily(
        Font(R.font.inter_black, FontWeight.Black),
        Font(R.font.inter_blackitalic, FontWeight.Black, FontStyle.Italic),
        Font(R.font.inter_bold, FontWeight.Bold),
        Font(R.font.inter_bolditalic, FontWeight.Bold, FontStyle.Italic),
        Font(R.font.inter_extrabold, FontWeight.ExtraBold),
        Font(R.font.inter_extrabolditalic, FontWeight.ExtraBold, FontStyle.Italic),
        Font(R.font.inter_extralight, FontWeight.ExtraLight),
        Font(R.font.inter_extralightitalic, FontWeight.ExtraLight, FontStyle.Italic),
        Font(R.font.inter_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_mediumitalic, FontWeight.Medium, FontStyle.Italic),
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_semibold, FontWeight.SemiBold),
        Font(R.font.inter_semibolditalic, FontWeight.SemiBold, FontStyle.Italic),
        Font(R.font.inter_thin, FontWeight.Thin),
        Font(R.font.inter_thinitalic, FontWeight.Thin, FontStyle.Italic),
    )

const val TAG = "DesignCompose"

val EXAMPLES: ArrayList<Triple<String, @Composable () -> Unit, String?>> =
    arrayListOf(
        Triple("Hello", { HelloWorld() }, HelloWorldDoc.javaClass.name),
        Triple("Image Update", { ImageUpdateTest() }, ImageUpdateTestDoc.javaClass.name),
        Triple("Telltales", { TelltaleTest() }, TelltaleTestDoc.javaClass.name),
        Triple("OpenLink", { OpenLinkTest() }, OpenLinkTestDoc.javaClass.name),
        Triple("Variant *", { VariantAsteriskTest() }, VariantAsteriskTestDoc.javaClass.name),
        Triple("Alignment", { AlignmentTest() }, AlignmentTestDoc.javaClass.name),
        Triple("Battleship", { BattleshipTest() }, BattleshipDoc.javaClass.name),
        Triple("H Constraints", { HConstraintsTest() }, ConstraintsDoc.javaClass.name),
        Triple("V Constraints", { VConstraintsTest() }, ConstraintsDoc.javaClass.name),
        Triple("Interaction", { InteractionTest() }, InteractionTestDoc.javaClass.name),
        Triple("Shadows", { ShadowsTest() }, ShadowsTestDoc.javaClass.name),
        Triple("Item Spacing", { ItemSpacingTest() }, ItemSpacingTestDoc.javaClass.name),
        Triple(
            "Recurse Customization",
            { RecursiveCustomizations() },
            RecursiveCustomizationsDoc.javaClass.name
        ),
        Triple("Color Tint", { ColorTintTest() }, ColorTintTestDoc.javaClass.name),
        Triple(
            "Variant Properties",
            { VariantPropertiesTest() },
            VariantPropertiesTestDoc.javaClass.name
        ),
        // Lazy Grid doesn't actually use a doc
        Triple("Lazy Grid", { LazyGridItemSpans() }, null),
        Triple("Grid Layout", { GridLayoutTest() }, GridLayoutTestDoc.javaClass.name),
        Triple("Grid Widget", { GridWidgetTest() }, GridWidgetTestDoc.javaClass.name),
        Triple("List Widget", { ListWidgetTest() }, ListWidgetTestDoc.javaClass.name),
        Triple("1px Separator", { OnePxSeparatorTest() }, OnePxSeparatorDoc.javaClass.name),
        Triple(
            "Variant Interactions",
            { VariantInteractionsTest() },
            VariantInteractionsTestDoc.javaClass.name
        ),
        Triple(
            "Layout Replacement",
            { LayoutReplacementTest() },
            LayoutReplacementTestDoc.javaClass.name
        ),
        Triple("Text Elide", { TextElideTest() }, TextElideTestDoc.javaClass.name),
        Triple("Fancy Fills", { FancyFillTest() }, FancyFillTestDoc.javaClass.name),
        Triple("Fill Container", { FillTest() }, FillTestDoc.javaClass.name),
        Triple("CrossAxis Fill", { CrossAxisFillTest() }, CrossAxisFillTestDoc.javaClass.name),
        Triple(
            "Grid Layout Documentation",
            { GridLayoutDocumentation() },
            GridLayoutDoc.javaClass.name
        ),
        Triple("Blend Modes", { BlendModeTest() }, BlendModeTestDoc.javaClass.name),
        Triple(
            "Vector Rendering",
            { VectorRenderingTest() },
            VectorRenderingTestDoc.javaClass.name
        ),
        Triple("Dials Gauges", { DialsGaugesTest() }, DialsGaugesTestDoc.javaClass.name),
        Triple("Masks", { MaskTest() }, MaskTestDoc.javaClass.name),
        Triple("Variable Borders", { VariableBorderTest() }, VariableBorderTestDoc.javaClass.name),
        Triple("Layout Tests", { LayoutTests() }, LayoutTestsDoc.javaClass.name)
    )

// TEST Basic Hello World example
@DesignDoc(id = "pxVlixodJqZL95zo2RzTHl")
interface HelloWorld {
    @DesignComponent(node = "#MainFrame") fun Main(@Design(node = "#Name") name: String)
}

@Composable
fun HelloWorld() {
    HelloWorldDoc.Main(
        name = "World",
        designComposeCallbacks =
            DesignComposeCallbacks(
                docReadyCallback = { id ->
                    Log.i("DesignCompose", "HelloWorld Ready: doc ID = $id")
                },
                newDocDataCallback = { docId, data ->
                    Log.i(
                        "DesignCompose",
                        "HelloWorld Updated doc ID $docId: ${data?.size ?: 0} bytes"
                    )
                },
            )
    )
}

// TEST Image Update Test. After this loads, rename #Stage in the Figma doc. After the app
// updates,
// rename it back to #Stage. The image should reload correctly.
@DesignDoc(id = "oQw7kiy94fvdVouCYBC9T0")
interface ImageUpdateTest {
    @DesignComponent(node = "#Stage") fun Main() {}
}

@Composable
fun ImageUpdateTest() {
    ImageUpdateTestDoc.Main()
}

// TEST Telltale Test. Tests that rendering telltales as frames and as components is correct.
// When visibility is set to true, the telltales rendered in the app should match the #Main frame
// in the Figma document.
@DesignDoc(id = "TZgHrKWx8wvQM7UPTyEpmz")
interface TelltaleTest {
    @DesignComponent(node = "#Main")
    fun Main(
        @Design(node = "#left_f") leftFrame: Boolean,
        @Design(node = "#seat_f") seatFrame: Boolean,
        @Design(node = "#left_i") leftInstance: Boolean,
        @Design(node = "#seat_i") seatInstance: Boolean,
        @Design(node = "#low_i") lowInstance: Boolean,
        @Design(node = "#brights_i") brightsInstance: Boolean,
    )
}

@Composable
fun TelltaleTest() {
    TelltaleTestDoc.Main(
        leftFrame = true,
        seatFrame = true,
        leftInstance = true,
        seatInstance = true,
        lowInstance = true,
        brightsInstance = true
    )
}

// TEST Open Link Test. Tests that the open link interaction works on frames and components. Tap
// the squares and watch the output. Tapping the "Swap" button should change the behavior of the
// taps.
enum class SquareColor {
    Red,
    Green,
    Blue
}

enum class SquareShadow {
    On,
    Off
}

@DesignDoc(id = "r7m4tqyKv6y9DWcg7QBEDf")
interface OpenLinkTest {
    @DesignComponent(node = "#MainFrame")
    fun MainFrame(
        @Design(node = "#Name") name: String,
        @Design(node = "#Content") content: ReplacementContent,
        @Design(node = "#Swap") clickSwap: Modifier,
    )

    @DesignComponent(node = "#Red") fun Red() {}

    @DesignComponent(node = "#Green") fun Green() {}

    @DesignComponent(node = "#Blue") fun Blue() {}

    @DesignComponent(node = "#PurpleCircle") fun PurpleCircle() {}

    @DesignComponent(node = "#SquareShadow")
    fun Square(
        @DesignVariant(property = "#SquareShadow") shadow: SquareShadow,
        @DesignVariant(property = "#SquareColor") color: SquareColor,
        @Design(node = "#icon") icon: ReplacementContent,
    )
}

@Composable
fun OpenLinkTest() {
    val openLinkOne = OpenLinkCallback { url -> Log.i("DesignCompose", "Open Link ONE: $url") }
    val openLinkTwo = OpenLinkCallback { url -> Log.i("DesignCompose", "Open Link TWO: $url") }
    val (useFuncOne, setUseFuncOne) = remember { mutableStateOf(true) }
    val openLinkFunc =
        if (useFuncOne) {
            openLinkOne
        } else {
            openLinkTwo
        }

    OpenLinkTestDoc.MainFrame(
        name = "Rob",
        openLinkCallback = openLinkFunc,
        content =
            ReplacementContent(
                count = 5,
                content = { index ->
                    { rc ->
                        when (index) {
                            0 ->
                                OpenLinkTestDoc.Red(
                                    parentLayout = ParentLayoutInfo(rc.parentLayoutId, index)
                                )
                            1 ->
                                OpenLinkTestDoc.Green(
                                    parentLayout = ParentLayoutInfo(rc.parentLayoutId, index)
                                )
                            2 ->
                                OpenLinkTestDoc.Blue(
                                    parentLayout = ParentLayoutInfo(rc.parentLayoutId, index)
                                )
                            3 ->
                                OpenLinkTestDoc.Square(
                                    shadow = SquareShadow.On,
                                    color = SquareColor.Green,
                                    icon =
                                        ReplacementContent(
                                            count = 1,
                                            content = { i ->
                                                { irc ->
                                                    OpenLinkTestDoc.PurpleCircle(
                                                        parentLayout =
                                                            ParentLayoutInfo(irc.parentLayoutId, i)
                                                    )
                                                }
                                            }
                                        ),
                                    parentLayout = ParentLayoutInfo(rc.parentLayoutId, index)
                                )
                            else ->
                                OpenLinkTestDoc.Square(
                                    shadow = SquareShadow.On,
                                    color = SquareColor.Red,
                                    icon =
                                        ReplacementContent(
                                            count = 1,
                                            content = { i ->
                                                { irc ->
                                                    OpenLinkTestDoc.PurpleCircle(
                                                        parentLayout =
                                                            ParentLayoutInfo(irc.parentLayoutId, i)
                                                    )
                                                }
                                            }
                                        ),
                                    parentLayout = ParentLayoutInfo(rc.parentLayoutId, index)
                                )
                        }
                    }
                }
            ),
        clickSwap = Modifier.clickable { setUseFuncOne(!useFuncOne) }
    )
}

// TEST Variant Asterisk Test. Tests the @DesignVariant annotation and the ability to match
// variant nodes whose property names and values match the current state passed in.
// The Figma doc contains a sparse component that has three variant properties. The component is
// setup with several wildcard variant names. This example lets you toggle between different values
// for the three properties, and when a node name with those specific values cannot be found, it
// will look for wildcard variants names and use those instead.

enum class PrndState {
    P,
    R,
    N,
    D
}

enum class ChargingState {
    off,
    on
}

enum class RegenState {
    off,
    on
}

data class Controls(
    val shiftState: MutableState<PrndState>,
    val charging: MutableState<ChargingState>,
    val regen: MutableState<RegenState>,
)

@DesignDoc(id = "gQeYHGCSaBE4zYSFpBrhre")
interface VariantAsteriskTest {
    @DesignComponent(node = "#Main")
    fun Main(
        @DesignVariant(property = "prnd") prnd: PrndState,
        @DesignVariant(property = "charging") charging: ChargingState,
        @DesignVariant(property = "regen") regen: RegenState,
    )
}

@Composable
fun VariantAsteriskTest() {
    val shiftState = remember { mutableStateOf(PrndState.P) }
    val charging = remember { mutableStateOf(ChargingState.off) }
    val regen = remember { mutableStateOf(RegenState.off) }
    val controls = remember { Controls(shiftState, charging, regen) }

    Controls(controls)
    VariantAsteriskTestDoc.Main(
        Modifier.absoluteOffset(x = 10.dp, y = 10.dp),
        prnd = shiftState.value,
        charging = charging.value,
        regen = regen.value
    )
}

@Composable
private fun Button(name: String, selected: Boolean, select: () -> Unit) {
    val textColor = if (selected) Color.Black else Color.Gray
    val borderColor = if (selected) Color.Black else Color.Gray
    var modifier =
        Modifier.padding(10.dp)
            .clickable { select() }
            .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .absolutePadding(10.dp, 2.dp, 10.dp, 2.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Text(name, fontSize = 30.sp, color = textColor)
    }
}

@Composable
private fun Controls(controls: Controls) {
    Box(Modifier.absoluteOffset(x = 10.dp, y = 300.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Shift State:   ", fontSize = 30.sp, color = Color.Black)
                Button("P", controls.shiftState.value == PrndState.P) {
                    controls.shiftState.value = PrndState.P
                }
                Button("R", controls.shiftState.value == PrndState.R) {
                    controls.shiftState.value = PrndState.R
                }
                Button("N", controls.shiftState.value == PrndState.N) {
                    controls.shiftState.value = PrndState.N
                }
                Button("D", controls.shiftState.value == PrndState.D) {
                    controls.shiftState.value = PrndState.D
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Charging:   ", fontSize = 30.sp, color = Color.Black)
                Button("On", controls.charging.value == ChargingState.on) {
                    controls.charging.value = ChargingState.on
                }
                Button("Off", controls.charging.value == ChargingState.off) {
                    controls.charging.value = ChargingState.off
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Regen:   ", fontSize = 30.sp, color = Color.Black)
                Button("On", controls.regen.value == RegenState.on) {
                    controls.regen.value = RegenState.on
                }
                Button("Off", controls.regen.value == RegenState.off) {
                    controls.regen.value = RegenState.off
                }
            }
        }
    }
}

// TEST Alignment Test. Observe that the app rendering is identital to the Figma doc
@DesignDoc(id = "JIjE9oKQbq8ipi66ab5UaK")
interface AlignmentTest {
    @DesignComponent(node = "#Test")
    fun AlignmentTestFrame(
        @Design(node = "Frame 1") click: Modifier,
        @Design(node = "Name") text: String,
    )
}

@Composable
fun AlignmentTest() {
    AlignmentTestDoc.AlignmentTestFrame(Modifier, click = Modifier, text = "Hello")
}

// TEST Battleship
@DesignDoc(id = "RfGl9SWnBEvdg8T1Ex6ZAR")
interface Battleship {
    @DesignComponent(node = "Start Board") fun MainFrame()
}

@Composable
fun BattleshipTest() {
    BattleshipDoc.MainFrame(Modifier)
}

// TEST Constraints
@DesignDoc(id = "KuHLbsKA23DjZPhhgHqt71")
interface Constraints {
    @DesignComponent(node = "#Horizontal") fun HorizontalFrame()

    @DesignComponent(node = "#Vertical") fun VerticalFrame()
}

@Preview
@Composable
fun HConstraintsTest() {
    ConstraintsDoc.HorizontalFrame(Modifier.fillMaxSize())
}

@Preview
@Composable
fun VConstraintsTest() {
    ConstraintsDoc.VerticalFrame(Modifier.fillMaxSize())
}

// TEST Interactions
@DesignDoc(id = "8Zg9viyjYTnyN29pbkR1CE")
interface InteractionTest {
    @DesignComponent(node = "Start Here")
    fun MainFrame(
        @Design(node = "#KeyButtonB") onTapKeyButtonB: TapCallback,
        @Design(node = "#KeyButtonC") onTapKeyButtonC: TapCallback,
        @Design(node = "#KeyInjectA") onTapInjectA: TapCallback,
        @Design(node = "#KeyInjectB") onTapInjectB: TapCallback,
        @Design(node = "#KeyInjectC") onTapInjectC: TapCallback,
        @Design(node = "#KeyInjectAB") onTapInjectAB: TapCallback,
        @Design(node = "#KeyInjectBC") onTapInjectBC: TapCallback,
    )

    // Inject a ctrl-shift-B key when the 'clickedB()' function is called
    @DesignKeyAction(key = 'B', metaKeys = [DesignMetaKey.MetaShift, DesignMetaKey.MetaCtrl])
    fun clickedShiftCtrlB()
    // Inject a meta-C key when the 'clickedC()' function is called
    @DesignKeyAction(key = 'C', metaKeys = [DesignMetaKey.MetaMeta]) fun clickedMetaC()
    @DesignKeyAction(key = 'A', metaKeys = []) fun clickedA()
    @DesignKeyAction(key = 'B', metaKeys = []) fun clickedB()
    @DesignKeyAction(key = 'C', metaKeys = []) fun clickedC()
}

@Preview
@Composable
fun InteractionTest() {
    InteractionTestDoc.MainFrame(
        onTapKeyButtonB = { InteractionTestDoc.clickedShiftCtrlB() },
        onTapKeyButtonC = { InteractionTestDoc.clickedMetaC() },
        onTapInjectA = { InteractionTestDoc.clickedA() },
        onTapInjectB = { InteractionTestDoc.clickedB() },
        onTapInjectC = { InteractionTestDoc.clickedC() },
        onTapInjectAB = {
            InteractionTestDoc.clickedA()
            InteractionTestDoc.clickedB()
        },
        onTapInjectBC = {
            InteractionTestDoc.clickedB()
            InteractionTestDoc.clickedC()
        },
    )
}

// TEST Shadows
@DesignDoc(id = "OqK58Y46IqP4wIgKCWys48")
interface ShadowsTest {
    @DesignComponent(node = "#Root") fun MainFrame()
}

@Preview
@Composable
fun ShadowsTest() {
    ShadowsTestDoc.MainFrame()
}

// TEST Item Spacing
@DesignDoc(id = "YXrHBp6C6OaW5ShcCYeGJc")
interface ItemSpacingTest {
    @DesignComponent(node = "#Main")
    fun MainFrame(
        @Design(node = "#HorizontalCustom") horizontalItems: ReplacementContent,
        @Design(node = "#VerticalCustom") verticalItems: ReplacementContent,
    )

    @DesignComponent(node = "#Square") fun Square()
}

@Preview
@Composable
fun ItemSpacingTest() {
    ItemSpacingTestDoc.MainFrame(
        horizontalItems =
            ReplacementContent(
                count = 3,
                content = { index ->
                    { rc ->
                        ItemSpacingTestDoc.Square(
                            parentLayout = ParentLayoutInfo(rc.parentLayoutId, index)
                        )
                    }
                }
            ),
        verticalItems =
            ReplacementContent(
                count = 3,
                content = { index ->
                    { rc ->
                        ItemSpacingTestDoc.Square(
                            parentLayout = ParentLayoutInfo(rc.parentLayoutId, index)
                        )
                    }
                }
            )
    )
}

// TEST Custom content children
@DesignDoc(id = "o0GWzcqdOWEgzj4kIeIlAu")
interface RecursiveCustomizations {
    @DesignComponent(node = "#MainFrame")
    fun MainFrame(
        @Design(node = "#Name") name: String,
        @Design(node = "#ChildFrame") child: ReplacementContent,
        @Design(node = "#Content") content: ReplacementContent,
    )

    @DesignComponent(node = "#NameFrame") fun NameFrame()

    @DesignComponent(node = "#TitleFrame")
    fun TitleFrame(
        @Design(node = "#Name") title: String,
    )
}

@Composable
fun RecursiveCustomizations() {
    RecursiveCustomizationsDoc.MainFrame(
        name = "Google",
        child =
            ReplacementContent(
                count = 1,
                content = {
                    { replacementContext ->
                        RecursiveCustomizationsDoc.NameFrame(
                            parentLayout = ParentLayoutInfo(replacementContext.parentLayoutId, 0)
                        )
                    }
                }
            ),
        content =
            ReplacementContent(
                count = 3,
                content = { index ->
                    { replacementContext ->
                        when (index) {
                            0 ->
                                RecursiveCustomizationsDoc.TitleFrame(
                                    parentLayout =
                                        ParentLayoutInfo(replacementContext.parentLayoutId, 0),
                                    title = "First"
                                )
                            1 ->
                                RecursiveCustomizationsDoc.TitleFrame(
                                    parentLayout =
                                        ParentLayoutInfo(replacementContext.parentLayoutId, 1),
                                    title = "Second"
                                )
                            else ->
                                RecursiveCustomizationsDoc.TitleFrame(
                                    parentLayout =
                                        ParentLayoutInfo(replacementContext.parentLayoutId, 2),
                                    title = "Third"
                                )
                        }
                    }
                }
            )
    )
}

// TEST Color tint test
@DesignDoc(id = "MCtUD3yjONxK6rQm65yqM5")
interface ColorTintTest {
    @DesignComponent(node = "#MainFrame")
    fun MainFrame(
        @Design(node = "#Square")
        squareReplacement: @Composable (ImageReplacementContext) -> Bitmap?,
    )
}

@Composable
fun ColorTintTest() {
    ColorTintTestDoc.MainFrame(
        squareReplacement = { context ->
            val color =
                ((context.imageContext.getBackgroundColor() ?: Color.Red.toArgb()) * 0.5).toInt()
            val width = context.imageContext.getPixelWidth() ?: 50
            val height = context.imageContext.getPixelHeight() ?: 50
            val colors = IntArray(width * height)
            for (i in 0 until width * height) {
                colors[i] = color
            }
            Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888)
        }
    )
}

enum class SquareBorder {
    Sharp,
    Curved
}

enum class Shape {
    Circle,
    Square,
}

// TEST Variant Extra Properties Test
// This tests that even though the Figma doc has four variant properties for the component named
// #SquareBorder, we can only use two in the code and pick a variant that matches the two.

@DesignDoc(id = "4P7zDdrQxj7FZsKJoIQcx1")
interface VariantPropertiesTest {
    @DesignComponent(node = "#MainFrame")
    fun MainFrame(
        @Design(node = "#Square1") square1: @Composable (ComponentReplacementContext) -> Unit,
        @Design(node = "#Square2") square2: @Composable (ComponentReplacementContext) -> Unit,
        @Design(node = "#Square3") square3: @Composable (ComponentReplacementContext) -> Unit,
        @Design(node = "#Square4") square4: @Composable (ComponentReplacementContext) -> Unit,
        @DesignVariant(property = "#bg1") bg1: Shape,
        @DesignVariant(property = "#bg2") bg2: Shape,
        @DesignVariant(property = "#SquareBorder") type: SquareBorder,
        @DesignVariant(property = "#SquareColor") color: SquareColor,
    )

    @DesignComponent(node = "#SquareBorder")
    fun Square(
        @DesignVariant(property = "#SquareBorder") type: SquareBorder,
        @DesignVariant(property = "#SquareColor") color: SquareColor,
    )
}

@Composable
fun VariantPropertiesTest() {
    val (bg1, setBg1) = remember { mutableStateOf(Shape.Circle) }
    val (bg2, setBg2) = remember { mutableStateOf(Shape.Circle) }
    val (borderType, setBorderType) = remember { mutableStateOf(SquareBorder.Sharp) }
    val (color, setColor) = remember { mutableStateOf(SquareColor.Green) }

    VariantPropertiesTestDoc.MainFrame(
        square1 = {
            VariantPropertiesTestDoc.Square(
                modifier = it.layoutModifier.then(it.appearanceModifier),
                parentLayout = it.parentLayout,
                type = SquareBorder.Sharp,
                color = SquareColor.Blue
            )
        },
        square2 = {
            VariantPropertiesTestDoc.Square(
                modifier = it.layoutModifier.then(it.appearanceModifier),
                parentLayout = it.parentLayout,
                type = SquareBorder.Sharp,
                color = SquareColor.Green
            )
        },
        square3 = {
            VariantPropertiesTestDoc.Square(
                modifier = it.layoutModifier.then(it.appearanceModifier),
                parentLayout = it.parentLayout,
                type = SquareBorder.Curved,
                color = SquareColor.Blue
            )
        },
        square4 = {
            VariantPropertiesTestDoc.Square(
                modifier = it.layoutModifier.then(it.appearanceModifier),
                parentLayout = it.parentLayout,
                type = SquareBorder.Curved,
                color = SquareColor.Green
            )
        },
        bg1 = bg1,
        bg2 = bg2,
        type = borderType,
        color = color,
    )

    Column(modifier = Modifier.absoluteOffset(x = 20.dp, y = 600.dp)) {
        Row {
            Text("Background 1 ", fontSize = 30.sp, color = Color.Black)
            Button("Square", bg1 == Shape.Square) { setBg1(Shape.Square) }
            Button("Circle", bg1 == Shape.Circle) { setBg1(Shape.Circle) }
        }
        Row {
            Text("Background 2 ", fontSize = 30.sp, color = Color.Black)
            Button("Square", bg2 == Shape.Square) { setBg2(Shape.Square) }
            Button("Circle", bg2 == Shape.Circle) { setBg2(Shape.Circle) }
        }
        Row {
            Text("Border ", fontSize = 30.sp, color = Color.Black)
            Button("Sharp", borderType == SquareBorder.Sharp) { setBorderType(SquareBorder.Sharp) }
            Button("Curved", borderType == SquareBorder.Curved) {
                setBorderType(SquareBorder.Curved)
            }
        }
        Row {
            Text("Color ", fontSize = 30.sp, color = Color.Black)
            Button("Green", color == SquareColor.Green) { setColor(SquareColor.Green) }
            Button("Blue", color == SquareColor.Blue) { setColor(SquareColor.Blue) }
        }
    }
}

@Composable
fun FakeBrowseItem(name: String) {
    Column(Modifier.border(1.dp, Color.Red).width(200.dp).height(240.dp)) {
        Box(Modifier.border(1.dp, Color.Black).background(Color.Blue).width(200.dp).height(200.dp))
        Text(name, fontSize = 20.sp)
    }
}

@Composable
fun NumColumnButton(num: Int, setNum: (Int) -> Unit, setAdaptive: (Boolean) -> Unit) {
    Box(
        modifier = Modifier.border(1.dp, Color.Black).width(30.dp).height(30.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier =
                Modifier.clickable {
                    setNum(num)
                    setAdaptive(false)
                },
            text = num.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
    Box(Modifier.width(10.dp))
}

@Composable
fun AdaptiveButton(setAdaptive: (Boolean) -> Unit) {
    Box(
        modifier = Modifier.border(1.dp, Color.Black).width(130.dp).height(30.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.clickable { setAdaptive(true) },
            text = "Adaptive",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
    Box(Modifier.width(10.dp))
}

@Composable
private fun Slider(value: MutableState<Float>, min: Float, max: Float) {
    val density = LocalDensity.current.density
    val sliderMax = 400f * density
    val v = remember { mutableStateOf(sliderMax * (value.value - min) / (max - min)) }
    Box(
        modifier =
            Modifier.width(440.dp)
                .height(40.dp)
                .border(width = 2.dp, color = Color.Gray, shape = RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier =
                Modifier.offset {
                        IntOffset(
                            v.value.roundToInt() + (5 * density).toInt(),
                            (5 * density).toInt()
                        )
                    }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state =
                            rememberDraggableState { delta ->
                                v.value = max(min(v.value + delta, sliderMax), 0f)
                                value.value = min + (max - min) * v.value / sliderMax
                            }
                    )
                    .size(30.dp)
                    .border(width = 25.dp, color = Color.Black, shape = RoundedCornerShape(5.dp))
        )
    }
}

@Composable
fun LazyGridItemSpans() {
    val (numColumns, setNumColumns) = remember { mutableStateOf(3) }
    val (adaptive, setAdaptive) = remember { mutableStateOf(false) }
    val horizontalSpacing = remember { mutableStateOf(10f) }
    val verticalSpacing = remember { mutableStateOf(10f) }
    val adaptiveMin = remember { mutableStateOf(200f) }
    Column(Modifier.offset(20.dp, 10.dp)) {
        Row(Modifier.height(50.dp)) {
            Text("Number of Columns: ", fontSize = 20.sp)
            NumColumnButton(1, setNumColumns, setAdaptive)
            NumColumnButton(2, setNumColumns, setAdaptive)
            NumColumnButton(3, setNumColumns, setAdaptive)
            NumColumnButton(4, setNumColumns, setAdaptive)
            NumColumnButton(5, setNumColumns, setAdaptive)
            AdaptiveButton(setAdaptive)
        }
        Row(Modifier.height(50.dp)) {
            Text("Horizontal Item Spacing: ", fontSize = 20.sp)
            Slider(horizontalSpacing, 0f, 200f)
            Text(horizontalSpacing.value.toString(), fontSize = 20.sp)
        }
        Row(Modifier.height(50.dp)) {
            Text("Vertical Item Spacing: ", fontSize = 20.sp)
            Slider(verticalSpacing, 0f, 200f)
            Text(verticalSpacing.value.toString(), fontSize = 20.sp)
        }
        Row(Modifier.height(50.dp)) {
            Text("Adaptive Min Spacing: ", fontSize = 20.sp)
            Slider(adaptiveMin, 1f, 400f)
            Text(adaptiveMin.value.toString(), fontSize = 20.sp)
        }
    }

    val adaptiveMinDp = (adaptiveMin.value.toInt()).dp
    val sections = (0 until 100).toList().chunked(5)
    Box(Modifier.offset(20.dp, 230.dp).border(2.dp, Color.Black)) {
        LazyVerticalGrid(
            modifier =
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp, 0.dp).height(1000.dp),
            columns =
                if (adaptive) GridCells.Adaptive(adaptiveMinDp) else GridCells.Fixed(numColumns),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing.value.toInt().dp),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing.value.toInt().dp)
        ) {
            sections.forEachIndexed { index, items ->
                item(span = { GridItemSpan(if (adaptive) maxLineSpan else numColumns) }) {
                    Text(
                        "This is section $index",
                        Modifier.border(2.dp, Color.Black).height(80.dp).wrapContentSize(),
                        fontSize = 26.sp,
                    )
                }
                items(
                    items,
                    // not required as it is the default
                    span = { GridItemSpan(1) }
                ) {
                    FakeBrowseItem("Item $it")
                }
            }
        }
    }
}

enum class ItemType {
    Grid,
    List
}

// TEST Grid Layout
@DesignDoc(id = "JOSOEvsrjvMqanyQa5OpNR")
interface GridLayoutTest {
    @DesignComponent(node = "#MainFrame")
    fun MainFrame(
        @DesignContentTypes(nodes = ["#SectionTitle", "#Item"])
        @DesignPreviewContent(
            name = "Browse",
            nodes =
                [
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(6, "#Item=Grid"),
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(4, "#Item=Grid")
                ]
        )
        @Design(node = "#VerticalGrid1")
        vertical1: ListContent,
        @DesignContentTypes(nodes = ["#SectionTitle", "#VItem"])
        @DesignPreviewContent(
            name = "Browse",
            nodes =
                [
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(6, "#VItem=Grid"),
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(4, "#VItem=Grid")
                ]
        )
        @Design(node = "#HorizontalGrid1")
        horizontal1: ListContent,
        @DesignContentTypes(nodes = ["#SectionTitle", "#Item"])
        @DesignPreviewContent(
            name = "Browse",
            nodes =
                [
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(6, "#Item=Grid"),
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(8, "#Item=Grid"),
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(6, "#Item=Grid"),
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(10, "#Item=List")
                ]
        )
        @Design(node = "#VerticalGrid2")
        vertical2: ListContent,
        @DesignContentTypes(nodes = ["#SectionTitle", "#VItem"])
        @DesignPreviewContent(
            name = "Browse",
            nodes =
                [
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(6, "#VItem=Grid"),
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(6, "#VItem=Grid"),
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(6, "#VItem=Grid"),
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(11, "#VItem=List")
                ]
        )
        @Design(node = "#HorizontalGrid2")
        horizontal2: ListContent,
    )
    @DesignComponent(node = "#Item")
    fun Item(
        @DesignVariant(property = "#Item") type: ItemType,
        @Design(node = "#Title") title: String,
    )
    @DesignComponent(node = "#VItem")
    fun VItem(
        @DesignVariant(property = "#VItem") type: ItemType,
        @Design(node = "#Title") title: String,
    )
    @DesignComponent(node = "#SectionTitle")
    fun SectionTitle(@Design(node = "#Title") title: String)
    @DesignComponent(node = "#VSectionTitle")
    fun VSectionTitle(@Design(node = "#Title") title: String)
}

enum class GridItemType {
    SectionTitle,
    VSectionTitle,
    RowGrid,
    RowList,
    ColGrid,
    ColList,
}

@Composable
fun GridLayoutTest() {
    val vertItems: ArrayList<Pair<GridItemType, String>> = arrayListOf()
    for (i in 1..20) vertItems.add(Pair(GridItemType.RowGrid, "Item $i"))
    for (i in 21..40) vertItems.add(Pair(GridItemType.RowList, "Row Item $i"))
    vertItems.add(0, Pair(GridItemType.SectionTitle, "Group One"))
    vertItems.add(7, Pair(GridItemType.SectionTitle, "Group Two"))
    vertItems.add(12, Pair(GridItemType.SectionTitle, "Group Three"))
    vertItems.add(20, Pair(GridItemType.SectionTitle, "Group Four"))

    val horizItems: ArrayList<Pair<GridItemType, String>> = arrayListOf()
    for (i in 1..20) horizItems.add(Pair(GridItemType.ColGrid, "Item $i"))
    for (i in 21..40) horizItems.add(Pair(GridItemType.ColList, "Row Item $i"))
    horizItems.add(0, Pair(GridItemType.SectionTitle, "Group One"))
    horizItems.add(7, Pair(GridItemType.SectionTitle, "Group Two"))
    horizItems.add(14, Pair(GridItemType.SectionTitle, "Group Three"))
    horizItems.add(20, Pair(GridItemType.SectionTitle, "Group Four"))

    fun getNodeData(items: ArrayList<Pair<GridItemType, String>>, index: Int): GetDesignNodeData {
        return when (items[index].first) {
            GridItemType.SectionTitle -> {
                { GridLayoutTestDoc.SectionTitleDesignNodeData() }
            }
            GridItemType.VSectionTitle -> {
                { GridLayoutTestDoc.VSectionTitleDesignNodeData() }
            }
            GridItemType.RowGrid -> {
                { GridLayoutTestDoc.ItemDesignNodeData(type = ItemType.Grid) }
            }
            GridItemType.RowList -> {
                { GridLayoutTestDoc.ItemDesignNodeData(type = ItemType.List) }
            }
            GridItemType.ColGrid -> {
                { GridLayoutTestDoc.VItemDesignNodeData(type = ItemType.Grid) }
            }
            GridItemType.ColList -> {
                { GridLayoutTestDoc.VItemDesignNodeData(type = ItemType.List) }
            }
        }
    }

    @Composable
    fun itemComposable(items: ArrayList<Pair<GridItemType, String>>, index: Int) {
        when (items[index].first) {
            GridItemType.SectionTitle -> GridLayoutTestDoc.SectionTitle(title = items[index].second)
            GridItemType.VSectionTitle ->
                GridLayoutTestDoc.VSectionTitle(title = items[index].second)
            GridItemType.RowGrid ->
                GridLayoutTestDoc.Item(type = ItemType.Grid, title = items[index].second)
            GridItemType.RowList ->
                GridLayoutTestDoc.Item(type = ItemType.List, title = items[index].second)
            GridItemType.ColGrid ->
                GridLayoutTestDoc.VItem(type = ItemType.Grid, title = items[index].second)
            GridItemType.ColList ->
                GridLayoutTestDoc.VItem(type = ItemType.List, title = items[index].second)
        }
    }

    GridLayoutTestDoc.MainFrame(
        modifier = Modifier.fillMaxSize(),
        vertical1 = { spanFunc ->
            ListContentData(
                count = vertItems.size,
                span = { index ->
                    val nodeData = getNodeData(vertItems, index)
                    spanFunc(nodeData)
                },
            ) { index ->
                itemComposable(vertItems, index)
            }
        },
        vertical2 = { spanFunc ->
            ListContentData(
                count = vertItems.size,
                span = { index ->
                    val nodeData = getNodeData(vertItems, index)
                    spanFunc(nodeData)
                },
            ) { index ->
                itemComposable(vertItems, index)
            }
        },
        horizontal1 = { spanFunc ->
            ListContentData(
                count = horizItems.size,
                span = { index ->
                    val nodeData = getNodeData(horizItems, index)
                    spanFunc(nodeData)
                },
            ) { index ->
                itemComposable(horizItems, index)
            }
        },
        horizontal2 = { spanFunc ->
            ListContentData(
                count = horizItems.size,
                span = { index ->
                    val nodeData = getNodeData(horizItems, index)
                    spanFunc(nodeData)
                },
            ) { index ->
                itemComposable(horizItems, index)
            }
        },
    )
}

// TEST Grid Preview Widget
@DesignDoc(id = "OBhNItd9i9J2LwVYuLxEIx")
interface GridWidgetTest {
    @DesignComponent(node = "#Main")
    fun MainFrame(
        @DesignContentTypes(nodes = ["#SectionTitle", "#Item", "#LoadingPage", "#ErrorPage"])
        @DesignPreviewContent(
            name = "Browse",
            nodes =
                [
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(2, "#Item=Grid, #Playing=Off"),
                    PreviewNode(1, "#Item=Grid, #Playing=On"),
                    PreviewNode(6, "#Item=Grid, #Playing=Off"),
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(1, "#Item=List, #Playing=On"),
                    PreviewNode(3, "#Item=List, #Playing=Off")
                ]
        )
        @DesignPreviewContent(
            name = "Album",
            nodes =
                [
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(1, "#Item=List, #Playing=On"),
                    PreviewNode(16, "#Item=List, #Playing=Off")
                ]
        )
        @DesignPreviewContent(name = "Loading", nodes = [PreviewNode(1, "#LoadingPage")])
        @DesignPreviewContent(name = "Error", nodes = [PreviewNode(1, "#ErrorPage")])
        @Design(node = "#column-auto-content")
        columns: ListContent,
        @DesignContentTypes(nodes = ["#VSectionTitle", "#VItem", "#LoadingPage", "#ErrorPage"])
        @DesignPreviewContent(
            name = "Browse",
            nodes =
                [
                    PreviewNode(1, "#VSectionTitle"),
                    PreviewNode(2, "#VItem=Grid, #Playing=Off"),
                    PreviewNode(1, "#VItem=Grid, #Playing=On"),
                    PreviewNode(4, "#VItem=Grid, #Playing=Off"),
                    PreviewNode(1, "#VSectionTitle"),
                    PreviewNode(1, "#VItem=List, #Playing=On"),
                    PreviewNode(3, "#VItem=List, #Playing=Off")
                ]
        )
        @DesignPreviewContent(name = "Loading", nodes = [PreviewNode(1, "#LoadingPage")])
        @DesignPreviewContent(name = "Error", nodes = [PreviewNode(1, "#ErrorPage")])
        @Design(node = "#row-auto-content")
        rows: ListContent,
        @DesignContentTypes(nodes = ["#Item"])
        @DesignPreviewContent(name = "List", nodes = [PreviewNode(10, "#Item=Grid, #Playing=Off")])
        @Design(node = "#list-auto-content")
        items: ListContent,
    )
    @DesignComponent(node = "#Item")
    fun Item(
        @DesignVariant(property = "#Item") type: ItemType,
        @Design(node = "#Title") title: String,
    )
    @DesignComponent(node = "#VItem")
    fun VItem(
        @DesignVariant(property = "#VItem") type: ItemType,
        @Design(node = "#Title") title: String,
    )
    @DesignComponent(node = "#SectionTitle")
    fun SectionTitle(@Design(node = "#Title") title: String)
    @DesignComponent(node = "#VSectionTitle")
    fun VSectionTitle(@Design(node = "#Title") title: String)
}

@Composable
fun GridWidgetTest() {
    val vertItems: ArrayList<Pair<GridItemType, String>> = arrayListOf()
    for (i in 1..20) vertItems.add(Pair(GridItemType.RowGrid, "Item $i"))
    for (i in 21..40) vertItems.add(Pair(GridItemType.RowList, "Row Item $i"))
    vertItems.add(0, Pair(GridItemType.SectionTitle, "Group One"))
    vertItems.add(15, Pair(GridItemType.SectionTitle, "Group Two"))
    vertItems.add(20, Pair(GridItemType.SectionTitle, "Group Three"))

    val horizItems: ArrayList<Pair<GridItemType, String>> = arrayListOf()
    for (i in 1..20) horizItems.add(Pair(GridItemType.ColGrid, "Item $i"))
    for (i in 21..40) horizItems.add(Pair(GridItemType.ColList, "Row Item $i"))
    horizItems.add(0, Pair(GridItemType.VSectionTitle, "Group One"))
    horizItems.add(7, Pair(GridItemType.VSectionTitle, "Group Two"))
    horizItems.add(14, Pair(GridItemType.VSectionTitle, "Group Three"))
    horizItems.add(20, Pair(GridItemType.VSectionTitle, "Group Four"))

    fun getNodeData(items: ArrayList<Pair<GridItemType, String>>, index: Int): GetDesignNodeData {
        return when (items[index].first) {
            GridItemType.SectionTitle -> {
                { GridWidgetTestDoc.SectionTitleDesignNodeData() }
            }
            GridItemType.VSectionTitle -> {
                { GridWidgetTestDoc.VSectionTitleDesignNodeData() }
            }
            GridItemType.RowGrid -> {
                { GridWidgetTestDoc.ItemDesignNodeData(type = ItemType.Grid) }
            }
            GridItemType.RowList -> {
                { GridWidgetTestDoc.ItemDesignNodeData(type = ItemType.List) }
            }
            GridItemType.ColGrid -> {
                { GridWidgetTestDoc.VItemDesignNodeData(type = ItemType.Grid) }
            }
            GridItemType.ColList -> {
                { GridWidgetTestDoc.VItemDesignNodeData(type = ItemType.List) }
            }
        }
    }

    @Composable
    fun itemComposable(items: ArrayList<Pair<GridItemType, String>>, index: Int) {
        when (items[index].first) {
            GridItemType.SectionTitle ->
                GridWidgetTestDoc.SectionTitle(
                    title = items[index].second,
                    parentLayout = widgetParent
                )
            GridItemType.VSectionTitle ->
                GridWidgetTestDoc.VSectionTitle(
                    title = items[index].second,
                    parentLayout = widgetParent
                )
            GridItemType.RowGrid ->
                GridWidgetTestDoc.Item(
                    type = ItemType.Grid,
                    title = items[index].second,
                    parentLayout = widgetParent
                )
            GridItemType.RowList ->
                GridWidgetTestDoc.Item(
                    type = ItemType.List,
                    title = items[index].second,
                    parentLayout = widgetParent
                )
            GridItemType.ColGrid ->
                GridWidgetTestDoc.VItem(
                    type = ItemType.Grid,
                    title = items[index].second,
                    parentLayout = widgetParent
                )
            GridItemType.ColList ->
                GridWidgetTestDoc.VItem(
                    type = ItemType.List,
                    title = items[index].second,
                    parentLayout = widgetParent
                )
        }
    }

    GridWidgetTestDoc.MainFrame(
        modifier = Modifier.fillMaxSize(),
        columns = { spanFunc ->
            ListContentData(
                count = vertItems.size,
                span = { index ->
                    val nodeData = getNodeData(vertItems, index)
                    spanFunc(nodeData)
                },
            ) { index ->
                itemComposable(vertItems, index)
            }
        },
        rows = { spanFunc ->
            ListContentData(
                count = horizItems.size,
                span = { index ->
                    val nodeData = getNodeData(horizItems, index)
                    spanFunc(nodeData)
                },
            ) { index ->
                itemComposable(horizItems, index)
            }
        },
        items = {
            ListContentData(
                count = 10,
                span = { LazyContentSpan(1) },
            ) { index ->
                GridWidgetTestDoc.Item(type = ItemType.Grid, title = "Item $index")
            }
        },
    )
}
// TEST List Preview Widget
@DesignDoc(id = "9ev0MBNHFrgTqJOrAGcEpV")
interface ListWidgetTest {
    @DesignComponent(node = "#Main")
    fun MainFrame(
        @DesignContentTypes(nodes = ["#Item"])
        @DesignPreviewContent(
            name = "Items",
            nodes =
                [
                    PreviewNode(4, "#Item=Grid, #Playing=Off"),
                    PreviewNode(1, "#Item=Grid, #Playing=On"),
                    PreviewNode(6, "#Item=Grid, #Playing=Off"),
                ]
        )
        @Design(node = "#row-content")
        rowItems: ListContent,
        @DesignContentTypes(nodes = ["#Item"])
        @DesignPreviewContent(
            name = "Items",
            nodes =
                [
                    PreviewNode(4, "#Item=Grid, #Playing=Off"),
                    PreviewNode(1, "#Item=Grid, #Playing=On"),
                    PreviewNode(6, "#Item=Grid, #Playing=Off"),
                ]
        )
        @Design(node = "#row-content-scrolling")
        rowScrollItems: ListContent,
        @DesignContentTypes(nodes = ["#VItem"])
        @DesignPreviewContent(
            name = "Items",
            nodes =
                [
                    PreviewNode(4, "#VItem=Grid, #Playing=Off"),
                    PreviewNode(1, "#VItem=Grid, #Playing=On"),
                    PreviewNode(6, "#VItem=Grid, #Playing=Off"),
                ]
        )
        @Design(node = "#col-content")
        colItems: ListContent,
        @DesignContentTypes(nodes = ["#VItem"])
        @DesignPreviewContent(
            name = "Items",
            nodes =
                [
                    PreviewNode(4, "#VItem=Grid, #Playing=Off"),
                    PreviewNode(1, "#VItem=Grid, #Playing=On"),
                    PreviewNode(6, "#VItem=Grid, #Playing=Off"),
                ]
        )
        @Design(node = "#col-content-scrolling")
        colScrollItems: ListContent,
    )
    @DesignComponent(node = "#Item")
    fun Item(
        @DesignVariant(property = "#Item") type: ItemType,
        @Design(node = "#Title") title: String,
    )
    @DesignComponent(node = "#VItem")
    fun VItem(
        @DesignVariant(property = "#VItem") type: ItemType,
        @Design(node = "#Title") title: String,
    )
}

@Composable
fun ListWidgetTest() {
    val rowItems: ArrayList<Pair<GridItemType, String>> = arrayListOf()
    for (i in 1..6) rowItems.add(Pair(GridItemType.RowGrid, "Item $i"))
    val rowScrollItems: ArrayList<Pair<GridItemType, String>> = arrayListOf()
    for (i in 1..30) rowScrollItems.add(Pair(GridItemType.RowGrid, "Item $i"))

    val colItems: ArrayList<Pair<GridItemType, String>> = arrayListOf()
    for (i in 1..6) colItems.add(Pair(GridItemType.ColGrid, "Item $i"))
    val colScrollItems: ArrayList<Pair<GridItemType, String>> = arrayListOf()
    for (i in 1..30) colScrollItems.add(Pair(GridItemType.ColGrid, "Item $i"))

    fun getNodeData(items: ArrayList<Pair<GridItemType, String>>, index: Int): GetDesignNodeData {
        return when (items[index].first) {
            GridItemType.RowGrid -> {
                { ListWidgetTestDoc.ItemDesignNodeData(type = ItemType.Grid) }
            }
            else -> {
                { ListWidgetTestDoc.VItemDesignNodeData(type = ItemType.Grid) }
            }
        }
    }

    @Composable
    fun itemComposable(items: ArrayList<Pair<GridItemType, String>>, index: Int) {
        when (items[index].first) {
            GridItemType.RowGrid ->
                ListWidgetTestDoc.Item(type = ItemType.Grid, title = items[index].second)
            else -> ListWidgetTestDoc.VItem(type = ItemType.Grid, title = items[index].second)
        }
    }

    ListWidgetTestDoc.MainFrame(
        modifier = Modifier.fillMaxSize(),
        rowItems = { spanFunc ->
            ListContentData(
                count = rowItems.size,
                span = { index ->
                    val nodeData = getNodeData(rowItems, index)
                    spanFunc(nodeData)
                },
            ) { index ->
                itemComposable(rowItems, index)
            }
        },
        rowScrollItems = { spanFunc ->
            ListContentData(
                count = rowScrollItems.size,
                span = { index ->
                    val nodeData = getNodeData(rowScrollItems, index)
                    spanFunc(nodeData)
                },
            ) { index ->
                itemComposable(rowScrollItems, index)
            }
        },
        colItems = { spanFunc ->
            ListContentData(
                count = colItems.size,
                span = { index ->
                    val nodeData = getNodeData(colItems, index)
                    spanFunc(nodeData)
                },
            ) { index ->
                itemComposable(colItems, index)
            }
        },
        colScrollItems = { spanFunc ->
            ListContentData(
                count = colScrollItems.size,
                span = { index ->
                    val nodeData = getNodeData(colScrollItems, index)
                    spanFunc(nodeData)
                },
            ) { index ->
                itemComposable(colScrollItems, index)
            }
        },
    )
}

// TEST One Pixel Separator
@DesignDoc(id = "EXjTHxfMNBtXDrz8hr6MFB")
interface OnePxSeparator {
    @DesignComponent(node = "#stage") fun MainFrame()
}

@Composable
fun OnePxSeparatorTest() {
    OnePxSeparatorDoc.MainFrame()
}

enum class PlayState {
    Play,
    Pause
}

enum class ButtonState {
    On,
    Off,
    Blue,
    Green
}

// TEST Variant Interactions
// This test checks that variants can have onPress CHANGE_TO interactions as well as a tap callback.
// Tapping on one of the variants should trigger the onPress state change for that node and only
// that node. Releasing while still on top of the variant should trigger the onTap callback that
// simply prints a line to stdout. Note that a unique "key" must be passed into each variant in
// order to uniquely identify each one so that the CHANGE_TO interaction only affects one instance.
@DesignDoc(id = "WcsgoLR4aDRSkZHY29Qdhq")
interface VariantInteractionsTest {
    @DesignComponent(node = "#MainFrame")
    fun MainFrame(
        @DesignContentTypes(nodes = ["#SectionTitle", "#Item"])
        @Design(node = "#Content")
        content: ReplacementContent,
        @DesignVariant(property = "#ButtonCircle") buttonCircleState: ButtonState,
    )
    @DesignComponent(node = "#ButtonVariant1")
    fun ButtonVariant1(
        @DesignVariant(property = "#ButtonVariant1") type: ItemType,
        @Design(node = "#Title") title: String,
        @Design(node = "#ButtonVariant1") onTap: TapCallback
    )
    @DesignComponent(node = "#ButtonVariant2")
    fun ButtonVariant2(
        @DesignVariant(property = "#ButtonVariant2") type: ItemType,
        @DesignVariant(property = "#PlayState") playState: PlayState,
        @Design(node = "#Title") title: String,
        @Design(node = "#ButtonVariant2") onTap: TapCallback
    )
}

@Composable
fun VariantInteractionsTest() {
    val (buttonCircleState, setButtonCircleState) = remember { mutableStateOf(ButtonState.Off) }

    VariantInteractionsTestDoc.MainFrame(
        content =
            ReplacementContent(
                count = 9,
                content = { index ->
                    { rc ->
                        when (index) {
                            0 ->
                                VariantInteractionsTestDoc.ButtonVariant1(
                                    parentLayout = ParentLayoutInfo(rc.parentLayoutId, index),
                                    type = ItemType.List,
                                    title = "One",
                                    onTap = { println("Tap One") },
                                    key = "One"
                                )
                            1 ->
                                VariantInteractionsTestDoc.ButtonVariant1(
                                    parentLayout = ParentLayoutInfo(rc.parentLayoutId, index),
                                    type = ItemType.List,
                                    title = "Two",
                                    onTap = { println("Tap Two") },
                                    key = "Two"
                                )
                            2 ->
                                VariantInteractionsTestDoc.ButtonVariant1(
                                    parentLayout = ParentLayoutInfo(rc.parentLayoutId, index),
                                    type = ItemType.List,
                                    title = "Three",
                                    onTap = { println("Tap Three") },
                                    key = "Three"
                                )
                            3 ->
                                VariantInteractionsTestDoc.ButtonVariant2(
                                    parentLayout = ParentLayoutInfo(rc.parentLayoutId, index),
                                    type = ItemType.Grid,
                                    playState = PlayState.Play,
                                    title = "Four",
                                    onTap = { println("Tap Four") },
                                    key = "Four"
                                )
                            4 ->
                                VariantInteractionsTestDoc.ButtonVariant2(
                                    parentLayout = ParentLayoutInfo(rc.parentLayoutId, index),
                                    type = ItemType.Grid,
                                    playState = PlayState.Play,
                                    title = "Five",
                                    onTap = { println("Tap Five") },
                                    key = "Five"
                                )
                            5 ->
                                VariantInteractionsTestDoc.ButtonVariant2(
                                    parentLayout = ParentLayoutInfo(rc.parentLayoutId, index),
                                    type = ItemType.Grid,
                                    playState = PlayState.Pause,
                                    title = "Six",
                                    onTap = { println("Tap Six") },
                                    key = "Six"
                                )
                            6 ->
                                VariantInteractionsTestDoc.ButtonVariant2(
                                    parentLayout = ParentLayoutInfo(rc.parentLayoutId, index),
                                    type = ItemType.Grid,
                                    playState = PlayState.Pause,
                                    title = "Seven",
                                    onTap = { println("Tap Seven") },
                                    key = "Seven"
                                )
                            7 ->
                                VariantInteractionsTestDoc.ButtonVariant2(
                                    parentLayout = ParentLayoutInfo(rc.parentLayoutId, index),
                                    type = ItemType.List,
                                    playState = PlayState.Pause,
                                    title = "Eight",
                                    onTap = { println("Tap Eight") },
                                    key = "Eight"
                                )
                            8 ->
                                VariantInteractionsTestDoc.ButtonVariant2(
                                    parentLayout = ParentLayoutInfo(rc.parentLayoutId, index),
                                    type = ItemType.List,
                                    playState = PlayState.Pause,
                                    title = "Nine",
                                    onTap = { println("Tap Nine") },
                                    key = "Nine"
                                )
                        }
                    }
                }
            ),
        buttonCircleState = buttonCircleState
    )
}

// TEST Layout Replacement
// This test places components (#fill, #topleft, #bottomright, #center) which have Layout
// Constraints applied to them inside of various parent elements which are larger than
// the components.
// We should see that the layout constraints are applied to the components and that they
// resize.
@DesignDoc(id = "dwk2GF7RiNvlbbAKPjqldx")
interface LayoutReplacementTest {
    @DesignComponent(node = "#stage")
    fun MainFrame(
        @Design(node = "#next") onNext: TapCallback,
        @Design(node = "#parent1") parent1: ReplacementContent,
        @Design(node = "#parent2") parent2: ReplacementContent,
        @Design(node = "#parent3") parent3: ReplacementContent,
    )
    @DesignComponent(node = "#fill") fun Fill()
    @DesignComponent(node = "#topleft") fun TopLeft()
    @DesignComponent(node = "#bottomright") fun BottomRight()
    @DesignComponent(node = "#center") fun Center()
}

@Composable
fun LayoutReplacementTestCase(idx: Int, rc: ContentReplacementContext) {
    if (idx == 0) {
        LayoutReplacementTestDoc.Fill(parentLayout = ParentLayoutInfo(rc.parentLayoutId, 0))
    } else if (idx == 1) {
        LayoutReplacementTestDoc.TopLeft(parentLayout = ParentLayoutInfo(rc.parentLayoutId, 0))
    } else if (idx == 2) {
        LayoutReplacementTestDoc.BottomRight(parentLayout = ParentLayoutInfo(rc.parentLayoutId, 0))
    } else if (idx == 3) {
        LayoutReplacementTestDoc.Center(parentLayout = ParentLayoutInfo(rc.parentLayoutId, 0))
    }
}

@Composable
fun LayoutReplacementTest() {
    val (idx, setIdx) = remember { mutableStateOf(0) }
    LayoutReplacementTestDoc.MainFrame(
        onNext = { setIdx((idx + 1) % 4) },
        parent1 =
            ReplacementContent(
                count = 1,
                content = { { rc -> LayoutReplacementTestCase(idx, rc) } }
            ),
        parent2 =
            ReplacementContent(
                count = 1,
                content = { { rc -> LayoutReplacementTestCase(idx, rc) } }
            ),
        parent3 =
            ReplacementContent(
                count = 1,
                content = { { rc -> LayoutReplacementTestCase(idx, rc) } }
            ),
    )
}

// TEST Text Elide Test
// This test tests that text max line count and eliding with ellipsis works
@DesignDoc(id = "oQ7nK49Ya5PJ3GpjI5iy8d")
interface TextElideTest {
    @DesignComponent(node = "#stage") fun MainFrame()
}

@Composable
fun TextElideTest() {
    TextElideTestDoc.MainFrame()
}

// TEST fancy fill types (solid color, gradients, images) on text, frames, and strokes
@DesignDoc(id = "xQ9cunHt8VUm6xqJJ2Pjb2")
interface FancyFillTest {
    @DesignComponent(node = "#stage") fun MainFrame(@Design(node = "#xyz") onTap: TapCallback)
}

@Composable
fun FancyFillTest() {
    FancyFillTestDoc.MainFrame(onTap = { Log.e("onTap", "frame clicked!") })
}

// TEST Fill Container test
// The outer black frame should fill the whole screen. Within the black frame there should be a top
// and bottom blue frame of equal size that both stretch to fill the black frame. Each of those
// frames should have additional frames that also stretch in both directions.
@DesignDoc(id = "dB3q96FkxkTO4czn5NqnxV")
interface FillTest {
    @DesignComponent(node = "#stage") fun MainFrame()
}

@Composable
fun FillTest() {
    FillTestDoc.MainFrame(Modifier.fillMaxSize())
}

// TEST Cross Axis Fill
@DesignDoc(id = "GPr1cx4n3zBPwLhqlSL1ba")
interface CrossAxisFillTest {
    @DesignComponent(node = "#stage")
    fun MainFrame(
        @Design(node = "#FixedWidth") fixedWidth: Modifier,
        @Design(node = "#OuterColumn") outerColumnContents: ReplacementContent,
    )
    @DesignComponent(node = "#LargeFixedWidth") fun LargeFixedWidth()
    @DesignComponent(node = "#FillParentWidth") fun FillParentWidth()
}

@Composable
fun CrossAxisFillTest() {
    CrossAxisFillTestDoc.MainFrame(
        modifier = Modifier.fillMaxWidth(),
        fixedWidth = Modifier.width(200.dp),
        outerColumnContents =
            ReplacementContent(
                count = 2,
                content = { index ->
                    { rc ->
                        if (index == 0)
                            CrossAxisFillTestDoc.LargeFixedWidth(
                                parentLayout = ParentLayoutInfo(rc.parentLayoutId, index),
                                modifier = Modifier.width(200.dp)
                            )
                        else
                            CrossAxisFillTestDoc.FillParentWidth(
                                parentLayout = ParentLayoutInfo(rc.parentLayoutId, index)
                            )
                    }
                }
            )
    )
}

// TEST Grid Layout for Documentation
@DesignDoc(id = "MBNjjSbzzKeN7nBjVoewsl")
interface GridLayout {
    @DesignComponent(node = "#stage")
    fun MainFrame(
        @Design(node = "#ListTitle") title: String,
        @DesignContentTypes(nodes = ["#SectionTitle", "#Item", "#Loading"])
        @DesignPreviewContent(name = "Loading", nodes = [PreviewNode(1, "#Loading")])
        @DesignPreviewContent(
            name = "LoadedList",
            nodes =
                [
                    PreviewNode(1, "#SectionTitle"),
                    PreviewNode(8, "#Item=Grid"),
                ]
        )
        @Design(node = "#BrowseList")
        items: ListContent,
    )
    @DesignComponent(node = "#SectionTitle")
    fun SectionTitle(@Design(node = "#Title") title: String)
    @DesignComponent(node = "#LoadingPage") fun LoadingPage()
    @DesignComponent(node = "#Item")
    fun Item(
        @DesignVariant(property = "#Item") itemType: ItemType,
        @Design(node = "#Title") title: String
    )
}

@Composable
fun GridLayoutDocumentation() {
    GridLayoutDoc.MainFrame(title = "Media Browse") { spanFunc ->
        ListContentData(
            count = 9,
            span = { index ->
                if (index == 0) spanFunc { GridLayoutDoc.SectionTitleDesignNodeData() }
                else spanFunc { GridLayoutDoc.ItemDesignNodeData(itemType = ItemType.Grid) }
            },
        ) { index ->
            if (index == 0) GridLayoutDoc.SectionTitle(title = "Recently Played")
            else GridLayoutDoc.Item(itemType = ItemType.Grid, title = "Item $index")
        }
    }
}

@DesignDoc(id = "ZqX5i5g6inv9tANIwMMXUV")
interface BlendModeTest {
    @DesignComponent(node = "#stage") fun MainFrame()
}

// Demonstration of blend modes applied to different objects
@Composable
fun BlendModeTest() {
    BlendModeTestDoc.MainFrame()
}

// TEST vector rendering
@DesignDoc(id = "Z3ucY0wMAbIwZIa6mLEWIK")
interface VectorRenderingTest {
    @DesignComponent(node = "#stage") fun MainFrame()
}

// Test page for vector rendering support
@Composable
fun VectorRenderingTest() {
    VectorRenderingTestDoc.MainFrame(modifier = Modifier.fillMaxSize())
}

// TEST dials and gauges
@DesignDoc(id = "lZj6E9GtIQQE4HNLpzgETw")
interface DialsGaugesTest {
    @DesignComponent(node = "#stage")
    fun MainFrame(
        @Design(node = "#arc-angle") arcAngle: Meter,
        @Design(node = "#needle-rotation") needleRotation: Meter,
        @Design(node = "#progress-bar") progressBar: Meter,
        @Design(node = "#progress-indicator") progressIndicator: Meter,
    )
}

@Composable
fun DialsGaugesTest() {
    val angle = remember { mutableStateOf(50f) }
    val rotation = remember { mutableStateOf(50f) }
    val progress = remember { mutableStateOf(50f) }
    val progressIndicator = remember { mutableStateOf(50f) }
    DialsGaugesTestDoc.MainFrame(
        arcAngle = angle.value,
        needleRotation = rotation.value,
        progressBar = progress.value,
        progressIndicator = progressIndicator.value,
    )

    Row(
        Modifier.absoluteOffset(0.dp, 1410.dp).height(50.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Dial angle: ", Modifier.width(120.dp), fontSize = 20.sp)
        Slider(angle, 0f, 100f)
        Text(angle.value.toString(), fontSize = 20.sp)
    }
    Row(
        Modifier.absoluteOffset(0.dp, 1460.dp).height(50.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Dial rotation: ", Modifier.width(120.dp), fontSize = 20.sp)
        Slider(rotation, 0f, 100f)
        Text(rotation.value.toString(), fontSize = 20.sp)
    }
    Row(
        Modifier.absoluteOffset(0.dp, 1510.dp).height(50.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Progress Bar: ", Modifier.width(120.dp), fontSize = 20.sp)
        Slider(progress, 0f, 100f)
        Text(progress.value.toString(), fontSize = 20.sp)
    }
    Row(
        Modifier.absoluteOffset(0.dp, 1560.dp).height(50.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Progress Indicator: ", Modifier.width(120.dp), fontSize = 20.sp)
        Slider(progressIndicator, 0f, 100f)
        Text(progressIndicator.value.toString(), fontSize = 20.sp)
    }
}

@DesignDoc(id = "mEmdUVEIjvBBbV0kELPy37")
interface MaskTest {
    @DesignComponent(node = "#MainFrame") fun Main()
}

@Composable
fun MaskTest() {
    MaskTestDoc.Main()
}

@DesignDoc(id = "MWnVAfW3FupV4VMLNR1m67")
interface VariableBorderTest {
    @DesignComponent(node = "#MainFrame") fun Main()
}

@Composable
fun VariableBorderTest() {
    VariableBorderTestDoc.Main()
}

enum class ButtonSquare {
    On,
    Off,
    Blue,
    Green
}

// TEST Various layout tests for new Rust based layout system
@DesignDoc(id = "Gv63fYTzpeH2ZtxP4go31E")
interface LayoutTests {
    @DesignComponent(node = "#stage")
    fun Main(
        @Design(node = "#Name") name: String,
        @Design(node = "#NameAutoWidth") nameAutoWidth: String,
        @Design(node = "#NameAutoHeight") nameAutoHeight: String,
        @Design(node = "#NameFixed") nameFixed: String,
        @Design(node = "#NameFillWidthAutoHeight") nameFillWidthAutoHeight: String,
        @DesignVariant(property = "#ButtonSquare") buttonSquare: ButtonSquare,
        @Design(node = "#HorizontalContent") horizontalContent: ReplacementContent,
        @Design(node = "#Parent") parent: ReplacementContent,
        @DesignContentTypes(nodes = ["#BlueSquare", "#RedSquare", "#ButtonSquare"])
        @DesignPreviewContent(
            name = "Items",
            nodes =
                [
                    PreviewNode(1, "#ButtonSquare=Green"),
                    PreviewNode(1, "#ButtonSquare=Blue"),
                    PreviewNode(1, "#RedSquare"),
                    PreviewNode(1, "#BlueSquare"),
                    PreviewNode(1, "#ButtonSquare=Green"),
                    PreviewNode(1, "#ButtonSquare=Blue"),
                    PreviewNode(1, "#RedSquare"),
                    PreviewNode(1, "#BlueSquare"),
                ]
        )
        @Design(node = "#WidgetContent")
        widgetItems: ListContent,
        @Design(node = "#Rect1") showRect1: Boolean,
        @Design(node = "#Rect2") showRect2: Boolean,
        @Design(node = "#Replacement1")
        replacement1: @Composable (ComponentReplacementContext) -> Unit,
        @Design(node = "#Replacement2")
        replacement2: @Composable (ComponentReplacementContext) -> Unit,
    )
    @DesignComponent(node = "#BlueSquare") fun BlueSquare()
    @DesignComponent(node = "#RedSquare") fun RedSquare()
    @DesignComponent(node = "#fill") fun Fill()
    @DesignComponent(node = "#topleft") fun TopLeft()
    @DesignComponent(node = "#bottomright") fun BottomRight()
    @DesignComponent(node = "#center") fun Center()
    @DesignComponent(node = "#ButtonSquare")
    fun ButtonSquare(
        @DesignVariant(property = "#ButtonSquare") type: ButtonSquare,
    )
}

@Composable
fun LayoutTests() {
    val loremText =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
    val (autoWidthLen, setAutoWidthLen) = remember { mutableStateOf(17) }
    val (autoHeightLen, setAutoHeightLen) = remember { mutableStateOf(17) }
    val (fixedSizeLen, setFixedSizeLen) = remember { mutableStateOf(17) }
    val (fillWidthAutoHeightLen, setFillWidthAutoHeightLen) = remember { mutableStateOf(89) }
    val (buttonSquare, setButtonSquare) = remember { mutableStateOf(ButtonSquare.Off) }
    val (numChildren, setNumChildren) = remember { mutableStateOf(2) }
    val (showRect1, setShowRect1) = remember { mutableStateOf(false) }
    val (showRect2, setShowRect2) = remember { mutableStateOf(true) }

    LayoutTestsDoc.Main(
        name = "LongerText",
        nameAutoWidth = loremText.subSequence(0, autoWidthLen).toString(),
        nameAutoHeight = loremText.subSequence(0, autoHeightLen).toString(),
        nameFixed = loremText.subSequence(0, fixedSizeLen).toString(),
        nameFillWidthAutoHeight = loremText.subSequence(0, fillWidthAutoHeightLen).toString(),
        buttonSquare = buttonSquare,
        horizontalContent =
            ReplacementContent(
                count = numChildren,
                content = { index ->
                    { replacementContext ->
                        if (index % 2 == 0)
                            LayoutTestsDoc.BlueSquare(
                                parentLayout =
                                    ParentLayoutInfo(replacementContext.parentLayoutId, index)
                            )
                        else
                            LayoutTestsDoc.RedSquare(
                                parentLayout =
                                    ParentLayoutInfo(replacementContext.parentLayoutId, index)
                            )
                    }
                }
            ),
        parent =
            ReplacementContent(
                count = 1,
                content = { index ->
                    { rc ->
                        LayoutTestsDoc.Fill(
                            parentLayout = ParentLayoutInfo(rc.parentLayoutId, index)
                        )
                    }
                }
            ),
        widgetItems = { spanFunc ->
            ListContentData(
                count = 10,
                span = { index ->
                    val nodeData =
                        if (index % 4 == 0) {
                            { LayoutTestsDoc.RedSquareDesignNodeData() }
                        } else {
                            { LayoutTestsDoc.BlueSquareDesignNodeData() }
                        }
                    spanFunc(nodeData)
                },
            ) { index ->
                if (index % 4 == 0) LayoutTestsDoc.RedSquare(parentLayout = widgetParent)
                else LayoutTestsDoc.BlueSquare(parentLayout = widgetParent)
            }
        },
        showRect1 = showRect1,
        showRect2 = showRect2,
        replacement1 = { LayoutTestsDoc.BlueSquare(parentLayout = it.parentLayout) },
        replacement2 = { LayoutTestsDoc.BlueSquare(parentLayout = it.parentLayout) },
        designComposeCallbacks =
            DesignComposeCallbacks(
                docReadyCallback = { id ->
                    Log.i("DesignCompose", "HelloWorld Ready: doc ID = $id")
                },
                newDocDataCallback = { docId, data ->
                    Log.i(
                        "DesignCompose",
                        "HelloWorld Updated doc ID $docId: ${data?.size ?: 0} bytes"
                    )
                },
            )
    )
    Column(Modifier.offset(10.dp, 820.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("AutoWidth", fontSize = 30.sp, color = Color.Black)
            Button("-", false) {
                val len = (autoWidthLen - 1).coerceAtLeast(1)
                setAutoWidthLen(len)
            }
            Button("+", false) {
                val len = (autoWidthLen + 1).coerceAtMost(loremText.length)
                setAutoWidthLen(len)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("AutoHeight", fontSize = 30.sp, color = Color.Black)
            Button("-", false) {
                val len = (autoHeightLen - 1).coerceAtLeast(1)
                setAutoHeightLen(len)
            }
            Button("+", false) {
                val len = (autoHeightLen + 1).coerceAtMost(loremText.length)
                setAutoHeightLen(len)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Fixed", fontSize = 30.sp, color = Color.Black)
            Button("-", false) {
                val len = (fixedSizeLen - 1).coerceAtLeast(1)
                setFixedSizeLen(len)
            }
            Button("+", false) {
                val len = (fixedSizeLen + 1).coerceAtMost(loremText.length)
                setFixedSizeLen(len)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("FillWidth AutoHeight", fontSize = 30.sp, color = Color.Black)
            Button("-", false) {
                val len = (fillWidthAutoHeightLen - 1).coerceAtLeast(1)
                setFillWidthAutoHeightLen(len)
            }
            Button("+", false) {
                val len = (fillWidthAutoHeightLen + 1).coerceAtMost(loremText.length)
                setFillWidthAutoHeightLen(len)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ButtonSquare", fontSize = 30.sp, color = Color.Black)
            Button("Change Variant", false) {
                setButtonSquare(
                    when (buttonSquare) {
                        ButtonSquare.Off -> ButtonSquare.On
                        ButtonSquare.On -> ButtonSquare.Blue
                        ButtonSquare.Blue -> ButtonSquare.Green
                        ButtonSquare.Green -> ButtonSquare.Off
                    }
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Replacement Content", fontSize = 30.sp, color = Color.Black)
            Button("-", false) {
                val num = (numChildren - 1).coerceAtLeast(0)
                setNumChildren(num)
            }
            Button("+", false) { setNumChildren(numChildren + 1) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Visibility", fontSize = 30.sp, color = Color.Black)
            Button("Rect1", false) { setShowRect1(!showRect1) }
            Button("Rect2", false) { setShowRect2(!showRect2) }
        }
    }
}

// Main Activity class. Setup auth token and font, then build the UI with buttons for each test
// on the left and the currently selected test on the right.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DesignSettings.addFontFamily("Inter", interFont)
        DesignSettings.enableLiveUpdates(this)

        setContent {
            val index = remember { mutableStateOf(0) }
            Row {
                TestButtons(index)
                Divider(color = Color.Black, modifier = Modifier.fillMaxHeight().width(1.dp))
                TestContent(index.value)
            }
        }
    }
}

// Draw all the buttons on the left side of the screen, one for each test
@Composable
fun TestButtons(index: MutableState<Int>) {

    Column(Modifier.width(110.dp).verticalScroll(rememberScrollState())) {
        var count = 0
        EXAMPLES.forEach {
            TestButton(it.first, index, count)
            Divider(color = Color.Black, modifier = Modifier.height(1.dp))
            ++count
        }
    }
}

// Draw a single button
@Composable
fun TestButton(name: String, currentIndex: MutableState<Int>, myIndex: Int) {
    val weight = if (currentIndex.value == myIndex) FontWeight.Bold else FontWeight.Normal

    Text(
        modifier =
            Modifier.clickable {
                Log.i(TAG, "Button $name")
                currentIndex.value = myIndex
            },
        text = name,
        fontSize = 20.sp,
        fontWeight = weight
    )
}

// Draw the content for the current test
@Composable
fun TestContent(index: Int) {
    Box { EXAMPLES[index].second() }
}

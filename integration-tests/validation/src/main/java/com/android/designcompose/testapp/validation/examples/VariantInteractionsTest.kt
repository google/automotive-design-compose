package com.android.designcompose.testapp.validation.examples

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.android.designcompose.ParentLayoutInfo
import com.android.designcompose.ReplacementContent
import com.android.designcompose.TapCallback
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignContentTypes
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignVariant

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
                                    parentLayout =
                                        ParentLayoutInfo(rc.parentLayoutId, index, rc.rootLayoutId),
                                    type = ItemType.List,
                                    title = "One",
                                    onTap = { println("Tap One") },
                                    key = "One"
                                )
                            1 ->
                                VariantInteractionsTestDoc.ButtonVariant1(
                                    parentLayout =
                                        ParentLayoutInfo(rc.parentLayoutId, index, rc.rootLayoutId),
                                    type = ItemType.List,
                                    title = "Two",
                                    onTap = { println("Tap Two") },
                                    key = "Two"
                                )
                            2 ->
                                VariantInteractionsTestDoc.ButtonVariant1(
                                    parentLayout =
                                        ParentLayoutInfo(rc.parentLayoutId, index, rc.rootLayoutId),
                                    type = ItemType.List,
                                    title = "Three",
                                    onTap = { println("Tap Three") },
                                    key = "Three"
                                )
                            3 ->
                                VariantInteractionsTestDoc.ButtonVariant2(
                                    parentLayout =
                                        ParentLayoutInfo(rc.parentLayoutId, index, rc.rootLayoutId),
                                    type = ItemType.Grid,
                                    playState = PlayState.Play,
                                    title = "Four",
                                    onTap = { println("Tap Four") },
                                    key = "Four"
                                )
                            4 ->
                                VariantInteractionsTestDoc.ButtonVariant2(
                                    parentLayout =
                                        ParentLayoutInfo(rc.parentLayoutId, index, rc.rootLayoutId),
                                    type = ItemType.Grid,
                                    playState = PlayState.Play,
                                    title = "Five",
                                    onTap = { println("Tap Five") },
                                    key = "Five"
                                )
                            5 ->
                                VariantInteractionsTestDoc.ButtonVariant2(
                                    parentLayout =
                                        ParentLayoutInfo(rc.parentLayoutId, index, rc.rootLayoutId),
                                    type = ItemType.Grid,
                                    playState = PlayState.Pause,
                                    title = "Six",
                                    onTap = { println("Tap Six") },
                                    key = "Six"
                                )
                            6 ->
                                VariantInteractionsTestDoc.ButtonVariant2(
                                    parentLayout =
                                        ParentLayoutInfo(rc.parentLayoutId, index, rc.rootLayoutId),
                                    type = ItemType.Grid,
                                    playState = PlayState.Pause,
                                    title = "Seven",
                                    onTap = { println("Tap Seven") },
                                    key = "Seven"
                                )
                            7 ->
                                VariantInteractionsTestDoc.ButtonVariant2(
                                    parentLayout =
                                        ParentLayoutInfo(rc.parentLayoutId, index, rc.rootLayoutId),
                                    type = ItemType.List,
                                    playState = PlayState.Pause,
                                    title = "Eight",
                                    onTap = { println("Tap Eight") },
                                    key = "Eight"
                                )
                            8 ->
                                VariantInteractionsTestDoc.ButtonVariant2(
                                    parentLayout =
                                        ParentLayoutInfo(rc.parentLayoutId, index, rc.rootLayoutId),
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

enum class ButtonSquare {
    On,
    Off,
    Blue,
    Green
}

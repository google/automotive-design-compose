package com.android.designcompose.testapp.validation.examples

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.designcompose.ParentLayoutInfo
import com.android.designcompose.ReplacementContent
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

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
                            parentLayout =
                                ParentLayoutInfo(rc.parentLayoutId, index, rc.rootLayoutId)
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
                            parentLayout =
                                ParentLayoutInfo(rc.parentLayoutId, index, rc.rootLayoutId)
                        )
                    }
                }
            )
    )
}

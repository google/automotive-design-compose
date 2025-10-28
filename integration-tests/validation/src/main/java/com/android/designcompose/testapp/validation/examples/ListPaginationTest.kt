package com.android.designcompose.testapp.validation.examples

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.android.designcompose.ReplacementContent
import com.android.designcompose.TapCallback
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

@DesignDoc(id = "dGS6HEmsfv8QSHgAGBwEH7")
interface ListPaginationTest {
    // For static test
    @DesignComponent(node = "#root")
    fun Root(
        @Design(node = "Item0") item0Callback: TapCallback,
        @Design(node = "Item1") item1Callback: TapCallback,
        @Design(node = "Item2") item2Callback: TapCallback,
        @Design(node = "Item3") item3Callback: TapCallback,
        @Design(node = "Item4") item4Callback: TapCallback,
    )

    // For dynamic test - Assuming a frame named #DynamicRoot in Figma
    // containing a child #ListContainer to be replaced.
    @DesignComponent(node = "#DynamicRoot")
    fun DynamicRoot(@Design(node = "List") listContent: ReplacementContent)

    // Shared Item Composable
    @DesignComponent(node = "#Item")
    fun Item(@Design(node = "label") title: String, @Design(node = "#Item") onTap: TapCallback)
}

// Composable for the static test
@Composable
fun StaticListPaginationTest(onTap: (Int) -> Unit = {}) {
    ListPaginationTestDoc.Root(
        modifier = Modifier.fillMaxSize(),
        item0Callback = { onTap(0) },
        item1Callback = { onTap(1) },
        item2Callback = { onTap(2) },
        item3Callback = { onTap(3) },
        item4Callback = { onTap(4) },
    )
}

// Composable for the dynamic test
@Composable
fun DynamicListPaginationTest(itemCount: Int, onTap: (Int) -> Unit = {}) {
    ListPaginationTestDoc.DynamicRoot(
        modifier = Modifier.fillMaxSize(),
        listContent =
            ReplacementContent(
                count = itemCount,
                content = { index ->
                    @Composable {
                        ListPaginationTestDoc.Item(
                            modifier = Modifier.testTag("Item$index"),
                            title = "Item $index",
                            onTap = { onTap(index) },
                        )
                    }
                },
            ),
    )
}

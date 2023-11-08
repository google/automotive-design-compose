package com.android.designcompose.testapp.validation.examples

import androidx.compose.runtime.Composable
import com.android.designcompose.ParentLayoutInfo
import com.android.designcompose.ReplacementContent
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

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
                    { rc ->
                        RecursiveCustomizationsDoc.NameFrame(
                            parentLayout = ParentLayoutInfo(rc.parentLayoutId, 0, rc.rootLayoutId)
                        )
                    }
                }
            ),
        content =
            ReplacementContent(
                count = 3,
                content = { index ->
                    { rc ->
                        when (index) {
                            0 ->
                                RecursiveCustomizationsDoc.TitleFrame(
                                    parentLayout =
                                        ParentLayoutInfo(rc.parentLayoutId, 0, rc.rootLayoutId),
                                    title = "First"
                                )
                            1 ->
                                RecursiveCustomizationsDoc.TitleFrame(
                                    parentLayout =
                                        ParentLayoutInfo(rc.parentLayoutId, 1, rc.rootLayoutId),
                                    title = "Second"
                                )
                            else ->
                                RecursiveCustomizationsDoc.TitleFrame(
                                    parentLayout =
                                        ParentLayoutInfo(rc.parentLayoutId, 2, rc.rootLayoutId),
                                    title = "Third"
                                )
                        }
                    }
                }
            )
    )
}

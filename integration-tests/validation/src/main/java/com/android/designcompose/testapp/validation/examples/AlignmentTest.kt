package com.android.designcompose.testapp.validation.examples

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// TEST Alignment Test. Observe that the app rendering is identical to the Figma doc
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

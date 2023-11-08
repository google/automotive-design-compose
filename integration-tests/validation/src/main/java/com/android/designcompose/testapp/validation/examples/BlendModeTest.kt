package com.android.designcompose.testapp.validation.examples

import androidx.compose.runtime.Composable
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

@DesignDoc(id = "ZqX5i5g6inv9tANIwMMXUV")
interface BlendModeTest {
    @DesignComponent(node = "#stage") fun MainFrame()
}

// Demonstration of blend modes applied to different objects
@Composable
fun BlendModeTest() {
    BlendModeTestDoc.MainFrame()
}

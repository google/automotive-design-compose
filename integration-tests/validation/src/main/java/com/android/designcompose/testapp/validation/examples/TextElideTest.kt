package com.android.designcompose.testapp.validation.examples

import androidx.compose.runtime.Composable
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

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

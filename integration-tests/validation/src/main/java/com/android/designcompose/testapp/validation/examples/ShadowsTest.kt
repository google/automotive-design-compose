package com.android.designcompose.testapp.validation.examples

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

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

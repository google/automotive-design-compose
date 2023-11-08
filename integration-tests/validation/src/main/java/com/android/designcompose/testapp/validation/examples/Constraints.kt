package com.android.designcompose.testapp.validation.examples

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

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

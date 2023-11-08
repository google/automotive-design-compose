package com.android.designcompose.testapp.validation.examples

import androidx.compose.runtime.Composable
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// TEST One Pixel Separator
@DesignDoc(id = "EXjTHxfMNBtXDrz8hr6MFB")
interface OnePxSeparator {
    @DesignComponent(node = "#stage") fun MainFrame()
}

@Composable
fun OnePxSeparatorTest() {
    OnePxSeparatorDoc.MainFrame()
}

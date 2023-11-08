package com.android.designcompose.testapp.validation.examples

import androidx.compose.runtime.Composable
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

@DesignDoc(id = "mEmdUVEIjvBBbV0kELPy37")
interface MaskTest {
    @DesignComponent(node = "#MainFrame") fun Main()
}

@Composable
fun MaskTest() {
    MaskTestDoc.Main()
}

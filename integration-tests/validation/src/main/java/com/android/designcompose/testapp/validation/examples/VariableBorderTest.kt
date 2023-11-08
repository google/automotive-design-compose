package com.android.designcompose.testapp.validation.examples

import androidx.compose.runtime.Composable
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

@DesignDoc(id = "MWnVAfW3FupV4VMLNR1m67")
interface VariableBorderTest {
    @DesignComponent(node = "#MainFrame") fun Main()
}

@Composable
fun VariableBorderTest() {
    VariableBorderTestDoc.Main()
}

package com.android.designcompose.testapp.validation.examples

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// TEST vector rendering
@DesignDoc(id = "Z3ucY0wMAbIwZIa6mLEWIK")
interface VectorRenderingTest {
    @DesignComponent(node = "#stage") fun MainFrame()
}

// Test page for vector rendering support
@Composable
fun VectorRenderingTest() {
    VectorRenderingTestDoc.MainFrame(modifier = Modifier.fillMaxSize())
}

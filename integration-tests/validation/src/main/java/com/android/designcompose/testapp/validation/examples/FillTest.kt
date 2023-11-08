package com.android.designcompose.testapp.validation.examples

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// TEST Fill Container test
// The outer black frame should fill the whole screen. Within the black frame there should be a top
// and bottom blue frame of equal size that both stretch to fill the black frame. Each of those
// frames should have additional frames that also stretch in both directions.
@DesignDoc(id = "dB3q96FkxkTO4czn5NqnxV")
interface FillTest {
    @DesignComponent(node = "#stage") fun MainFrame()
}

@Composable
fun FillTest() {
    FillTestDoc.MainFrame(Modifier.fillMaxSize())
}

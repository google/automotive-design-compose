package com.android.designcompose.testapp.validation.examples

import android.util.Log
import androidx.compose.runtime.Composable
import com.android.designcompose.TapCallback
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// TEST fancy fill types (solid color, gradients, images) on text, frames, and strokes
@DesignDoc(id = "xQ9cunHt8VUm6xqJJ2Pjb2")
interface FancyFillTest {
    @DesignComponent(node = "#stage") fun MainFrame(@Design(node = "#xyz") onTap: TapCallback)
}

@Composable
fun FancyFillTest() {
    FancyFillTestDoc.MainFrame(onTap = { Log.e("onTap", "frame clicked!") })
}

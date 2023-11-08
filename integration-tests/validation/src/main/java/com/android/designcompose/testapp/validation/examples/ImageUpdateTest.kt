package com.android.designcompose.testapp.validation.examples

import androidx.compose.runtime.Composable
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// TEST Image Update Test. After this loads, rename #Stage in the Figma doc. After the app
// updates,
// rename it back to #Stage. The image should reload correctly.
@DesignDoc(id = "oQw7kiy94fvdVouCYBC9T0")
interface ImageUpdateTest {
    @DesignComponent(node = "#Stage") fun Main() {}
}

@Composable
fun ImageUpdateTest() {
    ImageUpdateTestDoc.Main()
}

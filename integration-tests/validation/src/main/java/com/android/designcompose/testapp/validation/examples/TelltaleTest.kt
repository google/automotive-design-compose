package com.android.designcompose.testapp.validation.examples

import androidx.compose.runtime.Composable
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// TEST Telltale Test. Tests that rendering telltales as frames and as components is correct.
// When visibility is set to true, the telltales rendered in the app should match the #Main frame
// in the Figma document.
@DesignDoc(id = "TZgHrKWx8wvQM7UPTyEpmz")
interface TelltaleTest {
    @DesignComponent(node = "#Main")
    fun Main(
        @Design(node = "#left_f") leftFrame: Boolean,
        @Design(node = "#seat_f") seatFrame: Boolean,
        @Design(node = "#left_i") leftInstance: Boolean,
        @Design(node = "#seat_i") seatInstance: Boolean,
        @Design(node = "#low_i") lowInstance: Boolean,
        @Design(node = "#brights_i") brightsInstance: Boolean,
    )
}

@Composable
fun TelltaleTest() {
    TelltaleTestDoc.Main(
        leftFrame = true,
        seatFrame = true,
        leftInstance = true,
        seatInstance = true,
        lowInstance = true,
        brightsInstance = true
    )
}

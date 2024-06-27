/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.designcompose.testapp.validation.examples

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import kotlinx.coroutines.delay

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
        @Design(node = "#LeftBlinker") leftBlinker: State<Boolean>,
    )
}

@Composable
fun TelltaleTest() {
    val visible = remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            println("Swap")
            delay(1000)
            visible.value = !visible.value
        }
    }

    println("Main Recompose")
    TelltaleTestDoc.Main(
        leftFrame = true,
        seatFrame = true,
        leftInstance = true,
        seatInstance = true,
        lowInstance = true,
        brightsInstance = true,
        leftBlinker = visible
    )
}

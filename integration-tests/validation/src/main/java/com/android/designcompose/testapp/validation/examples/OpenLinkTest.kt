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

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.android.designcompose.OpenLinkCallback
import com.android.designcompose.ReplacementContent
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignVariant

// TEST Open Link Test. Tests that the open link interaction works on frames and components. Tap
// the squares and watch the output. Tapping the "Swap" button should change the behavior of the
// taps.
enum class SquareColor {
    Red,
    Green,
    Blue,
}

enum class SquareShadow {
    On,
    Off,
}

@DesignDoc(id = "r7m4tqyKv6y9DWcg7QBEDf")
interface OpenLinkTest {
    @DesignComponent(node = "#MainFrame")
    fun MainFrame(
        @Design(node = "#Name") name: String,
        @Design(node = "#Content") content: ReplacementContent,
        @Design(node = "#Swap") clickSwap: Modifier,
    )

    @DesignComponent(node = "#Red") fun Red() {}

    @DesignComponent(node = "#Green") fun Green() {}

    @DesignComponent(node = "#Blue") fun Blue() {}

    @DesignComponent(node = "#PurpleCircle") fun PurpleCircle() {}

    @DesignComponent(node = "#SquareShadow")
    fun Square(
        @DesignVariant(property = "#SquareShadow") shadow: SquareShadow,
        @DesignVariant(property = "#SquareColor") color: SquareColor,
        @Design(node = "#icon") icon: ReplacementContent,
    )
}

@Composable
fun OpenLinkTest() {
    val openLinkOne = OpenLinkCallback { url -> Log.i("DesignCompose", "Open Link ONE: $url") }
    val openLinkTwo = OpenLinkCallback { url -> Log.i("DesignCompose", "Open Link TWO: $url") }
    val (useFuncOne, setUseFuncOne) = remember { mutableStateOf(true) }
    val openLinkFunc =
        if (useFuncOne) {
            openLinkOne
        } else {
            openLinkTwo
        }

    OpenLinkTestDoc.MainFrame(
        name = "Rob",
        openLinkCallback = openLinkFunc,
        content =
            ReplacementContent(
                count = 5,
                content = { index ->
                    {
                        when (index) {
                            0 -> OpenLinkTestDoc.Red()
                            1 -> OpenLinkTestDoc.Green()
                            2 -> OpenLinkTestDoc.Blue()
                            3 ->
                                OpenLinkTestDoc.Square(
                                    shadow = SquareShadow.On,
                                    color = SquareColor.Green,
                                    icon =
                                        ReplacementContent(
                                            count = 1,
                                            content = { { OpenLinkTestDoc.PurpleCircle() } },
                                        ),
                                )
                            else ->
                                OpenLinkTestDoc.Square(
                                    shadow = SquareShadow.On,
                                    color = SquareColor.Red,
                                    icon =
                                        ReplacementContent(
                                            count = 1,
                                            content = { { OpenLinkTestDoc.PurpleCircle() } },
                                        ),
                                )
                        }
                    }
                },
            ),
        clickSwap = Modifier.clickable { setUseFuncOne(!useFuncOne) },
    )
}

/*
 * Copyright 2024 Google LLC
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

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.android.designcompose.MeterState
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import java.time.Clock
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@DesignDoc(id = "POWyniB6moGRmhZTJyejwa")
interface StateCustomizations {
    @DesignComponent(node = "#root")
    fun root(
        @Design(node = "#time") time: androidx.compose.runtime.State<String>,
        @Design(node = "#firstHand") firstHand: MeterState,
        @Design(node = "#secondHand") secondHand: MeterState,
    )
}

@Composable
fun StateCustomizationsTest(clock: Clock) {
    val time = remember { mutableStateOf("00:00:00") }
    val firstHand = remember { mutableStateOf<Float?>(0F) }
    val secondHand = remember { mutableStateOf<Float?>(0F) }

    val handler = Handler(Looper.getMainLooper())
    val runnable =
        object : Runnable {
            override fun run() {
                val currentTime = LocalTime.now(clock)
                val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                val formattedTime = currentTime.format(formatter)
                time.value = formattedTime
                firstHand.value = (currentTime.hour % 12 + currentTime.minute / 60F) * 100 / 12F
                secondHand.value = currentTime.minute * 100 / 60F
                handler.postDelayed(this, 1000) // Update every second
            }
        }
    handler.post(runnable)
    StateCustomizationsDoc.root(time = time, firstHand = firstHand, secondHand = secondHand)
}

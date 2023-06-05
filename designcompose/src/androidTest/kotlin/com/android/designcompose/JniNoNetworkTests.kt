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

package com.android.designcompose

import android.net.ConnectivityManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import kotlin.test.assertFailsWith
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.anyOf
import org.hamcrest.core.IsInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test

class JniNoNetworkTests {
    @Before
    fun setup() {
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand("cmd connectivity airplane-mode enable")
        // Add a sleep to make sure the connection goes down.
        val connectivityManager =
            getSystemService(
                InstrumentationRegistry.getInstrumentation().context,
                ConnectivityManager::class.java
            )
        println(connectivityManager?.isDefaultNetworkActive)
        println("Hii")

        wait
    }
    @Test
    fun networkFailure() {
        val exception =
            assertFailsWith<IOException> {
                LiveUpdateJni.jniFetchDoc("DummyDocId", dummyFigmaTokenJson)
            }
        assertThat(
            exception,
            anyOf(
                IsInstanceOf(ConnectException::class.java),
                IsInstanceOf(SocketException::class.java)
            )
        )
    }
    @After
    fun teardown() {
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand("cmd connectivity airplane-mode disable")
    }
}

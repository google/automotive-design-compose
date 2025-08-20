/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.designcompose

import android.app.Service
import android.content.Intent
import com.android.designcompose.ApiKeyService.Companion.ACTION_SET_API_KEY
import com.android.designcompose.ApiKeyService.Companion.EXTRA_SET_API_KEY
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class ApiKeyServiceTest {
    private lateinit var service: ApiKeyService
    private lateinit var dispatcher: TestDispatcher
    private val liveUpdateSettings: LiveUpdateSettingsRepository = mockk()
    private lateinit var controller: ServiceController<ApiKeyService>

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        controller = Robolectric.buildService(ApiKeyService::class.java)
        service = controller.get()
        service.dispatcher = dispatcher
        service.liveUpdateSettings = liveUpdateSettings
        coEvery { liveUpdateSettings.setFigmaApiKey(any()) } returns Unit
    }

    @Test
    fun testOnBind() =
        runTest(dispatcher) {
            val intent = Intent()
            intent.action = ACTION_SET_API_KEY
            intent.putExtra(EXTRA_SET_API_KEY, "TEST_API_KEY")
            val binder = service.onBind(intent)
            assertThat(binder).isNotNull()
            assertThat(binder).isInstanceOf(ApiKeyService.ApiKeyBinder::class.java)

            // Manually run the pending coroutine
            dispatcher.scheduler.runCurrent()

            coVerify(exactly = 1) { liveUpdateSettings.setFigmaApiKey("TEST_API_KEY") }
        }

    @Test
    fun testOnStartCommand() =
        runTest(dispatcher) {
            val intent = Intent()
            intent.action = ACTION_SET_API_KEY
            intent.putExtra(EXTRA_SET_API_KEY, "TEST_API_KEY")
            val result = service.onStartCommand(intent, 0, 0)
            assertThat(result).isEqualTo(Service.START_NOT_STICKY)

            // Manually run the pending coroutine
            dispatcher.scheduler.runCurrent()

            coVerify(exactly = 1) { liveUpdateSettings.setFigmaApiKey("TEST_API_KEY") }
        }

    @Test
    fun testSetApiKey() =
        runTest(dispatcher) {
            service.setApiKey("TEST_API_KEY")

            // Manually run the pending coroutine
            dispatcher.scheduler.runCurrent()

            coVerify(exactly = 1) { liveUpdateSettings.setFigmaApiKey("TEST_API_KEY") }
        }

    @Test
    fun testSetApiKeyNull() =
        runTest(dispatcher) {
            service.setApiKey(null)
            // No coroutine is launched here, so no need to advance the scheduler.
            coVerify(exactly = 0) { liveUpdateSettings.setFigmaApiKey(any()) }
        }
}

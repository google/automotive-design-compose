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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class LiveUpdateSettingsRepositoryTest {
    @Test
    fun testSetFigmaApiKey() = runTest {
        val dataStore = mockk<DataStore<Preferences>>()

        // 1. Mock the data flow accessed in the repository's init block.
        every { dataStore.data } returns flowOf(emptyPreferences())

        // 2. Mock the suspend function for the setFigmaApiKey call.
        coEvery { dataStore.updateData(any()) } returns emptyPreferences()

        val repository = LiveUpdateSettingsRepository(dataStore)
        val newKey = "newKey"
        repository.setFigmaApiKey(newKey)

        // Verify the underlying `updateData` call.
        coVerify { dataStore.updateData(any()) }
    }
}

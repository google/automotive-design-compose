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

import androidx.annotation.RestrictTo
import com.google.common.annotations.VisibleForTesting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * For use in tests only, restricted to use via the test fixture.
 *
 * Enable Live Update using a given Figma Token. This skips the LiveUpdateSettings setup and
 * coroutine flows associated with it, which simplifies its use in tests.
 *
 * @param token: The Figma Access Token to authenticate with
 */
@VisibleForTesting
@RestrictTo(RestrictTo.Scope.LIBRARY)
fun testOnlyTriggerLiveUpdate(token: String) {
    DesignSettings.figmaApiKeyStateFlow = MutableStateFlow(token)
    DesignSettings.liveUpdatesEnabled = true
    DesignSettings.isDocumentLive = flowOf(true)
    DocServer.fetchDocuments(true)
}

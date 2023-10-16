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

package com.android.designcompose.cargoplugin

import com.google.common.truth.Truth.assertThat
import org.gradle.api.GradleException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AbiFilterTests {
    private val defaultConfiguredAbis = setOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a")

    @Test
    fun androidStudioInjectedTwoAbis() {
        assertThat(selectActiveAbis(defaultConfiguredAbis, "x86_64,arm64-v8a", null))
            .containsExactly("x86_64")
    }

    @Test
    fun androidStudioInjectedOneAbi() {
        assertThat(selectActiveAbis(defaultConfiguredAbis, "arm64-v8a", null))
            .containsExactly("arm64-v8a")
    }

    @Test
    fun androidStudioInjectedUnknownAbi() {
        assertThrows<GradleException> { selectActiveAbis(defaultConfiguredAbis, "unknown", null) }
    }

    @Test
    fun overrideOneAbi() {
        assertThat(selectActiveAbis(defaultConfiguredAbis, null, "x86_64"))
            .containsExactly("x86_64")
    }

    @Test
    fun overrideThreeAbis() {
        assertThat(selectActiveAbis(defaultConfiguredAbis, null, "x86,arm64-v8a,x86_64"))
            .containsExactly("x86", "arm64-v8a", "x86_64")
    }

    @Test
    fun overrideWithInvalid() {
        assertThrows<GradleException> {
            selectActiveAbis(defaultConfiguredAbis, null, "x86,unknown")
        }
    }
}

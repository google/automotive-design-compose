/*
 * Copyright 2025 Google LLC
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

package com.android.designcompose.utils

import com.android.designcompose.definition.interaction.Trigger
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.Empty
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InteractionUtilsTest {
    @Test
    fun testIsSupportedInteraction() {
        val pressTrigger = Trigger.newBuilder().setPress(Empty.getDefaultInstance()).build()
        assertThat(pressTrigger.isSupportedInteraction()).isTrue()

        val clickTrigger = Trigger.newBuilder().setClick(Empty.getDefaultInstance()).build()
        assertThat(clickTrigger.isSupportedInteraction()).isTrue()

        val afterTimeoutTrigger =
            Trigger.newBuilder()
                .setAfterTimeout(Trigger.Timeout.newBuilder().setTimeout(100F))
                .build()
        assertThat(afterTimeoutTrigger.isSupportedInteraction()).isTrue()

        val dragTrigger = Trigger.newBuilder().setDrag(Empty.getDefaultInstance()).build()
        assertThat(dragTrigger.isSupportedInteraction()).isFalse()
    }
}

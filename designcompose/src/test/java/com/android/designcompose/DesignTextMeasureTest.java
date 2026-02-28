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

package com.android.designcompose;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import kotlin.Pair;

import static org.junit.Assert.assertEquals;

import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class DesignTextMeasureTest {

    @Test
    public void testMeasureTextSize() {
        try (MockedStatic<DesignTextKt> mocked = Mockito.mockStatic(DesignTextKt.class)) {
            mocked.when(() -> DesignTextKt.measureTextBoundsFunc(1, 10f, 20f, 100f, 200f))
                    .thenReturn(new Pair<>(50f, 60f));

            TextSize textSize = DesignTextMeasure.measureTextSize(1, 10f, 20f, 100f, 200f);

            assertEquals(50f, textSize.getWidth(), 0.0f);
            assertEquals(60f, textSize.getHeight(), 0.0f);
        }
    }
}

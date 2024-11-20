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

package com.android.designcompose;

import static com.android.designcompose.TestUtilsKt.testOnlyTriggerLiveUpdate;

import android.content.res.Resources;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class TestUtils {

    /**
     * triggerLiveUpdate
     * <p>
     * Reads a Figma token from the instrumentation registry and
     * uses it to trigger a one-off Live Update fetch
     */
    public static void triggerLiveUpdate() {
        String actualFigmaToken =
                InstrumentationRegistry.getArguments().getString("FIGMA_ACCESS_TOKEN");
        // Check for a system property as a backup
        if (actualFigmaToken == null)
            actualFigmaToken = System.getProperty("designcompose.test.figmaToken");
        if (actualFigmaToken == null)
            throw new RuntimeException("This test requires a Figma Access Token");

        testOnlyTriggerLiveUpdate(actualFigmaToken);
    }

    private static void clearInteractionStates() {
        InteractionStateManager.INSTANCE.getStates().clear();
    }

    private static void clearDocServer() {
        DocServer.INSTANCE.testOnlyClearDocuments();
    }

    private static void clearDesignSettings() {
        DesignSettings.INSTANCE.testOnlyClearFileFetchStatus();
        DesignSettings.INSTANCE.clearRawResources();
    }


    public static class ClearStateTestRule implements TestRule {

        @Override
        public Statement apply(Statement base, Description description) {
            clearInteractionStates();
            clearDocServer();
            clearDesignSettings();
            return base;
        }
    }
}
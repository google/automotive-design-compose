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

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

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
        DesignSettings.INSTANCE.setRawResourceId(Resources.ID_NULL);
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

    public static class LiveUpdateTestRule implements TestRule {
        private final String dcfOutPath;
        private final Boolean runFigmaFetch;

        public LiveUpdateTestRule() {
            dcfOutPath = System.getProperty("designcompose.test.dcfOutPath");
            runFigmaFetch = Boolean.valueOf(System.getProperty("designcompose.test.fetchFigma"));
        }

        @Override
        public Statement apply(Statement base, Description description) {
            return base;
        }

        public void performLiveFetch() {
            if (runFigmaFetch) {
                performLiveFetch(dcfOutPath);
            }
        }

        private void performLiveFetch(@Nullable String dcfOutPath) {
            if (dcfOutPath == null)
                throw new RuntimeException("designcompose.test.dcfOutPath not set");
            triggerLiveUpdate();
            Context context = ApplicationProvider.getApplicationContext();
            Arrays.stream(context.fileList()).filter(it -> it.endsWith(".dcf"))
                    .forEach(it -> {
                        File filepath = new File(context.getFilesDir().getAbsolutePath(), it);
                        try {
                            Files.copy(filepath.toPath(),
                                    new File(dcfOutPath, it).toPath(),
                                    StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}

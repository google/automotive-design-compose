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

package designcompose.conventions

import com.android.build.gradle.BaseExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins { id("io.github.takahirom.roborazzi") }

project.pluginManager.withPlugin("com.android.base") {
    project.extensions.getByType(BaseExtension::class.java).apply {
        testOptions.unitTests {
            isIncludeAndroidResources = true // For Roborazzi
            all {
                with(it) {
                    minHeapSize = "128m"
                    maxHeapSize = "1024m"
                    // Run the tests in parallel
                    maxParallelForks =
                        (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)

                    testLogging {
                        events("passed", "failed")
                        showExceptions = true
                        showStackTraces = true
                        showCauses = true
                        showStandardStreams = true
                        exceptionFormat = TestExceptionFormat.FULL
                    }
                }
            }
        }
    }
}

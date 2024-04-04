/*
 * Copyright 2024 Google LLC
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

import androidx.compose.runtime.compositionLocalOf

/**
 * When applied as a compositionLocal, these settings will alter features or add functionality to
 * DesignCompose Composables
 *
 * @property useSquoosh: Enable use of the Squoosh renderer. Squoosh implements its own view tree,
 *   rather than using Compose, which brings some performance and flexibility benefits. Squoosh
 *   isn't feature complete (no scrolling, no lists, no transformed input), but it does add
 *   animations and is likely the direction that DesignCompose will move in to be lighter weight and
 *   better integrate with external layout.
 */
class DesignDocSettings(
    val useSquoosh: Boolean = false,
)

val LocalDesignDocSettings = compositionLocalOf { DesignDocSettings() }

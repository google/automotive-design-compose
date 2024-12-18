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
import com.android.designcompose.squoosh.CustomVariantTransition

/**
 * When applied as a compositionLocal, these settings will alter features or add functionality to
 * DesignCompose Composables
 */
class DesignDocSettings(val customVariantTransition: CustomVariantTransition? = null)

val LocalDesignDocSettings = compositionLocalOf { DesignDocSettings() }

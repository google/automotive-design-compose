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

// If true, allow the abi list to be overriden by Android Studio (first) and by the overrideAbi
// property (second)
const val PROPERTY_ALLOW_ABI_OVERRIDE = "designcompose.cargoPlugin.allowAbiOverride"
// Comma separated list of abis to build instead of the configured ones. Must be a subset of the
// configured ABIs.
const val PROPERTY_ABI_FILTER = "designcompose.cargoPlugin.abiFilter"

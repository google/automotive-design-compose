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

package com.android.designcompose.common

/**
 * Id for the figma design doc with file id and version id. When version id is not specified, it
 * will load head of the figma doc.
 */
data class DesignDocId(var id: String, var versionId: String = "") {
    fun isValid(): Boolean {
        return id.isNotEmpty()
    }

    override fun toString(): String {
        return if (versionId.isEmpty()) id else "${id}_$versionId"
    }
}

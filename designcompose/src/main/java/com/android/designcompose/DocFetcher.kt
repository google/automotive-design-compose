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

package com.android.designcompose

import com.android.designcompose.common.DocumentServerParams
import com.android.designcompose.common.GenericDocContent

/*
 *  Constructs the Json request to Figma for the document update
 */
fun constructPostJson(
    figmaApiKey: String,
    previousDoc: GenericDocContent?,
    params: DocumentServerParams,
    first: Boolean = true,
): String {
    val lastModified = previousDoc?.header?.last_modified
    val version = previousDoc?.header?.response_version
    val imageSession = previousDoc?.imageSession

    var postData = "{ "
    postData += "\"figma_api_key\": \"$figmaApiKey\","

    if (!first && lastModified != null) { // Force an update on the first run
        postData += "\"last_modified\": \"$lastModified\", "
    }
    if (version != null) {
        postData += "\"version\": \"$version\", "
    }
    if (imageSession != null) {
        postData += "\"image_session\": $imageSession, "
    } else {
        postData += "\"image_session\": {}, "
    }

    postData += params.toJsonSnippet()

    postData += "}"
    return postData
}

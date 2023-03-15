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

package com.android.designcompose.common

// Current serialized doc version
const val FSAAS_DOC_VERSION = 11

// This has been commented out since it was only used by the gradle preview plugin which currently
// has been disabled. If we bring the plugin back we may need to uncomment this code so that the
// plugin can log feedback messages.
/*
// Design Compose service to transform Figma docs into Serialized View Trees (Figma Serialization
// as a Service)
// Change this to your local IP address to test your own build of figma_import_webserver, e.g.:
//  "http://192.168.6.67:8000/"

// The current default URL is for the latest successful build to master.
// During automated builds this is overwritten with the address for the service built from the same
// branch
const val DEFAULT_FSAAS_URL = "https://fsaas-snapshot-jpk7if5t3a-uc.a.run.app"

class FsaasSession(
  private val authToken: String,
  private val id: String,
) {
  var fsaasURL: String = DEFAULT_FSAAS_URL
    private set
  private lateinit var connection: HttpURLConnection
  lateinit var connectionURL: URL
    private set
  lateinit var receivedDocBytes: ByteArray
  var errorBytes: String? = null

  fun setServiceURL(url: String) {
    fsaasURL = url
  }

  fun openConnection() {
    val endpoint = "$fsaasURL/doc/$id"
    connectionURL = URL(endpoint)
    connection = connectionURL.openConnection() as HttpURLConnection
    connection.requestMethod = "POST"
    connection.connectTimeout = 30 * 1000 // 30 sec
  }

  fun constructPostJson(
    previousDoc: GenericDocContent?,
    params: DocumentServerParams,
    first: Boolean = true,
  ): String {
    val lastModified = previousDoc?.document?.last_modified
    val version = previousDoc?.document?.version
    val imageSession = previousDoc?.imageSession

    var postData = "{ "
    postData += "\"figma_api_key\": \"$authToken\","

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

  fun sendMessage(
    postData: String,
  ) {
    val encPostData = postData.toByteArray(StandardCharsets.UTF_8)
    connection.setRequestProperty("charset", "utf-8")
    connection.setRequestProperty("Content-Length", encPostData.size.toString())
    connection.setRequestProperty("Content-Type", "application/json")
    connection.doOutput = true // Required by the basic Java library

    val outputStream = DataOutputStream(connection.outputStream)
    outputStream.write(encPostData)
    outputStream.flush()
  }

  fun pendOnResponse(): Int {
    val responseCode =
      when (val rc = connection.responseCode) {
        200 -> {
          receivedDocBytes = readDocBytes(connection.inputStream, id)
          rc
        }
        304 -> rc
        else -> {
          errorBytes = readErrorBytes(connection.errorStream)
          rc
        }
      }
    connection.disconnect()
    return responseCode
  }
}
*/

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

package com.android.designcompose.testapp.validation.examples

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.designcompose.DesignComposeCallbacks
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.testapp.validation.R

// TEST Basic Hello World example
@DesignDoc(id = "1OqNt1MCCc4E8KXS0OVcxS")
interface HelloWorld {
    @DesignComponent(node = "#stage") fun Main(@Design(node = "#Name") name: String)
}

@Composable
fun HelloWorld() {
    HelloWorldDoc.Main(
        name = stringResource(id = R.string.label_world),
        designComposeCallbacks =
            DesignComposeCallbacks(
                docReadyCallback = { id ->
                    Log.i("DesignCompose", "HelloWorld Ready: doc ID = $id")
                },
                newDocDataCallback = { docId, data ->
                    Log.i(
                        "DesignCompose",
                        "HelloWorld Updated doc ID $docId: ${data?.size ?: 0} bytes",
                    )
                },
            ),
    )
}

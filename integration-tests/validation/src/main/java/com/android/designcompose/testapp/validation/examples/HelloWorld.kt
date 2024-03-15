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
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.DesignComposeCallbacks
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// TEST Basic Hello World example
@DesignDoc(id = "pxVlixodJqZL95zo2RzTHl")
interface HelloWorld {
    @DesignComponent(node = "#MainFrame") fun Main(@Design(node = "#Name") name: String)
}

@DesignDoc(id = "x9JgFP4Jfn406130rRc30Y")
interface TestMain {
    @DesignComponent(node = "#stage") fun Main1(
        @Design(node = "#replace-node") replaceNode: @Composable (ComponentReplacementContext) -> Unit,
    )
}

@DesignDoc(id = "x9JgFP4Jfn406130rRc30Y")
interface TestBlue {
    @DesignComponent(node = "#BlueReplacement") fun Node1()
}

@Composable
fun HelloWorld() {
    TestMainDoc.Main1(
        replaceNode = {
            TestBlueDoc.Node1()
        }
    )
}

@Composable
fun HelloWorld2() {
    HelloWorldDoc.Main(
        name = "World",
        designComposeCallbacks =
            DesignComposeCallbacks(
                docReadyCallback = { id ->
                    Log.i("DesignCompose", "HelloWorld Ready: doc ID = $id")
                },
                newDocDataCallback = { docId, data ->
                    Log.i(
                        "DesignCompose",
                        "HelloWorld Updated doc ID $docId: ${data?.size ?: 0} bytes"
                    )
                },
            )
    )
}

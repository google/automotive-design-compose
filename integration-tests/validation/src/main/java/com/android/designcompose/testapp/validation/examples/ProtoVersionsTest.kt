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

import androidx.compose.runtime.Composable
import com.android.designcompose.DesignComposeCallbacks
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.common.headerVersion
import java.io.ByteArrayInputStream

@DesignDoc(id = "49dxlUHfGMXbqmnPqGzVVU")
interface ProtoChange {
    @DesignComponent(node = "#stage") fun Stage()
}

@DesignDoc(id = "49dxlUHfGMXbqmnPqGzVVU")
interface ProtoChangeV27 {
    @DesignComponent(node = "#stage") fun Stage()
}

@Composable
fun ProtoChangeTest(versionCallback: ((Int) -> Unit)? = null) {
    ProtoChangeDoc.Stage(
        designComposeCallbacks =
            DesignComposeCallbacks(
                newDocDataCallback = { docId, data ->
                    data?.let {
                        val docStream = ByteArrayInputStream(it)
                        val dcVersion = headerVersion(docStream)
                        versionCallback?.invoke(dcVersion)
                    }
                }
            )
    )
}

@Composable
fun ProtoChangeTestV27(versionCallback: (Int) -> Unit) {
    ProtoChangeV27Doc.Stage(
        designComposeCallbacks =
            DesignComposeCallbacks(
                newDocDataCallback = { docId, data ->
                    data?.let {
                        val docStream = ByteArrayInputStream(it)
                        val dcVersion = headerVersion(docStream)
                        versionCallback.invoke(dcVersion)
                    }
                }
            )
    )
}

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

package com.android.designcompose.testapp.helloworld

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.designcompose.DesignSettings
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.testapp.helloworld.ui.theme.helloworldTheme
import java.io.File
import java.io.InputStream

const val helloWorldDocId = "pxVlixodJqZL95zo2RzTHl"

@DesignDoc(id = helloWorldDocId)
interface HelloWorld {
    @DesignComponent(node = "#MainFrame") fun mainFrame(@Design(node = "#Name") name: String)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dir = filesDir.path
        val inputStream: InputStream? =
            try {
                File(dir, "HelloWorldDoc_pxVlixodJqZL95zo2RzTHl.dcf").inputStream()
            } catch (e: Exception) {
                null
            }
        DesignSettings.enableLiveUpdates(this)
        setContent { HelloWorldDoc.mainFrame(name = "World!", dcfInputStream = inputStream) }
    }
}

@Preview(showBackground = true, widthDp = 700)
@Composable
fun ComposePreview() {
    helloworldTheme { HelloWorldDoc.mainFrame(name = "Developer!") }
}

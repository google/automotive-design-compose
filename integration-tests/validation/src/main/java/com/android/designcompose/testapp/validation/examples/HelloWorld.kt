package com.android.designcompose.testapp.validation.examples

import android.util.Log
import androidx.compose.runtime.Composable
import com.android.designcompose.DesignComposeCallbacks
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc

// TEST Basic Hello World example
@DesignDoc(id = "pxVlixodJqZL95zo2RzTHl")
interface HelloWorld {
    @DesignComponent(node = "#MainFrame") fun Main(@Design(node = "#Name") name: String)
}

@Composable
fun HelloWorld() {
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

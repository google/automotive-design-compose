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

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// To set a key:
// adb shell am startservice -a setFigmaKey -n \
// <YOUR_PACKAGE_NAME/com.android.designcompose.ApiKeyService -e ApiKey \
// $FIGMA_ACCESS_TOKEN

// Example for HelloWorld:
// adb shell am startservice -a setFigmaKey -n
// com.android.designcompose.testapp.helloworld/com.android.designcompose.ApiKeyService -e ApiKey
// $FIGMA_ACCESS_TOKEN

// Need to rename all of this to use "Access Token" instead of "Auth Key". This will match Figma's
// terminology: https://help.figma.com/hc/en-us/articles/8085703771159-Manage-personal-access-tokens
public var ACTION_SET_API_KEY = "setApiKey"
public var EXTRA_SET_API_KEY = "ApiKey"

class ApiKeyService : Service() {

    private val job = SupervisorJob()

    @VisibleForTesting var dispatcher = Dispatchers.IO

    inner class ApiKeyBinder : Binder() {
        fun getService(): ApiKeyService = this@ApiKeyService
    }

    private val binder = ApiKeyBinder()

    override fun onBind(intent: Intent): IBinder {
        if (intent.action == "setFigmaKey") {
            setApiKey(intent.getStringExtra("ApiKey"))
        }
        return binder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action == ACTION_SET_API_KEY) {
            intent.getStringExtra(EXTRA_SET_API_KEY)?.let { key -> setApiKey(key) }
        }
        stopSelf()
        return START_NOT_STICKY
    }

    fun setApiKey(key: String?) {
        if (key != null) {
            CoroutineScope(dispatcher).launch {
                DesignSettings.liveUpdateSettings?.setFigmaApiKey(key)
            }
        }
    }
}

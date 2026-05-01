/*
 * Copyright 2026 Google LLC
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
import android.util.Log
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

var ACTION_SET_API_KEY = "setApiKey"
var EXTRA_SET_API_KEY = "ApiKey"
var ACTION_ENABLE_LIVE_UPDATE = "enableLiveUpdate"
var EXTRA_ENABLE_LIVE_UPDATE = "Enabled"
var ACTION_SET_AUTOPAUSE_TIMEOUT = "setAutopauseTimeout"
var EXTRA_AUTOPAUSE_TIMEOUT = "TimeoutMs"
var ACTION_SET_LIVE_UPDATE_FETCH_MILLIS = "setLiveUpdateFetchMillis"
var EXTRA_LIVE_UPDATE_FETCH_MILLIS = "FetchIntervalMs"
var ACTION_ENABLE_ADAPTIVE_POLLING = "enableAdaptivePolling"
var EXTRA_ADAPTIVE_POLLING_ENABLED = "Enabled"
var ACTION_SET_INCREMENTAL_THRESHOLD = "setIncrementalThreshold"
var EXTRA_INCREMENTAL_THRESHOLD = "Threshold"
var ACTION_SET_DISCOVER_ALL_NODES = "setDiscoverAllNodes"
var EXTRA_DISCOVER_ALL_NODES = "Enabled"
var ACTION_SET_WEBSOCKET = "setWebSocket"
var EXTRA_USE_WEBSOCKET = "Enabled"
var ACTION_SET_WEBSOCKET_URL = "setWebSocketUrl"
var EXTRA_WEBSOCKET_URL = "Url"

class ApiKeyService : Service() {

    private val job = SupervisorJob()

    @VisibleForTesting var dispatcher = Dispatchers.IO

    inner class ApiKeyBinder : Binder() {
        fun getService(): ApiKeyService = this@ApiKeyService
    }

    private val binder = ApiKeyBinder()

    private fun processIntent(intent: Intent) {
        val action = intent.action
        val valueString =
            when (action) {
                ACTION_SET_API_KEY,
                "setFigmaKey" -> ""
                ACTION_ENABLE_LIVE_UPDATE ->
                    ", value: ${intent.getBooleanExtra(EXTRA_ENABLE_LIVE_UPDATE, true)}"
                ACTION_SET_AUTOPAUSE_TIMEOUT ->
                    ", value: ${intent.getLongExtra(EXTRA_AUTOPAUSE_TIMEOUT, -1)}"
                ACTION_SET_LIVE_UPDATE_FETCH_MILLIS ->
                    ", value: ${intent.getLongExtra(EXTRA_LIVE_UPDATE_FETCH_MILLIS, -1)}"
                ACTION_ENABLE_ADAPTIVE_POLLING ->
                    ", value: ${intent.getBooleanExtra(EXTRA_ADAPTIVE_POLLING_ENABLED, true)}"
                ACTION_SET_INCREMENTAL_THRESHOLD ->
                    ", value: ${intent.getFloatExtra(EXTRA_INCREMENTAL_THRESHOLD, -1f)}"
                ACTION_SET_DISCOVER_ALL_NODES ->
                    ", value: ${intent.getBooleanExtra(EXTRA_DISCOVER_ALL_NODES, false)}"
                ACTION_SET_WEBSOCKET ->
                    ", value: ${intent.getBooleanExtra(EXTRA_USE_WEBSOCKET, false)}"
                ACTION_SET_WEBSOCKET_URL -> ", value: ${intent.getStringExtra(EXTRA_WEBSOCKET_URL)}"
                else -> ""
            }
        Log.i(TAG, "ApiKeyService received intent action: $action$valueString")

        when (action) {
            ACTION_SET_API_KEY,
            "setFigmaKey" -> {
                setApiKey(
                    intent.getStringExtra(EXTRA_SET_API_KEY) ?: intent.getStringExtra("ApiKey")
                )
            }
            ACTION_ENABLE_LIVE_UPDATE -> {
                setLiveUpdateEnabled(intent.getBooleanExtra(EXTRA_ENABLE_LIVE_UPDATE, true))
            }
            ACTION_SET_AUTOPAUSE_TIMEOUT -> {
                val timeout = intent.getLongExtra(EXTRA_AUTOPAUSE_TIMEOUT, -1)
                if (timeout != -1L) setAutopauseTimeout(timeout)
            }
            ACTION_SET_LIVE_UPDATE_FETCH_MILLIS -> {
                val interval = intent.getLongExtra(EXTRA_LIVE_UPDATE_FETCH_MILLIS, -1)
                if (interval != -1L) setLiveUpdateFetchMillis(interval)
            }
            ACTION_ENABLE_ADAPTIVE_POLLING -> {
                setAdaptivePollingEnabled(
                    intent.getBooleanExtra(EXTRA_ADAPTIVE_POLLING_ENABLED, true)
                )
            }
            ACTION_SET_INCREMENTAL_THRESHOLD -> {
                val threshold = intent.getFloatExtra(EXTRA_INCREMENTAL_THRESHOLD, -1f)
                if (threshold in 0f..1f) setIncrementalThreshold(threshold)
            }
            ACTION_SET_DISCOVER_ALL_NODES -> {
                setDiscoverAllNodes(intent.getBooleanExtra(EXTRA_DISCOVER_ALL_NODES, false))
            }
            ACTION_SET_WEBSOCKET -> {
                setWebSocketEnabled(intent.getBooleanExtra(EXTRA_USE_WEBSOCKET, false))
            }
            ACTION_SET_WEBSOCKET_URL -> {
                val url = intent.getStringExtra(EXTRA_WEBSOCKET_URL)
                if (url != null) setWebSocketUrl(url)
            }
            else -> {
                Log.w(TAG, "ApiKeyService received unknown intent action: $action")
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        processIntent(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        processIntent(intent)
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

    fun setLiveUpdateEnabled(enabled: Boolean) {
        CoroutineScope(dispatcher).launch {
            DesignSettings.liveUpdateSettings?.setLiveUpdateEnabled(enabled)
        }
    }

    fun setAutopauseTimeout(timeoutMs: Long) {
        CoroutineScope(dispatcher).launch {
            DesignSettings.liveUpdateSettings?.setLiveUpdateTimeout(timeoutMs)
        }
    }

    fun setLiveUpdateFetchMillis(intervalMs: Long) {
        CoroutineScope(dispatcher).launch {
            DesignSettings.liveUpdateSettings?.setLiveUpdateFetchMillis(intervalMs)
        }
    }

    fun setAdaptivePollingEnabled(enabled: Boolean) {
        CoroutineScope(dispatcher).launch {
            DesignSettings.liveUpdateSettings?.setAdaptivePollingEnabled(enabled)
        }
    }

    fun setIncrementalThreshold(threshold: Float) {
        Log.i(TAG, "Setting incremental threshold to ${(threshold * 100).toInt()}%")
        DesignSettings.incrementalThreshold.value = threshold
    }

    fun setDiscoverAllNodes(enabled: Boolean) {
        Log.i(TAG, "Setting discover all top-level nodes to $enabled")
        DesignSettings.discoverAllTopLevelNodes.value = enabled
    }

    fun setWebSocketEnabled(enabled: Boolean) {
        Log.i(
            TAG,
            "Setting WebSocket mode to $enabled (url=${DesignSettings.webSocketRelayUrl.value})",
        )
        DesignSettings.useWebSocket.value = enabled
        // Restart live updates with the new mode
        if (DesignSettings.liveUpdatesEnabled) {
            DocServer.stopLiveUpdates()
            DocServer.startLiveUpdates()
        }
    }

    fun setWebSocketUrl(url: String) {
        Log.i(TAG, "Setting WebSocket relay URL to $url")
        DesignSettings.webSocketRelayUrl.value = url
    }
}

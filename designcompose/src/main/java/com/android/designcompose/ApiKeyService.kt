/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ApiKeyService : Service() {

    private val job = SupervisorJob()
    private val scope by lazy { CoroutineScope(dispatcher + job) }

    @VisibleForTesting var dispatcher = Dispatchers.IO

    inner class ApiKeyBinder : Binder() {
        fun getService(): ApiKeyService = this@ApiKeyService
    }

    private val binder = ApiKeyBinder()

    @VisibleForTesting
    internal var liveUpdateSettings: LiveUpdateSettingsRepository? =
        DesignSettings.liveUpdateSettings

    override fun onBind(intent: Intent): IBinder {
        if (intent.action == ACTION_SET_API_KEY) {
            setApiKey(intent.getStringExtra(EXTRA_SET_API_KEY))
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

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    // This remains the public entry point that launches the coroutine
    fun setApiKey(key: String?) {
        if (key != null) {
            scope.launch { setFigmaApiKeySuspend(key) }
        }
    }

    // This new suspend function contains the core logic and is easy to test directly
    @VisibleForTesting
    internal suspend fun setFigmaApiKeySuspend(key: String) {
        liveUpdateSettings?.setFigmaApiKey(key)
    }

    companion object {
        const val ACTION_SET_API_KEY = "com.android.designcompose.ACTION_SET_API_KEY"
        const val EXTRA_SET_API_KEY = "com.android.designcompose.EXTRA_SET_API_KEY"
    }
}

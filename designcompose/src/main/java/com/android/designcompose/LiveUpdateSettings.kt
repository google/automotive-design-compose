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

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import io.github.osipxd.security.crypto.createEncrypted
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class LiveUpdateSettings private constructor(context: Context) {
    private val liveUpdateTag = "LiveUpdateSettings"
    private val figmaApiPrefKey = stringPreferencesKey("FigmaApiKey")

    // The DataStore itself, created using an encrypted backing file
    private var settingsDataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.createEncrypted {
            EncryptedFile(
                context = context,
                file = context.dataStoreFile("designcompose_liveupdate_settings.preferences_pb"),
                masterKey = MasterKey(context)
            )
        }

    // Flow that watches the FigmaApiKey for new updates and returns the new value
    // to watchers
    internal val settingsUpdateFlow: Flow<String?> =
        settingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.e(liveUpdateTag, "Error reading LiveUpdate Settings.", exception)
                    emit(emptyPreferences())
                } else {
                    Log.e(liveUpdateTag, "ERROR")
                    throw exception
                }
            }
            .map { settings ->
                if (settings[figmaApiPrefKey] == "") null else settings[figmaApiPrefKey]
            }

    // DataStores need to be singletons
    companion object {
        private var instance: LiveUpdateSettings? = null
        fun getInstance(context: Context): LiveUpdateSettings {
            if (instance == null) {
                instance = LiveUpdateSettings(context)
            }
            return instance!!
        }
    }

    // Expose the function to set the key
    suspend fun setFigmaApiKey(newKey: String) {
        settingsDataStore.edit { it[figmaApiPrefKey] = newKey }
    }
}

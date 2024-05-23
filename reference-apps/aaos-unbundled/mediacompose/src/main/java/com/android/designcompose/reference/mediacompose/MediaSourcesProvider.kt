/*
 * Copyright 2024 Google LLC
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

package com.android.designcompose.reference.mediacompose

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.service.media.MediaBrowserService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.car.media.common.source.MediaSource
import java.util.Objects
import java.util.stream.Collectors

/** A simple implementation that returns the list of media apps in alphabetical order. */
class MediaSourcesProvider private constructor(private val context: Context) {
    companion object {
        @Volatile private var instance: MediaSourcesProvider? = null

        fun getInstance(context: Context) =
            instance
                ?: synchronized(this) {
                    instance ?: MediaSourcesProvider(context).also { instance = it }
                }
    }

    private val mediaSources: LiveData<List<MediaSource>> = MutableLiveData(getList())
    private val appInstallUninstallReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                mediaSources.value == getList()
            }
        }

    init {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_PACKAGE_ADDED)
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        filter.addDataScheme("package")
        context.applicationContext.registerReceiver(appInstallUninstallReceiver, filter)
    }

    /** Returns the livedata of the media sources. */
    fun getMediaSources(): LiveData<List<MediaSource>> {
        return mediaSources
    }

    /** Returns the sorted list of available media sources in alphabetical order. */
    private fun getList(): List<MediaSource> {
        return getComponentNames()
            .stream()
            .filter { obj: ComponentName? ->
                Objects.nonNull(obj) && MediaSource.isAudioMediaSource(context, obj)
            }
            .map<MediaSource?> { componentName: ComponentName? ->
                MediaSource.create(context, componentName!!)
            }
            .filter { mediaSource: MediaSource? -> Objects.nonNull(mediaSource) }
            .sorted(
                Comparator.comparing { mediaSource: MediaSource ->
                    mediaSource.getDisplayName(context).toString()
                }
            )
            .collect(Collectors.toList())
    }

    /** Generates a set of all possible media services to choose from. */
    private fun getComponentNames(): Set<ComponentName> {
        val packageManager: PackageManager = context.packageManager
        val mediaIntent = Intent()
        mediaIntent.setAction(MediaBrowserService.SERVICE_INTERFACE)
        val mediaServices =
            packageManager.queryIntentServices(mediaIntent, PackageManager.GET_RESOLVED_FILTER)

        val components: MutableSet<ComponentName> = HashSet()
        for (info in mediaServices) {
            val componentName = ComponentName(info.serviceInfo.packageName, info.serviceInfo.name)
            components.add(componentName)
        }
        return components
    }
}

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

package com.android.designcompose

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.collection.LruCache

/** https://developer.android.com/topic/performance/graphics/cache-bitmap */
object BitmapFactoryWithCache {
    private val bitmapCache: LruCache<String, Bitmap>

    init {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        bitmapCache =
            object : LruCache<String, Bitmap>(cacheSize) {

                override fun sizeOf(key: String, value: Bitmap): Int {
                    // The cache size will be measured in kilobytes rather than number of items.
                    return (value.byteCount / 1024).coerceAtLeast(1)
                }
            }
    }

    fun loadResource(resources: Resources, resId: Int): Bitmap {
        val locales = resources.configuration.locales
        val localeId = if (locales.isEmpty) "" else locales[0].toString()
        val imageKey: String = resId.toString() + localeId

        val bitmap: Bitmap =
            getBitmapFromMemCache(imageKey)
                ?: run {
                    val resBitmap = BitmapFactory.decodeResource(resources, resId)
                    putBitmapToMemCache(imageKey, resBitmap)
                    resBitmap
                }
        return bitmap
    }

    private fun getBitmapFromMemCache(key: String): Bitmap? {
        synchronized(bitmapCache) {
            return bitmapCache[key]
        }
    }

    private fun putBitmapToMemCache(key: String, bitmap: Bitmap) {
        synchronized(bitmapCache) { bitmapCache.put(key, bitmap) }
    }
}

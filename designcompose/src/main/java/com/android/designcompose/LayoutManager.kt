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

import android.util.SizeF

internal object LayoutManager {
    private var managerId: Int = 0
    private var textMeasures: HashMap<Int, TextMeasureData> = HashMap()
    private var customMeasure: HashMap<Int, ((Float, Float, Float, Float) -> SizeF?)> = HashMap()
    private var modifiedSizes: HashSet<Int> = HashSet()

    init {
        managerId = Jni.createLayoutManager()
    }

    internal fun squooshSetTextMeasureData(layoutId: Int, textMeasureData: TextMeasureData) {
        textMeasures[layoutId] = textMeasureData
    }

    internal fun squooshClearTextMeasureData(layoutId: Int) {
        textMeasures.remove(layoutId)
    }

    internal fun squooshSetCustomMeasure(
        layoutId: Int,
        m: ((Float, Float, Float, Float) -> SizeF?),
    ) {
        customMeasure[layoutId] = m
    }

    internal fun squooshClearCustomMeasure(layoutId: Int) {
        customMeasure.remove(layoutId)
    }

    internal fun getCustomMeasure(layoutId: Int): ((Float, Float, Float, Float) -> SizeF?)? =
        customMeasure[layoutId]

    internal fun getTextMeasureData(layoutId: Int): TextMeasureData? = textMeasures[layoutId]

    internal fun hasModifiedSize(layoutId: Int): Boolean = modifiedSizes.contains(layoutId)
}

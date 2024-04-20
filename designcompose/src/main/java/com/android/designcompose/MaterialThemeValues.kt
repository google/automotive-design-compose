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

import androidx.compose.ui.graphics.Color

// Collection name for material theme generated from Material Theme Builder plugin
const val MATERIAL_THEME_COLLECTION_NAME = "material-theme"

// Material theme variable names for variables generated from Material Theme Builder plugin
object MaterialThemeConstants {
    const val PRIMARY = "Schemes/Primary"
    const val ON_PRIMARY = "Schemes/On Primary"
    const val PRIMARY_CONTAINER = "Schemes/Primary Container"
    const val ON_PRIMARY_CONTAINER = "Schemes/On Primary Container"
    const val INVERSE_PRIMARY = "Schemes/Inverse Primary"
    const val SECONDARY = "Schemes/Secondary"
    const val ON_SECONDARY = "Schemes/On Secondary"
    const val SECONDARY_CONTAINER = "Schemes/Secondary Container"
    const val ON_SECONDARY_CONTAINER = "Schemes/On Secondary Container"
    const val TERTIARY = "Schemes/Tertiary"
    const val ON_TERTIARY = "Schemes/On Tertiary"
    const val TERTIARY_CONTAINER = "Schemes/Tertiary Container"
    const val ON_TERTIARY_CONTAINER = "Schemes/On Tertiary Container"
    const val BACKGROUND = "Schemes/Background"
    const val ON_BACKGROUND = "Schemes/On Background"
    const val SURFACE = "Schemes/Surface"
    const val ON_SURFACE = "Schemes/On Surface"
    const val SURFACE_VARIANT = "Schemes/Surface Variant"
    const val ON_SURFACE_VARIANT = "Schemes/On Surface Variant"
    const val SURFACE_TINT = "Schemes/Surface Tint"
    const val INVERSE_SURFACE = "Schemes/Inverse Surface"
    const val INVERSE_ON_SURFACE = "Schemes/Inverse On Surface"
    const val ERROR = "Schemes/Error"
    const val ON_ERROR = "Schemes/On Error"
    const val ERROR_CONTAINER = "Schemes/Error Container"
    const val ON_ERROR_CONTAINER = "Schemes/On Error Container"
    const val OUTLINE = "Schemes/Outline"
    const val OUTLINE_VARIANT = "Schemes/Outline Variant"
    const val SCRIM = "Schemes/Scrim"
    const val SURFACE_BRIGHT = "Schemes/Surface Bright"
    const val SURFACE_DIM = "Schemes/Surface Dim"
    const val SURFACE_CONTAINER = "Schemes/Surface Container"
    const val SURFACE_CONTAINER_HIGH = "Schemes/Surface Container High"
    const val SURFACE_CONTAINER_HIGHEST = "Schemes/Surface Container Highest"
    const val SURFACE_CONTAINER_LOW = "Schemes/Surface Container Low"
    const val SURFACE_CONTAINER_LOWEST = "Schemes/Surface Container Lowest"
}

// Helper object to convert a variable name into the corresponding Material Theme value
internal object MaterialThemeValues {
    fun getColor(name: String, collection: String, state: VariableState): Color? {
        if (!state.useMaterialTheme) return null
        if (collection != MATERIAL_THEME_COLLECTION_NAME) return null

        state.materialColorScheme?.let { c ->
            return when (name) {
                MaterialThemeConstants.PRIMARY -> c.primary
                MaterialThemeConstants.ON_PRIMARY -> c.onPrimary
                MaterialThemeConstants.PRIMARY_CONTAINER -> c.primaryContainer
                MaterialThemeConstants.ON_PRIMARY_CONTAINER -> c.onPrimaryContainer
                MaterialThemeConstants.INVERSE_PRIMARY -> c.inversePrimary
                MaterialThemeConstants.SECONDARY -> c.secondary
                MaterialThemeConstants.ON_SECONDARY -> c.onSecondary
                MaterialThemeConstants.SECONDARY_CONTAINER -> c.secondaryContainer
                MaterialThemeConstants.ON_SECONDARY_CONTAINER -> c.onSecondaryContainer
                MaterialThemeConstants.TERTIARY -> c.tertiary
                MaterialThemeConstants.ON_TERTIARY -> c.onTertiary
                MaterialThemeConstants.TERTIARY_CONTAINER -> c.tertiaryContainer
                MaterialThemeConstants.ON_TERTIARY_CONTAINER -> c.onTertiaryContainer
                MaterialThemeConstants.BACKGROUND -> c.background
                MaterialThemeConstants.ON_BACKGROUND -> c.onBackground
                MaterialThemeConstants.SURFACE -> c.surface
                MaterialThemeConstants.ON_SURFACE -> c.onSurface
                MaterialThemeConstants.SURFACE_VARIANT -> c.surfaceVariant
                MaterialThemeConstants.ON_SURFACE_VARIANT -> c.onSurfaceVariant
                MaterialThemeConstants.SURFACE_TINT -> c.surfaceTint
                MaterialThemeConstants.INVERSE_SURFACE -> c.inverseSurface
                MaterialThemeConstants.INVERSE_ON_SURFACE -> c.inverseOnSurface
                MaterialThemeConstants.ERROR -> c.error
                MaterialThemeConstants.ON_ERROR -> c.onError
                MaterialThemeConstants.ERROR_CONTAINER -> c.errorContainer
                MaterialThemeConstants.ON_ERROR_CONTAINER -> c.onErrorContainer
                MaterialThemeConstants.OUTLINE -> c.outline
                MaterialThemeConstants.OUTLINE_VARIANT -> c.outlineVariant
                MaterialThemeConstants.SCRIM -> c.scrim
                MaterialThemeConstants.SURFACE_BRIGHT -> c.surfaceBright
                MaterialThemeConstants.SURFACE_DIM -> c.surfaceDim
                MaterialThemeConstants.SURFACE_CONTAINER -> c.surfaceContainer
                MaterialThemeConstants.SURFACE_CONTAINER_HIGH -> c.surfaceContainerHigh
                MaterialThemeConstants.SURFACE_CONTAINER_HIGHEST -> c.surfaceContainerHighest
                MaterialThemeConstants.SURFACE_CONTAINER_LOW -> c.surfaceContainerLow
                MaterialThemeConstants.SURFACE_CONTAINER_LOWEST -> c.surfaceContainerLowest
                else -> null
            }
        }
        return null
    }
}

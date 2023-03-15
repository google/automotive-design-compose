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

package com.android.designcompose.reference.homecompose

import android.provider.Settings
import android.util.Log
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/// This file contains utility functions relating to Android's Immersive Mode.
///
/// AOSP honors requests for Immersive Mode by hiding the SystemUI elements (the
/// top status bar and bottom navigation bar), and the platform provides a few
/// methods for making an Immersive Mode request.
///
/// Some OEMs (Volvo and Polestar on Android R, at least) want to ensure that no
/// regular application can hide the SystemUI (because it can create a confusing
/// experience, especially if the "home" button isn't obvious) and so they ignore
/// requests to hide SystemUI.
///
/// For OEM builds we can write a global setting to hide the SystemUI, and then
/// write the setting back again when we exit. This requires a privileged permission
/// and is something we should only need to do while prototyping (because in
/// production, we'd implement the SystemUI or otherwise integrate with it).
internal object ImmersiveMode {
  var appliedSetting: Boolean = false
  var previousSetting: String? = null
}

const val POLICY_CONTROL = "android.car.SYSTEM_BAR_VISIBILITY_OVERRIDE"
const val IMMERSIVE_MODE_POLICY = "immersive.full=*"

internal fun ImmersiveMode.enterImmersiveMode(activity: MainActivity) {
  Log.i("DesignCompose", "Requesting immersive mode via platform API")
  val windowInsetsController =
    ViewCompat.getWindowInsetsController(activity.window.decorView) ?: return
  windowInsetsController.systemBarsBehavior =
    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
  windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

  // Now, for OEM builds that don't honor the SystemUI hide request for
  // unrecognized package names, we can attempt to update the global
  // policy for the SystemUI. This requires a permission to write settings
  // which we may not have, so we catch any exception and log it.
  try {
    Log.i("DesignCompose", "Requesting immersive mode via system policy")
    val contentResolver = activity.contentResolver
    previousSetting = Settings.Global.getString(contentResolver, POLICY_CONTROL)
    Settings.Global.putString(contentResolver, POLICY_CONTROL, IMMERSIVE_MODE_POLICY)
    appliedSetting = true
  } catch (t: Throwable) {
    Log.e("DesignCompose", "Unable to request immersive mode via policy", t)
  }
}

internal fun ImmersiveMode.exitImmersiveMode(activity: MainActivity) {
  // Un-apply the policy control setting
  if (!appliedSetting) return
  try {
    Log.i("DesignCompose", "Reverting immersive mode request via system policy")
    val contentResolver = activity.contentResolver
    Settings.Global.putString(contentResolver, POLICY_CONTROL, previousSetting)
    appliedSetting = false
  } catch (t: Throwable) {
    Log.e("DesignCompose", "Unable to revert immersive mode policy", t)
  }
}

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

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import java.util.ArrayList
import java.util.Comparator

class AppLauncher(private val mActivity: Activity, private val mHiddenApps: Set<String>) :
  LauncherApps.Callback() {
  private val mLauncherApps: LauncherApps = mActivity.getSystemService(LauncherApps::class.java)
  private var mSetApps: (ArrayList<App>) -> Unit = {}
  private var mApps: SnapshotStateList<App> = SnapshotStateList()

  /// Create an AppLauncher, which will populate CenterDisplay's app list, excluding any
  /// apps in the "hiddenApps" list.
  init {
    mLauncherApps.registerCallback(this, Handler(Looper.myLooper()!!))
    updateAppLaunchers()
  }

  private fun updateAppLaunchers() {
    val availableActivities = mLauncherApps.getActivityList(null, Process.myUserHandle())
    mApps.clear()
    availableActivities.sortWith(
      Comparator.comparing({ info: LauncherActivityInfo -> info.label.toString() }) {
        obj: String,
        str: String? ->
        obj.compareTo(str!!, ignoreCase = true)
      }
    )
    // We don't look for media services in this pass; we could extend this in the future
    // to create a variety of lists and populate them into the document with different
    // node names.
    //
    // For now, we assume the designer will use the media source list to find media apps
    // and prefer a shorter app list.
    for (info in availableActivities) {
      val componentName = info.componentName
      val packageName = componentName.packageName
      if (mHiddenApps.contains(packageName)) {
        continue
      }
      val launchIntent =
        Intent(Intent.ACTION_MAIN)
          .setComponent(componentName)
          .addCategory(Intent.CATEGORY_LAUNCHER)
          .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      val app =
        App(
          title = info.label?.toString() ?: "",
          icon = getIcon(info.getBadgedIcon(0)),
          onTap =
            Modifier.clickable {
              val options = ActivityOptions.makeBasic()
              options.launchDisplayId = mActivity.display!!.displayId
              mActivity.startActivity(launchIntent, options.toBundle())
            }
        )
      mApps.add(app)
    }
  }

  override fun onPackageRemoved(s: String, userHandle: UserHandle) {
    updateAppLaunchers()
  }

  override fun onPackageAdded(s: String, userHandle: UserHandle) {
    updateAppLaunchers()
  }

  override fun onPackageChanged(s: String, userHandle: UserHandle) {
    updateAppLaunchers()
  }

  override fun onPackagesAvailable(strings: Array<String>, userHandle: UserHandle, b: Boolean) {
    updateAppLaunchers()
  }

  override fun onPackagesUnavailable(strings: Array<String>, userHandle: UserHandle, b: Boolean) {
    updateAppLaunchers()
  }

  private fun getIcon(icon: Drawable): Bitmap {
    val b = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
    b.eraseColor(0)
    val c = Canvas(b)
    icon.setBounds(0, 0, 50, 50)
    icon.draw(c)
    return b
  }

  internal class App(
    var title: String = "",
    var icon: Bitmap? = null,
    var onTap: Modifier = Modifier
  )

  @Composable
  internal fun getAppLaunchers(): List<App> {
    val apps = remember { mApps }
    return apps.toList()
  }
}

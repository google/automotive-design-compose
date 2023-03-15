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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.assistant.suggestion.AssistantSuggestions
import com.google.assistant.suggestion.Display
import com.google.assistant.suggestion.HomeScreenSignal
import com.google.assistant.suggestion.HomeTile
import com.google.assistant.suggestion.RegisterDisplayParams

internal val sAssistantSuggestions = AssistantSuggestions.getInstance()

internal class GoogleAssistantSuggestions(
  private val mActivity: Activity,
  private val mLifecycleOwner: LifecycleOwner
) {
  private var mSuggestions: SnapshotStateList<Suggestion> = SnapshotStateList()
  private var mShortcuts: SnapshotStateList<Shortcut> = SnapshotStateList()

  init {
    var dpy =
      sAssistantSuggestions.registerDisplay(
        mActivity,
        RegisterDisplayParams.Builder.defaultHomeBuilder().build()
      ) { display: Display? ->
        if (display == null) {
          return@registerDisplay
        }
        mActivity.runOnUiThread(Runnable { updateDisplay(display) })
      }

    mLifecycleOwner.lifecycle.addObserver(
      object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
          sAssistantSuggestions.unregisterDisplay(mActivity, dpy)
        }
      }
    )

    // What does this do?
    var homeTile =
      HomeTile.Builder.newBuilder("How can I help?", "com.google.android.carassistant").build()
    var signal =
      HomeScreenSignal.Builder.newBuilder()
        .setAssistantVisibility(true)
        .setHomeScreenVisibility(true)
        .addHomeTile(homeTile)
        .build()

    sAssistantSuggestions.onNewSignal(mActivity, signal, dpy)
  }

  internal class Suggestion(
    var title: String = "",
    var icon: Bitmap? = null,
    var onTap: Modifier = Modifier,
  )

  internal class Shortcut(
    var title: String = "",
    var icon: Bitmap? = null,
    var onTap: Modifier = Modifier,
  )

  private fun updateDisplay(display: Display) {
    mSuggestions.clear()
    display.suggestions.forEach {
      Log.i(
        "DesignCompose",
        "Suggestion: " + it.humanReadableSuggestion + "/" + it.description + "/" + it.iconUri
      )
      val suggestion =
        Suggestion(
          title = it.humanReadableSuggestion,
          icon = getIcon(it.iconUri, false),
          onTap =
            Modifier.clickable {
              sAssistantSuggestions.openVisualElement(
                mActivity,
                Display.VisualElementType.TAPPABLE_SUGGESTION_TEXT,
                it.id
              )
            }
        )
      mSuggestions.add(suggestion)
    }

    mShortcuts.clear()
    display.shortcuts.forEach {
      Log.i("DesignCompose", "Shortcut: " + it.displayText + "/" + it.iconUri)
      val shortcut =
        Shortcut(
          title = it.displayText,
          icon = getIcon(it.iconUri, true),
          onTap =
            Modifier.clickable {
              sAssistantSuggestions.openVisualElement(
                mActivity,
                Display.VisualElementType.TAPPABLE_SUGGESTION_TEXT,
                it.id
              )
            }
        )
      mShortcuts.add(shortcut)
    }
  }

  private fun getIcon(iconUri: String, tint: Boolean): Bitmap? {
    try {
      val x =
        sAssistantSuggestions.loadDrawableResource(mActivity, Uri.parse(iconUri), mActivity.theme)
      val b = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
      b.eraseColor(0)
      val c = Canvas(b)
      x.setBounds(0, 0, 50, 50)
      if (tint) {
        x.setTint(-0x1000000)
      }
      x.draw(c)
      return b
    } catch (t: Throwable) {
      Log.e("DesignCompose", "Unable to get icon", t)
    }
    return null
  }

  @Composable
  internal fun getSuggestions(): List<Suggestion> {
    val suggestions = remember { mSuggestions }
    return suggestions.toList()
  }

  @Composable
  internal fun getShortcuts(): List<Shortcut> {
    val shortcuts = remember { mShortcuts }
    return shortcuts.toList()
  }
}

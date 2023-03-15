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

package com.android.designcompose.reference.homecompose;

import static android.content.Context.ACTIVITY_SERVICE;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityView;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Region;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.ViewGroup;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

/// EmbeddedActivityView extends the AOSP ActivityView to show a map in an Android view.
/// This class also extends the ActivityView interface to handle the
/// launching of the target activity and responding to lifecycle events
/// since those tasks are error-prone and repetitive.
public class EmbeddedActivityView extends ActivityView implements DefaultLifecycleObserver {
    private static final String TAG = "DesignCompose";
    private final DisplayManager mDisplayManager;
    private final Activity mActivity;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final Region mTapExcludeRegion = new Region();
    private int mExcludeLeft = 0;
    private int mExcludeTop = 0;
    private int mExcludeRight = 0;
    private int mExcludeBottom = 0;

    private Intent mLaunchIntent;

    private boolean mActivityViewReady = false;
    private boolean mIsStarted = false;

    /// Create an EmbeddedActivityView. This includes registering for display notifications
    /// and handling the underlying ActivityView's notifications. The ActivityView will also
    /// be added to the given Activity's window.
    public EmbeddedActivityView(Activity c, LifecycleOwner lifecycleOwner) {
        super(c);
        mActivity = c;

        super.setCallback(mActivityStateCallback);
        mDisplayManager = c.getSystemService(DisplayManager.class);
        mDisplayManager.registerDisplayListener(mDisplayListener, mMainHandler);

        c.getWindow().addContentView(
            this,
            new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            )
        );
        lifecycleOwner.getLifecycle().addObserver(this);
    }

    public void setLaunchIntent(Intent launchIntent) {
        mLaunchIntent = launchIntent;
    }

    public void onRestart() {
        Log.i(TAG, "EmbeddedActivityView onRestart");
        maybeLaunchActivity();
    }

    public void onStart(LifecycleOwner owner) {
        Log.i(TAG, "EmbeddedActivityView onStart");
        mIsStarted = true;
    }

    public void onStop(LifecycleOwner owner) {
        Log.i(TAG, "EmbeddedActivityView onStop");
        mIsStarted = false;
    }

    public void onDestroy(LifecycleOwner owner) {
        Log.i(TAG, "EmbeddedActivityView onDestroy");
        mDisplayManager.unregisterDisplayListener(mDisplayListener);
        if (mActivityViewReady) super.release();
    }

    public void setExcludeRegion(int left, int top, int right, int bottom) {
        mExcludeLeft = left;
        mExcludeTop = top;
        mExcludeRight = right;
        mExcludeBottom = bottom;
    }
    @Override
    public Region getTapExcludeRegion() {
        //Region mTapExcludeRegion = new Region();
        if (isAttachedToWindow() && canReceivePointerEvents()) {
            Point windowPos = getPositionInWindow();
            mTapExcludeRegion.set(
                    windowPos.x,
                    windowPos.y,
                    windowPos.x + getWidth(),
                    windowPos.y + getHeight()
            );

            mTapExcludeRegion.op(mExcludeLeft, mExcludeTop, mExcludeRight, mExcludeBottom, Region.Op.DIFFERENCE);
        } else {
            mTapExcludeRegion.setEmpty();
        }
        return mTapExcludeRegion;
    }

    private void maybeLaunchActivity() {
    Log.i(
        TAG,
        "maybe launch: layout ready: activity view ready: "
            + mActivityViewReady
            + " launch intent set: "
            + (mLaunchIntent != null));
        if (!mActivityViewReady || mLaunchIntent == null)
            return;

        // This logic for navigating Android's various window manager modes is copied from
        // CarLauncher.java in the AOSP codebase.
        if (mActivity.isInMultiWindowMode() || mActivity.isInPictureInPictureMode())
            return;
        if (getDisplay().getState() != Display.STATE_ON)
            return;
        super.startActivity((Intent) mLaunchIntent.clone());
    }

  private final ActivityView.StateCallback mActivityStateCallback =
      new ActivityView.StateCallback() {
        @Override
        public void onActivityViewReady(ActivityView view) {
          mActivityViewReady = true;
          maybeLaunchActivity();
        }

        @Override
        public void onActivityViewDestroyed(ActivityView view) {
          mActivityViewReady = false;
        }

        @Override
        public void onTaskMovedToFront(int taskId) {
          try {
            if (mIsStarted) {
              ActivityManager am =
                  (ActivityManager) getContext().getSystemService(ACTIVITY_SERVICE);
              am.moveTaskToFront(mActivity.getTaskId(), 0);
            }
          } catch (RuntimeException e) {
            Log.w(TAG, "Failed to move com.android.designcompose.reference.home to front");
          }
        }
      };
    private final DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {}

        @Override
        public void onDisplayRemoved(int displayId) {}

        @Override
        public void onDisplayChanged(int displayId) {
            if (getDisplay() == null || displayId != getDisplay().getDisplayId()) {
                return;
            }
            maybeLaunchActivity();
        }
    };
}

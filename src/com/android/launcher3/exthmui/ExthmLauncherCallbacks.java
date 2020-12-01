/*
 * Copyright (C) 2018 The LineageOS Project
 *               2020 The exTHmUI OpenSource Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.exthmui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;

import com.android.launcher3.AppInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherCallbacks;
import com.android.launcher3.Utilities;

import org.exthmui.libraries.launcher.LauncherClient;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class ExthmLauncherCallbacks implements LauncherCallbacks,
        OnSharedPreferenceChangeListener {
    public static final String KEY_ENABLE_EXTHMUI_FEED = "pref_enable_exthmui_feed";
    public static final String EXTHMUI_FEED_PACKAGE = "org.exthmui.feed";

    private final Launcher mLauncher;

    private OverlayCallbackImpl mOverlayCallbacks;
    private LauncherClient mLauncherClient;
    private boolean mDeferCallbacks;

    public ExthmLauncherCallbacks(Launcher launcher) {
        mLauncher = launcher;
    }

    public void deferCallbacksUntilNextResumeOrStop() {
        mDeferCallbacks = true;
    }

    public LauncherClient getLauncherClient() {
        return mLauncherClient;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = Utilities.getPrefs(mLauncher);
        mOverlayCallbacks = new OverlayCallbackImpl(mLauncher);
        mLauncherClient = new LauncherClient(mLauncher, mOverlayCallbacks, EXTHMUI_FEED_PACKAGE, isEnabled(prefs));
        mOverlayCallbacks.setClient(mLauncherClient);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDetachedFromWindow() {
        mLauncherClient.onDetachedFromWindow();
    }

    @Override
    public void onAttachedToWindow() {
        mLauncherClient.onAttachedToWindow();
    }

    @Override
    public void onHomeIntent(boolean internalStateHandled) {
        mLauncherClient.hideOverlay(mLauncher.isStarted() && !mLauncher.isForceInvisible());
    }

    @Override
    public void onResume() {
        Handler handler = mLauncher.getDragLayer().getHandler();
        if (mDeferCallbacks) {
            if (handler == null) {
                // Finish defer if we are not attached to window.
                checkIfStillDeferred();
            } else {
                // Wait one frame before checking as we can get multiple resume-pause events
                // in the same frame.
                handler.post(this::checkIfStillDeferred);
            }
        } else {
            mLauncherClient.onResume();
        }

    }

    @Override
    public void onPause() {
        if (!mDeferCallbacks) {
            mLauncherClient.onPause();
        }
    }

    @Override
    public void onStart() {
        if (!mDeferCallbacks && mLauncherClient != null) {
            mLauncherClient.onStart();
        }
    }

    @Override
    public void onStop() {
        if (mDeferCallbacks) {
            checkIfStillDeferred();
        } else {
            mLauncherClient.onStop();
        }
    }

    private void checkIfStillDeferred() {
        if (!mDeferCallbacks) {
            return;
        }
        if (!mLauncher.hasBeenResumed() && mLauncher.isStarted()) {
            return;
        }
        mDeferCallbacks = false;

        // Move the client to the correct state. Calling the same method twice is no-op.
        if (mLauncher.isStarted()) {
            mLauncherClient.onStart();
        }
        if (mLauncher.hasBeenResumed()) {
            mLauncherClient.onResume();
        } else {
            mLauncherClient.onPause();
        }
        if (!mLauncher.isStarted()) {
            mLauncherClient.onStop();
        }
    }
    @Override
    public void onDestroy() {
        mLauncherClient.onDestroy();
        Utilities.getPrefs(mLauncher).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (KEY_ENABLE_EXTHMUI_FEED.equals(key)) {
            mLauncherClient.setOverlayEnabled(isEnabled(prefs));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) { }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) { }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) { }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter w, String[] args) { }

    @Override
    public boolean handleBackPressed() {
        return false;
    }

    @Override
    public void onTrimMemory(int level) { }

    @Override
    public void onLauncherProviderChange() { }

    @Override
    public boolean startSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData) {
        return false;
    }

    private boolean isEnabled(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_ENABLE_EXTHMUI_FEED, true);
    }
}

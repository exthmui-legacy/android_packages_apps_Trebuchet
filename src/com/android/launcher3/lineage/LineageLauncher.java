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
package com.android.launcher3.lineage;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherCallbacks;
import com.android.launcher3.exthmui.ExthmLauncherCallbacks;

public class LineageLauncher extends Launcher {

    private final LineageLauncherCallbacks mLineageCallbacks;
    private final ExthmLauncherCallbacks mExthmCallbacks;
    private LauncherCallbacks mCallbacks;

    public LineageLauncher() {
        mLineageCallbacks = new LineageLauncherCallbacks(this);
        mExthmCallbacks = new ExthmLauncherCallbacks(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLineageCallbacks.onCreate(savedInstanceState);
        mExthmCallbacks.onCreate(savedInstanceState);
        updateCallbacks(getSharedPrefs());
    }

    public LauncherCallbacks getCallbacks() {
        return mCallbacks;
    }

    private void updateCallbacks(SharedPreferences prefs) {
        boolean exthm = prefs.getBoolean(ExthmLauncherCallbacks.KEY_ENABLE_EXTHMUI_FEED, true) && 
            LineageUtils.hasPackageInstalled(this, ExthmLauncherCallbacks.EXTHMUI_FEED_PACKAGE);
        boolean google = prefs.getBoolean(LineageLauncherCallbacks.KEY_ENABLE_MINUS_ONE, true) && 
            LineageUtils.hasPackageInstalled(this, LineageLauncherCallbacks.SEARCH_PACKAGE);
        if (google) {
            mCallbacks = mLineageCallbacks;
        } else if (exthm) {
            mCallbacks = mExthmCallbacks;
        } else {
            mCallbacks = null;
        }
        setLauncherCallbacks(mCallbacks);
    }
}

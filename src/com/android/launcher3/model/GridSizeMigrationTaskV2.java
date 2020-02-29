/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.launcher3.model;

import android.content.Context;

import com.android.launcher3.InvariantDeviceProfile;

/**
 * This class takes care of shrinking the workspace (by maximum of one row and one column), as a
 * result of restoring from a larger device or device density change.
 */
public class GridSizeMigrationTaskV2 {

    private GridSizeMigrationTaskV2(Context context) {

    }

    /** See {@link #migrateGridIfNeeded(Context, InvariantDeviceProfile)} */
    public static boolean migrateGridIfNeeded(Context context) {
        // To be implemented.
        return true;
    }

    /**
     * Run the migration algorithm if needed. For preview, we provide the intended idp because it
     * has not been changed. If idp is null, we read it from the context, for actual grid migration.
     *
     * @return false if the migration failed.
     */
    public static boolean migrateGridIfNeeded(Context context, InvariantDeviceProfile idp) {
        // To be implemented.
        return true;
    }
}

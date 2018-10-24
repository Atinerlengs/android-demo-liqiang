/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.freeme.filemanager.util;

import android.database.Cursor;

import java.io.Closeable;
import java.io.IOException;
import java.util.zip.ZipFile;

/**
 * Utility methods for closing io streams and database cursors.
 */
public class CloseUtils {

    /**
     * If the argument is non-null, close the Closeable ignoring any {@link IOException}.
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // Ignore.
            }
        }
    }

    /**
     * If the argument is non-null, close the ZipFile ignoring any {@link IOException}.
     */
    public static void closeQuietly(ZipFile zipFile) {
        if (zipFile != null) {
            try {
            	zipFile.close();
            } catch (IOException e) {
                // Ignore.
            }
        }
    }

    /** If the argument is non-null, close the cursor. */
    public static void closeQuietly(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }
    
    private CloseUtils() { } // Do not instantiate
}

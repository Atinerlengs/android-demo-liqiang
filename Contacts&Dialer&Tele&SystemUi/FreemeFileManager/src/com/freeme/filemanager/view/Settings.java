/*
 * This file is part of FileManager.
 * FileManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FileManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 *
 * TYD Inc. (C) 2012. All rights reserved.
 */
package com.freeme.filemanager.view;

public class Settings {
    // whether show system and cache images, default not
    private boolean mShowDotAndHiddenFiles = true;
    private static Settings mInstance;

    private Settings() {

    }

    public static Settings instance() {
        if(mInstance == null) {
            mInstance = new Settings();
        }
        return mInstance;
    }

    public boolean getShowDotAndHiddenFiles() {
        return mShowDotAndHiddenFiles;
    }

    public void setShowDotAndHiddenFiles(boolean s) {
        mShowDotAndHiddenFiles = s;
    }
}

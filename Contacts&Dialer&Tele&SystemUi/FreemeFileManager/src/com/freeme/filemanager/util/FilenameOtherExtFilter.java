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
package com.freeme.filemanager.util;

import java.util.Arrays;
import java.util.HashSet;

public class FilenameOtherExtFilter extends FilenameExtFilter {

    private HashSet<FilenameExtFilter> mExts;

    // using lower case
    public FilenameOtherExtFilter(FilenameExtFilter[] exts) {
        super(null);
        mExts = new HashSet<FilenameExtFilter>();
        mExts.addAll(Arrays.asList(exts));
    }

    @Override
    public boolean contains(String ext) {
        for (FilenameExtFilter f : mExts) {
            if (f.contains(ext))
                return false;
        }
        return true;
    }
}

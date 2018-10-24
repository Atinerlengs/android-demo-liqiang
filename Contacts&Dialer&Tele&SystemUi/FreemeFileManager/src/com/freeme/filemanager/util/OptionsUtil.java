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

//import com.mediatek.featureoption.FeatureOption;

import com.freeme.filemanager.model.FileManagerLog;

import android.os.SystemProperties;


public class OptionsUtil {

    public static boolean isOp02Enabled() {
        FileManagerLog.d("SystemProperties", "ro.operator.optr="+SystemProperties.get("ro.operator.optr"));
        return "OP02".equals(SystemProperties.get("ro.operator.optr"));
    }

    public static boolean isDrmSupported() {
        return true;//FeatureOption.MTK_DRM_APP;
    }
}

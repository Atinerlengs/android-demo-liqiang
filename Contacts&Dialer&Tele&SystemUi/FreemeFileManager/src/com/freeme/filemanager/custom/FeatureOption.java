
package com.freeme.filemanager.custom;

import android.os.SystemProperties;

public class FeatureOption {
//    public static final boolean MTK_MULTI_STORAGE_SUPPORT = getValue("ro.mtk_multi_storage_support");
    public static final boolean MTK_MULTI_STORAGE_SUPPORT = true;
    public static final boolean CLEAN_BUTTON_SUPPORT = false;
    /* get the key's value*/
    private static boolean getValue(String key) {
        return SystemProperties.get(key).equals("1");
    }
}

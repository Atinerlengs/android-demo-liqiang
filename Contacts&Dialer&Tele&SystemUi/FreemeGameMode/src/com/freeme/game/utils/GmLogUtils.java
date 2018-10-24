package com.freeme.game.utils;

import android.os.Build;
import android.util.Log;

public final class GmLogUtils {
    private static final String TAG = "GameMode";
    private static final boolean FORCE = false;
    private static final boolean DEBUG = FORCE || Build.TYPE.equals("userdebug")
            || Build.TYPE.equals("eng");

    public static void logi(String tag, String msg) {
        if (DEBUG) {
            Log.i(TAG, "[" + tag + "] -- " + msg);
        }
    }

    public static void logd(String tag, String msg) {
        if (DEBUG) {
            Log.d(TAG, "[" + tag + "] -- " + msg);
        }
    }

    public static void loge(String tag, String msg) {
        Log.e(TAG, "[" + tag + "] -- " + msg);
    }
}

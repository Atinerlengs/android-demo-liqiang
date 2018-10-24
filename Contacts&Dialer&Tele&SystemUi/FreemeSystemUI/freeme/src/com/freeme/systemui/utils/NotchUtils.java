package com.freeme.systemui.utils;

import android.content.Context;
import android.content.res.Configuration;

import com.freeme.util.FreemeNotchUtil;

public class NotchUtils {

    public static boolean hasNotch() {
        return FreemeNotchUtil.hasNotch();
    }

    public static int getNotchWidth() {
        return FreemeNotchUtil.getNotchWidth();
    }

    public static int getNotchHeight() {
        return FreemeNotchUtil.getNotchHeight();
    }

    public static boolean isLandscape(Context context) {
        return (context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE);
    }
}

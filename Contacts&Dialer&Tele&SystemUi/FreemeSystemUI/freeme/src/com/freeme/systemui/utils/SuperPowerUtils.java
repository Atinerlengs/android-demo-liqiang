package com.freeme.systemui.utils;

import android.content.Context;
import android.net.Uri;
import android.provider.Settings;

import static com.freeme.provider.FreemeSettings.System.FREEME_SUPER_POWER_SAVER_ENABLE;

public class SuperPowerUtils {

    public static boolean isSuperPowerModeOn(Context context) {
        return Settings.System.getInt(context.getContentResolver(), FREEME_SUPER_POWER_SAVER_ENABLE, 0) == 1;
    }

    public static Uri getSuperPowerModeUri() {
        return Settings.System.getUriFor(FREEME_SUPER_POWER_SAVER_ENABLE);
    }
}
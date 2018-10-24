package com.freeme.systemui.statusbar;

import android.provider.Settings;

import com.freeme.provider.FreemeSettings;

public class FreemeStatusbarStateToolKit {
    // switch key
    public static final String SHOW_BATTERY_LEVEL_SWITCH    = Settings.System.SHOW_BATTERY_PERCENT;
    public static final String SHOW_NETWORK_SPEED_SWITCH    = FreemeSettings.System.FREEME_SHOW_NETWORK_SPEED;
    public static final String SHOW_NOTI_ICON_SWITCH        = FreemeSettings.System.FREEME_SHOW_NOTI_ICON;
    public static final String FORB_SLIDE_KEYGUARD_SWITCH   = "show_slide_switch";
    public static final String SHOW_CARRIER_LABEL           = "show_carrier_label";
}

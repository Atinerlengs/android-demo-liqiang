package com.freeme.onehand;

import android.os.Build;

import com.freeme.provider.FreemeSettings;

public final class OneHandConstants {
    public static final boolean DEBUG = !"user".equals(Build.TYPE);

    public static final String ACTION_ONEHAND_SETTINGS = "com.freeme.intent.action.onehand.SETTINGS";

    public static final String ACTION_ONEHAND_SERVICE = "com.freeme.action.onehand.SERVICE";
    public static final String ACTION_ONEHAND_SERVICE_SCREEN_OFF = "com.freeme.action.onehand.SERVICE_SCREEN_OFF";

    public static final String ONEHAND_ENABLED = FreemeSettings.System.FREEME_ONEHAND_ENABLED;
    public static final String ONEHAND_RUNNING = "onehand_running";
    public static final String ONEHAND_RUNNING_INFO = "onehand_running_info";
    public static final String ONEHAND_SHOW_HARD_KEYS = FreemeSettings.System.FREEME_ONEHAND_SHOW_HARD_KEYS;
    public static final String ONEHAND_WAKEUP_TYPE = "onehand_wakeup_type";
    public static final String ONEHAND_DIRECTION = FreemeSettings.System.FREEME_ONEHAND_DIRECTION;

    public static final float ONEHAND_INCH_MIN = 3.4f;
    public static final float ONEHAND_INCH_DEF = 3.4f; // 3.6f
    public static final float ONEHAND_SCALE_UNDEF = -0.6f;

    /**
     * Ref: frameworks/native/include/input/Input.h
     */
    public static final int AMOTION_EVENT_FLAG_PREDISPATCH = 0x20000000;
}

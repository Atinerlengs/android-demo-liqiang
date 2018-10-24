package com.freeme.incallui.floating;

import android.content.Context;
import android.os.PowerManager;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.view.WindowManager;

class FreemeInCallFloatingUtils {

    protected static final String PRFERENCE_KEY_FLOATING_BUTTON_X_POINT = "FLOATING_BUTTON_X_POINT";
    protected static final String PRFERENCE_KEY_FLOATING_BUTTON_Y_POINT = "FLOATING_BUTTON_Y_POINT";

    protected static TelecomManager getTelecomManager(Context context) {
        return (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
    }

    protected static TelephonyManager getTelephonyManager(Context context) {
        return (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    protected static WindowManager getWindowManager(Context context) {
        return (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    protected static PowerManager getPowerManager(Context context) {
        return (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }
}

package com.freeme.dialer.contacts;

import android.content.Context;
import android.provider.Settings;

public class FreemeSimServiceUtils {

    public static final String SERVICE_SUBSCRIPTION_KEY = "subscription_key";
    public static final String SERVICE_WORK_TYPE = "work_type";
    private static final String IMPORT_REMOVE_RUNNING = "import_remove_running";

    public static final int SERVICE_WORK_DELETE = 2;

    public static boolean isServiceRunning(Context context, int subId) {
        String isRunning = Settings.System.getString(
                context.getContentResolver(), IMPORT_REMOVE_RUNNING);
        return "true".equals(isRunning);
    }
}

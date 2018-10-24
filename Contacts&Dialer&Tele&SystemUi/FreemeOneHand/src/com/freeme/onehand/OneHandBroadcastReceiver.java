package com.freeme.onehand;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

public final class OneHandBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "OneHandBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            boolean enabled = Settings.System.getIntForUser(context.getContentResolver(),
                    OneHandConstants.ONEHAND_ENABLED, 0,
                    UserHandle.USER_CURRENT) != 0;
            if (enabled) {
                startOneHandService(context);
            }
        }
    }

    private void startOneHandService(Context context) {
        Log.d(TAG, "startOneHandService");
        try {
            context.startService(new Intent(OneHandConstants.ACTION_ONEHAND_SERVICE)
                    .setClass(context, OneHandService.class));
        } catch (Exception e) {
            Log.e(TAG, "startOneHandService", e);
        }
    }
}

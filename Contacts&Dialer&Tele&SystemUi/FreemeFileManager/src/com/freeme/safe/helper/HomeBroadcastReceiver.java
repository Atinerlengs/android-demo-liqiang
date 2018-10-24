package com.freeme.safe.helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class HomeBroadcastReceiver extends BroadcastReceiver {

    /**
     * see {@link #com.android.server.policy.PhoneWindowManager.SYSTEM_DIALOG_REASON*}
     */
    private static final String SYSTEM_DIALOG_REASON_KEY = "reason";
    private static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
    private static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";

    private HomeBroadcastListener mListener;

    public void setOnHomeBroadcastListener(HomeBroadcastListener listener) {
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null
                && action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
            String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
            if (SYSTEM_DIALOG_REASON_HOME_KEY.equals(reason)
                    || SYSTEM_DIALOG_REASON_RECENT_APPS.equals(reason)) {
                mListener.onReceiveListener();
            }
        }
    }
}

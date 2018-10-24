package com.mediatek.dialer.calllog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telecom.TelecomManager;

/**
 * Listening phone account changed, notify listeners added in PhoneAccountInfoHelper
 */
public class PhoneAccountChangedReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (TelecomManager.ACTION_PHONE_ACCOUNT_REGISTERED.equals(action)
            || TelecomManager.ACTION_PHONE_ACCOUNT_UNREGISTERED.equals(action) ) {
            PhoneAccountInfoHelper.getInstance(context).notifyAccountInfoUpdate();
        }
    }

}

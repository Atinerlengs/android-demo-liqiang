package com.freeme.dialer.contacts;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.os.BuildCompat;

import com.android.dialer.R;

public class FreemeContactsNotificationChannelsUtil {

    public static String DEFAULT_CHANNEL = "DEFAULT_CHANNEL";

    private FreemeContactsNotificationChannelsUtil() {
    }

    public static void createDefaultChannel(Context context) {
        if (!BuildCompat.isAtLeastO()) {
            return;
        }
        final NotificationManager nm = context.getSystemService(NotificationManager.class);
        final NotificationChannel channel = new NotificationChannel(DEFAULT_CHANNEL,
                context.getString(R.string.contacts_default_notification_channel),
                NotificationManager.IMPORTANCE_LOW);
        nm.createNotificationChannel(channel);
    }
}

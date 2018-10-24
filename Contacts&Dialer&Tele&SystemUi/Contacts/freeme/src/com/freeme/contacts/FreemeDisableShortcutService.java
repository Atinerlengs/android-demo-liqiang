package com.freeme.contacts;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.os.Build;

import com.android.contacts.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FreemeDisableShortcutService extends IntentService {

    private static final String TAG = "FreemeDisableShortcutService";
    private static final String LOOKUP_KEYS = "lookup_keys";
    private String channelID = "1";
    private String channelName = "channel_name";

    public FreemeDisableShortcutService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationChannel channel = new NotificationChannel(channelID, channelName, NotificationManager.IMPORTANCE_MIN);
        NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        manager.createNotificationChannel(channel);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setChannelId(channelID);
        Notification notification = builder.build();
        this.startForeground(1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.stopForeground(1);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ArrayList<String> lookupkeys = intent.getStringArrayListExtra(LOOKUP_KEYS);

            if (lookupkeys != null && lookupkeys.size() > 0) {
                ShortcutManager shortcutManager = this.getSystemService(ShortcutManager.class);
                List<ShortcutInfo> shortcutInfos = shortcutManager.getPinnedShortcuts();

                for (int i = 0; i < lookupkeys.size(); i++) {
                    for (ShortcutInfo shortcutInfo : shortcutInfos) {
                        if (shortcutInfo.getId().equals(lookupkeys.get(i))) {
                            shortcutManager.disableShortcuts(Arrays.asList(shortcutInfo.getId()),
                                    getResources().getString(R.string.freeme_contact_not_exist));
                        }
                    }
                }
            }
        }
    }
}
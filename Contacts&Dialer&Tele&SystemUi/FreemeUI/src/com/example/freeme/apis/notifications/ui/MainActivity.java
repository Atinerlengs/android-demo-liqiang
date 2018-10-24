package com.example.freeme.apis.notifications.ui;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.example.freeme.apis.R;
import com.example.freeme.apis.notifications.service.DownloadService;
import com.example.freeme.apis.notifications.util.NotificationUtil;

public class MainActivity extends Activity {

    public final static String TAG = "MainActivity";

    public static final int TYPE_Progress = 2;
    private Intent service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_activity_main);
        service = new Intent(this,DownloadService.class);
        createNotifiChannel();
    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.button_standard_notification:

                NotificationUtil.showStandardNotification(MainActivity.this);
                break;
            case R.id.button_bundled_notification:

                NotificationUtil.showBundledNotifications(MainActivity.this);
                break;
            case R.id.button_remote_input_notification:

                NotificationUtil.showRemoteInputNotification(MainActivity.this);
                break;
            case R.id.button_custom_content_view_notification:

                NotificationUtil.showCustomContentViewNotification(MainActivity.this);
                break;
            case R.id.button_custom_content_big_view_notification:

                NotificationUtil.showCustomBigContentViewNotification(MainActivity.this);
                break;
            case R.id.button_custom_normal_and_big_content_views_notification:

                NotificationUtil.showCustomBothContentViewNotification(MainActivity.this);
                break;
            case R.id.button_custom_media_content_view_notification:

                NotificationUtil.showCustomMediaViewNotification(MainActivity.this);
                break;
            case R.id.button_custom_layout_heads_up_notification:

                NotificationUtil.showCustomLayoutHeadsUpNotification(MainActivity.this);
                break;
            case R.id.button_inbox_style_notification:

                NotificationUtil.showInboxStyleNotification(MainActivity.this);
                break;
            case R.id.button_big_picture_style_notification:

                NotificationUtil.showBigPictureNotification(MainActivity.this);
                break;
            case R.id.button_download_style_notification:

                startService(service);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        if(service!=null){
            stopService(service);
        }
        super.onDestroy();
    }

    private void createNotifiChannel() {
        /**
         * NotificationManager.IMPORTANCE_DEFAULT
         * importance: shows everywhere, makes noise, but does not visually intrude
         */
        NotificationChannel channel1 = new NotificationChannel(NotificationUtil.PRIMARY_CHANNEL,
                getString(R.string.noti_channel_default), NotificationManager.IMPORTANCE_DEFAULT);
        channel1.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationUtil.getNotificationManager(this).createNotificationChannel(channel1);

        /**
         * NotificationManager.IMPORTANCE_HIGH
         * importance: shows everywhere, makes noise and peeks
         */
        NotificationChannel channel2 = new NotificationChannel(NotificationUtil.SECONDARY_CHANNEL,
                getString(R.string.noti_channel_second), NotificationManager.IMPORTANCE_HIGH);
        channel2.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationUtil.getNotificationManager(this).createNotificationChannel(channel2);

        /**
         * NotificationManager.IMPORTANCE_LOW
         * importance: shows everywhere, but is not intrusive
         */
        NotificationChannel channel3 = new NotificationChannel(NotificationUtil.THIRD_CHANNEL,
                getString(R.string.noti_channel_third), NotificationManager.IMPORTANCE_LOW);
        channel3.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationUtil.getNotificationManager(this).createNotificationChannel(channel3);
    }
}

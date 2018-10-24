package application.android.com.zhaozehong.action;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import java.util.List;

import application.android.com.zhaozehong.activities.TestPreferenceActivity;
import application.android.com.zhaozehong.demoapplication.R;

public class HeadsupNotificationAction extends Action {

    private final static String NOTIFICATION_CHANNEL_ID = "test_demo_channel_id";
    private final static String NOTIFICATION_CHANNEL_NAME = "test_demo_channel_name";
    private final static String NOTIFICATION_DESCRIPTION = "test_demo_channel_description";

    private final static int NOTIFICATION_ID = 0x1000;

    private NotificationManager nm;

    public HeadsupNotificationAction(Activity activity) {
        super(activity);
        nm = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);

        initNotificationChannel();
    }

    @Override
    public String getName() {
        return "Heads-up Notification";
    }

    @Override
    public void doAction() {
        Intent intent = new Intent(mActivity, TestPreferenceActivity.class);
        PendingIntent pendingIntent = PendingIntent
                .getActivity(mActivity, 0, intent, 0);

        RemoteViews remoteViews = new RemoteViews(mActivity.getPackageName(),
                R.layout.remote_view_notification);
        RemoteViews remoteViews2 = new RemoteViews(mActivity.getPackageName(),
                R.layout.remote_views_notification_content);

        Notification.Builder builder = new Notification.Builder(mActivity)
                .setWhen(System.currentTimeMillis())
                .setContentTitle("New Message")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
//                .setFullScreenIntent(pendingIntent, true)
//                .setCustomHeadsUpContentView(remoteViews)
                .setActions(new Notification.Action(0, "Message", null))
                .setActions(new Notification.Action(0, "Message2", null))
                .setContentText("You've received new messages.");

        builder.setCustomBigContentView(builder.createContentView());

        if (Build.VERSION.SDK_INT >= 26) {
            builder.setChannelId(NOTIFICATION_CHANNEL_ID);
        }

        nm.notify(NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onDestroy() {
        nm.cancel(NOTIFICATION_ID);
    }

    private void initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            nm.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID);
            createNotificationChannel();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                List<NotificationChannel> list = nm.getNotificationChannels();
                if (list == null || list.isEmpty()) {
                    createNotificationChannel();
                } else {
                    boolean isCreated = false;
                    for (NotificationChannel channel : list) {
                        String channel_id = channel.getId();
                        if (NOTIFICATION_CHANNEL_ID.equals(channel_id)) {
                            isCreated = true;
                            break;
                        }
                    }
                    if (!isCreated) {
                        createNotificationChannel();
                    }
                }
            }
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = NOTIFICATION_CHANNEL_NAME;
            String description = NOTIFICATION_DESCRIPTION;
            int importance = NotificationManager.IMPORTANCE_MAX;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            nm.createNotificationChannel(channel);
        }
    }
}

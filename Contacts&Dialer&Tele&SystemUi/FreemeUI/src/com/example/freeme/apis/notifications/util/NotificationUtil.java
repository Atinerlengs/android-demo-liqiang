package com.example.freeme.apis.notifications.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.content.ContextCompat;
import android.widget.RemoteViews;
import com.example.freeme.apis.R;
import com.example.freeme.apis.notifications.service.DownloadService;
import com.example.freeme.apis.notifications.ui.MainActivity;

public class NotificationUtil {

    private static final String TAG = "NotificationUtil";

    public static final int REPLY_INTENT_ID = 0;
    public static final int ARCHIVE_INTENT_ID = 1;

    public static final int STANDARD_ID = 1001;
    public static final int BUNDLED_FIREST_ID = 1101;
    public static final int BUNDLED_SECOND_ID = 1102;
    public static final int BUNDLED_THIRD_ID = 1103;
    public static final int BUNDLED_FOURTH_ID = 1104;
    public static final int REMOTE_INPUT_ID = 1003;
    public static final int CUSTOM_CONTENT_VIEW_ID = 1004;
    public static final int CUSTOM_BIG_CONTENT_VIEW_ID = 1005;
    public static final int CUSTOM_BOTH_CONTENT_VIEW_ID = 1006;
    public static final int CUSTOM_MEDIA_VIEW_ID = 1007;
    public static final int CUSTOM_LAYOUT_HEADSUP_ID = 1008;
    public static final int INBOX_STYLE_ID = 1009;
    public static final int BIG_PICTRUE_ID = 1010;

    public static final String LABEL_REPLY = "Reply";
    public static final String LABEL_ARCHIVE = "Archive";
    public static final String REPLY_ACTION = "com.hitherejoe.notifi.util.ACTION_MESSAGE_REPLY";
    public static final String KEY_PRESSED_ACTION = "KEY_PRESSED_ACTION";
    public static final String KEY_TEXT_REPLY = "KEY_TEXT_REPLY";
    private static final String KEY_NOTIFICATION_GROUP = "KEY_NOTIFICATION_GROUP";

    public static NotificationManager mNotificationManager;
    public static final String PRIMARY_CHANNEL = "default";
    public static final String SECONDARY_CHANNEL = "second";
    public static final String THIRD_CHANNEL = "third";

    // 1.  Standard
    public static  void showStandardNotification(Context context){
        NotificationCompat.Builder notification = createNotificationBuilder(context,
                "Standard Notification", "This is just a standard notification!");
        showNotification(context, notification.build(), STANDARD_ID);
    }

    // 2.  Bundled
    public static  void showBundledNotifications(Context context) {

        PendingIntent archiveIntent = PendingIntent.getActivity(context,
                ARCHIVE_INTENT_ID,
                getMessageReplyIntent(LABEL_ARCHIVE),
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action replyAction =
                new NotificationCompat.Action.Builder(android.R.drawable.sym_def_app_icon,
                        LABEL_REPLY, archiveIntent)
                        .build();
        NotificationCompat.Action archiveAction =
                new NotificationCompat.Action.Builder(android.R.drawable.sym_def_app_icon,
                        LABEL_ARCHIVE, archiveIntent)
                        .build();

        NotificationCompat.Builder first = createNotificationBuilder(
                context, "First notification", "This is the first bundled notification");
        first.setGroupSummary(true).setGroup(KEY_NOTIFICATION_GROUP);

        NotificationCompat.Builder second = createNotificationBuilder(
                context, "Second notification", "Here's the second one");
        second.setGroup(KEY_NOTIFICATION_GROUP);

        NotificationCompat.Builder third = createNotificationBuilder(
                context, "Third notification", "And another for luck!");
        third.setGroup(KEY_NOTIFICATION_GROUP);
        third.addAction(replyAction);
        third.addAction(archiveAction);

        NotificationCompat.Builder fourth = createNotificationBuilder(
                context, "Fourth notification", "This one sin't a part of our group");
        third.setGroup(KEY_NOTIFICATION_GROUP);

        showNotification(context, first.build(), BUNDLED_FIREST_ID);
        showNotification(context, second.build(), BUNDLED_SECOND_ID);
        showNotification(context, third.build(), BUNDLED_THIRD_ID);
        showNotification(context, fourth.build(), BUNDLED_FOURTH_ID);
    }

    // 3.  Reply
    public static  void showRemoteInputNotification(Context context) {
        RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY)
                .setLabel(context.getString(R.string.notification_text_label_reply))
                .build();

        PendingIntent replyIntent = PendingIntent.getActivity(context,
                REPLY_INTENT_ID,
                getMessageReplyIntent(LABEL_REPLY),
                PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent archiveIntent = PendingIntent.getActivity(context,
                ARCHIVE_INTENT_ID,
                getMessageReplyIntent(LABEL_ARCHIVE),
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action replyAction =
                new NotificationCompat.Action.Builder(android.R.drawable.sym_def_app_icon,
                        LABEL_REPLY, replyIntent)
                        .addRemoteInput(remoteInput)
                        .build();

        NotificationCompat.Action archiveAction =
                new NotificationCompat.Action.Builder(android.R.drawable.sym_def_app_icon,
                        LABEL_ARCHIVE, archiveIntent)
                        .build();

        NotificationCompat.Builder builder =
                createNotificationBuilder(context, "Remote input", "Try typing some text!");
        builder.addAction(replyAction);
        builder.addAction(archiveAction);

        showNotification(context, builder.build(), REMOTE_INPUT_ID);
    }

    // 4.  CustomContentView
    public static void showCustomContentViewNotification(Context context) {
        RemoteViews remoteViews = createRemoteViews(context,
                R.layout.notification_custom_content, R.drawable.ic_phonelink_ring_primary_24dp,
                "Custom notification", "This is a custom layout",
                R.drawable.ic_priority_high_primary_24dp);
        Notification.Builder builder = createCustomNotificationBuilder(context, PRIMARY_CHANNEL);
        builder.setCustomContentView(remoteViews).setStyle(new Notification.DecoratedCustomViewStyle());
        showNotification(context, builder.build(), CUSTOM_CONTENT_VIEW_ID);
    }

    // 5.  CustomBigContentView
    public static void showCustomBigContentViewNotification(Context context) {
        RemoteViews remoteViews = createRemoteViews(context,
                R.layout.notification_custom_big_content, R.drawable.ic_phonelink_ring_primary_24dp,
                "Custom notification", "This one is a little bigger!",
                R.drawable.ic_priority_high_primary_24dp);

        Notification.Builder builder = createCustomNotificationBuilder(context, PRIMARY_CHANNEL);
        builder.setCustomBigContentView(remoteViews)
                .setStyle(new Notification.DecoratedCustomViewStyle());

        showNotification(context, builder.build(), CUSTOM_BIG_CONTENT_VIEW_ID);
    }

    // 6.  CustomBothContentView
    public static void showCustomBothContentViewNotification(Context context) {
        RemoteViews remoteViews = createRemoteViews(context, R.layout.notification_custom_content,
                R.drawable.ic_phonelink_ring_primary_24dp, "Custom notification",
                "This is a custom layout", R.drawable.ic_priority_high_primary_24dp);

        RemoteViews bigRemoteView = createRemoteViews(context,
                R.layout.notification_custom_big_content, R.drawable.ic_phonelink_ring_primary_24dp,
                "Custom notification", "This one is a little bigger",
                R.drawable.ic_priority_high_primary_24dp);

        Notification.Builder builder = createCustomNotificationBuilder(context, PRIMARY_CHANNEL);
        builder.setCustomContentView(remoteViews)
                .setCustomBigContentView(bigRemoteView)
                .setStyle(new Notification.DecoratedCustomViewStyle());

        showNotification(context, builder.build(), CUSTOM_BOTH_CONTENT_VIEW_ID);
    }

    // 7.  CustomMediaView
    public static void showCustomMediaViewNotification(Context context) {
        RemoteViews remoteViews = createRemoteViews(context, R.layout.notification_custom_content,
                R.drawable.ic_phonelink_ring_primary_24dp, "Custom media notification",
                "This is a custom media layout", R.drawable.ic_play_arrow_primary_24dp);

        Notification.Builder builder = createCustomNotificationBuilder(context, PRIMARY_CHANNEL);
        builder.setCustomContentView(remoteViews)
                .setStyle(new Notification.DecoratedMediaCustomViewStyle());

        showNotification(context, builder.build(), CUSTOM_MEDIA_VIEW_ID);
    }

    // 8.  CustomLayoutHeadsUp
    public static void showCustomLayoutHeadsUpNotification(Context context) {

        RemoteViews remoteViews = createRemoteViews(context,
                R.layout.notification_custom_content, R.drawable.ic_phonelink_ring_primary_24dp,
                "Heads up!", "This is a custom heads-up notification",
                R.drawable.ic_priority_high_primary_24dp);

        Intent notificationIntent = new Intent(Intent.ACTION_VIEW);
        notificationIntent.setData(Uri.parse("http://www.hitherejoe.com"));
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        Notification.Builder builder = createCustomNotificationBuilder(context, SECONDARY_CHANNEL);
        builder.setCustomContentView(remoteViews)
                .setStyle(new Notification.DecoratedCustomViewStyle())
                .setPriority(Notification.PRIORITY_HIGH)
                .setVibrate(new long[0])
                .setContentIntent(contentIntent);

        Intent push = new Intent();
        push.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        push.setClass(context, MainActivity.class);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, 0,
                push, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setFullScreenIntent(fullScreenPendingIntent, true);
        showNotification(context, builder.build(), CUSTOM_LAYOUT_HEADSUP_ID);
    }

    // 9. InboxStyle
    public static void showInboxStyleNotification(Context context){
        NotificationCompat.Builder notificationBuider = createNotificationBuilder(context,
                "InboxStyle","This is a inbox style notification");
        android.support.v4.app.NotificationCompat.InboxStyle inboxStyle = new android.support.v4.app.NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("BigContentTitle")
                .addLine("first line，first line，first line")
                .addLine("second line")
                .addLine("third line")
                .addLine("fourth line")
                .addLine("five line")
                .setSummaryText("SummaryText");
        notificationBuider.setStyle(inboxStyle);
        showNotification(context, notificationBuider.build(), INBOX_STYLE_ID);
    }

    // 10. BigPictureStyle
    public static void showBigPictureNotification(Context context){
        NotificationCompat.Builder notificationBuider = createNotificationBuilder(context,
                "BigPictureStyle","This is a big picture style notification");
        android.support.v4.app.NotificationCompat.BigPictureStyle style = new android.support.v4.app.NotificationCompat.BigPictureStyle();
        style.setBigContentTitle("BigContentTitle");
        style.setSummaryText("SummaryText");
        style.bigPicture(BitmapFactory.decodeResource(context.getResources(),R.drawable.notification_small));
        notificationBuider.setStyle(style);
        showNotification(context, notificationBuider.build(), BIG_PICTRUE_ID);
    }

    // 11. DownLoad
    public static void showDownloadNotification(Context context,int progress){
        NotificationCompat.Builder builder = createSpecialNotificationBuilder(context,
                "DownLoad","download..."+progress+"%");
        builder.setProgress(100,progress,false);
        builder.setOngoing(true);
        builder.setShowWhen(false);
        Intent intent = new Intent(context,DownloadService.class);
        intent.putExtra("command",1);
        showNotification(context, builder.build(), MainActivity.TYPE_Progress);
    }

    public static void cancelNotification(){
        mNotificationManager.cancel(MainActivity.TYPE_Progress);
    }

    private static RemoteViews createRemoteViews(Context context, int layout, int iconResource,
            String title, String message, int imageResource) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), layout);
        remoteViews.setImageViewResource(R.id.image_icon, iconResource);
        remoteViews.setTextViewText(R.id.text_title, title);
        remoteViews.setTextViewText(R.id.text_message, message);
        remoteViews.setTextColor(R.id.text_title, R.color.notification_content);
        remoteViews.setTextColor(R.id.text_message, R.color.notification_content);
        remoteViews.setImageViewResource(R.id.image_end, imageResource);
        return remoteViews;
    }

    private static Intent getMessageReplyIntent(String label) {
        return new Intent()
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .setAction(REPLY_ACTION)
                .putExtra(KEY_PRESSED_ACTION, label);
    }

    public static Notification.Builder createCustomNotificationBuilder(Context context, String channels) {
        return new Notification.Builder(context, channels)
                .setSmallIcon(R.drawable.ic_phonelink_ring_primary_24dp)
                .setColor(ContextCompat.getColor(context, R.color.notification_primary))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true);
    }

    public static NotificationCompat.Builder createSpecialNotificationBuilder(Context context,
            String title, String message){
        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.notification_me);
        return new NotificationCompat.Builder(context, THIRD_CHANNEL)
                .setSmallIcon(R.drawable.ic_phonelink_ring_primary_24dp)
                .setContentTitle(title)
                .setContentText(message)
                .setColor(ContextCompat.getColor(context, R.color.notification_primary))
                .setLargeIcon(largeIcon)
                .setAutoCancel(true);
    }

    public static NotificationCompat.Builder createNotificationBuilder(Context context,
            String title, String message){
        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.notification_me);
        return new NotificationCompat.Builder(context, PRIMARY_CHANNEL)
                .setSmallIcon(R.drawable.ic_phonelink_ring_primary_24dp)
                .setContentTitle(title)
                .setContentText(message)
                .setColor(ContextCompat.getColor(context, R.color.notification_primary))
                .setLargeIcon(largeIcon)
                //default ringtone, vibrate and lighting
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true);
    }

    private static void showNotification(Context context, Notification notification, int id){
       getNotificationManager(context).notify(id, notification);
    }

    public static NotificationManager getNotificationManager(Context context) {
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mNotificationManager;
    }

}

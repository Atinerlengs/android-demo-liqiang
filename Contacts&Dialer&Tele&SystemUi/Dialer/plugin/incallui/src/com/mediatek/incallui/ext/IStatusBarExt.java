package com.mediatek.incallui.ext;

import android.app.Notification.Builder;
import android.graphics.Bitmap;

public interface IStatusBarExt {
    /**
     * Show status bar hd icon when the call have property of HIGH_DEF_AUDIO.
     * Plugin need to use call capability to show or dismiss statuar bar icon.
     *
     * @param obj    the incallui call
     */
    void updateInCallNotification(Object obj);

    /**
      * Check for notification change when call updated in calllist.
      * @return true if notification need to be updated
      */
    public boolean checkForNotificationChange();

    /**
      * Update the incall notification when vowifi call quality status changes.
      * @param builder the incallui notification builder
      * @param icon the notification large icon
      */
    void customizeNotification(Builder builder, Bitmap icon);

    /**
      * Update the status bar notification only when registered.
      */
    boolean needUpdateNotification();

    /**
      * Get StatusBarNotifier instance.
      * @param statusBarNotifier statusBarNotifier
      */
    void getStatusBarNotifier(Object statusBarNotifier);
}
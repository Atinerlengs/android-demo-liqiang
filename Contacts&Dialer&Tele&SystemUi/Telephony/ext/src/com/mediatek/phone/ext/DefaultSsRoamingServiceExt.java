package com.mediatek.phone.ext;

import android.content.Context;


public class DefaultSsRoamingServiceExt implements ISsRoamingServiceExt {

    /** For op01.
     * register ss roaming receiver
     * @param context context
     */
    @Override
    public void registerSsRoamingReceiver(Context context) {
        // do nothing
    }

    /**
     * Check if need to display roaming notification or not.
     * @param context context host app
     * @return false if notification must not be displayed
     */
    public boolean isNotificationForRoamingAllowed(Context context) {
        return true;
    }

    /**
     * Check if need to display roaming notification based on roaming condition.
     * @param context context host app
     * @return false if notification must not be displayed
     */
    public boolean isDisplayNotificationForRoaming(Context context) {
        return false;
    }
}

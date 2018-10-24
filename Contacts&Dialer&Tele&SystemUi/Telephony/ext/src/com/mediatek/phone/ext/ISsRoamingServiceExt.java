package com.mediatek.phone.ext;


import android.content.Context;


public interface ISsRoamingServiceExt {

    /** For op01.
     * register ss roaming receiver
     * @param context context
     */
    public void registerSsRoamingReceiver(Context context);

    /**
     * Check if need to display roaming notification or not.
     * @param context context of app
     * @return false if notification must not be displayed
     */
    public boolean isNotificationForRoamingAllowed(Context context);

    /**
     * Check if network condition to display roaming notification or not.
     * @param context context of app
     * @return false if notification must not be displayed
     */
    public boolean isDisplayNotificationForRoaming(Context context);
}

package com.mediatek.incallui.ext;

import android.os.Bundle;

public interface IInCallButtonExt {

    /**
     * Checks if contact is video call capable through presence
     * @param number number to get video capability.
     * @return true if contact is video call capable.
     */
    boolean isVideoCallCapable(String number);

    /**
     * Show toast for GTT feature.
     * @param extra extra.
     */
    void showToastForGTT(Bundle extra);

    /**
     * Checks if the device switch feature [Digits] is supported
     * @param the call object.
     * @return true if device switch [Digits] Feature is supported.
     */
    boolean isDeviceSwitchSupported(Object call);

    /**
     * Handle the call button item click
     * @param the id of the button clicked.
     * @return true if handled by plugin.
     */
    public boolean onMenuItemClick(int id);
}

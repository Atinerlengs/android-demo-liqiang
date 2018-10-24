package com.mediatek.dialer.ext;

import android.content.Context;
import android.telecom.PhoneAccountHandle;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class DefaultCallDetailExtension implements ICallDetailExtension {
    private static final String TAG = "DefaultCallDetailExtension";

    /**
     * for op01
     * @param durationView the duration text
     */
    @Override
    public void setDurationViewVisibility(TextView durationView) {
        log("setDurationViewVisibility");
    }

    /**
     * for op01,add for "blacklist" in call detail.
     * @param menu blacklist menu.
     * @param number phone number.
     * @param name contact name.
     */
    @Override
    public void onPrepareOptionsMenu(Object obj, Menu menu, CharSequence name,
            CharSequence number) {
        //do-nothing
    }

    private void log(String msg) {
        //Log.d(TAG, msg + " default");
    }

    /**
     * for OP09.
     * @param context context
     * @param phoneAccountHandle phoneAccountHandle
     */
    public void setCallAccountForCallDetail(Context context,
            PhoneAccountHandle phoneAccountHandle) {
        log("setCallAccountForCallDetail");
    }

    /**
     * for OP01, change call type text.
     * @param context context
     * @param callTypeTextView callTypeTextView
     * @param isVideoCall isVideoCall
     * @param callType callType
     */
    @Override
    public void changeVideoTypeText(Context context, TextView callTypeTextView,
            boolean isVideoCall, int callType) {
        //default do nothing
    }

    /**
     * for OP01, set navigation.
     * @param Object activity
     * @param Object toolbar
     */
    public void onCreate(Object activity, Object toolbar) {
        //default do nothing
    }

}

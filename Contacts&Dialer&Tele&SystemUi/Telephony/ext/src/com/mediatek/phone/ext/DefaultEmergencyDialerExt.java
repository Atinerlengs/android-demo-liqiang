package com.mediatek.phone.ext;

import android.app.Activity;
import android.util.Log;

/**
 * Telecom account registry extension plugin for op09.
*/
public class DefaultEmergencyDialerExt implements IEmergencyDialerExt {

    /**
     * Called when need to update dial buttons.
     * @param activity need to update.
     */
    public void onCreate(Activity activity) {
        Log.d("DefaultEmergencyDialerExt", "onCreate");
    }

    /**
     * Called when destory emergency dialer.
     * @param activity need to update
     */
    public void onDestroy(Activity activity) {
        Log.d("DefaultEmergencyDialerExt", "onDestroy");
    }

    /**
     * Called to get the ECBM dialog text.
     * @param phoneObj phone object
     * @param dialogType dialog Type
     * @param millisUntilFinished time to finish ECBM
     * @return dialog text
     */
    @Override
    public String getDialogText(Object phoneObj, int dialogType, long millisUntilFinished) {
        Log.d("DefaultEmergencyDialerExt", "getDialogText");
        return null;
    }
}

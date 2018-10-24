package com.mediatek.settings;

import android.content.Context;
import android.preference.SwitchPreference;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.widget.Toast;

import com.android.phone.R;

//import com.mediatek.ims.internal.MtkImsManager;


/**
 * Add this class for [MTK_Enhanced4GLTE]
 * we don't always want the switch preference always auto switch, and save the preference.
 * In some conditions the switch should not be switch, and show a toast to user.
 */
public class Enhanced4GLteSwitchPreference extends SwitchPreference {
    private static final String LOG_TAG = "Enhanced4GLteSwitchPreference";
    private int mSubId;
    private int mPhoneId;
    //VOLTE IMS STATE
    public static final int IMS_STATE_DISABLED = 0;
    public static final int IMS_STATE_ENABLE = 1;
    public static final int IMS_STATE_ENABLING = 2;
    public static final int IMS_STATE_DISABLING = 3;

    /**
     * Initialize LTE preference for subId.
     * @param context activity context
     */
    public Enhanced4GLteSwitchPreference(Context context) {
        super(context);
    }

    /**
     * Initialize LTE preference for subId.
     * @param context activity context
     * @param subId Sim sub id
     */
    public Enhanced4GLteSwitchPreference(Context context, int subId) {
        this(context);
        mSubId = subId;
        mPhoneId = SubscriptionManager.getPhoneId(subId);
    }

    @Override
    protected void onClick() {
        if (canNotSetAdvanced4GMode()) {
            log("[onClick] can't set Enhanced 4G mode.");
            showTips(R.string.can_not_switch_enhanced_4g_lte_mode_tips);
        } else {
            log("[onClick] can set Enhanced 4G mode.");
            super.onClick();
        }
    }

    /**
     * Three conditions can't switch the 4G button.
     * 1. In call
     * 2. In the process of switching
     * 3. Airplane mode is on
     * @return
     */
    private boolean canNotSetAdvanced4GMode() {
        return TelephonyUtils.isInCall(getContext()) /*|| isInSwitchProcess()*/
             || TelephonyUtils.isAirplaneModeOn(getContext());
    }

    /**
     * Get the IMS_STATE_XXX, so can get whether the state is in changing.
     * @return true if the state is in changing, else return false.
     */
/*    private boolean isInSwitchProcess() {
        int imsState = IMS_STATE_DISABLED;
        try {
            imsState = MtkImsManager.getInstance(getContext(), mSubId).getImsState();
        } catch (ImsException e) {
            Log.e(LOG_TAG, "[isInSwitchProcess]" + e);
            return false;
        }
        log("[canSetAdvanced4GMode] imsState = " + imsState);
        return imsState == IMS_STATE_DISABLING
                || imsState == IMS_STATE_ENABLING;
    }*/

    /**
     * Used for update the subId.
     * @param subId Sim sub Id
     */
    public void setSubId(int subId) {
        mSubId = subId;
    }

    private void showTips(int resId) {
        Toast.makeText(getContext(), resId, Toast.LENGTH_SHORT).show();
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}

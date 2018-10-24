package com.mediatek.incallui.volte;

import android.os.Bundle;
import android.text.TextUtils;

import com.android.incallui.Log;
import com.android.incallui.call.CallList;
import com.android.incallui.call.DialerCall;
import com.mediatek.incallui.utils.InCallUtils;
import mediatek.telecom.MtkTelecomManager;

public class InCallUIVolteUtils {

    private static final String LOG_TAG = "InCallUIVolteUtils";
    private static final int INVALID_RES_ID = -1;

    public static boolean isVolteSupport() {
        return InCallUtils.MTK_IMS_SUPPORT && InCallUtils.MTK_VOLTE_SUPPORT;
    }

    //-------------For VoLTE normal call switch to ECC------------------
    public static boolean isVolteMarkedEcc(final android.telecom.Call.Details details) {
        boolean result = false;
        if (isVolteSupport() && details != null) {
            Bundle bundle = details.getExtras();
            if (bundle != null
                    && bundle.containsKey(MtkTelecomManager.EXTRA_VOLTE_MARKED_AS_EMERGENCY)) {
                Object value = bundle.get(MtkTelecomManager.EXTRA_VOLTE_MARKED_AS_EMERGENCY);
                if (value instanceof Boolean) {
                    result = (Boolean) value;
                }
            }
        }
        return result;
    }

    public static boolean isVolteMarkedEccChanged(final android.telecom.Call.Details oldDetails,
            final android.telecom.Call.Details newDetails) {
        boolean result = false;
        boolean isVolteMarkedEccOld = isVolteMarkedEcc(oldDetails);
        boolean isVolteMarkedEccNew = isVolteMarkedEcc(newDetails);
        result = !isVolteMarkedEccOld && isVolteMarkedEccNew;
        return result;
    }

    //-------------For VoLTE PAU field------------------
    public static String getVoltePauField(final android.telecom.Call.Details details) {
        String result = "";
        if (isVolteSupport() && details != null) {
            Bundle bundle = details.getExtras();
            if (bundle != null) {
                result = bundle.getString(MtkTelecomManager.EXTRA_VOLTE_PAU, "");
            }
        }
        return result;
    }

    public static String getPhoneNumber(final android.telecom.Call.Details details) {
        String result = "";
        if (details != null) {
            if (details.getGatewayInfo() != null) {
                result = details.getGatewayInfo()
                        .getOriginalAddress().getSchemeSpecificPart();
            } else {
                result = details.getHandle() == null ? null : details.getHandle()
                        .getSchemeSpecificPart();
            }
        }
        if (result == null) {
            result = "";
        }
        return result;
    }

    public static boolean isPhoneNumberChanged(final android.telecom.Call.Details oldDetails,
            final android.telecom.Call.Details newDetails) {
        boolean result = false;
        String numberOld = getPhoneNumber(oldDetails);
        String numberNew = getPhoneNumber(newDetails);
        result = !TextUtils.equals(numberOld, numberNew);
        if (result) {
            log("number changed from " + numberOld + " to " + numberNew);
        }
        return result;
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    /**
     * Check if a call is an incoming VoLTE conference call.
     * @param call the call to be checked.
     * @return true if yes.
     */
    public static boolean isIncomingVolteConferenceCall(DialerCall call) {
        return call != null
                && DialerCall.State.isIncomingOrWaiting(call.getState())
                && call.isConferenceCall()
                && call.hasProperty(mediatek.telecom.MtkCall.MtkDetails.MTK_PROPERTY_VOLTE);
    }

    /**
     * M: check incoming call conference call or not.
     * @return
     */
    public static boolean isIncomingVolteConferenceCall() {
        DialerCall call = CallList.getInstance().getIncomingCall();
        return InCallUIVolteUtils.isIncomingVolteConferenceCall(call);
    }
}

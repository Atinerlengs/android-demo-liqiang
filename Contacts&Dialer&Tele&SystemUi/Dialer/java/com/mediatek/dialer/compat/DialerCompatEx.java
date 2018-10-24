package com.mediatek.dialer.compat;

import android.content.Context;
import android.telecom.TelecomManager;
import android.util.Log;

/**
 * [portable]Dialer new features compatible
 */
public class DialerCompatEx {
    private static final String TAG = DialerCompatEx.class.getSimpleName();

    //[MTK SIM Contacts feature] INDICATE_PHONE_SIM,IS_SDN_CONTACT
    private static final String COMPAT_CLASS_MTKCONTACTSCONTRACT =
            "com.mediatek.provider.MtkContactsContract";
    private static final String COMPAT_FIELD_INDICATE_PHONE_SIM= "INDICATE_PHONE_SIM";
    private static Boolean sSimContactsCompat = null;

    public static boolean isSimContactsCompat() {
        if (sSimContactsCompat == null) {
            sSimContactsCompat = DialerCompatExUtils.isClassExits(COMPAT_CLASS_MTKCONTACTSCONTRACT);
            Log.d(TAG, "init isSimContactsCompat got " + sSimContactsCompat);
        }
        return sSimContactsCompat;
    }

    /* package */static void setSimContactsCompat(Boolean supported) {
        Log.d(TAG, "setSimContactsCompat supported: " + supported);
        sSimContactsCompat = supported;
    }

    // [VoLTE ConfCallLog] Whether the VoLTE conference calLog compatible.
    private static final String COMPAT_CLASS_CALLS = "com.mediatek.provider.MtkCallLog$Calls";
    private static final String COMPAT_FIELD_CONFERENCE_CALL_ID = "CONFERENCE_CALL_ID";
    private static Boolean sConferenceCallLogCompat = null;

    public static boolean isConferenceCallLogCompat() {
        if (sConferenceCallLogCompat == null) {
            sConferenceCallLogCompat = DialerCompatExUtils.isFieldAvailable(
                    COMPAT_CLASS_CALLS, COMPAT_FIELD_CONFERENCE_CALL_ID);
            Log.d(TAG, "init isConferenceCallLogCompat got " + sConferenceCallLogCompat);
        }
        return sConferenceCallLogCompat;
    }

    /* package */static void setConferenceCallLogCompat(Boolean supported) {
        Log.d(TAG, "setConferenceCallLogCompat supported: " + supported);
        sConferenceCallLogCompat = supported;
    }

    // [VoLTE ConfCall] Whether the VoLTE enhanced conference call (Launch
    // conference call directly from dialer) supported.
    // here use carrier config in telecom
    private static final String COMPAT_CLASS_MTK_CARRIER_CONFIG =
            "mediatek.telephony.MtkCarrierConfigManager";
    private static final String COMPAT_FIELD_CAPABILITY_VOLTE_CONFERENCE_ENHANCED =
            "MTK_KEY_VOLTE_CONFERENCE_ENHANCED_ENABLE_BOOL";
    private static Boolean sVolteEnhancedConfCallCompat = null;

    public static boolean isVolteEnhancedConfCallCompat() {
        if (sVolteEnhancedConfCallCompat == null) {
            sVolteEnhancedConfCallCompat = DialerCompatExUtils.isFieldAvailable(
                COMPAT_CLASS_MTK_CARRIER_CONFIG, COMPAT_FIELD_CAPABILITY_VOLTE_CONFERENCE_ENHANCED);
            Log.d(TAG, "init isVolteEnhancedConfCallCompat got " + sVolteEnhancedConfCallCompat);
        }
        return sVolteEnhancedConfCallCompat;
    }

    private static final String COMPAT_CLASS_TMEX = "com.mediatek.telephony.MtkTelephonyManagerEx";
    private static final String COMPAT_METHOD_WFC = "isWifiCallingEnabled";
    private static Boolean sWfcCompat = null;
    public static boolean isWfcCompat() {
        if (sWfcCompat == null) {
            sWfcCompat = DialerCompatExUtils.isMethodAvailable(COMPAT_CLASS_TMEX, COMPAT_METHOD_WFC,
                int.class);
            Log.d(TAG, "init isWfcCompat:" + sWfcCompat);
        }
        return sWfcCompat;
    }
//    /**
//     * Blocked Number Permission check for portable. corresponding to
//     * BlockedNumberProvider, only default or system dialer can read/write its db.
//     */
//    public static boolean isDefaultOrSystemDialer(Context context) {
//        String self = context.getApplicationInfo().packageName;
//        final TelecomManager telecom = context.getSystemService(TelecomManager.class);
//        if (self.equals(telecom.getDefaultDialerPackage())
//                || self.equals(telecom.getSystemDialerPackage())) {
//            return true;
//        }
//        Log.d(TAG, "isDefaultOrSystemDialer, return false");
//        return false;
//    }
}

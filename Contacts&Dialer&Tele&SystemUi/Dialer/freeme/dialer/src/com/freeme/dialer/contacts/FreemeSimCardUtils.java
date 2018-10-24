package com.freeme.dialer.contacts;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.widget.Toast;

import com.android.dialer.R;
import com.freeme.contacts.common.utils.FreemeLogUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.internal.telephony.IMtkTelephonyEx;

public class FreemeSimCardUtils {
    private static final String TAG = "FreemeSimCardUtils";

    public interface SimType {
        public static final String SIM_TYPE_USIM_TAG = "USIM";
        public static final String SIM_TYPE_SIM_TAG = "SIM";
        public static final String SIM_TYPE_RUIM_TAG = "RUIM";
        public static final String SIM_TYPE_CSIM_TAG = "CSIM";
        public static final String SIM_TYPE_UNKNOWN_TAG = "UNKNOWN";
    }

    /**
     * check PhoneBook State is ready if ready, then return true.
     *
     * @param subId
     * @return
     */
    public static boolean isPhoneBookReady(int subId) {
        final IMtkTelephonyEx telephonyEx = IMtkTelephonyEx.Stub.asInterface(ServiceManager
                .getService("phoneEx"));
        if (null == telephonyEx) {
            FreemeLogUtils.w(TAG, "[isPhoneBookReady]phoneEx == null");
            return false;
        }
        boolean isPbReady = false;
        try {
            isPbReady = telephonyEx.isPhbReady(subId);
        } catch (RemoteException e) {
            FreemeLogUtils.e(TAG, "[isPhoneBookReady]catch exception:");
            e.printStackTrace();
        }
        FreemeLogUtils.d(TAG, "[isPhoneBookReady]subId:" + subId + ", isPbReady:" + isPbReady);
        return isPbReady;
    }

    /**
     * Check that whether the phone book is ready only
     *
     * @param context
     *            the caller's context.
     * @param subId
     *            the slot to check.
     * @return true the phb is ready false the phb is not ready
     */
    public static boolean isPhoneBookReady(Context context, int subId) {
        boolean hitError = false;
        int errorToastId = -1;
        if (!isPhoneBookReady(subId)) {
            hitError = true;
            errorToastId = R.string.icc_phone_book_invalid;
        }
        if (context == null) {
            FreemeLogUtils.w(TAG, "[isPhoneBookReady] context is null,subId:" + subId);
        }
        if (hitError && context != null) {
            Toast.makeText(context, errorToastId, Toast.LENGTH_LONG).show();
            FreemeLogUtils.d(TAG, "[isPhoneBookReady] hitError=" + hitError);
        }
        return !hitError;
    }

    private static final String[] UICCCARD_PROPERTY_TYPE = {
            "gsm.ril.uicctype",
            "gsm.ril.uicctype2",
            "gsm.ril.uicctype3",
            "gsm.ril.uicctype4",
    };

    private static String getSimTypeByProperty(int subId) {
        int slotId = SubInfoUtils.getSlotIdUsingSubId(subId);
        String cardType = null;

        if (slotId >= 0 && slotId < 4) {
            cardType = SystemProperties.get(UICCCARD_PROPERTY_TYPE[slotId]);
        }
        FreemeLogUtils.d(TAG, "[getSimTypeByProperty]slotId=" + slotId + ", cardType=" + cardType);
        return cardType;
    }

    /**
     * get sim type by subId, sim type is defined in
     * FreemeSimCardUtils.SimType
     *
     * @param subId
     * @return FreemeSimCardUtils.SimType
     */
    public static String getSimTypeBySubId(int subId) {
        String simType = FreemeSimCardUtils.SimType.SIM_TYPE_UNKNOWN_TAG;
        final IMtkTelephonyEx iTel = IMtkTelephonyEx.Stub.asInterface(ServiceManager
                .getService("phoneEx"));
        if (iTel == null) {
            FreemeLogUtils.w(TAG, "[getSimTypeBySubId]iTel == null");
            return simType;
        }
        try {
            simType = iTel.getIccCardType(subId);
            if (simType == null || simType.isEmpty()) {
                simType = getSimTypeByProperty(subId);
            }
        } catch (RemoteException e) {
            FreemeLogUtils.e(TAG, "[getSimTypeBySubId]catch exception:");
            e.printStackTrace();
        }
        return simType;
    }

    public static boolean isUsimType(String simType) {
        if (FreemeSimCardUtils.SimType.SIM_TYPE_USIM_TAG.equals(simType)) {
            FreemeLogUtils.d(TAG, "[isUsimType] true");
            return true;
        }
        FreemeLogUtils.d(TAG, "[isUsimType] false");
        return false;
    }

    public static boolean isCsimType(String simType) {
        if (FreemeSimCardUtils.SimType.SIM_TYPE_CSIM_TAG.equals(simType)) {
            FreemeLogUtils.d(TAG, "[isCsimType] true");
            return true;
        }
        FreemeLogUtils.d(TAG, "[isCsimType] false");
        return false;
    }

    /**
     * check whether a slot is insert a usim or csim card
     *
     * @param subId
     * @return true if it is usim or csim card
     */
    public static boolean isUsimOrCsimType(int subId) {
        String simType = getSimTypeBySubId(subId);
        return isUsimType(simType) || isCsimType(simType);
    }

    /**
     * M: [Gemini+] not only ready, but also idle for all sim operations its
     * requirement is: 1. iccCard is insert 2. radio is on 3. FDN is off 4. PHB
     * is ready 5. simstate is ready 6. simService is not running
     *
     * @param subId
     *            the slotId to check
     * @return true if idle
     */
    public static boolean isSimStateIdle(Context context, int subId) {
        FreemeLogUtils.i(TAG, "[isSimStateIdle] subId: " + subId);
        if (!SubInfoUtils.checkSubscriber(subId)) {
            return false;
        }
        // /change for SIM Service Refactoring
        boolean isSimServiceRunning = FreemeSimServiceUtils.isServiceRunning(context, subId);
        FreemeLogUtils.i(TAG, "[isSimStateIdle], isSimServiceRunning = " + isSimServiceRunning);
        return !isSimServiceRunning && isPhoneBookReady(subId);
    }
}

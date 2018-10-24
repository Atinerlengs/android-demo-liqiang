/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
package com.mediatek.settings;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.Preference;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.telephony.RadioAccessFamily;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;

import com.android.phone.GsmUmtsAdditionalCallOptions;
import com.android.phone.GsmUmtsCallForwardOptions;
import com.android.phone.SubscriptionInfoHelper;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.mediatek.ims.internal.MtkImsManager;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.MtkPhoneConstants;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import com.mediatek.phone.PhoneFeatureConstants.FeatureOption;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.settings.cdma.CdmaCallWaitingUtOptions;

//M: Add for data roaming tips
import com.mediatek.settings.cdma.TelephonyUtilsEx;
import mediatek.telephony.MtkCarrierConfigManager;

public class TelephonyUtils {
    private static final String TAG = "TelephonyUtils";
    public static final int MODEM_2G = 0x02;
    public static final String USIM = "USIM";
    ///Add for [Dual_Mic]
    private static final String DUALMIC_MODE = "Enable_Dual_Mic_Setting";
    private static final String GET_DUALMIC_MODE = "Get_Dual_Mic_Setting";
    private static final String DUALMIC_ENABLED = "Get_Dual_Mic_Setting=1";

    /// M: Add for dual volte feature @{
    private final static int DATA_USAGE_DIALOG = 1001;
    private final static int DATA_ROAMING_DIALOG = 1002;
    private final static int DATA_TRAFFIC_DIALOG = 1003;
    private static final String ADDITIONAL_GSM_SETTINGS_KEY = "additional_gsm_call_settings_key";
    private final static String BUTTON_CB_EXPAND = "button_cb_expand_key";
    private final static String CALL_FORWARDING_KEY = "call_forwarding_key";
    private static final String KEY_CALL_FORWARD = "button_cf_expand_key";
    private static final String KEY_CALLER_ID = "button_caller_id";
    private static final String SUB_ID_EXTRA =
            "com.android.phone.settings.SubscriptionInfoHelper.SubscriptionId";
    /// @}
    /// Add for [HAC]
    private static final String GET_HAC_SUPPORT = "GET_HAC_SUPPORT";
    private static final String GET_HAC_SUPPORT_ON = "GET_HAC_SUPPORT=1";
    private static final String GET_HAC_ENABLE = "GET_HAC_ENABLE";
    private static final String GET_HAC_ENABLE_ON = "GET_HAC_ENABLE=1";

    public static final int GET_PIN_PUK_RETRY_EMPTY = -1;
    public static final String[] PROPERTY_SIM_PIN2_RETRY = {
        "gsm.sim.retry.pin2",
        "gsm.sim.retry.pin2.2",
        "gsm.sim.retry.pin2.3",
        "gsm.sim.retry.pin2.4",
    };

    public static final String PROPERTY_SIM_PUK2_RETRY[] = {
        "gsm.sim.retry.puk2",
        "gsm.sim.retry.puk2.2",
        "gsm.sim.retry.puk2.3",
        "gsm.sim.retry.puk2.4",
    };

    private static final String[] CMCC_CU_NUMERIC = {"46000", "46002", "46007", "46008",
        "46001", "46006", "46009", "45407"};
    /// M: Add for dual volte feature @{
    private static int mDialogID = -1;
    private static Preference[] mPreferences = {null, null, null, null};
    /// @}
    private static final String OPERATOR_OP09 = "OP09";
    private static final String SEGDEFAULT = "SEGDEFAULT";
    private final static String ONE = "1";
    public static final String ACTION_NETWORK_CHANGED =
            "com.mediatek.intent.action.ACTION_NETWORK_CHANGED";


    /**
     * set DualMic noise reduction mode.
     * @param dualMic the value to show the user set
     */
    public static void setDualMicMode(String dualMic) {
        Context context = PhoneGlobals.getInstance().getApplicationContext();
        if (context == null) {
            return;
        }
        AudioManager audioManager = (AudioManager)
                context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setParameters(DUALMIC_MODE + "=" + dualMic);
    }

    /**
     * get DualMic noise reduction mode.
     */
    public static boolean isDualMicModeEnabled() {
        Context context = PhoneGlobals.getInstance().getApplicationContext();
        if (context == null) {
            return false;
        }
        String state = null;
        AudioManager audioManager = (AudioManager)
                context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            state = audioManager.getParameters(GET_DUALMIC_MODE);
            log("getDualMicMode(): state: " + state);
            if (state.equalsIgnoreCase(DUALMIC_ENABLED)) {
                return true;
            }
        }
        return false;
    }

    /**
     * add for HAC(hearing aid compatible).
     * if return true, support HAC ,show UI. otherwise, disappear.
     * @return true, support. false, not support.
     */
    public static boolean isHacSupport() {
        Context context = PhoneGlobals.getInstance().getApplicationContext();
        if (context == null) {
            return false;
        }
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            String hac = audioManager.getParameters(GET_HAC_SUPPORT);
            log("hac support: " + hac);
            return GET_HAC_SUPPORT_ON.equals(hac);
        }
        return false;
    }

    /**
     * Get HAC's state. For upgrade issue.
     * In KK we don't use DB, so we still need use query Audio State to sync with DB.
     * @return 1, HAC enable; 0, HAC disable.
     */
    public static int isHacEnable() {
        Context context = PhoneGlobals.getInstance().getApplicationContext();
        if (context == null) {
            log("isHacEnable : context is null");
            return 0;
        }
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            String hac = audioManager.getParameters(GET_HAC_ENABLE);
            log("hac enable: " + hac);
            return GET_HAC_ENABLE_ON.equals(hac) ? 1 : 0;
        }
        log("isHacEnable : audioManager is null");
        return 0;
    }

    /**
     * Check if the subscription card is USIM or SIM.
     * @param context using for query phone
     * @param subId according to the phone
     * @return true if is USIM card
     */
    public static boolean isUSIMCard(Context context, int subId) {
        log("isUSIMCard()... subId = " + subId);
        String type = MtkTelephonyManagerEx.getDefault().getIccCardType(subId);
        log("isUSIMCard()... type = " + type);
        return USIM.equals(type);
    }

    public static boolean isSimStateReady(int slot) {
        boolean isSimStateReady = false;
        isSimStateReady = TelephonyManager.SIM_STATE_READY == TelephonyManager.
                getDefault().getSimState(slot);
        log("isSimStateReady: "  + isSimStateReady);
        return isSimStateReady;
    }

    public static void goUpToTopLevelSetting(Activity activity, Class<?> targetClass) {
        Intent intent = new Intent(activity.getApplicationContext(), targetClass);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
        activity.finish();
    }

    public static boolean isAllRadioOff(Context context) {
        boolean result = true;
        boolean airplaneModeOn = isAirplaneModeOn(context);
        int subId;
        List<SubscriptionInfo> activeSubList = PhoneUtils.getActiveSubInfoList();
        for (int i = 0; i < activeSubList.size(); i++) {
            subId = activeSubList.get(i).getSubscriptionId();
            if (isRadioOn(subId, context)) {
                result = false;
                break;
            }
        }
        return result || airplaneModeOn;
    }

    /**
     * check the radio is on or off by sub id.
     *
     * @param subId the sub id
     * @return true if radio on
     */
    public static boolean isRadioOn(int subId, Context context) {
        log("[isRadioOn]subId:" + subId);
        boolean isRadioOn = false;
        final ITelephony iTel = ITelephony.Stub.asInterface(
                ServiceManager.getService(Context.TELEPHONY_SERVICE));
        if (iTel != null && PhoneUtils.isValidSubId(subId)) {
            try {
                isRadioOn = iTel.isRadioOnForSubscriber(subId, context.getPackageName());
            } catch (RemoteException e) {
                log("[isRadioOn] failed to get radio state for sub " + subId);
                isRadioOn = false;
            }
        } else {
            log("[isRadioOn]failed to check radio");
        }
        log("[isRadioOn]isRadioOn:" + isRadioOn);

        return isRadioOn && !isAirplaneModeOn(PhoneGlobals.getInstance());
    }

    /**
     * Get pin2 left retry times.
     * @param subId the sub which one user want to get
     * @return the left times
     */
    public static int getPin2RetryNumber(int subId) {
        if (!PhoneUtils.isValidSubId(subId)) {
            log("getPin2RetryNumber : inValid SubId = " + subId);
            return -1;
        }
        int slot = SubscriptionManager.getSlotIndex(subId);
        log("getPin2RetryNumber : --> Sub:Slot = " + subId + ":" + slot);
        String pin2RetryStr = null;
        try {
        if (isGeminiProject()) {
            if (slot < PROPERTY_SIM_PIN2_RETRY.length) {
                pin2RetryStr = PROPERTY_SIM_PIN2_RETRY[slot];
            } else {
                Log.w(TAG, "PIN2 --> Slot num is invalid : Error happened !!");
                pin2RetryStr = PROPERTY_SIM_PIN2_RETRY[0];
            }
        } else {
            pin2RetryStr = PROPERTY_SIM_PIN2_RETRY[0];
        }
        } catch (ArrayIndexOutOfBoundsException e) {
            log("getPin2RetryNumber: ArrayIndexOutOfBoundsException err ="
                            + e.getMessage());
            pin2RetryStr = PROPERTY_SIM_PIN2_RETRY[0];
        }
        return SystemProperties.getInt(pin2RetryStr, GET_PIN_PUK_RETRY_EMPTY);
    }

    /**
     * Get the pin2 retry tips messages.
     * @param context
     * @param subId
     * @return
     */
    public static String getPinPuk2RetryLeftNumTips(Context context, int subId, boolean isPin) {
        if (!PhoneUtils.isValidSubId(subId)) {
            log("getPinPuk2RetryLeftNumTips : inValid SubId =  " + subId);
            return " ";
        }
        int retryCount = GET_PIN_PUK_RETRY_EMPTY;
        if (isPin) {
            retryCount = getPin2RetryNumber(subId);
        } else {
            retryCount = getPuk2RetryNumber(subId);
        }
        log("getPinPuk2RetryLeftNumTips : retry count = " + retryCount + " isPin : " + isPin);
        switch (retryCount) {
            case GET_PIN_PUK_RETRY_EMPTY:
                return " ";
            default:
                return context.getString(R.string.retries_left, retryCount);
        }
    }

    /**
     * Get puk2 left retry times.
     * @param subId the sub which one user want to get
     * @return the left times
     */
    public static int getPuk2RetryNumber(int subId) {
        if (!PhoneUtils.isValidSubId(subId)) {
            log("getPuk2RetryNumber : inValid SubId = " + subId);
            return -1;
        }
        int slot = SubscriptionManager.getSlotIndex(subId);
        log("getPuk2RetryNumber --> Sub:Slot = " + subId + ":" + slot);
        String puk2RetryStr;
        if (isGeminiProject()) {
            if (slot < PROPERTY_SIM_PIN2_RETRY.length) {
                puk2RetryStr = PROPERTY_SIM_PUK2_RETRY[slot];
            } else {
                Log.w(TAG, "PUK2 --> Slot num is invalid : Error happened !!");
                puk2RetryStr = PROPERTY_SIM_PUK2_RETRY[0];
            }
        } else {
            puk2RetryStr = PROPERTY_SIM_PUK2_RETRY[0];
        }
        return SystemProperties.getInt(puk2RetryStr, GET_PIN_PUK_RETRY_EMPTY);
    }

    public static boolean isPhoneBookReady(Context context, int subId) {
        final IMtkTelephonyEx telephonyEx = IMtkTelephonyEx.Stub.asInterface(
                ServiceManager.getService("phoneEx")); //TODO: Use Context.TELEPHONY_SERVICEEX
        boolean isPhoneBookReady = false;
        try {
            isPhoneBookReady = telephonyEx.isPhbReady(subId);
            Log.d(TAG, "[isPhoneBookReady]isPbReady:" + isPhoneBookReady + " ||subId:" + subId);
        } catch (RemoteException e) {
            Log.e(TAG, "[isPhoneBookReady]catch exception:");
            e.printStackTrace();
        }
        if (!isPhoneBookReady) {
            Toast.makeText(context,
                    context.getString(R.string.fdn_phone_book_busy), Toast.LENGTH_SHORT).show();
        }
        return isPhoneBookReady;
    }

    /**
     * Return whether the project is Gemini or not.
     * @return If Gemini, return true, else return false
     */
    public static boolean isGeminiProject() {
        boolean isGemini = TelephonyManager.getDefault().isMultiSimEnabled();
        log("isGeminiProject : " + isGemini);
        return isGemini;
    }

    /**
     * Add for [MTK_Enhanced4GLTE].
     * Get the phone is inCall or not.
     */
    public static boolean isInCall(Context context) {
        TelecomManager manager = (TelecomManager) context.getSystemService(
                Context.TELECOM_SERVICE);
        boolean inCall = false;
        if (manager != null) {
            inCall = manager.isInCall();
        }
        log("[isInCall] = " + inCall);
        return inCall;
    }
    /**
     * Add for [MTK_Enhanced4GLTE].
     * Get the phone is inCall or not.
     */
    public static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    ///M: Add for [VoLTE_SS] @{
    /**
     * Get whether the IMS is IN_SERVICE.
     * @param subId the sub which one user selected.
     * @return true if the ImsPhone is IN_SERVICE, else false.
     */
    public static boolean isImsServiceAvailable(Context context, int subId) {
        boolean available = false;
        boolean isSupportMims = MtkImsManager.isSupportMims();
        log("[isImsServiceAvailable]is support Mims:" + isSupportMims);

        if (!isSupportMims) {
            IMtkTelephonyEx iTelEx = IMtkTelephonyEx.Stub.asInterface(
                    ServiceManager.getService("phoneEx"));
            try {
                if (iTelEx != null) {
                    int phoneId = iTelEx.getMainCapabilityPhoneId();
                    subId = MtkSubscriptionManager.getSubIdUsingPhoneId(phoneId);
                    log("[isImsServiceAvailable] Main Phone ID:" + phoneId);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "[isImsServiceAvailable]catch exception:");
            }
        }
        if (PhoneUtils.isValidSubId(subId)) {
             available = MtkTelephonyManagerEx.getDefault().isImsRegistered(subId);
        }
        log("isImsServiceAvailable[ " + subId + "], available = " + available);
        return available;
    }

    /**
     * Get the SIM card's mobile data connection status, which is inserted in the given sub
     * @param subId the given subId
     * @param context
     * @return true, if enabled, else false.
     */
    public static boolean isMobileDataEnabled(int subId) {
        if (PhoneUtils.isValidSubId(subId)) {
            boolean isDataEnable = PhoneUtils.getPhoneUsingSubId(subId).getDataEnabled();
            log("[isMobileDataEnabled] isDataEnable = " + isDataEnable);
            return isDataEnable;
        }
        log("[isMobileDataEnabled] SubId = " + subId);
        return false;
    }

    /**
     * When SS from VoLTE we should make the Mobile Data Connection open, if don't open,
     * the query will fail, so we should give users a tip, tell them how to get SS successfully.
     * This function is get the point, whether we should show a tip to user. Conditions:
     * 1. VoLTE condition / CMCC support VoLTE card, no mater IMS enable/not
     * 2. Mobile Data Connection is not enable
     * @param subId the given subId
     * @return true if should show tip, else false.
     */
    public static boolean shouldShowOpenMobileDataDialog(Context context, int subId) {
        boolean result = false;
        if (!PhoneUtils.isValidSubId(subId)) {
            log("[shouldShowOpenMobileDataDialog] invalid subId!!!  " + subId);
            return result;
        }

        PersistableBundle carrierConfig =
                   PhoneGlobals.getInstance().getCarrierConfigForSubId(subId);
        /// M: For plug-in Migration @{
        if (!ExtensionManager.getCallFeaturesSettingExt().
                needShowOpenMobileDataDialog(context, subId) || !carrierConfig.
                getBoolean(MtkCarrierConfigManager.MTK_KEY_SHOW_OPEN_MOBILE_DATA_DIALOG_BOOL)) {
            return result;
        }
        /// @}

        //move check to SS FW
        //String mccMnc = TelephonyManager.getDefault().getSimOperator(subId);
        //if (MtkOperatorUtils.isNotSupportXcap(mccMnc)) {
        //     return result;
        //}

        Phone phone = PhoneUtils.getPhoneUsingSubId(subId);
        int phoneId = phone.getPhoneId();
        MtkGsmCdmaPhone gsmCdmaphone =
                (MtkGsmCdmaPhone) PhoneFactory.getPhone(phoneId);
        // 1. Ims is registered, VoLTE condition
        // 2. Support UT and PS prefer (CNOP VoLTE)
        // 3. CT VoLTE enable and is CT 4G sim, the condition 2 can support
        // 4. Smart Fren sim, the condition 1 can support
        if (isImsServiceAvailable(context, subId)
                || (isUtSupport(subId) &&
                (gsmCdmaphone.getCsFallbackStatus() == MtkPhoneConstants.UT_CSFB_PS_PREFERRED))) {
            log("[shouldShowOpenMobileDataDialog] ss query need mobile data connection!");
            ///M: When wfc registered, no need check mobile data because SS can go over wifi. @{
            boolean isWfcEnabled = ((TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE)).isWifiCallingAvailable();
            if (isWfcEnabled && !isCmccOrCuCard(subId)) {
                return result;
            }
            /// @}

            // Data should open or not for singe volte.
            if (!TelephonyUtils.isMobileDataEnabled(subId)) {
                result = true;
            /// M: Add for dual volte feature, data traffic dialog @{
            } else if ((subId != SubscriptionManager.getDefaultDataSubscriptionId())
                && isSupportDualVolte(subId)) {
                result = true;
            /// @}
            } else if (!MtkTelephonyManagerEx.getDefault().isInHomeNetwork(subId)
                //is Network Roaming and special operator card
                && isCmccOrCuCard(subId)
                && !phone.getDataRoamingEnabled()
                // OP09A no roaming switch, so no need to show tips.
                && !isOP09ASupport()) {
                //M: Add for data roaming tips
                //When CMCC Network is Roaming and data roaming not enabled,
                //we should also give user tips to turn it on.
                log("[shouldShowOpenMobileDataDialog] network is roaming!");
                result = true;
            }
        }
        log("[shouldShowOpenMobileDataDialog] subId: " + subId + ",result: " + result);
        return result;
    }

    /**
     *M: Add for data connection and roaming and data traffic tips
     * Get tip message, let user open the mobile data connection or data roaming.
     * @param context current context
     * @param subId the given subId
     * @return tip message shown to user
     */
    public static String getTipsDialogMessage(final Context context, int subId) {
        String message = "";
        Phone phone = PhoneUtils.getPhoneUsingSubId(subId);

        boolean isMobileDataAvailable = TelephonyUtils.isMobileDataEnabled(subId);
        boolean isRoamingAvailable = phone.getDataRoamingEnabled();
        boolean isNetworkRoaming = !MtkTelephonyManagerEx.getDefault().isInHomeNetwork(subId);
        /// M: Add for dual volte feature @{
        boolean isUseDataTraffic = ((subId != SubscriptionManager.getDefaultDataSubscriptionId())
                                        || !isMobileDataAvailable);
        String displayName = PhoneUtils.getSubDisplayName(subId);
        mDialogID = -1;

        if (isUseDataTraffic && MtkImsManager.isSupportMims()) {
            if (isNetworkRoaming && !isRoamingAvailable) {
                message = context.getString(
                R.string.volte_ss_not_available_tips_data_roaming, displayName);
                mDialogID = DATA_ROAMING_DIALOG;
            } else {
                message = context.getString(
                R.string.volte_ss_not_available_tips_data_traffic, displayName);
                mDialogID = DATA_TRAFFIC_DIALOG;
            }
        /// @}
        } else if (!isMobileDataAvailable && !MtkImsManager.isSupportMims()) {
            if (isNetworkRoaming && !isRoamingAvailable) {
                message = context.getString(
                R.string.volte_ss_not_available_tips_data_roaming, displayName);
                mDialogID = DATA_ROAMING_DIALOG;
            } else {
                message = context.getString(
                R.string.volte_ss_not_available_tips_data, displayName);
                mDialogID = DATA_USAGE_DIALOG;
            }
        } else {
            if (isNetworkRoaming && !isRoamingAvailable) {
                message = context.getString(
                R.string.volte_ss_not_available_tips_roaming, displayName);
                mDialogID = DATA_ROAMING_DIALOG;
            }
        }
        log("getTipsDialogMessage, isUseDataTraffic= " + isUseDataTraffic +
                ", mDialogID= " + mDialogID);
        return message;
    }

    /**
     * Show a tip dialog, let user open the mobile data connection.
     * @param context
     */
    public static void showOpenMobileDataDialog(final Context context, int subId) {
        // Modified for data connection and roaming tips
        String message = getTipsDialogMessage(context, subId);

        /// M: Add for dual volte feature @{
        if (mDialogID == DATA_TRAFFIC_DIALOG) {
            int phoneId = SubscriptionManager.getPhoneId(subId);
            Preference mPreference = mPreferences[phoneId];
            log("showOpenMobileDataDialog, mPreferences[" + phoneId + "]" +
                    mPreferences[phoneId]);
            if (mPreference != null) {
                Intent intent = null;
                if (mPreference.getKey().equals(CALL_FORWARDING_KEY)
                   || (mPreference.getKey().equals(KEY_CALL_FORWARD))) {
                    intent = getIntent(context, subId, GsmUmtsCallForwardOptions.class);
                } else if (mPreference.getKey().equals(BUTTON_CB_EXPAND)) {
                    intent = getIntent(context, subId, CallBarring.class);
                } else if (mPreference.getKey().equals(ADDITIONAL_GSM_SETTINGS_KEY)) {
                    intent = getIntent(context, subId, GsmUmtsAdditionalCallOptions.class);
                } else {
                    intent = getIntent(context, subId, CdmaCallWaitingUtOptions.class);
                }
                SubscriptionInfoHelper.addExtrasToIntent(intent, MtkSubscriptionManager
                        .getSubInfo(null, subId));
                MobileDataDialogFragment.show(intent, message,
                        ((Activity) context).getFragmentManager());
            }
            return;
        }
        /// @}

        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setMessage(message);
        switch (mDialogID) {
            case DATA_ROAMING_DIALOG:
            case DATA_USAGE_DIALOG:
                b.setPositiveButton(R.string.alert_dialog_ok,
                        new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    log("showOpenMobileDataTips, OK button clicked!");
                }
                });
                b.setCancelable(false);
                break;
            default:
                log("Unknown abnormal case!");
                break;
        }
        AlertDialog dialog = b.create();
        // make the dialog more obvious by bluring the background.
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        dialog.show();
    }
    /// @}

    public static boolean is2GOnlyProject() {
        boolean result = true;
        Phone[] phones = PhoneFactory.getPhones();
        for (Phone phone : phones) {
            if (phone.getRadioAccessFamily() != RadioAccessFamily.RAF_GSM) {
                result = false;
            }
        }

        log("[is2GOnlyProject] result = " + result);

        return result;
    }

    /**
     * Return whether the phone is hot swap or not.
     * @return If hot swap, return true, else return false
     */
    public static boolean isHotSwapHanppened(List<SubscriptionInfo> originaList,
            List<SubscriptionInfo> currentList) {
        boolean result = false;
        if (originaList.size() != currentList.size()) {
            return true;
        }
        for (int i = 0; i < currentList.size(); i++) {
            SubscriptionInfo currentSubInfo = currentList.get(i);
            SubscriptionInfo originalSubInfo = originaList.get(i);
            if (!(currentSubInfo.getIccId()).equals(originalSubInfo.getIccId())) {
                result = true;
                break;
            } else {
                result = false;
            }
        }

        log("isHotSwapHanppened : " + result);
        return result;
    }

    /**
     * Return whether the project is support WCDMA Preferred.
     * @return If support, return true, else return false
     */
    public static boolean isWCDMAPreferredSupport() {
        String isWCDMAPreferred = SystemProperties.get("ro.mtk_rat_wcdma_preferred");
        if (TextUtils.isEmpty(isWCDMAPreferred)) {
            log("isWCDMAPreferredSupport : false; isWCDMAPreferred is empty. ");
            return false;
        }
        log("isWCDMAPreferredSupport : " + isWCDMAPreferred);
        return "1".equals(isWCDMAPreferred);
    }

    /**
     * CDMA project feature option.
     * @return true if this project support CDMA.
     */
    public static boolean isCdmaSupport() {
        return "1".equals(SystemProperties.get("ro.boot.opt_c2k_support"));
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    /**
     * M: Return if the sim card is cmcc or not. @{
     * @param subId sub id identify the sim card
     * @return true if the sim card is cmcc
     */
    public static boolean isCmccOrCuCard(int subId) {
        boolean result = false;
        String numeric = TelephonyManager.getDefault().getSimOperator(subId);
        for (String cmccOrCu : CMCC_CU_NUMERIC) {
            if (cmccOrCu.equals(numeric)) {
                result = true;
            }
        }
        log("isCmccOrCuCard:" + result);
        return result;
    }

    /// M: Add for dual volte feature @{
    /**
     * M: Return if the sim card supports dual volte. @{
     * @param subId sub id identify the sim card
     * @return true if the sim card supports dual volte
     */
    public static boolean isSupportDualVolte(int subId) {
        boolean result = false;
        result = MtkImsManager.isSupportMims() &&
                (isCmccOrCuCard(subId) || (TelephonyUtilsEx.isCtVolteEnabled() &&
                TelephonyUtilsEx.isCt4gSim(subId)));
        return result ;
    }
    /// @}

    /**
     * M: Get status whether the sim card is invalid or not.
     * @param subId sub id identify the sim card
     * @return true if the sim card is invalid
     */
    public static boolean isInvalidSimCard(int subId) {
        boolean result = false;
        String numeric = TelephonyManager.getDefault().getSimOperator(subId);
        if (numeric == null || numeric == "") {
            result = true;
        }

        log("isInvalidSimCard:" + result);
        return result;
    }
    /** @} */

    private static boolean isUtSupport(int subId) {
        boolean result = false;
        if (SystemProperties.get("persist.mtk_ims_support").equals("1") &&
                SystemProperties.get("persist.mtk_volte_support").equals("1")) {
            if (isCmccOrCuCard(subId)
                    && isUSIMCard(PhoneGlobals.getInstance().getApplicationContext(), subId)) {
                result = true;
            }
        }
        /// M: [CT VOLTE]
        if (CallSettingUtils.isCtVolte4gSim(subId)) {
            result = true;
        }
        return result;
    }

    public static boolean isMtkTddDataOnlySupport() {
        boolean isSupport = ONE.equals(SystemProperties.get(
                "ro.mtk_tdd_data_only_support")) ? true : false;
        Log.d(TAG, "isMtkTddDataOnlySupport(): " + isSupport);
        return isSupport;
    }

    public static boolean isCTLteTddTestSupport() {
        String[] type = MtkTelephonyManagerEx.getDefault().getSupportCardType(
                PhoneConstants.SIM_ID_1);
        if (type == null) {
            return false;
        }
        boolean isUsimOnly = false;
        if ((type.length == 1) && ("USIM".equals(type[0]))) {
            isUsimOnly = true;
        }
        return FeatureOption.isMtkSvlteSupport()
                && (ONE.equals(SystemProperties.get("persist.sys.forcttddtest", "0")))
                && isUsimOnly;
    }

    /// M: Add for dual volte feature @{
    /**
     * set the Parameters from CallfeaturesSetting by SubId.
     * @param subId SubId
     * @param preference Preference
     */
    public static void setParameters(int subId, Preference preference) {
        if (PhoneUtils.isValidSubId(subId)) {
            int phoneId = SubscriptionManager.getPhoneId(subId);
            mPreferences[phoneId] = preference;
            log("setParameters, mPreferences[" + phoneId + "]" + mPreferences[phoneId]);
        }
    }
    /// @}

    private static boolean isOP09ASupport() {
        log("isOP09ASupport.");
        return OPERATOR_OP09.equals(SystemProperties.get("persist.operator.optr", ""))
                && SEGDEFAULT.equals(SystemProperties.get("persist.operator.seg", ""));
    }

    /**
     * Get the phone id with main capability.
     */
    public static int getMainCapabilityPhoneId() {
        int phoneId = SubscriptionManager.INVALID_PHONE_INDEX;
        IMtkTelephonyEx iTelEx = IMtkTelephonyEx.Stub.asInterface(
                ServiceManager.getService("phoneEx"));
        if (iTelEx != null) {
            try {
                phoneId = iTelEx.getMainCapabilityPhoneId();
            } catch (RemoteException e) {
                log("getMainCapabilityPhoneId: remote exception");
            }
        } else {
            log("IMtkTelephonyEx service not ready!");
            phoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
        }
        log("getMainCapabilityPhoneId: phoneId = " + phoneId);
        return phoneId;
    }

     /**
     * Get the Intent.
     * @context Context.
     * @param subId SubId.
     * @param newActivityClass The class of the activity for the intent to start.
     * @return Intent containing extras for the subscription id.
     */
    public static Intent getIntent(Context context, int subId, Class newActivityClass) {
        Intent intent = new Intent(context, newActivityClass);
        if (PhoneUtils.isValidSubId(subId)) {
            intent.putExtra(SUB_ID_EXTRA, subId);
        }
        return intent;
    }
}

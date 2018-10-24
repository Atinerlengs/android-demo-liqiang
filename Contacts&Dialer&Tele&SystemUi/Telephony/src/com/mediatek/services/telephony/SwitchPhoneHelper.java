/* Copyright Statement:
*
* This software/firmware and related documentation ("MediaTek Software") are
* protected under relevant copyright laws. The information contained herein
* is confidential and proprietary to MediaTek Inc. and/or its licensors.
* Without the prior written permission of MediaTek inc. and/or its licensors,
* any reproduction, modification, use or disclosure of MediaTek Software,
* and information contained herein, in whole or in part, shall be strictly prohibited.
*/
/* MediaTek Inc. (C) 2017. All rights reserved.
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
* have been modified by MediaTek Inc. All revisions are subject to any receiver\'s
* applicable license agreements with MediaTek Inc.
*/

package com.mediatek.services.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.ims.ImsManager;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.services.telephony.Log;

import com.mediatek.ims.internal.MtkImsManager;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.internal.telephony.RadioManager;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.MtkProxyController;
import com.mediatek.telephony.MtkTelephonyManagerEx;

import static com.android.internal.telephony.PhoneConstants.PHONE_TYPE_CDMA;
import static com.android.internal.telephony.PhoneConstants.PHONE_TYPE_GSM;
import static com.android.internal.telephony.PhoneConstants.PHONE_TYPE_NONE;

/**
 * Helper class to switch phone for emergency call.
 */
public class SwitchPhoneHelper {
    // Handler message codes; see handleMessage()
    private static final int MSG_START_SWITCH_PHONE = 1;
    private static final int MSG_SWITCH_PHONE_TIMEOUT = 2;
    private static final int MSG_MODE_SWITCH_RESULT = 3;
    private static final int MSG_WAIT_FOR_INTENT_TIMEOUT = 4;
    private static final int MSG_START_TURN_OFF_VOLTE = 5;
    private static final int MSG_TURN_OFF_VOLTE_TIMEOUT = 6;

    private static final boolean MTK_C2K_SUPPORT
            = "1".equals(SystemProperties.get("ro.boot.opt_c2k_support"));
    private static final boolean MTK_CT_VOLTE_SUPPORT
            = "1".equals(SystemProperties.get("persist.mtk_ct_volte_support", "0"));
    private static final int PROJECT_SIM_NUM = TelephonyManager.getDefault().getPhoneCount();
    private static final int RETRY_SWITCH_PHONE_MILLIS = 2000;
    private static final int SWITCH_PHONE_TIMEOUT_MILLIS = 10000;
    private static final int WAIT_FOR_FINISH_TIMEOUT_MILLIS = 30000;
    private static final int TURN_OFF_VOLTE_TIMEOUT_MILLIS = 5000;
    private static final int MODE_GSM = 1;
    private static final int MODE_C2K = 4;
    private static final int MAX_FAIL_RETRY_COUNT = 10;

    private boolean mRegisterSwitchPhoneReceiver = false;
    private boolean mRegisterSimStateReceiver = false;
    private boolean mRegisterSimSwitchReceiver = false;
    private boolean mSkipFirstIntent = false;
    private int mTargetPhoneType = PHONE_TYPE_NONE;
    private int mFailRetryCount = 0;
    private int[] mSimStateCollected = new int[PROJECT_SIM_NUM];
    private int[] mRadioTechCollected = new int[PROJECT_SIM_NUM];
    private String[] mSimState = new String[PROJECT_SIM_NUM];
    private String mNumber;
    private TelephonyManager mTm;
    private IMtkTelephonyEx mTelEx;
    private SwitchPhoneReceiver mSwitchPhoneReceiver;
    private SimStateReceiver mSimStateReceiver;
    private SimSwitchReceiver mSimSwitchReceiver;
    private final Context mContext;
    private Callback mCallback;  // The callback to notify upon completion.
    private Phone mPhone;  // The phone which will be used to switch.
    private EmergencyNumberUtils mEccNumberUtils;
    private MtkTelephonyConnectionServiceUtil mMtkTelephonyConnectionServiceUtil =
            MtkTelephonyConnectionServiceUtil.getInstance();

    /**
     * Receives the result of the SwitchPhoneHelper's attempt to switch phone.
     */
    public interface Callback {
        void onComplete(boolean success);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_SWITCH_PHONE:
                    startSwitchPhoneInternal();
                    break;

                case MSG_SWITCH_PHONE_TIMEOUT:
                    logd("Receives MSG_SWITCH_PHONE_TIMEOUT");
                    finish();
                    break;

                case MSG_MODE_SWITCH_RESULT:
                    logd("Receives MSG_MODE_SWITCH_RESULT");
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        mFailRetryCount++;
                        logd("Fail to switch now, failCount=" + mFailRetryCount
                                + ", maxCount=" + MAX_FAIL_RETRY_COUNT);
                        if (mFailRetryCount < MAX_FAIL_RETRY_COUNT) {
                            mHandler.sendEmptyMessageDelayed(MSG_START_SWITCH_PHONE,
                                    RETRY_SWITCH_PHONE_MILLIS);
                        } else {
                            finish();
                        }
                    } else {
                        logd("Start switching phone");
                        startSwitchPhoneTimer();
                        if (!mRegisterSwitchPhoneReceiver) {
                            IntentFilter intentFilter = new IntentFilter(
                                    TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
                            intentFilter.addAction(
                                    TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
                            mSwitchPhoneReceiver = new SwitchPhoneReceiver();
                            mContext.registerReceiver(mSwitchPhoneReceiver, intentFilter);
                            mRegisterSwitchPhoneReceiver = true;
                        }
                    }
                    break;

                case MSG_WAIT_FOR_INTENT_TIMEOUT:
                    logd("Receives MSG_WAIT_FOR_INTENT_TIMEOUT");
                    if (needToSwitchPhone()) {
                        startSwitchPhone(mCallback);
                    } else {
                        finish();
                    }
                    break;

                case MSG_START_TURN_OFF_VOLTE:
                    logd("Start turn off VoLTE!");
                    exitCtLteOnlyMode();
                    startTurnOffVolteTimer();
                    break;

                case MSG_TURN_OFF_VOLTE_TIMEOUT:
                    logd("MSG_TURN_OFF_VOLTE_TIMEOUT");
                    finish();
                    break;

                default:
                    logd("Receives unexpected message=" + msg.what);
                    break;
            }
        }
    };

    public SwitchPhoneHelper(Context context, String number) {
        logd("SwitchPhoneHelper constructor");
        mContext = context;
        mNumber = number;
        mEccNumberUtils = new EmergencyNumberUtils(number);
        mTm = TelephonyManager.getDefault();
        mTelEx = IMtkTelephonyEx.Stub.asInterface(
                ServiceManager.getService("phoneEx"));
    }

    public boolean needToPrepareForDial() {
        if (!MTK_C2K_SUPPORT) {
            return false;
        }
        /// M: CC: LWG test mode, don't switch to avoid unexpected radio behavior. @{
        String testMode = SystemProperties.get("persist.radio.ct.ir.engmode");
        if ("2".equals(testMode)) {
            return false;
        }
        /// @}
        if (needToWaitSimState()) {
            return true;
        }
        if ((ProxyController.getInstance() instanceof MtkProxyController)
                && (((MtkProxyController) ProxyController.getInstance()).
                        isCapabilitySwitching())) {
            logd("needToPrepareForDial, capability switching");
            return true;
        }
        if (needToSwitchPhone()) {
            return true;
        }
        return false;
    }

    public void prepareForDial(Callback callback) {
        if (needToWaitSimState()) {
            // Only start it in cases that we need to wait for SIM state.
            // In other cases, don't wait for SIM state, and flight mode will be exited in
            // startSwitchPhone()
            startExitAirplaneModeAndWaitSimState(callback);
        } else if ((ProxyController.getInstance() instanceof MtkProxyController)
                && (((MtkProxyController) ProxyController.getInstance()).
                        isCapabilitySwitching())) {
            waitForCapabilitySwitchFinish(callback);
        } else if (needToSwitchPhone()) {
            startSwitchPhone(callback);
        }
    }

    private boolean needToWaitSimState() {
        int airplaneMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0);
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            // Don't wait for SIM state if modem already on (for example, WFC is actived under
            // flight mode, or mtk_flight_mode_power_off_md is off). There will be no
            // SIM state change broadcasts.
            if (!RadioManager.isModemPowerOff(i)) {
                mSimStateCollected[i] = 1;
                mSimState[i] = getSimState(i);
            } else {
                mSimStateCollected[i] = 0;
                mSimState[i] = null;
            }
        }
        boolean allSimReady = isAllSimReady();
        logd("needToWaitSimState, airplaneMode = " + airplaneMode
                + ", allSimReady = " + allSimReady);
        return airplaneMode > 0 && !allSimReady;
    }

    private void startSwitchPhone(Callback callback) {
        mPhone = null;
        unregisterReceiver();
        mCallback = callback;
        int mainPhoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
        logd("startSwitchPhone, mainPhoneId:" + mainPhoneId);
        if (mMtkTelephonyConnectionServiceUtil.hasPerformEccRetry()) {
            int previousPhoneType = mMtkTelephonyConnectionServiceUtil.getEccPhoneType();
            logd("startSwitchPhone, previousPhoneType:" + previousPhoneType);
            if (previousPhoneType == PHONE_TYPE_CDMA) {
                mTargetPhoneType = PHONE_TYPE_GSM;
                if (!mTm.hasIccCard(mainPhoneId)) {
                    mPhone = PhoneFactory.getPhone(mainPhoneId);
                } else {
                    logd("main phone has card, can't switch!");
                }
            } else {
                mTargetPhoneType = PHONE_TYPE_CDMA;
                int cdmaSlot = SystemProperties.getInt("persist.radio.cdma_slot", -1);
                logd("startSwitchPhone, cdmaSlot:" + cdmaSlot);
                if (cdmaSlot != -1) {
                    if (!mTm.hasIccCard(cdmaSlot - 1)) {
                        // Select no SIM card and CDMA capability slot
                        mPhone = PhoneFactory.getPhone(cdmaSlot - 1);

                        if (MTK_CT_VOLTE_SUPPORT) {
                            // Reset the target phone type for GSM always/only/preferred number
                            if (mEccNumberUtils.isGsmAlwaysNumber()
                                || mEccNumberUtils.isGsmOnlyNumber()
                                || mEccNumberUtils.isGsmPreferredNumber()) {
                                mTargetPhoneType = PHONE_TYPE_GSM;
                            }
                        }
                    } else {
                        if (isInCtLteOnlyMode()) {
                            // Turn off VoLTE to exit LTE only mode
                            mPhone = PhoneFactory.getPhone(cdmaSlot - 1);
                            mMtkTelephonyConnectionServiceUtil.setEccRetryPhoneId(mPhone.getPhoneId());
                            logd("startSwitchPhone, turn off VoLTE for phone" + mPhone.getPhoneId()
                                    + ", phoneType:" + mPhone.getPhoneType()
                                    + ", mTargetPhoneType:" + mTargetPhoneType);
                            mHandler.obtainMessage(MSG_START_TURN_OFF_VOLTE).sendToTarget();
                            return;
                        }

                        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
                            if (!mTm.hasIccCard(i)) {
                                // Select no SIM card slot
                                mPhone = PhoneFactory.getPhone(i);
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < PROJECT_SIM_NUM; i++) {
                        if (!mTm.hasIccCard(i)) {
                            // Select no SIM card slot
                            mPhone = PhoneFactory.getPhone(i);
                        }
                    }
                }
            }
            if (mPhone != null) {
                mMtkTelephonyConnectionServiceUtil.setEccRetryPhoneId(mPhone.getPhoneId());
            }
        } else {
            if (Settings.Global.getInt(mContext.getContentResolver(),
                        Settings.Global.AIRPLANE_MODE_ON, 0) > 0) {
                Settings.Global.putInt(mContext.getContentResolver(),
                        Settings.Global.AIRPLANE_MODE_ON, 0);
                Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                intent.putExtra("state", false);
                mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            }
            if (mEccNumberUtils.isCdmaAlwaysNumber()
                    || mEccNumberUtils.isCdmaPreferredNumber()) {
                int cdmaSlot = SystemProperties.getInt("persist.radio.cdma_slot", -1);
                logd("startSwitchPhone, cdmaSlot:" + cdmaSlot);
                if (cdmaSlot != -1 && !mTm.hasIccCard(cdmaSlot - 1)) {
                    // Select no SIM card and CDMA capability slot
                    mPhone = PhoneFactory.getPhone(cdmaSlot - 1);
                } else {
                    for (int i = 0; i < PROJECT_SIM_NUM; i++) {
                        if (!mTm.hasIccCard(i)) {
                            // Select no SIM card slot
                            mPhone = PhoneFactory.getPhone(i);
                        }
                    }
                }
                mTargetPhoneType = PHONE_TYPE_CDMA;
            } else if (mEccNumberUtils.isGsmAlwaysNumber() || mEccNumberUtils.isGsmOnlyNumber()
                    || mEccNumberUtils.isGsmPreferredNumber()) {
                if (!mTm.hasIccCard(mainPhoneId)) {
                    mPhone = PhoneFactory.getPhone(mainPhoneId);
                } else {
                    logd("main phone has card, can't switch!");
                }
                mTargetPhoneType = PHONE_TYPE_GSM;
            }
        }
        if (mPhone == null) {
            logd("startSwitchPhone, no suitable phone selected to switch!");
            finish();
            return;
        }
        logd("startSwitchPhone with phone" + mPhone.getPhoneId() + ", phoneType:"
                + mPhone.getPhoneType() + ", mTargetPhoneType:" + mTargetPhoneType);
        mHandler.obtainMessage(MSG_START_SWITCH_PHONE).sendToTarget();
    }

    private void startSwitchPhoneInternal() {
        if (!mTm.hasIccCard(mPhone.getPhoneId())) {
            if (mTargetPhoneType == PHONE_TYPE_GSM) {
                mPhone.exitEmergencyCallbackMode();
            }
            if (mPhone instanceof MtkGsmCdmaPhone) {
                ((MtkGsmCdmaPhone) mPhone).triggerModeSwitchByEcc(
                        mTargetPhoneType == PHONE_TYPE_CDMA ? MODE_C2K : MODE_GSM,
                        mHandler.obtainMessage(MSG_MODE_SWITCH_RESULT));
            }
        } else {
            logd("startSwitchPhoneInternal, no need to switch phone!");
            finish();
        }
    }

    private void finish() {
        onComplete(true);
        cleanup();
    }

    private void startSwitchPhoneTimer() {
        cancelSwitchPhoneTimer();
        mHandler.sendEmptyMessageDelayed(MSG_SWITCH_PHONE_TIMEOUT, WAIT_FOR_FINISH_TIMEOUT_MILLIS);
    }

    private void cancelSwitchPhoneTimer() {
        mHandler.removeMessages(MSG_SWITCH_PHONE_TIMEOUT);
        mHandler.removeMessages(MSG_START_SWITCH_PHONE);
    }

    public void onDestroy() {
        logd("onDestroy");
        cleanup();
        if ("OP09".equals(SystemProperties.get("persist.operator.optr"))
                && "SEGDEFAULT".equals(SystemProperties.get("persist.operator.seg"))) {
            Phone phone = PhoneFactory.getPhone(0);
            logd("Phone0 type:" + phone.getPhoneType() + ", hascard:" + mTm.hasIccCard(0));
            if (phone.getPhoneType() != PHONE_TYPE_CDMA && !mTm.hasIccCard(0)
                && (phone instanceof MtkGsmCdmaPhone)) {
                ((MtkGsmCdmaPhone) phone).triggerModeSwitchByEcc(MODE_C2K, null);
            }
        }
    }

    private void startExitAirplaneModeAndWaitSimState(Callback callback) {
        logd("startExitAirplaneModeAndWaitSimState");
        cleanup();
        mCallback = callback;
        IntentFilter intentFilter = new IntentFilter(
                TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        mSimStateReceiver = new SimStateReceiver();
        mContext.registerReceiver(mSimStateReceiver, intentFilter);
        mRegisterSimStateReceiver = true;
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0);
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", false);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        mHandler.sendEmptyMessageDelayed(MSG_WAIT_FOR_INTENT_TIMEOUT,
                WAIT_FOR_FINISH_TIMEOUT_MILLIS);

    }

    private String getSimState(int slotId) {
        String strAllSimState = SystemProperties.get(TelephonyProperties.PROPERTY_SIM_STATE);
        if (strAllSimState != null && strAllSimState.length() > 0) {
            String values[] = strAllSimState.split(",");
            if (slotId >= 0 && slotId < values.length) {
                return values[slotId];
            }
        }
        return null;
    }

    private boolean isAllSimReady() {
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            if (mSimStateCollected[i] != 1 || mSimState[i] == null
                    || mSimState[i].equals(IccCardConstants.INTENT_VALUE_ICC_NOT_READY)) {
                return false;
            }
        }
        return true;
    }

    private boolean isRoaming() {
        int cdmaSlot = SystemProperties.getInt("persist.radio.cdma_slot", -1);
        int appFamily = MtkTelephonyManagerEx.APP_FAM_NONE;
        if (mTelEx != null) {
            try {
                appFamily = mTelEx.getIccAppFamily(cdmaSlot - 1);
            } catch (RemoteException e) {
                logd("Cannot get appFamily of cdma slot" + (cdmaSlot - 1));
            }
        }
        Phone phone = PhoneFactory.getPhone(cdmaSlot - 1);
        logd("isRoaming, cdmaSlot:" + cdmaSlot + ", appFamily:" + appFamily
                + ", phonetype:" + (phone != null ? phone.getPhoneType() : PHONE_TYPE_NONE));
        if (cdmaSlot != -1 && appFamily != MtkTelephonyManagerEx.APP_FAM_NONE
                && appFamily != MtkTelephonyManagerEx.APP_FAM_3GPP
                && phone != null && phone.getPhoneType() == PHONE_TYPE_GSM) {
            logd("Card" + (cdmaSlot - 1) + " is roaming");
            return true;
        }
        return false;
    }

    private boolean hasSuitableCdmaPhone() {
        for (Phone p : PhoneFactory.getPhones()) {
            if (p.getPhoneType() == PHONE_TYPE_CDMA) {
                logd("hasSuitableCdmaPhone, phone" + p.getPhoneId());
                return true;
            }
        }
        return false;
    }

    private boolean hasSuitableGsmPhone() {
        // TODO: if support L+W or L+L, return true
        boolean noSimInserted = true;
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            if (mTm.hasIccCard(i)) {
                noSimInserted = false;
                break;
            }
        }
        int mainPhoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
        Phone mainPhone = PhoneFactory.getPhone(mainPhoneId);
        logd("hasSuitableGsmPhone, noSimInserted:" + noSimInserted + ", mainPhone type:"
                + (mainPhone != null ? mainPhone.getPhoneType() : PHONE_TYPE_NONE));
        if (noSimInserted && mainPhone != null && mainPhone.getPhoneType() != PHONE_TYPE_GSM) {
            return false;
        } else {
            return true;
        }
    }

    private boolean hasInServiceGsmPhone() {
        for (Phone p : PhoneFactory.getPhones()) {
            if (p.getPhoneType() == PHONE_TYPE_GSM
                    && ServiceState.STATE_IN_SERVICE == p.getServiceState().getState()) {
                logd("Phone" + p.getPhoneId() + " in service");
                return true;
            }
        }
        return false;
    }

    private boolean needToSwitchPhone() {
        if (!MTK_C2K_SUPPORT) {
            return false;
        }
        if (mMtkTelephonyConnectionServiceUtil.hasPerformEccRetry()) {
            int previousPhoneType = mMtkTelephonyConnectionServiceUtil.getEccPhoneType();
            logd("needToSwitchPhone, previousPhoneType:" + previousPhoneType);
            if (!mEccNumberUtils.isGsmAlwaysNumber() && !mEccNumberUtils.isGsmOnlyNumber()
                    && previousPhoneType == PHONE_TYPE_GSM && !hasSuitableCdmaPhone()
                    && (!isRoaming() || isInCtLteOnlyMode())) {
                logd("Need to switch to CDMAPhone");
                return true;
            }
            if (!mEccNumberUtils.isCdmaAlwaysNumber()
                    && ((previousPhoneType == PHONE_TYPE_CDMA && !hasSuitableGsmPhone())
                        || (previousPhoneType == PHONE_TYPE_GSM && isInCtLteOnlyMode()))) {
                logd("Need to switch to GSMPhone");
                return true;
            }
            logd("No need to switch phone");
            return false;
        }
        if ((mEccNumberUtils.isGsmAlwaysNumber() || mEccNumberUtils.isGsmOnlyNumber()
                || mEccNumberUtils.isGsmPreferredNumber())
                && !hasSuitableGsmPhone()) {
            logd("Need to switch to GSMPhone");
            return true;
        }
        if ((mEccNumberUtils.isCdmaAlwaysNumber()
                || (mEccNumberUtils.isCdmaPreferredNumber() && !hasInServiceGsmPhone()))
                && !hasSuitableCdmaPhone() && !isRoaming()
                // Check UE is set to test mode or not (CTA=1, FTA=2, IOT=3 ...)
                // Skip CDMA always check for TC_6.2.1 @{
                && SystemProperties.getInt("gsm.gcf.testmode", 0) != 2) {
            logd("Need to switch to CDMAPhone");
            return true;
        }
        logd("No need to switch phone");
        return false;
    }

    private void waitForCapabilitySwitchFinish(Callback callback) {
        cleanup();
        mCallback = callback;
        IntentFilter intentFilter;
        if (!isAllCdmaCard()) {
            intentFilter = new IntentFilter(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_DONE);
            intentFilter.addAction(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_FAILED);
        } else {
            for (int i = 0; i < PROJECT_SIM_NUM; i++) {
                mRadioTechCollected[i] = 0;
            }
            intentFilter = new IntentFilter(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
        }
        mSimSwitchReceiver = new SimSwitchReceiver();
        mContext.registerReceiver(mSimSwitchReceiver, intentFilter);
        mRegisterSimSwitchReceiver = true;
        mHandler.sendEmptyMessageDelayed(MSG_WAIT_FOR_INTENT_TIMEOUT,
                WAIT_FOR_FINISH_TIMEOUT_MILLIS);
    }

    private void onComplete(boolean success) {
        if (mCallback != null) {
            Callback tempCallback = mCallback;
            mCallback = null;
            tempCallback.onComplete(success);
        }
    }

    private void unregisterReceiver() {
        logd("unregisterReceiver, mRegisterSwitchPhoneReceiver:" + mRegisterSwitchPhoneReceiver
                + ", mRegisterSimStateReceiver:" + mRegisterSimStateReceiver
                + ", mRegisterSimSwitchReceiver:" + mRegisterSimSwitchReceiver);
        if (mRegisterSwitchPhoneReceiver) {
            try {
                mContext.unregisterReceiver(mSwitchPhoneReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            mRegisterSwitchPhoneReceiver = false;
        }
        if (mRegisterSimStateReceiver) {
            try {
                mContext.unregisterReceiver(mSimStateReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            mRegisterSimStateReceiver = false;
        }
        if (mRegisterSimSwitchReceiver) {
            try {
                mContext.unregisterReceiver(mSimSwitchReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            mRegisterSimSwitchReceiver = false;
        }
    }

    private void cleanup() {
        logd("cleanup");
        unregisterReceiver();
        // This will send a failure call back if callback has yet to be invoked.  If the callback
        // was already invoked, it's a no-op.
        onComplete(false);
        mHandler.removeCallbacksAndMessages(null);
        mPhone = null;
    }

    private boolean isAllCdmaCard() {
        if (mTelEx != null) {
            int appFamily;
            for (int i = 0; i < PROJECT_SIM_NUM; i++) {
                try {
                    appFamily = mTelEx.getIccAppFamily(i);
                    if (appFamily == MtkTelephonyManagerEx.APP_FAM_NONE
                            || appFamily == MtkTelephonyManagerEx.APP_FAM_3GPP) {
                        logd("appFamily of slot" + i + " is " + appFamily);
                        return false;
                    }
                } catch (RemoteException e) {
                    logd("Cannot get appFamily of slot" + i);
                }
            }
        }
        return true;
    }

    private boolean isAllPhoneReady() {
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            if (mRadioTechCollected[i] != 1) {
                return false;
            }
        }
        return true;
    }

    private void logd(String s) {
        Log.i(this, s);
    }

    private class SwitchPhoneReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            logd("Received:" + action);
            if (mPhone != null) {
                logd("Service state:" + mPhone.getServiceState().getState()
                        + ", phoneType:" + mPhone.getPhoneType()
                        + ", mTargetPhoneType:" + mTargetPhoneType
                        + ", phoneId:" + mPhone.getPhoneId()
                        + ", hasIccCard:" + mTm.hasIccCard(mPhone.getPhoneId()));
                if (TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED.equals(action)) {
                    if (mPhone.getPhoneType() == mTargetPhoneType) {
                        logd("Switch to target phone!");
                        cancelSwitchPhoneTimer();
                        finish();
                    }
                } else if (TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED.equals(action)) {
                    if (mTm.hasIccCard(mPhone.getPhoneId())) {
                        logd("No need to switch phone anymore!");
                        cancelSwitchPhoneTimer();
                        finish();
                    }
                }
            }
        }
    }

    private class SimStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int slotId = intent.getIntExtra(PhoneConstants.PHONE_KEY,
                    SubscriptionManager.INVALID_SIM_SLOT_INDEX);
            String simState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
            logd("Received ACTION_SIM_STATE_CHANGED, slotId:" + slotId
                    + ", simState:" + simState + ", mSkipFirstIntent:" + mSkipFirstIntent);
            if (!mSkipFirstIntent) {
                mSkipFirstIntent = true;
                return;
            }
            if (slotId != SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
                mSimStateCollected[slotId] = 1;
                mSimState[slotId] = simState;
            }
            if (isAllSimReady()) {
                mHandler.removeMessages(MSG_WAIT_FOR_INTENT_TIMEOUT);
                if (needToSwitchPhone()) {
                    startSwitchPhone(mCallback);
                } else {
                    finish();
                }
            }
        }
    }

    private class SimSwitchReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED.equals(action)) {
                int slotId = intent.getIntExtra(PhoneConstants.PHONE_KEY,
                        SubscriptionManager.INVALID_SIM_SLOT_INDEX);
                logd("Received ACTION_RADIO_TECHNOLOGY_CHANGED, slotId:" + slotId
                        + ", mSkipFirstIntent:" + mSkipFirstIntent);
                if (!mSkipFirstIntent) {
                    mSkipFirstIntent = true;
                    return;
                }
                if (slotId != SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
                    mRadioTechCollected[slotId] = 1;
                }
                if (isAllPhoneReady()) {
                    mHandler.removeMessages(MSG_WAIT_FOR_INTENT_TIMEOUT);
                    if (needToSwitchPhone()) {
                        startSwitchPhone(mCallback);
                    } else {
                        finish();
                    }
                }
            } else {
                logd("Received " + action);
                mHandler.removeMessages(MSG_WAIT_FOR_INTENT_TIMEOUT);
                if (needToSwitchPhone()) {
                    startSwitchPhone(mCallback);
                } else {
                    finish();
                }
            }
        }
    }

    private boolean isInCtLteOnlyMode() {
        if (!MTK_CT_VOLTE_SUPPORT) {
            return false;
        }

        boolean ctLteOnlyMode = false;
        boolean volteSupport = false;
        boolean volteSetting = false;
        int mainPhoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
        int cdmaSlot = SystemProperties.getInt("persist.radio.cdma_slot", -1);
        int appFamily = MtkTelephonyManagerEx.APP_FAM_NONE;
        try {
            appFamily = mTelEx.getIccAppFamily(cdmaSlot - 1);
        } catch (RemoteException e) {
            logd("Cannot get appFamily of cdma slot" + (cdmaSlot - 1));
        }
        Phone phone = PhoneFactory.getPhone(cdmaSlot - 1);
        if (cdmaSlot != -1
            && (appFamily == MtkTelephonyManagerEx.APP_FAM_NONE
                || appFamily >= MtkTelephonyManagerEx.APP_FAM_3GPP2)
            && phone != null && phone.getPhoneType() == PHONE_TYPE_GSM) {
            volteSupport = ImsManager.isVolteEnabledByPlatform(
                    phone.getContext(), phone.getPhoneId());
            volteSetting = MtkImsManager.isEnhanced4gLteModeSettingEnabledByUser(
                    phone.getContext(), phone.getPhoneId());
            if (volteSupport && volteSetting) {
                ctLteOnlyMode = true;
            }
        }
        logd("isInCtLteOnlyMode, mainPhoneId=" + mainPhoneId
                + ", cdmaSlot=" + cdmaSlot
                + ", appFamily=" + appFamily
                + ", phoneType=" + (phone != null ? phone.getPhoneType() : PHONE_TYPE_NONE)
                + ", volteSupport=" + volteSupport
                + ", volteSetting=" + volteSetting
                + ", ctLteOnlyMode=" + ctLteOnlyMode);
        return ctLteOnlyMode;
    }

    private void exitCtLteOnlyMode() {
        int cdmaSlot = SystemProperties.getInt("persist.radio.cdma_slot", -1);
        Phone phone = PhoneFactory.getPhone(cdmaSlot - 1);
        if (cdmaSlot != -1 && phone != null) {
            MtkImsManager.setEnhanced4gLteModeSetting(
                    phone.getContext(), false, phone.getPhoneId());
        }
        logd("exitCtLteOnlyMode, cdmaSlot=" + cdmaSlot);
    }

    private void startTurnOffVolteTimer() {
        cancelTurnOffVolteTimer();
        mHandler.sendEmptyMessageDelayed(MSG_TURN_OFF_VOLTE_TIMEOUT, TURN_OFF_VOLTE_TIMEOUT_MILLIS);
    }

    private void cancelTurnOffVolteTimer() {
        mHandler.removeMessages(MSG_TURN_OFF_VOLTE_TIMEOUT);
        mHandler.removeMessages(MSG_START_TURN_OFF_VOLTE);
    }
}

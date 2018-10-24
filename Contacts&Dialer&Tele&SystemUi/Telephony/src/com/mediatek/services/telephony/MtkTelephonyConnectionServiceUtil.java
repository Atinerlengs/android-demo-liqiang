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
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.services.telephony;

import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SubscriptionController;


import com.android.internal.telephony.gsm.GsmMmiCode;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;

import android.os.SystemProperties;

import java.util.List;
import java.util.ArrayList;

// for cell conn manager
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.telecom.Conference;
import android.telecom.ConnectionRequest;
import android.telecom.PhoneAccountHandle;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyDevController;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneUtils;
import com.android.services.telephony.DisconnectCauseUtil;
import com.android.services.telephony.ImsConferenceController;
import com.android.services.telephony.Log;
import android.telephony.CarrierConfigManager;

import com.android.ims.ImsManager;
import com.android.ims.ImsException;

import com.android.services.telephony.TelephonyConnection;
import com.android.services.telephony.TelephonyConnectionService;

import com.mediatek.ims.internal.MtkImsManager;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.MtkHardwareConfig;
import com.mediatek.internal.telephony.MtkLteDataOnlyController;
import com.mediatek.internal.telephony.MtkPhoneConstants;
import com.mediatek.internal.telephony.MtkPhoneNumberUtils;
import com.mediatek.internal.telephony.MtkTelephonyIntents;
import com.mediatek.internal.telephony.gsm.MtkGsmMmiCode;
import com.mediatek.internal.telephony.imsphone.MtkImsPhone;
import com.mediatek.internal.telephony.imsphone.MtkImsPhoneMmiCode;

import com.mediatek.phone.MtkSimErrorDialog;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.provider.MtkSettingsExt;
import com.mediatek.telephony.MtkTelephonyManagerEx;

/// M: CC: Proprietary CRSS handling
import com.mediatek.services.telephony.MtkSuppMessageManager;
import com.mediatek.services.telephony.MtkGsmCdmaConnection;

/// M: CC: Set ECC in progress
import com.mediatek.telephony.MtkTelephonyManagerEx;

import com.mediatek.settings.TelephonyUtils;

import mediatek.telephony.MtkCarrierConfigManager;
//*/ freeme.liqiang, 20180528. modify the prompt
import com.android.phone.R;
//*/

/**
 * Service for making GSM and CDMA connections.
 */
public class MtkTelephonyConnectionServiceUtil {

    private static final MtkTelephonyConnectionServiceUtil INSTANCE = new MtkTelephonyConnectionServiceUtil();
    private TelephonyConnectionService mService;

    /// M: CC: Proprietary CRSS handling
    private MtkSuppMessageManager mSuppMessageManager;

    // for cell conn manager
    private int mCurrentDialSubId;
    private int mCurrentDialSlotId;
    private CellConnMgr mCellConnMgr;
    private int mCellConnMgrCurrentRun;
    private int mCellConnMgrTargetRun;
    private int mCellConnMgrState;
    private ArrayList<String> mCellConnMgrStringArray;
    private Context mContext;
    private MtkSimErrorDialog mSimErrorDialog;
    private final BroadcastReceiver mCellConnMgrReceiver = new TcsBroadcastReceiver();

    /// M: CC: PPL (Phone Privacy Lock Service)
    private final BroadcastReceiver mPplReceiver = new TcsBroadcastReceiver();

    /// M: CC: TDD data only
    private MtkLteDataOnlyController mMtkLteDataOnlyController;

    /// M: CC: ECC retry @{
    private EmergencyRetryHandler mEccRetryHandler;
    private int mEccPhoneType = PhoneConstants.PHONE_TYPE_NONE;
    private int mEccRetryPhoneId = -1;
    private boolean mHasPerformEccRetry = false;
    /// @}

    /// M: CC: Vzw/CTVolte ECC @{
    private String mEccNumber;

    TelephonyDevController mTelDevController = TelephonyDevController.getInstance();
    private boolean hasC2kOverImsModem() {
        if (mTelDevController != null &&
                mTelDevController.getModem(0) != null &&
                ((MtkHardwareConfig) mTelDevController.getModem(0)).hasC2kOverImsModem() == true) {
                    return true;
        }
        return false;
    }

    private static final int RAF_C2K = RadioAccessFamily.RAF_IS95A | RadioAccessFamily.RAF_IS95B |
        RadioAccessFamily.RAF_1xRTT | RadioAccessFamily.RAF_EVDO_0 | RadioAccessFamily.RAF_EVDO_A |
        RadioAccessFamily.RAF_EVDO_B | RadioAccessFamily.RAF_EHRPD;
    /// @}

    private static final boolean MTK_CT_VOLTE_SUPPORT
            = "1".equals(SystemProperties.get("persist.mtk_ct_volte_support", "0"));

    MtkTelephonyConnectionServiceUtil() {
        mService = null;
        mContext = null;
        mSimErrorDialog = null;

        /// M: CC: Proprietary CRSS handling
        mSuppMessageManager = null;

        /// M: CC: TDD data only
        mMtkLteDataOnlyController = null;

        /// M: CC: ECC retry
        mEccRetryHandler = null;
    }

    public static MtkTelephonyConnectionServiceUtil getInstance() {
        return INSTANCE;
    }

    public void setService(TelephonyConnectionService s) {
        Log.d(this, "setService: " + s);
        mService = s;
        mContext = mService.getApplicationContext();

        /// M: CC: ECC Retry
        mEccRetryHandler = null;

        /// M: CC: CRSS notification
        enableSuppMessage(s);

        /// M: CC: PPL @{
        IntentFilter intentFilter = new IntentFilter("com.mediatek.ppl.NOTIFY_LOCK");
        mContext.registerReceiver(mPplReceiver, intentFilter);
        /// @}

        /// M: CC: TDD data only
        mMtkLteDataOnlyController = new MtkLteDataOnlyController(mContext);
    }

    /**
     * unset TelephonyConnectionService to be bind.
     */
    public void unsetService() {
        Log.d(this, "unSetService: " + mService);
        mService = null;

        /// M: CC: ECC retry @{
        mEccRetryHandler = null;
        mEccPhoneType = PhoneConstants.PHONE_TYPE_NONE;
        mEccRetryPhoneId = -1;
        mHasPerformEccRetry = false;
        /// @}

        /// M: CC: CRSS notification
        disableSuppMessage();

        /// M: CC: PPL
        mContext.unregisterReceiver(mPplReceiver);

        /// M: CC: TDD data only
        mMtkLteDataOnlyController = null;
    }


    /// M: CC: Proprietary CRSS handling @{
    /**
     * Register for Supplementary Messages once TelephonyConnection is created.
     * @param cs TelephonyConnectionService
     * @param conn TelephonyConnection
     */
    private void enableSuppMessage(TelephonyConnectionService cs) {
        Log.d(this, "enableSuppMessage for " + cs);
        if (mSuppMessageManager == null) {
            mSuppMessageManager = new MtkSuppMessageManager(cs);
            mSuppMessageManager.registerSuppMessageForPhones();
        }
    }

    /**
     * Unregister for Supplementary Messages  once TelephonyConnectionService is destroyed.
     */
    private void disableSuppMessage() {
        Log.d(this, "disableSuppMessage");
        if (mSuppMessageManager != null) {
            mSuppMessageManager.unregisterSuppMessageForPhones();
            mSuppMessageManager = null;
        }
    }

    /**
     * Force Supplementary Message update once TelephonyConnection is created.
     * @param conn The connection to update supplementary messages.
     */
    public void forceSuppMessageUpdate(TelephonyConnection conn) {
        if (mSuppMessageManager != null) {
            Phone p = conn.getPhone();
            if (p != null) {
                Log.d(this, "forceSuppMessageUpdate for " + conn + ", " + p
                        + " phone " + p.getPhoneId());
                mSuppMessageManager.forceSuppMessageUpdate(conn, p);
            }
        }
    }
    /// @}

    public boolean isECCExists() {
        if (mService == null) {
            // it means that never a call exist
            // so still not register in telephonyConnectionService
            // ECC doesn't exist
            return false;
        }

        if (mService.getFgConnection() == null) {
            return false;
        }
        if (mService.getFgConnection().getCall() == null ||
            mService.getFgConnection().getCall().getEarliestConnection() == null ||
            mService.getFgConnection().getCall().getPhone() == null) {
            return false;
        }

        String activeCallAddress = mService.getFgConnection().getCall().
                getEarliestConnection().getAddress();

        boolean bECCExists;

        bECCExists = (PhoneNumberUtils.isEmergencyNumber(activeCallAddress)
                     && !MtkPhoneNumberUtils.isSpecialEmergencyNumber(
                            mService.getFgConnection().getCall().getPhone().getSubId(),
                            activeCallAddress));

        if (bECCExists) {
            Log.d(this, "ECC call exists.");
        }
        else {
            Log.d(this, "ECC call doesn't exists.");
        }

        return bECCExists;
    }

    /// M: CC: TDD data only @{
    /**
     * check if the phone is in TDD data only mode.
     */
    public boolean isDataOnlyMode(Phone phone) {
        if (mMtkLteDataOnlyController != null && phone != null) {
            if (!mMtkLteDataOnlyController.checkPermission(phone.getSubId())) {
                Log.i(this, "isDataOnlyMode, phoneId=" + phone.getPhoneId()
                        + ", phoneType=" + phone.getPhoneType()
                        + ", dataOnly=true");
                return true;
            }
        }
        return false;
    }
    /// @}


    /// M: CC: Error message due to CellConnMgr checking @{
    /**
     * register broadcast Receiver.
     */
    private void cellConnMgrRegisterForSubEvent() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        mContext.registerReceiver(mCellConnMgrReceiver, intentFilter);
    }

    /**
     * unregister broadcast Receiver.
     */
    private void cellConnMgrUnregisterForSubEvent() {
        mContext.unregisterReceiver(mCellConnMgrReceiver);
    }

   /**
     For SIM unplugged, PhoneAccountHandle is null, hence TelephonyConnectionService returns OUTGOING_FAILURE,
     without CellConnMgr checking, UI will show "Call not Sent" Google default dialog.
     For SIM plugged, under
     (1) Flight mode on, MTK SimErrorDialog will show FLIGHT MODE string returned by CellConnMgr.
          Only turning off flight mode via notification bar can dismiss the dialog.
     (2) SIM off, MTK SimErrorDialog will show SIM OFF string returned by CellConnMgr.
          Turning on flight mode, or unplugging SIM can dismiss the dialog.
     (3) SIM locked, MTK SimErrorDialog will show SIM LOCKED string returned by CellConnMgr.
          Turning on flight mode, or unplugging SIM can dismiss the dialog.
     */

    /**
     * Listen to intent of Airplane mode and Sim mode.
     * In case of Airplane mode off or Sim Hot Swap, dismiss SimErrorDialog
     */
    private class TcsBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isInitialStickyBroadcast()) {
                Log.d(this, "Skip initial sticky broadcast");
                return;
            }
            String action = intent.getAction();
            switch (action) {
                /// M: CC: PPL @{
                case "com.mediatek.ppl.NOTIFY_LOCK":
                    Log.d(this, "Receives com.mediatek.ppl.NOTIFY_LOCK");
                    for (android.telecom.Connection conn : mService.getAllConnections()) {
                        if (conn instanceof TelephonyConnection) {
                            ((TelephonyConnection)conn).onHangupAll();
                            break;
                        }
                    }
                    break;
                /// @}

                case Intent.ACTION_AIRPLANE_MODE_CHANGED:
                    Log.d(this, "MtkSimErrorDialog finish due to ACTION_AIRPLANE_MODE_CHANGED");
                    mSimErrorDialog.dismiss();
                    break;
                case TelephonyIntents.ACTION_SIM_STATE_CHANGED:
                    String simState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                    int slotId = intent.getIntExtra(PhoneConstants.SLOT_KEY,
                            SubscriptionManager.INVALID_SIM_SLOT_INDEX);
                    Log.d(this, "slotId: " + slotId + " simState: " + simState);
                    if ((slotId != SubscriptionManager.INVALID_SIM_SLOT_INDEX) &&
                            (slotId == mCurrentDialSlotId) &&
                            (simState.equals(IccCardConstants.INTENT_VALUE_ICC_ABSENT))) {
                        Log.d(this, "MtkSimErrorDialog finish due hot plug out of SIM " +
                                (slotId + 1));
                        mSimErrorDialog.dismiss();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    public void cellConnMgrSetSimErrorDialogActivity(MtkSimErrorDialog dialog) {
        if (mContext == null) {
            Log.d(this, "cellConnMgrSetSimErrorDialogActivity, mContext is null");
            return;
        }

        if (mSimErrorDialog == dialog) {
            Log.d(this, "cellConnMgrSetSimErrorDialogActivity, skip duplicate");
            return;
        }

        mSimErrorDialog = dialog;
        if (mSimErrorDialog != null) {
            cellConnMgrRegisterForSubEvent();
            Log.d(this, "cellConnMgrRegisterForSubEvent for setSimErrorDialogActivity");
        } else {
            cellConnMgrUnregisterForSubEvent();
            Log.d(this, "cellConnMgrUnregisterForSubEvent for setSimErrorDialogActivity");
        }
    }

    public boolean cellConnMgrShowAlerting(int subId) {
        if (mContext == null) {
            Log.d(this, "cellConnMgrShowAlerting, mContext is null");
            return false;
        }

        if(MtkTelephonyManagerEx.getDefault().isWifiCallingEnabled(subId)) {
            Log.d(this, "cellConnMgrShowAlerting: WiFi calling is enabled, return directly.");
            return false;
        }

        mCellConnMgr = new CellConnMgr(mContext);
        mCurrentDialSubId = subId;
        mCurrentDialSlotId = SubscriptionController.getInstance().getSlotIndex(subId);

        //Step1. Query state by indicated request type, the return value are the combination of current states
        mCellConnMgrState = mCellConnMgr.getCurrentState(mCurrentDialSubId, CellConnMgr.STATE_FLIGHT_MODE |
            CellConnMgr.STATE_RADIO_OFF | CellConnMgr.STATE_NOIMSREG_FOR_CTVOLTE);

        // check if need to notify user to do something
        // Since UX might change, check the size of mCellConnMgrStringArray to show dialog.
        if (mCellConnMgrState != CellConnMgr.STATE_READY) {

            //Step2. Query string used to show dialog
            mCellConnMgrStringArray = mCellConnMgr.getStringUsingState(mCurrentDialSubId, mCellConnMgrState);
            mCellConnMgrCurrentRun = 0;
            mCellConnMgrTargetRun = mCellConnMgrStringArray.size() / 4;

            Log.d(this, "cellConnMgrShowAlerting, slotId: " + mCurrentDialSlotId +
                " state: " + mCellConnMgrState + " size: " + mCellConnMgrStringArray.size());

            /// M: for plugin @{
            mCellConnMgrStringArray = ExtensionManager.getTelephonyConnectionServiceExt()
                .customizeSimDisplayString(mCellConnMgrStringArray, mCurrentDialSlotId);
            /// @}

            if (mCellConnMgrTargetRun > 0) {
                cellConnMgrShowAlertingInternal();
                return true;
            }
        }
        return false;
    }

    public void cellConnMgrHandleEvent() {

        //Handle the request if user click on positive button
        mCellConnMgr.handleRequest(mCurrentDialSubId, mCellConnMgrState);

        mCellConnMgrCurrentRun++;

        if (mCellConnMgrCurrentRun != mCellConnMgrTargetRun) {
            cellConnMgrShowAlertingInternal();
        } else {
            cellConnMgrShowAlertingFinalize();
        }
    }

    private void cellConnMgrShowAlertingInternal() {

        //Show confirm dialog with returned dialog title, description, negative button and positive button

        ArrayList<String> stringArray = new ArrayList<String>();
        stringArray.add(mCellConnMgrStringArray.get(mCellConnMgrCurrentRun * 4));
        stringArray.add(mCellConnMgrStringArray.get(mCellConnMgrCurrentRun * 4 + 1));
        stringArray.add(mCellConnMgrStringArray.get(mCellConnMgrCurrentRun * 4 + 2));
        stringArray.add(mCellConnMgrStringArray.get(mCellConnMgrCurrentRun * 4 + 3));

        for (int i = 0; i < stringArray.size(); i++) {
            Log.d(this, "cellConnMgrShowAlertingInternal, string(" + i + ")=" + stringArray.get(i));
        }

        // call dialog ...
        Log.d(this, "cellConnMgrShowAlertingInternal");
        /// M: CC: to enable this part when SimErrorDiaglogActivity class migration done @{
//        final Intent intent = new Intent(mContext, SimErrorDialogActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
//        intent.putStringArrayListExtra(SimErrorDialogActivity.DIALOG_INFORMATION, stringArray);
//        mContext.startActivity(intent);
        /// @}
        if (stringArray.size() < 4) {
            Log.d(this, "cellConnMgrShowAlertingInternal, stringArray is illegle, do nothing.");
            return;
        }
        if (mSimErrorDialog != null) {
            Log.w(this, "cellConnMgrShowAlertingInternal, There's an existing error dialog: "
                    + mSimErrorDialog + ", ignore displaying the new error.");
            return;
        }
        mSimErrorDialog = new MtkSimErrorDialog(mContext, stringArray);
        Log.d(this, "cellConnMgrShowAlertingInternal, show SimErrorDialog: " + mSimErrorDialog);
        mSimErrorDialog.show();
    }

    public void cellConnMgrShowAlertingFinalize() {
        Log.d(this, "cellConnMgrShowAlertingFinalize");
        mCellConnMgrCurrentRun = -1;
        mCellConnMgrTargetRun = 0;
        mCurrentDialSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mCurrentDialSlotId = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
        mCellConnMgrState = -1;
        mCellConnMgr = null;
    }

    public boolean isCellConnMgrAlive() {
        return (mCellConnMgr != null);
    }
    /// @}

    private static class CellConnMgr {
        private static final String TAG = "CellConnMgr";

        /**
         * Bit mask: STATE_READY means the card is under ready state.
         *
         * @internal
         */
        public static final int STATE_READY = 0x00;

        /**
         * Bit mask: STATE_FLIGHT_MODE means under flight mode on.
         *
         * @internal
         */
        public static final int STATE_FLIGHT_MODE = 0x01;

        /**
         * Bit mask: STATE_RADIO_OFF means the card is under radio off state.
         *
         * @internal
         */
        public static final int STATE_RADIO_OFF = 0x02;

        /**
        * Bit mask: STATE_NOIMSREG_FOR_CTVOLTE means the CT SIM card is under ims unavailable state
        * when ENHANCED_4G_MODE_ENABLED is enabled.
        */
        public static final int STATE_NOIMSREG_FOR_CTVOLTE = 0x04;


        public static final boolean MTK_CTVOLTE_SUPPORT =
                SystemProperties.getInt("persist.mtk_ct_volte_support", 0) == 1;


        private Context mContext;
        private static final String INTENT_SET_RADIO_POWER =
                "com.mediatek.internal.telephony.RadioManager.intent.action.FORCE_SET_RADIO_POWER";

        /**
         * To use the utility function, please create the object on your local side.
         *
         * @param context the indicated context
         *
         * @internal
         */
        public CellConnMgr(Context context) {
            mContext = context;

            if (mContext == null) {
                throw new RuntimeException(
                    "CellConnMgr must be created by indicated context");
            }
        }

        /**
         * Query current state by indicated subscription and request type.
         *
         * @param subId indicated subscription
         * @param requestType the request type you cared
         *                    STATE_FLIGHT_MODE means that you would like to query if under flight mode.
         *                    STATE_RADIO_OFF means that you would like to query if this SIM radio off.
         *                    STATE_SIM_LOCKED will check flight mode and radio state first, and then
         *                                     check if under SIM locked state.
         *                    STATE_ROAMING will check flight mode and radio state first, and then
         *                                  check if under roaming.
         * @return a bit mask value composed by STATE_FLIGHT_MODE, STATE_RADIO_OFF, STATE_SIM_LOCKED and
         *         STATE_ROAMING.
         *
         * @internal
         */
        public int getCurrentState(int subId, int requestType) {
            int state = STATE_READY;

            // Query flight mode settings
            int flightMode = Settings.Global.getInt(
                    mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, -1);

            // Query radio state (need to check if the radio off is set by users)
            boolean isRadioOff = !isRadioOn(subId) && isRadioOffBySimManagement(subId);

            // Query SIM state
            int slotId = SubscriptionManager.getSlotIndex(subId);
            TelephonyManager telephonyMgr = TelephonyManager.getDefault();
            boolean isLocked =
                    (TelephonyManager.SIM_STATE_PIN_REQUIRED == telephonyMgr.getSimState(slotId)
                    || TelephonyManager.SIM_STATE_PUK_REQUIRED == telephonyMgr.getSimState(slotId)
                    || TelephonyManager.SIM_STATE_NETWORK_LOCKED == telephonyMgr.getSimState(slotId));

            // Query roaming state
            boolean isRoaming = false;

            Rlog.d(TAG, "[getCurrentState]subId: " + subId + ", requestType:" + requestType +
                    "; (flight mode, radio off, locked, roaming) = ("
                    + flightMode + "," + isRadioOff + "," + isLocked + "," + isRoaming + ")");

            switch (requestType) {
                case STATE_FLIGHT_MODE:
                    state = ((flightMode == 1) ? STATE_FLIGHT_MODE : STATE_READY);
                    break;

                case STATE_RADIO_OFF:
                    state = ((isRadioOff) ? STATE_RADIO_OFF : STATE_READY);
                    break;

                default:
                    state = ((flightMode == 1) ? STATE_FLIGHT_MODE : STATE_READY) |
                            ((isRadioOff) ? STATE_RADIO_OFF : STATE_READY);
            }

            if (state == STATE_READY
                && (requestType & STATE_NOIMSREG_FOR_CTVOLTE) == STATE_NOIMSREG_FOR_CTVOLTE) {
                state =
                    isImsUnavailableForCTVolte(subId) ? STATE_NOIMSREG_FOR_CTVOLTE : STATE_READY;
            }
            Rlog.d(TAG, "[getCurrentState] state:" + state);

            return state;
        }

        /**
         * Get dialog showing description, positive button and negative button string by state.
         *
         * @param subId indicated subscription
         * @param state current state query by getCurrentState(int subId, int requestType).
         * @return title, description, positive button and negative strings with following format.
         *         stringList.get(0) = "state1's title"
         *         stringList.get(1) = "state1's description",
         *         stringList.get(2) = "state1's positive buttion"
         *         stringList.get(3) = "state1's negative button"
         *         stringList.get(4) = "state2's title"
         *         stringList.get(5) = "state1's description",
         *         stringList.get(6) = "state1's positive buttion"
         *         stringList.get(7) = "state1's negative button"
         *         A set is composited of four strings.
         *
         * @internal
         */
        public ArrayList<String> getStringUsingState(int subId, int state) {
            ArrayList<String> stringList = new ArrayList<String>();

            Rlog.d(TAG, "[getStringUsingState] subId: " + subId + ", state:" + state);

            if ((state & (STATE_FLIGHT_MODE | STATE_RADIO_OFF))
                    == (STATE_FLIGHT_MODE | STATE_RADIO_OFF)) {
                // 0. Turn off flight mode + turn radio on
                stringList.add(Resources.getSystem().getString(
                        com.mediatek.internal.R.string.confirm_flight_mode_radio_title));
                stringList.add(Resources.getSystem().getString(
                        com.mediatek.internal.R.string.confirm_flight_mode_radio_msg));
                stringList.add(Resources.getSystem().getString(
                        com.mediatek.internal.R.string.confirm_button_ok));
                stringList.add(Resources.getSystem().getString(
                        com.mediatek.internal.R.string.confirm_button_cancel));
                Rlog.d(TAG, "[getStringUsingState] STATE_FLIGHT_MODE + STATE_RADIO_OFF");
            } else if ((state & STATE_FLIGHT_MODE) == STATE_FLIGHT_MODE) {
                // 1. Turn off flight mode
                stringList.add(Resources.getSystem().getString(
                        com.mediatek.internal.R.string.confirm_flight_mode_title));
                stringList.add(Resources.getSystem().getString(
                        com.mediatek.internal.R.string.confirm_flight_mode_msg));
                stringList.add(Resources.getSystem().getString(
                        com.mediatek.internal.R.string.confirm_button_turn_off));
                stringList.add(Resources.getSystem().getString(
                        com.mediatek.internal.R.string.confirm_button_cancel));
                Rlog.d(TAG, "[getStringUsingState] STATE_FLIGHT_MODE");
            } else if ((state & STATE_RADIO_OFF) == STATE_RADIO_OFF) {
                // 2. Turn radio on
                stringList.add(Resources.getSystem().getString(
                        com.mediatek.internal.R.string.confirm_radio_title));
                stringList.add(Resources.getSystem().getString(
                        com.mediatek.internal.R.string.confirm_radio_msg));
                stringList.add(Resources.getSystem().getString(
                        com.mediatek.internal.R.string.confirm_button_turn_on));
                stringList.add(Resources.getSystem().getString(
                        com.mediatek.internal.R.string.confirm_button_cancel));
                Rlog.d(TAG, "[getStringUsingState] STATE_RADIO_OFF");
            } else if ((state & STATE_NOIMSREG_FOR_CTVOLTE) == STATE_NOIMSREG_FOR_CTVOLTE) {
                // 4. no imsreg for ct volte
                stringList.add(Resources.getSystem().getString(
                        com.mediatek.internal.R.string.confirm_noimsreg_for_ctvolte_title));
                stringList.add(Resources.getSystem().getString(
                        /*/ freeme.liqiang, 20180528. modify the prompt
                        com.mediatek.internal.R.string.confirm_noimsreg_for_ctvolte_msg));
                        /*/
                        R.string.freeme_confirm_noimsreg_for_ctvolte_msg));
                        //*/
                stringList.add(Resources.getSystem().getString(
                        com.mediatek.internal.R.string.confirm_button_ok));
                stringList.add(Resources.getSystem().getString(
                        com.mediatek.internal.R.string.confirm_button_cancel));
                Rlog.d(TAG, "[getStringUsingState] STATE_NOIMSREG_FOR_CTVOLTE");
            }

            Rlog.d(TAG, "[getStringUsingState]stringList size: " + stringList.size());

            return ((ArrayList<String>) stringList.clone());
        }

        /**
         * Handle positive button operation by indicated state.
         *
         * @param subId indicated subscription
         * @param state current state query by getCurrentState(int subId, int requestType).
         *
         * @internal
         */
        public void handleRequest(int subId, int state) {

            Rlog.d(TAG, "[handleRequest] subId: " + subId + ", state:" + state);

            // 1.Turn off flight mode
            if ((state & STATE_FLIGHT_MODE) == STATE_FLIGHT_MODE) {
                Settings.Global.putInt(
                        mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
                mContext.sendBroadcastAsUser(
                        new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).putExtra("state", false),
                        UserHandle.ALL);

                Rlog.d(TAG, "[handleRequest] Turn off flight mode.");
            }

            // 2.Turn radio on
            if ((state & STATE_RADIO_OFF) == STATE_RADIO_OFF) {
                int mSimMode = 0;
                for (int i = 0 ; i < TelephonyManager.getDefault().getSimCount() ; i++) {
                    // TODO: need to revise in case of sub-based modem support
                    int[] targetSubId = SubscriptionManager.getSubId(i);

                    if (((targetSubId != null && isRadioOn(targetSubId[0]))
                            || (i == SubscriptionManager.getSlotIndex(subId)))) {
                        mSimMode = mSimMode | (1 << i);
                    }
                }

                Settings.Global.putInt(mContext.getContentResolver(),
                        MtkSettingsExt.Global.MSIM_MODE_SETTING, mSimMode);

                Intent intent = new Intent(INTENT_SET_RADIO_POWER);
                intent.putExtra(MtkTelephonyIntents.EXTRA_MSIM_MODE, mSimMode);
                mContext.sendBroadcastAsUser(intent, UserHandle.ALL);

                Rlog.d(TAG, "[handleRequest] Turn radio on, MSIM mode:" + mSimMode);
            }

            // 3. no imsreg for ct volte,disable enhanced 4g mode
            if ((state & STATE_NOIMSREG_FOR_CTVOLTE) == STATE_NOIMSREG_FOR_CTVOLTE) {
                int phoneId = SubscriptionManager.getPhoneId(subId);
                MtkImsManager.setEnhanced4gLteModeSetting(mContext, false, phoneId);
                Rlog.d(TAG, "[handleRequest] Turn off ct volte");
            }

        }


        private boolean isRadioOffBySimManagement(int subId) {
            boolean result = true;
            try {
                IMtkTelephonyEx iTelEx = IMtkTelephonyEx.Stub
                        .asInterface(ServiceManager.getService("phoneEx"));

                if (null == iTelEx) {
                    Rlog.d(TAG, "[isRadioOffBySimManagement] iTelEx is null");
                    return false;
                }
                result = iTelEx.isRadioOffBySimManagement(subId);
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }

            Rlog.d(TAG, "[isRadioOffBySimManagement]  subId " + subId + ", result = " + result);
            return result;
        }


        private boolean isRadioOn(int subId) {
            Rlog.d(TAG, "isRadioOff verify subId " + subId);
            boolean radioOn = true;
            try {
                ITelephony iTel = ITelephony.Stub.asInterface(
                        ServiceManager.getService(Context.TELEPHONY_SERVICE));

                if (null == iTel) {
                    Rlog.d(TAG, "isRadioOff iTel is null");
                    return false;
                }

                radioOn = iTel.isRadioOnForSubscriber(subId, mContext.getOpPackageName());
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }

            Rlog.d(TAG, "isRadioOff subId " + subId + " radio on? " + radioOn);
            return radioOn;
        }

        private int getNetworkType(int subId) {
            int networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
            final int dataNetworkType = TelephonyManager.getDefault().getDataNetworkType(subId);
            final int voiceNetworkType = TelephonyManager.getDefault().getVoiceNetworkType(subId);
            Rlog.d(TAG, "updateNetworkType(), dataNetworkType = " + dataNetworkType
                    + ", voiceNetworkType = " + voiceNetworkType);
            if (TelephonyManager.NETWORK_TYPE_UNKNOWN != dataNetworkType) {
                networkType = dataNetworkType;
            } else if (TelephonyManager.NETWORK_TYPE_UNKNOWN != voiceNetworkType) {
                networkType = voiceNetworkType;
            }
            return networkType;
        }

        private boolean isInEcbmMode(int phoneId) {
            String ecbmString = SystemProperties.get("ril.cdma.inecmmode");
            Rlog.d(TAG, "[isInEcbmMode] phoneId = " + phoneId + ", ecbmString = " + ecbmString);
            if (ecbmString == null) {
                return false;
            }

            if (phoneId == SubscriptionManager.INVALID_PHONE_INDEX) {
                return false;
            }


            ecbmString = ecbmString + ",";
            String[] ecmbArrary = ecbmString.split(",");
            if (ecmbArrary == null) {
                return false;
            }

            if (phoneId >= ecmbArrary.length) {
                return false;
            }

            return "true".equals(ecmbArrary[phoneId]);
        }

        private boolean isMainPhoneId(int index) {
            if (SystemProperties.getInt("persist.mtk_mims_support", 1) != 1 && !isDualCTCard()) {
                return true;
            } else {
                int phoneId =
                    SystemProperties.getInt(MtkPhoneConstants.PROPERTY_CAPABILITY_SWITCH, 1) - 1;
                return phoneId == index;
            }
        }

        private boolean isDualCTCard() {
            boolean isDualCTCard = true;
            for (int i = 0 ; i < TelephonyManager.getDefault().getSimCount() ; i++) {
                int[] targetSubId = SubscriptionManager.getSubId(i);
                if (targetSubId == null || !isCTStatusEnabled(targetSubId[0])) {
                    isDualCTCard = false;
                }
            }
            return isDualCTCard;
        }

        private boolean isImsUnavailableForCTVolte(int subId) {
            if (MTK_CTVOLTE_SUPPORT == true) {
                int phoneId = SubscriptionManager.getPhoneId(subId);
                boolean enable4G = true;
                boolean isCTClib = "OP09".equals(SystemProperties.get("persist.operator.optr"));

                if (isCTClib == false) {
                    int settingsNetworkMode = Settings.Global.getInt(
                        mContext.getContentResolver(),
                        Settings.Global.PREFERRED_NETWORK_MODE + subId,
                        Phone.PREFERRED_NT_MODE);
                    enable4G = (settingsNetworkMode == Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA
                        || settingsNetworkMode == MtkGsmCdmaPhone.NT_MODE_LTE_TDD_ONLY);
                    Rlog.d(TAG, "[isImsUnavailableForCTVolte] enable 4g = " + enable4G);
                }

                if (enable4G == true
                    && isMainPhoneId(phoneId)
                    && isCTStatusEnabled(subId)
                    && !TelephonyManager.getDefault().isNetworkRoaming(subId)
                    && MtkImsManager.isEnhanced4gLteModeSettingEnabledByUser(mContext, phoneId)
                    && (isInEcbmMode(phoneId) ||
                        getNetworkType(subId) == TelephonyManager.NETWORK_TYPE_LTE ||
                        getNetworkType(subId) == TelephonyManager.NETWORK_TYPE_LTE_CA)
                    && isImsReg(subId) == false) {
                    Rlog.d(TAG, "isImsUnavailableForCTVolte ture");
                    return true;
               }
            }
            return false;
        }

        private boolean isImsReg(int subId) {
            boolean isImsReg = MtkTelephonyManagerEx.getDefault().isImsRegistered(subId);
            Rlog.d(TAG,"[isImsReg] isImsReg = " + isImsReg);
            return isImsReg;
        }

        private boolean isCTStatusEnabled(int subId) {
            boolean result = false;
            CarrierConfigManager configMgr = (CarrierConfigManager)
                mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
            PersistableBundle b = configMgr.getConfigForSubId(subId);
            if (b != null) {
                result = b.getBoolean(
                        MtkCarrierConfigManager.MTK_KEY_CT_VOLTE_STATUS_BOOL);
            }
            Rlog.d(TAG,"isCTStatusEnabled, subId = " + subId + ", result = " + result);
            return result;
        }
    }

    /// M: CC: ECC retry @{
    public void setEccPhoneType(int phoneType) {
        mEccPhoneType = phoneType;
        Log.i(this, "ECC retry: setEccPhoneType, phoneType=" + phoneType);
    }

    public int getEccPhoneType() {
        return mEccPhoneType;
    }

    public void setEccRetryPhoneId(int phoneId) {
        mEccRetryPhoneId = phoneId;
        Log.i(this, "ECC retry: setEccRetryPhoneId, phoneId=" + phoneId);
    }

    public int getEccRetryPhoneId() {
        return mEccRetryPhoneId;
    }

    public boolean hasPerformEccRetry() {
        return mHasPerformEccRetry;
    }

    /**
     * Check if ECC Retry is running.
     * @return {@code true} if ECC Retry is running and {@code false} otherwise.
     */
    public boolean isEccRetryOn() {
        boolean bIsOn = (mEccRetryHandler != null);
        Log.i(this, "ECC retry: isEccRetryOn, retryOn=" + bIsOn);
        return bIsOn;
    }

    /**
     * Save ECC retry requested parameters. Register once ECC is created.
     * @param request connection request
     * @param initPhoneId phone id of the initial ECC
     */
    public void setEccRetryParams(ConnectionRequest request, int initPhoneId) {
        // Check if UE is set to test mode or not (CTA=1, FTA=2, IOT=3, ...)
        // Skip ECC Retry for TC26.9.6.2.2
        if (SystemProperties.getInt("gsm.gcf.testmode", 0) == 2) {
            Log.i(this, "ECC retry: setEccRetryParams, skip for FTA mode");
            return;
        }

        if (TelephonyManager.getDefault().getPhoneCount() <= 1) {
            if (!MTK_CT_VOLTE_SUPPORT) {
                Log.i(this, "ECC retry: setEccRetryParams, skip for SS project");
                return;
            }
        }

        Log.i(this, "ECC retry: setEccRetryParams, request=" + request
                + ", initPhoneId=" + initPhoneId);
        if (mEccRetryHandler == null) {
            mEccRetryHandler = new EmergencyRetryHandler(request, initPhoneId);
        }
    }

    public void clearEccRetryParams() {
        Log.i(this, "ECC retry: clearEccRetryParams");
        mEccRetryHandler = null;
    }

    /**
     * Set original ECC Call Id
     * @param id CallId
     */
    public void setEccRetryCallId(String id) {
        Log.i(this, "ECC retry: setEccRetryCallId, id=" + id);
        if (mEccRetryHandler != null) {
            mEccRetryHandler.setCallId(id);
        }
    }

    /**
     * If ECC Retry timeout
     * @return {@code true} if ECC Retry timeout {@code false} otherwise.
     */
    public boolean eccRetryTimeout() {
        boolean bIsTimeout = false;
        if (mEccRetryHandler != null) {
            if (mEccRetryHandler.isTimeout()) {
                mEccRetryHandler = null;
                bIsTimeout = true;
            }
        }
        Log.i(this, "ECC retry: eccRetryTimeout, timeout=" + bIsTimeout);
        return bIsTimeout;
    }

    /**
     * Perform ECC Retry
     */
    public void performEccRetry() {
        Log.i(this, "ECC retry: performEccRetry");
        if (mEccRetryHandler == null || mService == null) {
            return;
        }
        mHasPerformEccRetry = true;
        ConnectionRequest retryRequest = new ConnectionRequest(
                mEccRetryHandler.getNextAccountHandle(),
                mEccRetryHandler.getRequest().getAddress(),
                mEccRetryHandler.getRequest().getExtras(),
                mEccRetryHandler.getRequest().getVideoState());
        mService.createConnectionInternal(mEccRetryHandler.getCallId(), retryRequest);
    }

    /**
     * Select the phone by special ECC rule.
     *
     * @param accountHandle The target PhoneAccountHandle.
     * @param number The ecc number.
     */
    public Phone selectPhoneBySpecialEccRule(
            PhoneAccountHandle accountHandle,
            String number, Phone defaultEccPhone) {
        EmergencyRuleHandler eccRuleHandler = null;
        if (getEccRetryPhoneId() != -1) {
            eccRuleHandler = new EmergencyRuleHandler(
                    PhoneUtils.makePstnPhoneAccountHandle(
                            Integer.toString(getEccRetryPhoneId())),
                    number, true, defaultEccPhone);
        } else {
            eccRuleHandler = new EmergencyRuleHandler(
                    accountHandle,
                    number, isEccRetryOn(), defaultEccPhone);
        }
        return eccRuleHandler.getPreferredPhone();
    }
    /// @}

    /// M: CC: Vzw/CTVolte ECC @{
    public void setEmergencyNumber(String numberToDial) {
        mEccNumber = numberToDial;
    }

    public void enterEmergencyMode(Phone phone, int isAirplane) {
        if (!hasC2kOverImsModem()
                && !MtkTelephonyManagerEx.getDefault().useVzwLogic()
                && !MtkTelephonyManagerEx.getDefault().useATTLogic()) {
            return;
        }

        // Do not enter Emergency Mode for ISO ECC only.
        // isLocalEmergencyNumber() = true, isEmergencyNumber()=false for ISO ECC only.
        // Since FW checks ECC without ISO, emergency mode setting should be consistent with FW.
        // Do not enter Emergency Mode for CTA ECC (110,119,120,122).
        // CTA ECC: shown as ECC but dialed as normal call
        // with SIM: 93(true) -> ATD,   91-legacy(true) -> ATD
        // w/o SIM: 93(false) -> ATDE,   91-legacy(true) -> ATD -> MD(ATDE)
        if (mEccNumber == null ||
                MtkPhoneNumberUtils.isSpecialEmergencyNumber(phone.getSubId(), mEccNumber) ||
                !PhoneNumberUtils.isEmergencyNumber(phone.getSubId(), mEccNumber)) {
            return;
        }

        // TODO: Need to design for DSDS under airplane mode
        // ECM indicates different logic in Modem, set ECM per Modem request as below:
        // Condition to set ECM:
        // - 6M(with C2K) project: only to C2K-enabled phone
        // - 5M(without C2K) project: specific OP such as Vzw
        // Timing to set ECM:
        // - Under flight mode (set in TeleService, use EFUN channel):
        //    - SS: before Radio on to speed ECC network searching
        //    - DSDS: after Radio on and phone is selected
        // - Not Under flight mode:
        //    - 91-legacy: set in TeleService, use EFUN channel
        //    - 93: set in RILD , use ATD channel
        int raf = phone.getRadioAccessFamily();
        if ((raf & RAF_C2K) > 0
                || MtkTelephonyManagerEx.getDefault().useVzwLogic()
                || MtkTelephonyManagerEx.getDefault().useATTLogic()) {
            Log.d(this, "Enter Emergency Mode, airplane mode:" + isAirplane);
            ((MtkGsmCdmaPhone) phone).mMtkCi.setCurrentStatus(isAirplane,
                    phone.isImsRegistered() ? 1 : 0,
                    null);
        }

        mEccNumber = null;
    }
    /// @}

    /// M: CC: Set ECC in progress @{
    public void setInEcc(boolean inProgress) {
        MtkTelephonyManagerEx telEx = MtkTelephonyManagerEx.getDefault();
        if (inProgress) {
            if (!telEx.isEccInProgress()) {
                telEx.setEccInProgress(true);
                Intent intent = new Intent("android.intent.action.ECC_IN_PROGRESS");
                intent.putExtra("in_progress", inProgress);
                mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            }
        } else {
            if (telEx.isEccInProgress()) {
                telEx.setEccInProgress(false);
                Intent intent = new Intent("android.intent.action.ECC_IN_PROGRESS");
                intent.putExtra("in_progress", inProgress);
                mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            }
        }
    }
    /// @}

    /// M: For VoLTE enhanced conference call. @{
    /**
     * Create a conference connection given an incoming request. This is used to attach to existing
     * incoming calls.
     *
     * @param request Details about the incoming call.
     * @return The {@code GsmConnection} object to satisfy this call, or {@code null} to
     *         not handle the call.
     */
    private android.telecom.Connection createIncomingConferenceHostConnection(
            Phone phone, ConnectionRequest request) {
        Log.i(this, "createIncomingConferenceHostConnection, request: " + request);
        if (mService == null || phone == null) {
            return android.telecom.Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.ERROR_UNSPECIFIED));
        }
        Call call = phone.getRingingCall();
        if (!call.getState().isRinging()) {
            Log.i(this, "onCreateIncomingConferenceHostConnection, no ringing call");
            return android.telecom.Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.INCOMING_MISSED,
                            "Found no ringing call"));
        }
        com.android.internal.telephony.Connection originalConnection =
                call.getState() == Call.State.WAITING ?
                    call.getLatestConnection() : call.getEarliestConnection();
        for (android.telecom.Connection connection : mService.getAllConnections()) {
            if (connection instanceof TelephonyConnection) {
                TelephonyConnection telephonyConnection = (TelephonyConnection) connection;
                if (telephonyConnection.getOriginalConnection() == originalConnection) {
                    Log.i(this, "original connection already registered");
                    return android.telecom.Connection.createCanceledConnection();
                }
            }
        }
        /// M: CC: Vzw ECC/hVoLTE redial
        //GsmConnection connection = new GsmConnection(originalConnection, null);
        MtkGsmCdmaConnection connection = new MtkGsmCdmaConnection(PhoneConstants.PHONE_TYPE_GSM,
            originalConnection, null, null, false, false);
        return connection;
    }
    /**
     * Create a conference connection given an outgoing request. This is used to initiate new
     * outgoing calls.
     *
     * @param request Details about the outgoing call.
     * @return The {@code GsmConnection} object to satisfy this call, or the result of an invocation
     *         of {@link Connection#createFailedConnection(DisconnectCause)} to not handle the call.
     */
    private android.telecom.Connection createOutgoingConferenceHostConnection(
            Phone phone, final ConnectionRequest request, List<String> numbers) {
        Log.i(this, "createOutgoingConferenceHostConnection, request: " + request);
        if (phone == null) {
            Log.d(this, "createOutgoingConferenceHostConnection, phone is null");
            return android.telecom.Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.OUTGOING_FAILURE, "Phone is null"));
        }
        if (MtkTelephonyConnectionServiceUtil.getInstance().
                cellConnMgrShowAlerting(phone.getSubId())) {
            Log.d(this,
                "createOutgoingConferenceHostConnection, cellConnMgrShowAlerting() check fail");
            return android.telecom.Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            mediatek.telephony.MtkDisconnectCause.OUTGOING_CANCELED_BY_SERVICE,
                                    "cellConnMgrShowAlerting() check fail"));
        }
        int state = phone.getServiceState().getState();
        switch (state) {
            case ServiceState.STATE_IN_SERVICE:
            case ServiceState.STATE_EMERGENCY_ONLY:
                break;
            case ServiceState.STATE_OUT_OF_SERVICE:
                return android.telecom.Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                android.telephony.DisconnectCause.OUT_OF_SERVICE,
                                "ServiceState.STATE_OUT_OF_SERVICE"));
            case ServiceState.STATE_POWER_OFF:
                return android.telecom.Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                android.telephony.DisconnectCause.POWER_OFF,
                                "ServiceState.STATE_POWER_OFF"));
            default:
                Log.d(this, "onCreateOutgoingConnection, unknown service state: %d", state);
                return android.telecom.Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                android.telephony.DisconnectCause.OUTGOING_FAILURE,
                                "Unknown service state " + state));
        }
        // Don't call createConnectionFor() because we can't add this connection to
        // GsmConferenceController
        /// M: CC: Vzw ECC/hVoLTE redial
        //GsmConnection connection = new GsmConnection(null, null);
        MtkGsmCdmaConnection connection = new MtkGsmCdmaConnection(PhoneConstants.PHONE_TYPE_GSM,
            null, null, null, false, true);
        connection.setInitializing();
        connection.setVideoState(request.getVideoState());
        placeOutgoingConferenceHostConnection(connection, phone, request, numbers);
        return connection;
    }

    private void placeOutgoingConferenceHostConnection(
            TelephonyConnection connection, Phone phone, ConnectionRequest request,
            List<String> numbers) {
        com.android.internal.telephony.Connection originalConnection = null;
        try {
            if (phone instanceof MtkGsmCdmaPhone) {
                originalConnection =
                        ((MtkGsmCdmaPhone)phone).dial(numbers, request.getVideoState());
            } else {
                Log.d(this, "Phone is not MtkImsPhone");
            }
        } catch (CallStateException e) {
            Log.e(this, e, "placeOutgoingConfHostConnection, phone.dial exception: " + e);
            connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(
                    android.telephony.DisconnectCause.OUTGOING_FAILURE,
                    e.getMessage()));
            return;
        }
        if (originalConnection == null) {
            int telephonyDisconnectCause = android.telephony.DisconnectCause.OUTGOING_FAILURE;
            Log.d(this, "placeOutgoingConnection, phone.dial returned null");
            connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(
                    telephonyDisconnectCause, "Connection is null"));
        } else {
            connection.setOriginalConnection(originalConnection);
        }
    }

    /**
     * This can be used by telecom to either create a new outgoing conference call or attach
     * to an existing incoming conference call.
     */
    public Conference createConference(
            ImsConferenceController imsConfController,
            Phone phone,
            final ConnectionRequest request,
            final List<String> numbers,
            boolean isIncoming) {
        if (imsConfController == null) {
            return null;
        }
        android.telecom.Connection connection = isIncoming ?
            createIncomingConferenceHostConnection(phone, request)
                : createOutgoingConferenceHostConnection(phone, request, numbers);
        Log.d(this, "onCreateConference, connection: %s", connection);
        if (connection == null) {
            Log.d(this, "onCreateConference, connection: %s");
            return null;
        } else if (connection.getState() ==
                android.telecom.Connection.STATE_DISCONNECTED) {
            Log.d(this, "the host connection is dicsonnected");
            return createFailedConference(connection.getDisconnectCause());
        /// M: CC: Vzw ECC/hVoLTE redial
        //} else if (!(connection instanceof GsmConnection)) {
        } else if (!(connection instanceof MtkGsmCdmaConnection) ||
                ((MtkGsmCdmaConnection) connection).getPhoneType() != PhoneConstants.PHONE_TYPE_GSM) {
            Log.d(this, "abnormal case, the host connection isn't GsmConnection");
            int telephonyDisconnectCause = android.telephony.DisconnectCause.ERROR_UNSPECIFIED;
            connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(
                    telephonyDisconnectCause));
            return createFailedConference(telephonyDisconnectCause, "unexpected error");
        } else if (!(imsConfController instanceof ImsConferenceController)) {
            Log.d(this, "abnormal case, not ImsConferenceController");
            int telephonyDisconnectCause = android.telephony.DisconnectCause.ERROR_UNSPECIFIED;
            connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(
                    telephonyDisconnectCause));
            return createFailedConference(telephonyDisconnectCause,
                    "Not ImsConferenceController");
        }
        return ((ImsConferenceController)imsConfController).createConference(
                    (TelephonyConnection) connection);
    }

    public Conference createFailedConference(int disconnectCause, String reason) {
        return createFailedConference(
            DisconnectCauseUtil.toTelecomDisconnectCause(disconnectCause, reason));
    }

    public Conference createFailedConference(android.telecom.DisconnectCause disconnectCause) {
        Conference failedConference = new Conference(null) { };
        failedConference.setDisconnected(disconnectCause);
        return failedConference;
    }
    /// @}

    /// IMS SS
    /**
     * Register Supplementary Messages for ImsPhone.
     * @param phone ImsPhone
     */
    public void registerSuppMessageForImsPhone(Phone phone) {
        if (mSuppMessageManager == null) {
            return;
        }
        mSuppMessageManager.registerSuppMessageForPhone(phone);
    }
    /**
     * Unregister Supplementary Messages for ImsPhone.
     * @param phone ImsPhone
     */
    public void unregisterSuppMessageForImsPhone(Phone phone) {
        if (mSuppMessageManager == null) {
            return;
        }
        mSuppMessageManager.unregisterSuppMessageForPhone(phone);
    }
    /// @}

    /// M: SS: Error message due to VoLTE SS checking @{
    //--------------[VoLTE_SS] notify user when volte mmi request while data off-------------
    /**
     * This function used to judge whether the dialed mmi needs to be blocked (which needs XCAP)
     * Disallow SS setting/query.
     * @param phone The phone to dial
     * @param number The number to dial
     * @return {@code true} if the number has MMI format to be blocked and {@code false} otherwise.
     */
    private boolean isBlockedMmi(Phone phone, String dialString) {
        boolean isBlockedMmi = false;

        if (PhoneNumberUtils.isUriNumber(dialString)) {
            return false;
        }
        String dialPart = PhoneNumberUtils.extractNetworkPortionAlt(PhoneNumberUtils.
                stripSeparators(dialString));

        if (!((dialPart.startsWith("*") || dialPart.startsWith("#"))
                && dialPart.endsWith("#"))) {
            return false;
        }

        ImsPhone imsPhone = (ImsPhone)phone.getImsPhone();
        boolean imsUseEnabled = phone.isImsUseEnabled()
                 && imsPhone != null
                 && imsPhone.isVolteEnabled()
                 && imsPhone.isUtEnabled()
                 && (imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE);

        if (imsUseEnabled == true) {
            isBlockedMmi = MtkImsPhoneMmiCode.isUtMmiCode(
                    dialPart, imsPhone);
        } else if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM) {
            int slot = SubscriptionController.getInstance().getSlotIndex(phone.getSubId());
            UiccCardApplication cardApp = UiccController.getInstance().
                    getUiccCardApplication(slot, UiccController.APP_FAM_3GPP);
            isBlockedMmi = MtkGsmMmiCode.isUtMmiCode(
                    dialPart, (MtkGsmCdmaPhone) phone, cardApp);
        }
        Log.d(this, "isBlockedMmi = " + isBlockedMmi + ", imsUseEnabled = " + imsUseEnabled);
        return isBlockedMmi;
    }

    /**
     * This function used to check whether we should notify user to open data connection.
     * For now, we judge certain mmi code + "IMS-phoneAccount" + data connection is off.
     * @param number The number to dial
     * @param phone The target phone
     * @return {@code true} if the notification should pop up and {@code false} otherwise.
     */
    public boolean shouldOpenDataConnection(String number,  Phone phone) {

        return (isBlockedMmi(phone, number) &&
                TelephonyUtils.shouldShowOpenMobileDataDialog(mContext, phone.getSubId()));
    }
    /// @}

}

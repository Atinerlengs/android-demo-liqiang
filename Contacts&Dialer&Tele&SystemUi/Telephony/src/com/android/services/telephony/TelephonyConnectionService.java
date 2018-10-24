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
/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.services.telephony;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telecom.Conference;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.DisconnectCause;
import android.telecom.Logging.Session;
import android.telecom.ParcelableConference;
import android.telecom.ParcelableConnection;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.RadioAccessFamily;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.Toast;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.SomeArgs;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.imsphone.ImsExternalCallTracker;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.phone.MMIDialogActivity;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.mediatek.internal.telecom.IMtkConnectionService;
import com.mediatek.internal.telecom.IMtkConnectionServiceAdapter;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.services.telephony.MtkGsmCdmaConnection;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.selfactivation.ISelfActivation;
import com.mediatek.services.telephony.MtkTelephonyConnectionServiceUtil;


/// M: For ECC change feature @{
import com.mediatek.services.telephony.SwitchPhoneHelper;
/// @}
/// M: CC: Vzw/CTVolte ECC @{
import com.android.internal.telephony.TelephonyDevController;
import com.mediatek.internal.telephony.MtkHardwareConfig;
/// @}
import com.mediatek.phone.ext.IDigitsUtilExt;
/// M: CC: to check whether the device has on-going ECC
import com.mediatek.telephony.MtkTelephonyManagerEx;
import com.mediatek.phone.ext.ExtensionManager;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;


import mediatek.telecom.MtkConnection;
import mediatek.telecom.MtkTelecomManager;


/**
 * Service for making GSM and CDMA connections.
 */
public class TelephonyConnectionService extends ConnectionService {

    private static final String TAG = "TeleConnService";

    private static final int MTK_MSG_BASE = 1000;

    /// M: CC: Interface for ECT
    private static final int MSG_ECT = MTK_MSG_BASE + 0;
    /// M: CC: HangupAll for FTA 31.4.4.2
    private static final int MSG_HANGUP_ALL = MTK_MSG_BASE + 1;
    /// M: CC: For DSDS/DSDA Two-action operation.
    private static final int MSG_HANDLE_ORDERED_USER_OPERATION = MTK_MSG_BASE + 2;
    /// M: VoLte
    private static final int MSG_INVITE_CONFERENCE_PARTICIPANTS = MTK_MSG_BASE + 3;
    private static final int MSG_CREATE_CONFERENCE = MTK_MSG_BASE + 4;
    private static final int MSG_BLIND_ASSURED_ECT = MTK_MSG_BASE + 5;
    private static final int MSG_DEVICE_SWITCH = MTK_MSG_BASE + 6;
    private static final int MSG_CANCEL_DEVICE_SWITCH = MTK_MSG_BASE + 7;
    // If configured, reject attempts to dial numbers matching this pattern.
    private static final Pattern CDMA_ACTIVATION_CODE_REGEX_PATTERN =
            Pattern.compile("\\*228[0-9]{0,2}");

    /// M: CC: ECC retry @{
    private SwitchPhoneHelper mSwitchPhoneHelper;
    /// @}

    protected TelephonyConnectionServiceProxy mTelephonyConnectionServiceProxy =
            new TelephonyConnectionServiceProxy() {
        @Override
        public Collection<Connection> getAllConnections() {
            return TelephonyConnectionService.this.getAllConnections();
        }
        @Override
        public void addConference(TelephonyConference mTelephonyConference) {
            TelephonyConnectionService.this.addConference(mTelephonyConference);
        }
        @Override
        public void addConference(ImsConference mImsConference) {
            TelephonyConnectionService.this.addConference(mImsConference);
        }
        @Override
        public void removeConnection(Connection connection) {
            TelephonyConnectionService.this.removeConnection(connection);
        }
        @Override
        public void addExistingConnection(PhoneAccountHandle phoneAccountHandle,
                                          Connection connection) {
            TelephonyConnectionService.this
                    .addExistingConnection(phoneAccountHandle, connection);
        }
        @Override
        public void addExistingConnection(PhoneAccountHandle phoneAccountHandle,
                Connection connection, Conference conference) {
            TelephonyConnectionService.this
                    .addExistingConnection(phoneAccountHandle, connection, conference);
        }
        @Override
        public void addConnectionToConferenceController(TelephonyConnection connection) {
            TelephonyConnectionService.this.addConnectionToConferenceController(connection);
        }

        @Override
        public void performImsConferenceSRVCC(Conference imsConf,
                ArrayList<com.android.internal.telephony.Connection> radioConnections,
                        String telecomCallId) {
            TelephonyConnectionService.this.performImsConferenceSRVCC(
                    imsConf, radioConnections, telecomCallId);
        }
    };

    protected TelephonyConferenceController mTelephonyConferenceController =
            new TelephonyConferenceController(mTelephonyConnectionServiceProxy);
    protected final CdmaConferenceController mCdmaConferenceController =
            new CdmaConferenceController(this);
    protected ImsConferenceController mImsConferenceController =
            new ImsConferenceController(TelecomAccountRegistry.getInstance(this),
                    mTelephonyConnectionServiceProxy);

    private ComponentName mExpectedComponentName = null;
    private EmergencyCallHelper mEmergencyCallHelper;
    protected EmergencyTonePlayer mEmergencyTonePlayer;

    // Contains one TelephonyConnection that has placed a call and a memory of which Phones it has
    // already tried to connect with. There should be only one TelephonyConnection trying to place a
    // call at one time. We also only access this cache from a TelephonyConnection that wishes to
    // redial, so we use a WeakReference that will become stale once the TelephonyConnection is
    // destroyed.
    private Pair<WeakReference<TelephonyConnection>, List<Phone>> mEmergencyRetryCache;

    private IMtkConnectionServiceAdapter mMtkAdapter = null;

    /**
     * Keeps track of the status of a SIM slot.
     */
    private static class SlotStatus {
        public int slotId;
        // RAT capabilities
        public int capabilities;
        // By default, we will assume that the slots are not locked.
        public boolean isLocked = false;

        public SlotStatus(int slotId, int capabilities) {
            this.slotId = slotId;
            this.capabilities = capabilities;
        }
    }

    // SubscriptionManager Proxy interface for testing
    public interface SubscriptionManagerProxy {
        int getDefaultVoicePhoneId();
        int getSimStateForSlotIdx(int slotId);
        int getPhoneId(int subId);
    }

    private SubscriptionManagerProxy mSubscriptionManagerProxy = new SubscriptionManagerProxy() {
        @Override
        public int getDefaultVoicePhoneId() {
            return SubscriptionManager.getDefaultVoicePhoneId();
        }

        @Override
        public int getSimStateForSlotIdx(int slotId) {
            return SubscriptionManager.getSimStateForSlotIndex(slotId);
        }

        @Override
        public int getPhoneId(int subId) {
            return SubscriptionManager.getPhoneId(subId);
        }
    };

    // TelephonyManager Proxy interface for testing
    public interface TelephonyManagerProxy {
        int getPhoneCount();
        boolean hasIccCard(int slotId);
    }

    private TelephonyManagerProxy mTelephonyManagerProxy = new TelephonyManagerProxy() {
        private final TelephonyManager sTelephonyManager = TelephonyManager.getDefault();

        @Override
        public int getPhoneCount() {
            return sTelephonyManager.getPhoneCount();
        }

        @Override
        public boolean hasIccCard(int slotId) {
            return sTelephonyManager.hasIccCard(slotId);
        }
    };

    //PhoneFactory proxy interface for testing
    public interface PhoneFactoryProxy {
        Phone getPhone(int index);
        Phone getDefaultPhone();
        Phone[] getPhones();
    }

    protected PhoneFactoryProxy mPhoneFactoryProxy = new PhoneFactoryProxy() {
        @Override
        public Phone getPhone(int index) {
            return PhoneFactory.getPhone(index);
        }

        @Override
        public Phone getDefaultPhone() {
            return PhoneFactory.getDefaultPhone();
        }

        @Override
        public Phone[] getPhones() {
            return PhoneFactory.getPhones();
        }
    };

    @VisibleForTesting
    public void setSubscriptionManagerProxy(SubscriptionManagerProxy proxy) {
        mSubscriptionManagerProxy = proxy;
    }

    @VisibleForTesting
    public void setTelephonyManagerProxy(TelephonyManagerProxy proxy) {
        mTelephonyManagerProxy = proxy;
    }

    @VisibleForTesting
    public void setPhoneFactoryProxy(PhoneFactoryProxy proxy) {
        mPhoneFactoryProxy = proxy;
    }

    /// M: CC: Vzw/CTVolte ECC @{
    TelephonyDevController mTelDevController = TelephonyDevController.getInstance();
    private boolean hasC2kOverImsModem() {
        if (mTelDevController != null &&
                mTelDevController.getModem(0) != null &&
                ((MtkHardwareConfig) mTelDevController.getModem(0)).hasC2kOverImsModem() == true) {
                    return true;
        }
        return false;
    }
    /// @}

    /**
     * A listener to actionable events specific to the TelephonyConnection.
     */
    protected final TelephonyConnection.TelephonyConnectionListener mTelephonyConnectionListener =
            new TelephonyConnection.TelephonyConnectionListener() {
        @Override
        public void onOriginalConnectionConfigured(TelephonyConnection c) {
            addConnectionToConferenceController(c);
        }

        @Override
        public void onOriginalConnectionRetry(TelephonyConnection c) {
            retryOutgoingOriginalConnection(c);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.initLogging(this);
        mExpectedComponentName = new ComponentName(this, this.getClass());
        mEmergencyTonePlayer = new EmergencyTonePlayer(this);
        TelecomAccountRegistry.getInstance(this).setTelephonyConnectionService(this);
        /// M: CC: Use MtkTelephonyConnectionServiceUtil
        MtkTelephonyConnectionServiceUtil.getInstance().setService(this);
    }

    /// M: CC: Use MtkTelephonyConnectionServiceUtil @{
    @Override
    public void onDestroy() {
        /// M: CC: to check whether the device has on-going ECC
        MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
        MtkTelephonyConnectionServiceUtil.getInstance().unsetService();
        /// M: CC: Destroy CDMA conference controller.
        mCdmaConferenceController.onDestroy();
        /// M: CC: ECC retry @{
        if (mSwitchPhoneHelper != null) {
            mSwitchPhoneHelper.onDestroy();
        }
        /// @}
        /// M: CC: Cleanup all listeners to avoid callbacks after service destroyed. @{
        if (mEmergencyCallHelper != null) {
            mEmergencyCallHelper.cleanup();
        }
        /// @}
        super.onDestroy();
    }
    /// @}

    private MtkConnectionServiceBinder mMtkBinder = null;

    @Override
    protected IBinder getConnectionServiceBinder() {
        if (mMtkBinder == null) {
            Log.d(this, "init MtkConnectionServiceBinder");
            mMtkBinder = new MtkConnectionServiceBinder();
        }
        return (IBinder) mMtkBinder;
    }

    @Override
    public Connection onCreateOutgoingConnection(
            PhoneAccountHandle connectionManagerPhoneAccount,
            final ConnectionRequest request) {
        Log.i(this, "onCreateOutgoingConnection, request: " + request);
        /// M: clarify the correct PhoneAccountHandle used.@{
        log("onCreateOutgoingConnection, handle:" + request.getAccountHandle());
        /// @}

        Uri handle = request.getAddress();
        if (handle == null) {
            Log.d(this, "onCreateOutgoingConnection, handle is null");
            return Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.NO_PHONE_NUMBER_SUPPLIED,
                            "No phone number supplied"));
        }

        /// M: [ALPS02340908] To avoid JE @{
        if (request.getAccountHandle() == null) {
            log("onCreateOutgoingConnection, PhoneAccountHandle is null");
            return Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.NO_PHONE_NUMBER_SUPPLIED,
                            "No phone number supplied"));
        }
        /// @}
        /// M: CC: ECC retry @{
        if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
            int phoneId = SubscriptionManager.INVALID_PHONE_INDEX;
            try {
                phoneId = Integer.parseInt(request.getAccountHandle().getId());
            } catch (NumberFormatException e) {
                phoneId = SubscriptionManager.INVALID_PHONE_INDEX;
            } finally {
                if (PhoneFactory.getPhone(phoneId) == null) {
                    // We don't stop ECC retry, because it's for ignoring normal call during ECC,
                    // the emergency call is still on going.
                    Log.i(this, "onCreateOutgoingConnection, phone is null");
                    return Connection.createFailedConnection(
                            DisconnectCauseUtil.toTelecomDisconnectCause(
                                    android.telephony.DisconnectCause.OUTGOING_FAILURE,
                                    "Phone is null"));
                }
            }
        }
        /// @}

        String scheme = handle.getScheme();
        String number;
        if (PhoneAccount.SCHEME_VOICEMAIL.equals(scheme)) {
            // TODO: We don't check for SecurityException here (requires
            // CALL_PRIVILEGED permission).
            final Phone phone = getPhoneForAccount(request.getAccountHandle(), false);
            if (phone == null) {
                Log.d(this, "onCreateOutgoingConnection, phone is null");
                return Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                android.telephony.DisconnectCause.OUT_OF_SERVICE,
                                "Phone is null"));
            }
            number = phone.getVoiceMailNumber();
            if (TextUtils.isEmpty(number)) {
                Log.d(this, "onCreateOutgoingConnection, no voicemail number set.");
                return Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                android.telephony.DisconnectCause.VOICEMAIL_NUMBER_MISSING,
                                "Voicemail scheme provided but no voicemail number set."));
            }

            // Convert voicemail: to tel:
            handle = Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null);
        } else {
            if (!PhoneAccount.SCHEME_TEL.equals(scheme) && !PhoneAccount.SCHEME_SIP.equals(scheme)){
                Log.d(this, "onCreateOutgoingConnection, Handle %s is not type tel", scheme);
                return Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                android.telephony.DisconnectCause.INVALID_NUMBER,
                                "Handle scheme is not type tel"));
            }

            number = handle.getSchemeSpecificPart();
            if (TextUtils.isEmpty(number)) {
                Log.d(this, "onCreateOutgoingConnection, unable to parse number");
                return Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                android.telephony.DisconnectCause.INVALID_NUMBER,
                                "Unable to parse number"));
            }

            /// M: CC: ECC retry @{
            //final Phone phone = getPhoneForAccount(request.getAccountHandle(), false);
            Phone phone = null;
            if (!MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                phone = getPhoneForAccount(request.getAccountHandle(), false);
            }
            /// @}

            if (phone != null && CDMA_ACTIVATION_CODE_REGEX_PATTERN.matcher(number).matches()) {
                // Obtain the configuration for the outgoing phone's SIM. If the outgoing number
                // matches the *228 regex pattern, fail the call. This number is used for OTASP, and
                // when dialed could lock LTE SIMs to 3G if not prohibited..
                boolean disableActivation = false;
                CarrierConfigManager cfgManager = (CarrierConfigManager)
                        phone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
                if (cfgManager != null) {
                    disableActivation = cfgManager.getConfigForSubId(phone.getSubId())
                            .getBoolean(CarrierConfigManager.KEY_DISABLE_CDMA_ACTIVATION_CODE_BOOL);
                }

                if (disableActivation) {
                    return Connection.createFailedConnection(
                            DisconnectCauseUtil.toTelecomDisconnectCause(
                                    android.telephony.DisconnectCause
                                            .CDMA_ALREADY_ACTIVATED,
                                    "Tried to dial *228"));
                }
            }
        }

        // Convert into emergency number if necessary
        // This is required in some regions (e.g. Taiwan).
        /// M: CC: Avoid redundant emergency number checking @{
        boolean converted = false;
        boolean isEmergencyBeforeConvert = PhoneNumberUtils.isLocalEmergencyNumber(this, number);
        if (!isEmergencyBeforeConvert) {
            final Phone phone = getPhoneForAccount(request.getAccountHandle(), false);
            // We only do the conversion if the phone is not in service. The un-converted
            // emergency numbers will go to the correct destination when the phone is in-service,
            // so they will only need the special emergency call setup when the phone is out of
            // service.
            if (phone == null || phone.getServiceState().getState()
                    != ServiceState.STATE_IN_SERVICE) {
                String convertedNumber = PhoneNumberUtils.convertToEmergencyNumber(this, number);
                if (!TextUtils.equals(convertedNumber, number)) {
                    Log.i(this, "onCreateOutgoingConnection, converted to emergency number");
                    number = convertedNumber;
                    handle = Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null);
                    converted = true;
                }
            }
        }
        /// @}
        final String numberToDial = number;

        // M: CC: Avoid redundant emergency number checking
        final boolean isEmergencyNumber = converted ?
                PhoneNumberUtils.isLocalEmergencyNumber(this, numberToDial) :
                isEmergencyBeforeConvert;

        /// M: CC: ECC retry @{
        if (!isEmergencyNumber && MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
            Log.i(this, "ECC retry: clear ECC param due to SIM state/phone type change, not ECC");
            MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
            /// M: CC: to check whether the device has on-going ECC
            MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
            Log.i(this, "onCreateOutgoingConnection, phone is null");
            return Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.OUT_OF_SERVICE,
                            "Phone is null"));
        }
        /// @}

        if (isEmergencyNumber) {
            /* If current phone number will be treated as normal call in Telephony Framework,
               do not need to enable ECC retry mechanism */
            final boolean isDialedByEmergencyCommand = PhoneNumberUtils.isEmergencyNumber(
                                        numberToDial);

            /// M: CC: to check whether the device has on-going ECC
            MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(true);

            final Uri emergencyHandle = handle;
            // By default, Connection based on the default Phone, since we need to return to Telecom
            // now.
            final int defaultPhoneType = mPhoneFactoryProxy.getDefaultPhone().getPhoneType();
            final Connection emergencyConnection = getTelephonyConnection(request, numberToDial,
                    isEmergencyNumber, emergencyHandle, mPhoneFactoryProxy.getDefaultPhone());

            /// M: CC: Return the failed connection directly @{
            if (!(emergencyConnection instanceof TelephonyConnection)) {
                Log.i(this, "onCreateOutgoingConnection, create emergency connection failed");
                return emergencyConnection;
            }
            /// @}

            /// M: CC: Vzw/CTVolte ECC
            MtkTelephonyConnectionServiceUtil.getInstance().setEmergencyNumber(numberToDial);

            if (hasC2kOverImsModem()
                    || MtkTelephonyManagerEx.getDefault().useVzwLogic()
                    || MtkTelephonyManagerEx.getDefault().useATTLogic()) {
                mSwitchPhoneHelper = null;
            } else if (mSwitchPhoneHelper == null) {
                mSwitchPhoneHelper = new SwitchPhoneHelper(this, number);
            }

            /// M: For ECC change feature @{
            if (mSwitchPhoneHelper != null && mSwitchPhoneHelper.needToPrepareForDial()) {
                mSwitchPhoneHelper.prepareForDial(
                        new SwitchPhoneHelper.Callback() {
                            @Override
                            public void onComplete(boolean success) {
                                if (emergencyConnection.getState()
                                        == Connection.STATE_DISCONNECTED) {
                                    Log.i(this, "prepareForDial, connection disconnect");
                                    /// M: CC: to check whether the device has on-going ECC
                                    MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
                                    return;
                                } else if (success) {
                                    Log.i(this, "startTurnOnRadio");
                                    startTurnOnRadio(emergencyConnection, request,
                                            emergencyHandle, numberToDial);
                                } else {
                                    /// M: CC: ECC Retry @{
                                    // Assume only one ECC exists. Don't trigger retry
                                    // since MD fails to power on should be a bug.
                                    if (MtkTelephonyConnectionServiceUtil.getInstance()
                                            .isEccRetryOn()) {
                                        Log.i(this, "ECC retry: clear ECC param");
                                        MtkTelephonyConnectionServiceUtil.getInstance()
                                                .clearEccRetryParams();
                                    }
                                    /// @}
                                    Log.i(this, "prepareForDial, failed to turn on radio");
                                    emergencyConnection.setDisconnected(
                                            DisconnectCauseUtil.toTelecomDisconnectCause(
                                            android.telephony.DisconnectCause.POWER_OFF,
                                            "Failed to turn on radio."));
                                    /// M: CC: to check whether the device has on-going ECC
                                    MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
                                    emergencyConnection.destroy();
                                }
                            }
                        });
                // Return the still unconnected GsmConnection and wait for the Radios to boot before
                // connecting it to the underlying Phone.
                return emergencyConnection;
            }
            /// @}

            /// M: ECC special handle, select phone by ECC rule @{
            final Phone defaultPhone = getPhoneForAccount(request.getAccountHandle(),
                    isEmergencyNumber);
            Phone phone = MtkTelephonyConnectionServiceUtil.getInstance()
                    .selectPhoneBySpecialEccRule(request.getAccountHandle(),
                    numberToDial, defaultPhone);
            /// @}

            // Radio maybe on even airplane mode on
            boolean isAirplaneModeOn = false;
            if (Settings.Global.getInt(this.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) > 0) {
                isAirplaneModeOn = true;
            }

            /**
             * Use EmergencyCallHelper when phone is in any case below:
             * 1. in airplane mode.
             * 2. phone is radio off.
             * 3. phone is not in service and the emergency only is false.
             */
            if (isAirplaneModeOn || !phone.isRadioOn()
                    || (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA
                            && phone.getServiceState().getState() != ServiceState.STATE_IN_SERVICE
                            && !phone.getServiceState().isEmergencyOnly())) {
                if (mEmergencyCallHelper == null) {
                    mEmergencyCallHelper = new EmergencyCallHelper(this);
                }
                /// M: Self activation. @{
                if (phone instanceof MtkGsmCdmaPhone) {
                    if (((MtkGsmCdmaPhone)phone).shouldProcessSelfActivation()) {
                        notifyEccToSelfActivationSM((MtkGsmCdmaPhone)phone);
                    }
                }
                /// @}
                /// M: CC: ALPS03721910 - ECC by ATD command would need to wait for IN_SERIVCE state. @{
                if (!isDialedByEmergencyCommand) {
                    Log.d(this, "EmergencyCallHelper setEccByNormalPhoneId (phoneId="
                            + phone.getPhoneId() + ")");
                    mEmergencyCallHelper.setEccByNormalPhoneId(phone.getPhoneId());
                } else {
                    mEmergencyCallHelper.resetEccByNormalPhoneId();
                }
                /// @}
                mEmergencyCallHelper.enableEmergencyCalling(new EmergencyCallStateListener.Callback() {
                    @Override
                    public void onComplete(EmergencyCallStateListener listener, boolean isRadioReady) {
                        // Make sure the Call has not already been canceled by the user.
                        if (emergencyConnection.getState() == Connection.STATE_DISCONNECTED) {
                            Log.i(this, "Emergency call disconnected before the outgoing call was " +
                                    "placed. Skipping emergency call placement.");
                            /// M: CC: to check whether the device has on-going ECC
                            MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
                            return;
                        }
                        if (isRadioReady) {
                            ///M: MTK ECC choose phone rule is different @{
                            // Get the right phone object since the radio has been turned on
                            // successfully.
                            //final Phone phone = getPhoneForAccount(request.getAccountHandle(),
                            //        isEmergencyNumber);
                            Phone newDefaultPhone = getPhoneForAccount(request.getAccountHandle(),
                                    isEmergencyNumber);
                            /// @}

                            /// M: CC: Vzw/CTVolte ECC @{

                            Phone newPhone = MtkTelephonyConnectionServiceUtil.getInstance()
                                    .selectPhoneBySpecialEccRule(request.getAccountHandle(),
                                    numberToDial, newDefaultPhone);
                            Log.i(this, "Select phone after using EmergencyCallHelper again"
                                    + ", orig phone=" + phone + " , new phone=" + newPhone);

                            /// M: CC: ECC Retry @{
                            if ((!MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) &&
                                 isDialedByEmergencyCommand) {
                                Log.d(this, "ECC Retry : set param with Intial ECC.");
                                MtkTelephonyConnectionServiceUtil.getInstance().setEccRetryParams(
                                        request, newPhone.getPhoneId());
                            }
                            /// @}
                            /// M: CC: TDD data only @{
                            if (MtkTelephonyConnectionServiceUtil.getInstance().
                                    isDataOnlyMode(newPhone)) {
                                Log.i(this, "enableEmergencyCalling, phoneId=" + newPhone.getPhoneId()
                                        + " is in TDD data only mode.");
                                /// M: CC: ECC Retry @{
                                // Assume only one ECC exists
                                if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                                    Log.i(this, "ECC retry: clear ECC param");
                                    MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
                                }
                                /// @}
                                emergencyConnection.setDisconnected(
                                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                        android.telephony.DisconnectCause.OUTGOING_CANCELED, null));
                                emergencyConnection.destroy();
                                /// M: CC: to check whether the device has on-going ECC
                                MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
                                return;
                            }
                            /// @}

                            /// M: CC: Vzw/CTVolte ECC @{
                            // Timing to set ECM:
                            // - Under flight mode (set in TeleService, use EFUN channel):
                            //    - DSDS: after Radio on and phone is selected
                            if (TelephonyManager.getDefault().getPhoneCount() > 1) {
                                MtkTelephonyConnectionServiceUtil.getInstance()
                                        .enterEmergencyMode(newPhone, 1/*airplane*/);
                            }
                            /// @}

                            // If the PhoneType of the Phone being used is different than the Default
                            // Phone, then we need create a new Connection using that PhoneType and
                            // replace it in Telecom.
                            if (newPhone.getPhoneType() != defaultPhoneType) {
                                Connection repConnection = getTelephonyConnection(request, numberToDial,
                                        isEmergencyNumber, emergencyHandle, newPhone);
                                /// M: Modify the follow to handle the no sound issue. @{
                                // 1. Add the new connection into Telecom;
                                // 2. Disconnect the old connection;
                                // 3. Place the new connection.
                                if (repConnection instanceof TelephonyConnection) {
                                    addExistingConnection(PhoneUtils.makePstnPhoneAccountHandle(newPhone),
                                            repConnection);
                                    //M: Reset emergency call flag for destroying old connection.
                                    resetTreatAsEmergencyCall(emergencyConnection);
                                    // Remove the old connection from Telecom after.
                                    emergencyConnection.setDisconnected(
                                            DisconnectCauseUtil.toTelecomDisconnectCause(
                                                    android.telephony.DisconnectCause.OUTGOING_CANCELED,
                                                    "Reconnecting outgoing Emergency Call."));
                                } else {
                                    /// M: CC: ECC Retry @{
                                    // Assume only one ECC exists
                                    if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                                        Log.i(this, "ECC retry: clear ECC param");
                                        MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
                                    }
                                    emergencyConnection.setDisconnected(repConnection.getDisconnectCause());
                                    /// M: CC: to check whether the device has on-going ECC
                                    MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
                                }
                                emergencyConnection.destroy();
                                /// @}

                                // If there was a failure, the resulting connection will not be a
                                // TelephonyConnection, so don't place the call, just return!
                                if (repConnection instanceof TelephonyConnection) {
                                    placeOutgoingConnection((TelephonyConnection) repConnection,
                                            newPhone, request);
                                }
                            } else {
                                placeOutgoingConnection((TelephonyConnection) emergencyConnection,
                                        newPhone, request);
                            }
                        } else {
                            /// M: CC: ECC Retry @{
                            // Assume only one ECC exists. Don't trigger retry
                            // since Modem fails to power on should be a bug
                            if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                                Log.i(this, "ECC retry: clear ECC param");
                                MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
                            }
                            /// @}

                            Log.w(this, "onCreateOutgoingConnection, failed to turn on radio");
                            emergencyConnection.setDisconnected(
                                    DisconnectCauseUtil.toTelecomDisconnectCause(
                                            android.telephony.DisconnectCause.POWER_OFF,
                                            "Failed to turn on radio."));
                            /// M: CC: to check whether the device has on-going ECC
                            MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
                            emergencyConnection.destroy();
                        }
                    }
                });
            } else {
                /// M: CC: Vzw/CTVolte ECC @{
                // Timing to set ECM:
                // - Not Under flight mode:
                //    - 91-legacy: set in TeleService, use EFUN channel
                //    - 93: set in RILD , use ATD channel
                if (!hasC2kOverImsModem() &&
                        (MtkTelephonyManagerEx.getDefault().useVzwLogic() ||
                        MtkTelephonyManagerEx.getDefault().useATTLogic())) {
                    MtkTelephonyConnectionServiceUtil.getInstance()
                            .enterEmergencyMode(phone, 0/*airplane*/);
                }
                /// @}

                /// M: CC: ECC Retry @{
                if (!MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn() &&
                        isDialedByEmergencyCommand) {
                    Log.i(this, "ECC retry: set param with Intial ECC.");
                    MtkTelephonyConnectionServiceUtil.getInstance().setEccRetryParams(
                            request,
                            phone.getPhoneId());
                }
                /// @}

                // If the PhoneType of the Phone being used is different than the Default
                // Phone, then we need create a new Connection using that PhoneType and
                // replace it in Telecom.
                if (phone.getPhoneType() != defaultPhoneType) {
                    Connection repConnection = getTelephonyConnection(request, numberToDial,
                            isEmergencyNumber, emergencyHandle, phone);
                    // If there was a failure, the resulting connection will not be a
                    // TelephonyConnection, so don't place the call, just return!
                    if (repConnection instanceof TelephonyConnection) {
                        // M: CC: avoid redundant emergency number checking
                        placeOutgoingConnection((TelephonyConnection) repConnection, phone,
                                request, isEmergencyNumber);
                    /// M: CC: ECC Retry @{
                    } else if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                        // Assume only one ECC exists
                        Log.i(this, "ECC retry: clear ECC param");
                        MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
                        /// M: CC: to check whether the device has on-going ECC
                        MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
                    }

                    /// M: Reset the emergency call flag for destroying old connection.
                    resetTreatAsEmergencyCall(emergencyConnection);

                    // Remove the old connection from Telecom after.
                    emergencyConnection.setDisconnected(
                            DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.OUTGOING_CANCELED,
                            "Reconnecting outgoing Emergency Call."));
                    emergencyConnection.destroy();

                    /// M: Return the new connection to Telecom directly.
                    return repConnection;
                } else {
                    // M: CC: avoid redundant emergency number checking
                    placeOutgoingConnection((TelephonyConnection) emergencyConnection,
                            phone, request, isEmergencyNumber);
                }
            }
            // Return the still unconnected GsmConnection and wait for the Radios to boot before
            // connecting it to the underlying Phone.
            return emergencyConnection;
        } else {
            if (!canAddCall() && !isEmergencyNumber) {
                Log.d(this, "onCreateOutgoingConnection, cannot add call .");
                return Connection.createFailedConnection(
                        new DisconnectCause(DisconnectCause.ERROR,
                                getApplicationContext().getText(
                                        R.string.incall_error_cannot_add_call),
                                getApplicationContext().getText(
                                        R.string.incall_error_cannot_add_call),
                                "Add call restricted due to ongoing video call"));
            }

            // Get the right phone object from the account data passed in.
            final Phone phone = getPhoneForAccount(request.getAccountHandle(), isEmergencyNumber);
            Connection resultConnection = getTelephonyConnection(request, numberToDial,
                    isEmergencyNumber, handle, phone);
            // If there was a failure, the resulting connection will not be a TelephonyConnection,
            // so don't place the call!
            if (resultConnection instanceof TelephonyConnection) {
                // M: CC: avoid redundant emergency number checking
                placeOutgoingConnection((TelephonyConnection) resultConnection,
                        phone, request, isEmergencyNumber);
            }
            return resultConnection;
        }
    }

    /**
     * @return {@code true} if any other call is disabling the ability to add calls, {@code false}
     *      otherwise.
     */
    private boolean canAddCall() {
        Collection<Connection> connections = getAllConnections();
        for (Connection connection : connections) {
            if (connection.getExtras() != null &&
                    connection.getExtras().getBoolean(Connection.EXTRA_DISABLE_ADD_CALL, false)) {
                return false;
            }
        }
        return true;
    }

    protected Connection getTelephonyConnection(final ConnectionRequest request,
            final String number, boolean isEmergencyNumber, final Uri handle, Phone phone) {

        if (phone == null) {
            final Context context = getApplicationContext();
            if (context.getResources().getBoolean(R.bool.config_checkSimStateBeforeOutgoingCall)) {
                // Check SIM card state before the outgoing call.
                // Start the SIM unlock activity if PIN_REQUIRED.
                final Phone defaultPhone = mPhoneFactoryProxy.getDefaultPhone();
                final IccCard icc = defaultPhone.getIccCard();
                IccCardConstants.State simState = IccCardConstants.State.UNKNOWN;
                if (icc != null) {
                    simState = icc.getState();
                }
                if (simState == IccCardConstants.State.PIN_REQUIRED) {
                    final String simUnlockUiPackage = context.getResources().getString(
                            R.string.config_simUnlockUiPackage);
                    final String simUnlockUiClass = context.getResources().getString(
                            R.string.config_simUnlockUiClass);
                    if (simUnlockUiPackage != null && simUnlockUiClass != null) {
                        Intent simUnlockIntent = new Intent().setComponent(new ComponentName(
                                simUnlockUiPackage, simUnlockUiClass));
                        simUnlockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            context.startActivity(simUnlockIntent);
                        } catch (ActivityNotFoundException exception) {
                            Log.e(this, exception, "Unable to find SIM unlock UI activity.");
                        }
                    }
                    return Connection.createFailedConnection(
                            DisconnectCauseUtil.toTelecomDisconnectCause(
                                    android.telephony.DisconnectCause.OUT_OF_SERVICE,
                                    "SIM_STATE_PIN_REQUIRED"));
                }
            }

            Log.d(this, "onCreateOutgoingConnection, phone is null");
            /// M: CC: Error message due to CellConnMgr checking @{
            log("onCreateOutgoingConnection, use default phone for cellConnMgr");
            if (MtkTelephonyConnectionServiceUtil.getInstance().
                    cellConnMgrShowAlerting(PhoneFactory.getDefaultPhone().getSubId())) {
                log("onCreateOutgoingConnection, cellConnMgrShowAlerting() check fail");
                return Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                mediatek.telephony.MtkDisconnectCause.OUTGOING_CANCELED_BY_SERVICE,
                                "cellConnMgrShowAlerting() check fail"));
            }
            /// @}
            return Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.OUT_OF_SERVICE, "Phone is null"));
        }

        /// M: CC: Timing issue, radio maybe on even airplane mode on @{
        boolean isAirplaneModeOn = false;
        if (Settings.Global.getInt(phone.getContext().getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) > 0) {
            isAirplaneModeOn = true;
        }
        /// @}

        /// M: CC: TDD data only @{
        if (!isAirplaneModeOn
                && MtkTelephonyConnectionServiceUtil.getInstance().isDataOnlyMode(phone)) {
            /// M: CC: ECC Retry @{
            // Assume only one ECC exists. Don't trigger retry
            // since Modem fails to power on should be a bug
            if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                Log.i(this, "4G data only, ECC retry: clear ECC param");
                MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
            }
            /// @}
            /// M: CC: to check whether the device has on-going ECC
            MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);

            Log.d(this, "getTelephonyConnection, phoneId=" + phone.getPhoneId()
                    + " is in TDD data only mode.");
            return Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                    android.telephony.DisconnectCause.OUTGOING_CANCELED, null));
        }
        /// @}

        // Check both voice & data RAT to enable normal CS call,
        // when voice RAT is OOS but Data RAT is present.
        int state = phone.getServiceState().getState();
        if (state == ServiceState.STATE_OUT_OF_SERVICE) {
            int dataNetType = phone.getServiceState().getDataNetworkType();
            if (dataNetType == TelephonyManager.NETWORK_TYPE_LTE ||
                    dataNetType == TelephonyManager.NETWORK_TYPE_LTE_CA) {
                state = phone.getServiceState().getDataRegState();
            }
        }

        /// M : WFC <TO make MO call when WFC is on and radio is off> @{
        boolean isWfcEnabled = phone.isWifiCallingEnabled();
        log("WFC: phoneId: " + phone.getPhoneId() + " isWfcEnabled: " + isWfcEnabled
                + " isRadioOn: " + phone.isRadioOn());
        if (!phone.isRadioOn() && isWfcEnabled) {
            state = ServiceState.STATE_IN_SERVICE;
        }
        Log.d(this, "Service state:" + state + ", isAirplaneModeOn:" + isAirplaneModeOn);
        /// @}

        // If we're dialing a non-emergency number and the phone is in ECM mode, reject the call if
        // carrier configuration specifies that we cannot make non-emergency calls in ECM mode.
        if (!isEmergencyNumber && phone.isInEcm()) {
            boolean allowNonEmergencyCalls = true;
            CarrierConfigManager cfgManager = (CarrierConfigManager)
                    phone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
            if (cfgManager != null) {
                allowNonEmergencyCalls = cfgManager.getConfigForSubId(phone.getSubId())
                        .getBoolean(CarrierConfigManager.KEY_ALLOW_NON_EMERGENCY_CALLS_IN_ECM_BOOL);
            }

            if (!allowNonEmergencyCalls) {
                return Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                android.telephony.DisconnectCause.CDMA_NOT_EMERGENCY,
                                "Cannot make non-emergency call in ECM mode."
                        ));
            }
        }

        if (!isEmergencyNumber) {

            /// M: SS: Error message due to VoLTE SS checking @{
            if (MtkTelephonyConnectionServiceUtil.getInstance().
                    shouldOpenDataConnection(number, phone)) {
                log("onCreateOutgoingConnection, shouldOpenDataConnection() check fail");
                return Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                mediatek.telephony.MtkDisconnectCause.VOLTE_SS_DATA_OFF,
                                MtkTelecomManager.DISCONNECT_REASON_VOLTE_SS_DATA_OFF));
            }
            /// @}

            /// M: CC: Error message due to CellConnMgr checking @{
            if (isAirplaneModeOn && phone instanceof MtkGsmCdmaPhone &&
                    ((MtkGsmCdmaPhone)phone).shouldProcessSelfActivation()) {
                Log.d(this, "[Self-activation] Bypass Dial in flightmode.");
            } else if (MtkTelephonyConnectionServiceUtil.getInstance().
                    cellConnMgrShowAlerting(phone.getSubId())) {
                log("onCreateOutgoingConnection, cellConnMgrShowAlerting() check fail");
                return Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                mediatek.telephony.MtkDisconnectCause.OUTGOING_CANCELED_BY_SERVICE,
                                "cellConnMgrShowAlerting() check fail"));
            }
            /// @}

            switch (state) {
                case ServiceState.STATE_IN_SERVICE:
                case ServiceState.STATE_EMERGENCY_ONLY:
                    break;
                case ServiceState.STATE_OUT_OF_SERVICE:
                    if (phone.isUtEnabled() && number.endsWith("#")) {
                        Log.d(this, "onCreateOutgoingConnection dial for UT");
                        break;
                    } else {
                        /// M: CC: FTA requires call should be dialed out even out of service @{
                        if (SystemProperties.getInt("gsm.gcf.testmode", 0) == 2) {
                            break;
                        }
                        /// @}

                        /// M: VzW Cdmaless. @{
                        if (phone instanceof MtkGsmCdmaPhone &&
                                ((MtkGsmCdmaPhone)phone).isCdmaLessDevice()) {
                            Log.d(this, "onCreateOutgoingConnection dial even OOS");
                            break;
                        }
                        /// @}

                        return Connection.createFailedConnection(
                                DisconnectCauseUtil.toTelecomDisconnectCause(
                                        android.telephony.DisconnectCause.OUT_OF_SERVICE,
                                        "ServiceState.STATE_OUT_OF_SERVICE"));
                    }
                case ServiceState.STATE_POWER_OFF:
                    if (phone instanceof MtkGsmCdmaPhone) {
                        if (((MtkGsmCdmaPhone)phone).shouldProcessSelfActivation()) {
                            Log.d(this, "POWER_OF and need to do self activation");
                            break;
                        }
                    }
                    return Connection.createFailedConnection(
                            DisconnectCauseUtil.toTelecomDisconnectCause(
                                    android.telephony.DisconnectCause.POWER_OFF,
                                    "ServiceState.STATE_POWER_OFF"));
                default:
                    Log.d(this, "onCreateOutgoingConnection, unknown service state: %d", state);
                    return Connection.createFailedConnection(
                            DisconnectCauseUtil.toTelecomDisconnectCause(
                                    android.telephony.DisconnectCause.OUTGOING_FAILURE,
                                    "Unknown service state " + state));
            }

            /// M: CC: TelephonyConnectionService canDial check @{
            if (!canDial(request.getAccountHandle(), number)) {
                log("onCreateOutgoingConnection, canDial() check fail");
                return Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                android.telephony.DisconnectCause.OUTGOING_FAILURE,
                                "canDial() check fail"));
            }
            /// @}
         }

        final Context context = getApplicationContext();
        if (VideoProfile.isVideo(request.getVideoState()) && isTtyModeEnabled(context) &&
                !isEmergencyNumber) {
            return Connection.createFailedConnection(DisconnectCauseUtil.toTelecomDisconnectCause(
                    android.telephony.DisconnectCause.VIDEO_CALL_NOT_ALLOWED_WHILE_TTY_ENABLED));
        }

        // Check for additional limits on CDMA phones.
        final Connection failedConnection = checkAdditionalOutgoingCallLimits(phone);
        if (failedConnection != null) {
            return failedConnection;
        }

        // Check roaming status to see if we should block custom call forwarding codes
        if (blockCallForwardingNumberWhileRoaming(phone, number)) {
            return Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.DIALED_CALL_FORWARDING_WHILE_ROAMING,
                            "Call forwarding while roaming"));
        }


        final TelephonyConnection connection =
                createConnectionFor(phone, null, true /* isOutgoing */, request.getAccountHandle(),
                        request.getTelecomCallId(), request.getAddress(), request.getVideoState());
        if (connection == null) {
            /// M: CC: ECC retry @{
            // Not trigger retry since connection is null should be a bug
            // Assume only one ECC exists
            if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                Log.i(this, "Fail to create connection, ECC retry: clear ECC param");
                MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
            }
            /// @}
            /// M: CC: to check whether the device has on-going ECC
            MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);

            return Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.OUTGOING_FAILURE,
                            "Invalid phone type"));
        }

        /// M: CC: ECC Retry @{
        if (connection instanceof TelephonyConnection) {
            ((TelephonyConnection) connection).setEmergencyCall(isEmergencyNumber);
        }
        /// @}

        /// M: CC: Set PhoneAccountHandle for ECC @{
        //[ALPS01794357]
        if (isEmergencyNumber) {
            final PhoneAccountHandle phoneAccountHandle;
            /// M: CC: Get iccid from system property @{
            // when IccRecords is null, (updated as RILD is reinitialized).
            // [ALPS02312211] [ALPS02325107]
            String phoneIccId = phone.getFullIccSerialNumber();
            int slotId = SubscriptionController.getInstance().getSlotIndex(phone.getSubId());
            if (slotId != SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
                phoneIccId = !TextUtils.isEmpty(phoneIccId) ? phoneIccId
                        : TelephonyManager.getDefault().getSimSerialNumber(phone.getSubId());
            }
            /// @}
            if (TextUtils.isEmpty(phoneIccId)) {
                // If No SIM is inserted, the corresponding IccId will be null,
                // take phoneId as PhoneAccountHandle::mId which is IccId originally
                phoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(
                        Integer.toString(phone.getPhoneId()));
            } else {
                phoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(phoneIccId);
            }
            log("ECC PhoneAccountHandle mId: " + phoneAccountHandle.getId() +
                    ", iccId: " + phoneIccId);
            connection.setAccountHandle(phoneAccountHandle);
        }
        /// @}

        connection.setAddress(handle, PhoneConstants.PRESENTATION_ALLOWED);
        connection.setInitializing();
        connection.setVideoState(request.getVideoState());

        return connection;
    }

    @Override
    public Connection onCreateIncomingConnection(
            PhoneAccountHandle connectionManagerPhoneAccount,
            ConnectionRequest request) {
        Log.i(this, "onCreateIncomingConnection, request: " + request);
        // If there is an incoming emergency CDMA Call (while the phone is in ECBM w/ No SIM),
        // make sure the PhoneAccount lookup retrieves the default Emergency Phone.
        PhoneAccountHandle accountHandle = request.getAccountHandle();
        boolean isEmergency = false;
        if (accountHandle != null && PhoneUtils.EMERGENCY_ACCOUNT_HANDLE_ID.equals(
                accountHandle.getId())) {
            Log.i(this, "Emergency PhoneAccountHandle is being used for incoming call... " +
                    "Treat as an Emergency Call.");
            isEmergency = true;
        }
        Phone phone = getPhoneForAccount(accountHandle, isEmergency);
        if (phone == null) {
            return Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.ERROR_UNSPECIFIED,
                            "Phone is null"));
        }

        Call call = phone.getRingingCall();
        if (!call.getState().isRinging()) {
            Log.i(this, "onCreateIncomingConnection, no ringing call");
            return Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.INCOMING_MISSED,
                            "Found no ringing call"));
        }

        com.android.internal.telephony.Connection originalConnection =
                call.getState() == Call.State.WAITING ?
                    call.getLatestConnection() : call.getEarliestConnection();
        if (isOriginalConnectionKnown(originalConnection)) {
            Log.i(this, "onCreateIncomingConnection, original connection already registered");
            return Connection.createCanceledConnection();
        }

        // We should rely on the originalConnection to get the video state.  The request coming
        // from Telecom does not know the video state of the incoming call.
        int videoState = originalConnection != null ? originalConnection.getVideoState() :
                VideoProfile.STATE_AUDIO_ONLY;

        Connection connection =
                createConnectionFor(phone, originalConnection, false /* isOutgoing */,
                        request.getAccountHandle(), request.getTelecomCallId(),
                        request.getAddress(), videoState);

        //For RTT feature
        ExtensionManager.getRttUtilExt().setupRttTextStream(connection, request);

        if (connection == null) {
            return Connection.createCanceledConnection();
        } else {
            return connection;
        }
    }

    /**
     * Called by the {@link ConnectionService} when a newly created {@link Connection} has been
     * added to the {@link ConnectionService} and sent to Telecom.  Here it is safe to send
     * connection events.
     *
     * @param connection the {@link Connection}.
     */
    @Override
    public void onCreateConnectionComplete(Connection connection) {
        if (connection instanceof TelephonyConnection) {
            TelephonyConnection telephonyConnection = (TelephonyConnection) connection;
            maybeSendInternationalCallEvent(telephonyConnection);
        }
    }

    @Override
    public void triggerConferenceRecalculate() {
        if (mTelephonyConferenceController.shouldRecalculate()) {
            mTelephonyConferenceController.recalculate();
        }
    }

    @Override
    public Connection onCreateUnknownConnection(PhoneAccountHandle connectionManagerPhoneAccount,
            ConnectionRequest request) {
        Log.i(this, "onCreateUnknownConnection, request: " + request);
        // Use the registered emergency Phone if the PhoneAccountHandle is set to Telephony's
        // Emergency PhoneAccount
        PhoneAccountHandle accountHandle = request.getAccountHandle();
        boolean isEmergency = false;
        if (accountHandle != null && PhoneUtils.EMERGENCY_ACCOUNT_HANDLE_ID.equals(
                accountHandle.getId())) {
            Log.i(this, "Emergency PhoneAccountHandle is being used for unknown call... " +
                    "Treat as an Emergency Call.");
            isEmergency = true;
        }
        Phone phone = getPhoneForAccount(accountHandle, isEmergency);
        if (phone == null) {
            return Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.ERROR_UNSPECIFIED,
                            "Phone is null"));
        }
        Bundle extras = request.getExtras();

        final List<com.android.internal.telephony.Connection> allConnections = new ArrayList<>();

        // Handle the case where an unknown connection has an IMS external call ID specified; we can
        // skip the rest of the guesswork and just grad that unknown call now.
        if (phone.getImsPhone() != null && extras != null &&
                extras.containsKey(ImsExternalCallTracker.EXTRA_IMS_EXTERNAL_CALL_ID)) {

            ImsPhone imsPhone = (ImsPhone) phone.getImsPhone();
            ImsExternalCallTracker externalCallTracker = imsPhone.getExternalCallTracker();
            int externalCallId = extras.getInt(ImsExternalCallTracker.EXTRA_IMS_EXTERNAL_CALL_ID,
                    -1);

            if (externalCallTracker != null) {
                com.android.internal.telephony.Connection connection =
                        externalCallTracker.getConnectionById(externalCallId);

                if (connection != null) {
                    allConnections.add(connection);
                }
            }
        }

        if (allConnections.isEmpty()) {
            final Call ringingCall = phone.getRingingCall();
            if (ringingCall.hasConnections()) {
                allConnections.addAll(ringingCall.getConnections());
            }
            final Call foregroundCall = phone.getForegroundCall();
            if ((foregroundCall.getState() != Call.State.DISCONNECTED)
                    && (foregroundCall.hasConnections())) {
                allConnections.addAll(foregroundCall.getConnections());
            }
            if (phone.getImsPhone() != null) {
                final Call imsFgCall = phone.getImsPhone().getForegroundCall();
                if ((imsFgCall.getState() != Call.State.DISCONNECTED) && imsFgCall
                        .hasConnections()) {
                    allConnections.addAll(imsFgCall.getConnections());
                }
            }
            final Call backgroundCall = phone.getBackgroundCall();
            if (backgroundCall.hasConnections()) {
                allConnections.addAll(phone.getBackgroundCall().getConnections());
            }
        }

        com.android.internal.telephony.Connection unknownConnection = null;
        for (com.android.internal.telephony.Connection telephonyConnection : allConnections) {
            if (!isOriginalConnectionKnown(telephonyConnection)) {
                unknownConnection = telephonyConnection;
                Log.d(this, "onCreateUnknownConnection: conn = " + unknownConnection);
                break;
            }
        }

        if (unknownConnection == null) {
            Log.i(this, "onCreateUnknownConnection, did not find previously unknown connection.");
            return Connection.createCanceledConnection();
        }

        // We should rely on the originalConnection to get the video state.  The request coming
        // from Telecom does not know the video state of the unknown call.
        int videoState = unknownConnection != null ? unknownConnection.getVideoState() :
                VideoProfile.STATE_AUDIO_ONLY;

        TelephonyConnection connection =
                createConnectionFor(phone, unknownConnection,
                        !unknownConnection.isIncoming() /* isOutgoing */,
                        request.getAccountHandle(), request.getTelecomCallId(),
                        request.getAddress(), videoState);

        if (connection == null) {
            return Connection.createCanceledConnection();
        } else {
            connection.updateState();
            return connection;
        }
    }

    /**
     * Conferences two connections.
     *
     * Note: The {@link android.telecom.RemoteConnection#setConferenceableConnections(List)} API has
     * a limitation in that it can only specify conferenceables which are instances of
     * {@link android.telecom.RemoteConnection}.  In the case of an {@link ImsConference}, the
     * regular {@link Connection#setConferenceables(List)} API properly handles being able to merge
     * a {@link Conference} and a {@link Connection}.  As a result when, merging a
     * {@link android.telecom.RemoteConnection} into a {@link android.telecom.RemoteConference}
     * require merging a {@link ConferenceParticipantConnection} which is a child of the
     * {@link Conference} with a {@link TelephonyConnection}.  The
     * {@link ConferenceParticipantConnection} class does not have the capability to initiate a
     * conference merge, so we need to call
     * {@link TelephonyConnection#performConference(Connection)} on either {@code connection1} or
     * {@code connection2}, one of which is an instance of {@link TelephonyConnection}.
     *
     * @param connection1 A connection to merge into a conference call.
     * @param connection2 A connection to merge into a conference call.
     */
    @Override
    public void onConference(Connection connection1, Connection connection2) {
        if (connection1 instanceof TelephonyConnection) {
            ((TelephonyConnection) connection1).performConference(connection2);
        } else if (connection2 instanceof TelephonyConnection) {
            ((TelephonyConnection) connection2).performConference(connection1);
        } else {
            Log.w(this, "onConference - cannot merge connections " +
                    "Connection1: %s, Connection2: %2", connection1, connection2);
        }
    }

    protected boolean blockCallForwardingNumberWhileRoaming(Phone phone, String number) {
        if (phone == null || TextUtils.isEmpty(number) || !phone.getServiceState().getRoaming()) {
            return false;
        }
        String[] blockPrefixes = null;
        CarrierConfigManager cfgManager = (CarrierConfigManager)
                phone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (cfgManager != null) {
            blockPrefixes = cfgManager.getConfigForSubId(phone.getSubId()).getStringArray(
                    CarrierConfigManager.KEY_CALL_FORWARDING_BLOCKS_WHILE_ROAMING_STRING_ARRAY);
        }

        if (blockPrefixes != null) {
            for (String prefix : blockPrefixes) {
                if (number.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isRadioOn() {
        boolean result = false;
        for (Phone phone : mPhoneFactoryProxy.getPhones()) {
            result |= phone.isRadioOn();
        }
        return result;
    }

    private Pair<WeakReference<TelephonyConnection>, List<Phone>> makeCachedConnectionPhonePair(
            TelephonyConnection c) {
        List<Phone> phones = new ArrayList<>(Arrays.asList(mPhoneFactoryProxy.getPhones()));
        return new Pair<>(new WeakReference<>(c), phones);
    }

    // Check the mEmergencyRetryCache to see if it contains the TelephonyConnection. If it doesn't,
    // then it is stale. Create a new one!
    private void updateCachedConnectionPhonePair(TelephonyConnection c) {
        if (mEmergencyRetryCache == null) {
            Log.i(this, "updateCachedConnectionPhonePair, cache is null. Generating new cache");
            mEmergencyRetryCache = makeCachedConnectionPhonePair(c);
        } else {
            // Check to see if old cache is stale. If it is, replace it
            WeakReference<TelephonyConnection> cachedConnection = mEmergencyRetryCache.first;
            if (cachedConnection.get() != c) {
                Log.i(this, "updateCachedConnectionPhonePair, cache is stale. Regenerating.");
                mEmergencyRetryCache = makeCachedConnectionPhonePair(c);
            }
        }
    }

    /**
     * Returns the first Phone that has not been used yet to place the call. Any Phones that have
     * been used to place a call will have already been removed from mEmergencyRetryCache.second.
     * The phone that it excluded will be removed from mEmergencyRetryCache.second in this method.
     * @param phoneToExclude The Phone object that will be removed from our cache of available
     * phones.
     * @return the first Phone that is available to be used to retry the call.
     */
    private Phone getPhoneForRedial(Phone phoneToExclude) {
        List<Phone> cachedPhones = mEmergencyRetryCache.second;
        if (cachedPhones.contains(phoneToExclude)) {
            Log.i(this, "getPhoneForRedial, removing Phone[" + phoneToExclude.getPhoneId() +
                    "] from the available Phone cache.");
            cachedPhones.remove(phoneToExclude);
        }
        return cachedPhones.isEmpty() ? null : cachedPhones.get(0);
    }

    private void retryOutgoingOriginalConnection(TelephonyConnection c) {
        updateCachedConnectionPhonePair(c);
        Phone newPhoneToUse = getPhoneForRedial(c.getPhone());
        if (newPhoneToUse != null) {
            int videoState = c.getVideoState();
            Bundle connExtras = c.getExtras();
            Log.i(this, "retryOutgoingOriginalConnection, redialing on Phone Id: " + newPhoneToUse);
            c.clearOriginalConnection();
            placeOutgoingConnection(c, newPhoneToUse, videoState, connExtras);
        } else {
            // We have run out of Phones to use. Disconnect the call and destroy the connection.
            Log.i(this, "retryOutgoingOriginalConnection, no more Phones to use. Disconnecting.");
            c.setDisconnected(new DisconnectCause(DisconnectCause.ERROR));
            c.clearOriginalConnection();
            c.destroy();
        }
    }

    /// M: CC: add placeOutgoingConnection() with isEmergencyNumber parameter
    // to avoid redundant emergency number checking.
    private void placeOutgoingConnection(
            TelephonyConnection connection, Phone phone, ConnectionRequest request) {
        String number = connection.getAddress().getSchemeSpecificPart();
        boolean isEmergencyNumber = PhoneNumberUtils.isLocalEmergencyNumber(this, number);
        placeOutgoingConnection(connection, phone, request, isEmergencyNumber);
    }

    private void placeOutgoingConnection(
            TelephonyConnection connection, Phone phone, ConnectionRequest request,
            boolean isEmergencyNumber) {

        placeOutgoingConnection(connection, phone, request.getVideoState(), request.getExtras(),
                isEmergencyNumber);

        ///M: add RTT feature
        ExtensionManager.getRttUtilExt().setupOutgoingConnectionForRtt(connection, request);
    }

    protected void placeOutgoingConnection(
            TelephonyConnection connection, Phone phone, int videoState, Bundle extras) {
        String number = connection.getAddress().getSchemeSpecificPart();
        boolean isEmergencyNumber = PhoneNumberUtils.isLocalEmergencyNumber(this, number);
        placeOutgoingConnection(connection, phone, videoState, extras, isEmergencyNumber);
    }

    protected void placeOutgoingConnection(
            TelephonyConnection connection, Phone phone, int videoState, Bundle extras,
            boolean isEmergencyNumber) {
        String number = connection.getAddress().getSchemeSpecificPart();

        /// M: CC: Set PhoneAccountHandle for ECC @{
        //[ALPS01794357]
        if (isEmergencyNumber) {
            final PhoneAccountHandle phoneAccountHandle;
            String phoneIccId = phone.getFullIccSerialNumber();
            int slotId = SubscriptionController.getInstance().getSlotIndex(phone.getSubId());
            if (slotId != SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
                phoneIccId = !TextUtils.isEmpty(phoneIccId) ? phoneIccId
                        : TelephonyManager.getDefault().getSimSerialNumber(phone.getSubId());
            }
            if (TextUtils.isEmpty(phoneIccId)) {
                // If No SIM is inserted, the corresponding IccId will be null,
                // take phoneId as PhoneAccountHandle::mId which is IccId originally
                phoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(
                        Integer.toString(phone.getPhoneId()));
            } else {
                phoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(phoneIccId);
            }
            log("placeOutgoingConnection, set back account mId: " + phoneAccountHandle.getId() +
                    ", iccId: " + phoneIccId);
            connection.setAccountHandle(phoneAccountHandle);
            // Need to set current ECC phone type which will be used when retry to check if
            // need to switch phone or not.
            MtkTelephonyConnectionServiceUtil.getInstance().setEccPhoneType(phone.getPhoneType());
        }
        /// @}

        com.android.internal.telephony.Connection originalConnection = null;
        try {
            if (phone != null) {
                /// M: put MO from-line-number for Digits @{
                Context context = getApplicationContext();
                extras = ExtensionManager.getDigitsUtilExt().putLineNumberToExtras(extras, context);
                /// @}
                originalConnection = phone.dial(number, null, videoState, extras);
            }
        } catch (CallStateException e) {
            Log.e(this, e, "placeOutgoingConnection, phone.dial exception: " + e);
            int cause = android.telephony.DisconnectCause.OUTGOING_FAILURE;
            if (e.getError() == CallStateException.ERROR_OUT_OF_SERVICE) {
                cause = android.telephony.DisconnectCause.OUT_OF_SERVICE;
            } else if (e.getError() == CallStateException.ERROR_POWER_OFF) {
                cause = android.telephony.DisconnectCause.POWER_OFF;
            }
            /// M: CC: ECC retry @{
            // Assume only one ECC exists
            if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                Log.i(this, "ECC retry: clear ECC param");
                MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
            }
            /// @}
            connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(
                    cause, e.getMessage()));
            /// M: CC: to check whether the device has on-going ECC
            MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
            /// M: CC: Destroy TelephonyConnection if framework fails to dial @{
            connection.destroy();
            /// @}
            return;
        }

        if (originalConnection == null) {
            int telephonyDisconnectCause = android.telephony.DisconnectCause.OUTGOING_FAILURE;
            // On GSM phones, null connection means that we dialed an MMI code
            if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM) {
                Log.d(this, "dialed MMI code");
                int subId = phone.getSubId();
                Log.d(this, "subId: " + subId);
                telephonyDisconnectCause = android.telephony.DisconnectCause.DIALED_MMI;
                final Intent intent = new Intent(this, MMIDialogActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, subId);
                }
                startActivity(intent);
            }
            Log.d(this, "placeOutgoingConnection, phone.dial returned null");
            /// M: CC: ECC retry @{
            // Assume only one ECC exists
            if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                Log.i(this, "ECC retry: clear ECC param");
                MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
            }
            /// @}
            connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(
                    telephonyDisconnectCause, "Connection is null"));
            /// M: CC: to check whether the device has on-going ECC
            MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
            /// M: CC: Destroy TelephonyConnection if framework fails to dial @{
            connection.destroy();
            /// @}
        } else {
            connection.setOriginalConnection(originalConnection);
        }
    }
    /// @}

    /// M: CC: ECC retry @{
    /**
     * createConnection for ECC Retry
     *
     * @param callId The call Id.
     * @param request The connection request.
     */
    public void createConnectionInternal(
            final String callId,
            final ConnectionRequest request) {
        Log.i(this, "createConnectionInternal, callId=" + callId + ", request=" + request);

        Connection connection = onCreateOutgoingConnection(null, request);
        Log.i(this, "createConnectionInternal, connection=", connection);
        if (connection == null) {
            connection = Connection.createFailedConnection(
                    new DisconnectCause(DisconnectCause.ERROR));
        }

        connection.setTelecomCallId(callId);
        if (connection.getState() != Connection.STATE_DISCONNECTED) {
            addConnection(callId, connection);
        }

        Uri address = connection.getAddress();
        String number = address == null ? "null" : address.getSchemeSpecificPart();
        Log.i(this, "createConnectionInternal"
                + ", number=" + Connection.toLogSafePhoneNumber(number)
                + ", state=" + Connection.stateToString(connection.getState())
                + ", capabilities="
                + Connection.capabilitiesToString(connection.getConnectionCapabilities())
                + ", properties="
                + Connection.propertiesToString(connection.getConnectionProperties()));

        Log.i(this, "createConnectionInternal, calling handleCreateConnectionComplete"
                + " for callId=" + callId);
        /// M: CC: Set PhoneAccountHandle for ECC @{
        //[ALPS01794357]
        PhoneAccountHandle handle = null;
        if (connection instanceof TelephonyConnection) {
            handle = ((TelephonyConnection)connection).getAccountHandle();
            // Set account handle to telecom after addConnection (which will add
            // listener) so that telecom can update account. For the ECC replace
            // connection (addExistingConnection then disconnection) case,
            // handleCreateConnectionComplete can't update account handle.
            ((TelephonyConnection)connection).setAccountHandle(handle);
        }
        if (handle == null) {
            handle = request.getAccountHandle();
        } else {
            Log.i(this, "createConnectionInternal, set back phone account=" + handle);
        }
        //// @}
        mAdapter.handleCreateConnectionComplete(
                callId,
                request,
                new ParcelableConnection(
                        handle,  /* M: CC: Set PhoneAccountHandle for ECC [ALPS01794357] */
                        connection.getState(),
                        connection.getConnectionCapabilities(),
                        connection.getConnectionProperties(),
                        connection.getSupportedAudioRoutes(),
                        connection.getAddress(),
                        connection.getAddressPresentation(),
                        connection.getCallerDisplayName(),
                        connection.getCallerDisplayNamePresentation(),
                        connection.getVideoProvider() == null ?
                                null : connection.getVideoProvider().getInterface(),
                        connection.getVideoState(),
                        connection.isRingbackRequested(),
                        connection.getAudioModeIsVoip(),
                        connection.getConnectTimeMillis(),
                        connection.getConnectElapsedTimeMillis(),
                        connection.getStatusHints(),
                        connection.getDisconnectCause(),
                        createIdList(connection.getConferenceables()),
                        connection.getExtras()));
    }

    /**
     * Remove Connection without removing callId from Telecom
     *
     * @param connection The connection.
     * @return String The callId mapped to the removed connection.
     * @hide
     */
    protected String removeConnectionInternal(Connection connection) {
        String id = mIdByConnection.get(connection);
        connection.unsetConnectionService(this);
        connection.removeConnectionListener(mConnectionListener);
        mConnectionById.remove(mIdByConnection.get(connection));
        mIdByConnection.remove(connection);
        Log.i(this, "removeConnectionInternal, callId=" + id + ", connection=" + connection);
        return id;
    }
    /// @}

    protected TelephonyConnection createConnectionFor(
            Phone phone,
            com.android.internal.telephony.Connection originalConnection,
            boolean isOutgoing,
            PhoneAccountHandle phoneAccountHandle,
            String telecomCallId,
            Uri address,
            int videoState) {
        TelephonyConnection returnConnection = null;
        int phoneType = phone.getPhoneType();
        boolean allowsMute = allowsMute(phone);
        returnConnection = new MtkGsmCdmaConnection(phoneType, originalConnection, telecomCallId,
                mEmergencyTonePlayer, allowsMute, isOutgoing);

        if (returnConnection != null) {
            // Listen to Telephony specific callbacks from the connection
            returnConnection.addTelephonyConnectionListener(mTelephonyConnectionListener);
            returnConnection.setVideoPauseSupported(
                    TelecomAccountRegistry.getInstance(this).isVideoPauseSupported(
                            phoneAccountHandle));
        }
        return returnConnection;
    }

    private boolean isOriginalConnectionKnown(
            com.android.internal.telephony.Connection originalConnection) {
        for (Connection connection : getAllConnections()) {
            if (connection instanceof TelephonyConnection) {
                TelephonyConnection telephonyConnection = (TelephonyConnection) connection;
                if (telephonyConnection.getOriginalConnection() == originalConnection) {
                    return true;
                }
            }
        }
        return false;
    }

    protected Phone getPhoneForAccount(PhoneAccountHandle accountHandle, boolean isEmergency) {
        Phone chosenPhone = null;
        int subId = PhoneUtils.getSubIdForPhoneAccountHandle(accountHandle);
        if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            int phoneId = mSubscriptionManagerProxy.getPhoneId(subId);
            chosenPhone = mPhoneFactoryProxy.getPhone(phoneId);
        }
        // If this is an emergency call and the phone we originally planned to make this call
        // with is not in service or was invalid, try to find one that is in service, using the
        // default as a last chance backup.
        if (isEmergency && (chosenPhone == null || ServiceState.STATE_IN_SERVICE != chosenPhone
                .getServiceState().getState())) {
            Log.d(this, "getPhoneForAccount: phone for phone acct handle %s is out of service "
                    + "or invalid for emergency call.", accountHandle);
            chosenPhone = getFirstPhoneForEmergencyCall();
            Log.d(this, "getPhoneForAccount: using subId: " +
                    (chosenPhone == null ? "null" : chosenPhone.getSubId()));
        }
        return chosenPhone;
    }

    /**
     * Retrieves the most sensible Phone to use for an emergency call using the following Priority
     *  list (for multi-SIM devices):
     *  1) The User's SIM preference for Voice calling
     *  2) The First Phone that is currently IN_SERVICE or is available for emergency calling
     *  3) If there is a PUK locked SIM, compare the SIMs that are not PUK locked. If all the SIMs
     *     are locked, skip to condition 4).
     *  4) The Phone with more Capabilities.
     *  5) The First Phone that has a SIM card in it (Starting from Slot 0...N)
     *  6) The Default Phone (Currently set as Slot 0)
     */
    @VisibleForTesting
    public Phone getFirstPhoneForEmergencyCall() {
        // 1)
        int phoneId = mSubscriptionManagerProxy.getDefaultVoicePhoneId();
        if (phoneId != SubscriptionManager.INVALID_PHONE_INDEX) {
            Phone defaultPhone = mPhoneFactoryProxy.getPhone(phoneId);
            if (defaultPhone != null && isAvailableForEmergencyCalls(defaultPhone)) {
                return defaultPhone;
            }
        }

        Phone firstPhoneWithSim = null;
        int phoneCount = mTelephonyManagerProxy.getPhoneCount();
        List<SlotStatus> phoneSlotStatus = new ArrayList<>(phoneCount);
        for (int i = 0; i < phoneCount; i++) {
            Phone phone = mPhoneFactoryProxy.getPhone(i);
            if (phone == null) {
                continue;
            }
            // 2)
            if (isAvailableForEmergencyCalls(phone)) {
                // the slot has the radio on & state is in service.
                Log.i(this, "getFirstPhoneForEmergencyCall, radio on & in service, Phone Id:" + i);
                return phone;
            }
            // 4)
            // Store the RAF Capabilities for sorting later.
            int radioAccessFamily = phone.getRadioAccessFamily();
            SlotStatus status = new SlotStatus(i, radioAccessFamily);
            phoneSlotStatus.add(status);
            Log.i(this, "getFirstPhoneForEmergencyCall, RAF:" +
                    Integer.toHexString(radioAccessFamily) + " saved for Phone Id:" + i);
            // 3)
            // Report Slot's PIN/PUK lock status for sorting later.
            int simState = mSubscriptionManagerProxy.getSimStateForSlotIdx(i);
            if (simState == TelephonyManager.SIM_STATE_PIN_REQUIRED ||
                    simState == TelephonyManager.SIM_STATE_PUK_REQUIRED) {
                status.isLocked = true;
            }
            // 5)
            if (firstPhoneWithSim == null && mTelephonyManagerProxy.hasIccCard(i)) {
                // The slot has a SIM card inserted, but is not in service, so keep track of this
                // Phone. Do not return because we want to make sure that none of the other Phones
                // are in service (because that is always faster).
                firstPhoneWithSim = phone;
                Log.i(this, "getFirstPhoneForEmergencyCall, SIM card inserted, Phone Id:" +
                        firstPhoneWithSim.getPhoneId());
            }
        }
        // 6)
        if (firstPhoneWithSim == null && phoneSlotStatus.isEmpty()) {
            // No Phones available, get the default.
            Log.i(this, "getFirstPhoneForEmergencyCall, return default phone");
            return mPhoneFactoryProxy.getDefaultPhone();
        } else {
            // 4)
            final int defaultPhoneId = mPhoneFactoryProxy.getDefaultPhone().getPhoneId();
            final Phone firstOccupiedSlot = firstPhoneWithSim;
            if (!phoneSlotStatus.isEmpty()) {
                // Only sort if there are enough elements to do so.
                if (phoneSlotStatus.size() > 1) {
                    Collections.sort(phoneSlotStatus, (o1, o2) -> {
                        // First start by seeing if either of the phone slots are locked. If they
                        // are, then sort by non-locked SIM first. If they are both locked, sort
                        // by capability instead.
                        if (o1.isLocked && !o2.isLocked) {
                            return -1;
                        }
                        if (o2.isLocked && !o1.isLocked) {
                            return 1;
                        }
                        // sort by number of RadioAccessFamily Capabilities.
                        int compare = Integer.bitCount(o1.capabilities) -
                                Integer.bitCount(o2.capabilities);
                        if (compare == 0) {
                            // Sort by highest RAF Capability if the number is the same.
                            compare = RadioAccessFamily.getHighestRafCapability(o1.capabilities) -
                                    RadioAccessFamily.getHighestRafCapability(o2.capabilities);
                            if (compare == 0) {
                                if (firstOccupiedSlot != null) {
                                    // If the RAF capability is the same, choose based on whether or
                                    // not any of the slots are occupied with a SIM card (if both
                                    // are, always choose the first).
                                    if (o1.slotId == firstOccupiedSlot.getPhoneId()) {
                                        return 1;
                                    } else if (o2.slotId == firstOccupiedSlot.getPhoneId()) {
                                        return -1;
                                    }
                                } else {
                                    // No slots have SIMs detected in them, so weight the default
                                    // Phone Id greater than the others.
                                    if (o1.slotId == defaultPhoneId) {
                                        return 1;
                                    } else if (o2.slotId == defaultPhoneId) {
                                        return -1;
                                    }
                                }
                            }
                        }
                        return compare;
                    });
                }
                int mostCapablePhoneId = phoneSlotStatus.get(phoneSlotStatus.size() - 1).slotId;
                Log.i(this, "getFirstPhoneForEmergencyCall, Using Phone Id: " + mostCapablePhoneId +
                        "with highest capability");
                return mPhoneFactoryProxy.getPhone(mostCapablePhoneId);
            } else {
                // 5)
                return firstPhoneWithSim;
            }
        }
    }

    /**
     * Returns true if the state of the Phone is IN_SERVICE or available for emergency calling only.
     */
    private boolean isAvailableForEmergencyCalls(Phone phone) {
        return ServiceState.STATE_IN_SERVICE == phone.getServiceState().getState() ||
                phone.getServiceState().isEmergencyOnly();
    }

    /**
     * Determines if the connection should allow mute.
     *
     * @param phone The current phone.
     * @return {@code True} if the connection should allow mute.
     */
    protected boolean allowsMute(Phone phone) {
        // For CDMA phones, check if we are in Emergency Callback Mode (ECM).  Mute is disallowed
        // in ECM mode.
        if (phone.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
            if (phone.isInEcm()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void removeConnection(Connection connection) {
        /// M: CC: ECC retry @{
        //super.removeConnection(connection);
        boolean handleEcc = false;
        if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
            if (connection instanceof TelephonyConnection) {
                if (((TelephonyConnection) connection).shouldTreatAsEmergencyCall()) {
                    handleEcc = true;
                }
            }
        }

        if (handleEcc) {
            Log.i(this, "ECC retry: remove connection.");
            MtkTelephonyConnectionServiceUtil.getInstance().setEccRetryCallId(
                    removeConnectionInternal(connection));
        } else { //Original flow
            super.removeConnection(connection);
        }
        /// @}

        if (connection instanceof TelephonyConnection) {
            TelephonyConnection telephonyConnection = (TelephonyConnection) connection;
            telephonyConnection.removeTelephonyConnectionListener(mTelephonyConnectionListener);
        }
    }

    /**
     * When a {@link TelephonyConnection} has its underlying original connection configured,
     * we need to add it to the correct conference controller.
     *
     * @param connection The connection to be added to the controller
     */
    public void addConnectionToConferenceController(TelephonyConnection connection) {
        int connPhoneType = PhoneConstants.PHONE_TYPE_NONE;
        if (connection instanceof MtkGsmCdmaConnection) {
            connPhoneType = ((MtkGsmCdmaConnection) connection)
                    .getPhoneType();
        }
        // TODO: Need to revisit what happens when the original connection for the
        // TelephonyConnection changes.  If going from CDMA --> GSM (for example), the
        // instance of TelephonyConnection will still be a CdmaConnection, not a GsmConnection.
        // The CDMA conference controller makes the assumption that it will only have CDMA
        // connections in it, while the other conference controllers aren't as restrictive.  Really,
        // when we go between CDMA and GSM we should replace the TelephonyConnection.
        if (connection.isImsConnection()) {
            Log.d(this, "Adding IMS connection to conference controller: " + connection);
            mImsConferenceController.add(connection);
            mTelephonyConferenceController.remove(connection);
            if (connPhoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                mCdmaConferenceController.remove((MtkGsmCdmaConnection) connection);
            }
        } else {
            if (connection.getCall() == null || connection.getCall().getPhone() == null) {
                Log.d(this, "Connection died, no need to add to conference controller");
                return;
            }
            int phoneType = connection.getCall().getPhone().getPhoneType();
            if (phoneType == TelephonyManager.PHONE_TYPE_GSM) {
                Log.d(this, "Adding GSM connection to conference controller: " + connection);
                mTelephonyConferenceController.add(connection);
                if (connPhoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                    mCdmaConferenceController.remove((MtkGsmCdmaConnection) connection);
                }
            } else if (phoneType == TelephonyManager.PHONE_TYPE_CDMA &&
                    connPhoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                Log.d(this, "Adding CDMA connection to conference controller: " + connection);
                mCdmaConferenceController.add((MtkGsmCdmaConnection) connection);
                mTelephonyConferenceController.remove(connection);
            }
            Log.d(this, "Removing connection from IMS conference controller: " + connection);
            mImsConferenceController.remove(connection);
        }
    }

    /**
     * Create a new CDMA connection. CDMA connections have additional limitations when creating
     * additional calls which are handled in this method.  Specifically, CDMA has a "FLASH" command
     * that can be used for three purposes: merging a call, swapping unmerged calls, and adding
     * a new outgoing call. The function of the flash command depends on the context of the current
     * set of calls. This method will prevent an outgoing call from being made if it is not within
     * the right circumstances to support adding a call.
     */
    protected Connection checkAdditionalOutgoingCallLimits(Phone phone) {
        if (phone.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
            // Check to see if any CDMA conference calls exist, and if they do, check them for
            // limitations.
            for (Conference conference : getAllConferences()) {
                if (conference instanceof CdmaConference) {
                    CdmaConference cdmaConf = (CdmaConference) conference;

                    // If the CDMA conference has not been merged, add-call will not work, so fail
                    // this request to add a call.
                    if (cdmaConf.can(Connection.CAPABILITY_MERGE_CONFERENCE)) {
                        return Connection.createFailedConnection(new DisconnectCause(
                                    DisconnectCause.RESTRICTED,
                                    null,
                                    getResources().getString(R.string.callFailed_cdma_call_limit),
                                    "merge-capable call exists, prevent flash command."));
                    }
                }
            }
        }

        return null; // null means nothing went wrong, and call should continue.
    }

    protected boolean isTtyModeEnabled(Context context) {
        return (android.provider.Settings.Secure.getInt(
                context.getContentResolver(),
                android.provider.Settings.Secure.PREFERRED_TTY_MODE,
                TelecomManager.TTY_MODE_OFF) != TelecomManager.TTY_MODE_OFF);
    }

    /**
     * For outgoing dialed calls, potentially send a ConnectionEvent if the user is on WFC and is
     * dialing an international number.
     * @param telephonyConnection The connection.
     */
    private void maybeSendInternationalCallEvent(TelephonyConnection telephonyConnection) {
        if (telephonyConnection == null || telephonyConnection.getPhone() == null ||
                telephonyConnection.getPhone().getDefaultPhone() == null) {
            return;
        }
        Phone phone = telephonyConnection.getPhone().getDefaultPhone();
        if (phone instanceof GsmCdmaPhone) {
            GsmCdmaPhone gsmCdmaPhone = (GsmCdmaPhone) phone;
            if (telephonyConnection.isOutgoingCall() &&
                    gsmCdmaPhone.isNotificationOfWfcCallRequired(
                            telephonyConnection.getOriginalConnection().getOrigDialString())) {
                // Send connection event to InCall UI to inform the user of the fact they
                // are potentially placing an international call on WFC.
                Log.i(this, "placeOutgoingConnection - sending international call on WFC " +
                        "confirmation event");
                telephonyConnection.sendConnectionEvent(
                        TelephonyManager.EVENT_NOTIFY_INTERNATIONAL_CALL_ON_WFC, null);
            }
        }
    }

    /**
     * This can be used by telecom to either create a new outgoing call or attach to an existing
     * incoming call. In either case, telecom will cycle through a set of services and call
     * createConnection util a connection service cancels the process or completes it successfully.
     */
    /** {@hide} */
    protected void createConnection(
            final PhoneAccountHandle callManagerAccount,
            final String callId,
            final ConnectionRequest request,
            boolean isIncoming,
            boolean isUnknown) {
        Log.d(this, "createConnection, callManagerAccount: %s, callId: %s, request: %s, " +
                        "isIncoming: %b, isUnknown: %b", callManagerAccount, callId, request,
                isIncoming,
                isUnknown);

        /// M: CC: createConnection() may be called in post runnable, so there's timing issue that
        // it's invoked after call aborted. @{
        if (!isAdaptersAvailable()) {
            Log.i(this, "createConnection, adapter not available, call should have been aborted");
            return;
        }
        /// @}

        Connection connection = isUnknown ? onCreateUnknownConnection(callManagerAccount, request)
                : isIncoming ? onCreateIncomingConnection(callManagerAccount, request)
                : onCreateOutgoingConnection(callManagerAccount, request);
        Log.d(this, "createConnection, connection: %s", connection);
        if (connection == null) {
            connection = Connection.createFailedConnection(
                    new DisconnectCause(DisconnectCause.ERROR));
        }

        connection.setTelecomCallId(callId);
        if (connection.getState() != Connection.STATE_DISCONNECTED) {
            addConnection(callId, connection);
        }

        Uri address = connection.getAddress();
        String number = address == null ? "null" : address.getSchemeSpecificPart();
        Log.v(this, "createConnection, number: %s, state: %s, capabilities: %s, properties: %s",
                Connection.toLogSafePhoneNumber(number),
                Connection.stateToString(connection.getState()),
                Connection.capabilitiesToString(connection.getConnectionCapabilities()),
                Connection.propertiesToString(connection.getConnectionProperties()));

        Log.d(this, "createConnection, calling handleCreateConnectionSuccessful %s", callId);
        /// M: CC: Set PhoneAccountHandle for ECC @{
        //[ALPS01794357]
        PhoneAccountHandle handle = null;
        if (connection instanceof TelephonyConnection) {
            handle = ((TelephonyConnection)connection).getAccountHandle();
        }
        if (handle == null) {
            handle = request.getAccountHandle();
        } else {
            Log.d(this, "createConnection, set back phone account:%s", handle);
        }
        //// @}
        mAdapter.handleCreateConnectionComplete(
                callId,
                request,
                new ParcelableConnection(
                        handle,  /* M: CC: Set PhoneAccountHandle for ECC [ALPS01794357] */
                        connection.getState(),
                        connection.getConnectionCapabilities(),
                        connection.getConnectionProperties(),
                        connection.getSupportedAudioRoutes(),
                        connection.getAddress(),
                        connection.getAddressPresentation(),
                        connection.getCallerDisplayName(),
                        connection.getCallerDisplayNamePresentation(),
                        connection.getVideoProvider() == null ?
                                null : connection.getVideoProvider().getInterface(),
                        connection.getVideoState(),
                        connection.isRingbackRequested(),
                        connection.getAudioModeIsVoip(),
                        connection.getConnectTimeMillis(),
                        connection.getConnectElapsedTimeMillis(),
                        connection.getStatusHints(),
                        connection.getDisconnectCause(),
                        createIdList(connection.getConferenceables()),
                        connection.getExtras()));

        if (isIncoming && request.shouldShowIncomingCallUi() &&
                (connection.getConnectionProperties() & Connection.PROPERTY_SELF_MANAGED) ==
                        Connection.PROPERTY_SELF_MANAGED) {
            // Tell ConnectionService to show its incoming call UX.
            connection.onShowIncomingCallUi();
        }
        if (isUnknown) {
            triggerConferenceRecalculate();
        }
        /// M: CC: Proprietary CRSS handling @{
        // [ALPS01956888] For FailureSignalingConnection, CastException JE will happen.
        if (connection.getState() != Connection.STATE_DISCONNECTED) {
            forceSuppMessageUpdate(connection);
        }
        /// @}
    }

    private final Handler mMtkHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            /// M: CC: HangupAll for FTA 31.4.4.2 @{
            case MSG_HANGUP_ALL:
                hangupAll((String) msg.obj);
                break;
            /// @}

            /// M: CC: For DSDS/DSDA Two-action operation @{
            case MSG_HANDLE_ORDERED_USER_OPERATION: {
                SomeArgs args = (SomeArgs) msg.obj;
                try {
                    String callId = (String) args.arg1;
                    String currentOperation = (String) args.arg2;
                    String pendingOperation = (String) args.arg3;
                    if (MtkConnection.OPERATION_DISCONNECT_CALL.equals(currentOperation)) {
                        disconnect(callId, pendingOperation);
                    }
                } finally {
                    args.recycle();
                }
                break;
            }
            /// @}

            /// M: CC: Interface for ECT @{
            case MSG_ECT:
                explicitCallTransfer((String) msg.obj);
                break;
            /// @}
            /// M: For VoLTE @{
            case MSG_INVITE_CONFERENCE_PARTICIPANTS: {
                SomeArgs args = (SomeArgs) msg.obj;
                try {
                    String conferenceCallId = (String) args.arg1;
                    List<String> numbers = (List<String>) args.arg2;
                    inviteConferenceParticipants(conferenceCallId, numbers);
                } finally {
                    args.recycle();
                }
                break;
            }
            case MSG_CREATE_CONFERENCE: {
                SomeArgs args = (SomeArgs) msg.obj;
                try {
                    final PhoneAccountHandle connectionManagerPhoneAccount =
                            (PhoneAccountHandle) args.arg1;
                    final String conferenceCallId = (String) args.arg2;
                    final ConnectionRequest request = (ConnectionRequest) args.arg3;
                    final List<String> numbers = (List<String>) args.arg4;
                    final boolean isIncoming = args.argi1 == 1;
                    final Session.Info info = (Session.Info) args.arg5;
                    if (!mAreAccountsInitialized) {
                        Log.d(this, "Enqueueing pre-init request %s", conferenceCallId);
                        mPreInitializationConnectionRequests.add(new Runnable() {
                            @Override
                            public void run() {
                                createConference(
                                        connectionManagerPhoneAccount,
                                        conferenceCallId,
                                        request,
                                        numbers,
                                        isIncoming,
                                        info);
                            }
                        });
                    } else {
                        createConference(
                                connectionManagerPhoneAccount,
                                conferenceCallId,
                                request,
                                numbers,
                                isIncoming,
                                info);
                    }
                } finally {
                    args.recycle();
                }
                break;
            }
            /// M: CC: Interface for blind/assured ECT @{
            case MSG_BLIND_ASSURED_ECT: {
                SomeArgs args = (SomeArgs) msg.obj;
                try {
                    String callId = (String) args.arg1;
                    String number = (String) args.arg2;
                    int type = args.argi1;
                    explicitCallTransfer(callId, number, type);
                } finally {
                    args.recycle();
                }
                break;
            }
            /// @}
            case MSG_DEVICE_SWITCH: {
                SomeArgs args = (SomeArgs) msg.obj;
                try {
                    String callId = (String) args.arg1;
                    String number = (String) args.arg2;
                    String deviceId = (String) args.arg3;
                    deviceSwitch(callId, number, deviceId);
                } finally {
                    args.recycle();
                }
                break;
            }
            case MSG_CANCEL_DEVICE_SWITCH: {
                SomeArgs args = (SomeArgs) msg.obj;
                try {
                    String callId = (String) args.arg1;
                    cancelDeviceSwitch(callId);
                } finally {
                    args.recycle();
                }
                break;
            }
            default:
                Log.d(this, "mMtkHandler default return (msg.what=%d)", msg.what);
                break;
            }
        }
    };

    private class MtkConnectionServiceBinder extends IMtkConnectionService.Stub {

        public void addMtkConnectionServiceAdapter(IMtkConnectionServiceAdapter adapter,
                Session.Info sessionInfo) {
            Log.d(this, "MtkConnectionServiceBinder add IMtkConnectionServiceAdapter");
            try {
                adapter.setIConnectionServiceBinder(mBinder);
                mMtkAdapter = adapter;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return;
        }

        /// M: CC: HangupAll for FTA 31.4.4.2 @{
        @Override
        public void hangupAll(String callId) {
            mMtkHandler.obtainMessage(MSG_HANGUP_ALL, callId).sendToTarget();
        }
        /// @}

        /// M: CC: For MSMS/MSMA ordered user operations.
        @Override
        public void handleOrderedOperation(
                String callId, String currentOperation, String pendingOperation) {
            //mHandler.obtainMessage(MSG_DISCONNECT, callId).sendToTarget();
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = callId;
            args.arg2 = currentOperation;
            args.arg3 = pendingOperation;
            mMtkHandler.obtainMessage(MSG_HANDLE_ORDERED_USER_OPERATION, args).sendToTarget();
        }
        /// @}

        /// M: CC: Interface for ECT @{
        @Override
        public void explicitCallTransfer(String callId) {
            mMtkHandler.obtainMessage(MSG_ECT, callId).sendToTarget();
        }
        /// @}

        /// M: CC: Interface for blind/assured ECT @{
        @Override
        public void blindAssuredEct(String callId, String number, int type) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = callId;
            args.arg2 = number;
            args.argi1 = type;
            mMtkHandler.obtainMessage(MSG_BLIND_ASSURED_ECT, args).sendToTarget();
        }
        /// @}

        @Override
        public void inviteConferenceParticipants(String conferenceCallId, List<String> numbers) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = conferenceCallId;
            args.arg2 = numbers;
            mMtkHandler.obtainMessage(MSG_INVITE_CONFERENCE_PARTICIPANTS, args).sendToTarget();
        }

        @Override
        public void createConference(
                final PhoneAccountHandle connectionManagerPhoneAccount,
                final String conferenceCallId,
                final ConnectionRequest request,
                final List<String> numbers,
                boolean isIncoming,
                Session.Info sessionInfo) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = connectionManagerPhoneAccount;
            args.arg2 = conferenceCallId;
            args.arg3 = request;
            args.arg4 = numbers;
            args.argi1 = isIncoming ? 1 : 0;
            args.arg5 = sessionInfo;
            mMtkHandler.obtainMessage(MSG_CREATE_CONFERENCE, args).sendToTarget();
        }

        @Override
        public void deviceSwitch(String callId, String number, String deviceId) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = callId;
            args.arg2 = number;
            args.arg3 = deviceId;
            mMtkHandler.obtainMessage(MSG_DEVICE_SWITCH, args).sendToTarget();
        }

        @Override
        public void cancelDeviceSwitch(String callId) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = callId;
            mMtkHandler.obtainMessage(MSG_CANCEL_DEVICE_SWITCH, args).sendToTarget();
        }
    };

    /// M: CC: TelephonyConnectionService canDial check @{
    public TelephonyConnection getFgConnection() {

        for (Connection c : getAllConnections()) {

            if (!(c instanceof TelephonyConnection)) {
                // the connection may be ConferenceParticipantConnection.
                continue;
            }

            TelephonyConnection tc = (TelephonyConnection) c;

            if (tc.getCall() == null) {
                continue;
            }

            Call.State s = tc.getCall().getState();

            // it assume that only one Fg call at the same time
            if (s == Call.State.ACTIVE || s == Call.State.DIALING || s == Call.State.ALERTING) {
                return tc;
            }
        }
        return null;
    }

    protected List<TelephonyConnection> getBgConnection() {

        ArrayList<TelephonyConnection> connectionList = new ArrayList<TelephonyConnection>();

        for (Connection c : getAllConnections()) {

            if (!(c instanceof TelephonyConnection)) {
                // the connection may be ConferenceParticipantConnection.
                continue;
            }

            TelephonyConnection tc = (TelephonyConnection) c;

            if (tc.getCall() == null) {
                continue;
            }

            Call.State s = tc.getCall().getState();

            // it assume the ringing call won't have more than one connection
            if (s == Call.State.HOLDING) {
                connectionList.add(tc);
            }
        }
        return connectionList;
    }

    protected List<TelephonyConnection> getRingingConnection() {

        ArrayList<TelephonyConnection> connectionList = new ArrayList<TelephonyConnection>();

        for (Connection c : getAllConnections()) {

            if (!(c instanceof TelephonyConnection)) {
                // the connection may be ConferenceParticipantConnection.
                continue;
            }

            TelephonyConnection tc = (TelephonyConnection) c;

            if (tc.getCall() == null) {
                continue;
            }

            // it assume the ringing call won't have more than one connection
            if (tc.getCall().getState().isRinging()) {
                connectionList.add(tc);
            }
        }
        return connectionList;
    }

    protected int getFgCallCount() {
        if (getFgConnection() != null) {
            return 1;
        }
        return 0;
    }

    protected int getBgCallCount() {
        return getBgConnection().size();
    }

    protected int getRingingCallCount() {
        return getRingingConnection().size();
    }

    public boolean canDial(PhoneAccountHandle accountHandle, String dialString) {

        boolean hasRingingCall = (getRingingCallCount() > 0);
        boolean hasActiveCall = (getFgCallCount() > 0);
        boolean bIsInCallMmiCommands = isInCallMmiCommands(dialString);
        Call.State fgCallState = Call.State.IDLE;

        Phone phone = getPhoneForAccount(accountHandle, false);

        TelephonyConnection fConnection = getFgConnection();
        /* bIsInCallMmiCommands == true only when dialphone == activephone */
        if (bIsInCallMmiCommands && hasActiveCall && fConnection != null) {
            /// M: ALPS02123516. IMS incall MMI checking. @{
            /// M: ALPS02344383. null pointer check. @{
            if (phone != null && phone != fConnection.getPhone()
                    && phone.getImsPhone() != null
                    && phone.getImsPhone() != fConnection.getPhone()) {
                bIsInCallMmiCommands = false;
                log("phone is different, set bIsInCallMmiCommands to false");
            }
            /// @}
        }

        if (fConnection != null) {
            Call fCall = fConnection.getCall();
            if (fCall != null) {
                fgCallState = fCall.getState();
            }
        }

        /* Block dial if one of the following cases happens
        * 1. ECC exists in either phone
        * 2. has ringing call and the current dialString is not inCallMMI
        * 3. foreground connections in TelephonyConnectionService (both phones) are DISCONNECTING
        *
        * Different from AOSP canDial() in CallTracker which only checks state of current phone
        */
        boolean isECCExists = MtkTelephonyConnectionServiceUtil.getInstance().isECCExists();
        boolean result = (!isECCExists
                && !(hasRingingCall && !bIsInCallMmiCommands)
                && (fgCallState != Call.State.DISCONNECTING));

        if (result == false) {
            log("canDial"
                    + " hasRingingCall=" + hasRingingCall
                    + " hasActiveCall=" + hasActiveCall
                    + " fgCallState=" + fgCallState
                    + " getFgConnection=" + fConnection
                    + " getRingingConnection=" + getRingingConnection()
                    + " bECCExists=" + isECCExists);
        }
        return result;
    }

    private boolean isInCallMmiCommands(String dialString) {
        boolean result = false;
        char ch = dialString.charAt(0);

        switch (ch) {
            case '0':
            case '3':
            case '4':
            case '5':
                if (dialString.length() == 1) {
                    result = true;
                }
                break;

            case '1':
            case '2':
                if (dialString.length() == 1 || dialString.length() == 2) {
                    result = true;
                }
                break;

            default:
                break;
        }

        return result;
    }
    /// @}

    /// M: CC: HangupAll for FTA 31.4.4.2 @{
    private void hangupAll(String callId) {
        Log.d(this, "hangupAll %s", callId);
        if (mConnectionById.containsKey(callId)) {
            ((TelephonyConnection)findConnectionForAction(callId, "hangupAll")).onHangupAll();
        } else {
            Conference conf = findConferenceForAction(callId, "hangupAll");
            if (conf instanceof TelephonyConference) {
                ((TelephonyConference)conf).onHangupAll();
            } else if (conf instanceof CdmaConference) {
                ((CdmaConference)conf).onHangupAll();
            } else if (conf instanceof ImsConference) {
                ((ImsConference) conf).onHangupAll();
            }
        }
    }
    /// @}

    /// M: CC: For MSMS/MSMA ordered user operations.
    private void disconnect(String callId, String pendingOperation) {
        Log.d(this, "disconnect %s, pending call action %s", callId, pendingOperation);
        if (mConnectionById.containsKey(callId)) {
            ((TelephonyConnection)findConnectionForAction(callId,
                    MtkConnection.OPERATION_DISCONNECT_CALL)).onDisconnect();
        } else {
            Conference conf = findConferenceForAction(callId,
                    MtkConnection.OPERATION_DISCONNECT_CALL);
            if (conf instanceof TelephonyConference) {
                ((TelephonyConference)conf).onDisconnect(pendingOperation);
            } else if (conf instanceof CdmaConference) {
                ((CdmaConference)conf).onDisconnect();
            } else if (conf instanceof ImsConference) {
                ((ImsConference) conf).onDisconnect();
            }
        }
    }
    /// @}

    @Override
    protected void addConnection(String callId, Connection connection) {
        connection.setTelecomCallId(callId);
        mConnectionById.put(callId, connection);
        mIdByConnection.put(connection, callId);
        connection.addConnectionListener(mConnectionListener);
        connection.setConnectionService(this);
        /// M: CC: Force updateState for Connection once its ConnectionService is set @{
        // Forcing call state update after ConnectionService is set
        // to keep capabilities up-to-date.
        if (connection instanceof TelephonyConnection) {
            ((TelephonyConnection)connection).fireOnCallState();
        }
        /// @}
    }

    private void log(String s) {
        Log.d(TAG, s);
    }

    /// M: CC: Proprietary CRSS handling @{
    /**
     * Base class for forcing SuppMessage update after ConnectionService is set,
     * see {@link ConnectionService#addConnection}
     * To be overrided by children classes.
     * @hide
     */
    protected void forceSuppMessageUpdate(Connection conn) {
        MtkTelephonyConnectionServiceUtil.getInstance().forceSuppMessageUpdate(
                (TelephonyConnection) conn);
    }
    /// @}

    /// M: CC: Interface for ECT @{
    private void explicitCallTransfer(String callId) {
        if (!canTransfer(mConnectionById.get(callId))) {
            Log.d(this, "explicitCallTransfer %s fail", callId);
            return;
        }
        Log.d(this, "explicitCallTransfer %s", callId);
        ((TelephonyConnection) findConnectionForAction(callId, "explicitCallTransfer"))
                .onExplicitCallTransfer();
    }
    /// @}

    /// M: CC: Interface for blind/assured ECT @{
    private void explicitCallTransfer(String callId, String number, int type) {
        if (!canBlindAssuredTransfer(mConnectionById.get(callId))) {
            Log.d(this, "explicitCallTransfer %s fail", callId);
            return;
        }
        Log.d(this, "explicitCallTransfer %s %s %d", callId, number, type);
        ((TelephonyConnection) findConnectionForAction(callId, "explicitCallTransfer")).
                onExplicitCallTransfer(number, type);
    }
    /// @}

    private void deviceSwitch(String callId, String number, String deviceId) {
        Log.d(this, "deviceSwitch %s %s %s", callId, number, deviceId);
        ((TelephonyConnection) findConnectionForAction(callId, "deviceSwitch")).
                onDeviceSwitch(number, deviceId);
    }

    private void cancelDeviceSwitch(String callId) {
        Log.d(this, "cancelDeviceSwitch %s", callId);
        ((TelephonyConnection) findConnectionForAction(callId, "cancelDeviceSwitch")).
                onCancelDeviceSwitch();
    }

    /**
      * Check whether onExplicitCallTransfer() can be performed on a certain connection.
      * Default implementation, need to be overrided.
      * @param bgConnection
      * @return true allowed false disallowed
      * @hide
      */
    public boolean canTransfer(Connection bgConnection) {

        if (bgConnection == null) {
            log("canTransfer: connection is null");
            return false;
        }

        if (!(bgConnection instanceof TelephonyConnection)) {
            // the connection may be ConferenceParticipantConnection.
            log("canTransfer: the connection isn't telephonyConnection");
            return false;
        }

        TelephonyConnection bConnection = (TelephonyConnection) bgConnection;

        Phone activePhone = null;
        Phone heldPhone = null;

        TelephonyConnection fConnection = getFgConnection();
        if (fConnection != null) {
            activePhone = fConnection.getPhone();
        }

        if (bgConnection != null) {
            heldPhone = bConnection.getPhone();
        }

        return (heldPhone == activePhone && activePhone.canTransfer());
    }

    /// M: CC: ECC retry. @{
    // Used for destroy the old connection when ECC phone type is not default phone type.
    private void resetTreatAsEmergencyCall(Connection connection) {
        if (connection instanceof TelephonyConnection) {
            ((TelephonyConnection) connection).resetTreatAsEmergencyCall();
        }
    }

    private void startTurnOnRadio(final Connection connection,
            final ConnectionRequest request, final Uri emergencyHandle, String number) {
        // Get the right phone object from the account data passed in.
        Phone defaultPhone = getPhoneForAccount(request.getAccountHandle(), true);
        Phone phone = MtkTelephonyConnectionServiceUtil.getInstance()
                .selectPhoneBySpecialEccRule(request.getAccountHandle(), number, defaultPhone);

        /// M: TDD data only @{
        if (MtkTelephonyConnectionServiceUtil.getInstance().isDataOnlyMode(phone)) {
            Log.i(this, "startTurnOnRadio, 4G data only");
            /// M: CC: ECC Retry @{
            // Assume only one ECC exists. Don't trigger retry
            // since Modem fails to power on should be a bug
            if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                Log.i(this, "ECC retry: clear ECC param");
                MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
            }
            connection.setDisconnected(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                    android.telephony.DisconnectCause.OUTGOING_CANCELED,
                    null));
            /// M: CC: to check whether the device has on-going ECC
            MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
            connection.destroy();
            return;
        }
        /// @}

        /* If current phone number will be treated as normal call in Telephony Framework,
           do not need to enable ECC retry mechanism */
        boolean isDialedByEmergencyCommand = PhoneNumberUtils.isEmergencyNumber(number);
        if (!MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn() &&
            isDialedByEmergencyCommand) {
            Log.i(this, "ECC Retry : set param with Intial ECC.");
            MtkTelephonyConnectionServiceUtil.getInstance().setEccRetryParams(
                    request,
                    phone.getPhoneId());
        }
        /// @}

        final int defaultPhoneType = PhoneFactory.getDefaultPhone().getPhoneType();
        if (mEmergencyCallHelper == null) {
            mEmergencyCallHelper = new EmergencyCallHelper(this);
        }

        mEmergencyCallHelper.enableEmergencyCalling(new EmergencyCallStateListener.Callback() {
            @Override
            public void onComplete(EmergencyCallStateListener listener, boolean isRadioReady) {
                if (connection.getState() == Connection.STATE_DISCONNECTED) {
                    Log.i(this, "startTurnOnRadio, connection disconnect");
                    /// M: CC: to check whether the device has on-going ECC
                    MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
                    return;
                }
                if (isRadioReady) {
                    // If the PhoneType of the Phone being used is different than the Default
                    // Phone, then we need create a new Connection using that PhoneType and
                    // replace it in Telecom.
                    if (phone.getPhoneType() != defaultPhoneType) {
                        Connection repConnection = getTelephonyConnection(request, number,
                                true, emergencyHandle, phone);
                        /// M: Modify the follow to handle the no sound issue. @{
                        // 1. Add the new connection into Telecom;
                        // 2. Disconnect the old connection;
                        // 3. Place the new connection.
                        if (repConnection instanceof TelephonyConnection) {
                            addExistingConnection(PhoneUtils.makePstnPhoneAccountHandle(phone),
                                    repConnection);
                            // Reset the emergency call flag for destroying old connection.
                            resetTreatAsEmergencyCall(connection);
                            connection.setDisconnected(
                                    DisconnectCauseUtil.toTelecomDisconnectCause(
                                            android.telephony.DisconnectCause.OUTGOING_CANCELED,
                                            "Reconnecting outgoing Emergency Call."));
                        } else {
                            /// M: CC: ECC Retry @{
                            // Assume only one ECC exists
                            if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                                Log.i(this, "ECC retry: clear ECC param");
                                MtkTelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
                            }
                            /// @}
                            connection.setDisconnected(repConnection.getDisconnectCause());
                            /// M: CC: to check whether the device has on-going ECC
                            MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
                        }
                        connection.destroy();
                        /// @}

                        // If there was a failure, the resulting connection will not be a
                        // TelephonyConnection, so don't place the call, just return!
                        if (repConnection instanceof TelephonyConnection) {
                            placeOutgoingConnection((TelephonyConnection) repConnection,
                                    phone, request);
                        }
                    } else {
                        placeOutgoingConnection((TelephonyConnection) connection,
                                phone, request);
                    }
                } else {
                    /// M: CC: ECC Retry @{
                    // Assume only one ECC exists. Don't trigger retry
                    // since Modem fails to power on should be a bug
                    if (MtkTelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                        Log.i(this, "ECC retry: clear ECC param");
                        MtkTelephonyConnectionServiceUtil.getInstance()
                                .clearEccRetryParams();
                    }
                    /// @}
                    Log.i(this, "startTurnOnRadio, failed to turn on radio");
                    connection.setDisconnected(
                            DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.POWER_OFF,
                            "Failed to turn on radio."));
                    /// M: CC: to check whether the device has on-going ECC
                    MtkTelephonyConnectionServiceUtil.getInstance().setInEcc(false);
                    connection.destroy();
                }
            }
        });
    }
    /// @}

    /// MTK Enhance IMS Conference @{
    /**
     * This can be used by telecom to either create a new outgoing conference call or
     * attach to an existing incoming conference call.
     */
    public void inviteConferenceParticipants(String conferenceCallId, List<String> numbers) {
        Log.d(this, "inviteConferenceParticipants %s", conferenceCallId);
        if (mConferenceById.containsKey(conferenceCallId)) {
            Conference conf =
                    findConferenceForAction(conferenceCallId, "inviteConferenceParticipants");
            if (conf instanceof ImsConference) {
                ((ImsConference)conf).onInviteConferenceParticipants(numbers);
            }
        }
    }

    private void createConference(
            final PhoneAccountHandle callManagerAccount,
            final String conferenceCallId,
            final ConnectionRequest request,
            final List<String> numbers,
            boolean isIncoming,
            Session.Info sessionInfo) {
        Log.d(this,
            "createConference, callManagerAccount: %s, conferenceCallId: %s, request: %s, " +
            "numbers: %s, isIncoming: %b", callManagerAccount, conferenceCallId, request, numbers,
            isIncoming);
        // Because the ConferenceController will be used when create Conference
        Conference conference = onCreateConference(
            callManagerAccount,
            conferenceCallId,
            request,
            numbers,
            isIncoming,
            sessionInfo);
        if (conference == null) {
            Log.d(this, "Fail to create conference!");
            conference = getNullConference();
        } else if (conference.getState() != Connection.STATE_DISCONNECTED) {
            if (mIdByConference.containsKey(conference)) {
                Log.d(this, "Re-adding an existing conference: %s.", conference);
            } else {
                mConferenceById.put(conferenceCallId, conference);
                mIdByConference.put(conference, conferenceCallId);
                conference.addListener(mConferenceListener);
            }
        }

        ParcelableConference parcelableConference = new ParcelableConference(
                conference.getPhoneAccountHandle(),
                conference.getState(),
                conference.getConnectionCapabilities(),
                conference.getConnectionProperties(),
                null,
                conference.getVideoProvider() == null ?
                        null : conference.getVideoProvider().getInterface(),
                conference.getVideoState(),
                conference.getConnectTimeMillis(),
                conference.getConnectElapsedTime(),
                conference.getStatusHints(),
                conference.getExtras());
        if (mMtkAdapter != null) {
            try {
                mMtkAdapter.handleCreateConferenceComplete(
                    conferenceCallId,
                    request,
                    parcelableConference);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    protected Conference onCreateConference(
            final PhoneAccountHandle connectionManagerPhoneAccount,
            final String conferenceCallId,
            final ConnectionRequest request,
            final List<String> numbers,
            boolean isIncoming,
            Session.Info sessionInfo) {
        if (conferenceCallId == null ||
                (!numbers.isEmpty() && !canDial(request.getAccountHandle(), numbers.get(0)))) {
            Log.d(this, "onCreateConference(), canDial check fail");
            /// M: ALPS02331568.  Should reture the failed conference. @{
            return MtkTelephonyConnectionServiceUtil.getInstance().createFailedConference(
                android.telephony.DisconnectCause.OUTGOING_FAILURE,
                "canDial() check fail");
            /// @}
        }
        Phone phone = getPhoneForAccount(request.getAccountHandle(), false);
        /// M: ALPS02209724. Toast if there are more than 5 numbers.
        /// M: ALPS02331568. Take away null-check for numbers. @{
        if (!isIncoming
                && numbers.size() > ImsConference.IMS_CONFERENCE_MAX_SIZE) {
            Log.d(this, "onCreateConference(), more than 5 numbers");
            if (phone != null) {
                ImsConference.toastWhenConferenceIsFull(phone.getContext());
            }
            return MtkTelephonyConnectionServiceUtil.getInstance().createFailedConference(
                    android.telephony.DisconnectCause.OUTGOING_FAILURE,
                    "more than 5 numbers");
        }
        /// @}
        return MtkTelephonyConnectionServiceUtil.getInstance().createConference(
            mImsConferenceController,
            phone,
            request,
            numbers,
            isIncoming);
    }
    /// @}
    /// M: For VoLTE conference SRVCC. @{
    /**
     * perform Ims Conference SRVCC.
     * @param imsConf the ims conference.
     * @param radioConnections the new created radioConnection
     * @hide
     */
    void performImsConferenceSRVCC(
            Conference imsConf,
            ArrayList<com.android.internal.telephony.Connection> radioConnections,
            String telecomCallId) {
        if (imsConf == null) {
            Log.e(this, new CallStateException(),
                "performImsConferenceSRVCC(): abnormal case, imsConf is null");
            return;
        }
        if (radioConnections == null || radioConnections.size() < 2) {
            Log.e(this, new CallStateException(),
                "performImsConferenceSRVCC(): abnormal case, newConnections is null");
            return;
        }
        if (radioConnections.get(0) == null || radioConnections.get(0).getCall() == null ||
                radioConnections.get(0).getCall().getPhone() == null) {
            Log.e(this, new CallStateException(),
                "performImsConferenceSRVCC(): abnormal case, can't get phone instance");
            return;
        }
        /// M: CC: new TelephonyConference with phoneAccountHandle @{
        Phone phone = radioConnections.get(0).getCall().getPhone();
        PhoneAccountHandle handle = PhoneUtils.makePstnPhoneAccountHandle(phone);
        TelephonyConference newConf = new TelephonyConference(handle);
        /// @}
        replaceConference(imsConf, (Conference) newConf);
        if (mTelephonyConferenceController instanceof TelephonyConferenceController) {
            ((TelephonyConferenceController)
                    mTelephonyConferenceController).setHandoveredConference(newConf);
        }
        // we need to follow the order below:
        // 1. new empty GsmConnection
        // 2. addExistingConnection (and it will be added to TelephonyConferenceController)
        // 3. config originalConnection.
        // Then UI will not flash the participant calls during SRVCC.
        /// M: CC: Vzw ECC/hVoLTE redial
        //ArrayList<TelephonyConnection> newGsmConnections = new ArrayList<TelephonyConnection>();
        ArrayList<TelephonyConnection> newGsmCdmaConnections = new ArrayList<TelephonyConnection>();
        for (com.android.internal.telephony.Connection radioConn : radioConnections) {
            /// M: CC: Vzw ECC/hVoLTE redial
            MtkGsmCdmaConnection connection = new MtkGsmCdmaConnection(PhoneConstants.PHONE_TYPE_GSM,
                    null, telecomCallId, null, false, false);
            /// M: ALPS02136977. Sets address first for formatted dump log.
            connection.setAddress(
                    Uri.fromParts(PhoneAccount.SCHEME_TEL, radioConn.getAddress(), null),
                    PhoneConstants.PRESENTATION_ALLOWED);
            /// M: CC: Vzw ECC/hVoLTE redial
            //newGsmConnections.add(connection);
            newGsmCdmaConnections.add(connection);
            addExistingConnection(handle, connection);
            connection.addTelephonyConnectionListener(mTelephonyConnectionListener);
        }
        for (int i = 0; i < newGsmCdmaConnections.size(); i++) {
            /// M: CC: Vzw ECC/hVoLTE redial
            //newGsmConnections.get(i).setOriginalConnection(radioConnections.get(i));
            newGsmCdmaConnections.get(i).setOriginalConnection(radioConnections.get(i));
        }
    }

    protected void replaceConference(Conference oldConf, Conference newConf) {
        Log.d(this, "SRVCC: oldConf= %s , newConf= %s", oldConf, newConf);
        if (oldConf == newConf) {
            return;
        }
        if (mIdByConference.containsKey(oldConf)) {
            Log.d(this, "SRVCC: start to do replacement");
            oldConf.removeListener(mConferenceListener);
            String id = mIdByConference.get(oldConf);
            mConferenceById.remove(id);
            mIdByConference.remove(oldConf);
            mConferenceById.put(id, newConf);
            mIdByConference.put(newConf, id);
            newConf.addListener(mConferenceListener);
        }
    }
    /// @}
    /**
     * Check whether IMS ECT can be performed on a certain connection.
     *
     * @param connection The connection to be transferred
     * @return true allowed false disallowed
     * @hide
     */
    public boolean canBlindAssuredTransfer(Connection connection) {
        if (connection == null) {
            Log.d(this, "canBlindAssuredTransfer: connection is null");
            return false;
        }
        if (!(connection instanceof TelephonyConnection)) {
            // the connection may be ConferenceParticipantConnection.
            Log.d(this, "canBlindAssuredTransfer: the connection isn't telephonyConnection");
            return false;
        } else if (((TelephonyConnection) connection).isImsConnection() == false) {
            Log.d(this, "canBlindAssuredTransfer: the connection is not an IMS connection");
            return false;
        } else if (canTransfer(connection)) {
            // We only allow one kind of transfer at same time. If it can execute consultative
            // transfer, then we disable blind/assured transfer capability.
            Log.d(this, "canBlindAssuredTransfer: the connection has consultative ECT capability");
            return false;
        }
        return true;
    }
    /// @}

    /// M: Self activation. @{
    void notifyEccToSelfActivationSM(MtkGsmCdmaPhone phone) {
        Log.d(this, "notifyEccToSelfActivationSM()");
        Bundle extra = new Bundle();
        extra.putInt(ISelfActivation.EXTRA_KEY_MO_CALL_TYPE, ISelfActivation.CALL_TYPE_EMERGENCY);
        phone.getSelfActivationInstance().selfActivationAction(
                ISelfActivation.ACTION_MO_CALL, extra);

    }
    /// @}

    /// M: Check if adapters are still alive. Use reflection to access private member in AOSP. @{
    private boolean isAdaptersAvailable() {
        try {
            Field fieldAdapters = mAdapter.getClass().getDeclaredField("mAdapters");
            fieldAdapters.setAccessible(true);
            Object adapters = fieldAdapters.get(mAdapter);
            if (adapters != null) {
                Method method = adapters.getClass().getMethod("size");
                Object size = method.invoke(adapters);
                if (size != null && size instanceof Integer) {
                    if ((Integer) size == 0) {
                        Log.w(this, "isAdaptersAvailable, " + adapters + ", " + size);
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            // It should never happen, just assume it's ok to call and return true.
            e.printStackTrace();
        }
        return true;
    }
    /// @}
}

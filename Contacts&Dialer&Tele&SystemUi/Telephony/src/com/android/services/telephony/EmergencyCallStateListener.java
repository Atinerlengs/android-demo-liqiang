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
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.SomeArgs;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;

/// M: CC: ECC phone selection rule @{
import com.mediatek.internal.telephony.RadioManager;
/// @}
/// M: CC: Vzw/CTVolte ECC @{
import com.android.internal.telephony.TelephonyDevController;

import com.mediatek.internal.telephony.IRadioPower;
import com.mediatek.internal.telephony.MtkHardwareConfig;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.services.telephony.MtkTelephonyConnectionServiceUtil;
import com.mediatek.telephony.MtkTelephonyManagerEx;
/// @}

/**
 * Helper class that listens to a Phone's radio state and sends a callback when the radio state of
 * that Phone is either "in service" or "emergency calls only."
 */
public class EmergencyCallStateListener {

    /**
     * Receives the result of the EmergencyCallStateListener's attempt to turn on the radio.
     */
    public interface Callback {
        void onComplete(EmergencyCallStateListener listener, boolean isRadioReady);
    }

    // Number of times to retry the call, and time between retry attempts.
    private static int MAX_NUM_RETRIES = 5;
    private static long TIME_BETWEEN_RETRIES_MILLIS = 5000;  // msec

    // Handler message codes; see handleMessage()
    @VisibleForTesting
    public static final int MSG_START_SEQUENCE = 1;
    @VisibleForTesting
    public static final int MSG_SERVICE_STATE_CHANGED = 2;
    @VisibleForTesting
    public static final int MSG_RETRY_TIMEOUT = 3;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_SEQUENCE:
                    SomeArgs args = (SomeArgs) msg.obj;
                    try {
                        Phone phone = (Phone) args.arg1;
                        EmergencyCallStateListener.Callback callback =
                                (EmergencyCallStateListener.Callback) args.arg2;
                        startSequenceInternal(phone, callback);
                        mWaitForInService = (boolean) args.arg3;
                    } finally {
                        args.recycle();
                    }
                    break;
                case MSG_SERVICE_STATE_CHANGED:
                    onServiceStateChanged((ServiceState) ((AsyncResult) msg.obj).result);
                    break;
                case MSG_RETRY_TIMEOUT:
                    onRetryTimeout();
                    break;
                default:
                    Log.wtf(this, "handleMessage: unexpected message: %d.", msg.what);
                    break;
            }
        }
    };


    private Callback mCallback;  // The callback to notify upon completion.
    private Phone mPhone;  // The phone that will attempt to place the call.
    private int mNumRetriesSoFar;

    /// M: CC: ECC retry @{
    private TelephonyManager mTm;
    /// @}

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

    private RadioPowerInterface mRadioPowerIf;
    class RadioPowerInterface implements IRadioPower {
        public void notifyRadioPowerChange(boolean power, int phoneId) {
            Log.d(this, "notifyRadioPowerChange, power:" + power + " phoneId:" + phoneId);
            if (mPhone == null) {
                Log.d(this, "notifyRadioPowerChange, return since mPhone is null");
                return;
            }

            // Timing to set ECM:
            // - Under flight mode (set in TeleService, use EFUN channel):
            //    - SS: before Radio on to speed ECC network searching
            if ((mPhone.getPhoneId() == phoneId) && (power == true)) {
                if (TelephonyManager.getDefault().getPhoneCount() <= 1) {
                    MtkTelephonyConnectionServiceUtil.getInstance()
                            .enterEmergencyMode(mPhone, 1/*airplane*/);
                }
            }
        }
    }
    /// @}

    /**
     * Starts the "wait for radio" sequence. This is the (single) external API of the
     * EmergencyCallStateListener class.
     *
     * This method kicks off the following sequence:
     * - Listen for the service state change event telling us the radio has come up.
     * - Retry if we've gone {@link #TIME_BETWEEN_RETRIES_MILLIS} without any response from the
     *   radio.
     * - Finally, clean up any leftover state.
     *
     * This method is safe to call from any thread, since it simply posts a message to the
     * EmergencyCallStateListener's handler (thus ensuring that the rest of the sequence is entirely
     * serialized, and runs only on the handler thread.)
     */
    public void waitForRadioOn(Phone phone, Callback callback) {
        Log.i(this, "waitForRadioOn: Phone " + phone.getPhoneId());

        if (mPhone != null) {
            // If there already is an ongoing request, ignore the new one!
            return;
        }

        /// M: CC: ECC retry @{
        mTm = TelephonyManager.getDefault();
        /// @}

        /// M: CC: Vzw/CTVolte ECC @{
        mPhone = phone;
        mRadioPowerIf = new RadioPowerInterface();
        RadioManager.registerForRadioPowerChange("EmergencyCallHelper", mRadioPowerIf);
        /// @}

        SomeArgs args = SomeArgs.obtain();
        args.arg1 = phone;
        args.arg2 = callback;
        args.arg3 = mWaitForInService;
        mHandler.obtainMessage(MSG_START_SEQUENCE, args).sendToTarget();
    }

    /**
     * Actual implementation of waitForRadioOn(), guaranteed to run on the handler thread.
     *
     * @see #waitForRadioOn
     */
    private void startSequenceInternal(Phone phone, Callback callback) {
        Log.d(this, "startSequenceInternal: Phone " + phone.getPhoneId());

        // First of all, clean up any state left over from a prior emergency call sequence. This
        // ensures that we'll behave sanely if another startTurnOnRadioSequence() comes in while
        // we're already in the middle of the sequence.
        cleanup();

        mPhone = phone;
        mCallback = callback;

        registerForServiceStateChanged();
        // Next step: when the SERVICE_STATE_CHANGED event comes in, we'll retry the call; see
        // onServiceStateChanged(). But also, just in case, start a timer to make sure we'll retry
        // the call even if the SERVICE_STATE_CHANGED event never comes in for some reason.
        startRetryTimer();
    }

    /**
     * Handles the SERVICE_STATE_CHANGED event. Normally this event tells us that the radio has
     * finally come up. In that case, it's now safe to actually place the emergency call.
     */
    private void onServiceStateChanged(ServiceState state) {
        Log.i(this, "onServiceStateChanged(), new state = %s, Phone = %s", state,
                mPhone.getPhoneId());

        /// M: CC: ECC retry @{
        Log.i(this, "onServiceStateChanged(), phoneId=" + mPhone.getPhoneId()
                + ", isEmergencyOnly=" + state.isEmergencyOnly()
                + ", phoneType=" + mPhone.getPhoneType()
                + ", hasCard=" + mTm.hasIccCard(mPhone.getPhoneId()));
        /// @}

        // Possible service states:
        // - STATE_IN_SERVICE        // Normal operation
        // - STATE_OUT_OF_SERVICE    // Still searching for an operator to register to,
        //                           // or no radio signal
        // - STATE_EMERGENCY_ONLY    // Phone is locked; only emergency numbers are allowed
        // - STATE_POWER_OFF         // Radio is explicitly powered off (airplane mode)

        if (isOkToCall(state.getState())
                || ((!mWaitForInService || !mTm.hasIccCard(mPhone.getPhoneId()))
                        && state.isEmergencyOnly())) {
            // Woo hoo!  It's OK to actually place the call.
            Log.i(this, "onServiceStateChanged: ok to call! (mWaitForInService=" + mWaitForInService
                    + ")");

            onComplete(true);
            cleanup();
        } else {
            // The service state changed, but we're still not ready to call yet.
            Log.i(this, "onServiceStateChanged: not ready to call yet, keep waiting.");
        }
    }

    /**
     * We currently only look to make sure that the radio is on before dialing. We should be able to
     * make emergency calls at any time after the radio has been powered on and isn't in the
     * UNAVAILABLE state, even if it is reporting the OUT_OF_SERVICE state.
     */
    private boolean isOkToCall(int serviceState) {
        /// M: Vzw ECC @{
        if (MtkTelephonyManagerEx.getDefault().useVzwLogic()) {
            return (mPhone.getState() == PhoneConstants.State.OFFHOOK) ||
                (serviceState != ServiceState.STATE_POWER_OFF) ||
                MtkTelephonyManagerEx.getDefault().isWifiCallingEnabled(mPhone.getSubId());
        }
        /// @}
        return ((mPhone.getState() == PhoneConstants.State.OFFHOOK) ||
                /// M: CC: ECC retry @{
                //mPhone.getServiceStateTracker().isRadioOn() ||
                (serviceState == ServiceState.STATE_IN_SERVICE) ||
                ((!mWaitForInService || !mTm.hasIccCard(mPhone.getPhoneId())) &&
                        serviceState == ServiceState.STATE_EMERGENCY_ONLY)) ||
                // Allow STATE_OUT_OF_SERVICE if we are at the max number of retries.
                (mNumRetriesSoFar == MAX_NUM_RETRIES &&
                serviceState == ServiceState.STATE_OUT_OF_SERVICE) ||
                /// @}
                /// M: CC: [ALPS03837972] Performance enhancement. With two CDMA cards,
                // the second card may not be in service or emergency only in time.
                // Don't waste time for waiting if we already have a phone in service. @{
                (isAllCdmaCard() && serviceState == ServiceState.STATE_OUT_OF_SERVICE
                        && mPhone.getPhoneId()
                                != RadioCapabilitySwitchUtil.getMainCapabilityPhoneId()
                        && hasInServicePhone()) ||
                /// @}
                /// M: CC: [ALPS02185470] Only retry once for WFC ECC. @{
                MtkTelephonyManagerEx.getDefault().isWifiCallingEnabled(mPhone.getSubId());
                /// @}
    }

    private boolean isAllCdmaCard() {
        MtkTelephonyManagerEx tmEx = MtkTelephonyManagerEx.getDefault();
        for (int i = 0; i < mTm.getPhoneCount(); i++) {
            int appFamily = tmEx.getIccAppFamily(i);
            if (appFamily == MtkTelephonyManagerEx.APP_FAM_NONE
                    || appFamily == MtkTelephonyManagerEx.APP_FAM_3GPP) {
                return false;
            }
        }
        return true;
    }

    private boolean hasInServicePhone() {
        for (int i = 0; i < mTm.getPhoneCount(); i++) {
            if (ServiceState.STATE_IN_SERVICE
                    == PhoneFactory.getPhone(i).getServiceState().getState()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles the retry timer expiring.
     */
    protected void onRetryTimeout() {
        int serviceState = mPhone.getServiceState().getState();
        Log.i(this, "onRetryTimeout():  phone state = %s, service state = %d, retries = %d.",
                mPhone.getState(), serviceState, mNumRetriesSoFar);

        /// M: CC: ECC retry @{
        Log.i(this, "onRetryTimeout(), phoneId=" + mPhone.getPhoneId()
                + ", emergencyOnly=" + mPhone.getServiceState().isEmergencyOnly()
                + ", phonetype=" + mPhone.getPhoneType()
                + ", hasCard=" + mTm.hasIccCard(mPhone.getPhoneId()));
        /// @}

        // - If we're actually in a call, we've succeeded.
        // - Otherwise, if the radio is now on, that means we successfully got out of airplane mode
        //   but somehow didn't get the service state change event.  In that case, try to place the
        //   call.
        // - If the radio is still powered off, try powering it on again.

        if (isOkToCall(serviceState)
                || ((!mWaitForInService || !mTm.hasIccCard(mPhone.getPhoneId()))
                        && mPhone.getServiceState().isEmergencyOnly())) {
            Log.i(this, "onRetryTimeout: Radio is on. Cleaning up. (mWaitForInService="
                    + mWaitForInService + ")");

            // Woo hoo -- we successfully got out of airplane mode.
            onComplete(true);
            cleanup();
        } else {
            // Uh oh; we've waited the full TIME_BETWEEN_RETRIES_MILLIS and the radio is still not
            // powered-on.  Try again.

            mNumRetriesSoFar++;
            Log.i(this, "mNumRetriesSoFar is now " + mNumRetriesSoFar);

            if (mNumRetriesSoFar > MAX_NUM_RETRIES) {
                Log.w(this, "Hit MAX_NUM_RETRIES; giving up.");
                cleanup();
            } else {
                Log.i(this, "Trying (again) to turn on the radio.");
                /// M: CC: ECC phone selection rule @{
                // Use RadioManager for powering on radio during ECC [ALPS01785370]
                if (RadioManager.isMSimModeSupport()) {
                    //RadioManager will help to turn on radio even this iccid is off by sim mgr
                    Log.i(this, "isMSimModeSupport true, use RadioManager forceSetRadioPower");
                    RadioManager.getInstance().forceSetRadioPower(true, mPhone.getPhoneId());
                } else {
                    //android's default action
                    Log.i(this, "isMSimModeSupport false, use default setRadioPower");
                    mPhone.setRadioPower(true);
                }
                /// @}
                startRetryTimer();
            }
        }
    }

    /**
     * Clean up when done with the whole sequence: either after successfully turning on the radio,
     * or after bailing out because of too many failures.
     *
     * The exact cleanup steps are:
     * - Notify callback if we still hadn't sent it a response.
     * - Double-check that we're not still registered for any telephony events
     * - Clean up any extraneous handler messages (like retry timeouts) still in the queue
     *
     * Basically this method guarantees that there will be no more activity from the
     * EmergencyCallStateListener until someone kicks off the whole sequence again with another call
     * to {@link #waitForRadioOn}
     *
     * TODO: Do the work for the comment below:
     * Note we don't call this method simply after a successful call to placeCall(), since it's
     * still possible the call will disconnect very quickly with an OUT_OF_SERVICE error.
     */
    // M: CC: change to public method to invoke from EmergencyCallHelper.
    public void cleanup() {
        Log.d(this, "cleanup(), "
                + (mPhone != null ? "phoneId=" + mPhone.getPhoneId() : "(mPhone null)"));

        // This will send a failure call back if callback has yet to be invoked.  If the callback
        // was already invoked, it's a no-op.
        onComplete(false);

        unregisterForServiceStateChanged();
        cancelRetryTimer();

        // M: CC: Clean up any pending MSG_START_SEQUENCE message.
        mHandler.removeMessages(MSG_START_SEQUENCE);

        // Used for unregisterForServiceStateChanged() so we null it out here instead.
        mPhone = null;
        mNumRetriesSoFar = 0;
        mWaitForInService = false;
    }

    private void startRetryTimer() {
        cancelRetryTimer();
        mHandler.sendEmptyMessageDelayed(MSG_RETRY_TIMEOUT, TIME_BETWEEN_RETRIES_MILLIS);
    }

    private void cancelRetryTimer() {
        mHandler.removeMessages(MSG_RETRY_TIMEOUT);
    }

    private void registerForServiceStateChanged() {
        // Unregister first, just to make sure we never register ourselves twice.  (We need this
        // because Phone.registerForServiceStateChanged() does not prevent multiple registration of
        // the same handler.)
        unregisterForServiceStateChanged();
        mPhone.registerForServiceStateChanged(mHandler, MSG_SERVICE_STATE_CHANGED, null);
    }

    private void unregisterForServiceStateChanged() {
        // This method is safe to call even if we haven't set mPhone yet.
        if (mPhone != null) {
            mPhone.unregisterForServiceStateChanged(mHandler);  // Safe even if unnecessary
        }
        mHandler.removeMessages(MSG_SERVICE_STATE_CHANGED);  // Clean up any pending messages too
    }

    private void onComplete(boolean isRadioReady) {
        if (mCallback != null) {
            Callback tempCallback = mCallback;
            mCallback = null;
            tempCallback.onComplete(this, isRadioReady);
            RadioManager.unregisterForRadioPowerChange(mRadioPowerIf);
        }
    }

    @VisibleForTesting
    public Handler getHandler() {
        return mHandler;
    }

    @VisibleForTesting
    public void setMaxNumRetries(int retries) {
        MAX_NUM_RETRIES = retries;
    }

    @VisibleForTesting
    public void setTimeBetweenRetriesMillis(long timeMs) {
        TIME_BETWEEN_RETRIES_MILLIS = timeMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !getClass().equals(o.getClass())) return false;

        EmergencyCallStateListener that = (EmergencyCallStateListener) o;

        if (mNumRetriesSoFar != that.mNumRetriesSoFar) {
            return false;
        }
        if (mCallback != null ? !mCallback.equals(that.mCallback) : that.mCallback != null) {
            return false;
        }
        return mPhone != null ? mPhone.equals(that.mPhone) : that.mPhone == null;

    }

    private boolean mWaitForInService = false;
    public void setWaitForInService(boolean waitForInService) {
        mWaitForInService = waitForInService;
    }
}

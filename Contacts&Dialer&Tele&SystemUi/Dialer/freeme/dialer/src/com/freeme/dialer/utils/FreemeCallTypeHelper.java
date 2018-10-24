/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.freeme.dialer.utils;

import android.content.Context;
import android.content.res.Resources;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.dialer.app.calllog.CallLogActivity;
import com.android.dialer.binary.common.DialerApplication;
import com.android.dialer.calllogutils.R;
import com.android.dialer.compat.AppCompatConstants;

import mediatek.telephony.MtkCarrierConfigManager;

/**
 * Helper class to perform operations related to call types.
 */
public class FreemeCallTypeHelper {

    /**
     * Name used to identify incoming calls.
     */
    private final CharSequence mIncomingName;
    /**
     * Name used to identify incoming calls which were transferred to another device.
     */
    private final CharSequence mIncomingPulledName;
    /**
     * Name used to identify outgoing calls.
     */
    private final CharSequence mOutgoingName;
    /**
     * Name used to identify outgoing calls which were transferred to another device.
     */
    private final CharSequence mOutgoingPulledName;
    /**
     * Name used to identify missed calls.
     */
    private final CharSequence mMissedName;
    /**
     * Name used to identify incoming video calls.
     */
    private final CharSequence mIncomingVideoName;
    /**
     * Name used to identify incoming video calls which were transferred to another device.
     */
    private final CharSequence mIncomingVideoPulledName;
    /**
     * Name used to identify outgoing video calls.
     */
    private final CharSequence mOutgoingVideoName;
    /**
     * Name used to identify outgoing video calls which were transferred to another device.
     */
    private final CharSequence mOutgoingVideoPulledName;
    /**
     * Name used to identify missed video calls.
     */
    private final CharSequence mMissedVideoName;
    /**
     * Name used to identify voicemail calls.
     */
    private final CharSequence mVoicemailName;
    /**
     * Name used to identify rejected calls.
     */
    private final CharSequence mRejectedName;
    /**
     * Name used to identify blocked calls.
     */
    private final CharSequence mBlockedName;
    /**
     * Name used to identify calls which were answered on another device.
     */
    private final CharSequence mAnsweredElsewhereName;

    private final CharSequence mOutGoingUnConnected;

    private final CharSequence mInConmingUnConnected;
    ///M: For customization using carrier config
    private static boolean mIsSupportCallPull = false;

    public FreemeCallTypeHelper(Resources resources) {
        // Cache these values so that we do not need to look them up each time.
        mIncomingName = resources.getString(R.string.freeme_type_incoming);
        mIncomingPulledName = resources.getString(R.string.freeme_type_incoming_pulled);
        mOutgoingName = resources.getString(R.string.freeme_type_outgoing);
        mOutgoingPulledName = resources.getString(R.string.freeme_type_outgoing_pulled);
        mMissedName = resources.getString(R.string.freeme_type_missed);
        mIncomingVideoName = resources.getString(R.string.freeme_type_incoming_video);
        mIncomingVideoPulledName = resources.getString(R.string.freeme_type_incoming_video_pulled);
        mOutgoingVideoName = resources.getString(R.string.freeme_type_outgoing_video);
        mOutgoingVideoPulledName = resources.getString(R.string.freeme_type_outgoing_video_pulled);
        mMissedVideoName = resources.getString(R.string.freeme_type_missed_video);
        mVoicemailName = resources.getString(R.string.freeme_type_voicemail);
        mRejectedName = resources.getString(R.string.freeme_type_rejected);
        mBlockedName = resources.getString(R.string.freeme_type_blocked);
        mAnsweredElsewhereName = resources.getString(R.string.freeme_type_answered_elsewhere);
        mOutGoingUnConnected = resources.getString(R.string.freeme_outgoing_unconnected);
        mInConmingUnConnected = resources.getString(R.string.freeme_incoming_unconnected);

        ///M: For customization using carrier config
        CarrierConfigManager configMgr = (CarrierConfigManager) DialerApplication
                .getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle carrierConfig =
                configMgr.getConfigForSubId(SubscriptionManager.getDefaultVoiceSubscriptionId());
        if (carrierConfig != null) {
            mIsSupportCallPull = carrierConfig.getBoolean(
                    MtkCarrierConfigManager.MTK_KEY_DIALER_CALL_PULL_BOOL);
        }
    }

    public static boolean isMissedCallType(int callType) {
    /// M: For operator, to check for call pull @{
        boolean isCallPulledType = true;
        if (mIsSupportCallPull) {
            Log.d("CallTypeHelper", "Call pull supported");
            isCallPulledType = (callType != CallLogActivity.INCOMING_PULLED_AWAY_TYPE
                    && callType != CallLogActivity.OUTGOING_PULLED_AWAY_TYPE);
        }
        //}@
        return (callType != AppCompatConstants.CALLS_INCOMING_TYPE
                && callType != AppCompatConstants.CALLS_OUTGOING_TYPE
                && callType != AppCompatConstants.CALLS_VOICEMAIL_TYPE
                && callType != AppCompatConstants.CALLS_ANSWERED_EXTERNALLY_TYPE
                && isCallPulledType);
    }

    /**
     * Returns the text used to represent the given call type.
     */
    public CharSequence getCallTypeText(int callType, boolean isVideoCall, boolean isPulledCall) {
        switch (callType) {
            case AppCompatConstants.CALLS_INCOMING_TYPE:
                if (isVideoCall) {
                    if (isPulledCall) {
                        return mIncomingVideoPulledName;
                    } else {
                        return mIncomingVideoName;
                    }
                } else {
                    if (isPulledCall) {
                        return mIncomingPulledName;
                    } else {
                        return mIncomingName;
                    }
                }

            case AppCompatConstants.CALLS_OUTGOING_TYPE:
                if (isVideoCall) {
                    if (isPulledCall) {
                        return mOutgoingVideoPulledName;
                    } else {
                        return mOutgoingVideoName;
                    }
                } else {
                    if (isPulledCall) {
                        return mOutgoingPulledName;
                    } else {
                        return mOutgoingName;
                    }
                }

            default:
                return mIncomingName;
        }
    }

    public CharSequence getCallTypeText(int callType) {
        switch (callType) {
            case AppCompatConstants.CALLS_INCOMING_TYPE:
                return mInConmingUnConnected;
            case AppCompatConstants.CALLS_OUTGOING_TYPE:
                return mOutGoingUnConnected;
            case AppCompatConstants.CALLS_REJECTED_TYPE:
                return mRejectedName;
        }
        return mInConmingUnConnected;
    }
}

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
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.server.telecom.testapps;

import android.content.Context;
import android.os.Bundle;
import android.telecom.Call;
/// M: Remotely held event and video state. @{
import android.telecom.Call.Details;
import android.telecom.Connection;
/// M: @}
import android.telecom.InCallService;
import android.telecom.VideoProfile;
import android.telecom.VideoProfile.CameraCapabilities;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.widget.Toast;

import java.util.Collections;
// M: Multi calls, sort calls.
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains a list of calls received via the {@link TestInCallServiceImpl}.
 */
public class TestCallList extends Call.Callback {

    public static abstract class Listener {
        public void onCallAdded(Call call) {}
        public void onCallRemoved(Call call) {}
        public void onRttStarted(Call call) {}
        public void onRttStopped(Call call) {}
        public void onRttInitiationFailed(Call call, int reason) {}
        public void onRttRequest(Call call, int id) {}
        /// M: Connection event for remotely hold call.
        public void onConnectionEvent(Call call, String event) {}
    }

    private static final TestCallList INSTANCE = new TestCallList();
    private static final String TAG = "TestCallList";
    // M: Have a reference of TestInCallUI to set video call.
    private TestInCallUI mTestInCallUI = null;

    private class TestVideoCallListener extends InCallService.VideoCall.Callback {
        private Call mCall;

        public TestVideoCallListener(Call call) {
            mCall = call;
        }

        @Override
        public void onSessionModifyRequestReceived(VideoProfile videoProfile) {
            Log.v(TAG,
                    "onSessionModifyRequestReceived: videoState = " + videoProfile.getVideoState()
                            + " call = " + mCall);
            /// M: Set accept video call button enabled when receive upgrade request.
            setAcceptVideoEnabled(mCall, videoProfile);
        }

        @Override
        public void onSessionModifyResponseReceived(int status, VideoProfile requestedProfile,
                VideoProfile responseProfile) {
            Log.v(TAG,
                    "onSessionModifyResponseReceived: status = " + status + " videoState = "
                            + responseProfile.getVideoState()
                            + " call = " + mCall);
            /// M: This event response from remote side, like upgrade/downgrade fail or success,
            // set property of video call, like camera, surface base responseProfile.
            setVideoCall(mCall, responseProfile);
        }

        @Override
        public void onCallSessionEvent(int event) {

        }

        @Override
        public void onPeerDimensionsChanged(int width, int height) {

        }

        @Override
        public void onVideoQualityChanged(int videoQuality) {
            Log.v(TAG,
                    "onVideoQualityChanged: videoQuality = " + videoQuality + " call = " + mCall);
        }

        @Override
        public void onCallDataUsageChanged(long dataUsage) {

        }

        @Override
        public void onCameraCapabilitiesChanged(CameraCapabilities cameraCapabilities) {

        }
    }

    // The calls the call list knows about.
    private List<Call> mCalls = new LinkedList<Call>();
    private Map<Call, TestVideoCallListener> mVideoCallListeners =
            new ArrayMap<Call, TestVideoCallListener>();
    private Set<Listener> mListeners = Collections.newSetFromMap(
            new ConcurrentHashMap<Listener, Boolean>(16, 0.9f, 1));
    private Context mContext;
    private int mLastRttRequestId = -1;

    /**
     * Singleton accessor.
     */
    public static TestCallList getInstance() {
        return INSTANCE;
    }

    public void addListener(Listener listener) {
        if (listener != null) {
            mListeners.add(listener);
        }
    }

    public boolean removeListener(Listener listener) {
        return mListeners.remove(listener);
    }

    public Call getCall(int position) {
        return mCalls.get(position);
    }

    public void addCall(Call call) {
        if (mCalls.contains(call)) {
            Log.e(TAG, "addCall: Call already added.");
            return;
        }
        Log.i(TAG, "addCall: " + call + " " + System.identityHashCode(this));
        mCalls.add(call);
        call.registerCallback(this);
        /// M: Avoid timing issue. Video call change event maybe
        // received before add call event.
        onVideoCallChanged(call, call.getVideoCall());

        for (Listener l : mListeners) {
            l.onCallAdded(call);
        }
    }

    public void removeCall(Call call) {
        if (!mCalls.contains(call)) {
            Log.e(TAG, "removeCall: Call cannot be removed -- doesn't exist.");
            return;
        }
        Log.i(TAG, "removeCall: " + call);
        mCalls.remove(call);
        call.unregisterCallback(this);

        for (Listener l : mListeners) {
            if (l != null) {
                l.onCallRemoved(call);
            }
        }
    }

    public void clearCalls() {
        for (Call call : new LinkedList<Call>(mCalls)) {
            removeCall(call);
        }

        for (Call call : mVideoCallListeners.keySet()) {
            if (call.getVideoCall() != null) {
                call.getVideoCall().destroy();
            }
        }
        mVideoCallListeners.clear();
    }

    public int size() {
        return mCalls.size();
    }

    public int getLastRttRequestId() {
        return mLastRttRequestId;
    }

    /**
     * For any video calls tracked, sends an upgrade to video request.
     */
    public void sendUpgradeToVideoRequest(Call call, int videoState) {
        Log.v(TAG, "sendUpgradeToVideoRequest : videoState = " + videoState);

        // M: Only send upgrade request to active call. @{
        //for (Call call : mCalls) {
            InCallService.VideoCall videoCall = call.getVideoCall();
            Log.v(TAG, "sendUpgradeToVideoRequest: checkCall " + call);
            if (videoCall == null) {
                return;
            }

            Log.v(TAG, "send upgrade to video request for call: " + call);
            videoCall.sendSessionModifyRequest(new VideoProfile(videoState));
        //}
        // M: @}
    }

    /**
     * For any video calls which are active, sends an upgrade to video response with the specified
     * video state.
     *
     * @param videoState The video state to respond with.
     */
    public void sendUpgradeToVideoResponse(Call call, int videoState) {
        Log.v(TAG, "sendUpgradeToVideoResponse : videoState = " + videoState);

        // M: Only send response to active call. @{
        //for (Call call : mCalls) {
            InCallService.VideoCall videoCall = call.getVideoCall();
            if (videoCall == null) {
                return;
            }

            Log.v(TAG, "send upgrade to video response for call: " + call);
            videoCall.sendSessionModifyResponse(new VideoProfile(videoState));
        //}
        // M: @}
    }

    @Override
    public void onVideoCallChanged(Call call, InCallService.VideoCall videoCall) {
        Log.v(TAG, "onVideoCallChanged: call = " + call + " " + System.identityHashCode(this));
        if (videoCall != null) {
            if (!mVideoCallListeners.containsKey(call)) {
                TestVideoCallListener listener = new TestVideoCallListener(call);
                videoCall.registerCallback(listener);
                mVideoCallListeners.put(call, listener);
                Log.v(TAG, "onVideoCallChanged: added new listener");
                /// M: Temp solution for timing issue.
                setVideoCall(call, new VideoProfile(call.getDetails().getVideoState()));
            }
        }
    }

    @Override
    public void onRttStatusChanged(Call call, boolean enabled, Call.RttCall rttCall) {
        Log.v(TAG, "onRttStatusChanged: call = " + call + " " + System.identityHashCode(this));
        if (enabled) {
            for (Listener l : mListeners) {
                l.onRttStarted(call);
            }
        } else {
            for (Listener l : mListeners) {
                l.onRttStopped(call);
            }
        }
    }

    @Override
    public void onRttInitiationFailure(Call call, int reason) {
        for (Listener l : mListeners) {
            l.onRttInitiationFailed(call, reason);
        }
    }

    @Override
    public void onRttRequest(Call call, int id) {
        mLastRttRequestId = id;
        for (Listener l : mListeners) {
            l.onRttRequest(call, id);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onConnectionEvent(Call call, String event, Bundle extras) {
        super.onConnectionEvent(call, event, extras);
        Log.i(TAG, "onConnectionEvent: event = " + event);
        if ((Connection.EVENT_CALL_REMOTELY_HELD).equals(event)
                || (Connection.EVENT_CALL_REMOTELY_UNHELD).equals(event)) {
            for (Listener l : mListeners) {
                l.onConnectionEvent(call, event);
            }
            return;
        }

        String text = MtkUtil.makeTextForConnectionEvent(call, event, extras);
        if (!TextUtils.isEmpty(text)) {
            MtkTelecomTestappsGlobals.getInstance().showToast(text);
        }
    }

    @Override
    public void onRttModeChanged(Call call, int mode) {
        super.onRttModeChanged(call, mode);
        String text = MtkUtil.makeTextRttModeChanged(call, mode);
        if (!TextUtils.isEmpty(text)) {
            MtkTelecomTestappsGlobals.getInstance().showToast(text);
        }
    }

    /**
     * M: Set a reference of TestInCallUI, support video call to
     * set camera, preview surface, display surface.
     *
     * @param testInCallUI The activity that need be used.
     */
    public void setActivity(TestInCallUI testInCallUI) {
        mTestInCallUI = testInCallUI;
    }

    /**
     * M: Get sorted calls list, the order is ACTIVE, HOLDING, RINGING, CONNECTING...
     *
     * @return The sorted call list.
     */
    public List getSortedCalls() {
        int callNum = size();

        if (callNum == 0) {
            return null;
        }

        if (callNum == 1) {
            return mCalls;
        }

        LinkedList<Call> sortedCalls = new LinkedList<Call>(mCalls);

        Collections.sort(sortedCalls, new Comparator<Call>() {
            @Override
            public int compare(Call call1, Call call2) {
                return call2.getState() - call1.getState();
            }
        });

        return sortedCalls;
    }

    /** {@inheritDoc} */
    @Override
    public void onDetailsChanged(Call call, Details details) {
        Log.i(TAG, "onDetailsChanged details = " + details);
        setVideoCall(call, new VideoProfile(details.getVideoState()));
    }

    /* M: Set video call, like camera, surface. */
    private void setVideoCall(Call call, VideoProfile videoProfile) {
        if (mTestInCallUI != null) {
            mTestInCallUI.setVideoCall(call, videoProfile);
        }
    }

    /* M: Set accept video call */
    private void setAcceptVideoEnabled(Call call, VideoProfile videoProfile) {
        if (mTestInCallUI != null) {
            mTestInCallUI.setAcceptVideoEnabled(call, videoProfile);
        }
    }
}

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

package com.mediatek.server.telecom;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.telecom.Log;
import com.android.server.telecom.Call;
import com.android.server.telecom.CallState;
import com.android.server.telecom.CallsManagerListenerBase;
import com.android.server.telecom.TelephonyUtil;
import com.mediatek.internal.telecom.ICallRecorderCallback;
import com.mediatek.internal.telecom.ICallRecorderService;
import mediatek.telecom.MtkTelecomManager;
import com.android.server.telecom.R;
import com.android.server.telecom.TelecomSystem;

import java.util.Objects;

public class CallRecorderManager {

    private static final String TAG = CallRecorderManager.class.getSimpleName();

    private static final int RECORD_STATE_IDLE = 0;
    private static final int RECORD_STATE_STARTING = 1;
    private static final int RECORD_STATE_STARTED = 2;
    private static final int RECORD_STATE_STOPING = 3;

    private static final int MSG_SERVICE_CONNECTED = 1;
    private static final int MSG_SERVICE_DISCONNECTED = 2;
    private static final int MSG_START_RECORD = 3;
    private static final int MSG_STOP_RECORD = 4;
    private static final int MSG_RECORD_STATE_CHANGED = 5;
    private final Context mContext;
    private ICallRecorderService mCallRecorderService;
    private int mRecordingState = RECORD_STATE_IDLE;

    private Call mRecordingCall = null;
    private RecordStateListener mListener;
    private Call mPendingStopRecordCall = null;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            logd("[onServiceConnected]");
            mHandler.obtainMessage(MSG_SERVICE_CONNECTED, service).sendToTarget();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            logd("[onServiceDisconnected]");
            mHandler.obtainMessage(MSG_SERVICE_DISCONNECTED).sendToTarget();
        }
    };
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SERVICE_CONNECTED:
                    handleServiceConnected((IBinder) msg.obj);
                    break;
                case MSG_SERVICE_DISCONNECTED:
                    handleServiceDisconnected();
                    break;
                case MSG_START_RECORD:
                    handleStartRecord((Call) msg.obj);
                    break;
                case MSG_STOP_RECORD:
                    handleStopRecord((Call) msg.obj);
                    break;
                case MSG_RECORD_STATE_CHANGED:
                    handleRecordStateChanged((Integer)msg.obj);
            }
        }
    };

    private synchronized void setRecordingState(int recordState) {
        logd("setRecordingState to " + recordStateToString(recordState));
        mRecordingState = recordState;
    }

    private synchronized int getRecordingState() {
        return mRecordingState;
    }

    private synchronized Call getPendingStopRecordCall() {
        return mPendingStopRecordCall;
    }

    private synchronized void setPendingStopRecordCall(Call call) {
        mPendingStopRecordCall = call;
    }
    private boolean canStartRecord() {
        return getRecordingState() == RECORD_STATE_IDLE;
    }

    private boolean canStopRecord() {
        return getRecordingState() == RECORD_STATE_STARTED;
    }

    private boolean needPendingStopRecord() {
        return getRecordingState() == RECORD_STATE_STARTING;
    }

    private String recordStateToString(int recordState) {
        switch (recordState) {
            case RECORD_STATE_IDLE:
                return "RECORD_STATE_IDLE";
            case RECORD_STATE_STARTING:
                return "RECORD_STATE_STARTING";
            case RECORD_STATE_STARTED:
                return "RECORD_STATE_STARTED";
            case RECORD_STATE_STOPING:
                return "RECORD_STATE_STOPING";
            default:
                return "Unknown message";
        }
    }

    private void handleStartRecord(Call call) {
        logd("[handleStartRecord] on call " + call.getId());
        if (getRecordingState() != RECORD_STATE_STARTING
                    && getRecordingState() != RECORD_STATE_IDLE) {
            logw("[handleStartRecord] return without start, mPendingRequest=" + getRecordingState()
                    + ", mRecordingCall=" + mRecordingCall);
            return;
        }

        if (call.getState() != CallState.ACTIVE) {
            logw("[handleStartRecord]call not active: " + call.getState());
            setRecordingState(RECORD_STATE_IDLE);
            return;
        }

        /// M: ALPS03759580, Cannot start record if call is not foreground. @{
        if (TelecomSystem.getInstance().getCallsManager().getForegroundCall() != call) {
            logw("[handleStartRecord]call not foreground");
            setRecordingState(RECORD_STATE_IDLE);
            return;
        }
        /// @}

        mRecordingCall = call;
        setRecordingState(RECORD_STATE_STARTING);
        if (mCallRecorderService != null) {
            startVoiceRecordInternal();
        } else {
            logd("[handleStartRecord]start bind");
            Intent intent = new Intent(MtkTelecomManager.ACTION_CALL_RECORD);
            intent.setComponent(new ComponentName(
                    "com.mediatek.callrecorder",
                    "com.mediatek.callrecorder.CallRecorderService"));
            boolean isBound = mContext.bindServiceAsUser(intent, mConnection, Context.BIND_AUTO_CREATE,
                    UserHandle.SYSTEM);
            if (!isBound) {
                MtkTelecomGlobals.getInstance().showToast(R.string.start_record_failed);
                mRecordingCall = null;
                mContext.unbindService(mConnection);
                setRecordingState(RECORD_STATE_IDLE);
            }
        }
    }

    private void handleStopRecord(Call call) {
        logd("[handleStopRecord] on call " + call.getId());
        if (getRecordingState() != RECORD_STATE_STOPING
                    && getRecordingState() != RECORD_STATE_STARTED) {
            logw("[handleStopRecord] unexpected state, just return");
            return;
        }
        if (mRecordingCall == null) {
            logw("[handleStopRecord] no call recording");
            setRecordingState(RECORD_STATE_IDLE);
            return;
        }
        if (mCallRecorderService == null) {
            logw("[handleStopRecord] call recorder service not connected");
            setRecordingState(RECORD_STATE_IDLE);
            return;
        }
        if (call != null && mRecordingCall != call) {
            logw("[handleStopRecord] state machine wrong, trying to stop a call which is not" +
                    "in recording state: " + mRecordingCall.getId() + " vs " + call.getId());
        }
        try {
            //M:fix CR:ALPS03438135,null pointer exception.
            if (mCallRecorderService == null) {
                logw("[handleStopRecord] call recorder service not connected");
                setRecordingState(RECORD_STATE_IDLE);
                return;
            }
            setRecordingState(RECORD_STATE_STOPING);
            mCallRecorderService.stopVoiceRecord();
        } catch (RemoteException e) {
            e.printStackTrace();
            setRecordingState(RECORD_STATE_IDLE);
        }
    }

    private void handleServiceConnected(IBinder binder) {
        mCallRecorderService = ICallRecorderService.Stub.asInterface(binder);
        startVoiceRecordInternal();
    }

    private void handleServiceDisconnected() {
        logd("[handleServiceDisconnected]");
        if (mRecordingCall != null && mListener != null) {
            logd("handleServiceDisconnected mRecordingCall not null, do error handling");
            mListener.onRecordStateChanged(MtkTelecomManager.CALL_RECORDING_STATE_IDLE);
        }
        mRecordingCall = null;
        mCallRecorderService = null;
        setRecordingState(RECORD_STATE_IDLE);
        setListener(null);
    }

    private void handleRecordStateChanged(int state) {
        logd("[handleRecordStateChanged]");
        if (mRecordingCall != null && mListener != null) {
            mListener.onRecordStateChanged(state);
        }
        if (state == MtkTelecomManager.CALL_RECORDING_STATE_IDLE) {
            setRecordingState(RECORD_STATE_IDLE);
            mContext.unbindService(mConnection);
            mRecordingCall = null;
            mCallRecorderService = null;
            setListener(null);
        } else if (state == MtkTelecomManager.CALL_RECORDING_STATE_ACTIVE) {
            setRecordingState(RECORD_STATE_STARTED);
            ///M: ALPS03638218 Call recording cannot be started, @{
            // If there has pending stop record which caused by pervious record stop failed.
            // Trigger stop record msg  again when record state change to STARTED.
            Call pendingStopRecordCall = getPendingStopRecordCall();
            if (pendingStopRecordCall != null && pendingStopRecordCall == mRecordingCall) {
                logd("handlePendingStopRecord");
                mHandler.obtainMessage(MSG_STOP_RECORD,
                        pendingStopRecordCall).sendToTarget();
            }
            setPendingStopRecordCall(null);
            /// @}
        }
    }

    public CallRecorderManager(Context context) {
        mContext = context;
    }

    public boolean startVoiceRecord(Call call) {
        if(!canStartRecord()) {
            logd("[startVoiceRecord] fail, record state is "
                + recordStateToString(getRecordingState()));
            return false;
        }

        mRecordingCall = call;
        setRecordingState(RECORD_STATE_STARTING);
        setPendingStopRecordCall(null);
        logd("[startVoiceRecord] on call " + call.getId());
        mHandler.obtainMessage(MSG_START_RECORD, call).sendToTarget();
        return true;
    }

    public void stopVoiceRecord(Call call) {
        if(!canStopRecord()) {
            ///M: ALPS03638218 Call recording cannot be started @{
            //  1. Current record is STARTING state and waiting response from record service.
            //  2. Stop record operation is triggered by call state change to hold.
            //      CallRecorderManager cannot handle stop operation here and just pending.
            //  3. When record service start record done, record state will change to STARTED.
            //     Then we will execute stop record msg.
            if (needPendingStopRecord()) {
                logd("[stopVoiceRecord] pending, record state is"
                        + recordStateToString(getRecordingState()));
                setPendingStopRecordCall(call);
            /// @}
            } else {
                logd("[stopVoiceRecord] fail, record state is"
                        + recordStateToString(getRecordingState()));
            }
            return;
        }

        setRecordingState(RECORD_STATE_STOPING);
        setPendingStopRecordCall(null);
        mHandler.obtainMessage(MSG_STOP_RECORD, call).sendToTarget();
    }

    private void startVoiceRecordInternal() {
        if (mCallRecorderService == null) {
            return;
        }
        try {
            mCallRecorderService.setCallback(new Callback());
            if (getRecordingState() == RECORD_STATE_STARTING) {
                mCallRecorderService.startVoiceRecord();
            } else if ((getRecordingState() == RECORD_STATE_STARTED)
                    || (getRecordingState() == RECORD_STATE_STOPING)) {
                logw("handleServiceConnected, unexpeted state %d" + getRecordingState());
                setRecordingState(RECORD_STATE_IDLE);
            }
        } catch (RemoteException e) {
            mRecordingCall = null;
            mCallRecorderService = null;
            setRecordingState(RECORD_STATE_IDLE);
            e.printStackTrace();
        }
    }

    private class Callback extends ICallRecorderCallback.Stub {

        @Override
        public void onRecordStateChanged(int state) throws RemoteException {
            logd("[onRecordStateChanged] state: " + state);
            mHandler.obtainMessage(MSG_RECORD_STATE_CHANGED, state).sendToTarget();
        }
    }

    public void setListener(RecordStateListener listener) {
        mListener = listener;
    }

    public interface RecordStateListener {
        void onRecordStateChanged(int state);
    }

    private void logd(String msg) {
        Log.d(TAG, msg);
    }
    private void logw(String msg) {
        Log.w(TAG, msg);
    }
}

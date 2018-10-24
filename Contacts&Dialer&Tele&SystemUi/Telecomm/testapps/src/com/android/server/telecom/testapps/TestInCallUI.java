/*
 * Copyright (C) 2015 Android Open Source Project
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

package com.android.server.telecom.testapps;

import android.app.Activity;
// M: Show RTT conference dialog.
import android.app.AlertDialog;
// M: Clear RTT text.
import android.content.Context;
/// M: Show RTT conference dialog.
import android.content.DialogInterface;
import android.content.Intent;
// M: Video call.
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
// M: Set RTT audio mode.
import android.provider.Settings;
import android.telecom.Call;
// M: Initial fail event.
import android.telecom.Connection;
// M: Video call.
import android.telecom.InCallService;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
// M: Video call.
import android.telecom.VideoProfile;
/// M: ECC number handle. @{
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
// M: @}
import android.util.Log;
// M: Video call.
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
// M: Set text.
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import mediatek.telecom.MtkCall;
import mediatek.telecom.MtkConnection;

// M: RTT calls list.
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class TestInCallUI extends Activity {
    // M: Set audio route. @{
    private static final int ROUTE_EARPIECE = 0x00000001;
    private static final int ROUTE_SPEAKER = 0x00000008;
    // @}

    private ListView mListView;
    private TestCallList mCallList;

    // M: Preview and display SurfaceView of video call. @{
    private Call mUpgradeVideoCall;
    private VideoProfile mUpgradeVideoProfile;

    private View mAcceptVideoButton;
    private SurfaceView mPreviewSurfaceView;
    private SurfaceView mDisplaySurfaceView;
    private CameraManager mCameraManager;
    // M: @}

    // M: For register and unRegister listeners. @{
    private TestCallList.Listener mCallListListener;
    private CallListAdapter mCallListAdapter;
    // M: @}

    // M: Mute and speaker call. @{
    private boolean mMute = false;
    private boolean mSpeaker = false;
    // M: @}

    /// M: Record call held/unheld status. @{
    private ArrayMap<String, Integer> mCallHeldStatus = new ArrayMap<String, Integer>();
    /// @}

    /** ${inheritDoc} */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.incall_screen);

        mListView = (ListView) findViewById(R.id.callListView);
        // M: For register and unRegister listeners. @{
        mCallListAdapter = new CallListAdapter(this);
        mListView.setAdapter(mCallListAdapter);
        // M: @}
        mListView.setVisibility(View.VISIBLE);

        mCallList = TestCallList.getInstance();

        // M: Set activity to TestCallList to set camera and surface. @{
        mCallList.setActivity(this);
        mCameraManager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        // M: @}

        // M: Create and register listeners. @{
        mCallListListener = new TestCallList.Listener() {
            /**
             * M: Listen call added event to show EMC notification.
             */
            @Override
            public void onCallAdded(Call call) {
                boolean isRttRequest = call.getDetails().getIntentExtras().getBoolean(
                        TelecomManager.EXTRA_START_CALL_WITH_RTT, false);
                if (isRttRequest) {
                    String number = call.getDetails().getHandle().getSchemeSpecificPart();
                    if (PhoneNumberUtils.isEmergencyNumber(number)) {
                        TelephonyManager tm =
                            (TelephonyManager) TestInCallUI.this.getSystemService(
                                    Context.TELEPHONY_SERVICE);
                        if (tm != null) {
                            int nt = tm.getDataNetworkType((SubscriptionManager.getSubId(0))[0]);
                            if (!(nt == TelephonyManager.NETWORK_TYPE_LTE
                                    || nt == TelephonyManager.NETWORK_TYPE_LTE_CA)) {
                                Toast.makeText(TestInCallUI.this, "RTT is not available for " +
                                        "this 911 call. Your call has been connected as a " +
                                        "voice-only call. If you experience difficulties, " +
                                        "please place a voice or relay call to 911.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }

            @Override
            public void onCallRemoved(Call call) {
                // M: Clear the call state when call is removed. @{
                mCallHeldStatus.remove(call.getDetails().getTelecomCallId());
                MtkTelecomTestappsGlobals.getInstance().clearCallState(
                        call.getDetails().getTelecomCallId());
                if (mCallList.size() == 0) {
                    MtkTelecomTestappsGlobals.getInstance().clearCallState();
                    // M: @}
                    Log.i(TestInCallUI.class.getSimpleName(), "Ending the incall UI");
                    finish();
                }
            }

            @Override
            public void onRttStarted(Call call) {
                // M: [RTT] if RTT call answered but the remote side has no RTT capability. @{
                Log.i(TestInCallUI.class.getSimpleName(),
                        String.format("RTT started for call: %s, capabilities: %s, properties: %s",
                                call.getDetails().getTelecomCallId(),
                                MtkCall.MtkDetails.capabilitiesToStringShort(
                                        call.getDetails().getCallCapabilities()),
                                MtkCall.MtkDetails.propertiesToStringShort(
                                        call.getDetails().getCallProperties())));
                // it means the call was answered by an answer machine and would never reply
                // any text.
                // FIXME: Currently, haven't defined the RTT_REMOTE_CAPABILITY, the IMS framework
                // would reuse PROPERTY_GTT_REMOTE for the same purpose.
                // This solution likes a work around.
                /*if (!call.getDetails().hasProperty(MtkCall.MtkDetails.PROPERTY_GTT_REMOTE)) {
                    MtkTelecomTestappsGlobals.getInstance()
                            .showToast("RTT answered by answer machine.\n");
                    return;
                }*/
                //Toast.makeText(TestInCallUI.this, "RTT now enabled", Toast.LENGTH_SHORT).show();
                // M: @}
            }

            @Override
            public void onRttStopped(Call call) {
                // M: Only toast when stop RTT byself. @{
                if (MtkTelecomTestappsGlobals.getInstance().isRttEndBySelf(
                        call.getDetails().getTelecomCallId())) {
                    Toast.makeText(TestInCallUI.this, "RTT now disabled",
                            Toast.LENGTH_SHORT).show();
                }
                // M: @}
            }

            @Override
            public void onRttInitiationFailed(Call call, int reason) {
                // M: Handle RTT initial fail. @{
                String number = call.getDetails().getHandle().getSchemeSpecificPart();
                if (PhoneNumberUtils.isPotentialLocalEmergencyNumber(TestInCallUI.this, number)) {
                    Toast.makeText(TestInCallUI.this, "RTT is not available for " +
                            "this 911 call. Your call has been connected as a " +
                            "voice-only call. If you experience difficulties, " +
                            "please place a voice or relay call to 911.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (reason == MtkConnection.MtkRttModifyStatus.SESSION_DOWNGRADED_BY_REMOTE) {
                    Toast.makeText(TestInCallUI.this, "RTT downgraded by Remote side",
                            Toast.LENGTH_SHORT).show();
                    return;
                } else if (reason == Connection.RttModifyStatus.SESSION_MODIFY_REQUEST_FAIL) {
                    Toast.makeText(TestInCallUI.this, "Real-time text not being established",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                // M: @}
                Toast.makeText(TestInCallUI.this, String.format("RTT failed to init: %d", reason),
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRttRequest(Call call, int id) {
                // M: Mark it as "RTT now enabled" toast shows. @{
                /*Toast.makeText(TestInCallUI.this, String.format("RTT request: %d", id),
                        Toast.LENGTH_SHORT).show();*/
                Log.i(TestInCallUI.class.getSimpleName(), "[onRttRequest] try to auto accept");

                String rttRequestToast = call.getDetails().getHandle().getSchemeSpecificPart()
                        + " switches to a RTT call";
                Toast.makeText(TestInCallUI.this, rttRequestToast, Toast.LENGTH_SHORT).show();

                // Auto accept upgrade request.
                if (!call.isRttActive()) {
                    call.respondToRttRequest(id, true);
                }
                // M: @}
            }

            /// M: Remotely hold/unhold evnet. @{
            @Override
            public void onConnectionEvent(Call call, String event) {
                if ((Connection.EVENT_CALL_REMOTELY_HELD).equals(event)) {
                    mCallHeldStatus.put(call.getDetails().getTelecomCallId(), 1);
                } else if ((Connection.EVENT_CALL_REMOTELY_UNHELD).equals(event)) {
                    mCallHeldStatus.put(call.getDetails().getTelecomCallId(), 0);
                } else {
                    // do nothing.
                }
            }
            /// M: @}
        };

        mCallList.addListener(mCallListListener);
        // M: @}

        View endCallButton = findViewById(R.id.end_call_button);
        View holdButton = findViewById(R.id.hold_button);
        View muteButton = findViewById(R.id.mute_button);
        // M: Reject call.
        View rejectButton = findViewById(R.id.reject_button);
        View rttIfaceButton = findViewById(R.id.rtt_iface_button);
        View answerButton = findViewById(R.id.answer_button);
        View startRttButton = findViewById(R.id.start_rtt_button);
        // M: Merge call.
        View mergeButton = findViewById(R.id.merge_call_button);
        // M: Speaker call.
        View speakerButton = findViewById(R.id.speaker_button);
        /// M: DTMF button.
        View dtmfButton = findViewById(R.id.play_dtmf_button);

        // M: Video call SurfaceView. @{
        View startVideoButton = findViewById(R.id.start_video_button);
        View stopVideoButton = findViewById(R.id.stop_video_button);
        mAcceptVideoButton = findViewById(R.id.accept_video_button);
        mPreviewSurfaceView = findViewById(R.id.preview_surface);
        mDisplaySurfaceView = findViewById(R.id.display_surface);
        // M: @}

        // M: Hide some buttons as for ATT only. @{
        if (MtkTelecomTestappsGlobals.isAdvancedFeatureSupport(this)) {
            muteButton.setVisibility(View.VISIBLE);
            mergeButton.setVisibility(View.VISIBLE);
            speakerButton.setVisibility(View.VISIBLE);
            startVideoButton.setVisibility(View.VISIBLE);
            stopVideoButton.setVisibility(View.VISIBLE);
            dtmfButton.setVisibility(View.VISIBLE);
        }
        // M: @}

        endCallButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // M: Get call that can be operated with. @{
                List<Call> calls = mCallList.getSortedCalls();
                if (calls != null) {
                    Call call = calls.get(0);
                    if (call != null) {
                        call.disconnect();
                    }
                }
                // M: @}
            }
        });

        holdButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // M: Hold active call and unhold hold call. @{
                List<Call> calls = mCallList.getSortedCalls();
                if (calls != null) {
                    Call call = calls.get(0);
                    if (call.getState() == Call.STATE_HOLDING) {
                        call.unhold();
                    } else {
                        call.hold();
                        if (calls.size() > 1) {
                            for (Call otherCall : calls) {
                                if (otherCall != null &&
                                        otherCall.getState() == Call.STATE_HOLDING) {
                                    otherCall.unhold();
                                    break;
                                }
                            }
                        }
                    }
                }
                // M: @}
            }
        });

        // M: Mute button. @{
        muteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                List<Call> calls = mCallList.getSortedCalls();
                if (calls != null) {
                    mMute = !mMute;
                    MtkTelecomTestappsGlobals.getInstance().setMuted(mMute);
                    ((Button)muteButton).setText(mMute ? "unMute" : "Mute");
                }
            }
        });
        // @}

        // M: Speaker button. @{
        speakerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                List<Call> calls = mCallList.getSortedCalls();
                if (calls != null) {
                    mSpeaker = !mSpeaker;
                    MtkTelecomTestappsGlobals.getInstance().setAudioRoute(
                            mSpeaker ? ROUTE_SPEAKER : ROUTE_EARPIECE);
                    ((Button)speakerButton).setText(mSpeaker ? "Normal" : "Speaker");
                }
            }
        });
        // M: @}

        // M: Reject call when the incoming call state is ringing. @{
        rejectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                List<Call> calls = mCallList.getSortedCalls();
                if (calls != null) {
                    for (Call call : calls) {
                        if (call.getState() == Call.STATE_RINGING) {
                            call.reject(false, null);
                            break;
                        }
                    }
                }
            }
        });
        // M: @}

        rttIfaceButton.setOnClickListener((view) -> {
            // M: Get call that RTT is active. @{
            List<Call> calls = mCallList.getSortedCalls();
            if (calls != null) {
                Call call = calls.get(0);
                boolean isCallHeld = (call.getState() == Call.STATE_HOLDING
                        || isCallRemotelyHeld(call.getDetails().getTelecomCallId()));
                Log.i(TestInCallUI.class.getSimpleName(), "isCallHeld = " + isCallHeld
                        + " rtt active = " + call.isRttActive());
                if (call.isRttActive() && !isCallHeld) {
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setClass(this, TestRttActivity.class);
                    startActivity(intent);
                }
            }
            // M: @}
        });

        answerButton.setOnClickListener(view -> {
            // M: Get call that state is ringing. @{
            List<Call> calls = mCallList.getSortedCalls();
            if (calls != null) {
                for (Call call : calls) {
                    if (call.getState() == Call.STATE_RINGING) {
                        call.answer(call.getDetails().getVideoState());
                        break;
                    }
                }
            }
            // M: @}
        });

        startRttButton.setOnClickListener(view -> {
            // M: Get call that RTT is not active. @{
            List<Call> calls = mCallList.getSortedCalls();
            if (calls != null) {
                Call call = calls.get(0);
                if (!call.isRttActive()) {
                    call.sendRttRequest();
                    // M: Show toast to notify user that has sent a RTT request. @{
                    String number = call.getDetails().getHandle().getSchemeSpecificPart();
                    String rttRequestToast = "RTT request is being sent to "
                            + call.getDetails().getHandle().getSchemeSpecificPart();
                    Toast.makeText(TestInCallUI.this, rttRequestToast, Toast.LENGTH_SHORT).show();
                }
            }
            // M: @}
        });

        // M: Merge call. @{
        mergeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                List<Call> calls = mCallList.getSortedCalls();
                if (calls != null && calls.size() > 1) {
                    Call call = calls.get(0);
                    Call call1 = calls.get(1);
                    if (call.isRttActive() || call1.isRttActive()) {
                        showRttConferenceDialog(call, call1);
                    } else {
                        call.conference(call1);
                    }
                }
            }
        });
        // @}

        // M: Send upgrade/downgrade video call request to remote side. @{
        startVideoButton.setOnClickListener(view -> {
            List<Call> calls = mCallList.getSortedCalls();
            if (calls != null) {
                Call call = calls.get(0);
                if (!call.isRttActive()) {
                    mCallList.sendUpgradeToVideoRequest(calls.get(0),
                            VideoProfile.STATE_BIDIRECTIONAL);
                }
            }
        });

        stopVideoButton.setOnClickListener((view) -> {
            List<Call> calls = mCallList.getSortedCalls();
            if (calls != null) {
                mCallList.sendUpgradeToVideoRequest(calls.get(0),
                        VideoProfile.STATE_AUDIO_ONLY);
            }
        });

        /// M: Dtmf. @{
        dtmfButton.setOnClickListener((view) -> {
            List<Call> calls = mCallList.getSortedCalls();
            if (calls != null) {
                if (MtkTelecomTestappsGlobals.getInstance() != null) {
                    MtkTelecomTestappsGlobals.getInstance().playDtmf(TestInCallUI.this,
                            calls.get(0));
                }
            }
        });
        /// @}

        mAcceptVideoButton.setOnClickListener((view) -> {
            List<Call> calls = mCallList.getSortedCalls();
            if (mUpgradeVideoCall != null
                    && (mUpgradeVideoCall.getDetails().getTelecomCallId()
                            == calls.get(0).getDetails().getTelecomCallId())) {
                mCallList.sendUpgradeToVideoResponse(mUpgradeVideoCall,
                        mUpgradeVideoProfile.getVideoState());
            }

            mAcceptVideoButton.setVisibility(View.GONE);
            mUpgradeVideoCall = null;
            mUpgradeVideoProfile = null;
        });
    }

    /** ${inheritDoc} */
    @Override
    protected void onDestroy() {
        // M: Unregister listeners. @{
        mCallListAdapter.removeListener();
        mCallList.removeListener(mCallListListener);
        // M: @}
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private PhoneAccountHandle getHandoverToPhoneAccountHandle() {
        TelecomManager tm = TelecomManager.from(this);

        List<PhoneAccountHandle> handles = tm.getAllPhoneAccountHandles();
        Optional<PhoneAccountHandle> found = handles.stream().filter(h -> {
            PhoneAccount account = tm.getPhoneAccount(h);
            Bundle extras = account.getExtras();
            return extras != null && extras.getBoolean(PhoneAccount.EXTRA_SUPPORTS_HANDOVER_TO);
        }).findFirst();
        PhoneAccountHandle foundHandle = found.orElse(null);
        Log.i(TestInCallUI.class.getSimpleName(), "getHandoverToPhoneAccountHandle() = " +
            foundHandle);
        return foundHandle;
    }

    /**
     * M: Set camera, surface of video call when recieve session modify request
     * or response, can see preview and dispaly surface after this step.
     *
     * @param call The call operated with.
     * @param videoProfile The attributes of video calls to be set.
     */
    public void setVideoCall(Call call, VideoProfile videoProfile) {
        Log.i(TestInCallUI.class.getSimpleName(), "call = " + call
                + " videoProfile = " + videoProfile);
        if (call == null) {
            return;
        }

        if (videoProfile.getVideoState() == VideoProfile.STATE_BIDIRECTIONAL) {
            try {
                mPreviewSurfaceView.setVisibility(View.VISIBLE);
                mDisplaySurfaceView.setVisibility(View.VISIBLE);
                String[] cameraIds = mCameraManager.getCameraIdList();
                if (cameraIds.length > 0) {
                    call.getVideoCall().setCamera(cameraIds[0]);
                }

                if (mCallList.size() == 1) {
                    call.getVideoCall().setPreviewSurface(
                            mPreviewSurfaceView.getHolder().getSurface());
                    call.getVideoCall().setDisplaySurface(
                            mDisplaySurfaceView.getHolder().getSurface());
                }
            } catch (Exception e) {
            }
        }

        if (videoProfile.getVideoState() == VideoProfile.STATE_AUDIO_ONLY) {
            try {
                mPreviewSurfaceView.setVisibility(View.INVISIBLE);
                mDisplaySurfaceView.setVisibility(View.INVISIBLE);

                call.getVideoCall().setPreviewSurface(null);
                call.getVideoCall().setDisplaySurface(null);
            } catch (Exception e) {
            }
        }
    }

    /**
     * M: Set accept video call button enabled.
     *
     * @param call The call operated with.
     * @param videoProfile The attributes of video calls to be set.
     */
    public void setAcceptVideoEnabled(Call call, VideoProfile videoProfile) {
        mUpgradeVideoProfile = videoProfile;
        mUpgradeVideoCall = call;

        if (call.getDetails().getVideoState() == VideoProfile.STATE_AUDIO_ONLY) {
            mAcceptVideoButton.setVisibility(View.VISIBLE);
            Toast.makeText(TestInCallUI.this, "Receive video upgrade request",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * M: Show dialog to notify user that the Rtt call will be downgraded
     * before merge to conference.
     */
    private void showRttConferenceDialog(Call call, Call call1) {
        AlertDialog.Builder adb = new AlertDialog.Builder(TestInCallUI.this);
        adb.setTitle("RTT downgrade notification")
                .setMessage("Rtt call will be downgraded to normal call before " +
                        "merge to conference.")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        call.conference(call1);
                    }
                });
        adb.show();
    }

    /** M: Check whether call is held remotely. */
    public boolean isCallRemotelyHeld(String callId) {
        if (mCallHeldStatus.containsKey(callId)) {
            return (mCallHeldStatus.get(callId) == 1);
        }
        return false;
    }
}

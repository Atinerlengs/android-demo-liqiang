/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.app.Activity;
// M: Handle RTT text.
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telecom.Call;
/// M: Remotely held event.
import android.telecom.Connection;
import android.telecom.Log;
/// M: ECC number handle.
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
// M: Handle RTT text.
import android.text.TextUtils;
import android.text.TextWatcher;
// M: TextView scrollbar.
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class TestRttActivity extends Activity {
    private static final String LOG_TAG = TestRttActivity.class.getSimpleName();
    private static final long NEWLINE_DELAY_MILLIS = 3000;

    private static final int UPDATE_RECEIVED_TEXT = 1;
    private static final int UPDATE_SENT_TEXT = 2;
    private static final int RECEIVED_MESSAGE_GAP = 3;
    private static final int SENT_MESSAGE_GAP = 4;
    private static final int UPATE_CALL_DURATION = 5;

    private TextView mReceivedText;
    private TextView mSentText;
    private EditText mTypingBox;
    // M: Call duration time. @{
    private TextView mDurationText;
    private boolean mStopDuration = false;
    // M: @}

    private TestCallList mCallList;

    // M: Multi RTT call and multi thread. @{
    private Call mCall;
    private TextWatcher mTextWatcher;
    private ReceiveThread mReceiveReader;
    private TestCallList.Listener mCallListListener;
    private Object mLock = new Object();

    class ReceiveThread extends Thread {
        private Handler mReceiveHandler;
        private Call mHandlerCall;
        private boolean mUsage;
        private String mTempReceived;

        public ReceiveThread(Handler handler, Call call) {
            mReceiveHandler = handler;
            mHandlerCall = call;
            mUsage = true;
            mTempReceived = mReceivedText.getText().toString();
        }

        public void setParameters(Handler handler, Call call) {
            mReceiveHandler = handler;
            mHandlerCall = call;
            mUsage = true;
            mTempReceived = mReceivedText.getText().toString();
        }

        public void setUsage(boolean usage) {
            mUsage = usage;
        }

        @Override
        public void run() {
            // outer loop
            while (true) {
                begin :
                // sleep and wait if there are no calls
                while (mCallList.size() > 0) {
                    // M: Multi call, use the active RTT call. @{
                    //Call.RttCall rttCall = mCallList.getCall(0).getRttCall();
                    if (mHandlerCall.getRttCall() == null) {
                        break;
                    }
                    // inner read loop
                    while (true) {
                        String receivedText;
                        receivedText = mHandlerCall.getRttCall().read();
                        Log.i(LOG_TAG, "Received %s", receivedText);
                        if (receivedText == null) {
                            if (Thread.currentThread().isInterrupted()) {
                                break begin;
                            }
                            break;
                        } else {
                            /*int index = receivedText.indexOf("\u001B");
                            if (index > -1) {
                                break begin;
                            }*/
                        }
                        /*mTextDisplayHandler.removeMessages(RECEIVED_MESSAGE_GAP);
                        mTextDisplayHandler.sendEmptyMessageDelayed(RECEIVED_MESSAGE_GAP,
                                NEWLINE_DELAY_MILLIS);*/
                        synchronized (mLock) {
                            if (mUsage) {
                                mReceiveHandler.obtainMessage(UPDATE_RECEIVED_TEXT, receivedText)
                                        .sendToTarget();
                            }

                            mTempReceived = getNewTexts(receivedText, mTempReceived);
                            MtkTelecomTestappsGlobals.getInstance().saveReceivedText(
                                    mHandlerCall.getDetails().getTelecomCallId(), mTempReceived);
                        }
                    }
                }
                if (Thread.currentThread().isInterrupted()) {
                    // M: Debug.
                    Log.i(LOG_TAG, "End the thread as interrupted.");
                    break;
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // M: Debug.
                    Log.i(LOG_TAG, "End the thread as interrupted exception.");
                    break;
                }
            }

            // M: Remove the cached thread.
            MtkTelecomTestappsGlobals.getInstance().removeReceiverThread(
                    mHandlerCall.getDetails().getTelecomCallId());
        }
    }
    // M: @}

    private Handler mTextDisplayHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            String text;
            switch (msg.what) {
                case UPDATE_RECEIVED_TEXT:
                    // M: Handle RTT text delete event. @{
                    text = (String) msg.obj;
                    mReceivedText.setText(getNewTexts(text, mReceivedText.getText().toString()));
                    // M: @}
                    break;
                case UPDATE_SENT_TEXT:
                    text = (String) msg.obj;
                    mSentText.setText(getNewTexts(text, mSentText.getText().toString()));
                    break;
                // M: Currently unused. @{
                /*case RECEIVED_MESSAGE_GAP:
                    mReceivedText.append("\n> ");
                    break;
                case SENT_MESSAGE_GAP:
                    mSentText.append("\n> ");
                    mTypingBox.setText("");
                    break;*/
                // M: @}
                // M: Call duration time. @{
                case UPATE_CALL_DURATION:
                    showCallDuration();
                    break;
                // M: @}
                default:
                    Log.w(LOG_TAG, "Invalid message %d", msg.what);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.rtt_incall_screen);

        mReceivedText = (TextView) findViewById(R.id.received_messages_text);
        mSentText = (TextView) findViewById(R.id.sent_messages_text);

        // M: Set scrollbar to TextView. @{
        mReceivedText.setMovementMethod(ScrollingMovementMethod.getInstance());
        mSentText.setMovementMethod(ScrollingMovementMethod.getInstance());
        // M: @}

        mTypingBox = (EditText) findViewById(R.id.rtt_typing_box);

        Button endRttButton = (Button) findViewById(R.id.end_rtt_button);
        Spinner rttModeSelector = (Spinner) findViewById(R.id.rtt_mode_selection_spinner);
        /// M: DTMF.
        Button dtmfButton = (Button) findViewById(R.id.play_dtmf_button);
        if (MtkTelecomTestappsGlobals.isAdvancedFeatureSupport(this)) {
            dtmfButton.setVisibility(View.VISIBLE);
        }

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.rtt_mode_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rttModeSelector.setAdapter(adapter);
        // M: Currently unused.
        rttModeSelector.setVisibility(View.GONE);
        // M: Call duration time.
        mDurationText = (TextView) findViewById(R.id.rtt_call_duration);

        // M: Create and register listeners. @{
        mCallListListener = new TestCallList.Listener() {
            @Override
            public void onCallRemoved(Call call) {
                if (mCallList.size() == 0) {
                    Log.i(LOG_TAG, "Ending the RTT UI");
                    finish();
                }
            }

            @Override
            public void onRttStopped(Call call) {
                /// M: Remove the cached thread. @{
                MtkTelecomTestappsGlobals.getInstance().removeReceiverThread(
                        call.getDetails().getTelecomCallId());
                /// @}
                TestRttActivity.this.finish();
            }

            /// M: Remotely hold/unhold evnet. @{
            @Override
            public void onConnectionEvent(Call call, String event) {
                if ((mCall.getDetails().getTelecomCallId()).equals(
                        call.getDetails().getTelecomCallId())
                        && (Connection.EVENT_CALL_REMOTELY_HELD).equals(event)) {
                    Toast.makeText(TestRttActivity.this, "Remotely held call",
                            Toast.LENGTH_SHORT).show();
                    TestRttActivity.this.finish();
                }
            }
            /// M: @}
        };
        // M: @}

        endRttButton.setOnClickListener((view) -> {
            // M: Can't stop RTT call with emergency number byself. @{
            Log.i(LOG_TAG, "endRttButton clicked.");
            String number = mCall.getDetails().getHandle().getSchemeSpecificPart();
            if (PhoneNumberUtils.isPotentialLocalEmergencyNumber(TestRttActivity.this, number)) {
                Toast.makeText(TestRttActivity.this, "Can't stop RTT call with emergency number!",
                        Toast.LENGTH_SHORT).show();
            } else {
                // M: Set stop RTT byself flag.
                MtkTelecomTestappsGlobals.getInstance().setEndRttFlag(
                        mCall.getDetails().getTelecomCallId(), true);
                mCall.stopRtt();
            }
            // @}
        });

        rttModeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CharSequence selection = (CharSequence) parent.getItemAtPosition(position);
                Call.RttCall call = mCall.getRttCall();
                switch (selection.toString()) {
                    case "Full":
                        call.setRttMode(Call.RttCall.RTT_MODE_FULL);
                        break;
                    case "HCO":
                        call.setRttMode(Call.RttCall.RTT_MODE_HCO);
                        break;
                    case "VCO":
                        call.setRttMode(Call.RttCall.RTT_MODE_VCO);
                        break;
                    default:
                        Log.w(LOG_TAG, "Bad name for rtt mode: %s", selection.toString());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        /// M: DTMF. @{
        dtmfButton.setOnClickListener((view) -> {
            if (MtkTelecomTestappsGlobals.getInstance() != null) {
                MtkTelecomTestappsGlobals.getInstance().playDtmf(TestRttActivity.this, mCall);
            }
        });
        /// @}


        mTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count == 0 || count < before) {
                    // ignore deletions and clears
                    // M: Send text delete strings. @{
                    int delNum = before - count;
                    Log.i(LOG_TAG, "delNum = %d", delNum);

                    if (delNum > 0) {
                        String delText = "";
                        for (int i = 0; i < delNum; i++) {
                            delText += "\u0008";
                        }
                        try {
                            mCall.getRttCall().write(delText);
                        } catch (IOException e) {
                            Log.w(LOG_TAG, "Exception sending delete %s", e);
                        }

                        mTextDisplayHandler.obtainMessage(UPDATE_SENT_TEXT, delText).sendToTarget();
                    }
                    // @}
                    return;
                }
                // Only appending at the end is supported.
                int numCharsInserted = count - before;
                String toAppend =
                        s.subSequence(s.length() - numCharsInserted, s.length()).toString();

                if (toAppend.isEmpty()) {
                    return;
                }
                try {
                    /// M: ATT requirement, replace line break. @{
                    if (MtkTelecomTestappsGlobals.isAdvancedFeatureSupport(TestRttActivity.this)) {
                        mCall.getRttCall().write(toAppend.replace("\n", "\r\n"));
                    } else {
                        mCall.getRttCall().write(toAppend);
                    }
                    /// @}
                } catch (IOException e) {
                    Log.w(LOG_TAG, "Exception sending text %s: %s", toAppend, e);
                }
                // M: Handle text delete event. @{
                /*mTextDisplayHandler.removeMessages(SENT_MESSAGE_GAP);
                mTextDisplayHandler.sendEmptyMessageDelayed(SENT_MESSAGE_GAP,
                        NEWLINE_DELAY_MILLIS);*/
                // @}
                mTextDisplayHandler.obtainMessage(UPDATE_SENT_TEXT, toAppend).sendToTarget();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        // M: Will cause JE, start the receive thread in onCreate. @{
        //mReceiveReader.start();
        handleOnStartEvent();
        // M: @}
    }

    @Override
    public void onStop() {
        super.onStop();
        mReceiveReader.interrupt();
        // M: Save sent text and call state. @{
        mStopDuration = true;
        mReceiveReader.setUsage(false);
        MtkTelecomTestappsGlobals.getInstance().saveSentTextAndState(
                mCall.getDetails().getTelecomCallId(),
                mSentText.getText().toString());
        // M: @}
    }

    /** M: Remove listeners when activity is destroyed.*/
    @Override
    protected void onDestroy() {
        mCallList.removeListener(mCallListListener);
        super.onDestroy();
    }

    /** M: Get new texts from current texts and received texts. */
    private String getNewTexts(String recTexts, String curTexts) {
        String newTexts = curTexts;
        for (int i = 0; i < recTexts.length(); i++) {
            String si = String.valueOf(recTexts.charAt(i));
            if (("\u0008").equals(si)) {
                Log.i(LOG_TAG, "Receive delete character");
                if (newTexts.length() > 0) {
                    newTexts = newTexts.substring(0, newTexts.length() - 1);
                }
            } else if (("\u2028").equals(si)) {
                newTexts += si.replace(si, "\n");
            } else if (("\r").equals(si)) {
                continue;
            } else {
                newTexts += si;
            }
        }
        return newTexts;
    }

    /** M: Set ever received text to the view. */
    private void setReceiveText(String callId) {
        // Write BOM as spec.
        try {
            byte[] bom = new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF};
            mCall.getRttCall().write(new String(bom, "utf-8"));
        } catch (IOException e) {
            Log.w(LOG_TAG, "Exception writing BOM %s", e);
        }

        // Set received text.
        if (MtkTelecomTestappsGlobals.getInstance().isCalling(callId)) {
            String everSendText = MtkTelecomTestappsGlobals.getInstance().getSendText(callId);
            String everReceivedText =
                    MtkTelecomTestappsGlobals.getInstance().getReceivedText(callId);

            mTypingBox.setText(everSendText);
            mTypingBox.setSelection(everSendText.length());
            mSentText.setText(everSendText);
            mReceivedText.setText(everReceivedText);
        } else {
            mTypingBox.setText("");
            mSentText.setText("");
            mReceivedText.setText("");
        }
    }

    /** M: Set receive thread, reset the parameter if the thread is already created. */
    private void setReceiveThread(String callId) {
        mReceiveReader = (ReceiveThread)(MtkTelecomTestappsGlobals.getInstance()
                .getReceiverThread(callId));
        if (mReceiveReader == null) {
            Log.i(LOG_TAG, "Start the thread.");
            mReceiveReader = new ReceiveThread(mTextDisplayHandler, mCall);
            mReceiveReader.start();
            MtkTelecomTestappsGlobals.getInstance().saveReceiverThread(callId, mReceiveReader);
        } else {
            Log.i(LOG_TAG, "Set the parameters to receive thread.");
            mReceiveReader.setParameters(mTextDisplayHandler, mCall);
        }
    }

    /** M: Handle onStart event to decide the received text and the receive thread. */
    private void handleOnStartEvent() {
        mCallList = TestCallList.getInstance();
        mCall = (Call)(mCallList.getSortedCalls().get(0));
        String callId = mCall.getDetails().getTelecomCallId();
        mStopDuration = false;
        Log.i(LOG_TAG, "callId = " + callId);

        synchronized (mLock) {
            // Set text.
            mTypingBox.removeTextChangedListener(mTextWatcher);
            setReceiveText(callId);
            mTypingBox.addTextChangedListener(mTextWatcher);

            // Set thread.
            setReceiveThread(callId);
        }

        // Set listener.
        mCallList.removeListener(mCallListListener);
        mCallList.addListener(mCallListListener);

        // Set downgrade flag.
        MtkTelecomTestappsGlobals.getInstance().setEndRttFlag(callId, false);

        // Set duration and title
        if (MtkTelecomTestappsGlobals.isAdvancedFeatureSupport(this)) {
            setTitle(mCall.getDetails().getHandle().getSchemeSpecificPart());
            showCallDuration();
        }
    }

    /** M: Show call duration. */
    private void showCallDuration() {
        if (mStopDuration) {
            return;
        }
        long durationMs = System.currentTimeMillis() - mCall.getDetails().getConnectTimeMillis();
        mDurationText.setText((durationMs / 1000) + " secs");
        mTextDisplayHandler.sendMessageDelayed(
                mTextDisplayHandler.obtainMessage(UPATE_CALL_DURATION), 1000);
    }
}

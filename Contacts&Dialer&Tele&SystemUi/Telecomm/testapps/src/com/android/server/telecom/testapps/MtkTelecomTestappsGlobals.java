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

package com.android.server.telecom.testapps;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.telecom.Call;
import android.telephony.CarrierConfigManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.HashMap;

public class MtkTelecomTestappsGlobals {
    private static final String TAG = MtkTelecomTestappsGlobals.class.getSimpleName();
    private static final int SHOW_TOAST = 1;
    private static MtkTelecomTestappsGlobals sInstance;
    private final Context mContext;
    private TestInCallServiceImpl mInCallServiceImpl;
    private HashMap<String, Thread> mReceiverThreads = new HashMap<String, Thread>();

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == SHOW_TOAST) {
                Log.i(TAG, "show event notification = " + ((String) msg.obj));
                Toast.makeText(mContext, ((String) msg.obj), Toast.LENGTH_LONG).show();
            }
        }
    };

    private MtkTelecomTestappsGlobals(Context applicationContext) {
        mContext = applicationContext;
    }

    public Context getContext() {
        return mContext;
    }

    public void showToast(int resId) {
        String text = mContext.getString(resId);
        showToast(text);
    }

    public void showToast(String text) {
        mHandler.obtainMessage(SHOW_TOAST, text).sendToTarget();
    }

    synchronized public static void createInstance(Context applicationContext) {
        if (sInstance != null) {
            return;
        }
        sInstance = new MtkTelecomTestappsGlobals(applicationContext);
    }

    public static MtkTelecomTestappsGlobals getInstance() {
        return sInstance;
    }

    /**
     * M: Save received text.
     *
     * @param key The preference key, usually call id.
     * @param receivedText The received text.
     */
    public void saveReceivedText(String key, String receivedText) {
        SharedPreferences sp = mContext.getSharedPreferences("rtt", Context.MODE_PRIVATE);
        Editor e = sp.edit();
        e.putString("rtt_received_text_" + key, receivedText);
        e.commit();
    }

    /**
     * M: Save sent text and call state.
     *
     * @param key The preference key, usually call id.
     * @param sentText The sent text.
     */
    public void saveSentTextAndState(String key, String sentText) {
        SharedPreferences sp = mContext.getSharedPreferences("rtt", Context.MODE_PRIVATE);
        Editor e = sp.edit();
        e.putString("rtt_sent_text_" + key, sentText);
        e.putBoolean("rtt_calling_" + key, true);
        e.commit();
    }

    /**
     * M: Get RTT saved sent text.
     *
     * @param key The preference key, usually call id.
     * @return The sent text.
     */
    public String getSendText(String key) {
        SharedPreferences sp = mContext.getSharedPreferences("rtt", Context.MODE_PRIVATE);
        return sp.getString("rtt_sent_text_" + key, "");
    }

    /**
     * M: Get RTT saved received text.
     *
     * @param key The preference key, usually call id.
     * @return The received text.
     */
    public String getReceivedText(String key) {
        SharedPreferences sp = mContext.getSharedPreferences("rtt", Context.MODE_PRIVATE);
        return sp.getString("rtt_received_text_" + key, "");
    }

    /**
     * M: Get whether the call is in calling state.
     *
     * @param key The preference key, usually call id.
     * @return Check whether the call is ongoing.
     */
    public boolean isCalling(String key) {
        SharedPreferences sp = mContext.getSharedPreferences("rtt", Context.MODE_PRIVATE);
        return sp.getBoolean("rtt_calling_" + key, false);
    }

    /** M: Clear all the calls' state when the calls are removed. */
    public void clearCallState() {
        SharedPreferences sp = mContext.getSharedPreferences("rtt", Context.MODE_PRIVATE);
        sp.edit().clear().commit();
    }

    /**
     * M: Clear the saved state of the call when the call is removed.
     *
     * @param key The preference key, usually call id.
     */
    public void clearCallState(String key) {
        SharedPreferences sp = mContext.getSharedPreferences("rtt", Context.MODE_PRIVATE);
        Editor e = sp.edit();
        e.putString("rtt_sent_text_" + key, "");
        e.putString("rtt_received_text_" + key, "");
        e.putBoolean("rtt_calling_" + key, false);
        e.commit();
    }

    /**
     * M: Set whether the call is downgraded by user.
     *
     * @param key The preference key, usually call id.
     * @param isStop The activity is stopped by user.
     */
    public void setEndRttFlag(String key, boolean isStop) {
        SharedPreferences sp = mContext.getSharedPreferences("rtt", Context.MODE_PRIVATE);
        Editor e = sp.edit();
        e.putBoolean("rtt_end_flag_" + key, isStop);
        e.commit();
    }

    /**
     * M: Check whether the RTT call is downgraded by user.
     *
     * @param key The preference key, usually call id.
     * @return The call is stopped by user.
     */
    public boolean isRttEndBySelf(String key) {
        SharedPreferences sp = mContext.getSharedPreferences("rtt", Context.MODE_PRIVATE);
        return sp.getBoolean("rtt_end_flag_" + key, false);
    }

    /**
     * M: Save the RTT receiver thread in TestRttActivity.
     *
     * @param key The preference key, usually call id.
     * @param t The receiver thread.
     */
    public void saveReceiverThread(String key, Thread t) {
        mReceiverThreads.put(key, t);
    }

    /**
     * M: Remove the RTT receiver thread in TestRttActivity.
     *
     * @param key The preference key, usually call id.
     */
    public void removeReceiverThread(String key) {
        mReceiverThreads.remove(key);
    }

    /**
     * M: Get the RTT receiver thread in TestRttActivity.
     *
     * @param key The preference key, usually call id.
     * @param t The receiver thread.
     */
    public Thread getReceiverThread(String key) {
        return mReceiverThreads.get(key);
    }

    /**
     * M: Have a reference to TestInCallServiceImpl to call some functions, like setMuted.
     *
     * @param icsi The reference of TestInCallServiceImpl.
     */
    public void setInCallServiceImpl(TestInCallServiceImpl icsi) {
        mInCallServiceImpl = icsi;
    }

    /**
     * M: Set muted/unmueted to the call.
     *
     * @param state The mute state.
     */
    public void setMuted(boolean state) {
        mInCallServiceImpl.setMuted(state);
    }

    /**
     * M: Set speaker/unspeaker to the call.
     *
     * @param audioRoute The audio path.
     */
    public void setAudioRoute(int audioRoute) {
        mInCallServiceImpl.setAudioRoute(audioRoute);
    }

    /**
     * M: Play DTMF.
     *
     * @param context The context.
     * @param call The call.
     */
    public void playDtmf(Context context, Call call) {
        AlertDialog.Builder adb = new AlertDialog.Builder(context);
        View dtmfView = View.inflate(context, R.layout.dtmf, null);
        Button one = (Button)dtmfView.findViewById(R.id.one);
        Button two = (Button)dtmfView.findViewById(R.id.two);
        Button three = (Button)dtmfView.findViewById(R.id.three);
        Button four = (Button)dtmfView.findViewById(R.id.four);
        Button five = (Button)dtmfView.findViewById(R.id.five);
        Button six = (Button)dtmfView.findViewById(R.id.six);
        Button seven = (Button)dtmfView.findViewById(R.id.seven);
        Button eight = (Button)dtmfView.findViewById(R.id.eight);
        Button nine = (Button)dtmfView.findViewById(R.id.nine);
        Button zero = (Button)dtmfView.findViewById(R.id.zero);
        Button star = (Button)dtmfView.findViewById(R.id.star);
        Button hash = (Button)dtmfView.findViewById(R.id.hash);
        one.setOnClickListener((view) -> {call.playDtmfTone('1');
                mHandler.postDelayed(() -> {call.stopDtmfTone();}, 200);});
        two.setOnClickListener((view) -> {call.playDtmfTone('2');
                mHandler.postDelayed(() -> {call.stopDtmfTone();}, 200);});
        three.setOnClickListener((view) -> {call.playDtmfTone('3');
                mHandler.postDelayed(() -> {call.stopDtmfTone();}, 200);});
        four.setOnClickListener((view) -> {call.playDtmfTone('4');
                mHandler.postDelayed(() -> {call.stopDtmfTone();}, 200);});
        five.setOnClickListener((view) -> {call.playDtmfTone('5');
                mHandler.postDelayed(() -> {call.stopDtmfTone();}, 200);});
        six.setOnClickListener((view) -> {call.playDtmfTone('6');
                mHandler.postDelayed(() -> {call.stopDtmfTone();}, 200);});
        seven.setOnClickListener((view) -> {call.playDtmfTone('7');
                mHandler.postDelayed(() -> {call.stopDtmfTone();}, 200);});
        eight.setOnClickListener((view) -> {call.playDtmfTone('8');
                mHandler.postDelayed(() -> {call.stopDtmfTone();}, 200);});
        nine.setOnClickListener((view) -> {call.playDtmfTone('9');
                mHandler.postDelayed(() -> {call.stopDtmfTone();}, 200);});
        zero.setOnClickListener((view) -> {call.playDtmfTone('0');
                mHandler.postDelayed(() -> {call.stopDtmfTone();}, 200);});
        star.setOnClickListener((view) -> {call.playDtmfTone('*');
                mHandler.postDelayed(() -> {call.stopDtmfTone();}, 200);});
        hash.setOnClickListener((view) -> {call.playDtmfTone('#');
                mHandler.postDelayed(() -> {call.stopDtmfTone();}, 200);});
        adb.setView(dtmfView).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        adb.show();
    }

    /**
     * M: Check whether support advanced features, like merge, video call...
     * Now only AT&T supports these features.
     *
     * @return Support AT&T RTT features or not.
     */
    public static boolean isAdvancedFeatureSupport(Context context) {
        boolean support = false;
        CarrierConfigManager ccm = (CarrierConfigManager)context.getSystemService(
                Context.CARRIER_CONFIG_SERVICE);

        if (ccm != null) {
            PersistableBundle bundle = ccm.getConfig();
            if (bundle != null) {
                support = bundle.getBoolean("mtk_rtt_advaced_features_support_bool", false);
            }
        }

        Log.v(TAG, "CarrierConfig isAdvancedFeatureSupport = " + support);
        return (support || (SystemProperties.getInt("persist.rtt.advanced.features", 0) == 1));
    }
}

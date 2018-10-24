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

import android.content.Context;
import android.os.SystemVibrator;
import android.os.VibrationEffect;
import android.os.Vibrator;
import com.android.server.telecom.Call;
import com.android.server.telecom.CallState;
import com.android.server.telecom.CallsManagerListenerBase;

public class CallConnectedVibrator extends CallsManagerListenerBase {

    private final Vibrator mVibrator;
    private static final long VIBRATE_TIME_MILLIS = 200;
    private final static VibrationEffect EFFECT_CONNECTED =
            VibrationEffect.createOneShot(VIBRATE_TIME_MILLIS, VibrationEffect.DEFAULT_AMPLITUDE);

    public CallConnectedVibrator(Context context) {
        mVibrator = new SystemVibrator(context);
    }

    @Override
    public void onCallStateChanged(Call call, int oldState, int newState) {
        // CDMA framework will handle the CDMA vibrate by itself.
        /*/ freeme.liqiang, 20180323. add vibrate for GSM connection
        if (!call.isCdma()
                && (oldState == CallState.CONNECTING || oldState == CallState.DIALING)
                && newState == CallState.ACTIVE) {
            // TODO: checking the EngineerMode configuration
            mVibrator.vibrate(EFFECT_CONNECTED);
        }
        /*/
        if (!call.isCdma()
                && (oldState == CallState.CONNECTING || oldState == CallState.DIALING
                || oldState == CallState.RINGING)
                && newState == CallState.ACTIVE) {
            // TODO: checking the EngineerMode configuration
            // CDMA Phone controlled inside class MtkGsmCdmaConnection.java
            boolean isVibrate = android.provider.Settings.System.getInt(
                    call.getContext().getContentResolver(),
                    com.freeme.provider.FreemeSettings.System.FREEME_PHONE_VIBRAT_KEY, 0) == 1;
            if (!isVibrate) return;
            mVibrator.vibrate(EFFECT_CONNECTED);
        }
        //*/
    }
}

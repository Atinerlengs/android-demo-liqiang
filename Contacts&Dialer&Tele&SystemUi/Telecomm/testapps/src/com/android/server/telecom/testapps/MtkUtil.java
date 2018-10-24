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

import android.os.Bundle;
import android.telecom.Call;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import mediatek.telecom.MtkConnection;

public class MtkUtil {
    public static String makeTextForConnectionEvent(Call call, String event, Bundle extras) {
        StringBuilder sb = new StringBuilder();
        switch (event) {
            case MtkConnection.EVENT_SRVCC:
                sb.append("Simultaneous voice and real-time text calling is " +
                        "currently available only over high speed LTE and Wi-Fi " +
                        "networks. Real-time text may not be available over 2G/3G.");
                break;
            case MtkConnection.EVENT_CSFB:
                String number = call.getDetails().getHandle().getSchemeSpecificPart();
                if (PhoneNumberUtils.isEmergencyNumber(number)) {
                    sb.append("RTT is not available for this 911 call. Your call has been " +
                            "connected as a voice-only call. If you experience difficulties, " +
                            "please place a voice or relay call to 911.");
                } else {
                    sb.append("CSFB happened.");
                }
                break;
            case MtkConnection.EVENT_RTT_UPDOWN_FAIL:
                if (!call.isRttActive()) {
                    sb.append("RTT is not available for this call. Your call has " +
                            "been connected as a voice-only call.");
                }
                break;
            case MtkConnection.EVENT_RTT_EMERGENCY_REDIAL:
                sb.append("RTT is not available for this 911 call. Your call has been " +
                        "re-dialed as a voice-only call. If you experience difficulties " +
                        "with voice, please place a relay call to 911.");
                break;
            default:
                //do nothing
                break;
        }
        return sb.toString();
    }

    public static String makeTextRttModeChanged(Call call, int mode) {
        String modeText = "";
        switch (mode) {
            case Call.RttCall.RTT_MODE_INVALID:
                modeText = "RTT_MODE_INVALID";
                break;
            case Call.RttCall.RTT_MODE_FULL:
                modeText = "RTT_MODE_FULL";
                break;
            case Call.RttCall.RTT_MODE_HCO:
                modeText = "RTT_MODE_HCO";
                break;
            case Call.RttCall.RTT_MODE_VCO:
                modeText = "RTT_MODE_VCO";
                break;
            default:
                // do nothing
        }
        if (TextUtils.isEmpty(modeText)) {
            return "";
        }
        return String.format("RTT mode changed to %s, call id = %s, number = %s",
                modeText, call.getDetails().getTelecomCallId(), call.getDetails().getHandle());
    }
}

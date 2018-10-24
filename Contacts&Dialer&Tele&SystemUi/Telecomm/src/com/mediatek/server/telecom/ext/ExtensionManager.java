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

package com.mediatek.server.telecom.ext;

import android.content.Context;
import android.telecom.Log;

public final class ExtensionManager {

    private static final String LOG_TAG = "ExtensionManager";

    private static Context sApplicationContext;
    private static ICallMgrExt sCallMgrExt;
    private static IPhoneAccountExt sPhoneAccountExt;
    private static IRttUtilExt sRttUtilExt;
    private static IDigitsUtilExt sDigitsUtilExt;
    private static IGttUtilExt sGttUtilExt;

    private ExtensionManager() {
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    public static void registerApplicationContext(Context context) {
        if (sApplicationContext == null) {
            sApplicationContext = context;
        }
    }

    public static ICallMgrExt getCallMgrExt() {
        if (sCallMgrExt == null) {
            synchronized (ICallMgrExt.class) {
                if (sCallMgrExt == null) {
                    sCallMgrExt = OpTelecomCustomizationUtils
                            .getOpFactory(sApplicationContext).makeCallMgrExt();
                    log("[getCallMgrExt]create ext instance: " + sCallMgrExt);
                }
            }
        }

        return sCallMgrExt;
    }

    public static IPhoneAccountExt getPhoneAccountExt() {
        if (sPhoneAccountExt == null) {
            synchronized (IPhoneAccountExt.class) {
                if (sPhoneAccountExt == null) {
                    sPhoneAccountExt = OpTelecomCustomizationUtils
                            .getOpFactory(sApplicationContext).makePhoneAccountExt();
                    log("[getPhoneAccountExt]create ext instance: " + sPhoneAccountExt);
                }
            }
        }

        return sPhoneAccountExt;
    }

    public static IGttEventExt makeGttEventExt() {
        return CommonTelecomCustomizationUtils.getOpFactory(sApplicationContext).makeGttEventExt();
    }

    public static IRttUtilExt getRttUtilExt() {
        if (sRttUtilExt == null) {
            synchronized (IRttUtilExt.class) {
                if (sRttUtilExt == null) {
                    sRttUtilExt = CommonTelecomCustomizationUtils.getOpFactory(
                            sApplicationContext).makeRttUtilExt();
                    log("[getRttUtilExt] create ext instance: " + sRttUtilExt);
                }
            }
        }
        return sRttUtilExt;
    }

    public static IDigitsUtilExt getDigitsUtilExt() {
        if (sDigitsUtilExt == null) {
            synchronized (IDigitsUtilExt.class) {
                if (sDigitsUtilExt == null) {
                    sDigitsUtilExt = OpTelecomCustomizationUtils
                            .getOpFactory(sApplicationContext).makeDigitsUtilExt();
                    log("[getDigitsUtilExt] create ext instance: " + sDigitsUtilExt);
                }
            }
        }
        return sDigitsUtilExt;
    }

    public static IGttUtilExt getGttUtilExt() {
        if (sGttUtilExt == null) {
            synchronized (IGttUtilExt.class) {
                if (sGttUtilExt == null) {
                    sGttUtilExt = CommonTelecomCustomizationUtils.getOpFactory(
                            sApplicationContext).makeGttUtilExt();
                    log("[getGttUtilExt] create ext instance: " + sGttUtilExt);
                }
            }
        }
        return sGttUtilExt;
    }

    public static IRttEventExt makeRttEventExt() {
        return CommonTelecomCustomizationUtils.getOpFactory(sApplicationContext).makeRttEventExt();
    }
}

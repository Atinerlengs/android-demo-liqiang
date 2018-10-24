/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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
package com.mediatek.dialer.ext;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class ExtensionManager {
    private static final String TAG = "ExtensionManager";

    private static Context sContext;
    private static volatile ICallDetailExtension sCallDetailExtension;
    private static volatile IDialPadExtension sDialPadExtension;
    private static volatile ICallLogExtension sCallLogExtension;
    private static volatile IRCSeCallLogExtension sRCSeCallLogExtension;
    private static volatile IDialerSearchExtension sDialerSearchExtension;
    private static volatile IDialerUtilsExtension sDialerUtilsExtension;

    private static final Object sLockCallDetailExtension = new Object();
    private static final Object sLockDialPadExtension = new Object();
    private static final Object sLockCallLogExtension = new Object();
    private static final Object sLockRCSeCallLogExtension = new Object();
    private static final Object sLockDialerSearchExtension = new Object();
    private static final Object sLockDialerUtilsExtension = new Object();

    public static void init(Application application) {
        sContext = application.getApplicationContext();
    }

    public static ICallDetailExtension getCallDetailExtension() {
        if (sCallDetailExtension == null) {
            synchronized (sLockCallDetailExtension) {
                if (sCallDetailExtension == null) {
                    sCallDetailExtension = OpDialerCustomizationUtils
                            .getOpFactory(sContext).makeCallDetailExt();
                }
            }
        }
        return sCallDetailExtension;
    }

    public static IDialPadExtension getDialPadExtension() {
        if (sDialPadExtension == null) {
            synchronized (sLockDialPadExtension) {
                if (sDialPadExtension == null) {
                    sDialPadExtension = OpDialerCustomizationUtils
                            .getOpFactory(sContext).makeDialPadExt();
                    Log.i(TAG, "[getDialPadExtension]create ext instance: "
                            + sDialPadExtension);
                }
            }
        }
        return sDialPadExtension;
    }

    public static ICallLogExtension getCallLogExtension() {
        if (sCallLogExtension == null) {
            synchronized (sLockCallLogExtension) {
                if (sCallLogExtension == null) {
                    sCallLogExtension = OpDialerCustomizationUtils
                            .getOpFactory(sContext).makeCallLogExt();
                    Log.i(TAG, "[getCallLogAdapterExtension]create ext instance: "
                            + sCallLogExtension);
                }
            }
        }
        return sCallLogExtension;
    }

    public static IRCSeCallLogExtension getRCSeCallLogExtension() {
        if (sRCSeCallLogExtension == null) {
            synchronized (sLockRCSeCallLogExtension) {
                if (sRCSeCallLogExtension == null) {
                    sRCSeCallLogExtension = OpDialerCustomizationUtils
                            .getOpFactory(sContext).makeRCSeCallLogExt();
                    Log.i(TAG, "[getRCSeCallLogExtension]create ext instance: "
                            + sRCSeCallLogExtension);
                }
            }
        }
        return sRCSeCallLogExtension;
    }

    public static IDialerSearchExtension getDialerSearchExtension() {
        if (sDialerSearchExtension == null) {
            synchronized (sLockDialerSearchExtension) {
                if (sDialerSearchExtension == null) {
                    sDialerSearchExtension = OpDialerCustomizationUtils
                            .getOpFactory(sContext).makeDialerSearchExt();
                    Log.i(TAG, "[getDialerSearchExtension]create ext instance: "
                            + sDialerSearchExtension);
                }
            }
        }
        return sDialerSearchExtension;
    }

    public static IDialerUtilsExtension getDialerUtilsExtension() {
        if (sDialerUtilsExtension == null) {
            synchronized (sLockDialerUtilsExtension) {
                if (sDialerUtilsExtension == null) {
                    sDialerUtilsExtension = OpDialerCustomizationUtils
                            .getOpFactory(sContext).makeDialerUtilsExt();
                    Log.i(TAG, "[getDialerUtilsExtension]create ext instance: "
                            + sDialerUtilsExtension);
                }
            }
        }
        return sDialerUtilsExtension;
    }
}

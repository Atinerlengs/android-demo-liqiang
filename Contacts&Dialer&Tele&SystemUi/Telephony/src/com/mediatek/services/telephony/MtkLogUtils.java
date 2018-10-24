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

package com.mediatek.services.telephony;

import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;

/**
 * MTK Logging Utility Class
 *
 */
public class MtkLogUtils {

    private static final String TAG = "Telephony";

    private static final String KEY_FORCE_LOGGING_ON = "persist.log.tag.tel_dbg";

    public static void initLogging(Context context) {
        // Register Telephony with the Telecom Logger.
        setMtkTag(TAG);
        android.telecom.Log.setSessionContext(context);
        android.telecom.Log.initMd5Sum();
    }

    private static void setMtkTag(String tag) {
        android.telecom.Log.TAG = tag;
        if (Build.IS_ENG || (SystemProperties.getInt(KEY_FORCE_LOGGING_ON, 0) > 0)) {
            android.telecom.Log.ERROR = true;
            android.telecom.Log.WARN = true;
            android.telecom.Log.INFO = true;
            android.telecom.Log.DEBUG = true;
            android.telecom.Log.VERBOSE = true;
        } else {
            android.telecom.Log.ERROR = android.util.Log.isLoggable(tag, android.util.Log.ERROR);
            android.telecom.Log.WARN = android.util.Log.isLoggable(tag, android.util.Log.WARN);
            android.telecom.Log.INFO = android.util.Log.isLoggable(tag, android.util.Log.INFO);
            android.telecom.Log.DEBUG = android.util.Log.isLoggable(tag, android.util.Log.DEBUG);
            android.telecom.Log.VERBOSE = android.util.Log.isLoggable(tag, android.util.Log.VERBOSE);
        }
    }

}

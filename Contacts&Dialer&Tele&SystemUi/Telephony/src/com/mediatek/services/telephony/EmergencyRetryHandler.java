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
* have been modified by MediaTek Inc. All revisions are subject to any receiver\'s
* applicable license agreements with MediaTek Inc.
*/

package com.mediatek.services.telephony;

import android.os.SystemProperties;
import android.telecom.ConnectionRequest;
import android.telecom.PhoneAccountHandle;
import android.telephony.TelephonyManager;

import com.android.phone.PhoneUtils;
import com.android.services.telephony.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The emergency call retry handler.
 * Selected the proper Phone for setting up the ecc call.
 */
public class EmergencyRetryHandler {
    private static final String TAG = "ECCRetryHandler";
    private static final boolean DBG = true;

    private static final boolean MTK_CT_VOLTE_SUPPORT
            = "1".equals(SystemProperties.get("persist.mtk_ct_volte_support", "0"));

    private static final int MAX_NUM_RETRIES =
            TelephonyManager.getDefault().getPhoneCount() > 1 ?
            (TelephonyManager.getDefault().getPhoneCount() - 1) : (MTK_CT_VOLTE_SUPPORT ? 1 : 0);

    private ConnectionRequest mRequest = null;
    private int mNumRetriesSoFar = 0;
    private List<PhoneAccountHandle> mAttemptRecords;
    private Iterator<PhoneAccountHandle> mAttemptRecordIterator;
    private String mCallId = null;

    /**
     * Init the EmergencyRetryHandler.
     * @param request ConnectionRequest
     * @param initPhoneId PhoneId of the initial ECC
     */
    public EmergencyRetryHandler(ConnectionRequest request, int initPhoneId) {
        mRequest = request;
        mNumRetriesSoFar = 0;
        mAttemptRecords = new ArrayList<PhoneAccountHandle>();

        PhoneAccountHandle phoneAccountHandle;
        int num = 0;

        while (num <  MAX_NUM_RETRIES) {
            // 1. Add other phone rather than initPhone sequentially
            for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
                if (initPhoneId != i) {
                    // If No SIM is inserted, the corresponding IccId will be null,
                    // so take phoneId as PhoneAccountHandle::mId which is IccId originally
                    phoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(
                            Integer.toString(i));
                    mAttemptRecords.add(phoneAccountHandle);
                    num++;
                    log("Add #" + num + " to ECC retry list, " + phoneAccountHandle);
                }
            }

            // 2. Add initPhone at last
            phoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(
                    Integer.toString(initPhoneId));
            mAttemptRecords.add(phoneAccountHandle);
            num++;
            log("Add #" + num + " to ECC Retry list, " + phoneAccountHandle);
        }

        mAttemptRecordIterator = mAttemptRecords.iterator();
    }

    public void setCallId(String id) {
        log("setCallId, id=" + id);
        mCallId = id;
    }

    public String getCallId() {
        log("getCallId, id=" + mCallId);
        return mCallId;
    }

    public boolean isTimeout() {
        boolean isOut = (mNumRetriesSoFar >= MAX_NUM_RETRIES);
        log("isTimeout, timeout=" + isOut
                + ", mNumRetriesSoFar=" + mNumRetriesSoFar);
        return isOut;
    }

    public ConnectionRequest getRequest() {
        log("getRequest, request=" + mRequest);
        return mRequest;
    }

    public PhoneAccountHandle getNextAccountHandle() {
        if (mAttemptRecordIterator.hasNext()) {
            mNumRetriesSoFar++;
            log("getNextAccountHandle, next account handle exists");
            return mAttemptRecordIterator.next();
        }
        log("getNextAccountHandle, next account handle is null");
        return null;
    }

    private void log(String s) {
        Log.d(TAG, s);
    }
}

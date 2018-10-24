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

package com.mediatek.phone.ext;

import android.content.Context;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import com.android.internal.telephony.Connection;

public class DefaultDigitsUtilExt implements IDigitsUtilExt {
    @Override
    public PhoneAccountHandle convertToPstnPhoneAccountHandle(
            PhoneAccountHandle phoneAccountHandle, Context context) {
        return phoneAccountHandle;
    }

    @Override
    public PhoneAccountHandle convertToPstnPhoneAccount(PhoneAccount phoneAccount) {
        return phoneAccount.getAccountHandle();
    }

    @Override
    public String getVirtualLineNumber(PhoneAccountHandle phoneAccountHandle) {
        return "";
    }

    @Override
    public PhoneAccountHandle makeVirtualPhoneAccountHandle(String virtualLineNumber,
            String parentId) {
        return null;
    }

    @Override
    public boolean isVirtualPhoneAccount(PhoneAccountHandle phoneAccountHandle, Context context) {
        return false;
    }

    @Override
    public boolean isPotentialVirtualPhoneAccount(PhoneAccountHandle phoneAccountHandle) {
        return false;
    }

    @Override
    public Object replaceTelecomAccountRegistry(Object telecomAccountRegistry, Context context) {
        return telecomAccountRegistry;
    }

    @Override
    public Bundle putLineNumberToExtras(Bundle extras, Context context) {
        return extras;
    }

    @Override
    public boolean isConnectionMatched(Connection connection, PhoneAccountHandle
            phoneAccountHandle, Context context) {
        return true;
    }

    @Override
    public PhoneAccountHandle getCorrectPhoneAccountHandle(PhoneAccountHandle handle,
            PhoneAccountHandle memberHandle, Context context) {
        return handle;
    }

    @Override
    public void setPhoneAccountHandle(Object notifier, PhoneAccountHandle phoneAccountHandle) {
    }

    @Override
    public String getIccidFromPhoneAccountHandle(PhoneAccountHandle phoneAccountHandle) {
        return phoneAccountHandle.getId();
    }

    @Override
    public PhoneAccountHandle getHandleByConnectionIfRequired(PhoneAccountHandle phoneAccountHandle,
            Object connection) {
        return phoneAccountHandle;
    }

    @Override
    public boolean areConnectionsInSameLine(Object connections, Object conference) {
        return true;
    }
}

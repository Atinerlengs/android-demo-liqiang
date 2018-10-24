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
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.telecom.Log;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import com.android.server.telecom.Call;
import com.android.server.telecom.CallsManager;
import com.android.server.telecom.PhoneAccountRegistrar;
import com.android.server.telecom.TelecomSystem;
import com.mediatek.internal.telecom.IMtkTelecomService;
import mediatek.telecom.MtkTelecomManager;
import mediatek.telecom.MtkConnection;
import com.android.server.telecom.CallState;

import java.util.List;

public class MtkTelecomServiceImpl extends IMtkTelecomService.Stub {

    private static final String TAG = MtkTelecomServiceImpl.class.getSimpleName();

    private final Context mContext;
    private final CallsManager mCallsManager;
    private final TelecomSystem.SyncRoot mLock;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;

    /**
     *  @hide
     */
    public MtkTelecomServiceImpl(
            Context context,
            CallsManager callsManager,
            PhoneAccountRegistrar phoneAccountRegistrar,
            TelecomSystem.SyncRoot lock) {
        mContext = context;
        mCallsManager = callsManager;
        mPhoneAccountRegistrar = phoneAccountRegistrar;
        mLock = lock;

        publish();
    }

    private void publish() {
        Log.i(TAG, "[publish]adding to ServiceManager");
        // Note: need SELinux permission for adding service
        ServiceManager.addService(MtkTelecomManager.MTK_TELECOM_SERVICE_NAME, this);
    }

    @Override
    public boolean isInVideoCall(String callingPackage) throws RemoteException {
        try {
            Log.startSession("MTSI.iIVC");
            Log.i(TAG, "[isInVideoCall] from " + callingPackage);
            // TODO: Add permission check here.
            // MTK component(data, camera, etc) no need permission check.
            synchronized (mLock) {
                for (Call call : mCallsManager.getCalls()) {
                    if (!VideoProfile.isAudioOnly(call.getVideoState())) {
                        return true;
                    }
                }
            }
        } finally {
            Log.endSession();
        }
        return false;
    }

    /**
     * Fix CR:ALPS03200278,TTY Support part for op07.
     * M: return if current has live ongoing volte call or not;
     * @see android.telecom.TelecomManager#isInVolteCall
     */
    @Override
    public boolean isInVolteCall(String callingPackage) {
        try {
            Log.startSession("MTSI.iIVC");
            Log.i(TAG, "[isInVolteCall] from " + callingPackage);
            synchronized (mLock) {
                java.util.Collection<Call> calls = mCallsManager.getCalls();
                for (Call call : calls) {
                    if (call.hasProperty(MtkConnection.PROPERTY_VOLTE) &&
                            (call.getState() == CallState.DIALING
                                    || call.getState() == CallState.ACTIVE
                                    || call.getState() == CallState.RINGING
                                    || call.getState() == CallState.ON_HOLD)) {
                        return true;
                    }
                }
            }
            return false;
        } finally {
            Log.endSession();
        }
    }

    /*
     * TODO: Currently the same behavior as {@link TelecomManager#getAllPhoneAccounts()}
     * Introduce this API in case the [Digits] virtual line feature might need different
     * API behavior.
     */
    @Override
    public List<PhoneAccount> getAllPhoneAccountsIncludingVirtual() throws RemoteException {
        final UserHandle callingUserHandle = Binder.getCallingUserHandle();
        long token = Binder.clearCallingIdentity();
        try {
            Log.startSession("MTSI.gAPAIV");
            synchronized (mLock) {
                return mPhoneAccountRegistrar.getAllPhoneAccounts(callingUserHandle);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
            Log.endSession();
        }
    }

    /**
     * TODO: Currently the same behavior as {@link TelecomManager#getAllPhoneAccountHandles()}
     * Introduce this API in case the [Digits] virtual line feature might need different
     * API behavior.
     */
    @Override
    public List<PhoneAccountHandle> getAllPhoneAccountHandlesIncludingVirtual() throws RemoteException {
        final UserHandle callingUserHandle = Binder.getCallingUserHandle();
        long token = Binder.clearCallingIdentity();
        try {
            Log.startSession("MTSI.gAPAHIV");
            synchronized (mLock) {
                return mPhoneAccountRegistrar.getAllPhoneAccountHandles(callingUserHandle);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
            Log.endSession();
        }
    }

    /**
     * @see mediatek.telecom.MtkTelecomManager#isInCall
     */
    @Override
    public boolean isInCall(String callingPackage) {
        try {
            Log.startSession("MTSI.iIC");
            // TODO: Add permission check here.

            synchronized (mLock) {
                return mCallsManager.hasOngoingCallsEx();
            }
        } finally {
            Log.endSession();
        }
    }
}

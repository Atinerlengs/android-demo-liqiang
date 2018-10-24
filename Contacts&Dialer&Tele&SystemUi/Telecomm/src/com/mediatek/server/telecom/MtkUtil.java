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

import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.sip.SipManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.telecom.Log;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.server.telecom.TelephonyUtil;
import mediatek.telecom.MtkTelecomManager;
import mediatek.telecom.MtkPhoneAccount;
import android.os.UserHandle;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Collections;

import com.android.server.telecom.Call;
import com.android.server.telecom.PhoneAccountRegistrar;
import com.android.server.telecom.R;
import com.android.server.telecom.TelecomSystem;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import com.mediatek.internal.telephony.MtkSubscriptionInfo;
import android.telecom.DisconnectCause;
import com.android.server.telecom.components.ErrorDialogActivity;
import android.text.TextUtils;

import mediatek.telephony.MtkCarrierConfigManager;

public class MtkUtil {
    private static final String TAG = MtkUtil.class.getSimpleName();
    private static Boolean sSipSupport = null;
    // add to enable specify a slot to MO.
    // using cmd:adb shell am start -a android.intent.action.CALL
    // -d tel:10010 --ei com.android.phone.extra.slot 1
    public static final String EXTRA_SLOT = "com.android.phone.extra.slot";

    private static final ComponentName AOSP_PSTN_COMPONENT_NAME = new ComponentName(
            "com.android.phone", "com.android.services.telephony.TelephonyConnectionService");
    private static final ComponentName MTK_PSTN_COMPONENT_NAME = new ComponentName(
            "com.android.phone", "com.mediatek.services.telephony.MtkTelephonyConnectionService");

    public static boolean canConference(String callingPackageName, String msg) {
        if (checkConferencePermission(callingPackageName, msg)) {
            return true;
        }
        MtkTelecomGlobals.getInstance().showToast(R.string.denied_required_permission);
        return false;
    }

    /**
     * This can only be called before binder information cleared.
     * Because the CtaUtils.enforceCheckPermission need to get the calling uid.
     */
    private static boolean checkConferencePermission(String callingPackageName, String msg) {
        try {
            ReflectionHelper.callStaticMethod(
                    "com.mediatek.cta.CtaUtils", /* class */
                    "enforceCheckPermission", /* static method */
                    callingPackageName, /* param 1 */
                    "com.mediatek.permission.CTA_CONFERENCE_CALL", /* param 2 */
                    msg); /* param 3 */
            // The result is not important.
            // Reflection failed means no need CTA checking, then the conference should be allowed.
            // Reflection succeeded and no SecurityException raised means the permission check pass
            // Only when SecurityException raised means permission check failed, no conference then
            return true;
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof SecurityException) {
                Log.w(TAG, "[canConference]No permission to make conference call for %s via %s",
                        callingPackageName, msg);
            } else {
                Log.w(TAG, "[canConference]Unexpected exception happened: %s",
                        e.getTargetException());
                e.getTargetException().printStackTrace();
            }
            return false;
        }
    }

    public static boolean canVoiceRecord(String callingPackageName, String msg) {
        if (checkCallingPermission(
                MtkTelecomGlobals.getInstance().getContext(),
                android.Manifest.permission.RECORD_AUDIO,
                callingPackageName, msg)) {
            return true;
        }

        MtkTelecomGlobals.getInstance().showToast(R.string.denied_required_permission);
        return false;
    }

    /**
     * M: Check caller's permission. Only for AOSP permission checking.
     *
     * @param context The application context
     * @param permission The permission to check
     * @param callingPackageName The caller's package name
     * @param msg The message print in the log if failed.
     * @return has permission or not.
     */
    private static boolean checkCallingPermission(Context context, String permission,
                                                  String callingPackageName, String msg) {
        try {
            /// This checking is only valid for SDK >= 23 callers. For earlier ones, it would pass
            /// directly.
            context.enforceCallingPermission(permission, msg);
        } catch (SecurityException e) {
            Log.e(TAG, e, "[checkCallerPermission]Permission checking failed for" +
                    " SDK level >= 23 caller. Permission: "+ permission);
            return false;
        }

        /// Reach here means a SDK >= 23 caller passed the checking OR the caller just a SDK < 23
        /// one. So we need to double check with the AppOpManager.
        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(
                Context.APP_OPS_SERVICE);
        if (appOpsManager == null) {
            Log.w(TAG, "[checkCallingPermission]Failed to get AppOpsManager");
            return false;
        }
        String op = AppOpsManager.permissionToOp(permission);
        int opMode = appOpsManager.noteOp(op, Binder.getCallingUid(), callingPackageName);
        Log.d(TAG, "[checkCallingPermission]permission: " + permission + " -> op: " + op
                + ", checking mode = " + opMode);
        return opMode == AppOpsManager.MODE_ALLOWED;
    }

    public static boolean isConferenceInvitation(Bundle extras) {
        return extras == null ? false :
                extras.getBoolean(MtkTelecomManager.EXTRA_START_VOLTE_CONFERENCE, false);
    }

    public static boolean isInDsdaMode() {
        // TODO: need performance optimize
        return com.mediatek.telephony.MtkTelephonyManagerEx.getDefault().isInDsdaMode();
    }

    /**
     * This SingleVideoCallMode means only one video call allowed.
     * All the 3G VT projects allow one video call living.
     * In some operator network, only one ViLTE call allowed, too.
     * In such mode:
     *  1. If there is a call, no new video call allowed.
     *  2. The ongoing video call can't be put on hold.
     *  3. No new call allowed when there was an ongoing video call.
     *  4. Disallow upgrade to video call if other call exists.
     *  5. Multi-call -> Single call, the capabilities should revert.
     */
    public static boolean isInSingleVideoCallMode(Call newCall) {
        boolean result = false;
        Context context = MtkTelecomGlobals.getInstance().getContext();
        PhoneAccountHandle accountHandle = newCall.getTargetPhoneAccount();
        if (context != null && accountHandle != null) {
            int subId = getSubIdForPhoneAccountHandle(accountHandle);
            CarrierConfigManager carrierConfig =
                    (CarrierConfigManager)context.getSystemService(Context.CARRIER_CONFIG_SERVICE);
            PersistableBundle bundle = carrierConfig.getConfigForSubId(subId);
            if (bundle != null) {
                result = bundle.getBoolean(
                        MtkCarrierConfigManager.MTK_KEY_ALLOW_ONE_VIDEO_CALL_ONLY_BOOL);
            }
        }
        Log.d(TAG, "isInSingleVideoCallMode()...result = %s", result);
        return result;
    }

    public static boolean is3GVtEnabled() {
        return false;
    }

    public static int getSubIdForPhoneAccountHandle(PhoneAccountHandle phoneAccountHandle) {
        if (!TelephonyUtil.isPstnComponentName(phoneAccountHandle.getComponentName())) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }
        TelephonyManager telephonyManager = (TelephonyManager) MtkTelecomGlobals.getInstance()
                .getContext().getSystemService(Context.TELEPHONY_SERVICE);
        TelecomManager telecomManager = (TelecomManager) MtkTelecomGlobals.getInstance()
                .getContext().getSystemService(Context.TELECOM_SERVICE);
        // Optimization: Access PhoneAccount from PhoneAccountRegistrar first; if access
        // PhoneAccountRegistrar fail then try to use TelecomManager.getPhoneAccount.
        PhoneAccount phoneAccount;
        TelecomSystem telecomSystem = TelecomSystem.getInstance();
        if (telecomSystem != null) {
            phoneAccount = telecomSystem.getPhoneAccountRegistrar()
                    .getPhoneAccountOfCurrentUser(phoneAccountHandle);
        } else {
            phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandle);
        }
        if (phoneAccount == null) {
            // [ALPS03454821]In SELDOM case, like ALPS03454821 demonstrated, The
            // PhoneAccount has not been registered when it was reported to Telecom.
            // In such scenario, we have to double confirming that whether can we
            // retrieve the subId in some other way instead, like via SubscriptionManager.@{
             Log.w(TAG, "[getSubId]PhoneAccount not registered,"
                     + " try SubscriptionManager, the iccId = " + phoneAccountHandle.getId());
             int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
             MtkSubscriptionInfo subscriptionInfo =
                     MtkSubscriptionManager.getSubInfoForIccId(null, phoneAccountHandle.getId());
             if (subscriptionInfo != null) {
                 subId = subscriptionInfo.getSubscriptionId();
                 Log.d(TAG, "[getPhoneId]get subId from SubscriptionManager: " + subId);
             }
             return subId;
             // @}
        }
        return telephonyManager.getSubIdForPhoneAccount(phoneAccount);
    }

    public static boolean isIncomingConferenceCall(Bundle extras) {
        boolean result = false;
        if (extras != null &&
                extras.containsKey(MtkTelecomManager.EXTRA_INCOMING_VOLTE_CONFERENCE)) {
            result = extras.getBoolean(MtkTelecomManager.EXTRA_INCOMING_VOLTE_CONFERENCE, false);
        }
        Log.d(TAG, "[isIncomingConferenceCall]: " + result);
        return result;
    }

    public static List<String> getConferenceInvitationNumbers(Bundle extras) {
        if (extras != null
                && extras.containsKey(MtkTelecomManager.EXTRA_VOLTE_CONFERENCE_NUMBERS)) {
            return extras.getStringArrayList(MtkTelecomManager.EXTRA_VOLTE_CONFERENCE_NUMBERS);
        }
        return null;
    }

    public static boolean isSipSupported() {
        if (sSipSupport == null) {
            sSipSupport = SipManager.isVoipSupported(MtkTelecomGlobals.getInstance().getContext());
        }
        return sSipSupport;
    }

    public static int cliValidityToPresentation(int cliValidity) {
        int presentation = -1;
        switch (cliValidity) {
            case 0:
                presentation = TelecomManager.PRESENTATION_ALLOWED;
                break;
            case 1:
                presentation = TelecomManager.PRESENTATION_RESTRICTED;
                break;
            case 2:
                presentation = TelecomManager.PRESENTATION_UNKNOWN;
                break;
            default:
                break;
        }
        return presentation;
    }

    public static void showOutgoingFailedToast(String reason) {
        Log.i(TAG, "[showOutgoingFailedToast] call failed: %s", reason);
        MtkTelecomGlobals.getInstance().showToast(R.string.outgoing_call_failed);
    }

    public static String dumpBundle(Bundle bundle) {
        if (bundle == null) {
            return "null Bundle";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Bundle[");
        bundle.keySet().stream()
                .forEach(k -> sb.append(k.toString()).append(": ")
                        .append(bundle.get(k.toString())).append(", "));
        sb.append("]");
        return sb.toString();
    }

    /**
     * This function used to get PhoneAccountHandle(s), which is sim based.
     * @return
     */
    public static List<PhoneAccountHandle> getSimPhoneAccountHandles() {
        List<PhoneAccountHandle> simPhoneAccountHandles = new ArrayList<PhoneAccountHandle>();
        PhoneAccountRegistrar phoneAccountRegistrar = TelecomSystem.getInstance().
                getPhoneAccountRegistrar();
        if (phoneAccountRegistrar != null) {
            simPhoneAccountHandles.addAll(
                    phoneAccountRegistrar.getSimPhoneAccountsOfCurrentUser());
        }
        return simPhoneAccountHandles;
    }

    /**
     * This function used to get PhoneAccountHandle by slot id.
     * @param context
     * @param slotId
     * @return
     */
    public static PhoneAccountHandle getPhoneAccountHandleWithSlotId(Context context,
            int slotId, PhoneAccountHandle defaultPhoneAccountHandle) {
        PhoneAccountHandle result = defaultPhoneAccountHandle;
        if (SubscriptionManager.isValidSlotIndex(slotId)) {
            SubscriptionInfo subInfo = SubscriptionManager.from(context)
                    .getActiveSubscriptionInfoForSimSlotIndex(slotId);
            List<PhoneAccountHandle> phoneAccountHandles = getSimPhoneAccountHandles();
            if (subInfo != null && phoneAccountHandles != null && !phoneAccountHandles.isEmpty()) {
                for (PhoneAccountHandle accountHandle : phoneAccountHandles) {
                    if (Objects.equals(accountHandle.getId(), subInfo.getIccId())) {
                        result = accountHandle;
                        break;
                    }
                }
            }
        }
        Log.d(TAG, "getPhoneAccountHandleWithSlotId()... slotId = %s; account changed: %s => %s",
                slotId, defaultPhoneAccountHandle, result);
        return result;
    }

    /**
     * Check whether the disconnect call is a mmi dial request with data off case.
     * @param disconnectCause use this info to check
     */
    public static boolean isMmiWithDataOff(DisconnectCause disconnectCause) {
        boolean result = false;
        if (disconnectCause != null) {
            int disconnectCode = disconnectCause.getCode();
            String disconnectReason = disconnectCause.getReason();
            if (disconnectCode == DisconnectCause.ERROR && !TextUtils.isEmpty(disconnectReason)
                    && disconnectReason.contains(
                    MtkTelecomManager.DISCONNECT_REASON_VOLTE_SS_DATA_OFF)) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Notify user to open data connection.
     * @param context
     * @param phoneAccountHandle
     */
    public static void showNoDataDialog(Context context, PhoneAccountHandle phoneAccountHandle) {
        int subId = getSubIdForPhoneAccountHandle(phoneAccountHandle);

        if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            /**
             * M: ALPS03564343.
             * CMCC Spec request the prompt message for failed MMI calling shuold be same with call
             * setting.
             * We change the method to get the prompt message: get string value from package
             * "com.android.phone" which call setting also get string from.
             * @{
             */
            Context telephonyContext = null;
             try {
                telephonyContext = context.createPackageContext("com.android.phone", Context.CONTEXT_IGNORE_SECURITY);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, e, "showNoDataDialog() - Can't find Telephony package.");
            }
            String errorMessage = null;
            if (null != telephonyContext) {
                int strId = telephonyContext.getResources().getIdentifier("volte_ss_not_available_tips_data",
                        "string", "com.android.phone");
                errorMessage = telephonyContext.getString(strId, getSubDisplayName(context, subId));
            } else {
                errorMessage = context.getString(R.string.volte_ss_not_available_tips,
                        getSubDisplayName(context, subId));
            }
            /**@}*/
            final Intent errorIntent = new Intent(context, ErrorDialogActivity.class);
            errorIntent.putExtra(ErrorDialogActivity.ERROR_MESSAGE_STRING_EXTRA, errorMessage);
            errorIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivityAsUser(errorIntent, UserHandle.CURRENT);
        }
    }

    /**
     * Get the sub's display name.
     * @param subId the sub id
     * @return the sub's display name, may return null
     */
    private static String getSubDisplayName(Context context, int subId) {
        String displayName = "";
        SubscriptionInfo subInfo = SubscriptionManager.from(context).getActiveSubscriptionInfo(subId);
        if (subInfo != null) {
            displayName = subInfo.getDisplayName().toString();
        }
        if (TextUtils.isEmpty(displayName)) {
            Log.d(TAG,"getSubDisplayName()... subId / subInfo: " + subId + " / " + subInfo);
        }
        return displayName;
    }

    /**
     * fix CR:ALPS03416900,To make the phoneaccount selection UI
     * show the accounts in ascend sequence,
     * we sort the PhoneAccount by slotId ascend.
     * since slotId is start from 0, so we only need to
     * put the PhoneAccount object to an ArrayList with
     * the index as its slotId.
     */
    public static void sortPhoneAccountsBySortKeyAscend(List<PhoneAccount> phoneAccounts) {
        Log.d(TAG, "sortPhoneAccountsBySortKeyAscend");
        if (phoneAccounts == null || phoneAccounts.size() <= 1) {
            return;
        }

        List<PhoneAccount> sortedList = new ArrayList<PhoneAccount>();
        sortedList.addAll(phoneAccounts);

        Collections.sort(sortedList, new java.util.Comparator<PhoneAccount>() {
            @Override
            public int compare(PhoneAccount a, PhoneAccount b) {
                int aSortKey = a.getExtras() == null ? Integer.MAX_VALUE :
                        a.getExtras().getInt(MtkPhoneAccount.EXTRA_PHONE_ACCOUNT_SORT_KEY,
                                Integer.MAX_VALUE);
                int bSortKey = b.getExtras() == null ? Integer.MAX_VALUE :
                        b.getExtras().getInt(MtkPhoneAccount.EXTRA_PHONE_ACCOUNT_SORT_KEY,
                                Integer.MAX_VALUE);
                return aSortKey - bSortKey;
            }
        });

        if (sortedList.size() > 0) {
            phoneAccounts.clear();
            phoneAccounts.addAll(sortedList);
        }
    }

    /*
     * In some cases, like MOTA, there might be some MTK PhoneAccountHandle user.
     * In android o0, the MTK PhoneAccountHandle might have been saved in CallLog db or marked as
     * user selected outgoing PhoneAccount. But in MTK o1, the PhoneAccount ComponentName changed
     * to AOSP version. For compatible, we should covert these MTK PhoneAccountHandle to AOSP one
     * to make it possible to get the upgraded result.
     * In another word, covert the MTK PhoneAccountHandle to AOSP version before use it.
     */
    public static PhoneAccountHandle compatConvertPhoneAccountHandle(PhoneAccountHandle handle) {
        if (handle != null
                && Objects.equals(handle.getComponentName(), MTK_PSTN_COMPONENT_NAME)) {
            return new PhoneAccountHandle(
                    AOSP_PSTN_COMPONENT_NAME, handle.getId(), handle.getUserHandle());
        }
        return handle;
    }

    /**
     * M: Checking the original Intent data scheme. If a Uri number was specified to be "tel:",
     * It would be considered as an Telephony uri number other than sip.
     * Telephony Uri numbers is typically used as IMS numbers.
     */
    public static boolean isImsCallIntent(Intent intent) {
        if (intent == null || intent.getData() == null) {
            return false;
        }
        Uri handle = intent.getData();
        boolean ret = PhoneNumberUtils.isUriNumber(handle.getSchemeSpecificPart())
                && PhoneAccount.SCHEME_TEL.equals(handle.getScheme());
        if (ret) {
            Log.d(TAG, "[isImsCallIntent] Dealing with IMS number with @");
        }
        return ret;
    }
}

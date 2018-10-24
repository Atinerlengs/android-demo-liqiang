package com.freeme.dialer.utils;

import android.content.Context;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import java.util.List;

public class FreemeDialerUtils {
    public static PhoneAccountHandle getPhoneAccountHandleBySlot(Context c, int slot) {
        TelecomManager telecomManager = TelecomManager.from(c);
        TelephonyManager telephonyManager = TelephonyManager.from(c);
        List<PhoneAccountHandle> mPhoneAccountHandles = telecomManager.getAllPhoneAccountHandles();
        for (PhoneAccountHandle account : mPhoneAccountHandles) {
            PhoneAccount phoneAccount = telecomManager.getPhoneAccount(account);
            int subId = telephonyManager.getSubIdForPhoneAccount(phoneAccount);
            SubscriptionInfo sir = SubscriptionManager.from(c).getActiveSubscriptionInfo(subId);
            if (sir != null && slot == sir.getSimSlotIndex()) {
                return account;
            }
        }
        return null;
    }

    public static PhoneAccountHandle getDefaultSmartDialAccount(Context context) {
        PhoneAccountHandle defaultPhoneAccountHandle = null;
        final TelecomManager telecomManager = TelecomManager.from(context);
        final List<PhoneAccountHandle> phoneAccountsList = telecomManager.getCallCapablePhoneAccounts();
        if (phoneAccountsList.size() == 1) {
            defaultPhoneAccountHandle = phoneAccountsList.get(0);
        } else if (phoneAccountsList.size() > 1) {
            defaultPhoneAccountHandle = telecomManager.getUserSelectedOutgoingPhoneAccount();
            if (defaultPhoneAccountHandle == null) {
                defaultPhoneAccountHandle = phoneAccountsList.get(0);
            }
        }
        return defaultPhoneAccountHandle;
    }

    public static final boolean supportDualSimCustomRingtone() {
        return com.freeme.util.FreemeOption.FREEME_DUAL_SIM_RINGTONE_SUPPORT;
    }

    public static final int getSimCount(Context context) {
        List<SubscriptionInfo> infos = SubscriptionManager.from(context)
                .getActiveSubscriptionInfoList();
        return infos == null ? 0 : infos.size();
    }
}
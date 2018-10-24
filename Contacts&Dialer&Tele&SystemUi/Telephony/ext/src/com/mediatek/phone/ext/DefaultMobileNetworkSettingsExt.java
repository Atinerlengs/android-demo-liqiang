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

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.IntentFilter;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;

import com.android.internal.telephony.Phone;
/**
 * Operator requirements add implement for NetworkSettings UI.
 * opeator like China Mobile,China Telecom, China Unicom, etc.
 */
public class DefaultMobileNetworkSettingsExt implements IMobileNetworkSettingsExt {

    @Override
    public void initOtherMobileNetworkSettings(PreferenceActivity activity, int subId) {
    }

    @Override
    public void initMobileNetworkSettings(PreferenceActivity activity, int currentTab) {
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return false;
    }

    @Override
    public void updateLTEModeStatus(ListPreference preference) {
    }

    @Override
    public void updateNetworkTypeSummary(ListPreference preference) {
    }

    @Override
    public void customizeAlertDialog(Preference preference, AlertDialog.Builder builder) {
    }

    @Override
    public void customizePreferredNetworkMode(ListPreference listPreference, int subId) {
    }

    @Override
    public void onPreferenceChange(Preference preference, Object objValue) {
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void unRegister() {
    }

    @Override
    public boolean isCtPlugin() {
        return false;
    }

    @Override
    public void changeEntries(ListPreference buttonEnabledNetworks) {
    }

    @Override
    public void updatePreferredNetworkValueAndSummary(ListPreference buttonEnabledNetworks,
            int networkMode) {
    }

    @Override
    public void customizeEnhanced4GLteSwitchPreference(PreferenceActivity prefAct,
                           SwitchPreference switchPreference) {
       Log.d("DefaultMobileNetworkSettingsExt", "customizeEnhanced4GLteSwitchPreference");
    }

    @Override
    public boolean isNetworkUpdateNeeded(ListPreference buttonEnabledNetworks,
            int networkMode, int currentMode, Phone phone,
            ContentResolver cr,
            int phoneSubId, Handler handler) {
        return true;
    }

    @Override
    public boolean isNetworkModeSettingNeeded() {
        return true;
    }

    @Override
    public boolean isEnhancedLTENeedToAdd(boolean defaultValue, int phoneId) {
        return defaultValue;
    }


    @Override
    public boolean customizeDualVolteOpDisable(int subId, boolean enableForCtVolte) {
        return enableForCtVolte;
    }

    @Override
    public void customizeDualVolteIntentFilter(IntentFilter intentFilter) {
    }

    @Override
    public boolean customizeDualVolteReceiveIntent(String action) {
        return false;
    }

    @Override
    public void customizeDualVolteOpHide(PreferenceScreen preferenceScreen,
            Preference preference, boolean showPreference) {
    }

    @Override
    public boolean customizeCUVolte() {
        return false;
    }

    @Override
    public boolean isNetworkChanged(ListPreference buttonEnabledNetworks, int networkMode,
            int currentMode, Phone phone) {
        return false;
    }
}

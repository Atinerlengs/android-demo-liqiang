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

import com.android.internal.telephony.Phone;
/**
 * Operator requirements add interface for NetworkSettings UI.
 * opeator like China Mobile,China Telecom, China Unicom, etc.
 */
public interface IMobileNetworkSettingsExt {
    /**
     * called in onCreate() of the Activity
     * Plug-in can init itself, preparing for it's function
     * @param activity the MobileNetworkSettings activity
     * @param subId sub id
     */
    void initOtherMobileNetworkSettings(PreferenceActivity activity, int subId);

    /**
     * called in onCreate() of the Activity.
     * Plug-in can init itself, preparing for it's function
     * @param activity the MobileNetworkSettings activity
     * @param currentTab current Tab
     */
    void initMobileNetworkSettings(PreferenceActivity activity, int currentTab);

    /**
     * Attention, returning false means nothing but telling host to go on its own flow.
     * host would never return plug-in's "false" to the caller of onPreferenceTreeClick()
     *
     * @param preferenceScreen the clicked preference screen
     * @param preference the clicked preference
     * @return true if plug-in want to skip host flow. whether return true or false, host will
     * return true to its real caller.
     * @internal
     */
    boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference);

    /**
     * This interface is for updating the MobileNetworkSettings' item "Preferred network type"
     * @param preference there are two cases:
     *                   1. mButtonPreferredNetworkMode in host APP
     *                   2. mButtonEnabledNetworks in host APP
     */
    void updateNetworkTypeSummary(ListPreference preference);

    /**
     * TODO: Clear about what is this interface for
     * @param preference
     */
    void updateLTEModeStatus(ListPreference preference);

    /**
     * Allow Plug-in to customize the AlertDialog passed.
     * This API should be called right before builder.create().
     * Plug-in should check the preference to determine how the Dialog should act.
     * @param preference the clicked preference
     * @param builder the AlertDialog.Builder passed from host APP
     */
    void customizeAlertDialog(Preference preference, AlertDialog.Builder builder);


    /**
     * Update the ButtonPreferredNetworkMode's summary and enable when sim2 is CU card.
     * @param listPreference ButtonPreferredNetworkMode
     */
    void customizePreferredNetworkMode(ListPreference listPreference, int subId);

    /**
     * Preference Change, update network preference value and summary
     * @param preference the clicked preference
     * @param objValue choose obj value
     */
    void onPreferenceChange(Preference preference, Object objValue);

    /**
     * For Plug-in to update Preference.
     */
    void onResume();

    /**
     * For Plug-in to update Preference.
     */
    void onPause();

    /**
     * For Plug-in to pause event and listener registration.
     */
    void unRegister();

    /**
     * for CT feature , CT Plug-in should return true.
     * @return true,if is CT Plug-in
     */
    boolean isCtPlugin();

    /**
     * For changing entry names in list preference dialog box.
     * @param buttonEnabledNetworks list preference
     */
    void changeEntries(ListPreference buttonEnabledNetworks);

    /**
     * For updating network mode and summary.
     * @param buttonEnabledNetworks list preference
     * @param networkMode network mode
     */
    void updatePreferredNetworkValueAndSummary(ListPreference buttonEnabledNetworks,
            int networkMode);

     /**
     * For updating Enhanced4GLteSwitchPreference.
     * @param prefAct PreferenceActivity
     * @param switchPreference SwitchPreference
     */
    void customizeEnhanced4GLteSwitchPreference(PreferenceActivity prefAct,
                           SwitchPreference switchPreference);

    /**
     * Confirm with user to update Network.
     * @param buttonEnabledNetworks list preference
     * @param networkMode network mode selected
     * @param currentMode current network mode
     * @param phone Phone on which UE will handle
     * @param cr Content resolver of Settings
     * @param phoneSubId SubID on which change occurred
     * @param handler Handler to take care of NEtwork update
     * @return false if operator handle
     * @internal
     */
    boolean isNetworkUpdateNeeded(ListPreference buttonEnabledNetworks,
            int networkMode, int currentMode, Phone phone,
            ContentResolver cr,
            int phoneSubId, Handler handler);

    /**
     * Confirm with user to show updated network mode.
     * @return false if operator handle
     * @internal
     */
    boolean isNetworkModeSettingNeeded();

    /**
     * Confirm with user to add Enhanced LTE Services.
     * @param defaultValue currently volte service option is visible or not
     * @param phoneId phoneid on which UE check for Volte services
     * @return false if operator handle
     * @internal
     */
    boolean isEnhancedLTENeedToAdd(boolean defaultValue, int phoneId);


    /**
     * For CMCC dual VOLTE feature.
     * @param subId sub id
     * @param enableForCtVolte enhance4glte state.
     * @return true if this is CMCC card.
     */
    boolean customizeDualVolteOpDisable(int subId, boolean enableForCtVolte);

    /**
     * For CMCC VOLTE feature.
     * when is CMCC card, VOLTE show enable.
     * else VOLTE show disable.
     * SIM_STATE_CHANGED broadcast register.
     * @param intentFilter SIM_STATE_CHANGED.
     */
    void customizeDualVolteIntentFilter(IntentFilter intentFilter);

    /**
     * For CMCC VOLTE feature.
     * when is CMCC card, VOLTE show enable.
     * else VOLTE show disable.
     * SIM_STATE_CHANGED broadcast dual with.
     * @param action SIM_STATE_CHANGED.
     * @return true if SIM_STATE_CHAGNED.
     */
    boolean customizeDualVolteReceiveIntent(String action);

    /**
     * For CMCC VOLTE feature.
     * when is CMCC card ,VOLTE show.
     * else VOLTE button hide.
     * @param preferenceScreen Mobile preferenceScreen
     * @param preference mEnhancedButton4glte
     * @param showPreference if true means CMCC card, need show item
     */
    void customizeDualVolteOpHide(PreferenceScreen preferenceScreen,
            Preference preference, boolean showPreference);

    /**
     * For CU votle feature.
     * @return
     */
    boolean customizeCUVolte();

    /**
     * For TMO US to differ between LTE (Auto) and LTE/3G.
     * @param buttonEnabledNetworks list preference
     * @param networkMode network mode selected
     * @param currentMode current network mode
     * @param phone Phone on which UE will handle
     * @return true if mode changed else ignore
     */
    boolean isNetworkChanged(ListPreference buttonEnabledNetworks,
            int networkMode, int currentMode, Phone phone);
}

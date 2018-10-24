/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.phone;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.ims.ImsConfig;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.ims.ImsConnectionStateListener;
import com.android.ims.ImsReasonInfo;
import com.android.ims.ImsServiceClass;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.phone.PhoneGlobals.SubInfoUpdateListener;
import com.android.phone.settings.PhoneAccountSettingsFragment;
import com.android.phone.settings.VoicemailSettingsActivity;
import com.android.phone.settings.fdn.FdnSetting;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.phone.ext.DefaultCallFeaturesSettingExt;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.phone.ext.ICallFeaturesSettingExt;
import com.mediatek.ims.config.ImsConfigContract;
import com.mediatek.ims.internal.MtkImsManager;
import com.mediatek.settings.CallBarring;
import com.mediatek.settings.CallSettingUtils;
import com.mediatek.settings.TelephonyUtils;
import com.mediatek.settings.cdg.CdgCallSettings;
import com.mediatek.settings.cdg.CdgUtils;
import com.mediatek.settings.cdma.CdmaCallForwardOptions;
import com.mediatek.settings.cdma.CdmaCallWaitOptions;
import com.mediatek.settings.vtss.GsmUmtsVTCBOptions;
import com.mediatek.settings.vtss.GsmUmtsVTCFOptions;

import com.mediatek.settings.cdma.CdmaCallWaitingUtOptions;
import com.mediatek.settings.cdma.TelephonyUtilsEx;

import java.util.List;

import mediatek.telephony.MtkCarrierConfigManager;

//*/ freeme.liqiang, 20180320. modify back title in dialer settings
import android.text.TextUtils;
import com.freeme.actionbar.app.FreemeActionBarUtil;
//*/

/**
 * Top level "Call settings" UI; see res/xml/call_feature_setting.xml
 *
 * This preference screen is the root of the "Call settings" hierarchy available from the Phone
 * app; the settings here let you control various features related to phone calls (including
 * voicemail settings, the "Respond via SMS" feature, and others.)  It's used only on
 * voice-capable phone devices.
 *
 * Note that this activity is part of the package com.android.phone, even
 * though you reach it from the "Phone" app (i.e. DialtactsActivity) which
 * is from the package com.android.contacts.
 *
 * For the "Mobile network settings" screen under the main Settings app,
 * See {@link MobileNetworkSettings}.
 *
 * @see com.android.phone.MobileNetworkSettings
 */
public class CallFeaturesSetting extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener,
                SubInfoUpdateListener {
    private static final String LOG_TAG = "CallFeaturesSetting";
    private static final boolean DBG = true; //(PhoneGlobals.DBG_LEVEL >= 2);

    // String keys for preference lookup
    // TODO: Naming these "BUTTON_*" is confusing since they're not actually buttons(!)
    // TODO: Consider moving these strings to strings.xml, so that they are not duplicated here and
    // in the layout files. These strings need to be treated carefully; if the setting is
    // persistent, they are used as the key to store shared preferences and the name should not be
    // changed unless the settings are also migrated.
    private static final String VOICEMAIL_SETTING_SCREEN_PREF_KEY = "button_voicemail_category_key";
    private static final String BUTTON_FDN_KEY   = "button_fdn_key";
    private static final String BUTTON_RETRY_KEY       = "button_auto_retry_key";
    private static final String BUTTON_GSM_UMTS_OPTIONS = "button_gsm_more_expand_key";
    private static final String BUTTON_CDMA_OPTIONS = "button_cdma_more_expand_key";
    private static final String MULTI_IMS_SUPPORT = "persist.mtk_mims_support";

    /// M: add for call private voice feature @{
    private static final String BUTTON_CP_KEY = "button_voice_privacy_key";
    /// @}

    private static final String CALL_FORWARDING_KEY = "call_forwarding_key";
    private static final String ADDITIONAL_GSM_SETTINGS_KEY = "additional_gsm_call_settings_key";
    /// M: GSM type phone call settings item --> call barring
    private static final String BUTTON_CB_EXPAND = "button_cb_expand_key";

    /// M: CDMA type phone call settings item --> call forward & call wait
    private static final String KEY_CALL_FORWARD = "button_cf_expand_key";
    private static final String KEY_CALL_WAIT = "button_cw_key";
    /// M: SmartFren card type support
    private static final String KEY_CALLER_ID = "button_caller_id";

    private static final String PHONE_ACCOUNT_SETTINGS_KEY =
            "phone_account_settings_preference_screen";

    private static final String ENABLE_VIDEO_CALLING_KEY = "button_enable_video_calling";

    private static final String IMS_STATE_CHANGED = "com.mediatek.INTENT.IMS_STATE_CHANGED";

    private Phone mPhone;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    private TelecomManager mTelecomManager;

    private SwitchPreference mButtonAutoRetry;
    private PreferenceScreen mVoicemailSettingsScreen;
    private SwitchPreference mEnableVideoCalling;

     /**
     * Listen to the IMS service state change
     *
     */
    private ImsConnectionStateListener mImsConnectionStateListener =
        new ImsConnectionStateListener() {
        @Override
        public void onImsConnected(int imsRadioTech) {
            if (DBG) log("onImsConnected imsRadioTech=" + imsRadioTech);
            sendBroadcast(new Intent(IMS_STATE_CHANGED));
        }
        @Override
        public void onImsDisconnected(ImsReasonInfo imsReasonInfo) {
            if (DBG) log("onImsDisconnected imsReasonInfo=" + imsReasonInfo);
            sendBroadcast(new Intent(IMS_STATE_CHANGED));
        }
    };

    /*
     * Click Listeners, handle click based on objects attached to UI.
     */

    // Click listener for all toggle events
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        /// M: Add for our inner features @{
        if (onPreferenceTreeClickMTK(preferenceScreen, preference)) {
            return true;
        }
        /// @}
        if (preference == mButtonAutoRetry) {
            android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.CALL_AUTO_RETRY,
                    mButtonAutoRetry.isChecked() ? 1 : 0);
            return true;
        }
        return false;
    }

    /**
     * Implemented to support onPreferenceChangeListener to look for preference
     * changes.
     *
     * @param preference is the preference to be changed
     * @param objValue should be the value of the selection, NOT its localized
     * display value.
     */
    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (DBG) log("onPreferenceChange: \"" + preference + "\" changed to \"" + objValue + "\"");

        if (preference == mEnableVideoCalling) {
            /// M: IMS video call @{
            int phoneId = getPhoneIdForVideoCalling();
            if (MtkImsManager.isEnhanced4gLteModeSettingEnabledByUser(
                    mPhone.getContext(), phoneId)) {
                MtkImsManager.setVtSetting(mPhone.getContext(), (boolean) objValue, phoneId);
                ///M: For Plugin to get updated video Preference
                ExtensionManager.getCallFeaturesSettingExt()
                        .videoPreferenceChange((boolean) objValue);
            /// @}
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                DialogInterface.OnClickListener networkSettingsClickListener =
                        new Dialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(mPhone.getContext(),
                                        com.android.phone.MobileNetworkSettings.class));
                            }
                        };
                builder.setMessage(getResources().getString(
                                /*./ freeme.liqiang, 20180528. modify the prompt
                                R.string.enable_video_calling_dialog_msg))
                                /*/
                                R.string.freeme_enable_video_calling_dialog_msg))
                                //
                        .setNeutralButton(getResources().getString(
                                R.string.enable_video_calling_dialog_settings),
                                networkSettingsClickListener)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return false;
            }
        }

        // Always let the preference setting proceed.
        return true;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (DBG) log("onCreate: Intent is " + getIntent());

        // Make sure we are running as an admin user.
        if (!UserManager.get(this).isAdminUser()) {
            Toast.makeText(this, R.string.call_settings_admin_user_only,
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //*/ freeme.liqiang, 20180309. add back
        ActionBar actionbar = getActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            setNavigateTitle(actionbar, getIntent());
        }
        //*/
        mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        mSubscriptionInfoHelper.setActionBarTitle(
                getActionBar(), getResources(), R.string.call_settings_with_label);
        mPhone = mSubscriptionInfoHelper.getPhone();
        mTelecomManager = TelecomManager.from(this);
        /// M: Register related listeners & events.
        registerEventCallbacks();

        /// M: Add for MTK hotswap
        if (mPhone == null) {
            log("onCreate: mPhone is null, finish!!!");
            finish();
            return;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            preferenceScreen.removeAll();
        }

        addPreferencesFromResource(R.xml.call_feature_setting);

        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        PreferenceScreen prefSet = getPreferenceScreen();
        mVoicemailSettingsScreen =
                (PreferenceScreen) findPreference(VOICEMAIL_SETTING_SCREEN_PREF_KEY);
        Intent voiceMailIntent = new Intent(this, VoicemailSettingsActivity.class);
        SubscriptionInfoHelper.addExtrasToIntent(voiceMailIntent,
                MtkSubscriptionManager.getSubInfo(null, mPhone.getSubId()));
        mVoicemailSettingsScreen.setIntent(voiceMailIntent);
        maybeHideVoicemailSettings();

        mButtonAutoRetry = (SwitchPreference) findPreference(BUTTON_RETRY_KEY);

        mEnableVideoCalling = (SwitchPreference) findPreference(ENABLE_VIDEO_CALLING_KEY);

        PersistableBundle carrierConfig =
                PhoneGlobals.getInstance().getCarrierConfigForSubId(mPhone.getSubId());

        if (carrierConfig.getBoolean(CarrierConfigManager.KEY_AUTO_RETRY_ENABLED_BOOL)) {
            mButtonAutoRetry.setOnPreferenceChangeListener(this);
            int autoretry = Settings.Global.getInt(
                    getContentResolver(), Settings.Global.CALL_AUTO_RETRY, 0);
            mButtonAutoRetry.setChecked(autoretry != 0);
        } else {
            prefSet.removePreference(mButtonAutoRetry);
            mButtonAutoRetry = null;
        }
        Intent fdnIntent = new Intent(this, FdnSetting.class);
        SubscriptionInfoHelper.addExtrasToIntent(fdnIntent, MtkSubscriptionManager
                                               .getSubInfo(null, mPhone.getSubId()));
        Preference cdmaOptions = prefSet.findPreference(BUTTON_CDMA_OPTIONS);
        Preference gsmOptions = prefSet.findPreference(BUTTON_GSM_UMTS_OPTIONS);
        Preference fdnButton = prefSet.findPreference(BUTTON_FDN_KEY);
        if (carrierConfig.getBoolean(CarrierConfigManager.KEY_WORLD_PHONE_BOOL)) {
            cdmaOptions.setIntent(mSubscriptionInfoHelper.getIntent(CdmaCallOptions.class));
            gsmOptions.setIntent(mSubscriptionInfoHelper.getIntent(GsmUmtsCallOptions.class));
        } else {
            prefSet.removePreference(cdmaOptions);
            prefSet.removePreference(gsmOptions);

            int phoneType = mPhone.getPhoneType();
            if (carrierConfig.getBoolean(CarrierConfigManager
                                    .KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL)) {
                prefSet.removePreference(fdnButton);
            } else {
                /// M: [CT VOLTE] The CT card no fdn item
                boolean isCtVolte = CallSettingUtils.isCtVolte4gSim(mPhone.getSubId())
                        && !TelephonyUtilsEx.isRoaming(mPhone);
                /// M: For Indonesia VOLTE card, no fdn item
                boolean isSmartFren = TelephonyUtilsEx.isSmartFren4gSim(this, mPhone.getSubId());
                if (phoneType == PhoneConstants.PHONE_TYPE_CDMA || isCtVolte || isSmartFren) {
                    /// Add for CDG OMH, show fdn when CDG OMH SIM card. @{
                    if (CdgUtils.isCdgOmhSimCard(mPhone.getSubId())) {
                        fdnButton.setIntent(fdnIntent);
                    } else {
                    /// @}
                        prefSet.removePreference(fdnButton);
                    }

                    if (!carrierConfig.getBoolean(
                            CarrierConfigManager.KEY_VOICE_PRIVACY_DISABLE_UI_BOOL)) {
                        addPreferencesFromResource(R.xml.cdma_call_privacy);
                        /// M: for ALPS02087723, get the right cdma phone instance @{
                        CdmaVoicePrivacySwitchPreference ccp =
                                (CdmaVoicePrivacySwitchPreference) findPreference(BUTTON_CP_KEY);
                        if (ccp != null) {
                            /// M: No voice privacy item for special operator
                            if (isCtVolte || isSmartFren) {
                                log("Voice privacy option removed");
                                prefSet.removePreference(ccp);
                            } else {
                               ccp.setPhone(mPhone);
                            }
                        }
                        /// @}
                    }
                    /// M: For C2K project to group GSM and C2K Call Settings @{
                    boolean isCdmaSupport = TelephonyUtils.isCdmaSupport();
                    log("isCdmaSupport = " + isCdmaSupport);
                    if (isCdmaSupport) {
                        addPreferencesFromResource(R.xml.mtk_cdma_call_options);
                        if (carrierConfig.getBoolean(
                            MtkCarrierConfigManager.MTK_KEY_CALL_WAITING_DISABLE_UI_BOOL)) {
                            Preference callWaiting = prefSet.findPreference(KEY_CALL_WAIT);
                            prefSet.removePreference(callWaiting);
                            log("No support by operator, so remove call waiting pref for CDMA");
                        }
                        if (!isSmartFren) {
                            Preference callerIDPreference = prefSet.findPreference(KEY_CALLER_ID);
                            prefSet.removePreference(callerIDPreference);
                            log("No SmartFren SIM, so remove Caller ID pref for CDMA");
                        }
                    }
                    /// @}
                } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                    fdnButton.setIntent(fdnIntent);

                    if (carrierConfig.getBoolean(
                            CarrierConfigManager.KEY_ADDITIONAL_CALL_SETTING_BOOL)) {
                        addPreferencesFromResource(R.xml.gsm_umts_call_options);
                        GsmUmtsCallOptions.init(prefSet, mSubscriptionInfoHelper);
                    }
                } else {
                    throw new IllegalStateException("Unexpected phone type: " + phoneType);
                }
            }
        }

        /// M: IMS video call @{
        int phoneId = getPhoneIdForVideoCalling();
        boolean isVtEnabledByPlatform = ImsManager.isVtEnabledByPlatform(
                mPhone.getContext(), phoneId);
        boolean isVtProvisionedOnDevice = ImsManager.isVtProvisionedOnDevice(mPhone.getContext());
        boolean isIgnoreDataChanged = carrierConfig.getBoolean(
                CarrierConfigManager.KEY_IGNORE_DATA_ENABLED_CHANGED_FOR_VIDEO_CALLS);
        boolean isDataEnabled =  mPhone.mDcTracker.isDataEnabled();
        log("isVtEnabledByPlatform:" + isVtEnabledByPlatform +
                   "\nisVtProvisionedOnDevice:" + isVtProvisionedOnDevice +
                   "\nisIgnoreDataChanged:" + isIgnoreDataChanged +
                   "\nisDataEnabled:" + isDataEnabled);
        if (isVtEnabledByPlatform && isVtProvisionedOnDevice &&
                     (isIgnoreDataChanged || isDataEnabled)) {
            boolean currentValue = isVideoCallingEnabled(phoneId);
            mEnableVideoCalling.setChecked(currentValue);
            mEnableVideoCalling.setOnPreferenceChangeListener(this);
        } else {
            prefSet.removePreference(mEnableVideoCalling);
        }
        /// @}

        if (ImsManager.isVolteEnabledByPlatform(this) &&
                !carrierConfig.getBoolean(
                        CarrierConfigManager.KEY_CARRIER_VOLTE_TTY_SUPPORTED_BOOL)) {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            /* tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE); */
        }

        /// M: WFC @{
        boolean removeWfcPrefMode = carrierConfig.getBoolean(
                MtkCarrierConfigManager.MTK_KEY_WFC_REMOVE_PREFERENCE_MODE_BOOL);
        Log.d(LOG_TAG, "removeWfcPrefMode:" + removeWfcPrefMode);
        Preference wifiCallingSettings = findPreference(
                getResources().getString(R.string.wifi_calling_settings_key));

        final PhoneAccountHandle simCallManager = mTelecomManager.getSimCallManager();
        if (simCallManager != null) {
            Intent intent = PhoneAccountSettingsFragment.buildPhoneAccountConfigureIntent(
                    this, simCallManager);
            Log.d(LOG_TAG, "--- simCallManager is not null ---");
            if (intent != null) {
                PackageManager pm = mPhone.getContext().getPackageManager();
                List<ResolveInfo> resolutions = pm.queryIntentActivities(intent, 0);
                if (!resolutions.isEmpty()) {
                    Log.d(LOG_TAG, "--- set wfc ---");
                    wifiCallingSettings.setTitle(resolutions.get(0).loadLabel(pm));
                    wifiCallingSettings.setSummary(null);
                    wifiCallingSettings.setIntent(intent);
                } else {
                    Log.d(LOG_TAG, "Remove WFC Preference since resolutions is empty");
                    prefSet.removePreference(wifiCallingSettings);
                }
            } else {
                Log.d(LOG_TAG, "Remove WFC Preference since PhoneAccountConfigureIntent is null");
                prefSet.removePreference(wifiCallingSettings);
            }
        } else if (!MtkImsManager.isWfcEnabledByPlatform(mPhone.getContext(), mPhone.getPhoneId())
                    || !ImsManager.isWfcProvisionedOnDevice(mPhone.getContext())) {
            Log.d(LOG_TAG, "Remove WFC Preference since wfc is not enabled on the device.");
            prefSet.removePreference(wifiCallingSettings);
        } else {
            if (MtkImsManager.isSupportMims() || removeWfcPrefMode) {
                wifiCallingSettings.setSummary("");
                Log.d(LOG_TAG, "Multi IMS support so no wfc summary");
            } else {
                int resId = com.android.internal.R.string.wifi_calling_off_summary;
                if (ImsManager.isWfcEnabledByUser(mPhone.getContext())) {
                    boolean isRoaming = telephonyManager.isNetworkRoaming();
                    int wfcMode = ImsManager.getWfcMode(mPhone.getContext(), isRoaming);
                    switch (wfcMode) {
                        case ImsConfig.WfcModeFeatureValueConstants.WIFI_ONLY:
                            resId = com.android.internal.R.string.wfc_mode_wifi_only_summary;
                            break;
                        case ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED:
                            resId = com.android.internal.R.string.
                                    wfc_mode_cellular_preferred_summary;
                            break;
                        case ImsConfig.WfcModeFeatureValueConstants.WIFI_PREFERRED:
                            resId = com.android.internal.R.string.wfc_mode_wifi_preferred_summary;
                            break;
                        default:
                            if (DBG) log("Unexpected WFC mode value: " + wfcMode);
                    }
                }
                wifiCallingSettings.setSummary(resId);
            }
        }
        /// @}

        ///M: [OMH]
        updateOmhItems();

        /// M: update screen status
        updateScreenStatus();

        /// M: WFC @{
        ExtensionManager.getCallFeaturesSettingExt().initOtherCallFeaturesSetting(this);
        ExtensionManager.getCallFeaturesSettingExt()
                .onCallFeatureSettingsEvent(DefaultCallFeaturesSettingExt.RESUME);
        /// @}
    }

    /**
     * Hides the top level voicemail settings entry point if the default dialer contains a
     * particular manifest metadata key. This is required when the default dialer wants to display
     * its own version of voicemail settings.
     */
    private void maybeHideVoicemailSettings() {
        String defaultDialer = getSystemService(TelecomManager.class).getDefaultDialerPackage();
        if (defaultDialer == null) {
            return;
        }
        try {
            Bundle metadata = getPackageManager()
                    .getApplicationInfo(defaultDialer, PackageManager.GET_META_DATA).metaData;
            if (metadata == null) {
                return;
            }
            if (!metadata
                    .getBoolean(TelephonyManager.METADATA_HIDE_VOICEMAIL_SETTINGS_MENU, false)) {
                if (DBG) {
                    log("maybeHideVoicemailSettings(): not disabled by default dialer");
                }
                return;
            }
            getPreferenceScreen().removePreference(mVoicemailSettingsScreen);
            if (DBG) {
                log("maybeHideVoicemailSettings(): disabled by default dialer");
            }
        } catch (NameNotFoundException e) {
            // do nothing
            if (DBG) {
                log("maybeHideVoicemailSettings(): not controlled by default dialer");
            }
        }
    }

    @Override
    protected void onNewIntent(Intent newIntent) {
        setIntent(newIntent);

        mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        mSubscriptionInfoHelper.setActionBarTitle(
                getActionBar(), getResources(), R.string.call_settings_with_label);
        mPhone = mSubscriptionInfoHelper.getPhone();
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {  // See ActionBar#setDisplayHomeAsUpEnabled()
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Finish current Activity and go up to the top level Settings ({@link CallFeaturesSetting}).
     * This is useful for implementing "HomeAsUp" capability for second-level Settings.
     */
    public static void goUpToTopLevelSetting(
            Activity activity, SubscriptionInfoHelper subscriptionInfoHelper) {
        Intent intent = subscriptionInfoHelper.getIntent(CallFeaturesSetting.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
        activity.finish();
    }

    // -------------------- Mediatek ---------------------
    /// Add for plug-in
    private ICallFeaturesSettingExt mExt;
    /// Add for CDG OMH
    private CdgCallSettings mCdgCallSettings = null;
    private static final String ACTION_OMH = "com.mediatek.internal.omh.cardcheck";

    /**
     * Add for IMS provisioning
     */
    private void updateVtOption() {
        new Thread() {

            // TODO: to check this API, should not be using another thread in UI code
            @Override
            public void run() {
                /// getVtProvisioned api contains two cases:
                /// 1. Don't support provision, it will return true, so that
                ///    the provision value will not affect the decision(show/not)
                /// 2. Support provision, it will return the current status.
                boolean enableProvision = ImsManager.isVtProvisionedOnDevice(
                        mPhone.getContext());
                boolean enablePlatform = ImsManager.isVtEnabledByPlatform(
                        mPhone.getContext(), getPhoneIdForVideoCalling());
                log("updateVtOption enableProvision = " + enableProvision
                        + " enablePlatform = " + enablePlatform);

                PreferenceScreen prefSet = getPreferenceScreen();
                if (enableProvision && enablePlatform) {
                    if (prefSet != null && mEnableVideoCalling != null &&
                            prefSet.findPreference(ENABLE_VIDEO_CALLING_KEY) == null) {
                        prefSet.addPreference(mEnableVideoCalling);
                    }
                }
            }
        }.start();
    }

    private void updateOmhItems() {
        if (CdgUtils.isCdgOmhSimCard(mSubscriptionInfoHelper.getSubId())) {
            log("new CdgCallSettings.");
            mCdgCallSettings = new CdgCallSettings(this, mSubscriptionInfoHelper);
            Preference callForwardPreference = this.findPreference(KEY_CALL_FORWARD);
            if (callForwardPreference != null) {
                this.getPreferenceScreen().removePreference(callForwardPreference);
            }

            Preference callWaitPreference = this.findPreference(KEY_CALL_WAIT);
            if (callWaitPreference != null) {
                this.getPreferenceScreen().removePreference(callWaitPreference);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (UserManager.get(this).isAdminUser()) {
            /// M: WFC @{
            ExtensionManager.getCallFeaturesSettingExt()
                    .onCallFeatureSettingsEvent(DefaultCallFeaturesSettingExt.DESTROY);
            /// @}
            unregisterEventCallbacks();
            /// M: Add for dual volte feature @{
            if (mPhone != null) {
                TelephonyUtils.setParameters(mPhone.getSubId(), null);
            }
            /// @}
        }
        super.onDestroy();
    }

    @Override
    public void handleSubInfoUpdate() {
        finish();
    }

    private void handlePreferenceClickForGsm(PreferenceScreen preferenceScreen,
            Preference preference, int subId) {
        Intent intent;
        PersistableBundle carrierConfig =
                PhoneGlobals.getInstance().getCarrierConfigForSubId(subId);
        if (preference == preferenceScreen.findPreference(CALL_FORWARDING_KEY)) {
            if (carrierConfig.getBoolean(MtkCarrierConfigManager.MTK_KEY_SUPPORT_VT_SS_BOOL)) {
               log("Support VT SS");
               intent = mSubscriptionInfoHelper.getIntent(GsmUmtsVTCFOptions.class);
            } else {
               log("Not Support VT SS");
               intent = mSubscriptionInfoHelper.getIntent(GsmUmtsCallForwardOptions.class);
            }
        } else if (preference == preferenceScreen.findPreference(BUTTON_CB_EXPAND)) {
            if (carrierConfig.getBoolean(MtkCarrierConfigManager.MTK_KEY_SUPPORT_VT_SS_BOOL)) {
                log("Support VT SS");
                intent = mSubscriptionInfoHelper.getIntent(GsmUmtsVTCBOptions.class);
            } else {
                log("Not Support VT SS");
                intent = mSubscriptionInfoHelper.getIntent(CallBarring.class);
            }
        } else {
            intent = mSubscriptionInfoHelper.getIntent(GsmUmtsAdditionalCallOptions.class);
        }
        SubscriptionInfoHelper.addExtrasToIntent(intent,
                MtkSubscriptionManager.getSubInfo(null, subId));
        startActivity(intent);
    }

    private void handlePreferenceClickForCdma(PreferenceScreen preferenceScreen,
            Preference preference, int subId, boolean isImsOn, boolean isUT) {
        Intent intent;
        if (preference == preferenceScreen.findPreference(KEY_CALL_FORWARD)) {
            /// M: [CT VOLTE] Do UT first @{
            if (isUT) {
                intent = mSubscriptionInfoHelper.getIntent(GsmUmtsCallForwardOptions.class);
            } else {
            /// @}
                intent = mSubscriptionInfoHelper.getIntent(CdmaCallForwardOptions.class);
                //*/ freeme.liqiang, 20180320. modify back title in dialer settings
                intent.putExtra(FreemeActionBarUtil.EXTRA_NAVIGATE_UP_TITLE_TEXT,R.string.phone_accounts_settings_header);
                //*/
            }
            SubscriptionInfoHelper.addExtrasToIntent(intent,
                    MtkSubscriptionManager.getSubInfo(null, subId));
            startActivity(intent);
        } else if (preference == preferenceScreen.findPreference(KEY_CALLER_ID)) {
            intent = mSubscriptionInfoHelper.getIntent(
                    GsmUmtsAdditionalCallOptions.class);
            SubscriptionInfoHelper.addExtrasToIntent(intent,
                    MtkSubscriptionManager.getSubInfo(null, subId));
            startActivity(intent);
        } else { // preference == preferenceScreen.findPreference(KEY_CALL_WAIT)
            /// M: remove CNIR and move CW option to cdma call option.
            if (isUT && isImsOn) {
                intent = mSubscriptionInfoHelper
                        .getIntent(CdmaCallWaitingUtOptions.class);
                startActivity(intent);
            } else {
                showDialog(CdmaCallWaitOptions.CW_MODIFY_DIALOG);
            }
        }
    }

    /**
     * For internal features
     * @param preferenceScreen
     * @param preference
     * @return
     */
    private boolean onPreferenceTreeClickMTK(
            PreferenceScreen preferenceScreen, Preference preference) {
        log("onPreferenceTreeClickMTK" + preference.getKey());
        int subId = mPhone.getSubId();
        /// M: Add for dual volte feature @{
        TelephonyUtils.setParameters(subId, preference);
        /// @}

        /// Add for [VoLTE_SS] @{
        if (preference == preferenceScreen.findPreference(CALL_FORWARDING_KEY) ||
                preference == preferenceScreen.findPreference(ADDITIONAL_GSM_SETTINGS_KEY) ||
                preference == preferenceScreen.findPreference(BUTTON_CB_EXPAND)) {
            // The dialog for all case
            if (TelephonyUtils.shouldShowOpenMobileDataDialog(this, subId)) {
                TelephonyUtils.showOpenMobileDataDialog(this, subId);
            } else {
                handlePreferenceClickForGsm(preferenceScreen, preference, subId);
            }
            return true;
        }
        /// @}

        /// M: CDMA type phone call setting item click handling
        if (preference == preferenceScreen.findPreference(KEY_CALL_FORWARD) ||
            preference == preferenceScreen.findPreference(KEY_CALL_WAIT) ||
            preference == preferenceScreen.findPreference(KEY_CALLER_ID)) {
            /// M: [CT VOLTE] @{
            boolean isImsOn = TelephonyUtils.isImsServiceAvailable(this, subId);
            boolean isUT = CallSettingUtils.isCtVolte4gSim(subId) ||
                    (TelephonyUtilsEx.isSmartFrenSim(subId) && isImsOn);
            if (isUT && TelephonyUtils.shouldShowOpenMobileDataDialog(this, subId)) {
                if (!isImsOn && MtkImsManager.isSupportMims()
                    && (preference == preferenceScreen.findPreference(KEY_CALL_WAIT))) {
                    // in dual volte load but not register Ims and callwaiting
                    // show cdma callwaiting dialog directly, cs dial domain.
                    showDialog(CdmaCallWaitOptions.CW_MODIFY_DIALOG);
                } else {
                    // The dialog for all case
                    TelephonyUtils.showOpenMobileDataDialog(this, subId);
                }
            } else {
            /// @}
                handlePreferenceClickForCdma(preferenceScreen, preference,
                        subId, isImsOn, isUT);
            }
            return true;
        }

        /// Add for CDG OMH @{
        if (mCdgCallSettings != null && mCdgCallSettings.onPreferenceTreeClick(
                preferenceScreen, preference)) {
            log("onPreferenceTreeClickMTK, handled by CDG call settings.");
            return true;
        }
        /// @}
        return false;
    }

    private void updateScreenStatus() {
        PreferenceScreen pres = getPreferenceScreen();

        boolean isAirplaneModeEnabled = TelephonyUtils.isAirplaneModeOn(
                PhoneGlobals.getInstance());
        // M: TODO rewrite the setEnable section
        if (pres == null) {
            return;
        }
        int subId = mPhone.getSubId();
        boolean hasSubId = SubscriptionManager.isValidSubscriptionId(subId);
        log("updateScreenStatus, hasSubId " + hasSubId);

        for (int i = 0; i < pres.getPreferenceCount(); i++) {
            Preference pref = pres.getPreference(i);
            pref.setEnabled(!isAirplaneModeEnabled && hasSubId);
        }

        /// M: The CF UI will be disabled when air plane mode is on.
        /// but SS should be still workable when IMS is registered,
        /// So Enable the CF UI when IMS is registered. {@
        if (hasSubId) {
            boolean isImsOn = TelephonyUtils.isImsServiceAvailable(this, subId);
            Preference prefCf = getPreferenceScreen().findPreference(CALL_FORWARDING_KEY);
            Preference prefCb = getPreferenceScreen().findPreference(BUTTON_CB_EXPAND);
            Preference prefCw = getPreferenceScreen().findPreference(ADDITIONAL_GSM_SETTINGS_KEY);
            if (prefCf != null) {
                if (isImsOn && (TelephonyUtilsEx.isCapabilityPhone(mPhone)
                        || MtkImsManager.isSupportMims())) {
                    log(" --- set SS item enabled when IMS is registered ---");
                    prefCf.setEnabled(true);
                    prefCb.setEnabled(true);
                    prefCw.setEnabled(true);
                }
            }
            if (TelephonyUtilsEx.isSmartFrenSim(subId) && isImsOn) {
                Preference prefCdmaCf = getPreferenceScreen().findPreference(KEY_CALL_FORWARD);
                Preference prefCdmaCw = getPreferenceScreen().findPreference(KEY_CALL_WAIT);
                Preference prefCdmaCi = getPreferenceScreen().findPreference(KEY_CALLER_ID);
                log(" -- set CDMA SS item enabled when IMS is registered for SmartFren only --");
                if (prefCdmaCf != null) {
                    prefCdmaCf.setEnabled(true);
                }
                if (prefCdmaCw != null) {
                    prefCdmaCw.setEnabled(true);
                }
                if (prefCdmaCi != null) {
                    prefCdmaCi.setEnabled(true);
                }
            }
        }
        updateVtEnableStatus();
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            log("onReceive, action = " + action);
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action) ||
                    IMS_STATE_CHANGED.equals(action)) {
                updateScreenStatus();
            } else if (ACTION_OMH.equals(action)) {
                log("update omh items");
                updateOmhItems();
            //  When IMS Configuration Provisioning value changed,
            // remove/add mEnableVideoCalling item.@{
            } else if (ImsConfigContract.ACTION_IMS_CONFIG_CHANGED == action) {
                int actionId = intent.getIntExtra(ImsConfigContract.EXTRA_CHANGED_ITEM, -1);
                log("EXTRA_CHANGED_ITEM actionId = " + actionId);
                if (ImsConfig.ConfigConstants.LVC_SETTING_ENABLED == actionId) {
                    updateVtOption();
                }
            }
        }
    };

    // dialog creation method, called by showDialog()
    @Override
    protected Dialog onCreateDialog(int dialogId) {
        /// M: remove CNIR and move CW option to cdma call option.
        if (dialogId == CdmaCallWaitOptions.CW_MODIFY_DIALOG) {
            return new CdmaCallWaitOptions(this, mPhone).createDialog();
        }

        /// Add for CDG OMH @{
        /// Todo: no used, remove
        if (mCdgCallSettings != null) {
            return mCdgCallSettings.onCreateDialog(dialogId);
        }
        /// @}
        return null;
    }

    /**
     * Add call status listener, for VT items(should be disable during calling)
     */
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            boolean enabled = (state == TelephonyManager.CALL_STATE_IDLE);
            log("[onCallStateChanged] enabled = " + enabled);
            updateVtEnableStatus();
        }
    };

    /**
     * 1. Listen sim hot swap related change.
     * 2. ACTION_AIRPLANE_MODE_CHANGED
     * 3. Call Status for VT item
     */
    private void registerEventCallbacks() {
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
        /// register airplane mode
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intentFilter.addAction(ACTION_OMH);
        intentFilter.addAction(ImsConfigContract.ACTION_IMS_CONFIG_CHANGED);
        intentFilter.addAction(IMS_STATE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
        try {
            ImsManager imsManager = ImsManager.getInstance(mPhone.getContext(),
                    mPhone.getPhoneId());
            if (imsManager != null) {
                imsManager.addRegistrationListener(ImsServiceClass.MMTEL,
                        mImsConnectionStateListener);
            }
        } catch (ImsException e) {
            log("ImsException:" + e);
        }
    }

    private void unregisterEventCallbacks() {
        PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);
        unregisterReceiver(mReceiver);
        try {
            ImsManager imsManager = ImsManager.getInstance(mPhone.getContext(),
                    mPhone.getPhoneId());
            if (imsManager != null) {
                imsManager.removeRegistrationListener(mImsConnectionStateListener);
            }
        } catch (ImsException e) {
            log("ImsException:" + e);
        }
    }

    /**
     * This is for VT option, when during call, disable it.
     */
    private void updateVtEnableStatus() {
        boolean hasSubId = mPhone != null
                && SubscriptionManager.isValidSubscriptionId(mPhone.getSubId());
        boolean isInCall = TelephonyUtils.isInCall(this);
        log("[updateVtEnableStatus] isInCall = " + isInCall + ", hasSubId = " + hasSubId);
        if (mEnableVideoCalling != null) {
            mEnableVideoCalling.setEnabled(hasSubId && !isInCall);
        }
        Preference wifiCallingSettings = findPreference(
                getResources().getString(R.string.wifi_calling_settings_key));
        if (wifiCallingSettings != null) {
            wifiCallingSettings.setEnabled(hasSubId && !isInCall);
        }
    }

    private int getPhoneIdForVideoCalling() {
        int phoneId;
        if (MtkImsManager.isSupportMims()) {
            phoneId = mPhone.getPhoneId();
        } else {
            phoneId = TelephonyUtils.getMainCapabilityPhoneId();
        }
        log("getPhoneIdForVideoCalling:" + phoneId);
        return phoneId;
    }

    private boolean isVideoCallingEnabled(int phoneId) {
        boolean enable = MtkImsManager.isEnhanced4gLteModeSettingEnabledByUser(
                mPhone.getContext(), phoneId) &&
                MtkImsManager.isVtEnabledByUser(mPhone.getContext(), phoneId);
        log("isVideoCallingEnabled phoneId("+ phoneId + ") = " + enable);

        return enable;
    }

    //*/ freeme.liqiang, 20180320. modify back title in dialer settings
    private String getSubTitle(Intent intent) {
        String title = null;
        if (intent != null) {
            title = intent.getStringExtra(FreemeActionBarUtil.EXTRA_NAVIGATE_UP_TITLE_TEXT);
        }
        if (TextUtils.isEmpty(title)) {
            title = getString(R.string.phone_accounts);
        }

        return title;
    }

    private void setNavigateTitle(ActionBar actionBar, Intent intent) {
        if (actionBar != null) {
            FreemeActionBarUtil.setBackTitle(actionBar, getSubTitle(intent));
        }
    }
    //*/
}

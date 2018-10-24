/*
 * Copyright (C) 2006 The Android Open Source Project
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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.ListPreference;
import android.preference.Preference;
//M: Use prefactivity
import android.preference.PreferenceActivity;
//import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.RadioAccessFamily;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;

import com.android.ims.ImsManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.settingslib.RestrictedLockUtils;

import com.mediatek.ims.internal.MtkImsManager;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.internal.telephony.MtkPhoneConstants;
import com.mediatek.internal.telephony.MtkTelephonyIntents;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.phone.PhoneFeatureConstants.FeatureOption;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.settings.Enhanced4GLteSwitchPreference;
import com.mediatek.settings.MobileNetworkSettingsOmEx;
import com.mediatek.settings.TelephonyUtils;
import com.mediatek.settings.cdma.CdmaNetworkSettings;
import com.mediatek.settings.cdma.TelephonyUtilsEx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import mediatek.telephony.MtkCarrierConfigManager;
//*/ freeme.liqiang, 20180328. add back title
import com.freeme.actionbar.app.FreemeActionBarUtil;
//*/

/**
 * "Mobile network settings" screen.  This screen lets you
 * enable/disable mobile data, and control data roaming and other
 * network-specific mobile data features.  It's used on non-voice-capable
 * tablets as well as regular phone devices.
 *
 * Note that this Activity is part of the phone app, even though
 * you reach it from the "Wireless & Networks" section of the main
 * Settings app.  It's not part of the "Call settings" hierarchy that's
 * available from the Phone app (see CallFeaturesSetting for that.)
 */
/*/ freeme.liqiang, 20180423. redesign mobile network setting UI
public class MobileNetworkSettings extends PreferenceActivity implements
        DialogInterface.OnClickListener, DialogInterface.OnDismissListener,
        Preference.OnPreferenceChangeListener {
    private enum TabState {
        NO_TABS, UPDATE, DO_NOTHING
    }


    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        switch (itemId) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

        // debug data
        private static final String LOG_TAG = "NetworkSettings";
        private static final boolean DBG = "eng".equals(Build.TYPE);
        public static final int REQUEST_CODE_EXIT_ECM = 17;

        // Number of active Subscriptions to show tabs
        private static final int TAB_THRESHOLD = 2;

        // fragment tag for roaming data dialog
        private static final String ROAMING_TAG = "RoamingDialogFragment";

        //String keys for preference lookup
        public static final String BUTTON_PREFERED_NETWORK_MODE = "preferred_network_mode_key";
        private static final String BUTTON_ROAMING_KEY = "button_roaming_key";
        private static final String BUTTON_CDMA_LTE_DATA_SERVICE_KEY = "cdma_lte_data_service_key";
        public static final String BUTTON_ENABLED_NETWORKS_KEY = "enabled_networks_key";
        private static final String BUTTON_4G_LTE_KEY = "enhanced_4g_lte";
        private static final String BUTTON_CELL_BROADCAST_SETTINGS = "cell_broadcast_settings";
        private static final String BUTTON_APN_EXPAND_KEY = "button_apn_key";
        private static final String BUTTON_OPERATOR_SELECTION_EXPAND_KEY = "button_carrier_sel_key";
        private static final String BUTTON_CARRIER_SETTINGS_KEY = "carrier_settings_key";
        private static final String BUTTON_CDMA_SYSTEM_SELECT_KEY = "cdma_system_select_key";
        private static final String ENHANCED_4G_MODE_ENABLED_SIM2 = "volte_vt_enabled_sim2";

        //private final BroadcastReceiver mPhoneChangeReceiver = new PhoneChangeReceiver();

        static final int preferredNetworkMode = Phone.PREFERRED_NT_MODE;

        //Information about logical "up" Activity
        private static final String UP_ACTIVITY_PACKAGE = "com.android.settings";
        private static final String UP_ACTIVITY_CLASS =
                "com.android.settings.Settings$WirelessSettingsActivity";

        private SubscriptionManager mSubscriptionManager;
        private TelephonyManager mTelephonyManager;

        //UI objects
        private ListPreference mButtonPreferredNetworkMode;
        private ListPreference mButtonEnabledNetworks;
        private RestrictedSwitchPreference mButtonDataRoam;
        private SwitchPreference mButton4glte;
        private Preference mLteDataServicePref;

        private static final String iface = "rmnet0"; //TODO: this will go away
        private List<SubscriptionInfo> mActiveSubInfos;

        private UserManager mUm;
        private Phone mPhone;
        private MyHandler mHandler;
        private boolean mOkClicked;

        // We assume the the value returned by mTabHost.getCurrentTab() == slotId
        private TabHost mTabHost;

        //GsmUmts options and Cdma options
        GsmUmtsOptions mGsmUmtsOptions;
        CdmaOptions mCdmaOptions;

        private Preference mClickedPreference;
        private boolean mShow4GForLTE;
        private boolean mIsGlobalCdma;
        private boolean mUnavailable;
        /// Add for C2K OM features
        private CdmaNetworkSettings mCdmaNetworkSettings;

        private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
            /*
             * Enable/disable the 'Enhanced 4G LTE Mode' when in/out of a call
             * and depending on TTY mode and TTY support over VoLTE.
             * @see android.telephony.PhoneStateListener#onCallStateChanged(int,
             * java.lang.String)
             * /
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (DBG) log("PhoneStateListener.onCallStateChanged: state=" + state);
                updateScreenStatus();

                /// M: should also update enable state in other places, so exact to method
                updateEnhanced4glteEnableState();
                /*boolean enabled = (state == TelephonyManager.CALL_STATE_IDLE) &&
                        MtkImsManager.isNonTtyOrTtyOnVolteEnabled(getApplicationContext(),
                                mPhone.getPhoneId());
                Preference pref = getPreferenceScreen().findPreference(BUTTON_4G_LTE_KEY);
                if (pref != null) pref.setEnabled(enabled && hasActiveSubscriptions());* /

            }
            /**
             *  For CU volte feature.
             * /
            @Override
            public void onServiceStateChanged(ServiceState state) {
                if (ExtensionManager.getMobileNetworkSettingsExt().customizeCUVolte()) {
                    updateEnhanced4glteEnableState();
                }
            }
        };

    /** This is a method implemented for DialogInterface.OnClickListener.
     * Used to dismiss the dialogs when they come up.
     * @param dialog roaming dialog
     * @param which button response.
     * /
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            mPhone.setDataRoamingEnabled(true);
            mOkClicked = true;
        } else {
            // Reset the toggle
            mButtonDataRoam.setChecked(false);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        // Assuming that onClick gets called first
        mButtonDataRoam.setChecked(mOkClicked);
    }

        /**
         * Invoked on each preference click in this hierarchy, overrides
         * PreferenceActivity's implementation.  Used to make sure we track the
         * preference click events.
         * /
        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                             Preference preference) {
            /** TODO: Refactor and get rid of the if's using subclasses * /
            final int phoneSubId = mPhone.getSubId();
            if (mCdmaNetworkSettings != null &&
                mCdmaNetworkSettings.onPreferenceTreeClick(preferenceScreen, preference)) {
                return true;
            }
          /// M: Add for Plug-in @{
            if (ExtensionManager.getMobileNetworkSettingsExt()
                    .onPreferenceTreeClick(preferenceScreen, preference)) {
                return true;
            }
            /// @}
            if (preference.getKey().equals(BUTTON_4G_LTE_KEY)) {
                return true;
            } else if (mGsmUmtsOptions != null &&
                    mGsmUmtsOptions.preferenceTreeClick(preference) == true) {
                return true;
            } else if (mCdmaOptions != null &&
                    mCdmaOptions.preferenceTreeClick(preference) == true) {
                /** M: Change get ECM mode by function @{
                if (Boolean.parseBoolean(
                        SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {
                * /
                if (mPhone.isInEcm()) {
                /** @} * /

                    mClickedPreference = preference;

                    // In ECM mode launch ECM app dialog
                    startActivityForResult(
                            new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                            REQUEST_CODE_EXIT_ECM);
                }
                return true;
            } else if (preference == mButtonPreferredNetworkMode) {
                //displays the value taken from the Settings.System
                int settingsNetworkMode = android.provider.Settings.Global.getInt(
                        mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                        preferredNetworkMode);
                mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
                return true;
            } else if (preference == mLteDataServicePref) {
                String tmpl = android.provider.Settings.Global.getString(
                        getContentResolver(),
                        android.provider.Settings.Global.SETUP_PREPAID_DATA_SERVICE_URL);
                if (!TextUtils.isEmpty(tmpl)) {
                    TelephonyManager tm = (TelephonyManager) getSystemService(
                            Context.TELEPHONY_SERVICE);
                    String imsi = tm.getSubscriberId();
                    if (imsi == null) {
                        imsi = "";
                    }
                    final String url = TextUtils.isEmpty(tmpl) ? null
                            : TextUtils.expandTemplate(tmpl, imsi).toString();
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } else {
                    android.util.Log.e(LOG_TAG, "Missing SETUP_PREPAID_DATA_SERVICE_URL");
                }
                return true;
            }  else if (preference == mButtonEnabledNetworks) {
                int settingsNetworkMode = android.provider.Settings.Global.getInt(
                        mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                        preferredNetworkMode);
                Log.d(LOG_TAG, "onPreferenceTreeClick settingsNetworkMode: " + settingsNetworkMode);
                /** M: Remove this for LW project, for we need set a temple value.
                mButtonEnabledNetworks.setValue(Integer.toString(settingsNetworkMode));
                * /
                return true;
            } else if (preference == mButtonDataRoam) {
                // Do not disable the preference screen if the user clicks Data roaming.
                return true;
            //* / freeme.wanglei, 20170713. add cellular data switch.
            } else if (preference == mButtonDataNetwork) {
                return true;
            //* /
            } else {
                // if the button is anything but the simple toggle preference,
                // we'll need to disable all preferences to reject all click
                // events until the sub-activity's UI comes up.
                preferenceScreen.setEnabled(false);
                // Let the intents be launched by the Preference manager
                return false;
            }
        }

        private final SubscriptionManager.OnSubscriptionsChangedListener
                mOnSubscriptionsChangeListener
                = new SubscriptionManager.OnSubscriptionsChangedListener() {
            @Override
            public void onSubscriptionsChanged() {
                if (DBG) {
                    log("onSubscriptionsChanged:");
                }
                /// M: add for hot swap @{
                if (TelephonyUtils.isHotSwapHanppened(
                            mActiveSubInfos, PhoneUtils.getActiveSubInfoList())) {
                    if (DBG) {
                        log("onSubscriptionsChanged:hot swap hanppened");
                    }
                    dissmissDialog(mButtonPreferredNetworkMode);
                    dissmissDialog(mButtonEnabledNetworks);
                    finish();
                    return;
                }
                /// @}
                initializeSubscriptions();
            }
        };

        private void initializeSubscriptions() {
            if (isDestroyed()) {
                // Process preferences in activity only if its not destroyed
                return;
            }
            int currentTab = 0;
            if (DBG) log("initializeSubscriptions:+");

            // Before updating the the active subscription list check
            // if tab updating is needed as the list is changing.
            List<SubscriptionInfo> sil = mSubscriptionManager.getActiveSubscriptionInfoList();
            TabState state = isUpdateTabsNeeded(sil);

            // Update to the active subscription list
            mActiveSubInfos.clear();
            if (sil != null) {
                mActiveSubInfos.addAll(sil);
                /* M: remove for 3SIM feature
                // If there is only 1 sim then currenTab should represent slot no. of the sim.
                if (sil.size() == 1) {
                    currentTab = sil.get(0).getSimSlotIndex();
                }* /
            }

            switch (state) {
                case UPDATE: {
                    if (DBG) {
                        log("initializeSubscriptions: UPDATE");
                    }
                    currentTab = mTabHost != null ? mTabHost.getCurrentTab() : mCurrentTab;

                    setContentView(com.android.internal.R.layout.common_tab_settings);

                    mTabHost = (TabHost) findViewById(android.R.id.tabhost);
                    mTabHost.setup();

                    // Update the tabName. Since the mActiveSubInfos are in slot order
                    // we can iterate though the tabs and subscription info in one loop. But
                    // we need to handle the case where a slot may be empty.

                    /// M: change design for 3SIM feature @{
                    for (int index = 0; index  < mActiveSubInfos.size(); index++) {
                        String tabName = String.valueOf(mActiveSubInfos.get(index).
                                getDisplayName());
                        if (DBG) {
                            log("initializeSubscriptions: tab=" + index + " name=" + tabName);
                        }

                        mTabHost.addTab(buildTabSpec(String.valueOf(index), tabName));
                    }
                    /// @}

                    mTabHost.setOnTabChangedListener(mTabListener);
                    mTabHost.setCurrentTab(currentTab);
                    break;
                }
                case NO_TABS: {
                    if (DBG) log("initializeSubscriptions: NO_TABS");

                    if (mTabHost != null) {
                        mTabHost.clearAllTabs();
                        mTabHost = null;
                    }
                    setContentView(com.android.internal.R.layout.common_tab_settings);
                    break;
                }
                case DO_NOTHING: {
                    if (DBG) log("initializeSubscriptions: DO_NOTHING");
                    if (mTabHost != null) {
                        currentTab = mTabHost.getCurrentTab();
                    }
                    break;
                }
            }
            updatePhone(convertTabToSlot(currentTab));
            updateBody();
            if (DBG) log("initializeSubscriptions:-");
        }

        private TabState isUpdateTabsNeeded(List<SubscriptionInfo> newSil) {
            TabState state = TabState.DO_NOTHING;
            if (newSil == null) {
                if (mActiveSubInfos.size() >= TAB_THRESHOLD) {
                    if (DBG) log("isUpdateTabsNeeded: NO_TABS, size unknown and was tabbed");
                    state = TabState.NO_TABS;
                }
            } else if (newSil.size() < TAB_THRESHOLD && mActiveSubInfos.size() >= TAB_THRESHOLD) {
                if (DBG) log("isUpdateTabsNeeded: NO_TABS, size went to small");
                state = TabState.NO_TABS;
            } else if (newSil.size() >= TAB_THRESHOLD && mActiveSubInfos.size() < TAB_THRESHOLD) {
                if (DBG) log("isUpdateTabsNeeded: UPDATE, size changed");
                state = TabState.UPDATE;
            } else if (newSil.size() >= TAB_THRESHOLD) {
                Iterator<SubscriptionInfo> siIterator = mActiveSubInfos.iterator();
                for (SubscriptionInfo newSi : newSil) {
                    SubscriptionInfo curSi = siIterator.next();
                    if (!newSi.getDisplayName().equals(curSi.getDisplayName())) {
                        if (DBG) log("isUpdateTabsNeeded: UPDATE, new name="
                                + newSi.getDisplayName());
                        state = TabState.UPDATE;
                        break;
                    }
                }
            }
            if (DBG) {
                Log.i(LOG_TAG, "isUpdateTabsNeeded:- " + state
                        + " newSil.size()=" + ((newSil != null) ? newSil.size() : 0)
                        + " mActiveSubInfos.size()=" + mActiveSubInfos.size());
            }
            return state;
        }

        private TabHost.OnTabChangeListener mTabListener = new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                if (DBG) log("onTabChanged:");
                // The User has changed tab; update the body.
                updatePhone(convertTabToSlot(Integer.parseInt(tabId)));
                mCurrentTab = Integer.parseInt(tabId);
                updateBody();
            }
        };

        private void updatePhone(int slotId) {
            final SubscriptionInfo sir = mSubscriptionManager
                    .getActiveSubscriptionInfoForSimSlotIndex(slotId);
            if (sir != null) {
                mPhone = PhoneFactory.getPhone(
                        SubscriptionManager.getPhoneId(sir.getSubscriptionId()));
            }
            if (mPhone == null) {
                // Do the best we can
                mPhone = PhoneGlobals.getPhone();
            }
            Log.i(LOG_TAG, "updatePhone:- slotId=" + slotId + " sir=" + sir);
        }

        private TabHost.TabContentFactory mEmptyTabContent = new TabHost.TabContentFactory() {
            @Override
            public View createTabContent(String tag) {
                return new View(mTabHost.getContext());
            }
        };

        private TabHost.TabSpec buildTabSpec(String tag, String title) {
            return mTabHost.newTabSpec(tag).setIndicator(title).setContent(
                    mEmptyTabContent);
        }

        @Override
        public void onCreate(Bundle icicle) {
            Log.i(LOG_TAG, "onCreate:+");
             /// Add for cmcc open market @{
             mOmEx = new MobileNetworkSettingsOmEx(this);
            /// @}
            super.onCreate(icicle);
            mHandler = new MyHandler();
            mUm = (UserManager) getSystemService(Context.USER_SERVICE);
            mSubscriptionManager = SubscriptionManager.from(this);
            mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            boolean isAdmin = mUm.isAdminUser();
            Log.d(LOG_TAG, "isAdmin = " + isAdmin);

            if (!isAdmin || mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)) {
                mUnavailable = true;
                setContentView(R.layout.telephony_disallowed_preference_screen);
                return;
            }

            //* / freeme.liqiang, 20180328. add back title
            com.freeme.actionbar.app.FreemeActionBarUtil.setNavigateTitle(this, getIntent());
            //* /
            addPreferencesFromResource(R.xml.network_setting_fragment);

            mButton4glte = (SwitchPreference) findPreference(BUTTON_4G_LTE_KEY);
            mButton4glte.setOnPreferenceChangeListener(this);
            //* / freeme.wanglei, 20170713. add cellular data switch.
            mButtonDataNetwork = (SwitchPreference) findPreference(BUTTON_DATA_NETWORK_KEY);
            mButtonDataNetwork.setOnPreferenceChangeListener(this);
            //* /

            try {
                Context con = createPackageContext("com.android.systemui", 0);
                int id = con.getResources().getIdentifier("config_show4GForLTE",
                        "bool", "com.android.systemui");
                mShow4GForLTE = con.getResources().getBoolean(id);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(LOG_TAG, "NameNotFoundException for show4GFotLTE");
                mShow4GForLTE = false;
            }
            Log.d(LOG_TAG, "mShow4GForLTE: " + mShow4GForLTE);

            //get UI object references
            PreferenceScreen prefSet = getPreferenceScreen();

            mButtonDataRoam = (RestrictedSwitchPreference) prefSet.findPreference(
                    BUTTON_ROAMING_KEY);
            mButtonPreferredNetworkMode = (ListPreference) prefSet.findPreference(
                    BUTTON_PREFERED_NETWORK_MODE);
            mButtonEnabledNetworks = (ListPreference) prefSet.findPreference(
                    BUTTON_ENABLED_NETWORKS_KEY);
            mButtonDataRoam.setOnPreferenceChangeListener(this);

            mLteDataServicePref = prefSet.findPreference(BUTTON_CDMA_LTE_DATA_SERVICE_KEY);

            // Initialize mActiveSubInfo
            int max = mSubscriptionManager.getActiveSubscriptionInfoCountMax();
            mActiveSubInfos = new ArrayList<SubscriptionInfo>(max);

            initIntentFilter();
            try {
                registerReceiver(mReceiver, mIntentFilter);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Receiver Already registred");
            }

            mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
            mTelephonyManager.listen(
                    mPhoneStateListener,
                    PhoneStateListener.LISTEN_CALL_STATE
                    | PhoneStateListener.LISTEN_SERVICE_STATE
                    );

            /// M: for screen rotate
            if (icicle != null) {
                mCurrentTab = icicle.getInt(CURRENT_TAB);
            }
            initializeSubscriptions();

            /// M: [CT VOLTE]
            getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.ENHANCED_4G_MODE_ENABLED),
                    true, mContentObserver);
            getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(ENHANCED_4G_MODE_ENABLED_SIM2),
                    true, mContentObserver);
            Log.i(LOG_TAG, "onCreate:-");
        }

        /// M: Replaced with mReceiver
        /*private class PhoneChangeReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(LOG_TAG, "onReceive:");
                // When the radio changes (ex: CDMA->GSM), refresh all options.
                mGsmUmtsOptions = null;
                mCdmaOptions = null;
                updateBody();
            }
        }* /

        @Override
        public void onDestroy() {
            super.onDestroy();

            boolean isAdmin = mUm.isAdminUser();
            Log.d(LOG_TAG, "onDestroy, isAdmin = " + isAdmin);
            if (!isAdmin) {
                return;
            }

            try {
                unregisterReceiver(mReceiver);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Receiver Already unregistred");
            }
            ExtensionManager.getMobileNetworkSettingsExt().unRegister();
            log("onDestroy ");
            if (mCdmaNetworkSettings != null) {
                mCdmaNetworkSettings.onDestroy();
                mCdmaNetworkSettings = null;
            }
            if (mSubscriptionManager != null) {
                mSubscriptionManager
                        .removeOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
            }
            if (mTelephonyManager != null) {
                mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            }
            /// M: [CT VOLTE] @{
            getContentResolver().unregisterContentObserver(mContentObserver);
            /// M: [CT VOLTE] @{
            if (TelephonyUtilsEx.isCtVolteEnabled()
                    && TelephonyUtilsEx.isCt4gSim(mPhone.getSubId())) {
                getContentResolver().unregisterContentObserver(mNetworkObserver);
            }
            if (mDialog != null && mDialog.isShowing()) {
                mDialog.dismiss();
            }
            /// @}
            /// Add for cmcc open market @{
            if (mOmEx != null) {
                mOmEx.unRegister();
            }
            /// @}
        }

        @Override
        public void onResume() {
            super.onResume();
            Log.i(LOG_TAG, "onResume:+");

            if (mUnavailable) {
                Log.i(LOG_TAG, "onResume:- ignore mUnavailable == false");
                return;
            }

            /// M: for C2K OM features @{
            if (mCdmaNetworkSettings != null) {
                mCdmaNetworkSettings.onResume();
            }
            /// @}
            // upon resumption from the sub-activity, make sure we re-enable the
            // preferences.
            //getPreferenceScreen().setEnabled(true);

            // Set UI state in onResume because a user could go home, launch some
            // app to change this setting's backend, and re-launch this settings app
            // and the UI state would be inconsistent with actual state
            mButtonDataRoam.setChecked(mPhone.getDataRoamingEnabled());

            //* / freeme.wanglei, 20170713. add cellular data switch.
            mDefaultSubscriptionId = getDefaultSubscriptionId();
            mHasDefaultSubId = mDefaultSubscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            mListener.setListener(true, mDefaultSubscriptionId, getApplicationContext());
            updateChecked();
            //* /

            if (getPreferenceScreen().findPreference(BUTTON_PREFERED_NETWORK_MODE) != null
                    || getPreferenceScreen().findPreference(BUTTON_ENABLED_NETWORKS_KEY) != null)  {
                updatePreferredNetworkUIFromDb();
            }
            /*ImsManager imsManager = ImsManager.getInstance(getApplicationContext(),
                    mPhone.getPhoneId());
            if (imsManager.isVolteEnabledByPlatformForSlot()
                    && ImsManager.isVolteProvisionedOnDevice(this)) {
                TelephonyManager tm =
                        (TelephonyManager) getSystemService(
                                Context.TELEPHONY_SERVICE);
                tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            }

            // NOTE: Buttons will be enabled/disabled in mPhoneStateListener
            boolean enh4glteMode = MtkImsManager.isEnhanced4gLteModeSettingEnabledByUser(this,
                    mPhone.getPhoneId()) && MtkImsManager.isNonTtyOrTtyOnVolteEnabled(this,
                    mPhone.getPhoneId());
            mButton4glte.setChecked(enh4glteMode);* /

            mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);

            /// M: For screen update
            updateScreenStatus();

            /// M: For plugin to update UI
            ExtensionManager.getMobileNetworkSettingsExt().onResume();

            Log.i(LOG_TAG, "onResume:-");

        }

        private boolean hasActiveSubscriptions() {
            return mActiveSubInfos.size() > 0;
        }

        private void updateBody() {
            Context context = getApplicationContext();
            PreferenceScreen prefSet = getPreferenceScreen();
            boolean isLteOnCdma = mPhone.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE;
            final int phoneSubId = mPhone.getSubId();

            if (DBG) {
                log("updateBody: isLteOnCdma=" + isLteOnCdma + " phoneSubId=" + phoneSubId);
            }

            if (prefSet != null) {
                prefSet.removeAll();
                prefSet.addPreference(mButtonDataRoam);
                prefSet.addPreference(mButtonPreferredNetworkMode);
                prefSet.addPreference(mButtonEnabledNetworks);
                prefSet.addPreference(mButton4glte);
                //* / freeme.wanglei, 20170713. add cellular data switch.
                if (mActiveSubInfos.size() >= TAB_THRESHOLD) {
                    boolean isDefaultSimSlot = false;
                    int currentTab = mTabHost != null ? mTabHost.getCurrentTab() : mCurrentTab;
                    SubscriptionInfo subscriptionInfo = mSubscriptionManager.getDefaultDataSubscriptionInfo();
                    if (subscriptionInfo != null) {
                        isDefaultSimSlot = currentTab == subscriptionInfo.getSimSlotIndex();
                    }
                    if (isDefaultSimSlot) {
                        prefSet.addPreference(mButtonDataNetwork);
                    }
                } else {
                    prefSet.addPreference(mButtonDataNetwork);
                }
                //* /
            }

            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                    preferredNetworkMode);

            PersistableBundle carrierConfig =
                    PhoneGlobals.getInstance().getCarrierConfigForSubId(mPhone.getSubId());
            mIsGlobalCdma = isLteOnCdma
                    && carrierConfig.getBoolean(CarrierConfigManager.KEY_SHOW_CDMA_CHOICES_BOOL);
            if (carrierConfig.getBoolean(
                    CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL)) {
                prefSet.removePreference(mButtonPreferredNetworkMode);
                prefSet.removePreference(mButtonEnabledNetworks);
                prefSet.removePreference(mLteDataServicePref);
            } else if (carrierConfig.getBoolean(CarrierConfigManager
                    .KEY_HIDE_PREFERRED_NETWORK_TYPE_BOOL)
                    && !mPhone.getServiceState().getRoaming()) {
                prefSet.removePreference(mButtonPreferredNetworkMode);
                prefSet.removePreference(mButtonEnabledNetworks);

                final int phoneType = mPhone.getPhoneType();
                if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                    mCdmaOptions = new CdmaOptions(this, prefSet, mPhone);
                    // In World mode force a refresh of GSM Options.
                    if (isWorldMode()) {
                        mGsmUmtsOptions = null;
                    }
                } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                    mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet, phoneSubId);
                } else {
                    throw new IllegalStateException("Unexpected phone type: " + phoneType);
                }
                // Since pref is being hidden from user, set network mode to default
                // in case it is currently something else. That is possible if user
                // changed the setting while roaming and is now back to home network.
                settingsNetworkMode = preferredNetworkMode;
            } else if (carrierConfig.getBoolean(
                    CarrierConfigManager.KEY_WORLD_PHONE_BOOL) == true) {
                prefSet.removePreference(mButtonEnabledNetworks);
                // set the listener for the mButtonPreferredNetworkMode list
                // preference so we can issue
                // change Preferred Network Mode.
                mButtonPreferredNetworkMode.setOnPreferenceChangeListener(this);

                mCdmaOptions = new CdmaOptions(this, prefSet, mPhone);
                mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet, phoneSubId);
            } else {
                prefSet.removePreference(mButtonPreferredNetworkMode);
                final int phoneType = mPhone.getPhoneType();
                int mainPhoneId = SubscriptionManager.INVALID_PHONE_INDEX;
                IMtkTelephonyEx iTelEx = IMtkTelephonyEx.Stub.asInterface(
                                                        ServiceManager.getService("phoneEx"));
                if (iTelEx != null) {
                    try {
                        mainPhoneId = iTelEx.getMainCapabilityPhoneId();
                    } catch (RemoteException e) {
                        log("getMainCapabilityPhoneId: remote exception");
                    }
                } else {
                    log("IMtkTelephonyEx service not ready!");
                    mainPhoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
                }
                if (TelephonyUtilsEx.isCDMAPhone(mPhone)
                    /// M: [CT VOLTE]
                    || (TelephonyUtilsEx.isCtVolteEnabled() &&
                        TelephonyUtilsEx.isCt4gSim(mPhone.getSubId()) &&
                        !TelephonyUtilsEx.isRoaming(mPhone) &&
                        (!TelephonyUtilsEx.isBothslotCtSim(mSubscriptionManager) ||
                        (mainPhoneId == mPhone.getPhoneId())))) {
                    log("phoneType == PhoneConstants.PHONE_TYPE_CDMA or is CT VOLTE...");
                    int lteForced = android.provider.Settings.Global.getInt(
                            mPhone.getContext().getContentResolver(),
                            android.provider.Settings.Global.LTE_SERVICE_FORCED + mPhone.getSubId(),
                            0);

                    if (isLteOnCdma) {
                        if (lteForced == 0) {
                            mButtonEnabledNetworks.setEntries(
                                    R.array.enabled_networks_cdma_choices);
                            mButtonEnabledNetworks.setEntryValues(
                                    R.array.enabled_networks_cdma_values);
                        } else {
                            switch (settingsNetworkMode) {
                                case Phone.NT_MODE_CDMA:
                                case Phone.NT_MODE_CDMA_NO_EVDO:
                                case Phone.NT_MODE_EVDO_NO_CDMA:
                                    mButtonEnabledNetworks.setEntries(
                                            R.array.enabled_networks_cdma_no_lte_choices);
                                    mButtonEnabledNetworks.setEntryValues(
                                            R.array.enabled_networks_cdma_no_lte_values);
                                    break;
                                case Phone.NT_MODE_GLOBAL:
                                case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                                case Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                                case Phone.NT_MODE_LTE_ONLY:
                                    mButtonEnabledNetworks.setEntries(
                                            R.array.enabled_networks_cdma_only_lte_choices);
                                    mButtonEnabledNetworks.setEntryValues(
                                            R.array.enabled_networks_cdma_only_lte_values);
                                    break;
                                default:
                                    mButtonEnabledNetworks.setEntries(
                                            R.array.enabled_networks_cdma_choices);
                                    mButtonEnabledNetworks.setEntryValues(
                                            R.array.enabled_networks_cdma_values);
                                    break;
                            }
                        }
                    }
                    mCdmaOptions = new CdmaOptions(this, prefSet, mPhone);

                    // In World mode force a refresh of GSM Options.
                    if (isWorldMode()) {
                        mGsmUmtsOptions = null;
                    }
                } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                    if (isSupportTdscdma()) {
                        mButtonEnabledNetworks.setEntries(
                                R.array.enabled_networks_tdscdma_choices);
                        mButtonEnabledNetworks.setEntryValues(
                                R.array.enabled_networks_tdscdma_values);
                    } else if (!carrierConfig.getBoolean(CarrierConfigManager.KEY_PREFER_2G_BOOL)
                            && !getResources().getBoolean(R.bool.config_enabled_lte)) {
                        mButtonEnabledNetworks.setEntries(
                                R.array.enabled_networks_except_gsm_lte_choices);
                        mButtonEnabledNetworks.setEntryValues(
                                R.array.enabled_networks_except_gsm_lte_values);
                    } else if (!carrierConfig.getBoolean(CarrierConfigManager.KEY_PREFER_2G_BOOL)) {
                        int select = (mShow4GForLTE == true) ?
                                R.array.enabled_networks_except_gsm_4g_choices
                                : R.array.enabled_networks_except_gsm_choices;
                        mButtonEnabledNetworks.setEntries(select);
                        mButtonEnabledNetworks.setEntryValues(
                                R.array.enabled_networks_except_gsm_values);
                    } else if (!FeatureOption.isMtkLteSupport()) {
                        mButtonEnabledNetworks.setEntries(
                                R.array.enabled_networks_except_lte_choices);
                        if (isC2kLteSupport() && FeatureOption.isNeedDisable4G()) {
                            log("for bad phone change entries~");
                            mButtonEnabledNetworks.setEntryValues(
                                    R.array.enabled_networks_except_lte_values_c2k);
                        } else {
                            mButtonEnabledNetworks.setEntryValues(
                                    R.array.enabled_networks_except_lte_values);
                        }
                    } else if (mIsGlobalCdma) {
                        mButtonEnabledNetworks.setEntries(
                                R.array.enabled_networks_cdma_choices);
                        mButtonEnabledNetworks.setEntryValues(
                                R.array.enabled_networks_cdma_values);
                    } else {
                        int select = (mShow4GForLTE == true) ? R.array.enabled_networks_4g_choices
                                : R.array.enabled_networks_choices;
                        mButtonEnabledNetworks.setEntries(select);
                        ExtensionManager.getMobileNetworkSettingsExt().changeEntries(
                                mButtonEnabledNetworks);
                        /// Add for C2K @{
                        if (isC2kLteSupport()) {
                            if (DBG) {
                                log("Change to C2K values");
                            }
                            mButtonEnabledNetworks.setEntryValues(
                                    R.array.enabled_networks_values_c2k);
                        } else {
                            mButtonEnabledNetworks.setEntryValues(
                                    R.array.enabled_networks_values);
                        }
                        /// @}
                    }
                    mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet, phoneSubId);
                } else {
                    throw new IllegalStateException("Unexpected phone type: " + phoneType);
                }
                if (isWorldMode()) {
                    mButtonEnabledNetworks.setEntries(
                            R.array.preferred_network_mode_choices_world_mode);
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.preferred_network_mode_values_world_mode);
                }
                mButtonEnabledNetworks.setOnPreferenceChangeListener(this);
                if (DBG) log("settingsNetworkMode: " + settingsNetworkMode);
            }

            final boolean missingDataServiceUrl = TextUtils.isEmpty(
                    android.provider.Settings.Global.getString(getContentResolver(),
                            android.provider.Settings.Global.SETUP_PREPAID_DATA_SERVICE_URL));
            if (!isLteOnCdma || missingDataServiceUrl) {
                prefSet.removePreference(mLteDataServicePref);
            } else {
                android.util.Log.d(LOG_TAG, "keep ltePref");
            }
            /// M: add mtk feature.
            onCreateMTK(prefSet);
            // Enable enhanced 4G LTE mode settings depending on whether exists on platform
             // Enable enhanced 4G LTE mode settings depending on whether exists on platform
            /** M: Add For [MTK_Enhanced4GLTE] @{
            ImsManager imsManager = ImsManager.getInstance(getApplicationContext(),
                    mPhone.getPhoneId());
            if (!(isVolteEnabled() &&
                    MtkImsManager.isVolteProvisionedOnDevice(this))) {
                Preference pref = prefSet.findPreference(BUTTON_4G_LTE_KEY);
                if (pref != null) {
                    prefSet.removePreference(pref);
                }
            }
            @} * /
            if (carrierConfig.getBoolean(MtkCarrierConfigManager.MTK_KEY_ROAMING_BAR_GUARD_BOOL)) {
                int order = mButtonDataRoam.getOrder();
                prefSet.removePreference(mButtonDataRoam);
                Preference sprintPreference = new Preference(this);
                sprintPreference.setKey(BUTTON_SPRINT_ROAMING_SETTINGS);
                sprintPreference.setTitle(R.string.roaming_settings);
                Intent intentRoaming = new Intent();
                intentRoaming.setClassName("com.android.phone",
                        "com.mediatek.services.telephony.RoamingSettings");
                intentRoaming.putExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, mPhone.getSubId());
                sprintPreference.setIntent(intentRoaming);
                sprintPreference.setOrder(order);
                prefSet.addPreference(sprintPreference);
            }
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                // android.R.id.home will be triggered in onOptionsItemSelected()
                actionBar.setDisplayHomeAsUpEnabled(true);
            }

            // Enable link to CMAS app settings depending on the value in config.xml.
            final boolean isCellBroadcastAppLinkEnabled = getResources().getBoolean(
                    com.android.internal.R.bool.config_cellBroadcastAppLinks);
            if (!mUm.isAdminUser() || !isCellBroadcastAppLinkEnabled
                    || mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_CELL_BROADCASTS)) {
                PreferenceScreen root = getPreferenceScreen();
                Preference ps = findPreference(BUTTON_CELL_BROADCAST_SETTINGS);
                if (ps != null) {
                    root.removePreference(ps);
                }
            }

            // Get the networkMode from Settings.System and displays it
            mButtonDataRoam.setChecked(mPhone.getDataRoamingEnabled());
            //* / freeme.wanglei, 20170713. add cellular data switch.
            updateChecked();
            //* /
            mButtonEnabledNetworks.setValue(Integer.toString(settingsNetworkMode));
            mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
            UpdatePreferredNetworkModeSummary(settingsNetworkMode);
            UpdateEnabledNetworksValueAndSummary(settingsNetworkMode);
            // Display preferred network type based on what modem returns b/18676277
            /// M: no need set mode here
            //mPhone.setPreferredNetworkType(settingsNetworkMode, mHandler
            //        .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));

            /**
             * Enable/disable depending upon if there are any active subscriptions.
             *
             * I've decided to put this enable/disable code at the bottom as the
             * code above works even when there are no active subscriptions, thus
             * putting it afterwards is a smaller change. This can be refined later,
             * but you do need to remember that this all needs to work when subscriptions
             * change dynamically such as when hot swapping sims.

            boolean hasActiveSubscriptions = hasActiveSubscriptions();
            TelephonyManager tm = (TelephonyManager) getSystemService(
                    Context.TELEPHONY_SERVICE);
            boolean canChange4glte = (tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) &&
                    ImsManager.isNonTtyOrTtyOnVolteEnabled(getApplicationContext()) &&
                    carrierConfig.getBoolean(
                            CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL);
            boolean useVariant4glteTitle = carrierConfig.getBoolean(
                    CarrierConfigManager.KEY_ENHANCED_4G_LTE_TITLE_VARIANT_BOOL);
            int enhanced4glteModeTitleId = useVariant4glteTitle ?
                    R.string.enhanced_4g_lte_mode_title_variant :
                    R.string.enhanced_4g_lte_mode_title;
            * /
            mButtonDataRoam.setDisabledByAdmin(false);
            //mButtonDataRoam.setEnabled(hasActiveSubscriptions);
            if (mButtonDataRoam.isEnabled()) {
                if (RestrictedLockUtils.hasBaseUserRestriction(context,
                        UserManager.DISALLOW_DATA_ROAMING, UserHandle.myUserId())) {
                    mButtonDataRoam.setEnabled(false);
                } else {
                    mButtonDataRoam.checkRestrictionAndSetDisabled(
                            UserManager.DISALLOW_DATA_ROAMING);
                }
            }
            /*
            mButtonPreferredNetworkMode.setEnabled(hasActiveSubscriptions);
            mButtonEnabledNetworks.setEnabled(hasActiveSubscriptions);
            mButton4glte.setTitle(enhanced4glteModeTitleId);
            mButton4glte.setEnabled(hasActiveSubscriptions && canChange4glte);
            mLteDataServicePref.setEnabled(hasActiveSubscriptions);
            Preference ps;
            PreferenceScreen root = getPreferenceScreen();
            ps = findPreference(BUTTON_CELL_BROADCAST_SETTINGS);
            if (ps != null) {
                ps.setEnabled(hasActiveSubscriptions);
            }
            ps = findPreference(BUTTON_APN_EXPAND_KEY);
            if (ps != null) {
                ps.setEnabled(hasActiveSubscriptions);
            }
            ps = findPreference(BUTTON_OPERATOR_SELECTION_EXPAND_KEY);
            if (ps != null) {
                ps.setEnabled(hasActiveSubscriptions);
            }
            ps = findPreference(BUTTON_CARRIER_SETTINGS_KEY);
            if (ps != null) {
                ps.setEnabled(hasActiveSubscriptions);
            }
            ps = findPreference(BUTTON_CDMA_SYSTEM_SELECT_KEY);
            if (ps != null) {
                ps.setEnabled(hasActiveSubscriptions);
            }* /
            /// Add for cmcc open market @{
            mOmEx.updateNetworkTypeSummary(mButtonEnabledNetworks);
            /// @}
            /// M: Add for L+W DSDS.
            if (ExtensionManager.getMobileNetworkSettingsExt().isNetworkModeSettingNeeded()) {
                updateNetworkModeForLwDsds();
            }

            /// M: Add for Plug-in @{
            if (mButtonEnabledNetworks != null) {
                log("Enter plug-in update updateNetworkTypeSummary - Enabled again!");
                ExtensionManager.getMobileNetworkSettingsExt()
                        .updateNetworkTypeSummary(mButtonEnabledNetworks);
            }
            /// @}
        }

        @Override
        public void onPause() {
            /// M: For plugin to update UI
            ExtensionManager.getMobileNetworkSettingsExt().onPause();
            super.onPause();
            if (DBG) {
                log("onPause");
            }
            //* / freeme.wanglei, 20170713. add cellular data switch.
            mListener.setListener(false, mDefaultSubscriptionId, getApplicationContext());
            //* /
        }

        /**
         * Implemented to support onPreferenceChangeListener to look for preference
         * changes specifically on CLIR.
         *
         * @param preference is the preference to be changed, should be mButtonCLIR.
         * @param objValue should be the value of the selection, NOT its localized
         * display value.
         * /
        public boolean onPreferenceChange(Preference preference, Object objValue) {
            final int phoneSubId = mPhone.getSubId();
            if (onPreferenceChangeMTK(preference, objValue)) {
                return true;
            }
            if (preference == mButtonPreferredNetworkMode) {
                //NOTE onPreferenceChange seems to be called even if there is no change
                //Check if the button value is changed from the System.Setting
                mButtonPreferredNetworkMode.setValue((String) objValue);
                int buttonNetworkMode;
                buttonNetworkMode = Integer.parseInt((String) objValue);
                int settingsNetworkMode = android.provider.Settings.Global.getInt(
                        mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                        preferredNetworkMode);
                log("onPreferenceChange buttonNetworkMode:"
                    + buttonNetworkMode + " settingsNetworkMode:" + settingsNetworkMode);
                if (buttonNetworkMode != settingsNetworkMode) {
                    int modemNetworkMode;
                    // if new mode is invalid ignore it
                    switch (buttonNetworkMode) {
                        case Phone.NT_MODE_WCDMA_PREF:
                        case Phone.NT_MODE_GSM_ONLY:
                        case Phone.NT_MODE_WCDMA_ONLY:
                        case Phone.NT_MODE_GSM_UMTS:
                        case Phone.NT_MODE_CDMA:
                        case Phone.NT_MODE_CDMA_NO_EVDO:
                        case Phone.NT_MODE_EVDO_NO_CDMA:
                        case Phone.NT_MODE_GLOBAL:
                        case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                        case Phone.NT_MODE_LTE_GSM_WCDMA:
                        case Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                        case Phone.NT_MODE_LTE_ONLY:
                        case Phone.NT_MODE_LTE_WCDMA:
                        case Phone.NT_MODE_TDSCDMA_ONLY:
                        case Phone.NT_MODE_TDSCDMA_WCDMA:
                        case Phone.NT_MODE_LTE_TDSCDMA:
                        case Phone.NT_MODE_TDSCDMA_GSM:
                        case Phone.NT_MODE_LTE_TDSCDMA_GSM:
                        case Phone.NT_MODE_TDSCDMA_GSM_WCDMA:
                        case Phone.NT_MODE_LTE_TDSCDMA_WCDMA:
                        case Phone.NT_MODE_LTE_TDSCDMA_GSM_WCDMA:
                        case Phone.NT_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                        case Phone.NT_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                            // This is one of the modes we recognize
                            modemNetworkMode = buttonNetworkMode;
                            break;
                        default:
                            loge("Invalid Network Mode (" + buttonNetworkMode +
                                    ") chosen. Ignore.");
                            return true;
                    }

                    mButtonPreferredNetworkMode.setValue(Integer.toString(modemNetworkMode));
                    mButtonPreferredNetworkMode.setSummary(mButtonPreferredNetworkMode.getEntry());

                    /// M: 03100374, need to revert the network mode if set fail
                    mPreNetworkMode = settingsNetworkMode;

                    android.provider.Settings.Global.putInt(
                            mPhone.getContext().getContentResolver(),
                            android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                            buttonNetworkMode);
                    if (DBG) {
                        log("setPreferredNetworkType, networkType: " + modemNetworkMode);
                    }
                    //Set the modem network mode
                    mPhone.setPreferredNetworkType(modemNetworkMode, mHandler
                            .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
                }
            } else if (preference == mButtonEnabledNetworks) {
                mButtonEnabledNetworks.setValue((String) objValue);
                int buttonNetworkMode;
                buttonNetworkMode = Integer.parseInt((String) objValue);
                if (DBG) log("buttonNetworkMode: " + buttonNetworkMode);
                int settingsNetworkMode = android.provider.Settings.Global.getInt(
                        mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                        preferredNetworkMode);

                if (DBG) {
                    log("buttonNetworkMode: " + buttonNetworkMode +
                        "settingsNetworkMode: " + settingsNetworkMode);
                }
                if (buttonNetworkMode != settingsNetworkMode ||
                        ExtensionManager.getMobileNetworkSettingsExt().isNetworkChanged(
                            mButtonEnabledNetworks, buttonNetworkMode, settingsNetworkMode,
                            mPhone)) {
                    int modemNetworkMode;
                    // if new mode is invalid ignore it
                    switch (buttonNetworkMode) {
                        case Phone.NT_MODE_WCDMA_PREF:
                        case Phone.NT_MODE_GSM_ONLY:
                        case Phone.NT_MODE_LTE_GSM_WCDMA:
                        case Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                        case Phone.NT_MODE_CDMA:
                        case Phone.NT_MODE_CDMA_NO_EVDO:
                        case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                        case Phone.NT_MODE_TDSCDMA_ONLY:
                        case Phone.NT_MODE_TDSCDMA_WCDMA:
                        case Phone.NT_MODE_LTE_TDSCDMA:
                        case Phone.NT_MODE_LTE_WCDMA:
                        case Phone.NT_MODE_TDSCDMA_GSM:
                        case Phone.NT_MODE_LTE_TDSCDMA_GSM:
                        case Phone.NT_MODE_TDSCDMA_GSM_WCDMA:
                        case Phone.NT_MODE_LTE_TDSCDMA_WCDMA:
                        case Phone.NT_MODE_LTE_TDSCDMA_GSM_WCDMA:
                        case Phone.NT_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                        case Phone.NT_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                        case Phone.NT_MODE_WCDMA_ONLY:
                        case Phone.NT_MODE_LTE_ONLY:
                        /// M: Add for C2K
                        case Phone.NT_MODE_GLOBAL:
                            // This is one of the modes we recognize
                            modemNetworkMode = buttonNetworkMode;
                            break;
                        default:
                            loge("Invalid Network Mode (" + buttonNetworkMode +
                                    ") chosen. Ignore.");
                            return true;
                    }

                    UpdateEnabledNetworksValueAndSummary(buttonNetworkMode);

                    /// M: 03100374, need to revert the network mode if set fail
                    mPreNetworkMode = settingsNetworkMode;


                    if (ExtensionManager.getMobileNetworkSettingsExt().isNetworkUpdateNeeded(
                            mButtonEnabledNetworks,
                            buttonNetworkMode,
                            settingsNetworkMode,
                            mPhone,
                            mPhone.getContext().getContentResolver(),
                            phoneSubId, mHandler)) {
                        UpdateEnabledNetworksValueAndSummary(buttonNetworkMode);
                        android.provider.Settings.Global.putInt(mPhone.getContext().
                                getContentResolver(),
                                android.provider.Settings.Global.PREFERRED_NETWORK_MODE +
                                phoneSubId,
                                buttonNetworkMode);

                        //if (DBG) {
                            log("setPreferredNetworkType, networkType: " + modemNetworkMode);
                        //}
                        //Set the modem network mode
                        mPhone.setPreferredNetworkType(modemNetworkMode, mHandler
                                .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
                    }
                }
            } else if (preference == mButton4glte) {
                SwitchPreference enhanced4gModePref = (SwitchPreference) preference;
                boolean enhanced4gMode = !enhanced4gModePref.isChecked();
                enhanced4gModePref.setChecked(enhanced4gMode);
                MtkImsManager.setEnhanced4gLteModeSetting(this, enhanced4gModePref.isChecked(),
                        mPhone.getPhoneId());
            //* / freeme.wanglei, 20170713. add cellular data switch.
            } else if (preference == mButtonDataNetwork) {
                setMobileDataEnabled(!mButtonDataNetwork.isChecked());
                disableDataForOtherSubscriptions(mDefaultSubscriptionId);
            //* /
            } else if (preference == mButtonDataRoam) {
                if (DBG) log("onPreferenceTreeClick: preference == mButtonDataRoam.");

                //normally called on the toggle click
                if (!mButtonDataRoam.isChecked()) {
                    // First confirm with a warning dialog about charges
                    mOkClicked = false;
                    /// M:Add for plug-in @{
                    /* Google Code, delete by MTK
                    new AlertDialog.Builder(this).setMessage(
                            getResources().getString(R.string.roaming_warning))
                            .setTitle(android.R.string.dialog_alert_title)
                    * /
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(getResources().getString(R.string.roaming_warning))
                            .setTitle(android.R.string.dialog_alert_title);
                    ExtensionManager.getMobileNetworkSettingsExt().customizeAlertDialog(
                            mButtonDataRoam, builder);
                    builder.setIconAttribute(android.R.attr.alertDialogIcon)
                            .setPositiveButton(android.R.string.yes, this)
                            .setNegativeButton(android.R.string.no, this)
                            .show()
                            .setOnDismissListener(this);
                    /// @}
                } else {
                    mPhone.setDataRoamingEnabled(false);
                }
            }
            /// Add for Plug-in @{
            ExtensionManager.getMobileNetworkSettingsExt().onPreferenceChange(preference, objValue);
            /// @}
            //updateBody();
            // always let the preference setting proceed.
            return true;
        }

        private class MyHandler extends Handler {

            static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 0;

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_SET_PREFERRED_NETWORK_TYPE:
                        handleSetPreferredNetworkTypeResponse(msg);
                        break;
                }
            }

            private void handleSetPreferredNetworkTypeResponse(Message msg) {
                /// M: 03100374 restore network mode in case set fail
                restorePreferredNetworkTypeIfNeeded(msg);
                if (isDestroyed()) {
                    // Access preferences of activity only if it is not destroyed
                    // or if fragment is not attached to an activity.
                    return;
                }

                AsyncResult ar = (AsyncResult) msg.obj;
                final int phoneSubId = mPhone.getSubId();

                if (ar.exception == null) {
                    int networkMode;
                    if (getPreferenceScreen().findPreference(
                            BUTTON_PREFERED_NETWORK_MODE) != null)  {
                        networkMode =  Integer.parseInt(mButtonPreferredNetworkMode.getValue());
                        if (DBG) {
                            log("handleSetPreferredNetwrokTypeResponse1: networkMode:" +
                                    networkMode);
                        }
                        android.provider.Settings.Global.putInt(
                                mPhone.getContext().getContentResolver(),
                                android.provider.Settings.Global.PREFERRED_NETWORK_MODE
                                        + phoneSubId,
                                networkMode);
                    }
                    if (getPreferenceScreen().findPreference(BUTTON_ENABLED_NETWORKS_KEY) != null) {
                        networkMode = Integer.parseInt(mButtonEnabledNetworks.getValue());
                        if (DBG) {
                            log("handleSetPreferredNetwrokTypeResponse2: networkMode:" +
                                    networkMode);
                        }
                        android.provider.Settings.Global.putInt(
                                mPhone.getContext().getContentResolver(),
                                android.provider.Settings.Global.PREFERRED_NETWORK_MODE
                                        + phoneSubId,
                                networkMode);
                    }
                    log("Start Network updated intent");
                    Intent intent = new Intent(TelephonyUtils.ACTION_NETWORK_CHANGED);
                    sendBroadcast(intent);
                } else {
                    Log.i(LOG_TAG, "handleSetPreferredNetworkTypeResponse:" +
                            "exception in setting network mode.");
                    updatePreferredNetworkUIFromDb();
                }
            }
        }

        private void updatePreferredNetworkUIFromDb() {
            final int phoneSubId = mPhone.getSubId();

            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                    preferredNetworkMode);

            if (DBG) {
                log("updatePreferredNetworkUIFromDb: settingsNetworkMode = " +
                        settingsNetworkMode);
            }

            UpdatePreferredNetworkModeSummary(settingsNetworkMode);
            UpdateEnabledNetworksValueAndSummary(settingsNetworkMode);
            // changes the mButtonPreferredNetworkMode accordingly to settingsNetworkMode
            mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
        }

        private void UpdatePreferredNetworkModeSummary(int NetworkMode) {
            // M: if is not 3/4G phone, init the preference with gsm only type @{
            if (!isCapabilityPhone(mPhone)) {
                NetworkMode = Phone.NT_MODE_GSM_ONLY;
                log("init PreferredNetworkMode with gsm only");
            }
            // @}
            switch (NetworkMode) {
                case Phone.NT_MODE_TDSCDMA_GSM_WCDMA:
                case Phone.NT_MODE_TDSCDMA_GSM:
                case Phone.NT_MODE_WCDMA_PREF:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_wcdma_perf_summary);
                    break;
                case Phone.NT_MODE_GSM_ONLY:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_gsm_only_summary);
                    break;
                case Phone.NT_MODE_TDSCDMA_WCDMA:
                case Phone.NT_MODE_WCDMA_ONLY:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_wcdma_only_summary);
                    break;
                case Phone.NT_MODE_GSM_UMTS:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_gsm_wcdma_summary);
                    break;
                case Phone.NT_MODE_CDMA:
                    switch (mPhone.getLteOnCdmaMode()) {
                        case PhoneConstants.LTE_ON_CDMA_TRUE:
                            mButtonPreferredNetworkMode.setSummary(
                                    R.string.preferred_network_mode_cdma_summary);
                            break;
                        case PhoneConstants.LTE_ON_CDMA_FALSE:
                        default:
                            mButtonPreferredNetworkMode.setSummary(
                                    R.string.preferred_network_mode_cdma_evdo_summary);
                            break;
                    }
                    break;
                case Phone.NT_MODE_CDMA_NO_EVDO:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_cdma_only_summary);
                    break;
                case Phone.NT_MODE_EVDO_NO_CDMA:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_evdo_only_summary);
                    break;
                case Phone.NT_MODE_LTE_TDSCDMA:
                case Phone.NT_MODE_LTE_ONLY:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_lte_summary);
                    break;
                case Phone.NT_MODE_LTE_TDSCDMA_GSM:
                case Phone.NT_MODE_LTE_TDSCDMA_GSM_WCDMA:
                case Phone.NT_MODE_LTE_GSM_WCDMA:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_lte_gsm_wcdma_summary);
                    break;
                case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_lte_cdma_evdo_summary);
                    break;
                case Phone.NT_MODE_TDSCDMA_ONLY:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_tdscdma_summary);
                    break;
                case Phone.NT_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                case Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                    if (mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA ||
                            mIsGlobalCdma ||
                            isWorldMode()) {
                        mButtonPreferredNetworkMode.setSummary(
                                R.string.preferred_network_mode_global_summary);
                    } else {
                        mButtonPreferredNetworkMode.setSummary(
                                R.string.preferred_network_mode_lte_summary);
                    }
                    break;
                case Phone.NT_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                case Phone.NT_MODE_GLOBAL:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_cdma_evdo_gsm_wcdma_summary);
                    break;
                case Phone.NT_MODE_LTE_TDSCDMA_WCDMA:
                case Phone.NT_MODE_LTE_WCDMA:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_lte_wcdma_summary);
                    break;
                default:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_global_summary);
            }
            /// Add for Plug-in @{
            ExtensionManager.getMobileNetworkSettingsExt()
                    .updateNetworkTypeSummary(mButtonPreferredNetworkMode);
            /// @}
            /// Add for cmcc open market @{
            mOmEx.updateNetworkTypeSummary(mButtonPreferredNetworkMode);
            /// @}
        }

        private void UpdateEnabledNetworksValueAndSummary(int NetworkMode) {
            Log.d(LOG_TAG, "NetworkMode: " + NetworkMode);
            // M: if is not 3/4G phone, init the preference with gsm only type @{
            if (!isCapabilityPhone(mPhone)) {
                NetworkMode = Phone.NT_MODE_GSM_ONLY;
                log("init EnabledNetworks with gsm only");
            }
            // @}
            switch (NetworkMode) {
                case Phone.NT_MODE_TDSCDMA_WCDMA:
                case Phone.NT_MODE_TDSCDMA_GSM_WCDMA:
                case Phone.NT_MODE_TDSCDMA_GSM:
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_TDSCDMA_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary(R.string.network_3G);
                    break;
                case Phone.NT_MODE_WCDMA_ONLY:
                case Phone.NT_MODE_GSM_UMTS:
                case Phone.NT_MODE_WCDMA_PREF:
                    if (!mIsGlobalCdma) {
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_WCDMA_PREF));
                        mButtonEnabledNetworks.setSummary(R.string.network_3G);
                    } else {
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                        mButtonEnabledNetworks.setSummary(R.string.network_global);
                    }
                    break;
                case Phone.NT_MODE_GSM_ONLY:
                    if (!mIsGlobalCdma) {
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_GSM_ONLY));
                        mButtonEnabledNetworks.setSummary(R.string.network_2G);
                    } else {
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                        mButtonEnabledNetworks.setSummary(R.string.network_global);
                    }
                    break;
                case Phone.NT_MODE_LTE_GSM_WCDMA:
                    if (isWorldMode()) {
                        mButtonEnabledNetworks.setSummary(
                                R.string.preferred_network_mode_lte_gsm_umts_summary);
                        controlCdmaOptions(false);
                        controlGsmOptions(true);
                        break;
                    }
                case Phone.NT_MODE_LTE_ONLY:
                case Phone.NT_MODE_LTE_WCDMA:
                    if (!mIsGlobalCdma) {
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_LTE_GSM_WCDMA));
                        mButtonEnabledNetworks.setSummary((mShow4GForLTE == true)
                                ? R.string.network_4G : R.string.network_lte);
                    } else {
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                        mButtonEnabledNetworks.setSummary(R.string.network_global);
                    }
                    break;
                case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                    if (isWorldMode()) {
                        mButtonEnabledNetworks.setSummary(
                                R.string.preferred_network_mode_lte_cdma_summary);
                        controlCdmaOptions(true);
                        controlGsmOptions(false);
                    } else {
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_LTE_CDMA_AND_EVDO));
                        mButtonEnabledNetworks.setSummary(R.string.network_lte);
                    }
                    break;
                case Phone.NT_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary(R.string.network_3G);
                    break;
                case Phone.NT_MODE_CDMA:
                case Phone.NT_MODE_EVDO_NO_CDMA:
                case Phone.NT_MODE_GLOBAL:
                    /// M: For C2K @{
                    if (isC2kLteSupport()) {
                        log("Update value to Global for c2k project");
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_GLOBAL));
                    } else {
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_CDMA));
                    }
                    /// @}

                    mButtonEnabledNetworks.setSummary(R.string.network_3G);
                    break;
                case Phone.NT_MODE_CDMA_NO_EVDO:
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_CDMA_NO_EVDO));
                    mButtonEnabledNetworks.setSummary(R.string.network_1x);
                    break;
                case Phone.NT_MODE_TDSCDMA_ONLY:
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_TDSCDMA_ONLY));
                    mButtonEnabledNetworks.setSummary(R.string.network_3G);
                    break;
                case Phone.NT_MODE_LTE_TDSCDMA_GSM:
                case Phone.NT_MODE_LTE_TDSCDMA_GSM_WCDMA:
                case Phone.NT_MODE_LTE_TDSCDMA:
                case Phone.NT_MODE_LTE_TDSCDMA_WCDMA:
                case Phone.NT_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                case Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                    if (isSupportTdscdma()) {
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA));
                        mButtonEnabledNetworks.setSummary(R.string.network_lte);
                    } else {
                        if (isWorldMode()) {
                            controlCdmaOptions(true);
                            controlGsmOptions(false);
                        }
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                        if (mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA ||
                                mIsGlobalCdma ||
                                isWorldMode()) {
                            mButtonEnabledNetworks.setSummary(R.string.network_global);
                        } else {
                            mButtonEnabledNetworks.setSummary((mShow4GForLTE == true)
                                    ? R.string.network_4G : R.string.network_lte);
                        }
                    }
                    break;
                default:
                    String errMsg = "Invalid Network Mode (" + NetworkMode + "). Ignore.";
                    loge(errMsg);
                    mButtonEnabledNetworks.setSummary(errMsg);
            }
            ExtensionManager.getMobileNetworkSettingsExt().
            updatePreferredNetworkValueAndSummary(mButtonEnabledNetworks, NetworkMode);
            /// Add for Plug-in @{
            if (mButtonEnabledNetworks != null) {
                log("Enter plug-in update updateNetworkTypeSummary - Enabled.");
                ExtensionManager.getMobileNetworkSettingsExt()
                        .updateNetworkTypeSummary(mButtonEnabledNetworks);
                /// Add for cmcc open market @{
                mOmEx.updateNetworkTypeSummary(mButtonEnabledNetworks);
                /// @}
            }
            /// @}
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case REQUEST_CODE_EXIT_ECM:
                    Boolean isChoiceYes = data.getBooleanExtra(
                            EmergencyCallbackModeExitDialog.EXTRA_EXIT_ECM_RESULT, false);
                    if (isChoiceYes && mClickedPreference != null) {
                        // If the phone exits from ECM mode, show the CDMA Options
                        mCdmaOptions.showDialog(mClickedPreference);
                    } else {
                        // do nothing
                    }
                    break;

                default:
                    break;
            }
        }

        private static void log(String msg) {
            Log.d(LOG_TAG, msg);
        }

        private static void loge(String msg) {
            Log.e(LOG_TAG, msg);
        }

        private boolean isWorldMode() {
            boolean worldModeOn = false;
            final TelephonyManager tm =
                    (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            final String configString = getResources().getString(R.string.config_world_mode);

            if (!TextUtils.isEmpty(configString)) {
                String[] configArray = configString.split(";");
                // Check if we have World mode configuration set to True
                // only or config is set to True
                // and SIM GID value is also set and matches to the current SIM GID.
                if (configArray != null &&
                        ((configArray.length == 1 && configArray[0].equalsIgnoreCase("true"))
                                || (configArray.length == 2 && !TextUtils.isEmpty(configArray[1])
                                && tm != null
                                && configArray[1].equalsIgnoreCase(tm.getGroupIdLevel1())))) {
                    worldModeOn = true;
                }
            }

            Log.d(LOG_TAG, "isWorldMode=" + worldModeOn);

            return worldModeOn;
        }

        private void controlGsmOptions(boolean enable) {
            PreferenceScreen prefSet = getPreferenceScreen();
            if (prefSet == null) {
                return;
            }

            if (mGsmUmtsOptions == null) {
                mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet, mPhone.getSubId());
            }
            PreferenceScreen apnExpand =
                    (PreferenceScreen) prefSet.findPreference(BUTTON_APN_EXPAND_KEY);
            PreferenceScreen operatorSelectionExpand =
                    (PreferenceScreen) prefSet.findPreference(BUTTON_OPERATOR_SELECTION_EXPAND_KEY);
            PreferenceScreen carrierSettings =
                    (PreferenceScreen) prefSet.findPreference(BUTTON_CARRIER_SETTINGS_KEY);
            if (apnExpand != null) {
                apnExpand.setEnabled(isWorldMode() || enable);
            }
            if (operatorSelectionExpand != null) {
                if (enable) {
                    operatorSelectionExpand.setEnabled(true);
                } else {
                    prefSet.removePreference(operatorSelectionExpand);
                }
            }
            if (carrierSettings != null) {
                prefSet.removePreference(carrierSettings);
            }
        }

        private void controlCdmaOptions(boolean enable) {
            PreferenceScreen prefSet = getPreferenceScreen();
            if (prefSet == null) {
                return;
            }
            if (enable && mCdmaOptions == null) {
                mCdmaOptions = new CdmaOptions(this, prefSet, mPhone);
            }
            CdmaSystemSelectListPreference systemSelect =
                    (CdmaSystemSelectListPreference) prefSet.findPreference
                            (BUTTON_CDMA_SYSTEM_SELECT_KEY);
            if (systemSelect != null) {
                systemSelect.setEnabled(enable);
            }
        }

        private boolean isSupportTdscdma() {
            /// M: TODO: temple solution for MR1 changes
            /*if (getResources().getBoolean(R.bool.config_support_tdscdma)) {
                return true;
            }

            String operatorNumeric = mPhone.getServiceState().getOperatorNumeric();
            String[] numericArray = getResources().getStringArray(
                    R.array.config_support_tdscdma_roaming_on_networks);
            if (numericArray.length == 0 || operatorNumeric == null) {
                return false;
            }
            for (String numeric : numericArray) {
                if (operatorNumeric.equals(numeric)) {
                    return true;
                }
            }* /
            return false;
        }

        private void dissmissDialog(ListPreference preference) {
            Dialog dialog = null;
            if (preference != null) {
                dialog = preference.getDialog();
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        }

        // -------------------- Mediatek ---------------------
        // M: Add for cmcc open market
        private MobileNetworkSettingsOmEx mOmEx;
        /// M: add for plmn list
        public static final String BUTTON_PLMN_LIST = "button_plmn_key";
        private static final String BUTTON_CDMA_ACTIVATE_DEVICE_KEY = "cdma_activate_device_key";
        private static final String BUTTON_SPRINT_ROAMING_SETTINGS = "sprint_roaming_settings";
        /// M: c2k 4g data only
        private static final String SINGLE_LTE_DATA = "single_lte_data";
        /// M: for screen rotate @{
        private static final String CURRENT_TAB = "current_tab";
        private int mCurrentTab = 0;
        /// @}
        private Preference mPLMNPreference;
        private IntentFilter mIntentFilter;

        /// M: 03100374 restore network mode in case set fail
        private int mPreNetworkMode = -1;
        private boolean mNetworkRegister = false;

        private Dialog mDialog;
        private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DBG) {
                    log("action: " + action);
                }
                /// When receive aiplane mode, we would like to finish the activity, for
                //  we can't get the modem capability, and will show the user selected network
                //  mode as summary, this will make user misunderstand.(ALPS01971666)
                if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                    finish();
                } else if (action.equals(MtkTelephonyIntents.ACTION_MSIM_MODE_CHANGED)
                        || action.equals(MtkTelephonyIntents.ACTION_MD_TYPE_CHANGE)
                        || action.equals(MtkTelephonyIntents.ACTION_LOCATED_PLMN_CHANGED)
                        || ExtensionManager.getMobileNetworkSettingsExt()
                                .customizeDualVolteReceiveIntent(action)) {
                    updateScreenStatus();
                } else if (action.equals(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_DONE)) {
                    if (DBG) {
                        log("Siwtch done Action ACTION_SET_PHONE_RAT_FAMILY_DONE received ");
                    }
                    mPhone = PhoneUtils.getPhoneUsingSubId(mPhone.getSubId());
                    updateScreenStatus();
                } else if (action.equals(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED)) {
                    // When the radio changes (ex: CDMA->GSM), refresh all options.
                    mGsmUmtsOptions = null;
                    mCdmaOptions = null;
                    updateBody();
                }
                /// @}
            }
        };

        /**
         * Add Preferences based on customer requirement to preference screen.
         * @param prefSet Preference screen that needs to be updated.
         * /
        private void onCreateMTK(PreferenceScreen prefSet) {

            /// M: Add For [MTK_Enhanced4GLTE] @{
            addEnhanced4GLteSwitchPreference(prefSet);
            /// @}
            /// M: Add for plmn list @{
            if ((!FeatureOption.isMtk3gDongleSupport() && FeatureOption.isMtkCtaSet()
                    && !TelephonyUtilsEx.isCDMAPhone(mPhone))
                    /// M: [CT VOLTE]
                    && !(TelephonyUtilsEx.isCtVolteEnabled() && TelephonyUtilsEx.isCt4gSim(mPhone
                            .getSubId()))) {
                if (DBG) {
                    log("---addPLMNList---");
                }
                addPLMNList(prefSet);
            }
            /// M: [CT VOLTE Network UI]
            if (TelephonyUtilsEx.isCtVolteEnabled()
                    && TelephonyUtilsEx.isCt4gSim(mPhone.getSubId())) {
                if (mNetworkRegister) {
                    getContentResolver().unregisterContentObserver(mNetworkObserver);
                }
                getContentResolver().registerContentObserver(
                        Settings.Global.getUriFor(Settings.Global.PREFERRED_NETWORK_MODE +
                        mPhone.getSubId()),
                        true, mNetworkObserver);
                mNetworkRegister = true;
            }

            /// @}
            /// M: Add For C2K OM, OP09 will implement its own cdma network setting @{
            int mainPhoneId = SubscriptionManager.INVALID_PHONE_INDEX;
            IMtkTelephonyEx iTelEx = IMtkTelephonyEx.Stub.asInterface(
                                                    ServiceManager.getService("phoneEx"));
            if (iTelEx != null) {
                try {
                    mainPhoneId = iTelEx.getMainCapabilityPhoneId();
                } catch (RemoteException e) {
                    log("getMainCapabilityPhoneId: remote exception");
                }
            } else {
                log("IMtkTelephonyEx service not ready!");
                mainPhoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
            }
            if (FeatureOption.isMtkLteSupport()
                    && (isC2kLteSupport())
                     && ((TelephonyUtilsEx.isCdmaCardInserted(mPhone)
                        || TelephonyUtils.isCTLteTddTestSupport())
                        /// M:[CT VOLTE]
                        || (TelephonyUtilsEx.isCtVolteEnabled()
                                && TelephonyUtilsEx.isCt4gSim(mPhone.getSubId()) &&
                                (!TelephonyUtilsEx.isBothslotCt4gSim(mSubscriptionManager) ||
                                (mainPhoneId == mPhone.getPhoneId()))))
                    && !ExtensionManager.getMobileNetworkSettingsExt().isCtPlugin()) {
                if (mCdmaNetworkSettings != null) {
                    log("CdmaNetworkSettings destroy " + this);
                    mCdmaNetworkSettings.onDestroy();
                    mCdmaNetworkSettings = null;
                }
                mCdmaNetworkSettings = new CdmaNetworkSettings(this, prefSet, mPhone);
                mCdmaNetworkSettings.onResume();
            }
            /// @}

            if (null != mPhone) {
                ExtensionManager.getMobileNetworkSettingsExt()
                    .initOtherMobileNetworkSettings(this, mPhone.getSubId());
            }
            /// Add for cmcc open market @{
            if (mActiveSubInfos.size() > 0) {
                mOmEx.initMobileNetworkSettings(this, convertTabToSlot(mCurrentTab));
            }
            updateScreenStatus();
            /// @}
            /// M: for mtk 3m
            handleC2k3MScreen(prefSet);
            /// M: for mtk 4m
            handleC2k4MScreen(prefSet);
            /// M: for mtk 5m
            handleC2k5MScreen(prefSet);
        }

        /**
         * For [MTK_3SIM].
         * Convert Tab id to Slot id.
         * @param currentTab tab id
         * @return slotId
         * /
        private int convertTabToSlot(int currentTab) {
            int slotId = mActiveSubInfos.size() > currentTab ?
                    mActiveSubInfos.get(currentTab).getSimSlotIndex() : 0;
            if (DBG) {
                log("convertTabToSlot: info size=" + mActiveSubInfos.size() +
                        " currentTab=" + currentTab + " slotId=" + slotId);
            }
            return slotId;
        }

        private void initIntentFilter() {
            /// M: for receivers sim lock gemini phone @{
            mIntentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            mIntentFilter.addAction(MtkTelephonyIntents.ACTION_MSIM_MODE_CHANGED);
            mIntentFilter.addAction(MtkTelephonyIntents.ACTION_MD_TYPE_CHANGE);
            mIntentFilter.addAction(MtkTelephonyIntents.ACTION_LOCATED_PLMN_CHANGED);
            mIntentFilter.addAction(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
            ///@}
            /// M: Add for Sim Switch @{
            mIntentFilter.addAction(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_DONE);
            /// @}
            ExtensionManager.getMobileNetworkSettingsExt()
                    .customizeDualVolteIntentFilter(mIntentFilter);
        }

        /**
         * Is the phone has 3/4G capability or not.
         * @return true if phone has 3/4G capability
         * /
        private boolean isCapabilityPhone(Phone phone) {
            boolean result = phone != null ? ((phone.getRadioAccessFamily()
                    & (RadioAccessFamily.RAF_UMTS | RadioAccessFamily.RAF_LTE)) > 0) : false;
            return result;
        }

        // M: Add for [MTK_Enhanced4GLTE] @{
        // Use our own button instand of Google default one mButton4glte
        private Enhanced4GLteSwitchPreference mEnhancedButton4glte;

        /**
         * Add our switchPreference & Remove google default one.
         * @param preferenceScreen
         * /
        private void addEnhanced4GLteSwitchPreference(PreferenceScreen preferenceScreen) {
            int phoneId = SubscriptionManager.getPhoneId(mPhone.getSubId());
            log("[addEnhanced4GLteSwitchPreference] volteEnabled :"
                    + isVolteEnabled());
            if (mButton4glte != null) {
                log("[addEnhanced4GLteSwitchPreference] Remove mButton4glte!");
                preferenceScreen.removePreference(mButton4glte);
            }
            boolean isCtPlugin = ExtensionManager.getMobileNetworkSettingsExt().isCtPlugin();
            log("[addEnhanced4GLteSwitchPreference] ss :" + isCtPlugin);
            if (isVolteEnabled() && !isCtPlugin) {
                int order = mButtonEnabledNetworks.getOrder() + 1;
                mEnhancedButton4glte = new Enhanced4GLteSwitchPreference(this, mPhone.getSubId());
                /// Still use Google's key, title, and summary.
                mEnhancedButton4glte.setKey(BUTTON_4G_LTE_KEY);
                /// M: [CT VOLTE]
                // show "VOLTE" for CT VOLTE SIM
                if (TelephonyUtilsEx.isCtVolteEnabled()
                        && TelephonyUtilsEx.isCtSim(mPhone.getSubId())) {
                    mEnhancedButton4glte.setTitle(R.string.hd_voice_switch_title);
                    mEnhancedButton4glte.setSummary(R.string.hd_voice_switch_summary);
                } else {
                    PersistableBundle carrierConfig =
                    PhoneGlobals.getInstance().getCarrierConfigForSubId(mPhone.getSubId());
                    boolean useVariant4glteTitle = carrierConfig.getBoolean(
                            CarrierConfigManager.KEY_ENHANCED_4G_LTE_TITLE_VARIANT_BOOL);
                    int enhanced4glteModeTitleId = useVariant4glteTitle ?
                            R.string.enhanced_4g_lte_mode_title_variant :
                            R.string.enhanced_4g_lte_mode_title;
                    mEnhancedButton4glte.setTitle(enhanced4glteModeTitleId);
                }
                /// M: [CT VOLTE]
                // show "VOLTE" for CT VOLTE SIM
                if (!TelephonyUtilsEx.isCtVolteEnabled()
                        || !TelephonyUtilsEx.isCtSim(mPhone.getSubId())) {
                /// @}
                    mEnhancedButton4glte.setSummary(R.string.enhanced_4g_lte_mode_summary);
                }
                mEnhancedButton4glte.setOnPreferenceChangeListener(this);
                mEnhancedButton4glte.setOrder(order);
                /// M: Customize the LTE switch preference. @{
                ExtensionManager.getMobileNetworkSettingsExt()
                        .customizeEnhanced4GLteSwitchPreference(this, mEnhancedButton4glte);
                /// @}
            } else {
                mEnhancedButton4glte = null;
            }
        }

        /**
         * Add for update the display of network mode preference.
         * @param enable is the preference or not
         * /
        private void updateCapabilityRelatedPreference(boolean enable) {
            // if airplane mode is on or all SIMs closed, should also dismiss dialog
            boolean isNWModeEnabled = enable && isCapabilityPhone(mPhone);
            if (DBG) {
                log("updateNetworkModePreference:isNWModeEnabled = " + isNWModeEnabled);
            }

            updateNetworkModePreference(mButtonPreferredNetworkMode, isNWModeEnabled);
            updateNetworkModePreference(mButtonEnabledNetworks, isNWModeEnabled);
            /// M: Add for L+W DSDS.
            if (ExtensionManager.getMobileNetworkSettingsExt().isNetworkModeSettingNeeded()) {
                updateNetworkModeForLwDsds();
            }
            /// Add for [MTK_Enhanced4GLTE]
            updateEnhanced4GLteSwitchPreference();

            /// Update CDMA network settings
            if (TelephonyUtilsEx.isCDMAPhone(mPhone) && mCdmaNetworkSettings != null) {
                mCdmaNetworkSettings.onResume();
            } else {
                log("updateCapabilityRelatedPreference don't update cdma settings");
            }
        }

        /**
         * Update the subId in mEnhancedButton4glte.
         * /
        private void updateEnhanced4GLteSwitchPreference() {
            int phoneId = SubscriptionManager.getPhoneId(mPhone.getSubId());
            if (mEnhancedButton4glte != null) {
                boolean showVolte =
                        (SystemProperties.getInt("persist.mtk_mims_support", 1) == 1 &&
                        ImsManager.isVolteEnabledByPlatform(this) &&
                        TelephonyUtilsEx.getMainPhoneId() == mPhone.getPhoneId()) ||
                        (SystemProperties.getInt("persist.mtk_mims_support", 1) > 1 &&
                        ImsManager.isVolteEnabledByPlatform(this, mPhone.getPhoneId()) &&
                        isCapabilityPhone(mPhone));
                if (ExtensionManager.getMobileNetworkSettingsExt().isEnhancedLTENeedToAdd(
                        showVolte, mPhone.getPhoneId())) {
                    if (findPreference(BUTTON_4G_LTE_KEY) == null) {
                        log("updateEnhanced4GLteSwitchPreference add switcher");
                        getPreferenceScreen().addPreference(mEnhancedButton4glte);
                    }
                } else {
                    if (findPreference(BUTTON_4G_LTE_KEY) != null) {
                        getPreferenceScreen().removePreference(mEnhancedButton4glte);
                    }
                }
                if (findPreference(BUTTON_4G_LTE_KEY) != null) {
                    mEnhancedButton4glte.setSubId(mPhone.getSubId());
                    boolean enh4glteMode = MtkImsManager.isEnhanced4gLteModeSettingEnabledByUser(
                            this, phoneId) &&
                            MtkImsManager.isNonTtyOrTtyOnVolteEnabled(this, phoneId);
                    mEnhancedButton4glte.setChecked(enh4glteMode);
                    log("[updateEnhanced4GLteSwitchPreference] SubId = " + mPhone.getSubId()
                        + ", enh4glteMode=" + enh4glteMode);
                }
                /// M: update enabled state
                updateEnhanced4glteEnableState();
            }
        }

        private void updateEnhanced4glteEnableState() {
            if (mEnhancedButton4glte != null) {
                boolean inCall = TelecomManager.from(this).isInCall();
                boolean nontty = MtkImsManager.isNonTtyOrTtyOnVolteEnabled(getApplicationContext(),
                        mPhone.getPhoneId());
                /// M: [CT VOLTE] @{
                boolean enableForCtVolte = true;
                int subId = mPhone.getSubId();
                if (TelephonyUtilsEx.isCtVolteEnabled() && TelephonyUtilsEx.isCtSim(subId)) {
                    int settingsNetworkMode = android.provider.Settings.Global.getInt(mPhone
                            .getContext().getContentResolver(),
                            android.provider.Settings.Global.PREFERRED_NETWORK_MODE + subId,
                            Phone.PREFERRED_NT_MODE);
                    enableForCtVolte = TelephonyUtilsEx.isCt4gSim(subId)
                            && (settingsNetworkMode == Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA
                            || settingsNetworkMode == Phone.NT_MODE_LTE_GSM_WCDMA
                            || settingsNetworkMode == Phone.NT_MODE_LTE_CDMA_AND_EVDO
                            || settingsNetworkMode == Phone.NT_MODE_LTE_ONLY
                            || settingsNetworkMode == Phone.NT_MODE_LTE_WCDMA);
                    if (TelephonyUtilsEx.isCtAutoVolteEnabled()) {
                        enableForCtVolte = true;
                    }
                }
                /// @}
                /// M: [CMCC DUAl VOLTE] @{
                enableForCtVolte = ExtensionManager.getMobileNetworkSettingsExt()
                        .customizeDualVolteOpDisable(subId, enableForCtVolte);
                /// @}
                boolean secondEnabled = isSecondVolteEnabled();
                log("updateEnhanced4glteEnableState, incall = " + inCall + ", nontty = " + nontty
                        + ", enableForCtVolte = " + enableForCtVolte + ", secondEnabled = "
                        + secondEnabled);
                mEnhancedButton4glte.setEnabled(!inCall && nontty && hasActiveSubscriptions()
                        && enableForCtVolte && secondEnabled);
                /// M: [CMCC DUAl VOLTE] @{
                ExtensionManager.getMobileNetworkSettingsExt()
                        .customizeDualVolteOpHide(getPreferenceScreen(),
                                mEnhancedButton4glte, enableForCtVolte);
                /// @}
            }
    }
        /**
         * For [MTK_Enhanced4GLTE]
         * We add our own SwitchPreference, and its own onPreferenceChange call backs.
         * @param preference
         * @param objValue
         * @return
         * /
        private boolean onPreferenceChangeMTK(Preference preference, Object objValue) {
            String volteTitle = getResources().getString(R.string.hd_voice_switch_title);
            String lteTitle = getResources().getString(R.string.enhanced_4g_lte_mode_title);
            log("[onPreferenceChangeMTK] Preference = " + preference.getTitle());

            if ((mEnhancedButton4glte == preference) || preference.getTitle().equals(volteTitle)
                    || preference.getTitle().equals(lteTitle)) {
                Enhanced4GLteSwitchPreference ltePref = (Enhanced4GLteSwitchPreference) preference;
                log("[onPreferenceChangeMTK] IsChecked = " + ltePref.isChecked());
                /// M: [CT VOLTE] @{
                if (TelephonyUtilsEx.isCtVolteEnabled() && TelephonyUtilsEx.isCtSim(
                        mPhone.getSubId())
                        && !ltePref.isChecked()) {
                    int type = TelephonyManager.getDefault().getNetworkType(mPhone.getSubId());
                    log("network type = " + type);
                    if (TelephonyManager.NETWORK_TYPE_LTE != type
                            && !TelephonyUtilsEx.isRoaming(mPhone)
                            && (TelephonyUtilsEx.getMainPhoneId() == mPhone.getPhoneId()
                            || TelephonyUtilsEx.isBothslotCt4gSim(mSubscriptionManager))) {
                        if (!TelephonyUtilsEx.isCtAutoVolteEnabled()) {
                            showVolteUnavailableDialog();
                            return false;
                        }
                    }
                }
                ltePref.setChecked(!ltePref.isChecked());
                MtkImsManager.setEnhanced4gLteModeSetting(this, ltePref.isChecked(),
                        mPhone.getPhoneId());
                return true;
            }
            return false;
        }
        /**
         * [CT VOLTE]When network type is not LTE, show dialog.
         * /
        private void showVolteUnavailableDialog() {
            log("showVolteUnavailableDialog ...");
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            String title = this.getString(R.string.alert_ct_volte_unavailable, PhoneUtils
                    .getSubDisplayName(mPhone.getSubId()));
            Dialog dialog = builder.setMessage(title).setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            log("dialog cancel mEnhanced4GLteSwitchPreference.setchecked  = "
                                    + !mEnhancedButton4glte.isChecked());
                            mEnhancedButton4glte.setChecked(false);

                        }
                    }).setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mEnhancedButton4glte.setChecked(true);
                    log("dialog ok" + " ims set " + mEnhancedButton4glte.isChecked() + " mSlotId = "
                            + SubscriptionManager.getPhoneId(mPhone.getSubId()));
                    MtkImsManager.setEnhanced4gLteModeSetting(MobileNetworkSettings.this,
                            mEnhancedButton4glte.isChecked(), mPhone.getPhoneId());
                }
            }).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.setOnKeyListener(new OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (KeyEvent.KEYCODE_BACK == keyCode) {
                        if (null != dialog) {
                            log("onKey keycode = back"
                                    + "dialog cancel mEnhanced4GLteSwitchPreference.setchecked  = "
                                    + !mEnhancedButton4glte.isChecked());
                            mEnhancedButton4glte.setChecked(false);
                            dialog.dismiss();
                            return true;
                        }
                    }
                    return false;
                }
            });
            mDialog = dialog;
            dialog.show();
        }

        private void addPLMNList(PreferenceScreen prefSet) {
            // add PLMNList, if c2k project the order should under the 4g data only
            int order = prefSet.findPreference(SINGLE_LTE_DATA) != null ?
                    prefSet.findPreference(SINGLE_LTE_DATA).getOrder() : mButtonDataRoam.getOrder();
            mPLMNPreference = new Preference(this);
            mPLMNPreference.setKey(BUTTON_PLMN_LIST);
            mPLMNPreference.setTitle(R.string.plmn_list_setting_title);
            Intent intentPlmn = new Intent();
            intentPlmn.setClassName("com.android.phone",
                    "com.mediatek.settings.PLMNListPreference");
            intentPlmn.putExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, mPhone.getSubId());
            //* / freeme.liqiang, 20180328. add back title
            intentPlmn.putExtra(FreemeActionBarUtil.EXTRA_NAVIGATE_UP_TITLE_TEXT,
                    getResources().getString(R.string.freeme_mobile_networks));
            //* /
            mPLMNPreference.setIntent(intentPlmn);
            mPLMNPreference.setOrder(order + 1);
            prefSet.addPreference(mPLMNPreference);
        }

        private void updateScreenStatus() {
            boolean isIdle = (TelephonyManager.getDefault().getCallState()
                    == TelephonyManager.CALL_STATE_IDLE);
            boolean isShouldEnabled = isIdle && TelephonyUtils.isRadioOn(mPhone.getSubId(), this);
            if (DBG) {
                log("updateScreenStatus:isShouldEnabled = "
                    + isShouldEnabled + ", isIdle = " + isIdle);
            }
            getPreferenceScreen().setEnabled(isShouldEnabled);
            updateCapabilityRelatedPreference(isShouldEnabled);
        }

        /**
         * Whether support c2k LTE or not.
         * @return true if support else false.
         * /
        private boolean isC2kLteSupport() {
            return FeatureOption.isMtkSrlteSupport()
                    || FeatureOption.isMtkSvlteSupport();
        }

        /**
         * Update the preferred network mode item Entries & Values.
         * /
        private void updateNetworkModeForLwDsds() {
            /// Get main phone Id;
            /*ITelephonyEx iTelEx = ITelephonyEx.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));* /
            int mainPhoneId = getMainCapabilityPhoneId();
            /*if (iTelEx != null) {
                try{
                    mainPhoneId = getMainCapabilityPhoneId();
                } catch (RemoteException e) {
                    loge("handleLwDsdsNetworkMode get iTelEx error" + e.getMessage());
                }
            }* /
            /// If the phone main phone we should do nothing special;
            if (DBG) {
                log("handleLwDsdsNetworkMode mainPhoneId = " + mainPhoneId);
            }
            if (mainPhoneId != mPhone.getPhoneId()) {
                /// We should compare the user's setting value & modem support info;
                int settingsNetworkMode = android.provider.Settings.Global.getInt(
                        mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE
                        + mPhone.getSubId(), Phone.NT_MODE_GSM_ONLY);
                int currRat = mPhone.getRadioAccessFamily();
                log("updateNetworkModeForLwDsds settingsNetworkMode = "
                        + settingsNetworkMode + "; currRat = " + currRat);
                if ((currRat & RadioAccessFamily.RAF_LTE) == RadioAccessFamily.RAF_LTE) {
                    int select = mShow4GForLTE ? R.array.enabled_networks_4g_choices
                            : R.array.enabled_networks_choices;
                    mButtonEnabledNetworks.setEntries(select);
                    mButtonEnabledNetworks.setEntryValues(isC2kLteSupport() ?
                            R.array.enabled_networks_values_c2k : R.array.enabled_networks_values);
                    log("updateNetworkModeForLwDsds mShow4GForLTE = " + mShow4GForLTE);
                } else if ((currRat & RadioAccessFamily.RAF_UMTS) == RadioAccessFamily.RAF_UMTS) {
                    // Support 3/2G for WorldMode is uLWG
                    mButtonEnabledNetworks.setEntries(
                            R.array.enabled_networks_except_lte_choices);
                    if (isC2kLteSupport()) {
                        mButtonEnabledNetworks.setEntryValues(
                                R.array.enabled_networks_except_lte_values_c2k);
                    } else {
                        mButtonEnabledNetworks.setEntryValues(
                                R.array.enabled_networks_except_lte_values);
                    }
                    // If user select contain LTE, should set UI to 3G;
                    // NT_MODE_LTE_CDMA_AND_EVDO = 8 is the smallest value supporting LTE.
                    if (settingsNetworkMode > Phone.NT_MODE_LTE_CDMA_AND_EVDO) {
                        log("updateNetworkModeForLwDsds set network mode to 3G");
                        if (isC2kLteSupport()) {
                            mButtonEnabledNetworks.setValue(
                                    Integer.toString(Phone.NT_MODE_GLOBAL));
                        } else {
                            mButtonEnabledNetworks.setValue(
                                    Integer.toString(Phone.NT_MODE_WCDMA_PREF));
                        }
                        mButtonEnabledNetworks.setSummary(R.string.network_3G);
                    } else {
                        log("updateNetworkModeForLwDsds set to what user select. ");
                        UpdateEnabledNetworksValueAndSummary(settingsNetworkMode);
                    }
                } else {
                    // Only support 2G for WorldMode is uLtTG
                    log("updateNetworkModeForLwDsds set to 2G only.");
                    mButtonEnabledNetworks.setSummary(R.string.network_2G);
                    mButtonEnabledNetworks.setEnabled(false);
                }
            }
        }

        /**
         * Add for update the display of network mode preference.
         * @param enable is the preference or not
         * /
        private void updateNetworkModePreference(ListPreference preference, boolean enable) {
            // if airplane mode is on or all SIMs closed, should also dismiss dialog
            if (preference != null) {
                preference.setEnabled(enable);
                if (!enable) {
                    dissmissDialog(preference);
                }
                if (getPreferenceScreen().findPreference(preference.getKey()) != null) {
                    updatePreferredNetworkUIFromDb();
                }
                /// Add for cmcc open market @{
                mOmEx.updateLTEModeStatus(preference);
                /// @}
            }
        }


        /**
         * For C2k Common screen, (3M, 5M).
         * @param preset
         * /
        private void handleC2kCommonScreen(PreferenceScreen prefSet) {
            log("--- go to C2k Common (3M, 5M) screen ---");

            if (prefSet.findPreference(BUTTON_PREFERED_NETWORK_MODE) != null) {
                prefSet.removePreference(prefSet.findPreference(BUTTON_PREFERED_NETWORK_MODE));
            }
            if (TelephonyUtilsEx.isCDMAPhone(mPhone)) {
                if (prefSet.findPreference(BUTTON_ENABLED_NETWORKS_KEY) != null) {
                    prefSet.removePreference(prefSet.findPreference(BUTTON_ENABLED_NETWORKS_KEY));
                }
            }
        }

        /**
         * For C2k 3M.
         * @param preset
         * /
        private void handleC2k3MScreen(PreferenceScreen prefSet) {
            if (!FeatureOption.isMtkLteSupport() && FeatureOption.isMtkC2k3MSupport()) {

                handleC2kCommonScreen(prefSet);
                log("--- go to C2k 3M ---");

                if (!TelephonyUtilsEx.isCDMAPhone(mPhone)) {
                    mButtonEnabledNetworks.setEntries(R.array.enabled_networks_except_lte_choices);
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_except_lte_values_c2k);
                }
            }
        }

        /**
         * For C2k OM 4M.
         * @param preset
         * /
        private void handleC2k4MScreen(PreferenceScreen prefSet) {
            if (FeatureOption.isMtkLteSupport() && FeatureOption.isMtkC2k4MSupport()) {
                log("--- go to C2k 4M ---");

                if (PhoneConstants.PHONE_TYPE_GSM == mPhone.getPhoneType()) {
                    mButtonEnabledNetworks.setEntries(
                            R.array.enabled_networks_except_td_cdma_3g_choices);
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_except_td_cdma_3g_values);
                }
            }
        }

        /**
         * For C2k 5M.
         * Under 5M(CLLWG).
         * @param prefSet
         * /
        private void handleC2k5MScreen(PreferenceScreen prefSet) {
            if (FeatureOption.isMtkLteSupport() && FeatureOption.isMtkC2k5MSupport()) {

                handleC2kCommonScreen(prefSet);
                log("--- go to c2k 5M ---");

                if (!TelephonyUtilsEx.isCDMAPhone(mPhone)) {
                    mButtonEnabledNetworks.setEntries(R.array.enabled_networks_4g_choices);
                    mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_values_c2k);
                }
            }
        }

        /**
         * Get main capability phone ID.
         *
         * @return Phone ID with main capability
         * /
        public static int getMainCapabilityPhoneId() {
            int phoneId = 0;
            phoneId = SystemProperties.getInt(MtkPhoneConstants.PROPERTY_CAPABILITY_SWITCH, 1) - 1;
            //Log.d(LOG_TAG, "[RadioCapSwitchUtil] getMainCapabilityPhoneId " + phoneId);
            return phoneId;
        }

        /// M: if set fail, restore the preferred network type
        private void restorePreferredNetworkTypeIfNeeded(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception != null && mPreNetworkMode != -1 && mPhone != null) {
                final int phoneSubId = mPhone.getSubId();
                log("set failed, reset preferred network mode to " + mPreNetworkMode + ", sub id = "
                        + phoneSubId);
                android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                        mPreNetworkMode);
            }
            mPreNetworkMode = -1;
        }

        /// M: [CT VOLTE]
        private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                log("onChange...");
                updateEnhanced4GLteSwitchPreference();
            }
        };

        /// M: [CT VOLTE Network UI]
        private ContentObserver mNetworkObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                log("mNetworkObserver onChange...");
                updateBody();
            }
        };

        private boolean isVolteEnabled() {
            boolean isVolteEnabled = ImsManager.isVolteEnabledByPlatform(this);
            if (SystemProperties.getInt("persist.mtk_mims_support", 1) > 1) {
                isVolteEnabled = ImsManager.isVolteEnabledByPlatform(this, mPhone.getPhoneId());
            }
            return isVolteEnabled;
        }

        private boolean isWfcEnabled() {
            boolean isWfcEnabled = ImsManager.isWfcEnabledByPlatform(this);
            if (SystemProperties.getInt("persist.mtk_mims_support", 1) > 1) {
                isWfcEnabled = ImsManager.isWfcEnabledByPlatform(this, mPhone.getPhoneId());
            }
            return isWfcEnabled;
        }

        private boolean isSecondVolteEnabled() {
            if (!TelephonyUtilsEx.isBothslotCtSim(mSubscriptionManager)) {
                return true;
            }
            if (TelephonyUtilsEx.getMainPhoneId() == mPhone.getPhoneId()) {
                return true;
            } else {
                return false;
            }
        }

    //* / freeme.wanglei, 20170713. add cellular data switch.
    private static final String BUTTON_DATA_NETWORK_KEY = "data_network";
    private SwitchPreference mButtonDataNetwork;
    private int mDefaultSubscriptionId;
    private boolean mHasDefaultSubId;

    private int getDefaultSubscriptionId() {
        if (mSubscriptionManager == null) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }
        SubscriptionInfo subscriptionInfo = mSubscriptionManager.getDefaultDataSubscriptionInfo();
        if (subscriptionInfo == null) {
            List<SubscriptionInfo> list = mSubscriptionManager.getAllSubscriptionInfoList();
            if (list.size() == 0) {
                return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            }
            subscriptionInfo = list.get(0);
        }
        return subscriptionInfo.getSubscriptionId();
    }

    private void setMobileDataEnabled(boolean enabled) {
        if (mHasDefaultSubId) {
            mTelephonyManager.setDataEnabled(mDefaultSubscriptionId, enabled);
            mButtonDataNetwork.setChecked(enabled);
        }
    }

    private void disableDataForOtherSubscriptions(int subId) {
        List<SubscriptionInfo> subInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                if (subInfo.getSubscriptionId() != subId) {
                    mTelephonyManager.setDataEnabled(subInfo.getSubscriptionId(), false);
                }
            }
        }
    }

    private void updateChecked() {
        if (mHasDefaultSubId) {
            mButtonDataNetwork.setChecked(mTelephonyManager.getDataEnabled(mDefaultSubscriptionId));
        }
    }

    private final DataStateListener mListener = new DataStateListener() {
        @Override
        public void onChange(boolean selfChange) {
            updateChecked();
        }
    };

    private abstract static class DataStateListener extends ContentObserver {
        public DataStateListener() {
            super(new Handler(Looper.getMainLooper()));
        }

        public void setListener(boolean listening, int subId, Context context) {
            if (listening) {
                Uri uri = Settings.Global.getUriFor(Settings.Global.MOBILE_DATA);
                if (TelephonyManager.getDefault().getSimCount() != 1) {
                    uri = Settings.Global.getUriFor(Settings.Global.MOBILE_DATA + subId);
                }
                context.getContentResolver().registerContentObserver(uri, false, this);
            } else {
                context.getContentResolver().unregisterContentObserver(this);
            }
        }
    }
    //* /
}
/*/
import com.freeme.preference.FreemeJumpPreference;
import android.app.Activity;
import android.content.ComponentName;
import android.content.res.Resources;
import android.preference.PreferenceCategory;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.SubscriptionController;
import com.mediatek.settings.cdma.CdmaVolteServiceChecker;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;

public class MobileNetworkSettings extends PreferenceActivity implements
        DialogInterface.OnClickListener, DialogInterface.OnDismissListener,
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "MobbileNetWorkSettings";

    private SimPreferenceCategory mSimPreferenceCategory;
    private UserManager mUm;
    private Phone mPhone;
    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;

    public static final String BUTTON_ENABLED_NETWORKS_KEY = "enabled_networks_key";
    public static final String BUTTON_PREFERED_NETWORK_MODE = "preferred_network_mode_key";
    public static final String BUTTON_PLMN_LIST = "button_plmn_key";

    private boolean mUnavailable;

    private RestrictedSwitchPreference mButtonDataRoam;
    private static final String BUTTON_ROAMING_KEY = "button_roaming_key";

    private static final boolean DBG = "eng".equals(Build.TYPE);

    private List<SubscriptionInfo> mActiveSubInfos;

    private final SubscriptionManager.OnSubscriptionsChangedListener
            mOnSubscriptionsChangeListener
            = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            if (DBG) {
                Log.i(TAG, "onSubscriptionsChanged:");
            }
            /// M: add for hot swap @{
            if (TelephonyUtils.isHotSwapHanppened(
                    mActiveSubInfos, PhoneUtils.getActiveSubInfoList())) {
                if (DBG) {
                    Log.i(TAG, "onSubscriptionsChanged:hot swap hanppened");
                }
                finish();
                return;
            }
            /// @}
            initializeSubscriptions();
        }
    };

    private void updateScreenStatus() {
        boolean isIdle = (TelephonyManager.getDefault().getCallState()
                == TelephonyManager.CALL_STATE_IDLE);
        boolean isShouldEnabled = isIdle && TelephonyUtils.isRadioOn(mPhone.getSubId(), this);
        if (DBG) {
            Log.i(TAG, "updateScreenStatus:isShouldEnabled = "
                    + isShouldEnabled + ", isIdle = " + isIdle);
        }
        getPreferenceScreen().setEnabled(isShouldEnabled);
        if (mSimPreferenceCategory != null) {
            //mSimPreferenceCategory.updateEnhanced4GLteSwitchPreference(mPhoneSubId);
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DBG) {
                Log.i(TAG, "action: " + action);
            }
            /// When receive aiplane mode, we would like to finish the activity, for
            //  we can't get the modem capability, and will show the user selected network
            //  mode as summary, this will make user misunderstand.(ALPS01971666)
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                finish();
            } else if (action.equals(MtkTelephonyIntents.ACTION_MSIM_MODE_CHANGED)
                    || action.equals(MtkTelephonyIntents.ACTION_MD_TYPE_CHANGE)
                    || action.equals(MtkTelephonyIntents.ACTION_LOCATED_PLMN_CHANGED)
                    || ExtensionManager.getMobileNetworkSettingsExt()
                    .customizeDualVolteReceiveIntent(action)) {
                updateScreenStatus();
            } else if (action.equals(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_DONE)) {
                if (DBG) {
                    Log.i(TAG, "Siwtch done Action ACTION_SET_PHONE_RAT_FAMILY_DONE received ");
                }
                mPhone = PhoneUtils.getPhoneUsingSubId(mPhone.getSubId());
                updateScreenStatus();
            } else if (action.equals(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED)) {
                // When the radio changes (ex: CDMA->GSM), refresh all options.
                updateBody();
            }
            /// @}
        }
    };

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        /*
         * Enable/disable the 'Enhanced 4G LTE Mode' when in/out of a call
         * and depending on TTY mode and TTY support over VoLTE.
         * @see android.telephony.PhoneStateListener#onCallStateChanged(int,
         * java.lang.String)
         */
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (DBG) Log.i(TAG, "PhoneStateListener.onCallStateChanged: state=" + state);
            updateScreenStatus();

            /// M: should also update enable state in other places, so exact to method
            if (mSimPreferenceCategory != null) {
                //mSimPreferenceCategory.updateEnhanced4glteEnableState();
            }
        }

        /**
         *  For CU volte feature.
         */
        @Override
        public void onServiceStateChanged(ServiceState state) {
            if (ExtensionManager.getMobileNetworkSettingsExt().customizeCUVolte() && mSimPreferenceCategory != null) {
                //mSimPreferenceCategory.updateEnhanced4glteEnableState();
            }
        }
    };

    private boolean mShow4GForLTE;
    private static final String ENHANCED_4G_MODE_ENABLED_SIM2 = "volte_vt_enabled_sim2";
    private PreferenceCategory mHeaderPreferenceCategory;
    private static final String HEAD_PREFERENCECATEGORY = "head_preferencecateggory";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUm = (UserManager) getSystemService(Context.USER_SERVICE);
        mSubscriptionManager = SubscriptionManager.from(this);
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        boolean isAdmin = mUm.isAdminUser();
        Log.d(TAG, "isAdmin = " + isAdmin);

        if (!isAdmin || mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)) {
            mUnavailable = true;
            setContentView(R.layout.telephony_disallowed_preference_screen);
            return;
        }

        com.freeme.actionbar.app.FreemeActionBarUtil.setNavigateTitle(this, getIntent());

        addPreferencesFromResource(R.xml.freeme_network_setting_fragment);

        mHeaderPreferenceCategory = (PreferenceCategory) findPreference(HEAD_PREFERENCECATEGORY);

        mButtonDataRoam = (RestrictedSwitchPreference) mHeaderPreferenceCategory.findPreference(BUTTON_ROAMING_KEY);
        mButtonDataRoam.setOnPreferenceChangeListener(this);

        mButtonDataNetwork = (SwitchPreference) mHeaderPreferenceCategory.findPreference(BUTTON_DATA_NETWORK_KEY);
        mButtonDataNetwork.setOnPreferenceChangeListener(this);

        // Initialize mActiveSubInfo
        int max = mSubscriptionManager.getActiveSubscriptionInfoCountMax();
        mActiveSubInfos = new ArrayList<SubscriptionInfo>(max);

        try {
            Context con = createPackageContext("com.android.systemui", 0);
            int id = con.getResources().getIdentifier("config_show4GForLTE",
                    "bool", "com.android.systemui");
            mShow4GForLTE = con.getResources().getBoolean(id);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "NameNotFoundException for show4GFotLTE");
            mShow4GForLTE = false;
        }
        Log.d(TAG, "mShow4GForLTE: " + mShow4GForLTE);

        initIntentFilter();

        try {
            registerReceiver(mReceiver, mIntentFilter);
        } catch (Exception e) {
            Log.e(TAG, "Receiver Already registred");
        }
        mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        mTelephonyManager.listen(
                mPhoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE
                        | PhoneStateListener.LISTEN_SERVICE_STATE
        );

        mSubscriptionController = SubscriptionController.getInstance();

        initializeSubscriptions();

        /// M: [CT VOLTE]
        getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ENHANCED_4G_MODE_ENABLED),
                true, mContentObserver);
        getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(ENHANCED_4G_MODE_ENABLED_SIM2),
                true, mContentObserver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume:+");

        if (mUnavailable) {
            Log.i(TAG, "onResume:- ignore mUnavailable == false");
            return;
        }

        if (mSimPreferenceCategory != null) {
            mSimPreferenceCategory.onResume();
        }
        // Set UI state in onResume because a user could go home, launch some
        // app to change this setting's backend, and re-launch this settings app
        // and the UI state would be inconsistent with actual state
        mButtonDataRoam.setChecked(mPhone.getDataRoamingEnabled());

        mDefaultSubscriptionId = getDefaultSubscriptionId();
        mHasDefaultSubId = mDefaultSubscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mListener.setListener(true, mDefaultSubscriptionId, getApplicationContext());
        updateChecked();

        mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);

        /// M: For screen update
        updateScreenStatus();

        /// M: For plugin to update UI
        ExtensionManager.getMobileNetworkSettingsExt().onResume();

        Log.i(TAG, "onResume:-");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        boolean isAdmin = mUm.isAdminUser();
        Log.d(TAG, "onDestroy, isAdmin = " + isAdmin);
        if (!isAdmin) {
            return;
        }

        try {
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Receiver Already unregistred");
        }
        ExtensionManager.getMobileNetworkSettingsExt().unRegister();
        log("onDestroy ");

        if (mSubscriptionManager != null) {
            mSubscriptionManager
                    .removeOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        }
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        /// M: [CT VOLTE] @{
        getContentResolver().unregisterContentObserver(mContentObserver);
        /// M: [CT VOLTE] @{
        if (TelephonyUtilsEx.isCtVolteEnabled()
                && TelephonyUtilsEx.isCt4gSim(mPhone.getSubId())) {
            getContentResolver().unregisterContentObserver(mNetworkObserver);
        }

        if (mSimPreferenceCategory != null) {
            mSimPreferenceCategory.disMissDialog();
            mSimPreferenceCategory.onDestory();
        }
    }

    /// M: [CT VOLTE Network UI]
    private ContentObserver mNetworkObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            log("mNetworkObserver onChange...");
            updateBody();
        }
    };

    private void initializeSubscriptions() {
        if (isDestroyed()) {
            // Process preferences in activity only if its not destroyed
            return;
        }
        if (DBG) log("initializeSubscriptions:+");

        // Before updating the the active subscription list check
        // if tab updating is needed as the list is changing.
        List<SubscriptionInfo> sil = mSubscriptionManager.getActiveSubscriptionInfoList();

        // Update to the active subscription list啊`
        mActiveSubInfos.clear();
        if (sil != null) {
            mActiveSubInfos.addAll(sil);
        }

        updatePhone();
        updateBody();
        if (DBG) log("initializeSubscriptions:-");
    }

    SubscriptionController mSubscriptionController;

    private void updatePhone() {
        mPhone = PhoneFactory.getPhone(
                SubscriptionManager.getPhoneId(mDefaultSubscriptionId));
        if (mPhone == null) {
            // Do the best we can
            mPhone = PhoneGlobals.getPhone();
        }
        Log.i(TAG, "updatePhone:");
    }

    /// M: [CT VOLTE]
    private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            log("onChange...");
            if (mSimPreferenceCategory != null) {
                //mSimPreferenceCategory.updateEnhanced4GLteSwitchPreference(mPhoneSubId);
            }
        }
    };

    private Phone mSimPreferencePhone;

    private int mPhoneSubId;

    private void updateSimPhone(int slotId) {
        final SubscriptionInfo sir = mSubscriptionManager
                .getActiveSubscriptionInfoForSimSlotIndex(slotId);
        if (sir != null) {
            mSimPreferencePhone = PhoneFactory.getPhone(
                    SubscriptionManager.getPhoneId(sir.getSubscriptionId()));
            mPhoneSubId = mSimPreferencePhone.getSubId();
        } else {
            mPhoneSubId = -1;
        }
        if (mSimPreferencePhone == null) {
            // Do the best we can
            mSimPreferencePhone = PhoneGlobals.getPhone();
        }

        Log.i(TAG, "updateSimPhone:- slotId=" + slotId + " sir=" + sir);
    }

    private static final String BUTTON_SPRINT_ROAMING_SETTINGS = "sprint_roaming_settings";

    private void updateBody() {
        PreferenceScreen prefSet = getPreferenceScreen();

        PersistableBundle carrierConfig =
                PhoneGlobals.getInstance().getCarrierConfigForSubId(mPhone.getSubId());

        if (prefSet != null) {
            prefSet.removeAll();
            prefSet.addPreference(mHeaderPreferenceCategory);
            mHeaderPreferenceCategory.addPreference(mButtonDataRoam);
            mHeaderPreferenceCategory.addPreference(mButtonDataNetwork);
        }

        for (int i = 0, slotCount = mTelephonyManager.getPhoneCount(); i < slotCount; i++) {
            PreferenceCategory prefCategory = new PreferenceCategory(this);

            prefCategory.setTitle("SIM" + (i + 1));
            prefSet.addPreference(prefCategory);
            updateSimPhone(i);
            boolean isLteOnCdma = mSimPreferencePhone.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE;

            PersistableBundle simCarrierConfig =
                    PhoneGlobals.getInstance().getCarrierConfigForSubId(mPhoneSubId);
            if (simCarrierConfig.getBoolean(CarrierConfigManager
                    .KEY_HIDE_PREFERRED_NETWORK_TYPE_BOOL)
                    && !mSimPreferencePhone.getServiceState().getRoaming()) {
                final int phoneType = mSimPreferencePhone.getPhoneType();
                if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                    mSimPreferenceCategory = new SimPreferenceCategory(mPhoneSubId, prefCategory, this);
                    // In World mode force a refresh of GSM Options.
                    if (isWorldMode()) {
                        mSimPreferenceCategory = null;
                    }
                } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                    mSimPreferenceCategory = new SimPreferenceCategory(mPhoneSubId, prefCategory, this);
                } else {
                    throw new IllegalStateException("Unexpected phone type: " + phoneType);
                }
            } else if (simCarrierConfig.getBoolean(
                    CarrierConfigManager.KEY_WORLD_PHONE_BOOL) == true) {
                mSimPreferenceCategory = new SimPreferenceCategory(mPhoneSubId, prefCategory, this);
                mSimPreferenceCategory = new SimPreferenceCategory(mPhoneSubId, prefCategory, this);
            } else {
                final int phoneType = mSimPreferencePhone.getPhoneType();
                if (TelephonyUtilsEx.isCDMAPhone(mSimPreferencePhone)
                        /// M: [CT VOLTE]
                        || (TelephonyUtilsEx.isCtVolteEnabled() &&
                        TelephonyUtilsEx.isCt4gSim(mPhoneSubId) &&
                        !TelephonyUtilsEx.isRoaming(mSimPreferencePhone) &&
                        (!TelephonyUtilsEx.isBothslotCtSim(mSubscriptionManager)))) {
                    if (isLteOnCdma) {
                        mSimPreferenceCategory = new SimPreferenceCategory(mPhoneSubId, prefCategory, this);
                        // In World mode force a refresh of GSM Options.
                        if (isWorldMode()) {
                            mSimPreferenceCategory = null;
                        }
                    }
                } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                    mSimPreferenceCategory = new SimPreferenceCategory(mPhoneSubId, prefCategory, this);
                } else {
                    throw new IllegalStateException("Unexpected phone type: " + phoneType);
                }
            }
        }
        if (carrierConfig.getBoolean(MtkCarrierConfigManager.MTK_KEY_ROAMING_BAR_GUARD_BOOL)) {
            int order = mButtonDataRoam.getOrder();
            prefSet.removePreference(mButtonDataRoam);
            Preference sprintPreference = new Preference(this);
            sprintPreference.setKey(BUTTON_SPRINT_ROAMING_SETTINGS);
            sprintPreference.setTitle(R.string.roaming_settings);
            Intent intentRoaming = new Intent();
            intentRoaming.setClassName("com.android.phone",
                    "com.mediatek.services.telephony.RoamingSettings");
            intentRoaming.putExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, mPhone.getSubId());
            sprintPreference.setIntent(intentRoaming);
            sprintPreference.setOrder(order);
            prefSet.addPreference(sprintPreference);
        }
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Get the networkMode from Settings.System and displays it
        mButtonDataRoam.setChecked(mPhone.getDataRoamingEnabled());
        updateChecked();

        mButtonDataRoam.setDisabledByAdmin(false);
        if (mButtonDataRoam.isEnabled()) {
            if (RestrictedLockUtils.hasBaseUserRestriction(getApplicationContext(),
                    UserManager.DISALLOW_DATA_ROAMING, UserHandle.myUserId())) {
                mButtonDataRoam.setEnabled(false);
            } else {
                mButtonDataRoam.checkRestrictionAndSetDisabled(
                        UserManager.DISALLOW_DATA_ROAMING);
            }
        }
        PreferenceCategory emptyPreferenceCategory = new PreferenceCategory(this);
        prefSet.addPreference(emptyPreferenceCategory);
    }

    private boolean isWorldMode() {
        boolean worldModeOn = false;
        final TelephonyManager tm =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        final String configString = getResources().getString(R.string.config_world_mode);

        if (!TextUtils.isEmpty(configString)) {
            String[] configArray = configString.split(";");
            // Check if we have World mode configuration set to True
            // only or config is set to True
            // and SIM GID value is also set and matches to the current SIM GID.
            if (configArray != null &&
                    ((configArray.length == 1 && configArray[0].equalsIgnoreCase("true"))
                            || (configArray.length == 2 && !TextUtils.isEmpty(configArray[1])
                            && tm != null
                            && configArray[1].equalsIgnoreCase(tm.getGroupIdLevel1())))) {
                worldModeOn = true;
            }
        }

        Log.d(TAG, "isWorldMode=" + worldModeOn);

        return worldModeOn;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    private static final String BUTTON_DATA_NETWORK_KEY = "data_network";
    private SwitchPreference mButtonDataNetwork;
    private int mDefaultSubscriptionId;
    private boolean mHasDefaultSubId;

    private int getDefaultSubscriptionId() {
        if (mSubscriptionManager == null) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }
        SubscriptionInfo subscriptionInfo = mSubscriptionManager.getDefaultDataSubscriptionInfo();
        if (subscriptionInfo == null) {
            List<SubscriptionInfo> list = mSubscriptionManager.getAllSubscriptionInfoList();
            if (list.size() == 0) {
                return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            }
            subscriptionInfo = list.get(0);
        }
        return subscriptionInfo.getSubscriptionId();
    }

    private void setMobileDataEnabled(boolean enabled) {
        if (mHasDefaultSubId) {
            mTelephonyManager.setDataEnabled(mDefaultSubscriptionId, enabled);
            mButtonDataNetwork.setChecked(enabled);
        }
    }

    private void disableDataForOtherSubscriptions(int subId) {
        List<SubscriptionInfo> subInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                if (subInfo.getSubscriptionId() != subId) {
                    mTelephonyManager.setDataEnabled(subInfo.getSubscriptionId(), false);
                }
            }
        }
    }

    private void updateChecked() {
        if (mHasDefaultSubId) {
            mButtonDataNetwork.setChecked(mTelephonyManager.getDataEnabled(mDefaultSubscriptionId));
        }
    }

    private final DataStateListener mListener = new DataStateListener() {
        @Override
        public void onChange(boolean selfChange) {
            updateChecked();
        }
    };

    private abstract static class DataStateListener extends ContentObserver {
        public DataStateListener() {
            super(new Handler(Looper.getMainLooper()));
        }

        public void setListener(boolean listening, int subId, Context context) {
            if (listening) {
                Uri uri = Settings.Global.getUriFor(Settings.Global.MOBILE_DATA);
                if (TelephonyManager.getDefault().getSimCount() != 1) {
                    uri = Settings.Global.getUriFor(Settings.Global.MOBILE_DATA + subId);
                }
                context.getContentResolver().registerContentObserver(uri, false, this);
            } else {
                context.getContentResolver().unregisterContentObserver(this);
            }
        }
    }

    private IntentFilter mIntentFilter;

    private void initIntentFilter() {
        /// M: for receivers sim lock gemini phone @{
        mIntentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mIntentFilter.addAction(MtkTelephonyIntents.ACTION_MSIM_MODE_CHANGED);
        mIntentFilter.addAction(MtkTelephonyIntents.ACTION_MD_TYPE_CHANGE);
        mIntentFilter.addAction(MtkTelephonyIntents.ACTION_LOCATED_PLMN_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
        ///@}
        /// M: Add for Sim Switch @{
        mIntentFilter.addAction(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_DONE);
        /// @}
        ExtensionManager.getMobileNetworkSettingsExt()
                .customizeDualVolteIntentFilter(mIntentFilter);
    }

    private boolean mOkClicked;

    @Override
    public void onDismiss(DialogInterface dialog) {
        // Assuming that onClick gets called first
        mButtonDataRoam.setChecked(mOkClicked);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            mPhone.setDataRoamingEnabled(true);
            mOkClicked = true;
        } else {
            // Reset the toggle
            mButtonDataRoam.setChecked(false);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mButtonDataNetwork) {
            setMobileDataEnabled(!mButtonDataNetwork.isChecked());
            disableDataForOtherSubscriptions(mDefaultSubscriptionId);
            //*/
        } else if (preference == mButtonDataRoam) {
            if (DBG) Log.i(TAG, "onPreferenceTreeClick: preference == mButtonDataRoam.");

            //normally called on the toggle click
            if (!mButtonDataRoam.isChecked()) {
                // First confirm with a warning dialog about charges
                mOkClicked = false;
                /// M:Add for plug-in @{
                    /* Google Code, delete by MTK
                    new AlertDialog.Builder(this).setMessage(
                            getResources().getString(R.string.roaming_warning))
                            .setTitle(android.R.string.dialog_alert_title)
                    */
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getResources().getString(R.string.roaming_warning))
                        .setTitle(android.R.string.dialog_alert_title);
                ExtensionManager.getMobileNetworkSettingsExt().customizeAlertDialog(
                        mButtonDataRoam, builder);
                builder.setIconAttribute(android.R.attr.alertDialogIcon)
                        .setPositiveButton(android.R.string.yes, this)
                        .setNegativeButton(android.R.string.no, this)
                        .show()
                        .setOnDismissListener(this);
                /// @}
            } else {
                mPhone.setDataRoamingEnabled(false);
            }
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mButtonDataRoam) {
            // Do not disable the preference screen if the user clicks Data roaming.
            return true;
        } else if (preference == mButtonDataNetwork) {
            return true;
        } else {
            // if the button is anything but the simple toggle preference,
            // we'll need to disable all preferences to reject all click
            // events until the sub-activity's UI comes up.
            // Let the intents be launched by the Preference manager
            return false;
        }
    }

    class SimPreferenceCategory {
        private static final String LOG_TAG = "SimPreferenceCategory";
        private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        private PreferenceCategory mPreferenceCategory;
        Context mContext;

        private boolean mIsCDMAPhone;

        public static final String EXTRA_SUB_ID = "sub_id";

        private static final String GMS_BUTTON_APN_KEY = "gsm_button_apn_key";
        private static final String CDMA_BUTTON_APN_KEY = "cdma_button_apn_key";

        FreemeJumpPreference mAPNJumpPref;
        private Enhanced4GLteSwitchPreference mEnhancedButton4glte;

        private static final String BUTTON_4G_LTE_KEY = "enhanced_4g_lte";

        private Phone mPhone;
        private int mPhoneType = PhoneConstants.PHONE_TYPE_NONE;
        private SubscriptionManager mSubscriptionManager;
        private Activity mActivity;
        private Resources mResource;

        public boolean isRadioOn(int subId, Context context) {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager
                    .getService(Context.TELEPHONY_SERVICE));
            boolean isOn = false;
            try {
                if (phone != null) {
                    isOn = subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID ? false :
                            phone.isRadioOnForSubscriber(subId, context.getPackageName());
                } else {
                    Log.e(TAG, "ITelephony is null !!!");
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            log("isRadioOn = " + isOn + ", subId: " + subId);
            return isOn;
        }

        SimPreferenceCategory(int subId, PreferenceCategory preferenceCategory, Activity activity) {
            mContext = activity.getApplicationContext();
            mSubscriptionManager = SubscriptionManager.from(mContext);
            mPreferenceCategory = preferenceCategory;
            mSubId = subId;
            mActivity = activity;
            mResource = mContext.getResources();

            SubscriptionInfo subInfo = SubscriptionManager.from(mContext).getActiveSubscriptionInfo(mSubId);
            if (subInfo != null && isRadioOn(mSubId, mContext)) {
                preferenceCategory.setEnabled(true);
                mPhone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(mSubId));

                mPhoneType = mPhone.getPhoneType();
            } else {
                preferenceCategory.setEnabled(false);
            }

            if (mPhone == null) {
                mPhone = PhoneGlobals.getPhone();
            }
            if (mPhone == null) {
                preferenceCategory.setEnabled(false);
            }

            if (TelephonyUtilsEx.isCDMAPhone(mPhone)
                    /// M: [CT VOLTE]
                    || (TelephonyUtilsEx.isCtVolteEnabled() &&
                    TelephonyUtilsEx.isCt4gSim(mSubId) &&
                    !TelephonyUtilsEx.isRoaming(mPhone) &&
                    (!TelephonyUtilsEx.isBothslotCtSim(mSubscriptionManager)))) {
                mIsCDMAPhone = true;
            } else {
                mIsCDMAPhone = false;
            }

            mEnable4GHandler = new Enable4GHandler();
            mHandler = new MyHandler();
            addGmsOrCdmaButtionJump();
        }

        private boolean isVolteEnabled() {
            boolean isVolteEnabled = false;
            if (mPhone == null) {
                return isVolteEnabled;
            }
            isVolteEnabled = ImsManager.isVolteEnabledByPlatform(mContext);
            if (SystemProperties.getInt("persist.mtk_mims_support", 1) > 1) {
                isVolteEnabled = ImsManager.isVolteEnabledByPlatform(mContext, mPhone.getPhoneId());
            }
            return isVolteEnabled;
        }

        private void addEnhanced4GLteSwitchPreference() {
            Log.i(LOG_TAG, "[addEnhanced4GLteSwitchPreference] volteEnabled :"
                    + isVolteEnabled());

            mEnhancedButton4glte = new Enhanced4GLteSwitchPreference(mActivity, mSubId);

            boolean isCtPlugin = ExtensionManager.getMobileNetworkSettingsExt().isCtPlugin();
            Log.i(LOG_TAG, "[addEnhanced4GLteSwitchPreference] ss :" + isCtPlugin);
            if (isVolteEnabled() && !isCtPlugin) {
                /// Still use Google's key, title, and summary.
                mEnhancedButton4glte.setKey(BUTTON_4G_LTE_KEY + mSubId);
                /// M: [CT VOLTE]
                // show "VOLTE" for CT VOLTE SIM
                if (TelephonyUtilsEx.isCtVolteEnabled() && TelephonyUtilsEx.isCtSim(mSubId)) {
                    mEnhancedButton4glte.setTitle(R.string.hd_voice_switch_title);
                    mEnhancedButton4glte.setSummary(R.string.hd_voice_switch_summary);
                } else {
                    PersistableBundle carrierConfig =
                            PhoneGlobals.getInstance().getCarrierConfigForSubId(mSubId);
                    boolean useVariant4glteTitle = carrierConfig.getBoolean(
                            CarrierConfigManager.KEY_ENHANCED_4G_LTE_TITLE_VARIANT_BOOL);
                    int enhanced4glteModeTitleId = useVariant4glteTitle ?
                            R.string.enhanced_4g_lte_mode_title_variant :
                            R.string.enhanced_4g_lte_mode_title;
                    mEnhancedButton4glte.setTitle(enhanced4glteModeTitleId);
                    mEnhancedButton4glte.setSummary(R.string.enhanced_4g_lte_mode_summary);
                }

                mEnhancedButton4glte.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String volteTitle = mContext.getResources().getString(R.string.hd_voice_switch_title);
                        String lteTitle = mContext.getResources().getString(R.string.enhanced_4g_lte_mode_title);
                        Log.i(LOG_TAG, "[onPreferenceChangeMTK] Preference = " + preference.getTitle());
                        if ((mEnhancedButton4glte == preference) || preference.getTitle().equals(volteTitle)
                                || preference.getTitle().equals(lteTitle)) {
                            Enhanced4GLteSwitchPreference ltePref = (Enhanced4GLteSwitchPreference) preference;
                            Log.i(LOG_TAG, "[onPreferenceChangeMTK] IsChecked = " + ltePref.isChecked());
                            /// M: [CT VOLTE] @{
                            if (TelephonyUtilsEx.isCtVolteEnabled()
                                    && TelephonyUtilsEx.isCtSim(mSubId)
                                    && !ltePref.isChecked()) {
                                int type = TelephonyManager.getDefault().getNetworkType(mSubId);
                                Log.i(LOG_TAG, "network type = " + type);
                                if (TelephonyManager.NETWORK_TYPE_LTE != type
                                        && !TelephonyUtilsEx.isRoaming(mPhone)
                                        && (TelephonyUtilsEx.getMainPhoneId() == mPhone.getPhoneId()
                                        || TelephonyUtilsEx.isBothslotCt4gSim(mSubscriptionManager))) {
                                    if (!TelephonyUtilsEx.isCtAutoVolteEnabled()) {
                                    showVolteUnavailableDialog();
                                    return false;
                                    }
                                }
                            }
                            updateEnhanced4GLteSwitchPreference(mSubId);
                            ltePref.setChecked(!ltePref.isChecked());
                            MtkImsManager.setEnhanced4gLteModeSetting(mContext, ltePref.isChecked(),
                                    mPhone.getPhoneId());
                            return true;
                        }
                        return false;
                    }
                });
                mEnhancedButton4glte.setOrder(ENHANCED_BUTTON_4G_LTE_ORDER);
                /// M: Customize the LTE switch preference. @{
                ExtensionManager.getMobileNetworkSettingsExt()
                        .customizeEnhanced4GLteSwitchPreference(MobileNetworkSettings.this, mEnhancedButton4glte);
                /// @}
                mPreferenceCategory.addPreference(mEnhancedButton4glte);
                if (mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    updateEnhanced4GLteSwitchPreference(mSubId);
                }
            } else {
                mEnhancedButton4glte = null;
            }
        }

        /**
         * Update the subId in mEnhancedButton4glte.
         */
        public void updateEnhanced4GLteSwitchPreference(int subId) {
            int phoneId = SubscriptionManager.getPhoneId(subId);
            if (mEnhancedButton4glte != null) {
                boolean showVolte =
                        (SystemProperties.getInt("persist.mtk_mims_support", 1) == 1 &&
                                ImsManager.isVolteEnabledByPlatform(mContext) &&
                                TelephonyUtilsEx.getMainPhoneId() == mPhone.getPhoneId()) ||
                                (SystemProperties.getInt("persist.mtk_mims_support", 1) > 1 &&
                                        ImsManager.isVolteEnabledByPlatform(mContext, mPhone.getPhoneId()) &&
                                        isCapabilityPhone(mPhone));
                if (ExtensionManager.getMobileNetworkSettingsExt().isEnhancedLTENeedToAdd(
                        showVolte, mPhone.getPhoneId())) {
                    if (mPreferenceCategory.findPreference(BUTTON_4G_LTE_KEY + subId) == null) {
                        Log.i(LOG_TAG, "updateEnhanced4GLteSwitchPreference add switcher");
                        mPreferenceCategory.addPreference(mEnhancedButton4glte);
                    }
                } else {
                    if (mPreferenceCategory.findPreference(BUTTON_4G_LTE_KEY + subId) != null) {
                        mEnhancedButton4glte.setEnabled(false);
                        mEnhancedButton4glte.setChecked(false);
                    }
                }
                if (mPreferenceCategory.findPreference(BUTTON_4G_LTE_KEY + subId) != null) {
                    mEnhancedButton4glte.setSubId(subId);
                    boolean enh4glteMode = MtkImsManager.isEnhanced4gLteModeSettingEnabledByUser(
                            mContext, phoneId) &&
                            MtkImsManager.isNonTtyOrTtyOnVolteEnabled(mContext, phoneId);
                    mEnhancedButton4glte.setChecked(enh4glteMode);
                    Log.i(LOG_TAG, "[updateEnhanced4GLteSwitchPreference] SubId = " + subId
                            + ", enh4glteMode=" + enh4glteMode);
                }
                /// M: update enabled state
                updateEnhanced4glteEnableState(subId);
            }
        }

        public void updateEnhanced4glteEnableState(int subId) {
            if (mEnhancedButton4glte != null) {
                boolean inCall = TelecomManager.from(mContext).isInCall();
                boolean nontty = MtkImsManager.isNonTtyOrTtyOnVolteEnabled(mContext,
                        mPhone.getPhoneId());
                /// M: [CT VOLTE] @{
                boolean enableForCtVolte = true;
                if (TelephonyUtilsEx.isCtVolteEnabled() && TelephonyUtilsEx.isCtSim(subId)) {
                    int settingsNetworkMode = Settings.Global.getInt(mPhone
                                    .getContext().getContentResolver(),
                            Settings.Global.PREFERRED_NETWORK_MODE + subId,
                            Phone.PREFERRED_NT_MODE);
                    enableForCtVolte = TelephonyUtilsEx.isCt4gSim(subId)
                            && (settingsNetworkMode == Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA
                            || settingsNetworkMode == Phone.NT_MODE_LTE_GSM_WCDMA
                            || settingsNetworkMode == Phone.NT_MODE_LTE_CDMA_AND_EVDO
                            || settingsNetworkMode == Phone.NT_MODE_LTE_ONLY
                            || settingsNetworkMode == Phone.NT_MODE_LTE_WCDMA);
                    if (TelephonyUtilsEx.isCtAutoVolteEnabled()) {
                        enableForCtVolte = true;
                    }
                }
                /// @}
                /// M: [CMCC DUAl VOLTE] @{
                enableForCtVolte = ExtensionManager.getMobileNetworkSettingsExt()
                        .customizeDualVolteOpDisable(mSubId, enableForCtVolte);
                /// @}
                boolean secondEnabled = isSecondVolteEnabled();
                Log.i(LOG_TAG, "updateEnhanced4glteEnableState, incall = " + inCall + ", nontty = " + nontty
                        + ", enableForCtVolte = " + enableForCtVolte + ", secondEnabled = "
                        + secondEnabled);
                mEnhancedButton4glte.setEnabled(!inCall && nontty
                        && enableForCtVolte);
            }
        }

        private boolean isSecondVolteEnabled() {
            if (!TelephonyUtilsEx.isBothslotCtSim(mSubscriptionManager)) {
                return true;
            }
            if (TelephonyUtilsEx.getMainPhoneId() == mPhone.getPhoneId()) {
                return true;
            } else {
                return false;
            }
        }

        /**
         * Is the phone has 3/4G capability or not.
         *
         * @return true if phone has 3/4G capability
         */
        private boolean isCapabilityPhone(Phone phone) {
            boolean result = phone != null ? ((phone.getRadioAccessFamily()
                    & (RadioAccessFamily.RAF_UMTS | RadioAccessFamily.RAF_LTE)) > 0) : false;
            return result;
        }

        private void addGmsOrCdmaButtionJump() {
            addCdmaOrGsmUmtsPreferenceSettings();
            addAPNSettings();
            addEnhanced4GLteSwitchPreference();
        }

        private void addAPNSettings() {
            if (mAPNJumpPref == null) {
                mAPNJumpPref = new FreemeJumpPreference(mActivity);
            } else {
                mPreferenceCategory.removePreference(mAPNJumpPref);
            }
            String apnPrefKey = mIsCDMAPhone ? CDMA_BUTTON_APN_KEY + mSubId : GMS_BUTTON_APN_KEY + mSubId;
            mAPNJumpPref.setKey(apnPrefKey);
            mAPNJumpPref.setTitle(mContext.getResources().getString(R.string.apn_settings));
            mAPNJumpPref.setOrder(APN_SETTINGS_ORDER);
            mPreferenceCategory.addPreference(mAPNJumpPref);

            if (!mIsCDMAPhone) {
                if (mPhoneType != PhoneConstants.PHONE_TYPE_GSM) {
                    mAPNJumpPref.setEnabled(false);
                } else {
                    PersistableBundle carrierConfig =
                            PhoneGlobals.getInstance().getCarrierConfigForSubId(mSubId);
                    if (!carrierConfig.getBoolean(CarrierConfigManager.KEY_APN_EXPAND_BOOL)
                            && mAPNJumpPref != null) {
                        mAPNJumpPref.setEnabled(false);
                    }
                }
            } else {
                if (isWorldMode() && mAPNJumpPref != null) {
                    mPreferenceCategory.removePreference(mAPNJumpPref);
                    mAPNJumpPref.setEnabled(false);
                }
            }

            mAPNJumpPref.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            // We need to build the Intent by hand as the Preference Framework
                            // does not allow to add an Intent with some extras into a Preference
                            // XML file
                            final Intent intent = new Intent(Settings.ACTION_APN_SETTINGS);
                            // This will setup the Home and Search affordance
                            intent.putExtra(":settings:show_fragment_as_subsetting", true);
                            intent.putExtra(EXTRA_SUB_ID, mIsCDMAPhone ? mPhone.getSubId() : mSubId);
                            intent.putExtra(FreemeActionBarUtil.EXTRA_NAVIGATE_UP_TITLE_TEXT,
                                    mContext.getResources().getString(R.string.freeme_mobile_networks));
                            mContext.startActivity(intent);
                            return true;
                        }
                    });
        }

        private boolean isWorldMode() {
            boolean worldModeOn = false;
            final TelephonyManager tm =
                    (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            final String configString = mContext.getResources().getString(R.string.config_world_mode);

            if (!TextUtils.isEmpty(configString)) {
                String[] configArray = configString.split(";");
                // Check if we have World mode configuration set to True
                // only or config is set to True
                // and SIM GID value is also set and matches to the current SIM GID.
                if (configArray != null &&
                        ((configArray.length == 1 && configArray[0].equalsIgnoreCase("true"))
                                || (configArray.length == 2 && !TextUtils.isEmpty(configArray[1])
                                && tm != null
                                && configArray[1].equalsIgnoreCase(tm.getGroupIdLevel1())))) {
                    worldModeOn = true;
                }
            }

            Log.d(LOG_TAG, "isWorldMode=" + worldModeOn);

            return worldModeOn;
        }

        /**
         * [CT VOLTE]When network type is not LTE, show dialog.
         */
        private void showVolteUnavailableDialog() {
            Log.i(LOG_TAG, "showVolteUnavailableDialog ...");
            final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            String title = mContext.getResources().getString(R.string.alert_ct_volte_unavailable, PhoneUtils
                    .getSubDisplayName(mPhone.getSubId()));
            Dialog dialog = builder.setMessage(title).setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.i(LOG_TAG, "dialog cancel mEnhanced4GLteSwitchPreference.setchecked  = "
                                    + !mEnhancedButton4glte.isChecked());
                            mEnhancedButton4glte.setChecked(false);

                        }
                    }).setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mEnhancedButton4glte.setChecked(true);
                            Log.i(LOG_TAG, "dialog ok" + " ims set " + mEnhancedButton4glte.isChecked() + " mSlotId = "
                                    + SubscriptionManager.getPhoneId(mPhone.getSubId()));
                            MtkImsManager.setEnhanced4gLteModeSetting(mContext,
                                    mEnhancedButton4glte.isChecked(), mPhone.getPhoneId());
                        }
                    }).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (KeyEvent.KEYCODE_BACK == keyCode) {
                        if (null != dialog) {
                            Log.i(LOG_TAG, "onKey keycode = back"
                                    + "dialog cancel mEnhanced4GLteSwitchPreference.setchecked  = "
                                    + !mEnhancedButton4glte.isChecked());
                            mEnhancedButton4glte.setChecked(false);
                            dialog.dismiss();
                            return true;
                        }
                    }
                    return false;
                }
            });
            mDialog = dialog;
            dialog.show();
        }

        private Dialog mDialog;

        public void disMissDialog() {
            if (mDialog != null && mDialog.isShowing()) {
                mDialog.dismiss();
            }
        }

        public void onDestory() {
            Log.d(TAG, "onDestroy");
        }

        public void onResume() {
            Log.d(TAG, "onResume");
            if (mPreferenceCategory.findPreference(BUTTON_ENABLED_NETWORKS_KEY + mSubId) != null) {
                updatePreferredNetworkUIFromDb();
            }
        }

        private void updatePreferredNetworkUIFromDb() {
            final int phoneSubId = mPhone.getSubId();

            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                    preferredNetworkMode);

            if (DBG) {
                log("updatePreferredNetworkUIFromDb: settingsNetworkMode = " +
                        settingsNetworkMode);
            }

            UpdateEnabledNetworksValueAndSummary(settingsNetworkMode);
        }

        private void UpdateEnabledNetworksValueAndSummary(int NetworkMode) {
            Log.d(LOG_TAG, "NetworkMode: " + NetworkMode);
            if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                return;
            }
            // M: if is not 3/4G phone, init the preference with gsm only type @{
            if (!isCapabilityPhone(mPhone)) {
                NetworkMode = Phone.NT_MODE_GSM_ONLY;
                log("init EnabledNetworks with gsm only");
            }
            // @}
            switch (NetworkMode) {
                case Phone.NT_MODE_TDSCDMA_WCDMA:
                case Phone.NT_MODE_TDSCDMA_GSM_WCDMA:
                case Phone.NT_MODE_TDSCDMA_GSM:
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_TDSCDMA_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary(R.string.network_3G);
                    break;
                case Phone.NT_MODE_WCDMA_ONLY:
                case Phone.NT_MODE_GSM_UMTS:
                case Phone.NT_MODE_WCDMA_PREF:
                    if (!mIsCDMAPhone) {
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_WCDMA_PREF));
                        mButtonEnabledNetworks.setSummary(R.string.network_3G);
                    } else {
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                        mButtonEnabledNetworks.setSummary(R.string.network_global);
                    }
                    break;
                case Phone.NT_MODE_GSM_ONLY:
                    if (!mIsCDMAPhone) {
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_GSM_ONLY));
                        mButtonEnabledNetworks.setSummary(R.string.network_2G);
                    } else {
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                        mButtonEnabledNetworks.setSummary(R.string.network_global);
                    }
                    break;
                case Phone.NT_MODE_LTE_GSM_WCDMA:
                    if (isWorldMode()) {
                        mButtonEnabledNetworks.setSummary(
                                R.string.preferred_network_mode_lte_gsm_umts_summary);
                        //controlCdmaOptions(false);
                        //controlGsmOptions(true);
                        break;
                    }
                case Phone.NT_MODE_LTE_ONLY:
                case Phone.NT_MODE_LTE_WCDMA:
                    if (!mIsCDMAPhone) {
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_LTE_GSM_WCDMA));
                        mButtonEnabledNetworks.setSummary((mShow4GForLTE == true)
                                ? R.string.network_4G : R.string.network_lte);
                    } else {
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                        mButtonEnabledNetworks.setSummary(R.string.network_global);
                    }
                    break;
                case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                    if (isWorldMode()) {
                        mButtonEnabledNetworks.setSummary(
                                R.string.preferred_network_mode_lte_cdma_summary);
                        //controlCdmaOptions(true);
                        //controlGsmOptions(false);
                    } else {
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_LTE_CDMA_AND_EVDO));
                        mButtonEnabledNetworks.setSummary(R.string.network_lte);
                    }
                    break;
                case Phone.NT_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary(R.string.network_3G);
                    break;
                case Phone.NT_MODE_CDMA:
                case Phone.NT_MODE_EVDO_NO_CDMA:
                case Phone.NT_MODE_GLOBAL:
                    /// M: For C2K @{
                    if (isC2kLteSupport()) {
                        log("Update value to Global for c2k project");
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_GLOBAL));
                    } else {
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_CDMA));
                    }
                    /// @}

                    mButtonEnabledNetworks.setSummary(R.string.network_3G);
                    break;
                case Phone.NT_MODE_CDMA_NO_EVDO:
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_CDMA_NO_EVDO));
                    mButtonEnabledNetworks.setSummary(R.string.network_1x);
                    break;
                case Phone.NT_MODE_TDSCDMA_ONLY:
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_TDSCDMA_ONLY));
                    mButtonEnabledNetworks.setSummary(R.string.network_3G);
                    break;
                case Phone.NT_MODE_LTE_TDSCDMA_GSM:
                case Phone.NT_MODE_LTE_TDSCDMA_GSM_WCDMA:
                case Phone.NT_MODE_LTE_TDSCDMA:
                case Phone.NT_MODE_LTE_TDSCDMA_WCDMA:
                case Phone.NT_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                case Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                    if (isSupportTdscdma()) {
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA));
                        mButtonEnabledNetworks.setSummary(R.string.network_lte);
                    } else {
                        if (isWorldMode()) {
                            //controlCdmaOptions(true);
                            //controlGsmOptions(false);
                        }
                        mButtonEnabledNetworks.setValue(
                                Integer.toString(Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                        if (mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA ||
                                mIsCDMAPhone ||
                                isWorldMode()) {
                            mButtonEnabledNetworks.setSummary(R.string.network_global);
                        } else {
                            mButtonEnabledNetworks.setSummary((mShow4GForLTE == true)
                                    ? R.string.network_4G : R.string.network_lte);
                        }
                    }
                    break;
                default:
                    String errMsg = "Invalid Network Mode (" + NetworkMode + "). Ignore.";
                    Log.e(TAG, errMsg);
                    mButtonEnabledNetworks.setSummary(errMsg);
            }
            ExtensionManager.getMobileNetworkSettingsExt().
                    updatePreferredNetworkValueAndSummary(mButtonEnabledNetworks, NetworkMode);
            /// Add for Plug-in @{
            if (mButtonEnabledNetworks != null) {
                log("Enter plug-in update updateNetworkTypeSummary - Enabled.");
                ExtensionManager.getMobileNetworkSettingsExt()
                        .updateNetworkTypeSummary(mButtonEnabledNetworks);
                /// Add for cmcc open market @{
                //mOmEx.updateNetworkTypeSummary(mButtonEnabledNetworks);
                /// @}
            }
            /// @}
        }

        private boolean isSupportTdscdma() {
            /// M: TODO: temple solution for MR1 changes
            return false;
        }

        private boolean isC2kLteSupport() {
            return FeatureOption.isMtkSrlteSupport()
                    || FeatureOption.isMtkSvlteSupport();
        }

        ListPreference mButtonEnabledNetworks;
        private static final String BUTTON_ENABLED_NETWORKS_KEY = "enabled_networks_key";
        private MyHandler mHandler;
        private boolean mIsGlobalCdma;

        private void addPreferredNetworks() {
            if (mButtonEnabledNetworks == null) {
                mButtonEnabledNetworks = new ListPreference(mActivity);
            } else {
                mPreferenceCategory.removePreference(mButtonEnabledNetworks);
            }

            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + mPhone.getSubId(),
                    preferredNetworkMode);

            boolean isLteOnCdma = mPhone.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE;
            PersistableBundle carrierConfig =
                    PhoneGlobals.getInstance().getCarrierConfigForSubId(mSubId);
            mIsGlobalCdma = isLteOnCdma
                    && carrierConfig.getBoolean(CarrierConfigManager.KEY_SHOW_CDMA_CHOICES_BOOL);
            if (carrierConfig.getBoolean(CarrierConfigManager
                    .KEY_HIDE_PREFERRED_NETWORK_TYPE_BOOL)
                    && !mPhone.getServiceState().getRoaming()) {
                settingsNetworkMode = preferredNetworkMode;
            }

            if (isSupportTdscdma()) {
                mButtonEnabledNetworks.setEntries(
                        R.array.enabled_networks_tdscdma_choices);
                mButtonEnabledNetworks.setEntryValues(
                        R.array.enabled_networks_tdscdma_values);
            } else if (!carrierConfig.getBoolean(CarrierConfigManager.KEY_PREFER_2G_BOOL)
                    && !getResources().getBoolean(R.bool.config_enabled_lte)) {
                mButtonEnabledNetworks.setEntries(
                        R.array.enabled_networks_except_gsm_lte_choices);
                mButtonEnabledNetworks.setEntryValues(
                        R.array.enabled_networks_except_gsm_lte_values);
            } else if (!carrierConfig.getBoolean(CarrierConfigManager.KEY_PREFER_2G_BOOL)) {
                int select = (mShow4GForLTE == true) ?
                        R.array.enabled_networks_except_gsm_4g_choices
                        : R.array.enabled_networks_except_gsm_choices;
                mButtonEnabledNetworks.setEntries(select);
                mButtonEnabledNetworks.setEntryValues(
                        R.array.enabled_networks_except_gsm_values);
            } else if (!FeatureOption.isMtkLteSupport()) {
                mButtonEnabledNetworks.setEntries(
                        R.array.enabled_networks_except_lte_choices);
                if (isC2kLteSupport() && FeatureOption.isNeedDisable4G()) {
                    log("for bad phone change entries~");
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_except_lte_values_c2k);
                } else {
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_except_lte_values);
                }
            } else if (mIsGlobalCdma) {
                mButtonEnabledNetworks.setEntries(
                        R.array.enabled_networks_cdma_choices);
                mButtonEnabledNetworks.setEntryValues(
                        R.array.enabled_networks_cdma_values);
            } else {
                int select = (mShow4GForLTE == true) ? R.array.enabled_networks_4g_choices
                        : R.array.enabled_networks_choices;
                mButtonEnabledNetworks.setEntries(select);
                ExtensionManager.getMobileNetworkSettingsExt().changeEntries(
                        mButtonEnabledNetworks);
                /// Add for C2K @{
                if (isC2kLteSupport()) {
                    if (DBG) {
                        log("Change to C2K values");
                    }
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_values_c2k);
                } else {
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_values);
                }
                /// @}
            }

            mButtonEnabledNetworks.setKey(BUTTON_ENABLED_NETWORKS_KEY + mSubId);
            mButtonEnabledNetworks.setTitle(mResource.getString(R.string.preferred_network_mode_title));
            mButtonEnabledNetworks.setDialogTitle(mResource.getString(R.string.preferred_network_mode_dialogtitle));
            mButtonEnabledNetworks.setOrder(PREFERRED_NETWORK_ORDER);

            mButtonEnabledNetworks.setValue(Integer.toString(settingsNetworkMode));
            UpdateEnabledNetworksValueAndSummary(settingsNetworkMode);

            mButtonEnabledNetworks.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object objValue) {

                    mButtonEnabledNetworks.setValue((String) objValue);
                    int buttonNetworkMode;
                    buttonNetworkMode = Integer.parseInt((String) objValue);
                    if (DBG) log("buttonNetworkMode: " + buttonNetworkMode);
                    int settingsNetworkMode = android.provider.Settings.Global.getInt(
                            mPhone.getContext().getContentResolver(),
                            android.provider.Settings.Global.PREFERRED_NETWORK_MODE + mPhone.getSubId(),
                            preferredNetworkMode);

                    if (DBG) {
                        log("buttonNetworkMode: " + buttonNetworkMode +
                                "settingsNetworkMode: " + settingsNetworkMode);
                    }
                    if (buttonNetworkMode != settingsNetworkMode ||
                            ExtensionManager.getMobileNetworkSettingsExt().isNetworkChanged(
                                    mButtonEnabledNetworks, buttonNetworkMode, settingsNetworkMode,
                                    mPhone)) {
                        int modemNetworkMode;
                        // if new mode is invalid ignore it
                        switch (buttonNetworkMode) {
                            case Phone.NT_MODE_WCDMA_PREF:
                            case Phone.NT_MODE_GSM_ONLY:
                            case Phone.NT_MODE_LTE_GSM_WCDMA:
                            case Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                            case Phone.NT_MODE_CDMA:
                            case Phone.NT_MODE_CDMA_NO_EVDO:
                            case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                            case Phone.NT_MODE_TDSCDMA_ONLY:
                            case Phone.NT_MODE_TDSCDMA_WCDMA:
                            case Phone.NT_MODE_LTE_TDSCDMA:
                            case Phone.NT_MODE_LTE_WCDMA:
                            case Phone.NT_MODE_TDSCDMA_GSM:
                            case Phone.NT_MODE_LTE_TDSCDMA_GSM:
                            case Phone.NT_MODE_TDSCDMA_GSM_WCDMA:
                            case Phone.NT_MODE_LTE_TDSCDMA_WCDMA:
                            case Phone.NT_MODE_LTE_TDSCDMA_GSM_WCDMA:
                            case Phone.NT_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                            case Phone.NT_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                            case Phone.NT_MODE_WCDMA_ONLY:
                            case Phone.NT_MODE_LTE_ONLY:
                                /// M: Add for C2K
                            case Phone.NT_MODE_GLOBAL:
                                // This is one of the modes we recognize
                                modemNetworkMode = buttonNetworkMode;
                                break;
                            default:
                                Log.e(TAG, "Invalid Network Mode (" + buttonNetworkMode +
                                        ") chosen. Ignore.");
                                return true;
                        }

                        UpdateEnabledNetworksValueAndSummary(buttonNetworkMode);

                        /// M: 03100374, need to revert the network mode if set fail
                        mPreNetworkMode = settingsNetworkMode;


                        if (ExtensionManager.getMobileNetworkSettingsExt().isNetworkUpdateNeeded(
                                mButtonEnabledNetworks,
                                buttonNetworkMode,
                                settingsNetworkMode,
                                mPhone,
                                mPhone.getContext().getContentResolver(),
                                mPhone.getSubId(), mHandler)) {
                            UpdateEnabledNetworksValueAndSummary(buttonNetworkMode);
                            android.provider.Settings.Global.putInt(mPhone.getContext().
                                            getContentResolver(),
                                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE +
                                            mPhone.getSubId(),
                                    buttonNetworkMode);

                            //if (DBG) {
                            log("setPreferredNetworkType, networkType: " + modemNetworkMode);
                            //}
                            //Set the modem network mode
                            mPhone.setPreferredNetworkType(modemNetworkMode, mHandler
                                    .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
                        }
                    }
                    return true;
                }
            });
            //mOmEx.updateNetworkTypeSummary(mButtonEnabledNetworks);
            mPreferenceCategory.addPreference(mButtonEnabledNetworks);
        }

        FreemeJumpPreference mNetworkOpreatorsPref;
        private static final String BUTTONN_CARRIER_SEL_KEY = "button_carrier_sel_key";

        private void addNetworkOperators() {
            if (mNetworkOpreatorsPref == null) {
                mNetworkOpreatorsPref = new FreemeJumpPreference(mActivity);
            } else {
                mPreferenceCategory.addPreference(mNetworkOpreatorsPref);
            }

            mNetworkOpreatorsPref.setKey(BUTTONN_CARRIER_SEL_KEY + mSubId);
            mNetworkOpreatorsPref.setTitle(mResource.getString(R.string.networks));
            mNetworkOpreatorsPref.setSummary(mResource.getString(R.string.sum_carrier_select));
            mNetworkOpreatorsPref.setPersistent(false);
            mNetworkOpreatorsPref.setOrder(NETWORK_OPERATORS_ORDER);

            mNetworkOpreatorsPref.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            final Intent intent = new Intent(Intent.ACTION_MAIN);
                            intent.setComponent(new ComponentName("com.android.phone",
                                    "com.android.phone.NetworkSetting"));
                            intent.putExtra(EXTRA_SUB_ID, mSubId);
                            intent.putExtra(FreemeActionBarUtil.EXTRA_NAVIGATE_UP_TITLE_TEXT,
                                    mResource.getString(R.string.freeme_mobile_networks));
                            mActivity.startActivity(intent);
                            return true;
                        }
                    });
            mPreferenceCategory.addPreference(mNetworkOpreatorsPref);
        }

        private FreemeJumpPreference mPLMNPreference;

        private void addPLMNList() {
            // add PLMNList, if c2k project the order should under the 4g data only
            if (mPLMNPreference == null) {
                mPLMNPreference = new FreemeJumpPreference(mActivity);
            } else {
                mPreferenceCategory.removePreference(mPLMNPreference);
            }

            mPLMNPreference.setKey(BUTTON_PLMN_LIST + mSubId);
            mPLMNPreference.setTitle(mResource.getString(R.string.plmn_list_setting_title));
            Intent intentPlmn = new Intent();
            intentPlmn.setClassName("com.android.phone",
                    "com.mediatek.settings.PLMNListPreference");
            intentPlmn.putExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, mPhone.getSubId());
            intentPlmn.putExtra(FreemeActionBarUtil.EXTRA_NAVIGATE_UP_TITLE_TEXT,
                    getResources().getString(R.string.freeme_mobile_networks));
            mPLMNPreference.setIntent(intentPlmn);
            mPLMNPreference.setOrder(PLMN_ORDER);
            mPreferenceCategory.addPreference(mPLMNPreference);
        }

        private void addCdmaSystemSelect() {
            if (mButtonCdmaSystemSelect == null) {
                mButtonCdmaSystemSelect = new CdmaSystemSelectListPreference(mActivity);
            } else {
                mPreferenceCategory.removePreference(mButtonCdmaSystemSelect);
            }

            mButtonCdmaSystemSelect.setKey(BUTTON_CDMA_SYSTEM_SELECT_KEY + mSubId);
            mButtonCdmaSystemSelect.setTitle(mResource.getString(R.string.cdma_system_select_title));
            mButtonCdmaSystemSelect.setSummary(mResource.getString(R.string.cdma_system_select_summary));
            mButtonCdmaSystemSelect.setEntries(mResource.getStringArray(R.array.cdma_system_select_choices));
            mButtonCdmaSystemSelect.setEntryValues(mResource.getStringArray(R.array.cdma_system_select_values));
            mButtonCdmaSystemSelect.setDialogTitle(mResource.getString(R.string.cdma_system_select_dialogtitle));
            mButtonCdmaSystemSelect.setOrder(CDMA_SYSTEM_ORDER);

            if (mButtonCdmaSystemSelect != null) {
                mButtonCdmaSystemSelect.setPhone(mPhone);
                mButtonCdmaSystemSelect.setEnabled(true);
                mButtonCdmaSystemSelect.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        return true;
                    }
                });
            }
            mPreferenceCategory.addPreference(mButtonCdmaSystemSelect);
        }

        private class MyHandler extends Handler {

            static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 0;

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_SET_PREFERRED_NETWORK_TYPE:
                        handleSetPreferredNetworkTypeResponse(msg);
                        break;
                }
            }

            private void handleSetPreferredNetworkTypeResponse(Message msg) {
                /// M: 03100374 restore network mode in case set fail
                restorePreferredNetworkTypeIfNeeded(msg);
                if (isDestroyed()) {
                    // Access preferences of activity only if it is not destroyed
                    // or if fragment is not attached to an activity.
                    return;
                }

                AsyncResult ar = (AsyncResult) msg.obj;
                final int phoneSubId = mPhone.getSubId();

                if (ar.exception == null) {
                    int networkMode;

                    if (getPreferenceScreen().findPreference(BUTTON_ENABLED_NETWORKS_KEY) != null) {
                        networkMode = Integer.parseInt(mButtonEnabledNetworks.getValue());
                        if (DBG) {
                            log("handleSetPreferredNetwrokTypeResponse2: networkMode:" +
                                    networkMode);
                        }
                        android.provider.Settings.Global.putInt(
                                mPhone.getContext().getContentResolver(),
                                android.provider.Settings.Global.PREFERRED_NETWORK_MODE
                                        + phoneSubId,
                                networkMode);
                    }
                    log("Start Network updated intent");
                    Intent intent = new Intent(TelephonyUtils.ACTION_NETWORK_CHANGED);
                    sendBroadcast(intent);
                } else {
                    Log.i(LOG_TAG, "handleSetPreferredNetworkTypeResponse:" +
                            "exception in setting network mode.");
                    updatePreferredNetworkUIFromDb();
                }
            }
        }

        private int mPreNetworkMode = -1;

        private void restorePreferredNetworkTypeIfNeeded(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception != null && mPreNetworkMode != -1 && mPhone != null) {
                final int phoneSubId = mPhone.getSubId();
                log("set failed, reset preferred network mode to " + mPreNetworkMode + ", sub id = "
                        + phoneSubId);
                android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                        mPreNetworkMode);
            }
            mPreNetworkMode = -1;
        }

        SwitchPreference mEnable4GDataPreference;
        private Enable4GHandler mEnable4GHandler;
        private static final String ENABLE_4G_DATA = "enable_4g_data";
        private static final String BUTTON_CDMA_SYSTEM_SELECT_KEY = "cdma_system_select_key";
        private static final int ENHANCED_BUTTON_4G_LTE_ORDER = 0;
        private static final int APN_SETTINGS_ORDER = ENHANCED_BUTTON_4G_LTE_ORDER + 1;
        private static final int ENABLE_4G_NETWORK_ORDER = APN_SETTINGS_ORDER + 1;
        private static final int PLMN_ORDER = APN_SETTINGS_ORDER + 1;
        private static final int PREFERRED_NETWORK_ORDER = PLMN_ORDER + 1;
        private static final int CDMA_SYSTEM_ORDER = ENABLE_4G_NETWORK_ORDER + 1;
        private static final int NETWORK_OPERATORS_ORDER = PREFERRED_NETWORK_ORDER + 1;

        CdmaSystemSelectListPreference mButtonCdmaSystemSelect;

        private void addCdmaOrGsmUmtsPreferenceSettings() {
            if (mIsCDMAPhone) {
                addEnable4GNetworkItem();
                addCdmaSystemSelect();
            } else {
                addPLMNList();
                addNetworkOperators();
                addPreferredNetworks();
            }
        }

        private void addEnable4GNetworkItem() {
            if (mEnable4GDataPreference == null) {
                mEnable4GDataPreference = new SwitchPreference(mActivity);
            } else {
                mPreferenceCategory.removePreference(mEnable4GDataPreference);
            }

            mEnable4GDataPreference.setTitle(mResource.getString(R.string.enable_4G_data));
            mEnable4GDataPreference.setKey(ENABLE_4G_DATA + mSubId);
            mEnable4GDataPreference.setSummary(mResource.getString(R.string.enable_4G_data_summary));
            mEnable4GDataPreference.setOrder(ENABLE_4G_NETWORK_ORDER);

            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + mPhone.getSubId(),
                    preferredNetworkMode);

            boolean enable = (isLteCardReady() && isCapabilityPhone(mPhone))
                    || TelephonyUtils.isCTLteTddTestSupport();
            boolean checked = enable && (settingsNetworkMode == Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA
                    || settingsNetworkMode == MtkGsmCdmaPhone.NT_MODE_LTE_TDD_ONLY
                    || settingsNetworkMode == Phone.NT_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA);
            Log.d(TAG, " enable = " + enable + " ,settingsNetworkMode: " + settingsNetworkMode
                    + " checked = " + checked);

            mEnable4GDataPreference.setEnabled(enable);
            mEnable4GDataPreference.setChecked(checked);

            mEnable4GDataPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    handleEnable4GDataClick(preference);
                    return true;
                }
            });
            mPreferenceCategory.addPreference(mEnable4GDataPreference);
        }

        private boolean isLteCardReady() {
            boolean result = false;
            boolean airPlaneMode = TelephonyUtilsEx.isAirPlaneMode();
            boolean callStateIdle = isCallStateIDLE();
            boolean isCdma4GCard = TelephonyUtilsEx.isCdma4gCard(mPhone.getSubId());

            result = isCdma4GCard && !airPlaneMode && callStateIdle;
            Log.d(TAG, "isLteCardReady: " + result + ";isCdma4GCard:" + isCdma4GCard);
            return result;
        }

        private boolean isCallStateIDLE() {
            boolean result = false;
            TelephonyManager telephonyManager =
                    (TelephonyManager) mActivity.getSystemService(Context.TELEPHONY_SERVICE);
            int currPhoneCallState = telephonyManager.getCallState();

            result = (currPhoneCallState == TelephonyManager.CALL_STATE_IDLE);

            Log.d(TAG, "isCallStateIDLE: " + result);

            return result;
        }

        private void handleEnable4GDataClick(Preference preference) {
            SwitchPreference switchPre = (SwitchPreference) preference;
            boolean isChecked = switchPre.isChecked();
            int modemNetworkMode = isChecked ?
                    Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA : Phone.NT_MODE_GLOBAL;

            Log.d(TAG, "handleEnable4GDataClick isChecked = " + isChecked);

            Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE
                            + mPhone.getSubId(), modemNetworkMode);

            mPhone.setPreferredNetworkType(modemNetworkMode,
                    mEnable4GHandler.obtainMessage(
                            mEnable4GHandler.MESSAGE_SET_ENABLE_4G_NETWORK_TYPE));
            /// M:[CT VOLTE]
            CdmaVolteServiceChecker.getInstance(mPhone.getContext()).onEnable4gStateChanged();
        }

        private final int preferredNetworkMode = Phone.PREFERRED_NT_MODE;

        private class Enable4GHandler extends Handler {

            static final int MESSAGE_SET_ENABLE_4G_NETWORK_TYPE = 0;

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_SET_ENABLE_4G_NETWORK_TYPE:
                        handleSetEnable4GNetworkTypeResponse(msg);
                        break;
                    default:
                        return;
                }
            }

            private void handleSetEnable4GNetworkTypeResponse(Message msg) {
                AsyncResult ar = (AsyncResult) msg.obj;

                if (ar.exception == null) {
                    if (mEnable4GDataPreference != null) {
                        boolean isChecked = mEnable4GDataPreference.isChecked();
                        Log.d(TAG, "isChecked = " + isChecked);
                    }
                } else {
                    Log.d(TAG, "handleSetEnable4GNetworkTypeResponse: exception.");
                    updateEnable4GNetworkUIFromDb();
                }
            }
        }

        private void updateEnable4GNetworkUIFromDb() {

            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + mPhone.getSubId(),
                    preferredNetworkMode);

            Log.d(TAG, "updateEnable4GNetworkUIFromDb: settingsNetworkMode = " + settingsNetworkMode);

            boolean isChecked = settingsNetworkMode == Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA;
            mEnable4GDataPreference.setChecked(isChecked);
        }
    }
}
//*/
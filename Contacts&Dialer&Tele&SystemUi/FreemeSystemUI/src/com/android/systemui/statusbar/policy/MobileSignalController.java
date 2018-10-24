/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.provider.Settings.Global;

import android.os.SystemProperties;

import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.android.ims.ImsManager;
import com.android.ims.ImsConnectionStateListener;
import com.android.ims.ImsServiceClass;
import com.android.ims.ImsException;
import com.android.ims.ImsReasonInfo;
import com.android.ims.ImsConfig;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.cdma.EriInfo;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.SignalDrawable;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import com.android.systemui.statusbar.policy.NetworkControllerImpl.Config;
import com.android.systemui.statusbar.policy.NetworkControllerImpl.SubscriptionDefaults;

/// M: Add for Plugin feature. @{
import com.mediatek.systemui.ext.IMobileIconExt;
import com.mediatek.systemui.ext.ISystemUIStatusBarExt;
import com.mediatek.systemui.ext.OpSystemUICustomizationFactoryBase;
/// @}
import com.mediatek.systemui.statusbar.networktype.NetworkTypeUtils;
import com.mediatek.systemui.statusbar.util.FeatureOptions;
import com.mediatek.systemui.statusbar.util.SIMHelper;
import com.mediatek.telephony.MtkTelephonyManagerEx;

import com.mediatek.systemui.statusbar.networktype.NetworkTypeUtils;

import java.io.PrintWriter;
import java.util.BitSet;
import java.util.Objects;

import mediatek.telephony.MtkCarrierConfigManager;



public class MobileSignalController extends SignalController<
        MobileSignalController.MobileState, MobileSignalController.MobileIconGroup> {
    private final TelephonyManager mPhone;
    private final SubscriptionDefaults mDefaults;
    private final String mNetworkNameDefault;
    private final String mNetworkNameSeparator;
    private final ContentObserver mObserver;
    @VisibleForTesting
    final PhoneStateListener mPhoneStateListener;
    // Save entire info for logging, we only use the id.
    /// M: Fix bug ALPS02416794
    /*final*/ SubscriptionInfo mSubscriptionInfo;

    // @VisibleForDemoMode
    final SparseArray<MobileIconGroup> mNetworkToIconLookup;

    // Since some pieces of the phone state are interdependent we store it locally,
    // this could potentially become part of MobileState for simplification/complication
    // of code.
    private int mDataNetType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
    private int mDataState = TelephonyManager.DATA_DISCONNECTED;
    private ServiceState mServiceState;
    private SignalStrength mSignalStrength;
    private MobileIconGroup mDefaultIcons;
    private Config mConfig;

    /// M: for volte icon @{
    private final ImsManager mImsManager;
    private Handler mReceiverHandler;
    /// @}

    /// M: Add for Plugin feature. @ {
    private IMobileIconExt mMobileIconExt;
    private ISystemUIStatusBarExt mStatusBarExt;
    /// @ }

    /// M: Add for roaming icon handling for optr. @ {
    private CarrierConfigManager mCarrierConfigManager;
    /// @ }


    // TODO: Reduce number of vars passed in, if we have the NetworkController, probably don't
    // need listener lists anymore.
    public MobileSignalController(Context context, Config config, boolean hasMobileData,
            TelephonyManager phone, CallbackHandler callbackHandler,
            NetworkControllerImpl networkController, SubscriptionInfo info,
            SubscriptionDefaults defaults, Looper receiverLooper) {
        super("MobileSignalController(" + info.getSubscriptionId() + ")", context,
                NetworkCapabilities.TRANSPORT_CELLULAR, callbackHandler,
                networkController);
        mNetworkToIconLookup = new SparseArray<>();
        mConfig = config;
        mPhone = phone;
        mDefaults = defaults;
        mSubscriptionInfo = info;
        /// M: Init plugin @ {
        mMobileIconExt = OpSystemUICustomizationFactoryBase.getOpFactory(context).makeMobileIcon();
        mStatusBarExt = OpSystemUICustomizationFactoryBase.getOpFactory(context)
                            .makeSystemUIStatusBar(context);
        /// @ }
        mPhoneStateListener = new MobilePhoneStateListener(info.getSubscriptionId(),
                receiverLooper);
        /// M: for volte icon @{
        mImsManager = ImsManager.getInstance(context,
                SubscriptionManager.getPhoneId(info.getSubscriptionId()));
        mReceiverHandler = new Handler(receiverLooper);
        /// @}

        mNetworkNameSeparator = getStringIfExists(R.string.status_bar_network_name_separator);
        mNetworkNameDefault = getStringIfExists(
                com.android.internal.R.string.lockscreen_carrier_default);

        mapIconSets();

        String networkName = info.getCarrierName() != null ? info.getCarrierName().toString()
                : mNetworkNameDefault;
        mLastState.networkName = mCurrentState.networkName = networkName;
        mLastState.networkNameData = mCurrentState.networkNameData = networkName;
        mLastState.enabled = mCurrentState.enabled = hasMobileData;
        mLastState.iconGroup = mCurrentState.iconGroup = mDefaultIcons;
        mCarrierConfigManager = (CarrierConfigManager) context
                          .getSystemService(Context.CARRIER_CONFIG_SERVICE);

        // Get initial data sim state.
        updateDataSim();
        mObserver = new ContentObserver(new Handler(receiverLooper)) {
            @Override
            public void onChange(boolean selfChange) {
                updateTelephony();
            }
        };
    }

    ///M: for volte icon @{
    /**
     * Listen to the IMS service state change
     *
     */
    @VisibleForTesting
    ImsConnectionStateListener mImsConnectionStateListener =
        new ImsConnectionStateListener() {
        @Override
        public void onImsConnected(int imsRadioTech) {
            mReceiverHandler.post(new Runnable(){
                @Override
                public void run() {
                    if (DEBUG) {
                        Log.d(mTag,"onImsConnected STATE_IN_SERVICE");
                    }
                    mCurrentState.imsRegState = ServiceState.STATE_IN_SERVICE;
                    updateTelephony();
                }
            });
        }

        @Override
        public void onImsDisconnected(ImsReasonInfo imsReasonInfo) {
            mReceiverHandler.post(new Runnable(){
                @Override
                public void run() {
                    if (DEBUG) {
                        Log.d(mTag,"onImsConnected STATE_OUT_OF_SERVICE");
                    }
                    mCurrentState.imsRegState = ServiceState.STATE_OUT_OF_SERVICE;
                    updateTelephony();
                }
            });
        }

        @Override
        public void onFeatureCapabilityChanged(int serviceClass,
                int[] enabledFeatures, int[] disabledFeatures) {
            final int[] enabledFs = enabledFeatures;
            if (serviceClass == ImsServiceClass.MMTEL) {
                mReceiverHandler.post(new Runnable(){
                    @Override
                    public void run() {
                        if (DEBUG) {
                            Log.d(mTag,"onFeatureCapabilityChanged MMTEL");
                        }
                        mCurrentState.imsCap = getImsEnableCap(enabledFs);
                        updateTelephony();
                    }
                });
            }
        }
    };

    private int getImsEnableCap(int[] enabledFeatures) {
        int cap = ImsConfig.FeatureConstants.FEATURE_TYPE_UNKNOWN;
        if (enabledFeatures != null) {
            if (enabledFeatures[
                   ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_WIFI]
                   == ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_WIFI) {
                cap = ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_WIFI;
            } else if (enabledFeatures[
                  ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_LTE]
                  == ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_LTE
                  ) {
                cap = ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_LTE;
            }
        }
        return cap;
    }

    private int getVolteIcon() {
        int icon = 0;
        if (isImsOverWfc()) {
            boolean needShowWfcSysIcon = mStatusBarExt.needShowWfcIcon();
            if (needShowWfcSysIcon) {
                icon = NetworkTypeUtils.WFC_ICON;
            }
        } else if (isImsOverVoice() && isLteNetWork()) {
            if (mCurrentState.imsRegState == ServiceState.STATE_IN_SERVICE) {
                icon = NetworkTypeUtils.VOLTE_ICON;
            } else if(FeatureOptions.MTK_CT_MIXED_VOLTE_SUPPORT &&
                    SIMHelper.isSecondaryCSIMForMixedVolte(mSubscriptionInfo.getSubscriptionId()) &&
                    mCurrentState.imsRegState == ServiceState.STATE_OUT_OF_SERVICE) {
                if (DEBUG) {
                    Log.d(mTag, "set dis volte icon");
                }
                icon = NetworkTypeUtils.VOLTE_DIS_ICON;
            }
        }
        /// M: add for disconnected volte feature. @{
        mStatusBarExt.setImsRegInfo(mSubscriptionInfo.getSubscriptionId(),
                mCurrentState.imsRegState, isImsOverWfc(), isImsOverVoice());
        /// @}
        return icon;
    }


    public boolean isImsOverWfc() {
        return mCurrentState.imsCap == ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_WIFI;
    }

    private boolean isImsOverVoice() {
        return mCurrentState.imsCap == ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_LTE;
    }

    public boolean isLteNetWork() {
        return (mDataNetType == TelephonyManager.NETWORK_TYPE_LTE
            || mDataNetType == TelephonyManager.NETWORK_TYPE_LTE_CA);
    }
    ///@}

    public void setConfiguration(Config config) {
        mConfig = config;
        mapIconSets();
        updateTelephony();
    }

    public int getDataContentDescription() {
        return getIcons().mDataContentDescription;
    }

    public void setAirplaneMode(boolean airplaneMode) {
        mCurrentState.airplaneMode = airplaneMode;
        notifyListenersIfNecessary();
    }

    public void setUserSetupComplete(boolean userSetup) {
        mCurrentState.userSetup = userSetup;
        notifyListenersIfNecessary();
    }

    @Override
    public void updateConnectivity(BitSet connectedTransports, BitSet validatedTransports) {
        boolean isValidated = validatedTransports.get(mTransportType);
        mCurrentState.isDefault = connectedTransports.get(mTransportType) &&
                // M: Add one more condition to judge whether the cellular connection is this subid
                mNetworkController.isCellularConnected(mSubscriptionInfo.getSubscriptionId());
        // Only show this as not having connectivity if we are default.
        mCurrentState.inetCondition = (isValidated || !mCurrentState.isDefault) ? 1 : 0;
        notifyListenersIfNecessary();
    }

    public void setCarrierNetworkChangeMode(boolean carrierNetworkChangeMode) {
        mCurrentState.carrierNetworkChangeMode = carrierNetworkChangeMode;
        updateTelephony();
    }

    /**
     * Start listening for phone state changes.
     */
    public void registerListener() {
        mPhone.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_SERVICE_STATE
                        | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                        | PhoneStateListener.LISTEN_CALL_STATE
                        | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                        | PhoneStateListener.LISTEN_DATA_ACTIVITY
                        | PhoneStateListener.LISTEN_CARRIER_NETWORK_CHANGE);
        ///M:for volte icon. @{
        try {
            mImsManager.addRegistrationListener(ImsServiceClass.MMTEL, mImsConnectionStateListener);
        }catch (ImsException e) {
            // Could not get the ImsService.
            Log.w(mTag,"could not get the ImsService!");
        }
        ///@}
        mContext.getContentResolver().registerContentObserver(Global.getUriFor(Global.MOBILE_DATA),
                true, mObserver);
        mContext.getContentResolver().registerContentObserver(Global.getUriFor(
                Global.MOBILE_DATA + mSubscriptionInfo.getSubscriptionId()),
                true, mObserver);
        mStatusBarExt.registerOpStateListener();
    }

    /**
     * Stop listening for phone state changes.
     */
    public void unregisterListener() {
        mPhone.listen(mPhoneStateListener, 0);
        mContext.getContentResolver().unregisterContentObserver(mObserver);
        ///M:for volte icon. @{
        try {
            mImsManager.removeRegistrationListener(mImsConnectionStateListener);
        }catch (ImsException e) {
            // Could not remove ImsService.
            Log.w(mTag,"could not remove ImsService!");
        }
        ///@}
    }

    /**
     * Produce a mapping of data network types to icon groups for simple and quick use in
     * updateTelephony.
     */
    private void mapIconSets() {
        mNetworkToIconLookup.clear();

        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyIcons.THREE_G);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyIcons.THREE_G);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyIcons.THREE_G);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyIcons.THREE_G);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_UMTS, TelephonyIcons.THREE_G);

        if (!mConfig.showAtLeast3G) {
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_UNKNOWN,
                    TelephonyIcons.UNKNOWN);
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_EDGE, TelephonyIcons.E);
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_CDMA, TelephonyIcons.ONE_X);
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyIcons.ONE_X);

            mDefaultIcons = TelephonyIcons.G;
        } else {
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_UNKNOWN,
                    TelephonyIcons.THREE_G);
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_EDGE,
                    TelephonyIcons.THREE_G);
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_CDMA,
                    TelephonyIcons.THREE_G);
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_1xRTT,
                    TelephonyIcons.THREE_G);
            mDefaultIcons = TelephonyIcons.THREE_G;
        }

        MobileIconGroup hGroup = TelephonyIcons.THREE_G;
        if (mConfig.hspaDataDistinguishable) {
            hGroup = TelephonyIcons.H;
        }
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_HSDPA, hGroup);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_HSUPA, hGroup);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_HSPA, hGroup);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_HSPAP, hGroup);

        if (mConfig.show4gForLte) {
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_LTE, TelephonyIcons.FOUR_G);
            if (mConfig.hideLtePlus) {
                mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_LTE_CA,
                        TelephonyIcons.FOUR_G);
            } else {
                mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_LTE_CA,
                        TelephonyIcons.FOUR_G_PLUS);
            }
        } else {
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_LTE, TelephonyIcons.LTE);
            if (mConfig.hideLtePlus) {
                mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_LTE_CA,
                        TelephonyIcons.LTE);
            } else {
                mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_LTE_CA,
                        TelephonyIcons.LTE_PLUS);
            }
        }
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_IWLAN, TelephonyIcons.WFC);
    }

    private int getNumLevels() {
        if (mConfig.inflateSignalStrengths) {
            return SignalStrength.NUM_SIGNAL_STRENGTH_BINS + 1;
        }
        return SignalStrength.NUM_SIGNAL_STRENGTH_BINS;
    }

    @Override
    public int getCurrentIconId() {
        if (mCurrentState.iconGroup == TelephonyIcons.CARRIER_NETWORK_CHANGE) {
            return SignalDrawable.getCarrierChangeState(getNumLevels());
        } else if (mCurrentState.connected) {
            int level = mCurrentState.level;
            if (mConfig.inflateSignalStrengths) {
                level++;
            }
            return SignalDrawable.getState(level, getNumLevels(),
                    mCurrentState.inetCondition == 0);
        } else if (mCurrentState.enabled) {
            return SignalDrawable.getEmptyState(getNumLevels());
        } else {
            return 0;
        }
    }

    @Override
    public int getQsCurrentIconId() {
        if (mCurrentState.airplaneMode) {
            return SignalDrawable.getAirplaneModeState(getNumLevels());
        }

        return getCurrentIconId();
    }

    @Override
    public void notifyListeners(SignalCallback callback) {
        MobileIconGroup icons = getIcons();

        String contentDescription = getStringIfExists(getContentDescription());
        String dataContentDescription = getStringIfExists(icons.mDataContentDescription);
        final boolean dataDisabled = mCurrentState.iconGroup == TelephonyIcons.DATA_DISABLED
                && mCurrentState.userSetup;

        /// M: Customize the signal strength icon id. @ {
        int iconId = getCurrentIconId();
        iconId = mStatusBarExt.getCustomizeSignalStrengthIcon(
                    mSubscriptionInfo.getSubscriptionId(),
                    iconId,
                    mSignalStrength,
                    mDataNetType,
                    mServiceState);
        /// @ }

        // Show icon in QS when we are connected or data is disabled.
        boolean showDataIcon = mCurrentState.dataConnected || dataDisabled;
        IconState statusIcon = new IconState(mCurrentState.enabled && !mCurrentState.airplaneMode,
                iconId, contentDescription);

        int qsTypeIcon = 0;
        IconState qsIcon = null;
        String description = null;
        // Only send data sim callbacks to QS.
        if (mCurrentState.dataSim) {
            qsTypeIcon = showDataIcon ? icons.mQsDataType : 0;
            qsIcon = new IconState(mCurrentState.enabled
                    && !mCurrentState.isEmergency, getQsCurrentIconId(), contentDescription);
            description = mCurrentState.isEmergency ? null : mCurrentState.networkName;
        }
        boolean activityIn = mCurrentState.dataConnected
                && !mCurrentState.carrierNetworkChangeMode
                && mCurrentState.activityIn;
        boolean activityOut = mCurrentState.dataConnected
                && !mCurrentState.carrierNetworkChangeMode
                && mCurrentState.activityOut;
        showDataIcon &= mCurrentState.isDefault || dataDisabled;
        int typeIcon = showDataIcon ? icons.mDataType : 0;

        /// M: Add for lwa.
        typeIcon = mCurrentState.lwaRegState == NetworkTypeUtils.LWA_STATE_CONNCTED
                && showDataIcon ? NetworkTypeUtils.LWA_ICON : typeIcon;
        /** M: Support [Network Type on StatusBar], change the implement methods.
          * Get the network icon base on service state.
          * Add one more parameter for network type.
          * @ { **/
        int networkIcon = mCurrentState.networkIcon;
        /// M: Support volte icon.Bug fix when airplane mode is on go to hide volte icon
        int volteIcon = mCurrentState.airplaneMode && !isImsOverWfc()
                ? 0 : mCurrentState.volteIcon;

        /// M: when data disabled, common show data icon as x, but op do not need show it @ {
        mStatusBarExt.isDataDisabled(mSubscriptionInfo.getSubscriptionId(), dataDisabled);
        /// @ }

        /// M: Customize the data type icon id. @ {
        typeIcon = mStatusBarExt.getDataTypeIcon(
                        mSubscriptionInfo.getSubscriptionId(),
                        typeIcon,
                        mDataNetType,
                        mCurrentState.dataConnected ? TelephonyManager.DATA_CONNECTED :
                            TelephonyManager.DATA_DISCONNECTED,
                        mServiceState);
        /// @ }
        /// M: Customize the network type icon id. @ {
        networkIcon = mStatusBarExt.getNetworkTypeIcon(
                        mSubscriptionInfo.getSubscriptionId(),
                        networkIcon,
                        mDataNetType,
                        mServiceState);
        /// @ }

        callback.setMobileDataIndicators(statusIcon, qsIcon, typeIcon, networkIcon, volteIcon,
                qsTypeIcon,activityIn, activityOut, dataContentDescription, description,
                 icons.mIsWide, mSubscriptionInfo.getSubscriptionId(), mCurrentState.roaming);

        /// M: update plmn label @{
        mNetworkController.refreshPlmnCarrierLabel();
        /// @}
    }

    @Override
    protected MobileState cleanState() {
        return new MobileState();
    }

    private boolean hasService() {
        if (mServiceState != null) {
            // Consider the device to be in service if either voice or data
            // service is available. Some SIM cards are marketed as data-only
            // and do not support voice service, and on these SIM cards, we
            // want to show signal bars for data service as well as the "no
            // service" or "emergency calls only" text that indicates that voice
            // is not available.
            switch (mServiceState.getVoiceRegState()) {
                case ServiceState.STATE_POWER_OFF:
                    return false;
                case ServiceState.STATE_OUT_OF_SERVICE:
                case ServiceState.STATE_EMERGENCY_ONLY:
                    return mServiceState.getDataRegState() == ServiceState.STATE_IN_SERVICE;
                default:
                    return true;
            }
        } else {
            return false;
        }
    }

    private boolean isCdma() {
        return (mSignalStrength != null) && !mSignalStrength.isGsm();
    }

    public boolean isEmergencyOnly() {
        return (mServiceState != null && mServiceState.isEmergencyOnly());
    }

    private boolean isRoaming() {
        // During a carrier change, roaming indications need to be supressed.
        if (isCarrierNetworkChangeActive()) {
            return false;
        }
        if (isCdma() && mServiceState != null) {
            final int iconMode = mServiceState.getCdmaEriIconMode();
            return mServiceState.getCdmaEriIconIndex() != EriInfo.ROAMING_INDICATOR_OFF
                    && (iconMode == EriInfo.ROAMING_ICON_MODE_NORMAL
                    || iconMode == EriInfo.ROAMING_ICON_MODE_FLASH);
        } else {
            boolean isInRoaming =  mServiceState != null && mServiceState.getRoaming();
            if (isInRoaming) {
                PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(
                        mSubscriptionInfo.getSubscriptionId());
                if (carrierConfig != null) {
                    isInRoaming = carrierConfig.getBoolean(
                        MtkCarrierConfigManager.MTK_KEY_CARRIER_NEED_SHOW_ROAMING_ICON);
                }
            }
            return isInRoaming;
        }
    }

    private boolean isCarrierNetworkChangeActive() {
        return mCurrentState.carrierNetworkChangeMode;
    }

    public void handleBroadcast(Intent intent) {
        String action = intent.getAction();
        if (action.equals(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION)) {
            updateNetworkName(intent.getBooleanExtra(TelephonyIntents.EXTRA_SHOW_SPN, false),
                    intent.getStringExtra(TelephonyIntents.EXTRA_SPN),
                    intent.getStringExtra(TelephonyIntents.EXTRA_DATA_SPN),
                    intent.getBooleanExtra(TelephonyIntents.EXTRA_SHOW_PLMN, false),
                    intent.getStringExtra(TelephonyIntents.EXTRA_PLMN));
            notifyListenersIfNecessary();
        } else if (action.equals(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
            updateDataSim();
            notifyListenersIfNecessary();
        } else if (action.equals(NetworkTypeUtils.LWA_STATE_CHANGE_ACTION)) {
            /// M: Add for 4G+W @{
            handleLwaAction(intent);
            notifyListenersIfNecessary();
            /// @}
        }
    }

    /// M: Add for 4G+W @{
    private void handleLwaAction(Intent intent){
        mCurrentState.lwaRegState = intent.getIntExtra(
                NetworkTypeUtils.EXTRA_STATE, NetworkTypeUtils.LWA_STATE_UNKNOWN);
    }
    /// @}

    private void updateDataSim() {
        int defaultDataSub = mDefaults.getDefaultDataSubId();
        if (SubscriptionManager.isValidSubscriptionId(defaultDataSub)) {
            mCurrentState.dataSim = defaultDataSub == mSubscriptionInfo.getSubscriptionId();
        } else {
            // There doesn't seem to be a data sim selected, however if
            // there isn't a MobileSignalController with dataSim set, then
            // QS won't get any callbacks and will be blank.  Instead
            // lets just assume we are the data sim (which will basically
            // show one at random) in QS until one is selected.  The user
            // should pick one soon after, so we shouldn't be in this state
            // for long.
            mCurrentState.dataSim = true;
        }
    }

    /**
     * Updates the network's name based on incoming spn and plmn.
     */
    void updateNetworkName(boolean showSpn, String spn, String dataSpn,
            boolean showPlmn, String plmn) {
        if (CHATTY) {
            Log.d("CarrierLabel", "updateNetworkName showSpn=" + showSpn
                    + " spn=" + spn + " dataSpn=" + dataSpn
                    + " showPlmn=" + showPlmn + " plmn=" + plmn);
        }
        StringBuilder str = new StringBuilder();
        StringBuilder strData = new StringBuilder();
        if (showPlmn && plmn != null) {
            str.append(plmn);
            strData.append(plmn);
        }
        if (showSpn && spn != null) {
            if (str.length() != 0) {
                str.append(mNetworkNameSeparator);
            }
            str.append(spn);
        }
        if (str.length() != 0) {
            mCurrentState.networkName = str.toString();
        } else {
            mCurrentState.networkName = mNetworkNameDefault;
        }
        if (showSpn && dataSpn != null) {
            if (strData.length() != 0) {
                strData.append(mNetworkNameSeparator);
            }
            strData.append(dataSpn);
        }

        // M: ALPS02744648 for C2K, there isn't dataspn parameter, when no plmn
        // and no dataspn, show spn instead "no service" here @{
        if (strData.length() == 0 && showSpn && spn != null) {
            Log.d("CarrierLabel", "show spn instead 'no service' here: " + spn);
            strData.append(spn);
        }
        // @}

        if (strData.length() != 0) {
            mCurrentState.networkNameData = strData.toString();
        } else {
            mCurrentState.networkNameData = mNetworkNameDefault;
        }
    }

    /**
     * Updates the current state based on mServiceState, mSignalStrength, mDataNetType,
     * mDataState, and mSimState.  It should be called any time one of these is updated.
     * This will call listeners if necessary.
     */
    private final void updateTelephony() {
        if (DEBUG && FeatureOptions.LOG_ENABLE) {
            Log.d(mTag, "updateTelephonySignalStrength: hasService=" + hasService()
                    + " ss=" + mSignalStrength);
        }
        mCurrentState.connected = hasService() && mSignalStrength != null;
        handleIWLANNetwork();
        if (mCurrentState.connected) {
            if (!mSignalStrength.isGsm() && mConfig.alwaysShowCdmaRssi) {
                mCurrentState.level = mSignalStrength.getCdmaLevel();
            } else {
                mCurrentState.level = mSignalStrength.getLevel();
            }
            /// M: Customize the signal strength level. @ {
            mCurrentState.level = mStatusBarExt.getCustomizeSignalStrengthLevel(
                    mCurrentState.level, mSignalStrength, mServiceState);
            /// @ }
        }
        if (mNetworkToIconLookup.indexOfKey(mDataNetType) >= 0) {
            mCurrentState.iconGroup = mNetworkToIconLookup.get(mDataNetType);
        } else {
            mCurrentState.iconGroup = mDefaultIcons;
        }
        /// M: Add for data network type.
        mCurrentState.dataNetType = mDataNetType;
        mCurrentState.dataConnected = mCurrentState.connected
                && mDataState == TelephonyManager.DATA_CONNECTED;
        /// M: Add for op network tower type.
        mCurrentState.customizedState = mStatusBarExt.getCustomizeCsState(mServiceState,
                mCurrentState.customizedState);
        /// M: Add for op signal strength tower icon.
        mCurrentState.customizedSignalStrengthIcon = mStatusBarExt.getCustomizeSignalStrengthIcon(
                mSubscriptionInfo.getSubscriptionId(),
                mCurrentState.customizedSignalStrengthIcon,
                mSignalStrength,
                mDataNetType,
                mServiceState);

        mCurrentState.roaming = isRoaming();
        if (isCarrierNetworkChangeActive()) {
            mCurrentState.iconGroup = TelephonyIcons.CARRIER_NETWORK_CHANGE;
        } else if (isDataDisabled()) {
            mCurrentState.iconGroup = TelephonyIcons.DATA_DISABLED;
        }
        if (isEmergencyOnly() != mCurrentState.isEmergency) {
            mCurrentState.isEmergency = isEmergencyOnly();
            mNetworkController.recalculateEmergency();
        }
        // Fill in the network name if we think we have it.
        if (mCurrentState.networkName == mNetworkNameDefault && mServiceState != null
                && !TextUtils.isEmpty(mServiceState.getOperatorAlphaShort())) {
            mCurrentState.networkName = mServiceState.getOperatorAlphaShort();
        }
        /// M: For network type big icon.
        mCurrentState.networkIcon =
            NetworkTypeUtils.getNetworkTypeIcon(mServiceState, mConfig, hasService());
        /// M: For volte type icon.
        mCurrentState.volteIcon = getVolteIcon();

        notifyListenersIfNecessary();
    }

    private boolean isDataDisabled() {
        return !mPhone.getDataEnabled(mSubscriptionInfo.getSubscriptionId());
    }

    /// M: bug fix for ALPS02603527.
    /** IWLAN is special case in which the transmission via WIFI, no need cellular network, then
    whenever PS type is IWLAN, cellular network is not connected. However, in special case, CS may
    still connect under IWLAN with valid network type.
    **/
     private void handleIWLANNetwork() {
        /// M: fix ALPS02742814
        if (mCurrentState.connected && mServiceState != null &&
            mServiceState.getDataNetworkType() == TelephonyManager.NETWORK_TYPE_IWLAN &&
            mServiceState.getVoiceNetworkType() == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            Log.d(mTag,"Current is IWLAN network only, no cellular network available");
            mCurrentState.connected = false;
        }
    }
    @VisibleForTesting
    void setActivity(int activity) {
        mCurrentState.activityIn = activity == TelephonyManager.DATA_ACTIVITY_INOUT
                || activity == TelephonyManager.DATA_ACTIVITY_IN;
        mCurrentState.activityOut = activity == TelephonyManager.DATA_ACTIVITY_INOUT
                || activity == TelephonyManager.DATA_ACTIVITY_OUT;
        notifyListenersIfNecessary();
    }

    @Override
    public void dump(PrintWriter pw) {
        super.dump(pw);
        pw.println("  mSubscription=" + mSubscriptionInfo + ",");
        pw.println("  mServiceState=" + mServiceState + ",");
        pw.println("  mSignalStrength=" + mSignalStrength + ",");
        pw.println("  mDataState=" + mDataState + ",");
        pw.println("  mDataNetType=" + mDataNetType + ",");
    }

    class MobilePhoneStateListener extends PhoneStateListener {
        public MobilePhoneStateListener(int subId, Looper looper) {
            super(subId, looper);
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            if (DEBUG) {
                Log.d(mTag, "onSignalStrengthsChanged signalStrength=" + signalStrength +
                        ((signalStrength == null) ? "" : (" level=" + signalStrength.getLevel())));
            }
            mSignalStrength = signalStrength;
            updateTelephony();
        }

        @Override
        public void onServiceStateChanged(ServiceState state) {
            if (DEBUG) {
                Log.d(mTag, "onServiceStateChanged voiceState=" + ((state == null) ? "" :
                        state.getVoiceRegState() + " dataState=" + state.getDataRegState()));
            }
            mServiceState = state;
            if (state != null) {
                mDataNetType = state.getDataNetworkType();
                if (mDataNetType == TelephonyManager.NETWORK_TYPE_LTE && mServiceState != null &&
                        mServiceState.isUsingCarrierAggregation()) {
                    mDataNetType = TelephonyManager.NETWORK_TYPE_LTE_CA;
                }
            }
            updateTelephony();
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            if (DEBUG) {
                Log.d(mTag, "onDataConnectionStateChanged: state=" + state
                        + " type=" + networkType);
            }
            mDataState = state;
            mDataNetType = networkType;
            if (mDataNetType == TelephonyManager.NETWORK_TYPE_LTE && mServiceState != null &&
                    mServiceState.isUsingCarrierAggregation()) {
                mDataNetType = TelephonyManager.NETWORK_TYPE_LTE_CA;
            }
            updateTelephony();
        }

        @Override
        public void onDataActivity(int direction) {
            if (DEBUG && FeatureOptions.LOG_ENABLE) {
                Log.d(mTag, "onDataActivity: direction=" + direction);
            }
            setActivity(direction);
        }

        @Override
        public void onCarrierNetworkChange(boolean active) {
            if (DEBUG && FeatureOptions.LOG_ENABLE) {
                Log.d(mTag, "onCarrierNetworkChange: active=" + active);
            }
            mCurrentState.carrierNetworkChangeMode = active;

            updateTelephony();
        }

        /// M: Add for Plugin feature. @{
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            boolean needHandle = mStatusBarExt.handleCallStateChanged(
                    mSubscriptionInfo.getSubscriptionId(), state, incomingNumber, mServiceState);
            if (needHandle) {
                updateTelephony();
            }
        }
        /// @}
    };

    static class MobileIconGroup extends SignalController.IconGroup {
        final int mDataContentDescription; // mContentDescriptionDataType
        final int mDataType;
        final boolean mIsWide;
        final int mQsDataType;

        public MobileIconGroup(String name, int[][] sbIcons, int[][] qsIcons, int[] contentDesc,
                int sbNullState, int qsNullState, int sbDiscState, int qsDiscState,
                int discContentDesc, int dataContentDesc, int dataType, boolean isWide,
                int qsDataType) {
            super(name, sbIcons, qsIcons, contentDesc, sbNullState, qsNullState, sbDiscState,
                    qsDiscState, discContentDesc);
            mDataContentDescription = dataContentDesc;
            mDataType = dataType;
            mIsWide = isWide;
            mQsDataType = qsDataType;
        }
    }

    static class MobileState extends SignalController.State {
        String networkName;
        String networkNameData;
        boolean dataSim;
        boolean dataConnected;
        boolean isEmergency;
        boolean airplaneMode;
        boolean carrierNetworkChangeMode;
        boolean isDefault;
        boolean userSetup;
        boolean roaming;

        /// M: Add for 4G+W
        int lwaRegState = NetworkTypeUtils.LWA_STATE_UNKNOWN;
        /// M: For network type big icon.
        int networkIcon;
        /// M: Add for data network type.
        int dataNetType;
        /// M: Add for op network tower type.
        int customizedState;
        /// M: Add for op signal strength tower icon.
        int customizedSignalStrengthIcon;
        /// M: Add for volte @{
        int imsRegState = ServiceState.STATE_POWER_OFF;
        int imsCap;
        int volteIcon;
        /// @}
        @Override
        public void copyFrom(State s) {
            super.copyFrom(s);
            MobileState state = (MobileState) s;
            dataSim = state.dataSim;
            networkName = state.networkName;
            networkNameData = state.networkNameData;
            dataConnected = state.dataConnected;
            isDefault = state.isDefault;
            isEmergency = state.isEmergency;
            airplaneMode = state.airplaneMode;
            carrierNetworkChangeMode = state.carrierNetworkChangeMode;
            userSetup = state.userSetup;
            /// M: For network type big icon.
            networkIcon = state.networkIcon;
            /// M: Add for data network type.
            dataNetType = state.dataNetType;
            /// M: Add for op network tower type.
            customizedState = state.customizedState;
            /// M: Add for op signal strength tower icon.
            customizedSignalStrengthIcon = state.customizedSignalStrengthIcon;
            /// M: Add for volte
            imsRegState = state.imsRegState;
            imsCap = state.imsCap;
            volteIcon = state.volteIcon;
            roaming = state.roaming;
            /// M: Add for 4G+W
            lwaRegState = state.lwaRegState;
        }

        @Override
        protected void toString(StringBuilder builder) {
            super.toString(builder);
            builder.append(',');
            builder.append("dataSim=").append(dataSim).append(',');
            builder.append("networkName=").append(networkName).append(',');
            builder.append("networkNameData=").append(networkNameData).append(',');
            builder.append("dataConnected=").append(dataConnected).append(',');
            builder.append("roaming=").append(roaming).append(',');
            builder.append("isDefault=").append(isDefault).append(',');
            builder.append("isEmergency=").append(isEmergency).append(',');
            builder.append("airplaneMode=").append(airplaneMode).append(',');
            /// M: Add for 4G+W
            builder.append("lwaRegState=").append(lwaRegState).append(',');
            builder.append("carrierNetworkChangeMode=").append(carrierNetworkChangeMode)
                    .append(',');
            builder.append("userSetup=").append(userSetup);
            /// M: For network type big icon.
            builder.append("networkIcon").append(networkIcon).append(',');
            /// M: Add for data network type.
            builder.append("dataNetType").append(dataNetType).append(',');
            /// M: Add for op network tower type.
            builder.append("customizedState").append(customizedState).append(',');
            /// M: Add for op signal strength tower icon.
            builder.append("customizedSignalStrengthIcon").append(customizedSignalStrengthIcon)
                    .append(',');
            /// M: Add for volte.
            builder.append("imsRegState=").append(imsRegState).append(',');
            builder.append("imsCap=").append(imsCap).append(',');
            builder.append("volteIconId=").append(volteIcon).append(',');
            builder.append("carrierNetworkChangeMode=").append(carrierNetworkChangeMode);
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o)
                    && Objects.equals(((MobileState) o).networkName, networkName)
                    && Objects.equals(((MobileState) o).networkNameData, networkNameData)
                    && ((MobileState) o).dataSim == dataSim
                    && ((MobileState) o).dataConnected == dataConnected
                    && ((MobileState) o).isEmergency == isEmergency
                    && ((MobileState) o).airplaneMode == airplaneMode
                    && ((MobileState) o).carrierNetworkChangeMode == carrierNetworkChangeMode
                    /// M: Add for 4G+W.
                    && ((MobileState) o).lwaRegState == lwaRegState
                    /// M: For network type big icon.
                    && ((MobileState) o).networkIcon == networkIcon
                    && ((MobileState) o).volteIcon == volteIcon
                    /// M: Add for data network type.
                    && ((MobileState) o).dataNetType == dataNetType
                    /// M: Add for op network tower type.
                    && ((MobileState) o).customizedState == customizedState
                    /// M: Add for op signal strength tower icon.
                    && ((MobileState) o).customizedSignalStrengthIcon ==
                                             customizedSignalStrengthIcon
                    && ((MobileState) o).userSetup == userSetup
                    && ((MobileState) o).isDefault == isDefault
                    && ((MobileState) o).roaming == roaming;
        }
    }

    /// M: Support for PLMN. @{
    public SubscriptionInfo getControllerSubInfo() {
        return mSubscriptionInfo;
    }

    public boolean getControllserHasService() {
        return hasService();
    }
    /// M: Support for PLMN. @}
}

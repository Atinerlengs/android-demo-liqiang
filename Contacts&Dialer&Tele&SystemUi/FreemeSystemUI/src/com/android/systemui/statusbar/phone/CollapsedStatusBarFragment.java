/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.statusbar.phone;

import static android.app.StatusBarManager.DISABLE_NOTIFICATION_ICONS;
import static android.app.StatusBarManager.DISABLE_SYSTEM_INFO;

import static com.android.systemui.statusbar.phone.StatusBar.reinflateSignalCluster;

import android.annotation.Nullable;
import android.app.Fragment;
import android.app.StatusBarManager;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.LinearLayout;

import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.SignalClusterView;
import com.android.systemui.statusbar.phone.StatusBarIconController.DarkIconManager;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.EncryptionHelper;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import com.freeme.provider.FreemeSettings;
import com.freeme.systemui.utils.NotchUtils;
import com.freeme.systemui.utils.SuperPowerUtils;

/**
 * Contains the collapsed status bar and handles hiding/showing based on disable flags
 * and keyguard state. Also manages lifecycle to make sure the views it contains are being
 * updated by the StatusBarIconController and DarkIconManager while it is attached.
 */
public class CollapsedStatusBarFragment extends Fragment implements CommandQueue.Callbacks {

    public static final String TAG = "CollapsedStatusBarFragment";
    private static final String EXTRA_PANEL_STATE = "panel_state";
    private PhoneStatusBarView mStatusBar;
    private KeyguardMonitor mKeyguardMonitor;
    private NetworkController mNetworkController;
    private LinearLayout mSystemIconArea;
    private View mNotificationIconAreaInner;
    private int mDisabled1;
    private StatusBar mStatusBarComponent;
    private DarkIconManager mDarkIconManager;
    private SignalClusterView mSignalClusterView;

    private SignalCallback mSignalCallback = new SignalCallback() {
        @Override
        public void setIsAirplaneMode(NetworkController.IconState icon) {
            mStatusBarComponent.recomputeDisableFlags(true /* animate */);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mKeyguardMonitor = Dependency.get(KeyguardMonitor.class);
        mNetworkController = Dependency.get(NetworkController.class);
        mStatusBarComponent = SysUiServiceProvider.getComponent(getContext(), StatusBar.class);
        //*/ freeme.gouzhouping, 20170908, show notification icon switch.
        getContext().getContentResolver().registerContentObserver(Settings.System.getUriFor(SHOW_NOTIFICATIONS_ICON),
                false, mNotificationObserver);
        //*/

        //*/ freeme.gouzhouping, 20180628. super power.
        mSuperPowerModeChanged = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                updateShowNotifications();
                refreshQsState();
            }
        };

        getContext().getContentResolver().registerContentObserver(SuperPowerUtils.getSuperPowerModeUri(),
                true, mSuperPowerModeChanged);
        //*/
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            Bundle savedInstanceState) {
        //*/ freeme.gouzhouping, 20180716. Notch.
        if (NotchUtils.hasNotch()) {
            return inflater.inflate(R.layout.freeme_notch_status_bar, container, false);
        }
        //*/
        return inflater.inflate(R.layout.status_bar, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mStatusBar = (PhoneStatusBarView) view;
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_PANEL_STATE)) {
            mStatusBar.go(savedInstanceState.getInt(EXTRA_PANEL_STATE));
        }
        mDarkIconManager = new DarkIconManager(view.findViewById(R.id.statusIcons));
        Dependency.get(StatusBarIconController.class).addIconGroup(mDarkIconManager);
        mSystemIconArea = mStatusBar.findViewById(R.id.system_icon_area);
        mSignalClusterView = mStatusBar.findViewById(R.id.signal_cluster);
        //*/ freeme.gouzhouping, 20180228. FreemeAppTheme, status bar style
        mCarrierArea = mStatusBar.findViewById(R.id.carrier_area);
        getContext().getContentResolver().registerContentObserver(Settings.System.getUriFor(CARRIER_LABEL),
                false, mShowCarrierLabelObserver);
        //*/

        //*/ freeme.gouzhouping, 20180718. Notch.
        mNotchSystemIcons = mStatusBar.findViewById(R.id.notch_system_icons);
        mSignalClusterView.setQuickNotch(false);
        //*/
        Dependency.get(DarkIconDispatcher.class).addDarkReceiver(mSignalClusterView);
        // Default to showing until we know otherwise.
        showSystemIconArea(false);
        initEmergencyCryptkeeperText();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_PANEL_STATE, mStatusBar.getState());
    }

    @Override
    public void onResume() {
        super.onResume();
        SysUiServiceProvider.getComponent(getContext(), CommandQueue.class).addCallbacks(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        SysUiServiceProvider.getComponent(getContext(), CommandQueue.class).removeCallbacks(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Dependency.get(DarkIconDispatcher.class).removeDarkReceiver(mSignalClusterView);
        Dependency.get(StatusBarIconController.class).removeIconGroup(mDarkIconManager);
        if (mNetworkController.hasEmergencyCryptKeeperText()) {
            mNetworkController.removeCallback(mSignalCallback);
        }
        //*/ freeme.gouzhouping, 20170908. show notification icon switch.
        getContext().getContentResolver().unregisterContentObserver(mNotificationObserver);
        //*/

        //*/ freeme.gouzhouping, 20180228. FreemeAppTheme, status bar style
        getContext().getContentResolver().unregisterContentObserver(mShowCarrierLabelObserver);
        //*/

        //*/ freeme.gouzhouping, 20180228. super power.
        getContext().getContentResolver().unregisterContentObserver(mSuperPowerModeChanged);
        //*/
    }

    public void initNotificationIconArea(NotificationIconAreaController
            notificationIconAreaController) {
        ViewGroup notificationIconArea = mStatusBar.findViewById(R.id.notification_icon_area);
        mNotificationIconAreaInner =
                notificationIconAreaController.getNotificationInnerAreaView();
        if (mNotificationIconAreaInner.getParent() != null) {
            ((ViewGroup) mNotificationIconAreaInner.getParent())
                    .removeView(mNotificationIconAreaInner);
        }
        notificationIconArea.addView(mNotificationIconAreaInner);
        // Default to showing until we know otherwise.
        showNotificationIconArea(false);
        //*/ freeme.gouzhouping, 20180226. FreemeAppTheme, noti
        updateShowNotifications();
        //*/
    }

    @Override
    public void disable(int state1, int state2, boolean animate) {
        state1 = adjustDisableFlags(state1);
        final int old1 = mDisabled1;
        final int diff1 = state1 ^ old1;
        mDisabled1 = state1;
        if ((diff1 & DISABLE_SYSTEM_INFO) != 0) {
            if ((state1 & DISABLE_SYSTEM_INFO) != 0) {
                hideSystemIconArea(animate);
            } else {
                showSystemIconArea(animate);
            }
        }
        if ((diff1 & DISABLE_NOTIFICATION_ICONS) != 0) {
            if ((state1 & DISABLE_NOTIFICATION_ICONS) != 0) {
                hideNotificationIconArea(animate);
            } else {
                /*/ freeme.gouzhouping, 20170908. show notification icon switch.
                showNotificationIconArea(animate);
                /*/
                updateShowNotifications();
                //*/
            }
        }
    }

    protected int adjustDisableFlags(int state) {
        if (!mStatusBarComponent.isLaunchTransitionFadingAway()
                /*/ freeme.gouzhouping, 20180322. fix systemiconArea and notificationiconArea show incorrect
                && !mKeyguardMonitor.isKeyguardFadingAway()
                //*/
                && shouldHideNotificationIcons()) {
            state |= DISABLE_NOTIFICATION_ICONS;
            state |= DISABLE_SYSTEM_INFO;
        }
        if (mNetworkController != null && EncryptionHelper.IS_DATA_ENCRYPTED) {
            if (mNetworkController.hasEmergencyCryptKeeperText()) {
                state |= DISABLE_NOTIFICATION_ICONS;
            }
            if (!mNetworkController.isRadioOn()) {
                state |= DISABLE_SYSTEM_INFO;
            }
        }
        //*/ freeme.gouzhouping, 20181011. status bar state error sometimes.
        if (mStatusBarComponent.mNotificationPanel != null
                && !mStatusBarComponent.mNotificationPanel.isFullyCollapsed()
                || mStatusBarComponent.mBouncerShowing) {
            state |= DISABLE_NOTIFICATION_ICONS;
            state |= DISABLE_SYSTEM_INFO;
        }
        //*/
        return state;
    }

    private boolean shouldHideNotificationIcons() {
        if (!mStatusBar.isClosed() && mStatusBarComponent.hideStatusBarIconsWhenExpanded()) {
            return true;
        }
        if (mStatusBarComponent.hideStatusBarIconsForBouncer()) {
            return true;
        }
        return false;
    }

    public void hideSystemIconArea(boolean animate) {
        animateHide(mSystemIconArea, animate);
        //*/ freeme.gouzhouping, 20180207. FreemeAppTheme, status bar.
        animateHide(mCarrierArea, animate);
        //*/

        //*/ freeme.gouzhouping, 20180718. Notch.
        if (NotchUtils.hasNotch()) {
            animateHide(mNotchSystemIcons, animate);
        }
        //*/
    }

    public void showSystemIconArea(boolean animate) {
        animateShow(mSystemIconArea, animate);
        //*/ freeme.gouzhouping, 20180207. FreemeAppTheme, status bar.
        animateShow(mCarrierArea, animate);
        updateCarrierAreaVisible();
        //*/

        //*/ freeme.gouzhouping, 20180718. Notch.
        if (NotchUtils.hasNotch()) {
            animateShow(mNotchSystemIcons, animate);
        }
        //*/
    }

    public void hideNotificationIconArea(boolean animate) {
        animateHide(mNotificationIconAreaInner, animate);
    }

    public void showNotificationIconArea(boolean animate) {
        animateShow(mNotificationIconAreaInner, animate);
    }

    /**
     * Hides a view.
     */
    private void animateHide(final View v, boolean animate) {
        v.animate().cancel();
        if (!animate) {
            v.setAlpha(0f);
            v.setVisibility(View.INVISIBLE);
            return;
        }
        v.animate()
                .alpha(0f)
                .setDuration(160)
                .setStartDelay(0)
                .setInterpolator(Interpolators.ALPHA_OUT)
                .withEndAction(() -> v.setVisibility(View.INVISIBLE));
    }

    /**
     * Shows a view, and synchronizes the animation with Keyguard exit animations, if applicable.
     */
    private void animateShow(View v, boolean animate) {
        v.animate().cancel();
        v.setVisibility(View.VISIBLE);
        if (!animate) {
            v.setAlpha(1f);
            return;
        }
        v.animate()
                .alpha(1f)
                .setDuration(320)
                .setInterpolator(Interpolators.ALPHA_IN)
                .setStartDelay(50)

                // We need to clean up any pending end action from animateHide if we call
                // both hide and show in the same frame before the animation actually gets started.
                // cancel() doesn't really remove the end action.
                .withEndAction(null);

        // Synchronize the motion with the Keyguard fading if necessary.
        if (mKeyguardMonitor.isKeyguardFadingAway()) {
            v.animate()
                    .setDuration(mKeyguardMonitor.getKeyguardFadingAwayDuration())
                    .setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN)
                    .setStartDelay(mKeyguardMonitor.getKeyguardFadingAwayDelay())
                    .start();
        }
    }

    private void initEmergencyCryptkeeperText() {
        View emergencyViewStub = mStatusBar.findViewById(R.id.emergency_cryptkeeper_text);
        if (mNetworkController.hasEmergencyCryptKeeperText()) {
            if (emergencyViewStub != null) {
                ((ViewStub) emergencyViewStub).inflate();
            }
            mNetworkController.addCallback(mSignalCallback);
        } else if (emergencyViewStub != null) {
            ViewGroup parent = (ViewGroup) emergencyViewStub.getParent();
            parent.removeView(emergencyViewStub);
        }
    }

    //*/ freeme.gouzhouping, 20170908. show notification icon switch.
    private static final String SHOW_NOTIFICATIONS_ICON = FreemeSettings.System.FREEME_SHOW_NOTI_ICON;

    private ContentObserver mNotificationObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            updateShowNotifications();
        }
    };

    public void updateShowNotifications() {
        boolean isShow = Settings.System.getInt(getContext().getContentResolver(),
                SHOW_NOTIFICATIONS_ICON, 0) != 0;
        boolean isSuperPowerOn = SuperPowerUtils.isSuperPowerModeOn(getContext());

        if (isShow && !isSuperPowerOn) {
            showNotificationIconArea(true);
        } else {
            hideNotificationIconArea(false);
        }
    }
    //*/

    //*/ freeme.gouzhouping, 20180207. FreemeAppTheme, status bar style.
    private View mCarrierArea;
    private static final String CARRIER_LABEL =
            com.freeme.systemui.statusbar.FreemeStatusbarStateToolKit.SHOW_CARRIER_LABEL;

    private ContentObserver mShowCarrierLabelObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            updateCarrierAreaVisible();
        }
    };

    public void updateCarrierAreaVisible() {
        boolean isShow = Settings.System.getInt(getContext().getContentResolver(),
                CARRIER_LABEL, 0) != 0;
        mCarrierArea.setVisibility(isShow ? View.VISIBLE : View.GONE);
        mCarrierArea.setAlpha(isShow ? 1.0f : 0);
    }
    //*/

    //*/ freeme.gouzhouping, 20180628. super power.
    private ContentObserver mSuperPowerModeChanged;

    private void refreshQsState() {
        boolean isSuper = SuperPowerUtils.isSuperPowerModeOn(getContext());
        if (isSuper) {
            Dependency.get(FlashlightController.class).setFlashlight(false);
        }
    }
    //*/

    //*/ freeme.gouzhouping, 20180718. Notch.
    private LinearLayout mNotchSystemIcons;
    //*/
}

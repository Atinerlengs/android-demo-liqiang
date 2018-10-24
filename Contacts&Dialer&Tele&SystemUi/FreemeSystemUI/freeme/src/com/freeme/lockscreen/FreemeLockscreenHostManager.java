package com.freeme.lockscreen;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.statusbar.BackDropView;
import com.android.systemui.statusbar.StatusBarState;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.phone.ScrimController;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.policy.FlashlightController;

import com.freeme.systemui.utils.SystemUIToast;
import com.freeme.systemui.utils.SuperPowerUtils;
import com.freeme.keyguard.FreemeKeyguardStatusViewConfig;
import com.freeme.lockscreen.FreemeLockscreenConfiguration;
import com.freeme.lockscreen.FreemeLockscreen;
import com.freeme.lockscreen.FreemeLockscreenController;
import com.freeme.lockscreen.FreemeLockscreenHostCallback;

public class FreemeLockscreenHostManager extends FreemeLockscreenHostCallback {
    private static final String TAG = "FreemeLockscreenHostManager";
    private static final boolean DEBUG = false;

    public interface HostChangedCallBack {
        void onUnlock(Intent target);
    }

    private int mLowBatteryLevel;

    private int mConfigNotifyVisibility;
    private int mConfigBottomVisibility = View.VISIBLE;
    private int mConfigStatusVisibility = View.VISIBLE;

    private boolean mIsLowPower;
    private boolean mIsShowLockscreenView;
    private boolean mHasLockscreenPanel;
    private boolean mShowWallPapers = true;
    private boolean mMonopolyMode;
    private boolean mLockscreenMainViewPrepared;

    private ImageView mBackdropBack;

    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private ScrimController mScrimController;
    private NotificationPanelView mNotificationPanel;

    private FrameLayout mLockscreenMainView;

    private HostChangedCallBack mHostChangedCallBack;
    private FreemeLockscreenController mLockscreenController;
    private FreemeLockscreenHostCallback mLockscreenHostCallback;

    private FreemeLockscreenConfiguration mLockscreenConfig;

    private FlashlightController mFlashLightController;
    private FlashlightController.FlashlightListener mFlashLightListener
            = new FlashlightController.FlashlightListener() {
        @Override
        public void onFlashlightChanged(boolean enabled) {
            mLockscreenConfig.flashlightOn = enabled;
            syncLockscreenConfiguration();
        }

        @Override
        public void onFlashlightError() {}

        @Override
        public void onFlashlightAvailabilityChanged(boolean available) {}
    };

    private Handler mHandler;

    private Context mContext;

    public FreemeLockscreenHostManager(Context context, Handler handler) {
        super(context);

        mHandler = handler;
        mContext = context;

        mLockscreenController = FreemeLockscreenController.createController(this);
    }

    public void addCallback(HostChangedCallBack callBack) {
        mHostChangedCallBack = callBack;
    }

    public void setNotificationPanel(NotificationPanelView notificationPanel) {
        mNotificationPanel = notificationPanel;
    }

    public void setStatusBarKeyguardViewManager(StatusBarKeyguardViewManager statusBarKeyguardViewManager) {
        mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
    }

    public void setScrimController(ScrimController scrimController) {
        mScrimController = scrimController;
    }

    /// --- Lifecycle

    public void initLockscreen(BackDropView backdrop) {
        mBackdropBack = (ImageView) backdrop.findViewById(R.id.keyguard_backdrop_back);
        mBackdropBack.setVisibility(View.VISIBLE);

        mLockscreenMainView = (FrameLayout) backdrop.findViewById(R.id.custom_lockscreen);
        mLockscreenMainView.setVisibility(View.GONE);

        mLowBatteryLevel = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_lowBatteryWarningLevel);

        mLockscreenConfig = new FreemeLockscreenConfiguration();
        mFlashLightController = Dependency.get(FlashlightController.class);

        mContext.getContentResolver().registerContentObserver(
                SuperPowerUtils.getSuperPowerModeUri(),
                true,
                new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                if (SuperPowerUtils.isSuperPowerModeOn(mContext)) {
                    destroyLockscreenHost();
                } else {
                    createLockscreenHost();
                }
            }
        });
    }

    public void createLockscreenHost() {
        if (DEBUG) {
            Log.d(TAG, "createLockscreenHost");
        }

        mLockscreenController.attachHost();
        mLockscreenController.performCreate();
    }

    public void showLockscreenView() {
        if (DEBUG) {
            Log.d(TAG, "showLockscreenView, prepared: " + mLockscreenMainViewPrepared);
        }

        if (mIsShowLockscreenView && !mLockscreenMainViewPrepared) {
            mLockscreenController.performCreateView(mLockscreenMainView);
            mLockscreenMainViewPrepared = true;

            setupLockscreenMonopolyMode();
            syncLockscreenConfiguration();
        }
    }

    public void hideLockscreenView() {
        if (DEBUG) {
            Log.d(TAG, "hideLockscreenView, prepared: " + mLockscreenMainViewPrepared);
        }

        if (mIsShowLockscreenView && mLockscreenMainViewPrepared) {
            mLockscreenController.performDestroyView();
            mLockscreenMainViewPrepared = false;

            mNotificationPanel.setKeyguardLockscreenShow(true);
        }
    }

    public void destroyLockscreenHost() {
        if (DEBUG) {
            Log.d(TAG, "destroyLockscreenHost");
        }

        mLockscreenController.performDestroy();
        mLockscreenController.detachHost();
    }

    public void onLockscreenTurnedOn() {
        if (DEBUG) {
            Log.d(TAG, "onLockscreenTurnedOn");
        }

        if (mIsShowLockscreenView) {
            mLockscreenController.performScreenOn();
        }
    }

    public void onLockscreenTurnedOff() {
        if (DEBUG) {
            Log.d(TAG, "onLockscreenTurnedOff");
        }

        if (mIsShowLockscreenView) {
            mLockscreenController.performScreenOff();
        }
    }

    public void onBatteryLevelChanged(int level) {
        mIsLowPower = level <= mLowBatteryLevel;
    }

    public void resetLockscreenStatus(int state) {
        if (state != StatusBarState.KEYGUARD) {
            mNotificationPanel.resetLockscreenShowStatus();  // reset lockscreen panel status
        }
    }

    private void syncLockscreenConfiguration() {
        if (DEBUG) {
            Log.d(TAG, "syncLockscreenConfiguration, config: " + mLockscreenConfig.toString());
        }

        if (mIsShowLockscreenView && mLockscreenMainViewPrepared) {
            mLockscreenController.performConfigurationChanged(new FreemeLockscreenConfiguration(mLockscreenConfig));
        } else {
            Log.w(TAG, "lockscreen not prepared, sync configuration on next show!" );
        }
    }

    private void setupLockscreenMonopolyMode() {
        if (mMonopolyMode) {
            mNotificationPanel.setPanelConfigFromLockscreen(View.GONE, View.GONE);
            mConfigNotifyVisibility = View.GONE;
            mNotificationPanel.setLockscreenShowMode(true);
        }
    }

    /// --- FreemeLockscreenHostCallback

    @Override
    public void onAttachLockscreen(FreemeLockscreen lockscreen) {
        setCurrentLockscreenStatus(true);
    }

    @Override
    public void onDetachLocksceen(FreemeLockscreen lockscreen) {
        resetConfigData();
        setCurrentLockscreenStatus(false);
    }

    @Override
    public void onSetKeyguardViewVisibility(int type, int visibility) {
        switch (type) {
            case FreemeLockscreen.KEYGUARD_VIEW_STATUS:
                mConfigStatusVisibility = visibility;
                break;
            case FreemeLockscreen.KEYGUARD_VIEW_NOTIFICATION:
                mConfigNotifyVisibility = visibility;
                break;
            case FreemeLockscreen.KEYGUARD_VIEW_BOTTOM:
                mConfigBottomVisibility = visibility;
                break;
        }
        mNotificationPanel.setPanelConfigFromLockscreen(mConfigStatusVisibility, mConfigBottomVisibility);
    }

    @Override
    public void onSetKeyguardViewAlpha(int type, float alpha) {
        switch (type) {
            case FreemeLockscreen.KEYGUARD_VIEW_STATUS:
                mNotificationPanel.setKeyguardStatusViewAlpha(alpha);
                break;
            case FreemeLockscreen.KEYGUARD_VIEW_NOTIFICATION:
                break;
            case FreemeLockscreen.KEYGUARD_VIEW_BOTTOM:
                mNotificationPanel.setKeyguardBottomViewAlpha(alpha);
                break;
        }
    }

    @Override
    public void onSetStageType(int type) {
        switch (type) {
            case FreemeLockscreen.STAGE_TYPE_NORMAL:
                mNotificationPanel.setLockscreenShowMode(false);
                break;
            case FreemeLockscreen.STAGE_TYPE_IMMERSIVE:
                mNotificationPanel.setLockscreenShowMode(true);
                break;
            case FreemeLockscreen.STAGE_TYPE_MONOPOLY:
                mMonopolyMode = true;
                break;
        }
    }

    @Override
    public void onSetStageTitle(CharSequence title) {
        mNotificationPanel.setKeyguardImageTitle(title.toString());
    }

    @Override
    public boolean onRequestUiFeature(long features) {
        if ((features & FreemeLockscreen.FEATURE_UI_HIDE_LOCKWALLPAPER) != 0) {
            mShowWallPapers = false;
            mBackdropBack.setVisibility(View.GONE);
        }
        if ((features & FreemeLockscreen.FEATURE_UI_TIMEAREA_GRAVITY_BOTTOM) != 0) {
            setKeyguardStatusViewConfig(FreemeKeyguardStatusViewConfig.CONFIG_KG_STATUSVIEW_BOTTOM);
        }
        if ((features & FreemeLockscreen.FEATURE_UI_HAVE_PANEL) != 0) {
            mHasLockscreenPanel = true;
        }
        return false;
    }


    @Override
    public void onUnlock(Intent target) {
        mHostChangedCallBack.onUnlock(target);
    }

    @Override
    public boolean onLaunch(String action) {
        boolean result = false;
        switch (action) {
            case FreemeLockscreen.LAUNCH_ACTION_CLOCK:
            case FreemeLockscreen.LAUNCH_ACTION_CALCULATOR:
            case FreemeLockscreen.LAUNCH_ACTION_SOUNDRECORDER: {
                result = launchToTarget(action);
                break;
            }
            case FreemeLockscreen.LAUNCH_ACTION_FLASHLIGHT_TOGGLE: {
                if (!mIsLowPower) {
                    mFlashLightController.setFlashlight(!mFlashLightController.isEnabled());
                    result = true;
                } else {
                    final String lowBatteryWarning = mContext.getResources()
                           .getString(R.string.freeme_low_battery_flashlight_canot_use);
                    SystemUIToast.makeText(mContext, lowBatteryWarning).show();
                }
                break;
            }
        }
        return result;
    }

    private boolean launchToTarget(String action) {
        int toolkitId = 0;
        switch (action) {
            case FreemeLockscreen.LAUNCH_ACTION_CLOCK:
                toolkitId = R.string.lockscreen_toolkit_clock;
                break;
            case FreemeLockscreen.LAUNCH_ACTION_CALCULATOR:
                toolkitId = R.string.lockscreen_toolkit_calculator;
                break;
            case FreemeLockscreen.LAUNCH_ACTION_SOUNDRECORDER:
                toolkitId = R.string.lockscreen_toolkit_soundrecorder;
                break;
        }

        if (toolkitId > 0) {
            //TODO: this launch need show on desktop, but need apk support, current still unlock keyguard first
            mHostChangedCallBack.onUnlock(null);

            final String componentName = mContext.getResources().getString(toolkitId);
            try {
                mContext.startActivity(new Intent()
                        .setComponent(ComponentName.unflattenFromString(componentName))
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return true;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "Activity not found: " + componentName);
            }
        }
        return false;
    }

    private void setCurrentLockscreenStatus(boolean show) {
        if (DEBUG) {
            Log.d(TAG, "setCurrentLockscreenStatus, show : " + show);
        }

        mIsShowLockscreenView = show;
        if (show) {
            mBackdropBack.setVisibility(mShowWallPapers ? View.VISIBLE : View.GONE);
            mLockscreenMainView.setVisibility(View.VISIBLE);
            mScrimController.setLockscreenShowStatus(true);
            mFlashLightController.addCallback(mFlashLightListener);
        } else {
            setKeyguardStatusViewConfig(FreemeKeyguardStatusViewConfig.CONFIG_KG_STATUSVIEW_TOP);
            mScrimController.setLockscreenShowStatus(false);
            mNotificationPanel.setKeyguardImageTitle("");
            mBackdropBack.setVisibility(View.VISIBLE);
            mLockscreenMainView.setVisibility(View.GONE);
            mFlashLightController.removeCallback(mFlashLightListener);
        }
    }

    private void setKeyguardStatusViewConfig(int mode) {
        FreemeKeyguardStatusViewConfig.sConfig = mode;
    }

    private void resetConfigData() {
        mConfigBottomVisibility = View.VISIBLE;
        mConfigStatusVisibility = View.VISIBLE;
        mConfigNotifyVisibility = View.VISIBLE;
        mHasLockscreenPanel = false;
        mLockscreenConfig.setToDefaults();
        mShowWallPapers = true;
        mMonopolyMode = false;
        mNotificationPanel.setPanelConfigFromLockscreen(mConfigStatusVisibility, mConfigBottomVisibility);
    }

    /// --- StatusBar

    public boolean notShowWallPapers() {
        return mIsShowLockscreenView && !mShowWallPapers;
    }

    public boolean isConfigNotifyNotVisible() {
        return mConfigNotifyVisibility != View.VISIBLE;
    }

    public boolean isShowLockscreenView() {
        return mIsShowLockscreenView;
    }

    /// --- NotificationPanelView

    public boolean enableLockscreenPanelTouch() {
        return mIsShowLockscreenView && mHasLockscreenPanel;
    }

    public void setLockscreenConfigShow(boolean isShow) {
        if (DEBUG) {
            Log.d(TAG, "setLockscreenConfigShow, vis: " + isShow);
        }

        if (mIsShowLockscreenView) {
            final int newStatus = isShow ?
                    FreemeLockscreen.STAGE_TYPE_IMMERSIVE :
                    FreemeLockscreen.STAGE_TYPE_NORMAL;
            if (mLockscreenConfig.stageType != newStatus) {
                mLockscreenConfig.stageType = newStatus;
                syncLockscreenConfiguration();
            }
        }
    }

    public boolean isMonopolyMode() {
        return mMonopolyMode;
    }
}

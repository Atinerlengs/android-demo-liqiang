package com.freeme.systemui.battery;

import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Paint.FontMetrics;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.KeyguardStatusBarView;
import com.android.systemui.statusbar.phone.PhoneStatusBarView;
import com.android.systemui.statusbar.phone.StatusBarWindowView;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback;
import com.freeme.systemui.notch.NotchStatusBar;
import com.freeme.systemui.tint.TintImageView;
import com.freeme.systemui.tint.TintTextView;
import com.freeme.systemui.utils.NotchUtils;
import com.freeme.systemui.utils.NumberLocationPercent;
import com.freeme.systemui.utils.PercentColor;

import com.freeme.systemui.statusbar.FreemeStatusbarStateToolKit;

public class FreemeBatteryManager implements BatteryStateChangeCallback {

    private BatteryController mBatteryController;
    private BatteryView[] mBatteryViews;
    private boolean mCharging;
    private Context mContext;
    private int mLevel;
    private int mPercentColor = -1;
    private PercentColor[] mPercentColors;
    private boolean mPlugged;
    private int mPluggedColor;

    private boolean mShowPercent;
    private boolean mShowPercentIn;
    private boolean mShowPercentFull;
    private boolean mUsePluggedColor;

    PhoneStatusBarView mStatusBarWindowView;
    protected KeyguardStatusBarView mKeyguardStatusBar;
    NotchStatusBar mNotchStatusBar;

    private final static String FREEME_SHOW_BATTERY_LEVEL =
            FreemeStatusbarStateToolKit.SHOW_BATTERY_LEVEL_SWITCH;

    private ContentObserver mBatteryLevelObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            update();
        }
    };

    public FreemeBatteryManager(Context context,
                                BatteryController batteryController,
                                PhoneStatusBarView statusBar,
                                KeyguardStatusBarView keyguardStatusBarView) {
        mContext = context;
        mBatteryController = batteryController;
        mStatusBarWindowView = statusBar;
        mKeyguardStatusBar = keyguardStatusBarView;
        initColor();
        initView();
        register();
        update();
    }

    public FreemeBatteryManager(Context context,
                                BatteryController batteryController,
                                PhoneStatusBarView statusBar,
                                KeyguardStatusBarView keyguardStatusBarView,
                                NotchStatusBar notchStatusBar) {
        mContext = context;
        mBatteryController = batteryController;
        mStatusBarWindowView = statusBar;
        mKeyguardStatusBar = keyguardStatusBarView;
        mNotchStatusBar = notchStatusBar;
        initColor();
        initView();
        register();
        update();
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        mLevel = level;
        mCharging = charging;
        mPlugged = pluggedIn;
        mPercentColor = PercentColor.getColor(mPercentColors, mLevel);
        update();
    }

    @Override
    public void onPowerSaveChanged(boolean isPowerSave) {
        update();
    }

    private void initColor() {
        String pluggedColorStr = System.getString(mContext.getContentResolver(), "plugged_battery_percent_color");
        String percentColorStr = System.getString(mContext.getContentResolver(), "battery_percent_color");
        if (TextUtils.isEmpty(pluggedColorStr)) {
            pluggedColorStr = "#b3ffffff";
        }
        if (TextUtils.isEmpty(percentColorStr)) {
            percentColorStr = "10:#ff3320;20:#ff9b1a;100:#b3ffffff";
        }
        mPluggedColor = Color.parseColor(pluggedColorStr);
        mPercentColors = PercentColor.createFromColorStrs(percentColorStr);
    }

    private void initView() {
        if (NotchUtils.hasNotch()) {
            mBatteryViews = new BatteryView[]{
                    new BatteryView(mStatusBarWindowView),
                    new BatteryView(mKeyguardStatusBar),
                    new BatteryView(mNotchStatusBar)
            };
        } else {
            mBatteryViews = new BatteryView[]{
                    new BatteryView(mStatusBarWindowView),
                    new BatteryView(mKeyguardStatusBar)
            };
        }
    }

    public void register() {
        mBatteryController.addCallback(this);
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(FREEME_SHOW_BATTERY_LEVEL),
                true, mBatteryLevelObserver, UserHandle.USER_ALL);
    }

    public void update() {
        updateResolver();

        for (BatteryView view : mBatteryViews) {
            view.update();
        }
    }

    public void onConfigurationChanged() {
        for (BatteryView view : mBatteryViews) {
            view.refresh();
            view.update();
        }
    }

    private void updateResolver() {
        mShowPercent = System.getIntForUser(mContext.getContentResolver(), FREEME_SHOW_BATTERY_LEVEL, 0,
                UserHandle.USER_CURRENT) == 1;
        if (NotchUtils.hasNotch()) {
            mShowPercent = false;
        }
        mShowPercentIn = System.getInt(mContext.getContentResolver(), "show_battery_percentin", 0) == 1;
        mUsePluggedColor = System.getInt(mContext.getContentResolver(), "use_plugged_color", 0) == 1;
    }

    private boolean isShowFullCharged() {
        return mLevel >= 100 && mPlugged && mShowPercentFull;
    }

    public boolean canOpenFlashLight() {
        return mLevel >= 5;
    }

    private boolean isWarnningLevel() {
        return mLevel <= 5;
    }

    private boolean isLowLevel() {
        return mLevel <= 20;
    }

    private boolean isBitLowLevel() {
        return mLevel > 5 && mLevel <= 20;
    }

    class BatteryView {
        private TintImageView mBorderView;
        private ImageView mInsideChargeLevelView;
        private TintImageView mInsideChargeView;
        private TintImageView mInsideLevelView;
        private TintTextView mInsidePercentView;
        private TintImageView mInsideWarnningCenterView;
        private TintImageView mInsideWarnningLeftView;
        private TintImageView mOutsideChargeView;
        private TintTextView mOutsidePercentView;

        private boolean mIsValid;

        public BatteryView(View container) {
            if (container != null) {
                mIsValid = true;
                mBorderView = (TintImageView) container.findViewById(R.id.battery_border);
                mInsideLevelView = (TintImageView) container.findViewById(R.id.battery_inside_level);
                mInsideChargeLevelView = (ImageView) container.findViewById(R.id.battery_inside_charge_level);
                mInsidePercentView = (TintTextView) container.findViewById(R.id.battery_inside_percent);
                mOutsidePercentView = (TintTextView) container.findViewById(R.id.battery_outside_percent);
                mInsideChargeView = (TintImageView) container.findViewById(R.id.battery_inside_charge);
                mOutsideChargeView = (TintImageView) container.findViewById(R.id.battery_outside_charge);
                mInsideWarnningCenterView = (TintImageView) container.findViewById(R.id.battery_inside_warning_center);
                mInsideWarnningLeftView = (TintImageView) container.findViewById(R.id.battery_inside_warning_left);

                mInsideLevelView.setImageResource(R.drawable.stat_sys_battery_new_svg);
                mInsideChargeLevelView.setImageResource(R.drawable.stat_sys_battery_charge_new_svg);
            }
        }

        private void update() {
            if (mIsValid) {
                boolean showOutsidePercentView = (isShowFullCharged() && !mShowPercent) || (mShowPercent && !mShowPercentIn);
                boolean showPercent = mShowPercent && mShowPercentIn;
                boolean showInsideChargeView = mCharging && !showPercent;
                boolean showPercentInCharging = mCharging && showPercent;
                boolean showInsideLevelView = !mCharging && !showPercent;
                boolean showInsideChargeLevelView = mCharging;
                boolean showInsideWarnningCenterView = showInsideLevelView && isWarnningLevel() && !mPlugged;
                boolean showInsideWarnningLeftView = showPercent && isWarnningLevel() && !mPlugged;
                mOutsidePercentView.setVisibility(showOutsidePercentView ? View.VISIBLE : View.GONE);
                mInsidePercentView.setVisibility(showPercent ? View.VISIBLE : View.GONE);
                mInsideChargeView.setVisibility(showInsideChargeView
                        && (mLevel < 100) ? View.VISIBLE : View.GONE);
                mOutsideChargeView.setVisibility(showPercentInCharging ? View.VISIBLE : View.GONE);

                mInsideLevelView.setVisibility(showInsideLevelView ? View.VISIBLE : View.GONE);
                mInsideWarnningCenterView.setVisibility(showInsideWarnningCenterView ? View.VISIBLE : View.GONE);
                mInsideChargeLevelView.setVisibility(showInsideChargeLevelView ? View.VISIBLE : View.GONE);
                mInsideWarnningLeftView.setVisibility(showInsideWarnningLeftView ? View.VISIBLE : View.GONE);
                updatePercent();
                updateLevel();
                updateCharge();
                updateBorderViewDescription();
            }
        }

        public void refresh() {
            mBorderView.setImageResource(R.drawable.ic_statusbar_battery);
            mInsideWarnningCenterView.setImageResource(R.drawable.ic_statusbar_battery_low);
            mInsideWarnningLeftView.setImageResource(R.drawable.ic_statusbar_battery_powersaving_low);
            mInsideLevelView.setImageResource(R.drawable.stat_sys_battery_new_svg);
            mInsideChargeLevelView.setImageResource(R.drawable.stat_sys_battery_charge_new_svg);
        }

        private void updateBorderViewDescription() {
            int chargeStr = mCharging ? R.string.accessibility_battery_level_charging :
                    R.string.accessibility_battery_level;
            mBorderView.setContentDescription(mContext.getString(chargeStr, mLevel));
        }

        private void updateLevel() {
            mInsideLevelView.setIsResever(isLevelUseTint());
            mInsideLevelView.setImageLevel(mLevel);
            mInsideLevelView.setImageResource(R.drawable.stat_sys_battery_new_svg);
            mInsideChargeLevelView.setImageLevel(mLevel);
        }

        private void updatePercent() {
            mInsidePercentView.setText(NumberLocationPercent.getFormatnumberString(mLevel, mContext));
            String level = NumberLocationPercent.getPercentage((double) mLevel, 0);
            mOutsidePercentView.setText(mContext.getString(R.string.status_bar_settings_battery_meter_format, level).trim());
            if (mPlugged && (mUsePluggedColor || isBitLowLevel())) {
                mInsidePercentView.setTextColor(mPluggedColor);
            } else {
                mInsidePercentView.setTextColor(mPercentColor);
            }
            translationY4InsidePercent();
        }

        private void translationY4InsidePercent() {
            FontMetrics fontMetrics = mInsidePercentView.getPaint().getFontMetrics();
            mInsidePercentView.setTranslationY((fontMetrics.top - fontMetrics.ascent) / 4.0f);
        }

        void updateCharge() {
            mInsideChargeView.setImageResource(R.drawable.ic_statusbar_battery_charge);
            mOutsideChargeView.setImageResource(R.drawable.ic_statusbar_battery_charge_figure);
        }

        private boolean isLevelUseTint() {
            return !isLowLevel();
        }
    }
}

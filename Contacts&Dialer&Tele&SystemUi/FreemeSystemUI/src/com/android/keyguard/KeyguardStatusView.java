/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.keyguard;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.support.v4.graphics.ColorUtils;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.internal.util.ArrayUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.systemui.ChargingView;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.DateView;

import com.freeme.keyguard.FreemeKeyguardStatusViewConfig;
import com.freeme.systemui.utils.SuperPowerUtils;
import com.freeme.systemui.utils.WeatherData;
import com.freeme.systemui.utils.WeatherUtils;

import java.util.Locale;

//*/ freeme.gouzhouping, 20180620. weather conditions.
public class KeyguardStatusView extends GridLayout implements WeatherUtils.WeatherCallBack,
        BatteryController.BatteryStateChangeCallback {
/*/
public class KeyguardStatusView extends GridLayout {
//*/
    private static final boolean DEBUG = KeyguardConstants.DEBUG;
    private static final String TAG = "KeyguardStatusView";
    private static final int MARQUEE_DELAY_MS = 2000;

    private final LockPatternUtils mLockPatternUtils;
    private final AlarmManager mAlarmManager;

    /// M: add for mock testcase
    TextView mAlarmStatusView;
    DateView mDateView;
    TextClock mClockView;
    TextView mOwnerInfo;
    private ViewGroup mClockContainer;
    private ChargingView mBatteryDoze;
    private View mKeyguardStatusArea;
    private Runnable mPendingMarqueeStart;
    private Handler mHandler;

    private View[] mVisibleInDoze;
    private boolean mPulsing;
    private float mDarkAmount = 0;
    private int mTextColor;
    private int mDateTextColor;
    private int mAlarmTextColor;

    /// M: add for mock testcase
    KeyguardUpdateMonitorCallback mInfoCallback = new KeyguardUpdateMonitorCallback() {

        @Override
        public void onTimeChanged() {
            refresh();
        }

        @Override
        public void onKeyguardVisibilityChanged(boolean showing) {
            if (showing) {
                if (DEBUG) Slog.v(TAG, "refresh statusview showing:" + showing);
                refresh();
                updateOwnerInfo();
                //*/ freeme.gouzhouping, 20180628. super power.
                boolean isSuperPowerOn = SuperPowerUtils.isSuperPowerModeOn(mContext);
                mKgWeatherConditions.setVisibility(isSuperPowerOn ? GONE : VISIBLE);
                mKgTemperature.setVisibility(isSuperPowerOn ? GONE : VISIBLE);
                //*/
            }
        }

        @Override
        public void onStartedWakingUp() {
            setEnableMarquee(true);
        }

        @Override
        public void onFinishedGoingToSleep(int why) {
            setEnableMarquee(false);
        }

        @Override
        public void onUserSwitchComplete(int userId) {
            refresh();
            updateOwnerInfo();
        }
    };

    public KeyguardStatusView(Context context) {
        this(context, null, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mLockPatternUtils = new LockPatternUtils(getContext());
        mHandler = new Handler(Looper.myLooper());
        //*/ freeme.gouzhouping, 20180425. weather conditions.
        mWeatherUtils = WeatherUtils.getInstance(context);
        mFilter.addAction(Intent.ACTION_SCREEN_ON);
        mFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
        mWeatherUtils.registerView(this);
        //*/
    }

    private void setEnableMarquee(boolean enabled) {
        if (DEBUG) Log.v(TAG, "Schedule setEnableMarquee: " + (enabled ? "Enable" : "Disable"));
        if (enabled) {
            if (mPendingMarqueeStart == null) {
                mPendingMarqueeStart = () -> {
                    setEnableMarqueeImpl(true);
                    mPendingMarqueeStart = null;
                };
                mHandler.postDelayed(mPendingMarqueeStart, MARQUEE_DELAY_MS);
            }
        } else {
            if (mPendingMarqueeStart != null) {
                mHandler.removeCallbacks(mPendingMarqueeStart);
                mPendingMarqueeStart = null;
            }
            setEnableMarqueeImpl(false);
        }
    }

    private void setEnableMarqueeImpl(boolean enabled) {
        if (DEBUG) Log.v(TAG, (enabled ? "Enable" : "Disable") + " transport text marquee");
        if (mAlarmStatusView != null) mAlarmStatusView.setSelected(enabled);
        if (mOwnerInfo != null) mOwnerInfo.setSelected(enabled);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mClockContainer = findViewById(R.id.keyguard_clock_container);
        mAlarmStatusView = findViewById(R.id.alarm_status);
        mDateView = findViewById(R.id.date_view);
        mClockView = findViewById(R.id.clock_view);
        mClockView.setShowCurrentUserTime(true);
        if (KeyguardClockAccessibilityDelegate.isNeeded(mContext)) {
            mClockView.setAccessibilityDelegate(new KeyguardClockAccessibilityDelegate(mContext));
        }
        mOwnerInfo = findViewById(R.id.owner_info);
        mBatteryDoze = findViewById(R.id.battery_doze);
        mKeyguardStatusArea = findViewById(R.id.keyguard_status_area);
        mVisibleInDoze = new View[]{mBatteryDoze, mClockView, mKeyguardStatusArea};
        mTextColor = mClockView.getCurrentTextColor();
        mDateTextColor = mDateView.getCurrentTextColor();
        mAlarmTextColor = mAlarmStatusView.getCurrentTextColor();

        //*/ freeme.xiaohui,20180524, FreemeLockscreen
        mBatteryController = Dependency.get(BatteryController.class);
        mImageTitleArea = (LinearLayout)findViewById(R.id.freeme_keyguard_image_title_area);
        mImageTitle = (TextView)findViewById(R.id.keyguard_image_title);
        mChargingInfo = findViewById(R.id.charging_info);
        updateImageTitle();
        //*/

        boolean shouldMarquee = KeyguardUpdateMonitor.getInstance(mContext).isDeviceInteractive();
        setEnableMarquee(shouldMarquee);
        refresh();
        updateOwnerInfo();

        // Disable elegant text height because our fancy colon makes the ymin value huge for no
        // reason.
        mClockView.setElegantTextHeight(false);

        //*/ freeme.gouzhouping, 20180425. weather conditions.
        mKgWeatherConditions = findViewById(R.id.kg_weather);
        mKgTemperature = findViewById(R.id.kg_tmp);
        mKgWeatherConditions.setText(WeatherUtils.NO_WEATHER);
        //*/
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.widget_big_font_size));
        // Some layouts like burmese have a different margin for the clock
        MarginLayoutParams layoutParams = (MarginLayoutParams) mClockView.getLayoutParams();
        layoutParams.bottomMargin = getResources().getDimensionPixelSize(
                R.dimen.bottom_text_spacing_digital);
        mClockView.setLayoutParams(layoutParams);
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.widget_label_font_size));
        if (mOwnerInfo != null) {
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.widget_label_font_size));
        }

        //*/ freeme.xiaohui, 20180630, FreemeLockscreen bugfix#0040399
        int fontSize = R.dimen.widget_label_font_size;
        resetTextViewSize(mChargingInfo, fontSize);
        resetTextViewSize(mImageTitle, fontSize);
        resetTextViewSize(mKgWeatherConditions, fontSize);
        resetTextViewSize(mKgTemperature, fontSize);
        //*/
    }

    public void refreshTime() {
        mDateView.setDatePattern(Patterns.dateViewSkel);

        mClockView.setFormat12Hour(Patterns.clockView12);
        mClockView.setFormat24Hour(Patterns.clockView24);
    }

    private void refresh() {
        AlarmManager.AlarmClockInfo nextAlarm =
                mAlarmManager.getNextAlarmClock(UserHandle.USER_CURRENT);
        Patterns.update(mContext, nextAlarm != null);

        refreshTime();
        refreshAlarmStatus(nextAlarm);
    }

    void refreshAlarmStatus(AlarmManager.AlarmClockInfo nextAlarm) {
        if (nextAlarm != null) {
            String alarm = formatNextAlarm(mContext, nextAlarm);
            mAlarmStatusView.setText(alarm);
            mAlarmStatusView.setContentDescription(
                    getResources().getString(R.string.keyguard_accessibility_next_alarm, alarm));
            /*/ freeme.gouzhouping, 20180115. FreemeAppTheme, keyguard view.
            mAlarmStatusView.setVisibility(View.VISIBLE);
            //*/
        } else {
            mAlarmStatusView.setVisibility(View.GONE);
        }
    }

    public int getClockBottom() {
        return mKeyguardStatusArea.getBottom();
    }

    public float getClockTextSize() {
        return mClockView.getTextSize();
    }

    public static String formatNextAlarm(Context context, AlarmManager.AlarmClockInfo info) {
        if (info == null) {
            return "";
        }
        String skeleton = DateFormat.is24HourFormat(context, ActivityManager.getCurrentUser())
                ? "EHm"
                : "Ehma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return DateFormat.format(pattern, info.getTriggerTime()).toString();
    }

    private void updateOwnerInfo() {
        if (mOwnerInfo == null) return;
        String ownerInfo = getOwnerInfo();
        if (!TextUtils.isEmpty(ownerInfo)) {
            mOwnerInfo.setVisibility(View.VISIBLE);
            mOwnerInfo.setText(ownerInfo);
        } else {
            mOwnerInfo.setVisibility(View.GONE);
        }

        //*/ freeme.xiaohui,20180525, FreemeLockscreen
        updateOwnerStatus();
        //*/
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mInfoCallback);
        //*/ freeme.gouzhouping, 20180425. weather conditions.
        mContext.registerReceiver(mWeaUpdateReceiver, mFilter);
        //*/

        //*/ freeme.xiaohui, 20180524, FreemeLockscreen
        mBatteryController.addCallback(this);
        //*/
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(mContext).removeCallback(mInfoCallback);
        //*/ freeme.gouzhouping, 20180425. weather conditions.
        mContext.unregisterReceiver(mWeaUpdateReceiver);
        //*/

        //*/ freeme.xiaohui, 20180524, FreemeLockscreen
        mBatteryController.removeCallback(this);
        //*/
    }

    private String getOwnerInfo() {
        String info = null;
        if (mLockPatternUtils.isDeviceOwnerInfoEnabled()) {
            // Use the device owner information set by device policy client via
            // device policy manager.
            info = mLockPatternUtils.getDeviceOwnerInfo();
        } else {
            // Use the current user owner information if enabled.
            final boolean ownerInfoEnabled = mLockPatternUtils.isOwnerInfoEnabled(
                    KeyguardUpdateMonitor.getCurrentUser());
            if (ownerInfoEnabled) {
                info = mLockPatternUtils.getOwnerInfo(KeyguardUpdateMonitor.getCurrentUser());
            }
        }
        return info;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    // DateFormat.getBestDateTimePattern is extremely expensive, and refresh is called often.
    // This is an optimization to ensure we only recompute the patterns when the inputs change.
    private static final class Patterns {
        static String dateViewSkel;
        static String clockView12;
        static String clockView24;
        static String cacheKey;

        static void update(Context context, boolean hasAlarm) {
            final Locale locale = Locale.getDefault();
            final Resources res = context.getResources();
            dateViewSkel = res.getString(hasAlarm
                    ? R.string.abbrev_wday_month_day_no_year_alarm
                    : R.string.abbrev_wday_month_day_no_year);
            final String clockView12Skel = res.getString(R.string.clock_12hr_format);
            final String clockView24Skel = res.getString(R.string.clock_24hr_format);
            final String key = locale.toString() + dateViewSkel + clockView12Skel + clockView24Skel;
            if (key.equals(cacheKey)) return;

            clockView12 = DateFormat.getBestDateTimePattern(locale, clockView12Skel);
            // CLDR insists on adding an AM/PM indicator even though it wasn't in the skeleton
            // format.  The following code removes the AM/PM indicator if we didn't want it.
            if (!clockView12Skel.contains("a")) {
                clockView12 = clockView12.replaceAll("a", "").trim();
            }

            clockView24 = DateFormat.getBestDateTimePattern(locale, clockView24Skel);

            // Use fancy colon.
            clockView24 = clockView24.replace(':', '\uee01');
            clockView12 = clockView12.replace(':', '\uee01');

            cacheKey = key;
        }
    }

    public void setDark(float darkAmount) {
        if (mDarkAmount == darkAmount) {
            return;
        }
        mDarkAmount = darkAmount;

        boolean dark = darkAmount == 1;
        final int N = mClockContainer.getChildCount();
        for (int i = 0; i < N; i++) {
            View child = mClockContainer.getChildAt(i);
            if (ArrayUtils.contains(mVisibleInDoze, child)) {
                continue;
            }
            child.setAlpha(dark ? 0 : 1);
        }
        if (mOwnerInfo != null) {
            mOwnerInfo.setAlpha(dark ? 0 : 1);
        }

        updateDozeVisibleViews();
        mBatteryDoze.setDark(dark);
        mClockView.setTextColor(ColorUtils.blendARGB(mTextColor, Color.WHITE, darkAmount));
        mDateView.setTextColor(ColorUtils.blendARGB(mDateTextColor, Color.WHITE, darkAmount));
        int blendedAlarmColor = ColorUtils.blendARGB(mAlarmTextColor, Color.WHITE, darkAmount);
        mAlarmStatusView.setTextColor(blendedAlarmColor);
        mAlarmStatusView.setCompoundDrawableTintList(ColorStateList.valueOf(blendedAlarmColor));
    }

    public void setPulsing(boolean pulsing) {
        mPulsing = pulsing;
        updateDozeVisibleViews();
    }

    private void updateDozeVisibleViews() {
        for (View child : mVisibleInDoze) {
            child.setAlpha(mDarkAmount == 1 && mPulsing ? 0.8f : 1);
        }
    }

    //*/ freeme.gouzhouping, 20180425. weather conditions.
    private TextView mKgWeatherConditions;
    private TextView mKgTemperature;
    private WeatherUtils mWeatherUtils;
    private IntentFilter mFilter = new IntentFilter();

    private BroadcastReceiver mWeaUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_ON)
                    || action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                mWeatherUtils.query();
            }
        }
    };

    @Override
    public void updateWeatherInfo(WeatherData weather) {
        if (weather != null && !TextUtils.isEmpty(weather.getWeatherType())
                && Integer.valueOf(weather.getWeatherType()) >= 0) {
            mKgWeatherConditions.setText(WeatherUtils.getInstance(mContext).getWeatherType(
                    Integer.valueOf(weather.getWeatherType())));
            mKgTemperature.setText(weather.getCurTemper() + WeatherUtils.TEMPURTURE_POSTFIX);
        } else {
            mKgWeatherConditions.setText(WeatherUtils.NO_WEATHER);
            mKgTemperature.setText("");
        }
    }
    //*/

    //*/ freeme.xiaohui,20180524, FreemeLockscreen
    TextView mImageTitle;
    private TextView mChargingInfo;
    LinearLayout mImageTitleArea;
    private String mImageTitleString;
    private BatteryController mBatteryController;
    private int mBatteryLevel;
    private boolean mChargePluged;

    public void setImageTitle(String title) {
        mImageTitleString = title;
        updateImageTitle();
    }

    public void updateImageTitle() {
        if (TextUtils.isEmpty(mImageTitleString)) {
            mImageTitleArea.setVisibility(View.GONE);
        } else if (getKeyguardStatusViewConfig() == FreemeKeyguardStatusViewConfig.CONFIG_KG_STATUSVIEW_BOTTOM){
            mImageTitleArea.setVisibility(View.VISIBLE);
            mImageTitle.setText(mImageTitleString);
        } else {
            mImageTitleArea.setVisibility(View.GONE);
        }
    }

    private int getKeyguardStatusViewConfig() {
        return FreemeKeyguardStatusViewConfig.sConfig;
    }

    private void updateOwnerStatus() {
        if (getKeyguardStatusViewConfig() == FreemeKeyguardStatusViewConfig.CONFIG_KG_STATUSVIEW_BOTTOM) {
            mOwnerInfo.setVisibility(View.GONE);
        }
        updateChargeInfo(mBatteryLevel, mChargePluged);
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        mBatteryLevel = level;
        mChargePluged = pluggedIn;
        updateChargeInfo(level, pluggedIn);
    }

    private void updateChargeInfo(int level, boolean pluggedIn) {
        String mChargeText = null;
        String chargeInfo = level == 100 ? getResources().getString(R.string.magazine_kg_charged):
                String.format(getResources().getString(R.string.magazine_kg_charging), level);
        String ownerInfo = getOwnerInfo();

        if (!needShowChargeStatus()) {
            mChargeText = null;
        } else if (pluggedIn) {
            if (TextUtils.isEmpty(ownerInfo)) {
                mChargeText = chargeInfo;
            } else {
                mChargeText = chargeInfo + " -- " + ownerInfo;
            }
        } else {
            mChargeText = ownerInfo;
        }

        if (TextUtils.isEmpty(mChargeText)) {
            mChargingInfo.setVisibility(View.GONE);
        } else {
            mChargingInfo.setText(mChargeText);
            mChargingInfo.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPowerSaveChanged(boolean isPowerSave) {
        // do nothing
    }

    private boolean needShowChargeStatus() {
        return getKeyguardStatusViewConfig() == FreemeKeyguardStatusViewConfig.CONFIG_KG_STATUSVIEW_BOTTOM;
    }

    private void resetTextViewSize(TextView v, int size) {
        if (v != null) {
            v.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(size));
        }
    }
    //*/
}

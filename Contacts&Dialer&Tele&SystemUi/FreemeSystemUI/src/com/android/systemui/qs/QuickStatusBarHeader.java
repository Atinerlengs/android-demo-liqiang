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

package com.android.systemui.qs;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settingslib.Utils;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.R.id;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.qs.QSDetail.Callback;
import com.android.systemui.statusbar.SignalClusterView;
import com.android.systemui.statusbar.phone.MultiUserSwitch;
import com.android.systemui.statusbar.phone.SettingsButton;
import com.android.systemui.statusbar.policy.Clock;
import com.android.systemui.statusbar.policy.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.tuner.TunerService;

import com.freeme.systemui.utils.NotchUtils;
import com.freeme.systemui.utils.WeatherData;
import com.freeme.systemui.utils.WeatherUtils;
import com.freeme.util.FreemeFeature;

import java.util.Calendar;
import java.util.Locale;


//*/ freeme.gouzhouping, 20180620. weather conditions.
public class QuickStatusBarHeader extends RelativeLayout implements WeatherUtils.WeatherCallBack,
        UserInfoController.OnUserInfoChangedListener {
/*/
public class QuickStatusBarHeader extends RelativeLayout {
//*/

    private ActivityStarter mActivityStarter;

    private QSPanel mQsPanel;

    private boolean mExpanded;
    private boolean mListening;

    protected QuickQSPanel mHeaderQsPanel;
    protected QSTileHost mHost;
    //*/ freeme.gouzhouping, 20180116. FreemeAppTheme, qs container.
    protected View mEdit;
    private SettingsButton mSettingsButton;
    private View mDate;
    private View mClock;
    protected MultiUserSwitch mMultiUserSwitch;
    private ImageView mMultiUserAvatar;
    private UserInfoController mUserInfoController;
    //*/

    public QuickStatusBarHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        //*/ freeme.gouzhouping, 20180619. weather conditions.
        WeatherUtils.getInstance(context).registerView(this);
        //*/

        //*/ freeme.gouzhouping, 20180823. quick statusbar header ampm.
        mIsShowAmPm = FreemeFeature.isLocalSupported("feature.qs.header.show_ampm");
        //*/
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources res = getResources();

        mHeaderQsPanel = findViewById(R.id.quick_qs_panel);

        // RenderThread is doing more harm than good when touching the header (to expand quick
        // settings), so disable it for this view

        updateResources();

        // Set the light/dark theming on the header status UI to match the current theme.
        int colorForeground = Utils.getColorAttr(getContext(), android.R.attr.colorForeground);
        float intensity = colorForeground == Color.WHITE ? 0 : 1;
        Rect tintArea = new Rect(0, 0, 0, 0);

        applyDarkness(R.id.battery, tintArea, intensity, colorForeground);
        /*/ freeme.lishoubo, 20180206. FreemeAppTheme, qs_header
        applyDarkness(R.id.clock, tintArea, intensity, colorForeground);
        /*/
        int clockcolorForeground = getResources ().getColor (R.color.freeme_status_bar_clock_color);
        float clockintensity = clockcolorForeground == Color.WHITE ? 0 : 1;;
        applyDarkness(R.id.clock, tintArea, clockintensity, clockcolorForeground);
        //*/

        /*/ freeme.gouzhouping, 20180116. FreemeAppTheme, qs container.
        BatteryMeterView battery = findViewById(R.id.battery);
        battery.setForceShowPercent(true);
        //*/

        mActivityStarter = Dependency.get(ActivityStarter.class);

        //*/ freeme.gouzhouping, 20180116. FreemeAppTheme, qs container.
        mHeaderQsPanel.setVisibility(View.GONE);
        mEdit = findViewById(android.R.id.edit);
        mEdit.setOnClickListener(view ->
                Dependency.get(ActivityStarter.class).postQSRunnableDismissingKeyguard(() ->
                        mQsPanel.showEdit(view)));
        mSettingsButton = findViewById(R.id.settings_button);
        mSettingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startSettings();
            }
        });

        mDate = findViewById(R.id.date);
        mDate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startCalendarActivity();
            }
        });

        mClock = findViewById(R.id.clock);
        mClock.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startDeskClockActivity();
            }
        });

        mMultiUserSwitch = findViewById(R.id.multi_user_switch);
        mMultiUserAvatar = mMultiUserSwitch.findViewById(R.id.multi_user_avatar);
        mUserInfoController = Dependency.get(UserInfoController.class);
        //*/

        //*/ freeme.gouzhouping, 20180424. weather conditions.
        mWeatherConditions = (TextView) findViewById(R.id.qs_header_weather);
        mTemperature = (TextView) findViewById(R.id.qs_header_tmp);
        mWeatherView = findViewById(R.id.weather_area);
        mWeatherView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startWeatherActivity();
            }
        });
        mWeatherConditions.setText(WeatherUtils.NO_WEATHER);
        //*/

        //*/ freeme.gouzhouping, 20180823. quick statusbar header ampm.
        mAmPmText = findViewById(id.am_pm);
        if (mIsShowAmPm) {
            updateAmPmVisible();
            ((Clock) mClock).setShowOriginalAmPm(false);
        }
        //*/
    }

    private void applyDarkness(int id, Rect tintArea, float intensity, int color) {
        View v = findViewById(id);
        if (v instanceof DarkReceiver) {
            ((DarkReceiver) v).onDarkChanged(tintArea, intensity, color);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResources();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        updateResources();
    }

    private void updateResources() {
        //*/ freeme.gouzhouping, 20180718. Notch.
        if (NotchUtils.hasNotch()) {
            setPadding(0, NotchUtils.getNotchHeight(),0,0);
        }
        //*/
    }

    public int getCollapsedHeight() {
        return getHeight();
    }

    public int getExpandedHeight() {
        return getHeight();
    }

    public void setExpanded(boolean expanded) {
        if (mExpanded == expanded) return;
        mExpanded = expanded;
        mHeaderQsPanel.setExpanded(expanded);
        updateEverything();
    }

    public void setExpansion(float headerExpansionFraction) {
        //*/ freeme.gouzhouping, 20180823. quick statusbar header ampm.
        if (mIsShowAmPm) {
            updateAmPmVisible();
        }
        //*/

        //*/ freeme.gouzhouping, 20180116. FreemeAppTheme, qs container.
        refreshUserView();
        //*/
    }

    @Override
    @VisibleForTesting
    public void onDetachedFromWindow() {
        setListening(false);
        super.onDetachedFromWindow();
    }

    public void setListening(boolean listening) {
        if (listening == mListening) {
            return;
        }
        mHeaderQsPanel.setListening(listening);
        mListening = listening;
        //*/ freeme.gouzhouping, 20180116. FreemeAppTheme, qs container.
        if (mListening) {
            mUserInfoController.addCallback(this);
        } else {
            mUserInfoController.removeCallback(this);
        }
        //*/
    }

    public void updateEverything() {
        post(() -> setClickable(false));

        //*/ freeme.gouzhouping, 20180116. FreemeAppTheme, qs container.
        refreshUserView();
        //*/
    }

    public void setQSPanel(final QSPanel qsPanel) {
        mQsPanel = qsPanel;
        setupHost(qsPanel.getHost());
        //*/ freeme.gouzhouping, 20180116. FreemeAppTheme, qs container.
        mMultiUserSwitch.setQsPanel(mQsPanel);
        //*/
    }

    public void setupHost(final QSTileHost host) {
        mHost = host;
        //host.setHeaderView(mExpandIndicator);
        mHeaderQsPanel.setQSPanelAndHeader(mQsPanel, this);
        mHeaderQsPanel.setHost(host, null /* No customization in header */);
    }

    public void setCallback(Callback qsPanelCallback) {
        mHeaderQsPanel.setCallback(qsPanelCallback);
    }

    //*/ freeme.gouzhouping, 20180116. FreemeAppTheme, qs container.
    private void startSettings() {
        if (!Dependency.get(DeviceProvisionedController.class).isCurrentUserSetup()) {
            mActivityStarter.postQSRunnableDismissingKeyguard(() -> { });
            return;
        }
        MetricsLogger.action(mContext,
                mExpanded ? MetricsProto.MetricsEvent.ACTION_QS_EXPANDED_SETTINGS_LAUNCH
                        : MetricsProto.MetricsEvent.ACTION_QS_COLLAPSED_SETTINGS_LAUNCH);
        if (mSettingsButton.isTunerClick()) {
            Dependency.get(ActivityStarter.class).postQSRunnableDismissingKeyguard(() -> {
                if (TunerService.isTunerEnabled(mContext)) {
                    TunerService.showResetRequest(mContext, () -> {
                        startSettingsActivity();
                    });
                } else {
                    Toast.makeText(getContext(), R.string.tuner_toast,
                            Toast.LENGTH_LONG).show();
                    TunerService.setTunerEnabled(mContext, true);
                }
                startSettingsActivity();
            });
        } else {
            startSettingsActivity();
        }
    }

    private void startSettingsActivity() {
        mActivityStarter.startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS),
                true /* dismissShade */);
    }

    private void startDeskClockActivity() {
        if (getStartName("config.qs.entry.clock") == null) {
            Log.i("QSBarHeader", " config clock not found ");
            return;
        }
        String[] str_clock = getStartName("config.qs.entry.clock");
        Intent intent = new Intent();
        intent.setClassName(str_clock[0], str_clock[1]);
        try {
            mActivityStarter.startActivity(intent, true);
        } catch (ActivityNotFoundException e) {
            Log.e("QSBarHeader", "startDeskClockActivity: activity not found, " + e);
        }
    }

    private void startCalendarActivity() {
        if (getStartName("config.qs.entry.date") == null) {
            Log.i("QSBarHeader", " config calendar not found ");
            return;
        }
        String[] str_calendar = getStartName("config.qs.entry.date");
        Intent intent = new Intent();
        intent.setClassName(str_calendar[0], str_calendar[1]);
        try {
            mActivityStarter.startActivity(intent, true);
        } catch (ActivityNotFoundException e) {
            Log.e("QSBarHeader", "startCalendarActivity: activity not found, " + e);
        }
    }

    private String[] getStartName(String startType) {
        String str = FreemeFeature.getLocal(startType);
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return str.split("/");
    }
    //*/

    //*/ freeme.gouzhouping, 20180424. weather conditions.
    private TextView mWeatherConditions;
    private TextView mTemperature;
    private View mWeatherView;

    private void startWeatherActivity() {
        String[] weatherEntry = getStartName("config.qs.entry.weather");

        if (weatherEntry == null) {
            Log.i("QSBarHeader", " config calendar not found ");
            return;
        }

        Intent intent = new Intent();
        intent.setClassName(weatherEntry[0], weatherEntry[1]);
        try {
            mActivityStarter.startActivity(intent, true);
        } catch (ActivityNotFoundException e) {
            Log.e("QSBarHeader", "startCalendarActivity: activity not found, " + e);
        }
    }

    @Override
    public void updateWeatherInfo(WeatherData weather) {
        if (weather != null && !TextUtils.isEmpty(weather.getWeatherType())
                && Integer.valueOf(weather.getWeatherType()) >= 0) {
            mWeatherConditions.setText(WeatherUtils.getInstance(mContext).getWeatherType(
                    Integer.valueOf(weather.getWeatherType())));
            mTemperature.setText(weather.getCurTemper() + WeatherUtils.TEMPURTURE_POSTFIX);
        } else {
            mWeatherConditions.setText(WeatherUtils.NO_WEATHER);
            mTemperature.setText("");
        }
    }
    //*/

    //*/ freeme.gouzhouping, 20180823. quick statusbar header ampm.
    private TextView mAmPmText;
    private boolean mIsShowAmPm;

    private void updateAmPmVisible() {
        int hour = Calendar.getInstance(Locale.getDefault()).get(Calendar.HOUR_OF_DAY);
        boolean is24 = DateFormat.is24HourFormat(
                getContext(), ActivityManager.getCurrentUser());
        mAmPmText.setText(hour > 12 ? getResources().getString(R.string.quick_status_bar_12_hour_pm)
                : getResources().getString(R.string.quick_status_bar_12_hour_am));
        mAmPmText.setVisibility(mIsShowAmPm ? is24 ? View.GONE : View.VISIBLE : View.GONE);
    }

    @Override
    public void onUserInfoChanged(String name, Drawable picture, String userAccount) {
        if (picture != null &&
                UserManager.get(mContext).isGuestUser(ActivityManager.getCurrentUser())) {
            picture = picture.getConstantState().newDrawable().mutate();
            picture.setColorFilter(
                    Utils.getColorAttr(mContext, android.R.attr.colorForeground),
                    PorterDuff.Mode.SRC_IN);
        }
        mMultiUserAvatar.setImageDrawable(picture);
    }

    private void refreshUserView() {
        mMultiUserSwitch.setVisibility(mMultiUserSwitch.hasMultipleUsers() ? View.VISIBLE : View.GONE);
    }
    //*/
}

package com.freeme.systemui.notch;

import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.keyguard.CarrierText;
import com.android.systemui.Dependency;
import com.android.systemui.R;

import com.android.systemui.statusbar.SignalClusterView;
import com.android.systemui.statusbar.policy.BatteryController;
import com.freeme.systemui.tint.TintTextView;
import com.freeme.systemui.utils.NotchUtils;

import java.text.NumberFormat;

public class NotchStatusBar extends LinearLayout implements BatteryController.BatteryStateChangeCallback {

    private static final String CARRIER_LABEL =
            com.freeme.systemui.statusbar.FreemeStatusbarStateToolKit.SHOW_CARRIER_LABEL;
    private final static String SHOW_BATTERY_LEVEL =
            com.freeme.systemui.statusbar.FreemeStatusbarStateToolKit.SHOW_BATTERY_LEVEL_SWITCH;

    private View mNotchCarrierLabel;
    private BatteryController mBatteryController;

    private TintTextView mNotchNetSpeed;
    private TextView mNotchBatLevel;
    private CarrierText mCarrierText;
    private int mTintColor = Color.WHITE;

    public NotchStatusBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mNotchCarrierLabel = findViewById(R.id.notch_carrier_area);
        mNotchNetSpeed = (TintTextView) findViewById(R.id.speed);
        ((ImageView) findViewById(R.id.battery_border)).setColorFilter(mTintColor, PorterDuff.Mode.MULTIPLY);
        ((ImageView) findViewById(R.id.battery_inside_level)).setColorFilter(mTintColor, PorterDuff.Mode.MULTIPLY);
        mNotchBatLevel = (TextView) findViewById(R.id.notch_battery_level);
        mCarrierText = (CarrierText) findViewById(R.id.carrier_text);

        mNotchNetSpeed.setIsResever(false);
        mNotchNetSpeed.setTextColor(mTintColor);
        mNotchBatLevel.setTextColor(mTintColor);
        ((SignalClusterView) findViewById(R.id.signal_cluster)).setQuickNotch(true);
        mCarrierText.setNotch(true);
        mCarrierText.setShowCarrier(true);
        mCarrierText.setTextColor(mTintColor);
    }

    private ContentObserver mNotchCarrierLabelObs = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            updateCarrierAreaVisible();
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mBatteryController = Dependency.get(BatteryController.class);
        mBatteryController.addCallback(this);
        mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CARRIER_LABEL),
                false, mNotchCarrierLabelObs);
        updateCarrierAreaVisible();

        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(SHOW_BATTERY_LEVEL),
                true, mBatteryLevelObserver, UserHandle.USER_ALL);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mBatteryController.removeCallback(this);
        mContext.getContentResolver().unregisterContentObserver(mNotchCarrierLabelObs);
        mContext.getContentResolver().unregisterContentObserver(mBatteryLevelObserver);
    }

    private ContentObserver mBatteryLevelObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateNotchBatteryVis();
        }
    };

    public void updateCarrierAreaVisible() {
        boolean isShow = Settings.System.getInt(getContext().getContentResolver(),
                CARRIER_LABEL, 0) != 0;
        mNotchCarrierLabel.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }

    private void updateNotchBatteryVis() {
        boolean isShow = Settings.System.getIntForUser(mContext.getContentResolver(),
                SHOW_BATTERY_LEVEL, 0, UserHandle.USER_CURRENT) == 1;
        boolean hasNotch = NotchUtils.hasNotch();
        mNotchBatLevel.setVisibility(isShow && hasNotch ? VISIBLE : GONE);
    }

    @Override
    public void onPowerSaveChanged(boolean isPowerSave) {
        // do nothing
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        String percentage = NumberFormat.getPercentInstance().format((double) level / 100.0);
        mNotchBatLevel.setText(percentage);
        updateNotchBatteryVis();
    }
}

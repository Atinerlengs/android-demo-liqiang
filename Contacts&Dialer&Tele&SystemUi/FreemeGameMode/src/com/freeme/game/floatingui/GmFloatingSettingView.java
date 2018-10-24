package com.freeme.game.floatingui;

import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.freeme.game.R;
import com.freeme.game.utils.GmNavBarUtils;
import com.freeme.game.utils.GmSettingsUtils;

public class GmFloatingSettingView extends AbsGmFloatingView
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private View mViewPort;
    private View mViewLand;

    private Switch mAnswerCallSwitch;
    private Switch mBlockNotifySwitch;
    private Switch mLockKeysSwitch;
    private Switch mBlockAutoBrightnessSwitch;

    private GmSettingsUtils mUtils;

    private class SettingUIConfig implements IGmFloatingUIConfig {

        @Override
        public void start() {
            mOrientationListener.enable();
        }

        @Override
        public void stop() {
            mOrientationListener.disable();
        }
    }
    private SettingUIConfig mSettingUIConfig;

    private class NavBarStatusChangeCallBack implements GmNavBarUtils.INavBarStatusChangedCallBack {
        @Override
        public void onNavBarStatusChanged() {
            initScreenSize();
            if (mParams != null) {
                initParams(mParams);
                updateViewLayout();
            }
        }
    }
    private NavBarStatusChangeCallBack mCallBack;

    GmFloatingSettingView(GmFloatingUIManager manager) {
        super(manager);
        mSettingUIConfig = new SettingUIConfig();
        mCallBack = new NavBarStatusChangeCallBack();
        manager.getNavBarUtils().registerNavBarCallBack(mCallBack);
        mUtils = new GmSettingsUtils();
    }

    @Override
    View inflate(LayoutInflater inflater) {
        View view;
        if (isLandScape()) {
            if (mViewLand == null) {
                mViewLand = inflater.inflate(R.layout.gm_floating_settings_view_land,
                        null);
            }
            view = mViewLand;
        } else {
            if (mViewPort == null) {
                mViewPort = inflater.inflate(R.layout.gm_floating_settings_view_port,
                        null);
            }
            view = mViewPort;
        }

        View answerCall = view.findViewById(R.id.game_mode_dnd_answer_call);
        mAnswerCallSwitch = view.findViewById(R.id.game_mode_dnd_answer_call_switch);
        View notifications = view.findViewById(R.id.game_mode_dnd_block_notifications);
        mBlockNotifySwitch = view.findViewById(R.id.game_mode_dnd_block_notifications_switch);
        View lockKeys = view.findViewById(R.id.game_mode_dnd_lock_keys);
        mLockKeysSwitch = view.findViewById(R.id.game_mode_dnd_lock_keys_switch);
        View brightness = view.findViewById(R.id.game_mode_dnd_auto_brightness);
        mBlockAutoBrightnessSwitch = view.findViewById(R.id.game_mode_dnd_auto_brightness_switch);

        setOnClickListener(view);
        setOnClickListener(answerCall);
        setOnClickListener(notifications);
        setOnClickListener(lockKeys);
        setOnClickListener(brightness);
        setOnCheckedChangeListener(mAnswerCallSwitch);
        setOnCheckedChangeListener(mBlockNotifySwitch);
        setOnCheckedChangeListener(mLockKeysSwitch);
        setOnCheckedChangeListener(mBlockAutoBrightnessSwitch);

        initSettings();

        return view;
    }

    @Override
    void initParams(WindowManager.LayoutParams params) {
        if (isLandScape()) {
            params.gravity = Gravity.END;
        } else {
            params.gravity = Gravity.BOTTOM;
        }
        params.width = mScreenW;
        params.height = mScreenH;
    }

    @Override
    void addViewToWindow() {
        super.addViewToWindow();
    }

    @Override
    void removeViewFromWindow() {
        removeViewFromWindow(true);
    }

    private void removeViewFromWindow(boolean notify) {
        super.removeViewFromWindow();
        mUIManager.getNavBarUtils().unRegisterNavBarCallBack(mCallBack);
        if (notify) {
            mUIManager.setFloatingViewVisible(GmFloatingUIManager.ViewType.VIEW_TYPE_FLOAT_BUTTON);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mMainView) {
            removeViewFromWindow();
        } else {
            switch (v.getId()) {
                case R.id.game_mode_dnd_answer_call:
                    mAnswerCallSwitch.setChecked(!mAnswerCallSwitch.isChecked());
                    break;
                case R.id.game_mode_dnd_block_notifications:
                    mBlockNotifySwitch.setChecked(!mBlockNotifySwitch.isChecked());
                    break;
                case R.id.game_mode_dnd_lock_keys:
                    mLockKeysSwitch.setChecked(!mLockKeysSwitch.isChecked());
                    break;
                case R.id.game_mode_dnd_auto_brightness:
                    mBlockAutoBrightnessSwitch.setChecked(!mBlockAutoBrightnessSwitch.isChecked());
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.game_mode_dnd_answer_call_switch:
                mUtils.setAnswerCallViaSpeaker(mContext, isChecked);
                break;
            case R.id.game_mode_dnd_block_notifications_switch:
                mUtils.setBlockNotification(mContext, isChecked);
                break;
            case R.id.game_mode_dnd_lock_keys_switch:
                mUtils.setLockKeys(mContext, isChecked);
                break;
            case R.id.game_mode_dnd_auto_brightness_switch:
                mUtils.setBlockAutoBrightness(mContext, isChecked);
                break;
            default:
                break;
        }
    }

    @Override
    void onOrientationChanged() {
        removeViewFromWindow(false);
        show();
    }

    @Override
    void registerGmFloatingUIAdapter() {
        mUIManager.registerGmFloatingUIAdapter(mSettingUIConfig);
    }

    @Override
    void unregisterGmFloatingUIAdapter() {
        mUIManager.unregisterGmFloatingUIAdapter(mSettingUIConfig);
    }

    private void setOnCheckedChangeListener(Switch view) {
        if (view != null) {
            view.setOnCheckedChangeListener(this);
        }
    }

    private void setOnClickListener(View view) {
        if (view != null) {
            view.setOnClickListener(this);
        }
    }

    private void initSettings() {
        if (mAnswerCallSwitch != null) {
            mAnswerCallSwitch.setChecked(mUtils.isAnswerCallViaSpeaker(mContext));
        }
        if (mBlockNotifySwitch != null) {
            mBlockNotifySwitch.setChecked(mUtils.isBlockedNotification(mContext));
        }
        if (mLockKeysSwitch != null) {
            mLockKeysSwitch.setChecked(mUtils.isLockedKeys(mContext, KeyEvent.KEYCODE_BACK)
                            || mUtils.isLockedKeys(mContext, KeyEvent.KEYCODE_APP_SWITCH));
        }
        if (mBlockAutoBrightnessSwitch != null) {
            mBlockAutoBrightnessSwitch.setChecked(mUtils.isBlockedAutoBrightness(mContext));
        }
    }
}

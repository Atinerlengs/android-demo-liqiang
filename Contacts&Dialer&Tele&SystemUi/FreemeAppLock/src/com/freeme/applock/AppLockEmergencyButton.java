package com.freeme.applock;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.telecom.TelecomManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import com.freeme.applock.service.AppLockCheckService.Callback;
import com.freeme.applock.settings.LogUtil;

public class AppLockEmergencyButton extends Button {
    private static final String TAG = "AppLockEmergencyButton";
    private static final String ACTION_EMERGENCY_DIAL = "com.android.phone.EmergencyDialer.DIAL";
    protected Callback mCallback;
    private Context mContext;
    private PowerManager mPowerManager;

    public AppLockEmergencyButton(Context context) {
        this(context, null);
    }

    public AppLockEmergencyButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                takeEmergencyCallAction();
            }
        });
    }

    public void takeEmergencyCallAction() {
        mPowerManager.userActivity(SystemClock.uptimeMillis(), true);
        if (isInCall()) {
            resumeCall();
            return;
        }
        launchEmergencyDialler();
        if (mCallback != null) {
            mCallback.onDismiss();
        }
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    private void launchEmergencyDialler() {
        Intent intent = new Intent(ACTION_EMERGENCY_DIAL);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        try {
            LogUtil.d(TAG, "launchEmergencyDialler");
            getContext().startActivityAsUser(intent, new UserHandle(0));
        } catch (ActivityNotFoundException e) {
            LogUtil.w(TAG, "Can't find the component " + e);
        }
    }

    private void resumeCall() {
        getTelecommManager().showInCallScreen(false);
    }

    private boolean isInCall() {
        return getTelecommManager().isInCall();
    }

    private TelecomManager getTelecommManager() {
        return (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
    }
}

package com.freeme.applock.service;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.widget.LockPatternUtils;

import com.freeme.applock.settings.AppLockUtil;
import com.freeme.internal.app.AppLockPolicy;
import com.freeme.applock.AppLockEmergencyButton;
import com.freeme.applock.AppLockUtils;
import com.freeme.applock.AppLockUtils.ClearPassViewReason;
import com.freeme.applock.AppLockViewStateCallback;
import com.freeme.applock.R;
import com.freeme.applock.service.AppLockCheckService.Callback;
import com.freeme.applock.settings.LogUtil;
import com.freeme.provider.FreemeSettings;

public class AppLockCheckBaseView extends FrameLayout implements OnKeyListener, AppLockViewStateCallback {
    private static final String TAG = "AppLockCheckBaseView";
    private static final String EMERGENCY_ACTION = "com.freeme.intent.action.EMERGENCY_START_SERVICE_BY_ORDER";
    private static final String LAUNCHER_RESUME = "com.freeme.intent.action.HOME_RESUME";

    protected ActivityManager mActivityManager;
    private ImageView mAppIcon;
    private LinearLayout mAppInfor;
    private TextView mAppLabel;
    private ApplicationInfo mApplicationInfo;
    protected Callback mCallback;
    protected CancellationSignal mCancelSignalFingerprint;
    protected boolean mCheckForFingerprint;
    private ViewGroup mContentLayout;
    protected Context mContext;
    protected String mEditTextPSW;
    protected AuthenticationCallback mFingerprintAuthenticationCallback;
    FingerprintManager mFingerprintManager;
    protected Intent mIntent;
    protected boolean mIsFingerprintLock;
    protected boolean mIsFingerprintRunning;
    protected boolean mIsFromResume;
    protected boolean mIsRequestToLock;
    protected boolean mIsShouldStartBiometrics;
    protected boolean mIsShouldStartFingerprint;
    protected boolean mIsShouldStartIris;
    protected boolean mIsStartFromAppLockSettings;
    protected LayoutInflater mLayoutInflater;
    protected LockPatternUtils mLockPatternUtils;
    protected int mLockType = AppLockPolicy.LOCK_TYPE_NONE;
    private PackageManager mPackageManager;
    protected String mPkgName;
    private PowerManager mPowerManager;
    protected SharedPreferences mPreferences;
    private boolean mReceiverRegistered;
    protected int mRotateState;
    private RelativeLayout mWholeLayout;
    private AppLockEmergencyButton mEmergencyBtn;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.w(TAG, "Action:" + intent.getAction() + ", clearPassView()");
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                    String reason = intent.getStringExtra(AppLockUtils.SYSTEM_DIALOG_REASON_KEY);
                    LogUtil.d(TAG, "ACTION_CLOSE_SYSTEM_DIALOGS, reason : " + reason);
                    if (AppLockUtils.SYSTEM_DIALOG_REASON_RECENT_APPS.equals(reason)
                            || AppLockUtils.SYSTEM_DIALOG_REASON_HOME_KEY.equals(reason)) {
                        LogUtil.d(TAG, "remove pass view with ACTION_CLOSE_SYSTEM_DIALOGS");
                        clearPassView(ClearPassViewReason.CLOSE_SYSTEM_DIALOG);
                    }
                } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                    LogUtil.d(TAG, "remove pass view with ACTION_SCREEN_OFF");
                    clearPassView(ClearPassViewReason.SCREEN_OFF);
                }
            }
        }
    };

    /*/ freeme.zhongkai.zhu. 20171027. applock
    BroadcastReceiver mEmergencyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.w(TAG, "mEmergencyReceiver, action:" + intent.getAction());
            String action = intent.getAction();
            if (AppLockCheckBaseView.EMERGENCY_ACTION.equals(action)) {
                removeAllLockedAppTask();
                clearPassView(ClearPassViewReason.EMERGENCY);
            }
        }
    };
    //*/

    public AppLockCheckBaseView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        LogUtil.i(TAG, "AppLockCheckBaseView ");
        mContext = context;
        int locktype = Secure.getInt(mContext.getApplicationContext().getContentResolver(),
                FreemeSettings.Secure.FREEME_APPLOCK_LOCK_TYPE, AppLockPolicy.LOCK_TYPE_NONE);
        if (locktype >= AppLockPolicy.LOCK_TYPE_FINGERPRINT_PATTERN
                && locktype <= AppLockPolicy.LOCK_TYPE_FINGERPRINT_PASSWORD) {
            mCheckForFingerprint = true;
        }
        setFocusableInTouchMode(true);
        setOnKeyListener(this);
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mPackageManager = mContext.getPackageManager();
        mLockPatternUtils = new LockPatternUtils(mContext);
        mLockType = AppLockUtils.getAppLockType(mContext);
        mIsFingerprintLock = AppLockUtils.isFingerprintLock(mLockType);
        mFingerprintManager = (FingerprintManager) mContext.getSystemService(Context.FINGERPRINT_SERVICE);
        if (mIsFingerprintLock && mFingerprintManager != null) {
            mIsShouldStartFingerprint = mFingerprintManager.isHardwareDetected()
                    && mFingerprintManager.hasEnrolledFingerprints();
        }
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        AppLockUtils.initFailedUnlockAttemptsFromPrefrence(mPreferences);
        mRotateState = getScreenOrientation();
        Intent intent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS).putExtra("reason", "applock_checkview");
        mContext.sendBroadcast(intent);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        mContext.registerReceiver(mReceiver, filter);
        IntentFilter emergencyFilter = new IntentFilter();
        emergencyFilter.addAction(EMERGENCY_ACTION);
        /*/ freeme.zhongkai.zhu. 20171027. applock
        mContext.registerReceiver(mEmergencyReceiver, emergencyFilter,
                "com.sec.android.emergencymode.permission.LAUNCH_EMERGENCYMODE_SERVICE", null);
        //*/
        mReceiverRegistered = true;

        AppLockUtil.setNavigationBarDisableRecent(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        LogUtil.d(TAG, "onAttachedToWindow");
        disableSystemKey();
        startFingerprintAuthentication();
        updateAppInfor();
    }

    @Override
    protected void onDetachedFromWindow() {
        LogUtil.d(TAG, "onDetachedFromWindow");
        resumeSystemKey();
        stopFingerprintAuthentication();
        super.onDetachedFromWindow();
    }

    protected void onDestroy() {
        LogUtil.d(TAG, "onDestroy");
        hideVirtualKeypad();
        if (mReceiverRegistered) {
            mContext.unregisterReceiver(mReceiver);
            /*/ freeme.zhongkai.zhu. 20171027. applock
            mContext.unregisterReceiver(mEmergencyReceiver);
            //*/
            mReceiverRegistered = false;
        }
    }

    protected void onResume() {
        LogUtil.d(TAG, "onResume");
    }

    private boolean canStartFingerprintAuth() {
        return mIsShouldStartFingerprint
                && mPowerManager != null && mPowerManager.isInteractive()
                && AppLockUtils.getRemaingTimeToUnlock() == 0;
    }

    protected void startFingerprintAuthentication() {
        if (!mIsFingerprintRunning && canStartFingerprintAuth()) {
            mIsFingerprintRunning = true;
            if (mFingerprintManager == null) {
                mFingerprintManager = (FingerprintManager) mContext.getSystemService(Context.FINGERPRINT_SERVICE);
            }
            mCancelSignalFingerprint = new CancellationSignal();
            mFingerprintAuthenticationCallback = new AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(AuthenticationResult result) {
                    verifySuccess();
                }

                @Override
                public void onAuthenticationAcquired(int acquireInfo) {
                    LogUtil.d(TAG, "mFingerprintAuthenticationCallback onAuthenticationAcquired");
                    super.onAuthenticationAcquired(acquireInfo);
                }

                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    LogUtil.d(TAG, "onAuthenticationError errString " + errString + " errorCode " + errorCode);
                    if (errorCode != FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
                        /*/ freeme.zhongkai.zhu. 20171027. fingerprint
                        switch (errorCode) {
                            case 1002:
                            case 1003:
                                errString = mContext.getString(R.string.applock_finger_print_not_responding_error_message);
                                break;
                            case 1004:
                                errString = mContext.getString(R.string.applock_finger_print_database_error_message);
                                break;
                            case 1005:
                                errString = mContext.getString(R.string.applock_finger_print_sensor_changed_error_message);
                                break;
                        }
                        //*/
                        Toast.makeText(mContext, errString, Toast.LENGTH_SHORT).show();
                        super.onAuthenticationError(errorCode, errString);
                    }
                }

                @Override
                public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                    LogUtil.d(TAG, "onAuthenticationHelp helpString " + helpString + " helpCode " + helpCode);
                    updateHelpText(false, (String) helpString);
                    super.onAuthenticationHelp(helpCode, helpString);
                }

                @Override
                public void onAuthenticationFailed() {
                    LogUtil.d(TAG, "mFingerprintAuthenticationCallback onAuthenticationFailed");
                    AppLockUtils.reportFailedUnlockAttempts();
                    if (AppLockUtils.getFailedUnlockAttempts() >= 5) {
                        AppLockUtils.setRemaingTimeToUnlock();
                        handleAttemptLockout(AppLockUtils.getRemaingTimeToUnlock());
                    }
                    updateHelpText(false, getResources().getString(R.string.iris_no_match));
                    super.onAuthenticationFailed();
                }
            };
            Bundle fingerPrintBundle = new Bundle();
            fingerPrintBundle.putInt("privileged_attr", 6);
            mFingerprintManager.authenticate(null, mCancelSignalFingerprint, 0, mFingerprintAuthenticationCallback, null);
        }
    }

    protected void stopFingerprintAuthentication() {
        if (mIsFingerprintRunning) {
            mIsFingerprintRunning = false;
            if (mCancelSignalFingerprint != null) {
                mCancelSignalFingerprint.cancel();
            }
            mCancelSignalFingerprint = null;
            mFingerprintAuthenticationCallback = null;
        }
    }

    private void updateAppInfor() {
        if (mContentLayout == null) {
            mContentLayout = (ViewGroup) findViewById(R.id.content_layout);
        }
        if (mWholeLayout == null) {
            mWholeLayout = (RelativeLayout) findViewById(R.id.whole_layout);
        }
        if (mAppInfor == null) {
            mAppInfor = (LinearLayout) findViewById(R.id.app_infor);
        }
        if (mAppIcon == null) {
            mAppIcon = (ImageView) findViewById(R.id.app_icon);
        }
        if (mAppLabel == null) {
            mAppLabel = (TextView) findViewById(R.id.app_label);
        }
        if (mEmergencyBtn == null) {
            mEmergencyBtn = (AppLockEmergencyButton) findViewById(R.id.app_lock_emergency_button);
        }
        if (mEmergencyBtn != null) {
            mEmergencyBtn.setCallback(mCallback);
        }
        if (!(mPackageManager == null || mPkgName == null)) {
            try {
                mApplicationInfo = mPackageManager.getApplicationInfo(mPkgName, 0);
                if (mApplicationInfo != null) {
                    if (mAppIcon != null) {
                        mAppIcon.setImageDrawable(mApplicationInfo.loadIcon(mPackageManager));
                    }
                    if (mAppLabel != null) {
                        mAppLabel.setText(mApplicationInfo.loadLabel(mPackageManager));
                    }
                }
            } catch (NameNotFoundException e) {
                LogUtil.d(TAG, "ApplicationInfo not found " + mPkgName);
            }
        }
        if (mIntent != null) {
            ResolveInfo resolveInfo = mPackageManager.resolveActivity(mIntent, 0);
            if (resolveInfo != null && checkCurrentActivityIsMain(mPkgName, resolveInfo.activityInfo.name)) {
                Drawable lockedIcon = resolveInfo.loadIcon(mPackageManager);
                CharSequence lockedLabel = resolveInfo.loadLabel(mPackageManager);
                if (!(mAppIcon == null || lockedIcon == null)) {
                    mAppIcon.setImageDrawable(lockedIcon);
                }
                if (!(mAppLabel == null || TextUtils.isEmpty(lockedLabel))) {
                    mAppLabel.setText(lockedLabel);
                }
            }
        }
        showAppInfor();
    }

    private boolean checkCurrentActivityIsMain(String pkgName, String className) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(pkgName);
        for (ResolveInfo info : mPackageManager.queryIntentActivities(intent, 0)) {
            if (info.activityInfo.name.equals(className)) {
                return true;
            }
        }
        return false;
    }

    protected void showAppInfor() {
        LogUtil.w(TAG, "showAppInfor !! mAppInfor " + mAppInfor);
        if (mAppInfor != null) {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(mContentLayout.getLayoutParams());
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;
            mContentLayout.setLayoutParams(lp);
            mWholeLayout.setBackgroundColor(0xfffafafa);
            mAppInfor.setVisibility(View.VISIBLE);
            if (mIntent != null && mEmergencyBtn != null) {
                ResolveInfo ri = mPackageManager.resolveActivity(mIntent, 0);
                if (ri == null || ri.activityInfo == null
                        || !"com.android.dialer.DialtactsActivity".equals(ri.activityInfo.name)) {
                    mEmergencyBtn.setVisibility(View.GONE);
                } else {
                    mEmergencyBtn.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public void setIntent(Intent intent) {
        mIntent = intent.getParcelableExtra(AppLockPolicy.LOCKED_PACKAGE_INTENT);
        mPkgName = intent.getStringExtra(AppLockPolicy.LOCKED_PACKAGE_NAME);
        mIsFromResume = intent.getBooleanExtra(AppLockPolicy.LAUNCH_FROM_RESUME, false);
        mIsRequestToLock = AppLockUtils.KEY_REQUEST_LOCK.equals(intent.getStringExtra(AppLockUtils.KEY_REQUEST_ACTION));
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void setPassword(String entry) {
        mEditTextPSW = entry;
    }

    public void onBackPressed() {
        LogUtil.d(TAG, "onBackPressed mIsFromResume:" + mIsFromResume);
        if (!mIsFromResume) {
            return;
        }
        LogUtil.d(TAG, "onBackPressed remove current Task");
        removeAppTask();
        removePassView();
    }

    protected int getScreenOrientation() {
        return getResources().getConfiguration().orientation;
    }

    protected boolean isPortrait() {
        return getScreenOrientation() == Configuration.ORIENTATION_PORTRAIT;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        LogUtil.d(TAG, "onWindowFocusChanged: " + hasFocus);
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        LogUtil.d(TAG, "onConfigurationChanged");
        mRotateState = newConfig.orientation;
        switch (newConfig.orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                LogUtil.d(TAG, "onConfigurationChanged ORIENTATION_PORTRAIT");
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                LogUtil.d(TAG, "onConfigurationChanged ORIENTATION_LANDSCAPE");
                break;
        }
        mCallback.onOrientationChanged(mRotateState);
        super.onConfigurationChanged(newConfig);
    }

    protected void hideVirtualKeypad() {
        InputMethodManager inputManager =
                (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputManager != null) {
            LogUtil.d(TAG, "hideVirtualKeypad");
            /*/ freeme.zhongkai.zhu. 20171027. applock
            inputManager.forceHideSoftInput();
            //*/
        }
    }

    private void disableSystemKey() {
        requestSystemKeyEvent(KeyEvent.KEYCODE_APP_SWITCH /*187*/, true);
    }

    private void resumeSystemKey() {
        requestSystemKeyEvent(KeyEvent.KEYCODE_APP_SWITCH, false);
    }

    private boolean requestSystemKeyEvent(int keyCode, boolean request) {
        LogUtil.d(TAG, " requestSystemKeyEvent " + getClass());
        /*/ freeme.zhongkai.zhu. 20171027. applock
        try {
            return Stub.asInterface(ServiceManager.getService(Context.WINDOW_SERVICE))
                    .requestSystemKeyEvent(keyCode,
                            new ComponentName(AppLockUtils.APP_LOCK_CHECKVIEW_WIN_PARAM_PACKAGE_NAME,
                                    AppLockUtils.APP_LOCK_CHECKVIEW_WIN_PARAM_CLASS_NAME), request);
        } catch (RemoteException e) {
            LogUtil.d(TAG, "requestSystemKeyEvent - " + e);
            return false;
        }
        /*/
        return false;
        //*/
    }

    private void stopVerifying() {
        stopFingerprintAuthentication();
    }

    protected void verifySuccess() {
        if (mActivityManager == null) {
            mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        }
        if (mActivityManager != null) {
            mActivityManager.setAppLockedUnLockPackage(mPkgName);
            LogUtil.i(TAG, "setAppLockedUnLockPackage mPkgName = " + mPkgName);
        }
        AppLockUtils.resetFailedUnlockNBackupAttempts();
        stopVerifying();
        sendVerifyStatusBroadcast(true);
        mCallback.onDismiss();
    }

    private void sendVerifyStatusBroadcast(boolean success) {
        Bundle statusBundle = new Bundle();
        statusBundle.putBoolean(AppLockUtils.APPLOCK_STATUS, success);
        statusBundle.putString(AppLockUtils.APPLOCK_STATUS_UNLOCKED_PACKAGE, mPkgName);
        AppLockUtils.sendStatusBroadcast(mContext, statusBundle,
                AppLockUtils.APPLOCK_STATUS_CHANGED_ACTION);
    }

    private void clearPassView(ClearPassViewReason reason) {
        sendVerifyStatusBroadcast(false);
        mCallback.clearPassView(reason);
    }

    protected void showBackupView() {
        mCallback.showBackupView(mLockType);
    }

    protected void removePassView() {
        sendVerifyStatusBroadcast(false);
        mCallback.onDismiss();
    }

    private void goHome() {
        AppLockUtils.sendHomekey();
    }

    private void removeAppTask() {
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> runningTasks = am.getRunningTasks(10);
        for (int i = 0; i < runningTasks.size(); i++) {
            String pkgName = runningTasks.get(i).topActivity.getPackageName();
            LogUtil.i(TAG, "removeAppTask RunningTaskInfo:" + runningTasks.get(i));
            LogUtil.i(TAG, "removeAppTask pkgName:" + runningTasks.get(i).topActivity.getPackageName());
            if (pkgName != null && pkgName.equals(mPkgName)) {
                LogUtil.i(TAG, "removeAppTask TaskId:" + runningTasks.get(i).id
                        + " result:" + am.removeTask(runningTasks.get(i).id));
                return;
            }
        }
    }

    private void removeAllLockedAppTask() {
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> runningTasks = am.getRunningTasks(10);
        for (int i = 0; i < runningTasks.size(); i++) {
            String pkgName = runningTasks.get(i).topActivity.getPackageName();
            LogUtil.i(TAG, "removeAllLockedAppTask RunningTaskInfo:" + runningTasks.get(i));
            LogUtil.i(TAG, "removeAllLockedAppTask pkgName:" + runningTasks.get(i).topActivity.getPackageName());
            if (pkgName != null && am.isAppLockedPackage(pkgName)) {
                LogUtil.i(TAG, "removeAllLockedAppTask TaskId:" + runningTasks.get(i).id
                        + " result:" + am.removeTask(runningTasks.get(i).id));
            }
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            LogUtil.d(TAG, "onKey() Down");
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    onBackPressed();
                    return true;
            }
        }
        return false;
    }

    @Override
    public void updateView() {
    }

    @Override
    public void succeedVerify() {
        verifySuccess();
    }

    @Override
    public void failVerify() {
    }

    protected void onCountDownFinished() {
        startFingerprintAuthentication();
    }

    protected void handleAttemptLockout(long elapsedRealtimeDeadline) {
        stopVerifying();
    }

    protected void updateHelpText(boolean isFaild, String helpString) {
    }
}

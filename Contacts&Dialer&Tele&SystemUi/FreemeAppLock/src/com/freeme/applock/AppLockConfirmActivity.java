package com.freeme.applock;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.KeyguardManager;
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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.IWindowManager.Stub;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.widget.LockPatternUtils;

import com.freeme.applock.settings.AppLockUtil;
import com.freeme.internal.app.AppLockPolicy;
import com.freeme.applock.AppLockUtils.ActivityCycle;
import com.freeme.applock.settings.LogUtil;

public class AppLockConfirmActivity extends Activity implements AppLockViewStateCallback {
    private static final String TAG = "AppLockConfirmActivity";

    private boolean isNeedFinishOnScreenOff;
    protected boolean isVerifySuccess;
    protected ActivityManager mActivityManager;
    private ActivityCycle mActivityStatus = ActivityCycle.UNKNOWN;
    private ImageView mAppIcon;
    private LinearLayout mAppInfor;
    private TextView mAppLabel;
    private ApplicationInfo mApplicationInfo;
    protected CancellationSignal mCancelSignalFingerprint;
    private ViewGroup mContentLayout;
    protected Bitmap mDrawableAppIcon;
    private AppLockEmergencyButton mEmergencyBtn;
    protected AuthenticationCallback mFingerprintAuthenticationCallback;
    FingerprintManager mFingerprintManager;
    private boolean mHasWindowFocus;
    protected Intent mIntent;
    protected boolean mIsFingerprintLock;
    protected boolean mIsFingerprintRunning;
    protected boolean mIsFromResume;
    protected boolean mIsRequestToLock;
    protected boolean mIsShouldStartBiometrics;
    protected boolean mIsShouldStartFingerprint;
    protected boolean mIsShouldStartIris;
    protected boolean mIsStartFromAppLockSettings;
    private KeyguardManager mKeyguardManager;
    protected LockPatternUtils mLockPatternUtils;
    protected int mLockType = AppLockPolicy.LOCK_TYPE_NONE;
    private PackageManager mPackageManager;
    protected String mPkgName;
    private PowerManager mPowerManager;
    protected SharedPreferences mPreferences;
    private boolean mReceiverRegistered;
    protected LayoutParams mRestoreWindowLayoutParams;
    protected String mStringAppLable;
    private RelativeLayout mWholeLayout;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                    String reason = intent.getStringExtra("reason");
                    LogUtil.d(TAG, "receive ACTION_CLOSE_SYSTEM_DIALOGS, reason:" + reason);
                    if ("recentapps".equals(reason) || "homekey".equals(reason)) {
                        LogUtil.d(TAG, "finish " + action);
                        sendVerifyStatusBroadcast(false);
                        finish();
                        AppLockUtils.mApplicationVerifySet.clear();
                    }
                } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                    if (isNeedFinishOnScreenOff && mPowerManager != null && mPowerManager.isInteractive()) {
                        isNeedFinishOnScreenOff = false;
                        LogUtil.d(TAG, "finish " + action);
                        sendVerifyStatusBroadcast(false);
                        finish();
                    }
                } else if ((action.equals(Intent.ACTION_USER_PRESENT) || action.equals(Intent.ACTION_SCREEN_ON))
                        && mActivityManager != null && mPkgName != null
                        && getIntent().getBooleanExtra(AppLockUtils.CONFIRM_ACTIVITY_NO_NEED_SHOW_ON_KEYGUARD, false)) {
                    boolean isTargetAppDead = true;
                    for (RunningTaskInfo info : mActivityManager.getRunningTasks(2)) {
                        if (mPkgName.equals(info.topActivity.getPackageName())) {
                            isTargetAppDead = false;
                            break;
                        }
                    }
                    if (isTargetAppDead) {
                        LogUtil.d(TAG, "Finish, TargetAppDead: " + mPkgName);
                        AppLockUtils.mApplicationVerifySet.remove(mPkgName);
                        sendVerifyStatusBroadcast(false);
                        finish();
                    }
                }
            }
        }
    };

    /*/ freeme.zhongkai.zhu. 20171027. applock
    BroadcastReceiver mEmergencyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                LogUtil.w(TAG, "mEmergencyReceiver, action:" + action + ", finish()");
                removeAppTask();
                sendVerifyStatusBroadcast(false);
                finish();
                AppLockUtils.mApplicationVerifySet.clear();
            }
        }
    };
    //*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityStatus = ActivityCycle.ON_CREATE;
        if (savedInstanceState != null) {
            mRestoreWindowLayoutParams = savedInstanceState.getParcelable(
                    AppLockUtils.CONFIRM_ACTIVITY_WINDOW_ATTRIBUTE_LAYOUT_PARAMS);
        }
        LogUtil.i(TAG, "onCreate Intent = " + getIntent());
        mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        mPackageManager = getPackageManager();
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mLockType = AppLockUtils.getAppLockType(getApplicationContext());
        mIsFingerprintLock = AppLockUtils.isFingerprintLock(mLockType);
        mFingerprintManager = (FingerprintManager) getApplicationContext().getSystemService(
                Context.FINGERPRINT_SERVICE);
        if (mIsFingerprintLock && mFingerprintManager != null) {
            mIsShouldStartFingerprint = mFingerprintManager.isHardwareDetected()
                    && mFingerprintManager.hasEnrolledFingerprints();
        }
        onLoadIntent(getIntent());
        mLockPatternUtils = new LockPatternUtils(this);
        isVerifySuccess = false;
        super.setTheme(R.style.AppLockActivityThemeWhite);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        AppLockUtils.initFailedUnlockAttemptsFromPrefrence(mPreferences);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mReceiver, filter);
        /*/ freeme.zhongkai.zhu. 20171027. applock
        IntentFilter emergencyFilter = new IntentFilter();
        emergencyFilter.addAction(EMERGENCY_ACTION);
        registerReceiver(mEmergencyReceiver, emergencyFilter,
                "com.sec.android.emergencymode.permission.LAUNCH_EMERGENCYMODE_SERVICE", null);
        //*/
        mReceiverRegistered = true;

        Window window = getWindow();
        View decorView = window.getDecorView();
        AppLockUtil.setNavigationBarDisableRecent(decorView);

        decorView.setSystemUiVisibility(decorView.getSystemUiVisibility()
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.setStatusBarColor(Color.TRANSPARENT);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        onLoadIntent(intent);
    }

    protected void isAppVerifying() {
        LogUtil.w(TAG, "isAppVerifying mPkgName = " + mPkgName
                + "Set:" + AppLockUtils.mApplicationVerifySet.get(mPkgName));
        if (mPkgName != null && AppLockUtils.isAppChecking(mPkgName)) {
            LogUtil.w(TAG, "isAppVerifying !!");
            if (AppLockUtils.mApplicationVerifySet.containsKey(mPkgName)) {
                Activity activity = (Activity) AppLockUtils.mApplicationVerifySet.get(mPkgName);
                AppLockUtils.mApplicationVerifySet.remove(mPkgName);
                activity.finish();
            } else if (AppLockUtils.getCheckViewShowingState()) {
                finish();
            } else {
                AppLockUtils.lockedPackageQueueRemove(mPkgName);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mActivityStatus = ActivityCycle.ON_START;
        disableSystemKey();
        LogUtil.d(TAG, "==onStart==");
    }

    private boolean canStartFingerprintAuth() {
        return mIsShouldStartFingerprint && mHasWindowFocus
                && mPowerManager != null && mPowerManager.isInteractive()
                && AppLockUtils.getRemaingTimeToUnlock() == 0;
    }

    protected void startFingerprintAuthentication() {
        if (!mIsFingerprintRunning && canStartFingerprintAuth()) {
            mIsFingerprintRunning = true;
            if (mFingerprintManager == null) {
                mFingerprintManager = (FingerprintManager) getApplicationContext()
                        .getSystemService(Context.FINGERPRINT_SERVICE);
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
                    if (FingerprintManager.FINGERPRINT_ERROR_CANCELED != errorCode) {
                        /*/ freeme.zhongkai.zhu. 20171027. fingerprint
                        if (1004 == errorCode) {
                            errString = getResources().getString(R.string.applock_finger_print_database_error_message);
                        } else if (1002 == errorCode || 1003 == errorCode) {
                            errString = getResources().getString(R.string.applock_finger_print_not_responding_error_message);
                        } else if (1001 == errorCode) {
                            errString = getResources().getString(R.string.applock_finger_print_sensor_with_recalibration_error_message);
                        } else if (1005 == errorCode) {
                            errString = getResources().getString(R.string.applock_finger_print_sensor_changed_error_message);
                        }
                        //*/
                        Toast.makeText(getApplicationContext(), errString, Toast.LENGTH_SHORT).show();
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
            /*/ freeme.zhongkai.zhu. 20171027. fingerprint
            Bundle fingerPrintBundle = new Bundle();
            fingerPrintBundle.putInt("privileged_attr", 6);
            mFingerprintManager.authenticate(null, mCancelSignalFingerprint, 0, mFingerprintAuthenticationCallback,
                    null, UserHandle.myUserId(), fingerPrintBundle);
            /*/
            mFingerprintManager.authenticate(null, mCancelSignalFingerprint, 0, mFingerprintAuthenticationCallback, null);
            //*/
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

    @Override
    protected void onResume() {
        super.onResume();
        mActivityStatus = ActivityCycle.ON_RESUME;
        LogUtil.w(TAG, "mApplicationVerifySet Put:" + this + " mPkgName:" + mPkgName);
        startFingerprintAuthentication();
        if (mPkgName != null) {
            AppLockUtils.applicationVerifySetPut(mPkgName, this);
        }
        if (mPkgName != null) {
            mActivityManager.setAppLockedVerifying(mPkgName, false);
        }
        if (isNeedFinishOnScreenOff) {
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                }
            }, 500);
        }
        updateAppInfor();
    }

    @Override
    public void onPause() {
        super.onPause();
        mActivityStatus = ActivityCycle.ON_PAUSE;
        stopFingerprintAuthentication();
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
        if (mApplicationInfo != null) {
            if (mAppIcon != null) {
                mAppIcon.setImageDrawable(mApplicationInfo.loadIcon(mPackageManager));
            }
            if (mAppLabel != null) {
                mAppLabel.setText(mApplicationInfo.loadLabel(mPackageManager));
            }
            if (mIntent != null) {
                ResolveInfo resolveInfo = mPackageManager.resolveActivity(mIntent, 0);
                if (resolveInfo != null && checkCurrentActivityIsMain(mPkgName, resolveInfo.activityInfo.name)) {
                    Drawable lockedIcon = resolveInfo.loadIcon(mPackageManager);
                    String lockedLabel = resolveInfo.loadLabel(mPackageManager).toString();
                    if (mAppIcon != null && lockedIcon != null) {
                        mAppIcon.setImageDrawable(lockedIcon);
                    }
                    if (mAppLabel != null && !TextUtils.isEmpty(lockedLabel)) {
                        mAppLabel.setText(lockedLabel);
                    }
                }
            }
            showAppInfor();
        } else if (mIsStartFromAppLockSettings) {
            hideAppInfor();
        } else {
            mAppIcon.setImageBitmap(mDrawableAppIcon);
            mAppLabel.setText(mStringAppLable);
            showAppInfor();
        }
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
        LogUtil.w(TAG, "showAppInfor !!");
        if (isInMultiWindowMode() &&
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (mAppInfor != null) {
                mAppInfor.setVisibility(View.GONE);
            }
            return;
        }

        if (mAppLabel != null) {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    (RelativeLayout.LayoutParams) mContentLayout.getLayoutParams());
            lp.height = LayoutParams.MATCH_PARENT;
            mContentLayout.setLayoutParams(lp);
            mWholeLayout.setBackgroundColor(0xfffafafa);
            mAppInfor.setVisibility(View.VISIBLE);
            if (mIntent != null && mEmergencyBtn != null) {
                ResolveInfo ri = mPackageManager.resolveActivity(mIntent, 0);
                if (ri == null || ri.activityInfo == null
                        || !("com.android.dialer.DialtactsActivity".equals(ri.activityInfo.name)
                        ||"com.freeme.dialer.app.FreemeDialtactsActivity".equals(ri.activityInfo.name))) {
                    mEmergencyBtn.setVisibility(View.GONE);
                } else {
                    mEmergencyBtn.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    protected void hideAppInfor() {
        LogUtil.w(TAG, "hideAppInfor !!");
        if (mWholeLayout != null) {
            mWholeLayout.setBackgroundColor(0x4d000000);
        }
        if (mAppInfor != null) {
            mAppInfor.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(AppLockUtils.CONFIRM_ACTIVITY_WINDOW_ATTRIBUTE_LAYOUT_PARAMS,
                getWindow().getAttributes());
        super.onSaveInstanceState(outState);
    }

    private void onLoadIntent(Intent intent) {
        mIsRequestToLock = AppLockUtils.KEY_REQUEST_LOCK.equals(intent.getStringExtra(AppLockUtils.KEY_REQUEST_ACTION));
        if (mPkgName != null && AppLockUtils.mApplicationVerifySet.get(mPkgName) == this) {
            LogUtil.w(TAG, "onLoadIntent remove old pkg = " + mPkgName);
            AppLockUtils.mApplicationVerifySet.remove(mPkgName);
        }
        mIsFromResume = intent.getBooleanExtra(AppLockPolicy.LAUNCH_FROM_RESUME, false);
        mIntent = intent.getParcelableExtra(AppLockPolicy.LOCKED_PACKAGE_INTENT);
        LogUtil.d(TAG, "onCreate mIntent:" + mIntent);
        if (mIntent != null) {
            mIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT | Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
        }
        mPkgName = intent.getStringExtra(AppLockPolicy.LOCKED_PACKAGE_NAME);
        mIsStartFromAppLockSettings = intent.getBooleanExtra(AppLockPolicy.LAUNCH_FROM_SETTINGS, false);
        try {
            mApplicationInfo = mPackageManager.getApplicationInfo(mPkgName, 0);
        } catch (NameNotFoundException e) {
            LogUtil.d(TAG, "mPkgName ApplicationInfo not found" + mPkgName);
        }
        Window win = getWindow();
        if (mRestoreWindowLayoutParams == null) {
            LogUtil.d(TAG, "mRestoreWindowLayoutParams " + mRestoreWindowLayoutParams);
            LayoutParams lp = win.getAttributes();
            boolean noNeedShowWhenLocked = intent.getBooleanExtra(AppLockUtils.CONFIRM_ACTIVITY_NO_NEED_SHOW_ON_KEYGUARD, false);
            if (!(mKeyguardManager == null || !mKeyguardManager.isKeyguardLocked() || noNeedShowWhenLocked)) {
                isNeedFinishOnScreenOff = true;
            }
            win.setAttributes(lp);
            return;
        }
        win.setAttributes(mRestoreWindowLayoutParams);
    }

    @Override
    public void onBackPressed() {
        LogUtil.d(TAG, "onBackPressed mIsFromResume:" + mIsFromResume);
        if (mIsFromResume) {
            LogUtil.d(TAG, "onBackPressed removeTask.");
            removeAppTask();
        }
        AppLockUtils.mApplicationVerifySet.remove(mPkgName);
        sendVerifyStatusBroadcast(false);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        LogUtil.w(TAG, "onDestroy this = " + this);
        if (mReceiverRegistered) {
            unregisterReceiver(mReceiver);
            /*/ freeme.zhongkai.zhu. 20171027. applock
            unregisterReceiver(mEmergencyReceiver);
            //*/
            mReceiverRegistered = false;
        }
        for (String pkg : AppLockUtils.mApplicationVerifySet.keySet()) {
            LogUtil.w(TAG, "onDestroy pkg = " + pkg);
            LogUtil.w(TAG, "onDestroy pkg this = " + AppLockUtils.mApplicationVerifySet.get(pkg));
        }
        if (mPkgName != null && AppLockUtils.mApplicationVerifySet.get(mPkgName) == this) {
            LogUtil.w(TAG, "onDestroy remove pkg = " + mPkgName);
            AppLockUtils.mApplicationVerifySet.remove(mPkgName);
        }
        if (!isVerifySuccess) {
            if (mIsStartFromAppLockSettings) {
                setResult(RESULT_CANCELED);
            }
        }
        super.onDestroy();
        mActivityStatus = ActivityCycle.ON_DESTRONY;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        LogUtil.d(TAG, "onWindowFocusChanged: " + hasFocus);
        mHasWindowFocus = hasFocus;
        if (mHasWindowFocus) {
            startFingerprintAuthentication();
        } else {
            stopFingerprintAuthentication();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mActivityStatus = ActivityCycle.ON_STOP;
        resumeSystemKey();
        LogUtil.d(TAG, "==onStop==");
    }

    @Override
    public void finish() {
        hideVirtualKeypad();
        super.finish();
    }

    protected void hideVirtualKeypad() {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getCurrentFocus() != null && inputManager != null) {
            LogUtil.i(TAG, "forceHideSoftInput");
            // FIXME
            // inputManager.forceHideSoftInput();
        }
    }

    private boolean requestSystemKeyEvent(int keyCode, boolean request) {
        /*/ freeme.zhongkai.zhu. 20171027. applock
        try {
            return Stub.asInterface(ServiceManager.getService("window"))
                        .requestSystemKeyEvent(keyCode, getComponentName(), request);
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
        isVerifySuccess = true;
        if (mActivityManager == null) {
            mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        }
        if (!(mPkgName == null || mPkgName.isEmpty())) {
            mActivityManager.setAppLockedUnLockPackage(mPkgName);
            LogUtil.i(TAG, "setAppLockedUnLockPackage mPkgName = " + mPkgName);
        }
        AppLockUtils.resetFailedUnlockNBackupAttempts();
        stopVerifying();
        sendVerifyStatusBroadcast(true);
        if (mIsStartFromAppLockSettings) {
            setResult(RESULT_OK);
        } else if (mIsFromResume) {
            LogUtil.i(TAG, "verifySuccess from resume, finish directly.");
        } else if (mIntent != null) {
            LogUtil.i(TAG, "verifySuccess start Intent = " + mIntent);
            startActivity(mIntent);
        } else {
            LogUtil.i(TAG, "verifySuccess but do nothing.");
        }
        finish();
    }

    private void sendVerifyStatusBroadcast(boolean success) {
        Bundle statusBundle = new Bundle();
        statusBundle.putBoolean(AppLockUtils.APPLOCK_STATUS, success);
        statusBundle.putString(AppLockUtils.APPLOCK_STATUS_UNLOCKED_PACKAGE, mPkgName);
        AppLockUtils.sendStatusBroadcast(this, statusBundle,
                AppLockUtils.APPLOCK_STATUS_CHANGED_ACTION);
    }

    private void disableSystemKey() {
        requestSystemKeyEvent(KeyEvent.KEYCODE_APP_SWITCH, true);
    }

    private void resumeSystemKey() {
        requestSystemKeyEvent(KeyEvent.KEYCODE_APP_SWITCH, false);
    }

    private void DisableSystemBackKey() {
        requestSystemKeyEvent(KeyEvent.KEYCODE_BACK, true);
    }

    private void goHome() {
        AppLockUtils.sendHomekey();
    }

    private void removeAppTask() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
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

    public void requestOnResumeAciton() {
        LogUtil.d(TAG, "CheckView request ConfirmActiviy does Resume aciton");
    }

    public void requestOnPauseAciton() {
        LogUtil.d(TAG, "CheckView request ConfirmActiviy does Pause aciton");
    }

    public ActivityCycle getCurrentStatus() {
        return mActivityStatus;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }
}

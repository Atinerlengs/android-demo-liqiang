package com.freeme.applock.service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManager.StackId;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.hardware.fingerprint.FingerprintManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.UserHandle;
import android.telephony.TelephonyManager;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;

import com.android.internal.widget.LockPatternUtils;

import com.freeme.internal.app.AppLockPolicy;
import com.freeme.applock.AppLockUtils;
import com.freeme.applock.AppLockUtils.ClearPassViewReason;
import com.freeme.applock.R;
import com.freeme.applock.settings.LogUtil;

public class AppLockCheckService extends Service {
    private static final String TAG = "AppLockCheckService";

    private boolean hasDummyActivity;
    private boolean isUserPresentRegistered;
    private ActivityManager mActivityManager;
    private Handler mAudioFocusMonitorHandler;
    private AudioFocusMonitorRunnable mAudioFocusMonitorRunnable;
    private AudioManager mAudioManager;
    private Queue<ConentViewInfor> mBackupViewQueue;
    private AppLockCheckBaseView mContentView;
    private Context mContext;
    private FingerprintManager mFingerprintManager;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private boolean mIsBackup;
    private boolean mIsCheckViewShowing;
    private boolean mIsScreenStateRegistered;
    private KeyguardManager mKeyguardManager;
    private LockPatternUtils mLockPatternUtils;
    private List<String> mNeedCheckingScreenOnPackageList;
    private List<String> mNeedDummyActivityPackageList;
    private PowerManager mPowerManager;
    private AppLockCheckBaseView mRemovingContentView;
    private IntentFilter mScreenStateFilter = new IntentFilter();
    private List<String> mSupportShowWhenLockedPackageList;
    private TranslateAnimation mTranslateIn;
    private TranslateAnimation mTranslateOut;
    private UpdatePassViewRunnable mUpdatePassViewRunnable = new UpdatePassViewRunnable();
    private LayoutParams mWLayoutParams;
    private WindowManager mWindowManager;
    private IntentFilter userPresentFilter = new IntentFilter(Intent.ACTION_USER_PRESENT);
    private IntentFilter mPhoneStateFilter = new IntentFilter();

    private BroadcastReceiver mPhoneStateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
                String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                if (state.equals(TelephonyManager.EXTRA_STATE_RINGING) && mContentView != null) {
                    mContentView.onBackPressed();
                }
            }
        }
    };

    private BroadcastReceiver mScreenStateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent in) {
            if (Intent.ACTION_SCREEN_OFF.equals(in.getAction())) {
                LogUtil.d(TAG, "Screen off");
                if (!mPowerManager.isInteractive() || mKeyguardManager.isKeyguardLocked()) {
                    doHandleActionScreenOff();
                }
            } else if (Intent.ACTION_SCREEN_ON.equals(in.getAction())) {
                LogUtil.d(TAG, "Screen on");
                String topPackagename = getTopActivityPackageName();
                LogUtil.d(TAG, "topPackagename " + topPackagename);
                if (!AppLockUtils.PACKAGE_NAME.equals(topPackagename)
                        && mBackupViewQueue != null && mKeyguardManager.isKeyguardLocked()) {
                    for (ConentViewInfor cvi : mBackupViewQueue) {
                        if (!AppLockUtils.isLockedPackageQueueContains(cvi.packageName)) {
                            AppLockUtils.mLockedPackageQueue.offer(cvi);
                        }
                    }
                    handleResumedAppsAfterKeyguardLocked();
                }
                mBackupViewQueue = null;
            }
        }
    };

    private BroadcastReceiver userPresentBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent in) {
            if (in.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                mHandler.post(userPresentUnregisterRunnable);
                checkLockedPackageQueue();
                LogUtil.d(TAG, "adding view by ACTION_USER_PRESENT size = " + AppLockUtils.mLockedPackageQueue.size());
                if (AppLockUtils.mLockedPackageQueue.isEmpty()) {
                    clearPassView();
                }
                ConentViewInfor currentCVI = AppLockUtils.mLockedPackageQueue.peek();
                if (currentCVI != null) {
                    String currentPackage = currentCVI.packageName;
                    if (AppLockUtils.isAppCheckingByActivity(currentPackage)) {
                        Activity activity = (Activity) AppLockUtils.mApplicationVerifySet.get(currentPackage);
                        AppLockUtils.mApplicationVerifySet.remove(currentPackage);
                        activity.finish();
                    }
                    addPassView(false);
                }
            }
        }
    };

    private Runnable userPresentUnregisterRunnable = new Runnable() {
        @Override
        public void run() {
            if (isUserPresentRegistered) {
                isUserPresentRegistered = false;
                mContext.unregisterReceiver(userPresentBroadcastReceiver);
            }
        }
    };

    private class AudioFocusMonitorRunnable implements Runnable {
        private final int CHECK_PERIOD = 200;
        private final int MAX_CHECK_COUNT = 10;
        public int checkCount = 0;

        @Override
        public void run() {
            LogUtil.d(TAG, "AudioFocus monitor, after " + (checkCount * CHECK_PERIOD)
                    + "ms currentAudioFocusOwner:" + null
                    + " isVerifying:" + false +
                    " hasDummyActivity:" + hasDummyActivity);
            if (checkCount > MAX_CHECK_COUNT) {
                LogUtil.d(TAG, "check AudioFocus timeout");
                finishAudioFocusMonitor();
                return;
            }
            mAudioFocusMonitorHandler.postDelayed(mAudioFocusMonitorRunnable, CHECK_PERIOD);
            checkCount++;
        }
    }

    public class Callback {
        public void clearPassView(ClearPassViewReason reason) {
            LogUtil.d(TAG, "clearPassView(" + reason + ")");
            if (reason.equals(ClearPassViewReason.SCREEN_OFF) && mPowerManager.isInteractive()) {
                if (mLockPatternUtils.isLockScreenDisabled(UserHandle.myUserId())
                        || AppLockUtils.mLockedPackageQueue.peek().showWhenLocked) {
                    LogUtil.d(TAG, "clearPassView(), the screen has been on and there is not the lockscreen, skip!");
                    return;
                }
                mBackupViewQueue = new LinkedList<>(AppLockUtils.mLockedPackageQueue);
            }
            AppLockCheckService.this.clearPassView();
        }

        public void showBackupView(int fromLockType) {
            updatePassView(true, fromLockType);
        }

        public void onOrientationChanged(int orientation) {
            if (!AppLockUtils.mLockedPackageQueue.isEmpty()) {
                updatePassView(mIsBackup);
            }
        }

        public void onSavePSWForRotation(String psw) {
            if (!AppLockUtils.mLockedPackageQueue.isEmpty()) {
                AppLockUtils.mLockedPackageQueue.peek().password = psw;
            }
        }

        public void onDismiss() {
            if (!AppLockUtils.mLockedPackageQueue.isEmpty()) {
                resetAmsVerifyingState(AppLockUtils.mLockedPackageQueue.poll().packageName);
            }
            if (AppLockUtils.mLockedPackageQueue.isEmpty()) {
                removePassView();
                finishAudioFocusMonitor();
                finishDummyActivity();
                return;
            }
            updatePassView(false);
        }
    }

    public class ConentViewInfor {
        boolean showWhenLocked = false;
        public String packageName;
        String password;
        Intent startInent;

        public ConentViewInfor(String pn, Intent in) {
            packageName = pn;
            startInent = in;
            LayoutParams lp = startInent.getParcelableExtra(AppLockPolicy.LOCKED_PACKAGE_WINDOW_ATTRIBUTES);
            if (lp != null && (lp.flags & (WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)) != 0) {
                showWhenLocked = true;
            }
        }
    }

    private class UpdatePassViewRunnable implements Runnable {
        int fromLockType = 0;
        boolean isBackup = false;

        @Override
        public void run() {
            if (!mKeyguardManager.isKeyguardLocked() || AppLockUtils.mLockedPackageQueue.peek().showWhenLocked) {
                mContentView = initialLayoutView(isBackup, fromLockType);
                mWindowManager.addView(mContentView, mWLayoutParams);
                mContentView.onResume();
                updateCheckViewShowingState(true);
                RelativeLayout wholeLayout = (RelativeLayout) mContentView.findViewById(R.id.whole_layout);
                wholeLayout.setAnimation(mTranslateIn);
                wholeLayout.startAnimation(mTranslateIn);
                return;
            }
            LogUtil.d(TAG, "Ignoring updatePassView for KeyguardLocked");
            handleResumedAppsAfterKeyguardLocked();
        }
    }

    public void onCreate() {
        super.onCreate();
        LogUtil.d(TAG, "onCreate");
        mContext = getApplicationContext();
        mHandler = new Handler();
        mLockPatternUtils = new LockPatternUtils(mContext);
        mKeyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mFingerprintManager = (FingerprintManager) mContext.getSystemService(Context.FINGERPRINT_SERVICE);
        mWLayoutParams = new LayoutParams();
        mWLayoutParams.setTitle(AppLockUtils.APP_LOCK_CHECKVIEW_WIN_PARAM_PACKAGE_NAME + "/" + AppLockUtils.APP_LOCK_CHECKVIEW_WIN_PARAM_CLASS_NAME);
        mWLayoutParams.type =  WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL;
        mWLayoutParams.format = PixelFormat.RGBA_8888;
        mWLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_FULLSCREEN;
        mWLayoutParams.width =  WindowManager.LayoutParams.MATCH_PARENT;
        mWLayoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        mWLayoutParams.gravity = Gravity.CENTER_VERTICAL;
        mWLayoutParams.x = 0;
        mWLayoutParams.y = 0;
        mTranslateIn = (TranslateAnimation) AnimationUtils.loadAnimation(mContext, R.anim.in_from_bottom_to_top);
        mTranslateOut = (TranslateAnimation) AnimationUtils.loadAnimation(mContext, R.anim.out_from_top_to_bottom);
        mTranslateOut.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (mRemovingContentView != null) {
                    mWindowManager.removeView(mRemovingContentView);
                    mRemovingContentView = null;
                }
            }
        });
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mHandlerThread = new HandlerThread("audio_focus_thread");
        mHandlerThread.start();
        mAudioFocusMonitorHandler = new Handler(mHandlerThread.getLooper());
        mAudioFocusMonitorRunnable = new AudioFocusMonitorRunnable();
        String[] needDummyActivityPackages = getResources().getStringArray(R.array.need_dummy_activity_package);
        mNeedDummyActivityPackageList = new ArrayList<>();
        for (String str : needDummyActivityPackages) {
            mNeedDummyActivityPackageList.add(str);
        }
        String[] needCheckingScreenOnPackages = getResources().getStringArray(R.array.need_check_screen_on_package);
        mNeedCheckingScreenOnPackageList = new ArrayList<>();
        for (String str : needCheckingScreenOnPackages) {
            mNeedCheckingScreenOnPackageList.add(str);
        }
        String[] supportShowWhenLockedPackages = getResources().getStringArray(R.array.support_show_when_locked_package);
        mSupportShowWhenLockedPackageList = new ArrayList<>();
        for (String str : supportShowWhenLockedPackages) {
            mSupportShowWhenLockedPackageList.add(str);
        }
        mScreenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        mScreenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mScreenStateBroadcastReceiver, mScreenStateFilter);
        mIsScreenStateRegistered = true;

        mPhoneStateFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        mContext.registerReceiver(mPhoneStateBroadcastReceiver, mPhoneStateFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.d(TAG, "onStartCommand");
        if (intent != null) {
            super.onStartCommand(intent, flags, startId);
            lockApp(intent);
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private void lockApp(Intent intent) {
        String packageName = null;
        try {
            packageName = intent.getStringExtra(AppLockPolicy.LOCKED_PACKAGE_NAME);
        } catch (Exception e) {
            LogUtil.e(TAG, "getStringExtra(AppLockPolicy.LOCKED_PACKAGE_NAME) error :" + e);
        }
        LogUtil.d(TAG, "size before:" + AppLockUtils.mLockedPackageQueue.size());
        LogUtil.d(TAG, "packageName = " + packageName);
        if (packageName != null) {
            boolean showWhenLocked = false;
            boolean isKeyguardLocked = mKeyguardManager.isKeyguardLocked();
            LayoutParams lp = intent.getParcelableExtra("LOCKED_PACKAGE_WINDOW_ATTRIBUTES");
            if (lp != null && (lp.flags & (WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)) != 0) {
                showWhenLocked = true;
            }
            ConentViewInfor currentCVI = AppLockUtils.mLockedPackageQueue.peek();
            if (showWhenLocked && isKeyguardLocked && currentCVI != null
                    && (!currentCVI.showWhenLocked || !currentCVI.packageName.equals(packageName)
                        || !mIsCheckViewShowing)) {
                clearPassView();
            }
            if (AppLockUtils.isAppCheckingByFloatingWindow(packageName)) {
                LogUtil.d(TAG, "isAppCheckingByFloatingWindow, skip!");
                return;
            }
            if (AppLockUtils.isAppCheckingByActivity(packageName)) {
                Activity activity = (Activity) AppLockUtils.mApplicationVerifySet.get(packageName);
                AppLockUtils.mApplicationVerifySet.remove(packageName);
                activity.finish();
            }
            if (!AppLockUtils.isLockedPackageQueueContains(packageName)) {
                AppLockUtils.mLockedPackageQueue.offer(new ConentViewInfor(packageName, intent));
            }
            boolean isScreenOn = mPowerManager.isInteractive();
            if (AppLockUtils.mLockedPackageQueue.size() == 1) {
                if ((!isKeyguardLocked || (showWhenLocked
                        && mSupportShowWhenLockedPackageList.contains(packageName))) && isScreenOn) {
                    addPassView(false);
                } else {
                    LogUtil.d(TAG, "screen locked packageName : " + packageName);
                    handleResumedAppsAfterKeyguardLocked();
                }
            }
            LogUtil.d(TAG, "size after:" + AppLockUtils.mLockedPackageQueue.size() + " isScreenOn:" + isScreenOn
                    + " showWhenLocked:" + showWhenLocked + " isKeyguardLocked:" + isKeyguardLocked);
        }
    }

    private int getCurrentWindowCount() {
        int dockedWindowCnt = 0;
        int freeFormWindowCnt = 0;
        for (RunningTaskInfo info : mActivityManager.getRunningTasks(7)) {
            if (info.stackId == 2) {
                freeFormWindowCnt++;
            } else if (info.stackId == 3) {
                dockedWindowCnt++;
            }
        }
        int maxCheckCnt = freeFormWindowCnt;
        if (dockedWindowCnt != 0) {
            maxCheckCnt += 2;
        } else {
            maxCheckCnt++;
        }
        LogUtil.d(TAG, "dockedWindowCnt:" + dockedWindowCnt + " freeFormWindowCnt:" + freeFormWindowCnt
                + " maxCheckCnt:" + maxCheckCnt);
        return maxCheckCnt;
    }

    private void checkLockedPackageQueue() {
        int confirmActivityCount = AppLockUtils.getActiveConfirmActivityCount();
        int shouldCheckRuningTasksNum = AppLockUtils.mLockedPackageQueue.size() + confirmActivityCount;
        int currentWindowCount = getCurrentWindowCount();
        LogUtil.d(TAG, "confirmActivityCount:" + confirmActivityCount + " shouldCheckRuningTasksNum:"
                + shouldCheckRuningTasksNum + " currentWindowCount:" + currentWindowCount);
        if (shouldCheckRuningTasksNum < currentWindowCount) {
            shouldCheckRuningTasksNum = currentWindowCount;
        }
        List<RunningTaskInfo> runingTaskList = mActivityManager.getRunningTasks(shouldCheckRuningTasksNum);
        ArrayList<String> runningPackageList = new ArrayList<>();
        for (int i = 0; i < runingTaskList.size(); i++) {
            String packageName = runingTaskList.get(i).topActivity.getPackageName();
            LogUtil.d(TAG, "running package: " + packageName);
            runningPackageList.add(packageName);
        }
        Queue<ConentViewInfor> newLockedPackageQueue = new LinkedList<>();
        for (ConentViewInfor coninf : AppLockUtils.mLockedPackageQueue) {
            if (runningPackageList.contains(coninf.packageName)) {
                newLockedPackageQueue.offer(coninf);
            } else {
                resetAmsVerifyingState(coninf.packageName);
            }
        }
        AppLockUtils.mLockedPackageQueue = newLockedPackageQueue;
    }

    private String getTopActivityPackageName() {
        return mActivityManager.getRunningTasks(1).get(0).topActivity.getPackageName();
    }

    private ComponentName getTopActivity() {
        return mActivityManager.getRunningTasks(1).get(0).topActivity;
    }

    /*/ freeme.zhongkai.zhu. 20171027. applock
    private int getTopTaskUserId() {
        return ((RunningTaskInfo) mActivityManager.getRunningTasks(1).get(0)).userId;
    }
    //*/

    private void handleResumedAppsAfterKeyguardLocked() {
        if (!isUserPresentRegistered) {
            isUserPresentRegistered = true;
            mContext.registerReceiver(userPresentBroadcastReceiver, userPresentFilter);
        }
    }

    private boolean hasMultiWindowRunning() {
        for (RunningTaskInfo info : mActivityManager.getRunningTasks(7)) {
            if (!StackId.normallyFullscreenWindows(info.stackId)) {
                return true;
            }
        }
        return false;
    }

    private void doHandleActionScreenOff() {
        ComponentName topActivity = getTopActivity();
        String topPackagename = topActivity.getPackageName();
        String packagename = null;
        if (mNeedCheckingScreenOnPackageList.contains(topPackagename)) {
            packagename = topPackagename;
        }
        boolean isMultiWindowRunning = hasMultiWindowRunning();
        LogUtil.d(TAG, "isMultiWindowRunning " + isMultiWindowRunning + " packagename " + packagename
                + " isAppLockedPackage:" + mActivityManager.isAppLockedPackage(packagename));
        if (packagename != null && mActivityManager.isAppLockedPackage(packagename) && !isMultiWindowRunning) {
            if (AppLockUtils.isLockedPackageQueueContains(packagename)) {
                clearPassView();
            }
            Intent intent = new Intent(mActivityManager.getAppLockedCheckAction());
            Intent targetIntent = new Intent();
            targetIntent.setComponent(topActivity);
            LogUtil.d(TAG, "startActivity, cover on the locked app when screen off");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(AppLockPolicy.LAUNCH_FROM_RESUME, true);
            intent.putExtra(AppLockPolicy.LOCKED_PACKAGE_NAME, packagename);
            intent.putExtra(AppLockPolicy.LOCKED_PACKAGE_INTENT, targetIntent);
            intent.putExtra(AppLockUtils.CONFIRM_ACTIVITY_NO_NEED_SHOW_ON_KEYGUARD, true);
            mContext.startActivity(intent);
        }
    }

    private int checkLockType() {
        boolean hasEnrolledFingers = mFingerprintManager.isHardwareDetected()
                && mFingerprintManager.hasEnrolledFingerprints();
        int lockType = AppLockUtils.getAppLockType(mContext);
        if (AppLockUtils.isFingerprintLock(lockType)) {
            if (!hasEnrolledFingers) {
                lockType -= AppLockPolicy.LOCK_TYPE_FINGERPRINT;
            }
        }

        return lockType;
    }

    private AppLockCheckBaseView initialLayoutView(boolean isBackup, int fromLockType) {
        AppLockCheckBaseView contentView;
        LogUtil.d(TAG, "initialLayoutView isBackup = " + isBackup);
        mIsBackup = isBackup;
        switch (checkLockType()) {
            case AppLockPolicy.LOCK_TYPE_PATTERN:
            case AppLockPolicy.LOCK_TYPE_FINGERPRINT_PATTERN: {
                if (!isBackup) {
                    contentView = new AppLockPatternCheckView(new ContextThemeWrapper(mContext, R.style.PatternUnlockTheme));
                } else {
                    contentView = new AppLockPatternBackupCheckView(new ContextThemeWrapper(mContext, R.style.PatternUnlockTheme));
                }
                break;
            }
            case AppLockPolicy.LOCK_TYPE_PIN:
            case AppLockPolicy.LOCK_TYPE_FINGERPRINT_PIN: {
                contentView = new AppLockPinCheckView(new ContextThemeWrapper(mContext, R.style.AppLockActivityThemeWhite));
                break;
            }
            case AppLockPolicy.LOCK_TYPE_PASSWORD:
            case AppLockPolicy.LOCK_TYPE_FINGERPRINT_PASSWORD: {
                contentView = new AppLockPasswordCheckView(new ContextThemeWrapper(mContext, R.style.AppLockActivityThemeWhite));
                break;
            }
            case AppLockPolicy.LOCK_TYPE_FINGERPRINT: {
                if (!isBackup) {
                    contentView = new AppLockSpassCheckView(new ContextThemeWrapper(mContext, R.style.AppLockActivityThemeWhite));
                } else {
                    contentView = new AppLockSpassPasswordCheckView(new ContextThemeWrapper(mContext, R.style.AppLockActivityThemeWhite));
                }
                break;
            }
            default: {
                LogUtil.d(TAG, "NO this case, Settings.Secure.SEM_APPLOCK_LOCK_TYPE error!!!");
                contentView = new AppLockPinCheckView(new ContextThemeWrapper(mContext, R.style.AppLockActivityThemeWhite));
                break;
            }
        }
        contentView.setCallback(new Callback());
        contentView.setIntent(AppLockUtils.mLockedPackageQueue.peek().startInent);
        contentView.setPassword(AppLockUtils.mLockedPackageQueue.peek().password);
        return contentView;
    }

    private void addPassView(boolean isBackup) {
        if (!mIsCheckViewShowing) {
            if (mHandler.hasCallbacks(mUpdatePassViewRunnable)) {
                mHandler.removeCallbacks(mUpdatePassViewRunnable);
            }
            AppLockUtils.pauseConfimActiviy();
            mContentView = initialLayoutView(isBackup, 0);
            mWindowManager.addView(mContentView, mWLayoutParams);
            mContentView.onResume();
            updateCheckViewShowingState(true);
            RelativeLayout wholeLayout = (RelativeLayout) mContentView.findViewById(R.id.whole_layout);
            wholeLayout.setAnimation(mTranslateIn);
            wholeLayout.startAnimation(mTranslateIn);
        }
    }

    private void startAudioFocusMonitor() {
        LogUtil.d(TAG, "startAudioFocusMonitor()");
        if (mAudioFocusMonitorHandler.hasCallbacks(mAudioFocusMonitorRunnable)) {
            mAudioFocusMonitorHandler.removeCallbacks(mAudioFocusMonitorRunnable);
            mAudioFocusMonitorRunnable.checkCount = 0;
            mAudioFocusMonitorHandler.post(mAudioFocusMonitorRunnable);
            return;
        }
        mAudioFocusMonitorRunnable.checkCount = 0;
        mAudioFocusMonitorHandler.post(mAudioFocusMonitorRunnable);
    }

    private void finishAudioFocusMonitor() {
        LogUtil.d(TAG, "finishAudioFocusMonitor()");
        if (mAudioFocusMonitorHandler.hasCallbacks(mAudioFocusMonitorRunnable)) {
            mAudioFocusMonitorHandler.removeCallbacks(mAudioFocusMonitorRunnable);
        }
        mAudioFocusMonitorRunnable.checkCount = 0;
    }

    private synchronized void startDummyActivity(String packageName) {
        LogUtil.d(TAG, "startDummyActivity(), packageName:" + packageName);
        Intent intent = new Intent(mContext, AppLockDummyActivity.class);
        intent.putExtra("verify_package", packageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        mContext.startActivity(intent);
        hasDummyActivity = true;
    }

    private synchronized void finishDummyActivity() {
        LogUtil.d(TAG, "finishDummyActivity(), hasDummyActivity:" + hasDummyActivity
                + ", dummyActivity:" + AppLockUtils.dummyActivity);
        if (hasDummyActivity) {
            hasDummyActivity = false;
            if (AppLockUtils.dummyActivity != null) {
                AppLockUtils.dummyActivity.finish();
                AppLockUtils.dummyActivity = null;
            }
        }
    }

    private void updateCheckViewShowingState(boolean showing) {
        mIsCheckViewShowing = showing;
        AppLockUtils.setCheckViewShowingState(showing);
    }

    private void removePassView() {
        if (mHandler.hasCallbacks(mUpdatePassViewRunnable)) {
            mHandler.removeCallbacks(mUpdatePassViewRunnable);
        }
        if (mIsCheckViewShowing) {
            if (mKeyguardManager.isKeyguardLocked() || !mPowerManager.isInteractive()) {
                mWindowManager.removeView(mContentView);
            } else {
                mRemovingContentView = mContentView;
                RelativeLayout wholeLayout = (RelativeLayout) mRemovingContentView.findViewById(R.id.whole_layout);
                wholeLayout.setAnimation(mTranslateOut);
                wholeLayout.startAnimation(mTranslateOut);
            }
            mContentView.onDestroy();
            mContentView = null;
            mIsBackup = false;
            updateCheckViewShowingState(false);
            AppLockUtils.resumeConfirmActivity();
        }
    }

    private void updatePassView(boolean isBackup) {
        updatePassView(isBackup, 0);
    }

    private void updatePassView(boolean isBackup, int fromLockType) {
        if (mIsCheckViewShowing) {
            mContentView.onDestroy();
            mWindowManager.removeView(mContentView);
            updateCheckViewShowingState(false);
            if (mHandler.hasCallbacks(mUpdatePassViewRunnable)) {
                mHandler.removeCallbacks(mUpdatePassViewRunnable);
            }
            mUpdatePassViewRunnable.isBackup = isBackup;
            mUpdatePassViewRunnable.fromLockType = fromLockType;
            mHandler.post(mUpdatePassViewRunnable);
        }
    }

    private void resetAmsVerifyingState(String packageName) {
        if (packageName != null) {
            mActivityManager.setAppLockedVerifying(packageName, false);
        }
    }

    private void clearAmsVerifyingState() {
        for (ConentViewInfor conentViewInfor : AppLockUtils.mLockedPackageQueue) {
            resetAmsVerifyingState(conentViewInfor.packageName);
        }
    }

    private void clearPassView() {
        clearAmsVerifyingState();
        AppLockUtils.mLockedPackageQueue.clear();
        removePassView();
        finishAudioFocusMonitor();
        finishDummyActivity();
    }

    @Override
    public void onDestroy() {
        LogUtil.d(TAG, "onDestroy()");
        if (mIsScreenStateRegistered) {
            mContext.unregisterReceiver(mScreenStateBroadcastReceiver);
            mIsScreenStateRegistered = false;
        }
        mContext.unregisterReceiver(mPhoneStateBroadcastReceiver);

        finishAudioFocusMonitor();
        finishDummyActivity();
        super.onDestroy();
    }
}

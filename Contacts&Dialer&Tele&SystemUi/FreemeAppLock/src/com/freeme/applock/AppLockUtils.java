package com.freeme.applock;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings.Secure;
import android.view.KeyEvent;

import com.freeme.applock.service.AppLockCheckService.ConentViewInfor;
import com.freeme.applock.settings.LogUtil;
import com.freeme.applock.settings.PackageInfoUtil;
import com.freeme.internal.app.AppLockPolicy;
import com.freeme.provider.FreemeSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import static com.freeme.internal.app.AppLockPolicy.LOCK_TYPE_FINGERPRINT;
import static com.freeme.internal.app.AppLockPolicy.LOCK_TYPE_FINGERPRINT_PASSWORD;

public class AppLockUtils {
    public static final String PACKAGE_NAME = "com.freeme.applock";
    public static final String APPLOCK_STATUS = "com.freeme.applock.status.APPLOCK_CHECKED_STATUS";
    public static final String APPLOCK_STATUS_CHANGED_ACTION = "com.freeme.applock.intent.action.APPLOCKED_STATUS_CHANGED";
    public static final String APPLOCK_STATUS_CHANGED_PERMISSION = "com.freeme.applock.permission.STATUSCHANGED";
    public static final String APPLOCK_STATUS_UNLOCKED_PACKAGE = "com.freeme.applock.status.APPLOCK_STATUS_UNLOCKED_PACKAGE";
    public static final String APP_LOCK_CHECKVIEW_WIN_PARAM_CLASS_NAME = "CheckView";
    public static final String APP_LOCK_CHECKVIEW_WIN_PARAM_PACKAGE_NAME = "Applock.CheckService";
    public static final String CONFIRM_ACTIVITY_NO_NEED_SHOW_ON_KEYGUARD = "CONFIRM_ACTIVITY_NO_NEED_SHOW_ON_KEYGUARD";
    public static final String CONFIRM_ACTIVITY_WINDOW_ATTRIBUTE_LAYOUT_PARAMS = "WINDOW_ATTRIBUTE_LAYOUT_PARAMS";
    public static final int DEFAULT_TRY_COUNT = 5;
    public static final int FAILED_ATTEMPTS_BEFORE_TIMEOUT = 5;
    public static final long FAILED_ATTEMPT_COUNTDOWN_INTERVAL_MS = 1000;
    public static final long FAILED_ATTEMPT_TIMEOUT_MARGIN = 1000;
    public static final long FAILED_ATTEMPT_TIMEOUT_MS = 30000;

    public static final String KEY_REQUEST_ACTION = "lock_or_unlock";
    public static final String KEY_REQUEST_LOCK = "request_lock";
    public static final String KEY_REQUEST_UNLOCK = "request_unlock";
    static public final String SYSTEM_DIALOG_REASON_KEY = "reason";
    static public final String SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS = "globalactions";
    static public final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
    static public final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
    public static final String PREF_ATTEMPT_BACKUP_COUNT = "pref_attempt_backup_count";
    public static final String PREF_ATTEMPT_DEADLINE = "pref_attempt_deadline";
    public static final String PREF_ATTEMPT_UNLOCK_COUNT = "pref_attempt_unlock_count";
    private static final String TAG = "AppLockUtils";
    public static final String TRUE = "true";
    public static final String FALSE = "false";
    public static final boolean IS_DEVICE_WHITE_THEME = true;

    public static final int MAX_MULTI_WINDOW_NUM = 7;
    public static final int MINIMUM_PASSWORD_LENGTH_BEFORE_REPORT = 3;

    public static Activity dummyActivity;
    public static HashMap<String, Object> mApplicationVerifySet = new HashMap<>();
    public static HashMap<String, Long> mApplicationVerifyTime = new HashMap<>();
    private static int mFailedBackupAttempts;
    private static int mFailedUnlockAttempts;
    public static Queue<ConentViewInfor> mLockedPackageQueue = new LinkedList<>();
    private static SharedPreferences mPrefrences;
    private static long mRemaingTimeToUnlock;
    private static ArrayList<String> mWhiteList;
    public static String CheckViewShowingState = FALSE;

    public enum ActivityCycle {
        UNKNOWN,
        ON_CREATE,
        ON_START,
        ON_RESUME,
        ON_PAUSE,
        ON_STOP,
        ON_DESTRONY
    }

    public enum ClearPassViewReason {
        SCREEN_OFF,
        CLOSE_SYSTEM_DIALOG,
        EMERGENCY,
        LAUNCHER_RESUME,
        BACK_KEY
    }

    public static synchronized void reportFailedBackupAttempts() {
        mFailedBackupAttempts++;
        LogUtil.i(TAG, "reportFailedBackupAttempts: mFailedBackupAttempts " + mFailedBackupAttempts);
        if (mFailedBackupAttempts >= 5) {
            mRemaingTimeToUnlock = SystemClock.elapsedRealtime() + 30000;
            if (mPrefrences != null) {
                mPrefrences.edit().putLong(PREF_ATTEMPT_DEADLINE, mRemaingTimeToUnlock)
                        .putInt(PREF_ATTEMPT_BACKUP_COUNT, 0).apply();
            }
        } else if (mPrefrences != null) {
            mPrefrences.edit().putInt(PREF_ATTEMPT_BACKUP_COUNT, mFailedBackupAttempts).apply();
        }
    }

    public static synchronized int getFailedBackupAttempts() {
        LogUtil.i(TAG, "getFailedBackupAttempts: return " + mFailedBackupAttempts);
        return mFailedBackupAttempts;
    }

    public static synchronized void resetFailedBackupAttempts() {
        mFailedBackupAttempts = 0;
        mRemaingTimeToUnlock = 0;
        if (mPrefrences != null) {
            mPrefrences.edit().putLong(PREF_ATTEMPT_DEADLINE, 0).putInt(PREF_ATTEMPT_BACKUP_COUNT, 0).apply();
        } else {
            LogUtil.e(TAG, "resetNoMatchCount: SharedPreferences is null");
        }
    }

    public static synchronized long getRemaingTimeToUnlock() {
        long retTime = 0;
        if (mRemaingTimeToUnlock != 0) {
            retTime = mRemaingTimeToUnlock - SystemClock.elapsedRealtime();
            if (retTime <= 0) {
                mRemaingTimeToUnlock = 0;
                retTime = 0;
            } else if (retTime > 30000) {
                setRemaingTimeToUnlock();
                retTime = 30000;
            }
        }
        LogUtil.i(TAG, "getRemaingTimeToUnlock: return " + retTime);
        return retTime;
    }

    public static synchronized void setRemaingTimeToUnlock() {
        mRemaingTimeToUnlock = SystemClock.elapsedRealtime() + 30000;
        if (mPrefrences != null) {
            mPrefrences.edit().putLong(PREF_ATTEMPT_DEADLINE, mRemaingTimeToUnlock).apply();
        }
    }

    public static synchronized void resetRemaingTimeToUnlock() {
        mRemaingTimeToUnlock = 0;
        if (mPrefrences != null) {
            mPrefrences.edit().putLong(PREF_ATTEMPT_DEADLINE, 0).apply();
        } else {
            LogUtil.e(TAG, "resetRemaingTimeToUnlock: SharedPreferences is null");
        }
    }

    public static synchronized void initFailedUnlockAttemptsFromPrefrence(SharedPreferences pref) {
        if (pref != null) {
            mPrefrences = pref;
            mFailedUnlockAttempts = mPrefrences.getInt(PREF_ATTEMPT_UNLOCK_COUNT, 0);
            mFailedBackupAttempts = mPrefrences.getInt(PREF_ATTEMPT_BACKUP_COUNT, 0);
            mRemaingTimeToUnlock = mPrefrences.getLong(PREF_ATTEMPT_DEADLINE, 0);
            long remainTime = mRemaingTimeToUnlock - SystemClock.elapsedRealtime();
            if (mRemaingTimeToUnlock != 0 && (remainTime > 30000 || remainTime < 0)) {
                resetRemaingTimeToUnlock();
                resetFailedUnlockNBackupAttempts();
            }
        } else {
            LogUtil.e(TAG, "SharedPreferences is null");
        }
    }

    public static synchronized int getFailedUnlockAttempts() {
        return mFailedUnlockAttempts;
    }

    public static synchronized void reportFailedUnlockAttempts() {
        mFailedUnlockAttempts++;
        if (mFailedUnlockAttempts <= 5) {
            if (mPrefrences != null) {
                mPrefrences.edit().putInt(PREF_ATTEMPT_UNLOCK_COUNT, mFailedUnlockAttempts).apply();
            }
        }
    }

    public static synchronized void resetFailedUnlockNBackupAttempts() {
        mFailedUnlockAttempts = 0;
        mFailedBackupAttempts = 0;
        mRemaingTimeToUnlock = 0;
        if (mPrefrences != null) {
            mPrefrences.edit().putLong(PREF_ATTEMPT_DEADLINE, 0)
                    .putInt(PREF_ATTEMPT_UNLOCK_COUNT, 0)
                    .putInt(PREF_ATTEMPT_BACKUP_COUNT, 0)
                    .apply();
        } else {
            LogUtil.e(TAG, "resetNoMatchCount: SharedPreferences is null");
        }
    }

    public static synchronized boolean isLockedPackageQueueContains(String packageName) {
        for (ConentViewInfor pkgInfo : mLockedPackageQueue) {
            if (packageName.equals(pkgInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    public static synchronized void lockedPackageQueueRemove(String packageName) {
        for (ConentViewInfor pkgInfo : mLockedPackageQueue) {
            if (packageName.equals(pkgInfo.packageName)) {
                mLockedPackageQueue.remove(pkgInfo);
                break;
            }
        }
    }

    public static void setCheckViewShowingState(boolean showing) {
        synchronized (CheckViewShowingState) {
            CheckViewShowingState = showing ? TRUE : FALSE;
        }
    }

    public static boolean getCheckViewShowingState() {
        boolean equals;
        synchronized (CheckViewShowingState) {
            equals = CheckViewShowingState.equals(TRUE);
        }
        return equals;
    }

    public static synchronized boolean isAppChecking(String packageName) {
        return isAppCheckingByFloatingWindow(packageName) || isAppCheckingByActivity(packageName);
    }

    public static synchronized boolean isAppCheckingByFloatingWindow(String packageName) {
        return isLockedPackageQueueContains(packageName);
    }

    public static synchronized boolean isAppCheckingByActivity(String packageName) {
        return mApplicationVerifySet.get(packageName) != null;
    }

    public static void sendStatusBroadcast(Context context, Bundle info, String destination) {
        /*/ freeme.yangzhengguang, 20180626. remove unused code
        Intent intent = new Intent(destination);
        if (info == null) {
            info = new Bundle();
            info.putBoolean(APPLOCK_STATUS, true);
        }
        intent.putExtras(info);
        context.sendBroadcast(intent, "com.freeme.applock.permission.STATUSCHANGED");
        //*/
    }

    public static void startHomeActivity(Context context) {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN, null);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        context.startActivity(homeIntent);
    }

    public static void sendHomekey() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                new Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_HOME);
                LogUtil.d(AppLockUtils.TAG, "Send home key");
            }
        }).start();
    }

    public static synchronized void applicationVerifySetPut(String pkgname, Activity activity) {
        mApplicationVerifySet.put(pkgname, activity);
        mApplicationVerifyTime.put(pkgname, SystemClock.elapsedRealtime());
    }

    private static Object getLatestHasMapKey(HashMap<String, Object> map) {
        Object[] pkgList = mApplicationVerifySet.keySet().toArray();
        Object latestPkg = pkgList[pkgList.length - 1];
        Long latestTime = mApplicationVerifyTime.get(latestPkg);
        for (Object pkg : pkgList) {
            Long time = mApplicationVerifyTime.get(pkg);
            if (latestTime < time) {
                latestTime = time;
                latestPkg = pkg;
            }
        }
        return latestPkg;
    }

    public static synchronized void pauseConfimActiviy() {
        if (mApplicationVerifySet.size() > 0) {
            AppLockConfirmActivity confirmActivity = (AppLockConfirmActivity)
                    mApplicationVerifySet.get(getLatestHasMapKey(mApplicationVerifySet));
            if (confirmActivity.getCurrentStatus().equals(ActivityCycle.ON_RESUME)) {
                confirmActivity.requestOnPauseAciton();
            } else {
                LogUtil.d(TAG, "pauseConfimActiviy skip, it has been " + confirmActivity.getCurrentStatus());
            }
        }
    }

    public static synchronized void resumeConfirmActivity() {
        synchronized (AppLockUtils.class) {
            if (mApplicationVerifySet.size() > 0) {
                AppLockConfirmActivity confirmActivity = (AppLockConfirmActivity)
                        mApplicationVerifySet.get(getLatestHasMapKey(mApplicationVerifySet));
                if (confirmActivity.getCurrentStatus().equals(ActivityCycle.ON_RESUME)) {
                    confirmActivity.requestOnResumeAciton();
                } else {
                    LogUtil.d(TAG, "resumeConfirmActivity skip, it has been " + confirmActivity.getCurrentStatus());
                }
            }
        }
    }

    public static synchronized int getActiveConfirmActivityCount() {
        int count = 0;
        if (mApplicationVerifySet.size() > 0) {
            for (String key : mApplicationVerifySet.keySet()) {
                AppLockConfirmActivity confirmActivity = (AppLockConfirmActivity) mApplicationVerifySet.get(key);
                if (confirmActivity.getCurrentStatus().equals(ActivityCycle.ON_RESUME)
                        || confirmActivity.getCurrentStatus().equals(ActivityCycle.ON_PAUSE)
                        || confirmActivity.getCurrentStatus().equals(ActivityCycle.ON_STOP)) {
                    count++;
                }
            }
        }
        if (dummyActivity != null) {
            count++;
        }
        return count;
    }

    public static boolean isSupportPatternBackupPin() {
        return false;
    }

    public static int getAppLockType(Context context) {
        return Secure.getInt(context.getContentResolver(),
                FreemeSettings.Secure.FREEME_APPLOCK_LOCK_TYPE, AppLockPolicy.LOCK_TYPE_NONE);
    }

    public static boolean isFingerprintLock(int locktype) {
        return (locktype > LOCK_TYPE_FINGERPRINT && locktype <= LOCK_TYPE_FINGERPRINT_PASSWORD);
    }

    public static boolean isInWhiteList(Context context, String packageName) {
        if (mWhiteList == null) {
            mWhiteList = PackageInfoUtil.getInstance().getWhiteList(context);
        }
        return mWhiteList.contains(packageName);
    }

    public static void updateLockedDB(Context context) {
        String lockedPackageString = Secure.getString(context.getContentResolver(), PackageInfoUtil.LOCKED_PACKAGE);
        LogUtil.i(TAG, "DB version change so update");
        if (lockedPackageString != null) {
            StringBuilder newLockedPkgs = new StringBuilder();
            for (String pkg : lockedPackageString.split(",")) {
                if (!isInWhiteList(context, pkg)) {
                    newLockedPkgs.append(pkg);
                    newLockedPkgs.append(",");
                }
            }
            if (newLockedPkgs.length() != 0) {
                newLockedPkgs.delete(newLockedPkgs.length() - 1, newLockedPkgs.length());
            }
            Secure.putString(context.getContentResolver(), PackageInfoUtil.LOCKED_PACKAGE, newLockedPkgs.toString());
        }
    }
}

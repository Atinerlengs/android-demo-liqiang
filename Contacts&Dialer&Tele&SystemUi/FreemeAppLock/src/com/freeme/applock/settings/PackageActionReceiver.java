package com.freeme.applock.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PackageActionReceiver extends BroadcastReceiver {
    public static final String TAG = PackageActionReceiver.class.getSimpleName();
    public static final String ADD_ACTION = Intent.ACTION_PACKAGE_ADDED;
    private static final String KEY_LASTEST_LOCKED_PACKAGES = "last_locked_package";
    private static final String NOTIFY_APPLOCK_UPDATE_ACTION = "com.freeme.applock.intent.action.NOTIFYUPDATE";
    public static final String REMOVE_ACTION = Intent.ACTION_PACKAGE_REMOVED;

    public void onReceive(Context context, Intent intent) {
        if (AppLockUtil.isSupportCHNEnhancedFeature("applock")) {
            String action = intent.getAction();
            Boolean isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            if (REMOVE_ACTION.equals(action) && !isReplacing) {
                updateStateChanged(context, intent, AppLockUtil.STATES_PACKAGE_REMOVED);
            } else if (ADD_ACTION.equals(action)) {
                updateStateChanged(context, intent, AppLockUtil.STATES_PACKAGE_ADD);
            } else if (NOTIFY_APPLOCK_UPDATE_ACTION.equals(action)) {
                String lastLockedPackages = intent.getStringExtra(KEY_LASTEST_LOCKED_PACKAGES);
                updateDB(context, lastLockedPackages);
                LogUtil.i(TAG, "lastLockedPackages=" + lastLockedPackages);
            }
        }
    }

    private void updateStateChanged(Context context, Intent intent, int status) {
        PackageInfoUtil.getInstance().updatePackageChanged(context, status, intent);
    }

    private void updateDB(Context context, String lastLockedPackages) {
        PackageInfoUtil.getInstance().updateDBStatus(context, lastLockedPackages);
    }
}

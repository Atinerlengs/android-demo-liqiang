package com.freeme.applock.service;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.freeme.applock.AppLockUtils;
import com.freeme.applock.settings.LogUtil;

public class AppLockDummyActivity extends Activity {
    private String TAG = "AppLockDummyActivity";
    private static int mDummyActivityCount = 0;
    private ActivityManager mActivityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.d(TAG, "onCreate()");
        mDummyActivityCount++;
        mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        String packageName = getIntent().getStringExtra("verify_package");
        LogUtil.d(TAG, "onCreate(), verify_package:" + packageName);
        if (packageName == null || !mActivityManager.isAppLockedVerifying(packageName)) {
            LogUtil.d(TAG, "onCreate(), finish!");
            finish();
            return;
        }
        AppLockUtils.dummyActivity = this;
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtil.d(TAG, "onResume()");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mDummyActivityCount++;
    }

    @Override
    protected void onDestroy() {
        LogUtil.d(TAG, "onDestroy()");
        mDummyActivityCount--;
        LogUtil.d(TAG, "mDummyActivityCount " + mDummyActivityCount);
        if (mDummyActivityCount == 0) {
            AppLockUtils.dummyActivity = null;
        }
        super.onDestroy();
    }
}

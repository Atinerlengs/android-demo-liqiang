package com.freeme.applock;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AppLockApplication extends Application {
    final int currentVersion = 1;
    SharedPreferences mSharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int oldVersion = mSharedPreferences.getInt("applock_application_version", 0);
        if (oldVersion < currentVersion) {
            update(oldVersion, currentVersion);
            mSharedPreferences.edit().putInt("applock_application_version", currentVersion).apply();
        }
    }

    void update(int oldVersion, int newVersion) {
        AppLockUtils.updateLockedDB(getApplicationContext());
    }
}

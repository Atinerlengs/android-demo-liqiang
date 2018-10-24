package com.freeme.safe.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.storage.IStorageManager;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.android.internal.widget.ILockSettings;
import com.google.android.collect.Lists;

import java.util.List;

import com.freeme.safe.view.LockPatternView;

public class LockPatternUtils {

    private static final String TAG = "LockPatternUtils";
    private final Context mContext;
    private final ContentResolver mContentResolver;
    private ILockSettings mLockSettingsService;
    private final Handler mHandler;

    public LockPatternUtils(Context context) {
        mContext = context;
        mContentResolver = context.getContentResolver();

        Looper looper = Looper.myLooper();
        mHandler = looper != null ? new Handler(looper) : null;
    }

    @VisibleForTesting
    public ILockSettings getLockSettings() {
        if (mLockSettingsService == null) {
            ILockSettings service = ILockSettings.Stub.asInterface(
                    ServiceManager.getService("lock_settings"));
            mLockSettingsService = service;
        }
        return mLockSettingsService;
    }

    /**
     * Deserialize a pattern.
     * @param string The pattern serialized with {@link #patternToString}
     * @return The pattern.
     */
    public static List<LockPatternView.Cell> stringToPattern(String string) {
        if (string == null) {
            return null;
        }

        List<LockPatternView.Cell> result = Lists.newArrayList();

        final byte[] bytes = string.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            byte b = (byte) (bytes[i] - '1');
            result.add(LockPatternView.Cell.of(b / 3, b % 3));
        }
        return result;
    }

    /**
     * Serialize a pattern.
     * @param pattern The pattern.
     * @return The pattern in string form.
     */
    public static String patternToString(List<LockPatternView.Cell> pattern) {
        if (pattern == null) {
            return "";
        }
        final int patternSize = pattern.size();

        byte[] res = new byte[patternSize];
        for (int i = 0; i < patternSize; i++) {
            LockPatternView.Cell cell = pattern.get(i);
            res[i] = (byte) (cell.getRow() * 3 + cell.getColumn() + '1');
        }
        return new String(res);
    }

    /**
     * @return Whether the visible pattern is enabled.
     */
    public boolean isVisiblePatternEnabled(int userId) {
        return getBoolean(Settings.Secure.LOCK_PATTERN_VISIBLE, false, userId);
    }

    /**
     * Set whether the visible pattern is enabled.
     */
    public void setVisiblePatternEnabled(boolean enabled, int userId) {
        setBoolean(Settings.Secure.LOCK_PATTERN_VISIBLE, enabled, userId);

        // Update for crypto if owner
        if (userId != UserHandle.USER_SYSTEM) {
            return;
        }

        IBinder service = ServiceManager.getService("mount");
        if (service == null) {
            Log.e(TAG, "Could not find the mount service to update the user info");
            return;
        }

        IStorageManager storageManager = IStorageManager.Stub.asInterface(service);
        try {
            storageManager.setField(StorageManager.PATTERN_VISIBLE_KEY, enabled ? "1" : "0");
        } catch (RemoteException e) {
            Log.e(TAG, "Error changing pattern visible state", e);
        }
    }

    /**
     * @return Whether tactile feedback for the pattern is enabled.
     */
    public boolean isTactileFeedbackEnabled() {
        return Settings.System.getIntForUser(mContentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 1, UserHandle.USER_CURRENT) != 0;
    }

    private boolean getBoolean(String secureSettingKey, boolean defaultValue, int userId) {
        try {
            return getLockSettings().getBoolean(secureSettingKey, defaultValue, userId);
        } catch (RemoteException re) {
            return defaultValue;
        }
    }

    private void setBoolean(String secureSettingKey, boolean enabled, int userId) {
        try {
            getLockSettings().setBoolean(secureSettingKey, enabled, userId);
        } catch (RemoteException re) {
            // What can we do?
            Log.e(TAG, "Couldn't write boolean " + secureSettingKey + re);
        }
    }
}

package com.freeme.applock.settings;

import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.SystemProperties;
import android.provider.Settings.Secure;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.freeme.internal.app.AppLockPolicy;
import com.freeme.provider.FreemeSettings;

import static com.freeme.internal.app.AppLockPolicy.LOCK_TYPE_PATTERN;
import static com.freeme.internal.app.AppLockPolicy.LOCK_TYPE_FINGERPRINT;
import static com.freeme.internal.app.AppLockPolicy.LOCK_TYPE_FINGERPRINT_PASSWORD;

public class AppLockUtil {
    public static final String APPLOCKED_STATUS_ACTION = "com.freeme.applock.intent.action.APPLOCKED_STATUS_CHANGED";
    public static final String APPLOCK_ENABLE_ACTION = "com.freeme.applock.intent.action.APPLOCK_ENABLE_CHANGED";
    public static final String FOLDERLOCK_ACTION = "com.sec.android.launcher.intent.action.FOLDERLOCK_CHANGED";
    public static final String KEY_SECURE_LOCK_SETTINGS_PATTERN_VISIBLE_SWITCH = "applock_secure_lock_settings_pattern_visible_switch";
    public static final String MASTER_SWITCH = "master_switch";

    public static final int REQUEST_CODE_SET_LOCK = 10001;
    public static final int REQUEST_CODE_VERIFY_LOCK = 10002;

    public static final int LOCK_STATE_CHANGE = 1;
    public static final int UPDATE_LIST_VIEW = 2;
    public static final int STATES_PACKAGE_ADD = 3;
    public static final int STATES_PACKAGE_REMOVED = 4;
    public static final int UPDATE_LOCK_TYPE = 5;
    private static boolean mFeatureChecked;
    private static boolean mFeatureEnabled;

    public static class ChooseLockConstant {
        public static final String HIDE_DISABLED_PREFS = "hide_disabled_prefs";
        public static final String MINIMUM_QUALITY_KEY = "minimum_quality";
        public static final String PASSWORD_MAX_KEY = "lockscreen.password_max";
        public static final String PASSWORD_MIN_KEY = "lockscreen.password_min";
    }

    public static int getLockType(Context context) {
        return Secure.getInt(context.getContentResolver(),
                FreemeSettings.Secure.FREEME_APPLOCK_LOCK_TYPE, AppLockPolicy.LOCK_TYPE_NONE);
    }

    public static int getBasicLockType(Context context) {
        int lockType = getLockType(context);
        if (isFingerPrint(lockType)) {
            lockType -= LOCK_TYPE_FINGERPRINT;
        }
        return lockType;
    }

    public static boolean isPattern(int lockType) {
        return (lockType % LOCK_TYPE_FINGERPRINT == LOCK_TYPE_PATTERN);
    }

    public static boolean isFingerPrint(int lockType) {
        return lockType > LOCK_TYPE_FINGERPRINT && lockType <= LOCK_TYPE_FINGERPRINT_PASSWORD;
    }

    public static String StringCat(String str1, String str2) {
        return str1 + "," + str2;
    }

    public static void setMaxFontScale(Context context, TextView textView) {
        float fontScale = context.getResources().getConfiguration().fontScale;
        float fontsize = textView.getTextSize() / context.getResources().getDisplayMetrics().scaledDensity;
        if (fontScale > 1.2f) {
            fontScale = 1.2f;
        }
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontsize * fontScale);
    }

    public static boolean isSupportCHNEnhancedFeature(String featureName) {
        return "applock".equals(featureName);
        //!TextUtils.isEmpty(featureName) && SemCscFeature.getInstance().getString("CscFeature_SmartManager_ConfigSubFeatures").contains(featureName)
    }

    public static synchronized boolean hasFingerprintFeature(Context context) {
        if (mFeatureChecked) {
            return mFeatureEnabled;
        }
        FingerprintManager fm = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
        mFeatureEnabled = fm.isHardwareDetected();
        mFeatureChecked = true;
        return mFeatureEnabled;
    }

    public static void setNavigationBarDisableRecent(View view) {
        view.setSystemUiVisibility(view.getSystemUiVisibility() | View.STATUS_BAR_DISABLE_RECENT);
    }

    public static void showKeyboard(View view) {
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager)
                    view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputManager != null) {
                view.requestFocus();
                inputManager.showSoftInput(view, 0);
            }
        }
    }

    public static void hideKeyboard(View view) {
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager)
                    view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputManager != null) {
                inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}

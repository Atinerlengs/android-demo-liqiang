package com.freeme.applock.settings;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.widget.LockPatternUtils.AppLockType;

import com.freeme.internal.app.AppLockPolicy;
import com.freeme.applock.R;
import com.freeme.applock.settings.AppLockUtil.ChooseLockConstant;
import com.freeme.preference.FreemeJumpPreference;
import com.freeme.provider.FreemeSettings;

public class AppLockTypeFragment extends PreferenceFragment implements OnPreferenceChangeListener {
    private static final String TAG = "AppPageLockTypeFragment";

    private static final String ACTIVITY_CHOOSELOCKGENERIC = "com.android.settings.password.ChooseLockGeneric";
    private static final String ACTIVITY_CHOOSELOCKPASSWORD = "com.android.settings.password.ChooseLockPassword";
    private static final String ACTIVITY_CHOOSELOCKPATTERN = "com.android.settings.password.ChooseLockPattern";
    private static final String CONFIRM_CREDENTIALS = "confirm_credentials";
    private static final String KEY_LOCK_TYPE_FINGER = "applock_locktype_fingerprint";
    private static final String KEY_LOCK_TYPE_PASSWORD = "applock_locktype_password";
    private static final String KEY_LOCK_TYPE_PATTERN = "applock_locktype_pattern";
    private static final String KEY_LOCK_TYPE_PIN = "applock_locktype_pin";
    private static final String KEY_SECURE_LOCK_SETTINGS_CATEGORY = "applock_secure_lock_settings_category";
    private static final int LOCK_TYPE_PATTERN = 1;
    private static final int LOCK_TYPE_PIN = 2;
    private static final int LOCK_TYPE_PASSWORD = 3;
    private static final int LOCK_TYPE_FINGERPRINT = 4;

    private static final int MIN_PASSWORD_LENGTH = 4;
    private static final String PACKAGE_SETTINGS = "com.android.settings";
    private static final int CONFIRM_REQUEST_ENTER = 1000;
    private static final int FOR_APP_LOCK_BACKUP_KEY = 1004;
    private ContentResolver mContentResolver;
    private FingerprintManager mFingerprintManager;
    private int mLockType;
    private SwitchPreference mLockTypeFinger;
    private FreemeJumpPreference mLockTypePassword;
    private FreemeJumpPreference mLockTypePattern;
    private FreemeJumpPreference mLockTypePin;
    private PackageInfoUtil mPackageInfoUtil;
    private PreferenceCategory mSecureLockSettingsCategory;
    private SwitchPreference mSecureLockSettingsPatternVisibleSwitch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPackageInfoUtil = PackageInfoUtil.getInstance();
        mFingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.applock_lock_type_layout, container, false);
        addPreferencesFromResource(R.xml.applock_locktype);
        getActivity().getActionBar().setTitle(R.string.applock_lock_type);
        mLockType = AppLockUtil.getLockType(getActivity());
        initPref();
        return view;
    }

    @Override
    public void onResume() {
        LogUtil.d(TAG, "onResume() ");
        super.onResume();
    }

    @Override
    public void onPause() {
        LogUtil.d(TAG, "onPause() ");
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        LogUtil.d(TAG, "onStop() ");
    }

    public void initPref() {
        mLockTypePattern = (FreemeJumpPreference) findPreference(KEY_LOCK_TYPE_PATTERN);
        mLockTypePin = (FreemeJumpPreference) findPreference(KEY_LOCK_TYPE_PIN);
        mLockTypePassword = (FreemeJumpPreference) findPreference(KEY_LOCK_TYPE_PASSWORD);
        mLockTypeFinger = (SwitchPreference) findPreference(KEY_LOCK_TYPE_FINGER);
        mSecureLockSettingsCategory = (PreferenceCategory) findPreference(KEY_SECURE_LOCK_SETTINGS_CATEGORY);
        mSecureLockSettingsPatternVisibleSwitch = (SwitchPreference) findPreference(
                AppLockUtil.KEY_SECURE_LOCK_SETTINGS_PATTERN_VISIBLE_SWITCH);
        if (AppLockUtil.hasFingerprintFeature(getActivity())) {
            boolean hasEnrolledFingers = isUnlockWithFingerprintPossible();
            if (!hasEnrolledFingers) {
                mLockTypeFinger.setEnabled(false);
                mLockTypeFinger.setSummary(R.string.unlock_set_unlock_fingerprint_summary_no_finger);
            } else {
                if (mLockType == AppLockPolicy.LOCK_TYPE_NONE) {
                    mLockTypeFinger.setChecked(false);
                } else {
                    if (AppLockUtil.isFingerPrint(mLockType)) {
                        mLockTypeFinger.setChecked(true);
                    } else {
                        mLockTypeFinger.setChecked(false);
                    }
                }
            }
        } else {
            getPreferenceScreen().removePreference(mLockTypeFinger);
        }

        if (!AppLockUtil.isPattern(mLockType)) {
            getPreferenceScreen().removePreference(mSecureLockSettingsCategory);
            getPreferenceScreen().removePreference(mSecureLockSettingsPatternVisibleSwitch);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object object) {
        mLockType = AppLockUtil.getLockType(getActivity());
        return false;
    }

    private void StartChooseLockPassword(int quality) {
        DevicePolicyManager DPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        int minLength = DPM.getPasswordMinimumLength(null);
        if (minLength < MIN_PASSWORD_LENGTH) {
            minLength = MIN_PASSWORD_LENGTH;
        }
        int maxLength = DPM.getPasswordMaximumLength(quality);
        ComponentName componentName = new ComponentName(PACKAGE_SETTINGS, ACTIVITY_CHOOSELOCKPASSWORD);
        Intent intent = new Intent();
        intent.setComponent(componentName);
        intent.putExtra("lockscreen.password_type", quality);
        intent.putExtra(ChooseLockConstant.PASSWORD_MIN_KEY, minLength);
        intent.putExtra(ChooseLockConstant.PASSWORD_MAX_KEY, maxLength);
        intent.putExtra(CONFIRM_CREDENTIALS, false);
        intent.putExtra(AppLockPolicy.KEY_FROM_APPLOCK, true);
        intent.putExtra(AppLockPolicy.KEY_APPLOCK_QUALITY, quality == DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
        startActivityForResult(intent, quality == DevicePolicyManager.PASSWORD_QUALITY_NUMERIC ? 2 : 3);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.equals(mLockTypePattern)) {
            ComponentName componentName = new ComponentName(PACKAGE_SETTINGS, ACTIVITY_CHOOSELOCKPATTERN);
            Intent intent = new Intent();
            intent.putExtra(AppLockPolicy.KEY_FROM_APPLOCK, true);
            intent.putExtra("key_lock_method", "pattern");
            intent.setComponent(componentName);
            startActivityForResult(intent, LOCK_TYPE_PATTERN);
        } else if (preference.equals(mLockTypePin)) {
            StartChooseLockPassword(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
        } else if (preference.equals(mLockTypePassword)) {
            StartChooseLockPassword(DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC);
        } else if (preference.equals(mLockTypeFinger)) {
            if (isFingerprintEnabled()) {
                int userId = ActivityManager.getCurrentUser();
                if (mFingerprintManager.hasEnrolledFingerprints(userId)) {
                    identifyFinger();
                } else {
                    Intent intent = new Intent();
                    intent.setClassName(PACKAGE_SETTINGS, "com.android.settings.fingerprint.RegisterFingerprint");
                    intent.putExtra("previousStage", "app_lock");
                    intent.putExtra("fingerIndex", 1);
                    intent.putExtra("fromApplock", true);
                    startActivityForResult(intent, LOCK_TYPE_FINGERPRINT);
                }
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void saveLockTypeForPPP(int lockType) {
        LogUtil.i(TAG, "mLockType:" + mLockType + ", lockType " + lockType);
        mLockType = lockType;
        Secure.putInt(getContentResolver(), FreemeSettings.Secure.FREEME_APPLOCK_LOCK_TYPE, lockType);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        LogUtil.i(TAG, "requestCode:" + requestCode + ", received - resultCode:" + resultCode);
        AppLockTypeActivity settingsActivity = (AppLockTypeActivity) getActivity();
        int bSuccess = 0;
        switch (requestCode) {
            case LOCK_TYPE_PATTERN:
                if (resultCode > Activity.RESULT_CANCELED) {
                    if (AppLockUtil.isFingerPrint(AppLockUtil.getLockType(getActivity()))) {
                        requestCode += LOCK_TYPE_FINGERPRINT;
                    }
                    saveLockTypeForPPP(requestCode);
                    bSuccess = 1;
                    mPackageInfoUtil.setMasterValue(getActivity(), true);
                }
                settingsActivity.finishPreferencePanel(this, bSuccess, null);
                break;
            case LOCK_TYPE_PIN:
            case LOCK_TYPE_PASSWORD:
                if (resultCode != Activity.RESULT_CANCELED) {
                    if (AppLockUtil.isFingerPrint(AppLockUtil.getLockType(getActivity()))) {
                        requestCode += LOCK_TYPE_FINGERPRINT;
                    }
                    saveLockTypeForPPP(requestCode);
                    bSuccess = 1;
                    mPackageInfoUtil.setMasterValue(getActivity(), true);
                }
                settingsActivity.finishPreferencePanel(this, bSuccess, null);
                break;
            case LOCK_TYPE_FINGERPRINT:
                if (resultCode == Activity.RESULT_OK) {
                    setAppLockBackupKey(KEY_LOCK_TYPE_FINGER);
                } else {
                    settingsActivity.finishPreferencePanel(this, -1, null);
                }
                break;
            case CONFIRM_REQUEST_ENTER /*1000*/:
                if (resultCode != Activity.RESULT_OK) {
                    LogUtil.i(TAG, "CONFIRM_REQUEST_ENTER success");
                    return;
                }
                LogUtil.i(TAG, "Failed to CONFIRM_REQUEST_ENTER");
                settingsActivity.finishPreferencePanel(this, -1, null);
                break;
            case FOR_APP_LOCK_BACKUP_KEY /*1004*/:
                LogUtil.d(TAG, "FOR_APP_LOCK_BACKUP_KEY  Result Code = " + resultCode);
                if (resultCode != Activity.RESULT_OK) {
                    bSuccess = 1;
                    mPackageInfoUtil.setMasterValue(getActivity(), true);
                }
                settingsActivity.finishPreferencePanel(this, bSuccess, null);
                break;
            default:
                break;
        }
    }

    public void finish() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.onBackPressed();
        }
    }

    protected ContentResolver getContentResolver() {
        Context context = getActivity();
        if (context != null) {
            mContentResolver = context.getContentResolver();
        }
        return mContentResolver;
    }

    protected Object getSystemService(String name) {
        return getActivity().getSystemService(name);
    }

    private void setAppLockBackupKey(String key) {
        Intent mIntent = new Intent();
        mIntent.setClassName(PACKAGE_SETTINGS, ACTIVITY_CHOOSELOCKGENERIC);
        mIntent.putExtra("minimum_quality", DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
        mIntent.putExtra("hide_disabled_prefs", true);
        mIntent.putExtra(AppLockPolicy.KEY_FOR_APPLOCK_BACKUP, key);
        startActivityForResult(mIntent, FOR_APP_LOCK_BACKUP_KEY);
    }

    //*/ freeme.zhongkai.zhu. 20171027. applock
    private boolean isFingerprintEnabled() {
        int userId = ActivityManager.getCurrentUser();
        return mFingerprintManager != null && mFingerprintManager.isHardwareDetected()
                && !isFingerprintDisabled(userId);
    }
    private boolean isUnlockWithFingerprintPossible() {
        int userId = ActivityManager.getCurrentUser();
        return mFingerprintManager != null && mFingerprintManager.isHardwareDetected()
                && !isFingerprintDisabled(userId)
                && mFingerprintManager.getEnrolledFingerprints(userId).size() > 0;
    }

    private boolean isFingerprintDisabled(int userId) {
        final DevicePolicyManager dpm =
                (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && (dpm.getKeyguardDisabledFeatures(null, userId)
                & DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT) != 0
                /* || isSimPinSecure() */;
    }

    private void identifyFinger() {
        int lockType = AppLockUtil.getLockType(getActivity());
        if (lockType == AppLockPolicy.LOCK_TYPE_NONE) {
            setAppLockBackupKey(KEY_LOCK_TYPE_FINGER);
        } else {
            if (AppLockUtil.isFingerPrint(lockType)) {
                int basicLockType = AppLockUtil.getBasicLockType(getActivity());
                Settings.Secure.putInt(getActivity().getContentResolver(),
                        FreemeSettings.Secure.FREEME_APPLOCK_LOCK_TYPE, basicLockType);
                mLockTypeFinger.setChecked(false);
            } else {
                lockType += AppLockPolicy.LOCK_TYPE_FINGERPRINT;
                Settings.Secure.putInt(getActivity().getContentResolver(),
                        FreemeSettings.Secure.FREEME_APPLOCK_LOCK_TYPE, lockType);
                mLockTypeFinger.setChecked(true);
            }
        }
    }
    //*/
}

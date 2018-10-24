//package com.freeme.dialer.settings;
//
//import android.os.Bundle;
//import android.preference.PreferenceFragment;
//import android.preference.SwitchPreference;
//import android.preference.Preference;
//import android.preference.PreferenceScreen;
//import android.provider.Settings;
//
//import com.android.dialer.R;
//import com.freeme.provider.FreemeSettings;
//
//public class FreemePhoneOtherSetting extends PreferenceFragment
//        implements Preference.OnPreferenceChangeListener {
//
//    private static final String BUTTON_OTHERS_GRADIENT_RING_KEY = "gradient_ring_key";
//    private static final String BUTTON_OTHERS_REVERSE_SILENT_KEY = "reverse_silent_key";
//    private static final String BUTTON_PHONE_VIBRATE_KEY = "phone_vibrate_key";
//    private static final String BUTTON_OTHERS_POCKET_MODE_KEY = "pocket_mode_ring_key";
//
//    private SwitchPreference mPhoneVibrat;
//    private SwitchPreference mReverseSilent;//reverse mute
//    private SwitchPreference mGradientRing;//gradient ring
//    private SwitchPreference mPocketMode;
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        addPreferencesFromResource(R.xml.freeme_phone_other_setting);
//        mReverseSilent = (SwitchPreference) findPreference(BUTTON_OTHERS_REVERSE_SILENT_KEY);
//        mGradientRing = (SwitchPreference) findPreference(BUTTON_OTHERS_GRADIENT_RING_KEY);
//        mPocketMode = (SwitchPreference) findPreference(BUTTON_OTHERS_POCKET_MODE_KEY);
//        mPhoneVibrat = (SwitchPreference) findPreference(BUTTON_PHONE_VIBRATE_KEY);
//    }
//
//    @Override
//    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
//        return true;
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        initPreferenceStatus(mReverseSilent);
//        initPreferenceStatus(mGradientRing);
//        initPreferenceStatus(mPocketMode);
//        initPreferenceStatus(mPhoneVibrat);
//    }
//
//    private void initPreferenceStatus(SwitchPreference preference) {
//        if (preference == null) {
//            return;
//        }
//        String key = getKey(preference);
//        boolean isOn = getStatusValue(key, 0) == 1;
//        preference.setChecked(isOn);
//        preference.setOnPreferenceChangeListener(this);
//    }
//
//    @Override
//    public boolean onPreferenceChange(Preference preference, Object objValue) {
//        String key = getKey(preference);
//        boolean isOn = getStatusValue(key, 0) == 1;
//        saveStatusValue(key, isOn ? 0 : 1);
//        return true;
//    }
//
//    private String getKey(Preference preference) {
//        if (preference == mReverseSilent) {
//            return FreemeSettings.System.FREEME_REVERSE_SILENT_SETTING;
//        } else if (preference == mGradientRing) {
//            return FreemeSettings.System.FREEME_GRADIENT_RING_KEY;
//        } else if (preference == mPocketMode) {
//            return FreemeSettings.System.FREEME_POCKET_MODE_KEY;
//        } else if (preference == mPhoneVibrat) {
//            return FreemeSettings.System.FREEME_PHONE_VIBRAT_KEY;
//        }
//        return null;
//    }
//
//    private int getStatusValue(String key, int defValue) {
//        if (key == null) {
//            return defValue;
//        }
//        return Settings.System.getInt(getContext().getContentResolver(), key, defValue);
//    }
//
//    private void saveStatusValue(String key, int value) {
//        if (key == null) {
//            return;
//        }
//        Settings.System.putInt(getContext().getContentResolver(), key, value);
//    }
//}
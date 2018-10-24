package com.example.freeme.apis.preference;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.freeme.preference.FreemeFullscreenListPreference;

import com.example.freeme.apis.R;

public class FreemeStylePreferences extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {

    private static final String PREF_KEY_FULLSCREEN_LIST = "fullscreen_list_preference";
    private FreemeFullscreenListPreference mFullscreenListPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_freeme_style);

        mFullscreenListPreference = (FreemeFullscreenListPreference) findPreference(
                PREF_KEY_FULLSCREEN_LIST);
        mFullscreenListPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateFullscreenListPreference(null);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mFullscreenListPreference) {
            updateFullscreenListPreference((String) newValue);
            return true;
        }
        return false;
    }

    private void updateFullscreenListPreference(String value) {
        int index = mFullscreenListPreference.findIndexOfValue(
                value != null ? value : mFullscreenListPreference.getValue());
         if (index >= 0) {
             CharSequence entry = mFullscreenListPreference.getEntries()[index];
             /*/
             mFullscreenListPreference.setStatusText1(entry);
             /*/
             mFullscreenListPreference.setSummary(entry);
             //*/
         }
    }
}

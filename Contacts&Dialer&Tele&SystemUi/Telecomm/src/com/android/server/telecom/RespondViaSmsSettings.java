/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.telecom;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.telecom.Log;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuItem;

//*/ freeme.liqiang, 20180320. modify back title in dialer settings
import android.content.Intent;
import com.freeme.actionbar.app.FreemeActionBarUtil;
//*/
//*/ freeme.zhaozehong, 20180514. do not save empty sms
import android.text.TextUtils;
import android.widget.Toast;
//*/

// TODO: This class is newly copied into Telecom (com.android.server.telecom) from it previous
// location in Telephony (com.android.phone). User's preferences stored in the old location
// will be lost. We need code here to migrate KLP -> LMP settings values.

/**
 * Settings activity to manage the responses available for the "Respond via SMS Message" feature to
 * respond to incoming calls.
 */
public class RespondViaSmsSettings extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {

    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.d(this, "Settings: onCreate()...");

        // This function guarantees that QuickResponses will be in our
        // SharedPreferences with the proper values considering there may be
        // old QuickResponses in Telephony pre L.
        QuickResponseUtils.maybeMigrateLegacyQuickResponses(this);

        getPreferenceManager().setSharedPreferencesName(QuickResponseUtils.SHARED_PREFERENCES_NAME);
        mPrefs = getPreferenceManager().getSharedPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            preferenceScreen.removeAll();
        }

        // This preference screen is ultra-simple; it's just 4 plain
        // <EditTextPreference>s, one for each of the 4 "canned responses".
        //
        // The only nontrivial thing we do here is copy the text value of
        // each of those EditTextPreferences and use it as the preference's
        // "title" as well, so that the user will immediately see all 4
        // strings when they arrive here.
        //
        // Also, listen for change events (since we'll need to update the
        // title any time the user edits one of the strings.)

        addPreferencesFromResource(R.xml.respond_via_sms_settings);
        initPref(findPreference(QuickResponseUtils.KEY_CANNED_RESPONSE_PREF_1));
        initPref(findPreference(QuickResponseUtils.KEY_CANNED_RESPONSE_PREF_2));
        initPref(findPreference(QuickResponseUtils.KEY_CANNED_RESPONSE_PREF_3));
        initPref(findPreference(QuickResponseUtils.KEY_CANNED_RESPONSE_PREF_4));

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
            //*/ freeme.liqiang, 20180320. modify back title in dialer settings
            setNavigateTitle(actionBar, getIntent());
            //*/
        }
    }

    // Preference.OnPreferenceChangeListener implementation
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(this, "onPreferenceChange: key = %s", preference.getKey());
        Log.d(this, "  preference = '%s'", preference);
        Log.d(this, "  newValue = '%s'", newValue);

        //*/freeme.zhaozehong, 20160624. do not save empty sms
        if (TextUtils.isEmpty((String) newValue)) {
            Toast.makeText(this, R.string.freeme_reject_sms_empty_forbid,
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        //*/

        EditTextPreference pref = (EditTextPreference) preference;

        // Copy the new text over to the title, just like in onCreate().
        // (Watch out: onPreferenceChange() is called *before* the
        // Preference itself gets updated, so we need to use newValue here
        // rather than pref.getText().)
        pref.setTitle((String) newValue);

        // Save the new preference value.
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(pref.getKey(), (String) newValue).commit();

        return true;  // means it's OK to update the state of the Preference with the new value
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                goUpToTopLevelSetting(this);
                return true;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Finish current Activity and go up to the top level Settings.
     */
    public static void goUpToTopLevelSetting(Activity activity) {
        activity.finish();
    }

    /**
     * Initialize the preference to the persisted preference value or default text.
     */
    private void initPref(Preference preference) {
        EditTextPreference pref = (EditTextPreference) preference;
        pref.setText(mPrefs.getString(pref.getKey(), pref.getText()));
        pref.setTitle(pref.getText());
        pref.setOnPreferenceChangeListener(this);
    }

    //*/ freeme.liqiang, 20180320. modify back title in dialer settings
    private String getSubTitle(Intent intent) {
        String title = null;
        if (intent != null) {
            title = intent.getStringExtra(FreemeActionBarUtil.EXTRA_NAVIGATE_UP_TITLE_TEXT);
        }
        return title;
    }

    private void setNavigateTitle(ActionBar actionBar, Intent intent) {
        if (actionBar != null) {
            FreemeActionBarUtil.setBackTitle(actionBar, getSubTitle(intent));
        }
    }
    //*/
}

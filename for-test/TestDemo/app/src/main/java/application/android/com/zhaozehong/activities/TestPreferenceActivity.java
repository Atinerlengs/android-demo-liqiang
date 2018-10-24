package application.android.com.zhaozehong.activities;


import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.Nullable;

import application.android.com.zhaozehong.demoapplication.R;

public class TestPreferenceActivity extends PreferenceActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.activity_preference);
    }
}

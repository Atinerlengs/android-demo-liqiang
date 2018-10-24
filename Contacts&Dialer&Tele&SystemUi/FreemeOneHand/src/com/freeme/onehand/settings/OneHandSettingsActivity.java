package com.freeme.onehand.settings;

import android.content.Intent;
import android.os.Bundle;

import com.android.settings.SettingsActivity;

import com.freeme.actionbar.app.FreemeActionBarUtil;
import com.freeme.onehand.R;

public class OneHandSettingsActivity extends SettingsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FreemeActionBarUtil.setNavigateTitle(this, getIntent());
    }

    @Override
    public Intent getIntent() {
        return super.getIntent()
                .putExtra(EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, true)
                .putExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID, R.string.onehand_settings_title);
    }
}

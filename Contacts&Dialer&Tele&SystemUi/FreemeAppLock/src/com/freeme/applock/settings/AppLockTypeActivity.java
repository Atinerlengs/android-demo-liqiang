package com.freeme.applock.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.freeme.actionbar.app.FreemeActionBarUtil;

public class AppLockTypeActivity extends Activity {
    private static final int RESULT_CODE_SUCCESS = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FreemeActionBarUtil.setNavigateTitle(this,getIntent());

        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new AppLockTypeFragment())
                .commitAllowingStateLoss();
    }

    public void finishPreferencePanel(Fragment caller, int resultCode, Intent resultData) {
        if (resultCode != RESULT_CODE_SUCCESS) {
            return;
        }

        onBackPressed();
        if (caller != null && caller.getTargetFragment() != null) {
            caller.getTargetFragment()
                  .onActivityResult(caller.getTargetRequestCode(), resultCode, resultData);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }
}

package com.freeme.filemanager.activity;

import android.app.Activity;
import android.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;

import com.freeme.filemanager.util.PermissionUtil;

public class BaseActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!PermissionUtil.hasSecurityPermissions(this)) {
            //ignore event if has no permission
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }
}

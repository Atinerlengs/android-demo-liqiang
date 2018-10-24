package com.freeme.safe.view.preview;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.freeme.filemanager.util.PermissionUtil;
import com.freeme.safe.helper.HomeBroadcastListener;
import com.freeme.safe.helper.HomeBroadcastReceiver;

public class BasePreviewActivity extends Activity {

    private HomeBroadcastReceiver mHomeBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionUtil.checkSecurityPermissions(this);
        registerHomeBroadcastReceiver();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterHomeBroadcastReceiver();
    }

    private void registerHomeBroadcastReceiver() {
        mHomeBroadcastReceiver = new HomeBroadcastReceiver();
        mHomeBroadcastReceiver.setOnHomeBroadcastListener(new HomeBroadcastListener() {
            @Override
            public void onReceiveListener() {
                finish();
            }
        });
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mHomeBroadcastReceiver, iFilter);
    }

    private void unregisterHomeBroadcastReceiver() {
        if (mHomeBroadcastReceiver != null) {
            unregisterReceiver(mHomeBroadcastReceiver);
            mHomeBroadcastReceiver = null;
        }
    }
}

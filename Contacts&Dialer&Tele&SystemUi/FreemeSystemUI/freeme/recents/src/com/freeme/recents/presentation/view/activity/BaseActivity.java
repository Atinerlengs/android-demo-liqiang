package com.freeme.recents.presentation.view.activity;

import android.app.Activity;
import android.os.Bundle;

import com.freeme.recents.RecentsUtils;
import com.freeme.recents.presentation.event.FreemeEventBus;

public class BaseActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FreemeEventBus.getDefault().register(this, RecentsUtils.EVENT_BUS_PRIORITY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FreemeEventBus.getDefault().unregister(this);
    }
}

package com.freeme.game.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.freeme.game.floatingui.GmFloatingUIManager;

public class GmToolService extends Service {

    private GmFloatingUIManager mManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mManager = new GmFloatingUIManager(getApplicationContext());
        mManager.showFloatingView(GmFloatingUIManager.ViewType.VIEW_TYPE_FLOAT_BUTTON);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mManager.onDestroy();
    }
}

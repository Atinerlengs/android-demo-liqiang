package com.freeme.filemanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;

import com.freeme.filemanager.model.GlobalConsts;
import com.freeme.filemanager.util.EntriesManager;
import com.freeme.filemanager.util.StorageHelper;
import com.freeme.filemanager.util.SettingProperties;
import com.freeme.filemanager.util.Util;

public class FMApplication extends Application {
    public static final String LOG_TAG = "FMApplication";
    private ScannerReceiver mScannerReceiver;
    private List<SDCardChangeListener> listeners = new ArrayList<SDCardChangeListener>();

    private static final int MSG_SDCARD_CHANGED = 0x01;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case MSG_SDCARD_CHANGED:
                int flag = msg.arg1;
                for (int i = 0; i < listeners.size(); i++) {
                    SDCardChangeListener listener = listeners.get(i);
                    if (listener != null) {
                        listener.onMountStateChange(flag);
                    }
                }
                break;
            default:
                break;
            }
        };
    };

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
            builder.detectFileUriExposure();
        }

        registMountListener();
    }

    private void registMountListener() {
        mScannerReceiver = new ScannerReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.setPriority(1000);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_CHECKING);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addDataScheme("file");
        registerReceiver(mScannerReceiver, intentFilter);
    }

    public void addSDCardChangeListener(SDCardChangeListener listener) {
        listeners.add(listener);
    }

    public void removeSDCardChangeListener(SDCardChangeListener listener) {
        listeners.remove(listener);
    }

    public interface SDCardChangeListener {
        public int flag_INJECT = 0x01;
        public int flag_UMMOUNT = 0x02;

        public void onMountStateChange(int flag);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        unregisterReceiver(mScannerReceiver);
        EntriesManager.releaseEntries();
    }

    public class ScannerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(LOG_TAG, "action = " + action);
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                StorageHelper.getInstance(FMApplication.this).release();
                StorageHelper.getInstance(FMApplication.this);
                mHandler.obtainMessage(MSG_SDCARD_CHANGED, SDCardChangeListener.flag_INJECT,0).sendToTarget();
            } else if (action.equals(Intent.ACTION_MEDIA_EJECT) || action.equals(Intent.ACTION_MEDIA_UNMOUNTED) || action.equals(Intent.ACTION_MEDIA_BAD_REMOVAL)) {
                StorageHelper.getInstance(FMApplication.this).release();
                StorageHelper.getInstance(FMApplication.this);
                mHandler.obtainMessage(MSG_SDCARD_CHANGED, SDCardChangeListener.flag_UMMOUNT,0).sendToTarget();
            } else if (action.equals(GlobalConsts.BROADCAST_REFRESH)) {
            }
        }
    }
}

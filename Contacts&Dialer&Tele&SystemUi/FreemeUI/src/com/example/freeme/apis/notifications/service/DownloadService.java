package com.example.freeme.apis.notifications.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import com.example.freeme.apis.notifications.util.NotificationUtil;

public class DownloadService extends Service {

    private static final String TAG = "DownloadService";
    private Handler mHandler;
    private int progress = 0;
    private Runnable runnable;
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(getMainLooper());
        runnable = new Runnable() {
            @Override
            public void run() {
                if(progress>99){
                    progress=0;
                    NotificationUtil.cancelNotification();
                }else{
                    NotificationUtil.showDownloadNotification(DownloadService.this, progress);
                    progress++;
                    mHandler.postDelayed(runnable,500);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent==null){
            return super.onStartCommand(intent, flags, startId);
        }
        int command = intent.getIntExtra("command",0);
        if(command==1){
            progress=0;
            mHandler.removeCallbacks(runnable);
            NotificationUtil.cancelNotification();
        }else {
            if (progress < 1) {
                mHandler.post(runnable);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(runnable);
        NotificationUtil.cancelNotification();
    }
}

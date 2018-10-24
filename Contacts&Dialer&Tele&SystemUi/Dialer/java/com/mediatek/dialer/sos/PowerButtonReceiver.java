package com.mediatek.dialer.sos;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.util.Log;


public class PowerButtonReceiver extends BroadcastReceiver {
     private static final String TAG = "PowerButtonReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean mIsSupportSOS = SystemProperties.get("persist.mtk_sos_quick_dial").equals("1");
        Log.d(TAG, "Boot Completed, SOS support:" + mIsSupportSOS);
        if (mIsSupportSOS) {
            ComponentName serviceComponent = new ComponentName(context,
                             PowerButtonJobService.class);
            JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
            builder.setMinimumLatency(1 * 1000); // wait at least
            builder.setOverrideDeadline(3 * 1000); // maximum delay
            JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
            jobScheduler.schedule(builder.build());
        }
    }
}
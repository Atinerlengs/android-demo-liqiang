package com.mediatek.dialer.sos;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;
/**
 * JobService to be scheduled by the JobScheduler.
 * start another service
 */
public class PowerButtonJobService extends JobService {
    private static final String TAG = "PowerButtonJobService";

    @Override
    public boolean onStartJob(JobParameters params) {
        try {
            Intent service = new Intent(getApplicationContext(), PowerButtonReceiverService.class);
            getApplicationContext().startService(service);
        } catch (Exception e) {
            Log.e(TAG, "PowerButtonReceiverService Not running");
        } finally {
            ComponentName serviceComponent = new ComponentName(getApplicationContext(),
                                    PowerButtonJobService.class);
            JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
            builder.setMinimumLatency(1 * 1000); // wait at least
            builder.setOverrideDeadline(3 * 1000); // maximum delay
            JobScheduler jobScheduler = getApplicationContext()
                                 .getSystemService(JobScheduler.class);
            jobScheduler.schedule(builder.build());
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

}
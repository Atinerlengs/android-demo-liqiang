package com.freeme.dialer.contacts.list.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.freeme.contacts.common.utils.FreemeLogUtils;
import com.freeme.dialer.app.DialerApplicationEx;
import com.freeme.dialer.contacts.FreemeDeleteProcessor;
import com.freeme.dialer.contacts.FreemeProcessorBase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;


public class FreemeMultiChoiceService extends Service {

    private static final String TAG = "FreemeMultiChoiceService";

    // Should be single thread, as we don't want to simultaneously handle import
    // and export requests.
    private final ExecutorService mExecutorService = DialerApplicationEx.getApplicationTaskService();

    // Stores all unfinished import/export jobs which will be executed by
    // mExecutorService. Key is jobId.
    private static final Map<Integer, FreemeProcessorBase> RUNNINGJOBMAP =
            new HashMap<Integer, FreemeProcessorBase>();

    public static final int TYPE_DELETE = 2;

    private static int sCurrentJobId;

    private FreemeMultiChoiceService.MyBinder mBinder;

    public class MyBinder extends Binder {
        public FreemeMultiChoiceService getService() {
            return FreemeMultiChoiceService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new FreemeMultiChoiceService.MyBinder();
        FreemeLogUtils.d(TAG, "[onCreate]Multi-choice Service is being created.");

        // / change for low_memory kill Contacts process CR.
        // startForeground(1, new Notification());
    }

    /**
     * M: change for low_memory kill Contacts process @{ reference CR:
     * ALPS00564966,ALPS00567689,ALPS00567905
     **/
    @Override
    public void onDestroy() {
        // stopForeground(true);
        super.onDestroy();
        STATUS = STATUS_IDLE;
    }

    /**
     * @}
     */

    @Override
    public int onStartCommand(Intent intent, int flags, int id) {
        // / M: change START_STICKY to START_NOT_STICKY for Service slim
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Tries to call {@link ExecutorService#execute(Runnable)} toward a given
     * processor.
     *
     * @return true when successful.
     */
    private synchronized boolean tryExecute(FreemeProcessorBase processor) {
        try {
            FreemeLogUtils.d(TAG, "[tryExecute]Executor service status: shutdown: "
                    + mExecutorService.isShutdown() + ", terminated: "
                    + mExecutorService.isTerminated());
            mExecutorService.execute(processor);
            RUNNINGJOBMAP.put(sCurrentJobId, processor);
            return true;
        } catch (RejectedExecutionException e) {
            FreemeLogUtils.w(TAG, "[tryExecute]Failed to excetute a job:" + e);
            return false;
        }
    }

    public synchronized void handleDeleteRequest(List<FreemeMultiChoiceRequest> requests,
                                                 FreemeMultiChoiceHandlerListener listener) {
        sCurrentJobId++;
        FreemeLogUtils.i(TAG, "[handleDeleteRequest]sCurrentJobId:" + sCurrentJobId);
        if (tryExecute(new FreemeDeleteProcessor(this, listener, requests, sCurrentJobId))) {
            if (listener != null) {
                listener.onProcessed(TYPE_DELETE, sCurrentJobId, 0, -1,
                        requests.get(0).mContactName);
            }
        }
    }

    public synchronized void handleCancelRequest(FreemeMultiChoiceCancelRequest request) {
        final int jobId = request.jobId;
        FreemeLogUtils.i(TAG, "[handleCancelRequest]jobId:" + jobId);
        final FreemeProcessorBase processor = RUNNINGJOBMAP.remove(jobId);

        if (processor != null) {
            processor.cancel(true);
        } else {
            FreemeLogUtils.w(TAG, "[handleCancelRequest]"
                    + String.format("Tried to remove unknown job (id: %d)", jobId));
        }
        stopServiceIfAppropriate();
    }

    /**
     * Checks job list and call {@link #stopSelf()} when there's no job and no
     * scanner connection is remaining. A new job (import/export) cannot be
     * submitted any more after this call.
     */
    private synchronized void stopServiceIfAppropriate() {
        if (RUNNINGJOBMAP.size() > 0) {
            for (final Map.Entry<Integer, FreemeProcessorBase> entry : RUNNINGJOBMAP.entrySet()) {
                final int jobId = entry.getKey();
                final FreemeProcessorBase processor = entry.getValue();
                if (processor.isDone()) {
                    RUNNINGJOBMAP.remove(jobId);
                } else {
                    FreemeLogUtils.i(TAG,
                            "[stopServiceIfAppropriate]"
                                    + String.format("Found unfinished job (id: %d)", jobId));
                    return;
                }
            }
        }

        FreemeLogUtils.i(TAG, "[stopServiceIfAppropriate]No unfinished job. Stop this service.");
        // mExecutorService.shutdown();
        stopSelf();
    }

    public synchronized void handleFinishNotification(int jobId, boolean successful) {
        FreemeLogUtils.i(TAG, "[handleFinishNotification]jobId = " + jobId + ",successful = "
                + successful);
        if (RUNNINGJOBMAP.remove(jobId) == null) {
            FreemeLogUtils.w(
                    TAG,
                    "[handleFinishNotification]"
                            + String.format("Tried to remove unknown job (id: %d)", jobId));
        }
        stopServiceIfAppropriate();
    }

    public static synchronized boolean isProcessing(int requestType) {
        if (RUNNINGJOBMAP.size() <= 0) {
            FreemeLogUtils.w(TAG, "[isProcessing] size is <=0,return false!");
            return false;
        }

        if (RUNNINGJOBMAP.size() > 0) {
            for (final Map.Entry<Integer, FreemeProcessorBase> entry : RUNNINGJOBMAP.entrySet()) {
                final FreemeProcessorBase processor = entry.getValue();
                if (processor.getType() == requestType) {
                    FreemeLogUtils.i(TAG, "[isProcessing]return true,requestType = " + requestType);
                    return true;
                }
            }
        }

        return false;
    }

    public static final int STATUS_IDLE     = 0;
    public static final int STATUS_DELETING = 1;

    public static int STATUS = STATUS_IDLE;

    public static boolean isCanDelete() {
        return STATUS == STATUS_IDLE;
    }
}

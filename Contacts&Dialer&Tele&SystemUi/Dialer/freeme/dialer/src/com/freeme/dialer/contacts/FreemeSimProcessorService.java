package com.freeme.dialer.contacts;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.freeme.contacts.common.utils.FreemeLogUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class FreemeSimProcessorService extends Service {

    private final String TAG = this.getClass().getSimpleName();

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 10;
    private static final int KEEP_ALIVE_TIME = 10; // 10 seconds

    private FreemeSimProcessorManager mProcessorManager;
    private AtomicInteger mNumber = new AtomicInteger();
    private final ExecutorService mExecutorService = createThreadPool(CORE_POOL_SIZE);

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FreemeLogUtils.i(TAG, "[onCreate]...");
        mProcessorManager = new FreemeSimProcessorManager(this, mListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int id) {
        super.onStartCommand(intent, flags, id);
        processIntent(intent);
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FreemeLogUtils.i(TAG, "[onDestroy]...");
    }

    private void processIntent(Intent intent) {
        if (intent == null) {
            FreemeLogUtils.w(TAG, "[processIntent] intent is null.");
            return;
        }
        int subId = intent.getIntExtra(FreemeSimServiceUtils.SERVICE_SUBSCRIPTION_KEY, 0);
        mProcessorManager.handleProcessor(getApplicationContext(), subId, intent);
    }

    private FreemeSimProcessorManager.ProcessorManagerListener mListener =
            new FreemeSimProcessorManager.ProcessorManagerListener() {
                @Override
                public void addProcessor(long scheduleTime, FreemeProcessorBase processor) {
                    if (processor != null) {
                        try {
                            mExecutorService.execute(processor);
                        } catch (RejectedExecutionException e) {
                            FreemeLogUtils.e(TAG, "[addProcessor] RejectedExecutionException", e);
                            // ALPS03527261: callback to UI for doing some action, e.g.change status
                            if (processor instanceof FreemeSimProcessorBase) {
                                mProcessorManager.onAddProcessorFail((FreemeSimProcessorBase) processor);
                            }
                        }
                    }
                }

                @Override
                public void onAllProcessorsFinished() {
                    FreemeLogUtils.d(TAG, "[onAllProcessorsFinished]...");
                    stopSelf();
                    mExecutorService.shutdown();
                }
            };

    private ExecutorService createThreadPool(int initPoolSize) {
        return new ThreadPoolExecutor(initPoolSize, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                String threadName = "SIM Service - " + mNumber.getAndIncrement();
                FreemeLogUtils.d(TAG, "[createThreadPool]thread name:" + threadName);
                return new Thread(r, threadName);
            }
        });
    }
}

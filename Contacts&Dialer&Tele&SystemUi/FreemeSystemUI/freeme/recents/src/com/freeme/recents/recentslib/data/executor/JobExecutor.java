package com.freeme.recents.recentslib.data.executor;

import android.support.annotation.NonNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class JobExecutor implements Executor {
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    private final BlockingQueue<Runnable> workQueue;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final ThreadFactory threadFactory;
    private static JobExecutor sInstance;

    private JobExecutor() {
        this.workQueue = new LinkedBlockingQueue ();
        this.threadFactory = new JobThreadFactory ();
        this.threadPoolExecutor = new ThreadPoolExecutor (3, 10, 10L, KEEP_ALIVE_TIME_UNIT, this.workQueue, this.threadFactory);
    }

    public static JobExecutor getInstance() {
        if (sInstance == null) {
            sInstance = new JobExecutor ();
        }
        return sInstance;
    }

    public void execute(@NonNull Runnable runnable) {
        if (runnable == null) {
            throw new IllegalArgumentException ("Runnable to execute cannot be null");
        }
        this.threadPoolExecutor.execute (runnable);
    }

    private static class JobThreadFactory
            implements ThreadFactory {
        private static final String THREAD_NAME = "android_";
        private int counter = 0;

        public Thread newThread(@NonNull Runnable runnable) {
            return new Thread (runnable, "android_" + this.counter++);
        }
    }
}



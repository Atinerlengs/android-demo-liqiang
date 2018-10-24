package com.freeme.systemui.utils;

import android.os.Handler;
import android.os.HandlerThread;

public class SystemUIThread {
    private static Handler mSubThread = null;
    private static Handler mUIThread = null;

    public static abstract class SimpleAsyncTask {
        public boolean runInThread() {
            return true;
        }

        public void runInUI() {
        }
    }

    public static void init() {
        HandlerThread mSubHandlerThread = new HandlerThread("SystemUIApplication_subThread");
        mSubHandlerThread.start();
        mSubThread = new Handler(mSubHandlerThread.getLooper());
        mUIThread = new Handler();
    }

    public static void runAsync(final SimpleAsyncTask async) {
        mSubThread.post(new Runnable() {
            @Override
            public void run() {
                if (async.runInThread()) {
                    Handler handler = SystemUIThread.mUIThread;
                    final SimpleAsyncTask simpleAsyncTask = async;
                    handler.post(new Runnable() {
                        public void run() {
                            simpleAsyncTask.runInUI();
                        }
                    });
                }
            }
        });
    }
}

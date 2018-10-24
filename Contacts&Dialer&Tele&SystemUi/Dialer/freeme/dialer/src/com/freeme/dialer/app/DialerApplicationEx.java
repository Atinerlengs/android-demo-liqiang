package com.freeme.dialer.app;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DialerApplicationEx {

    private static final ExecutorService mSingleTaskService = Executors.newSingleThreadExecutor();

    public static ExecutorService getApplicationTaskService() {
        return mSingleTaskService;
    }

}

package com.freeme.dialer.contacts;

import android.content.Intent;

import com.freeme.contacts.common.utils.FreemeLogUtils;

public abstract class FreemeSimProcessorBase extends FreemeProcessorBase {
    private final String TAG = this.getClass().getSimpleName();

    private volatile boolean mCanceled;
    private volatile boolean mDone;

    protected FreemeSimProcessorManager.ProcessorCompleteListener mListener;
    protected Intent mIntent;

    public FreemeSimProcessorBase(Intent intent,
                                  FreemeSimProcessorManager.ProcessorCompleteListener listener) {
        mIntent = intent;
        mListener = listener;
    }

    @Override
    public int getType() {
        return 0;
    }

    @Override
    public void run() {
        try {
            doWork();
        } finally {
            FreemeLogUtils.d(TAG, "[run]finish: type = " + getType() + ",mDone = " + mDone
                    + ",thread id = " + Thread.currentThread().getId());
            mDone = true;
            if (mListener != null && !mCanceled) {
                mListener.onProcessorCompleted(mIntent);
            }
        }
    }

    public abstract void doWork();

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (mDone || mCanceled) {
            return false;
        }
        mCanceled = true;

        return true;
    }

    @Override
    public boolean isCancelled() {
        return mCanceled;
    }

    @Override
    public boolean isDone() {
        return mDone;
    }

    public boolean isRunning() {
        return !isDone() && !isCancelled();
    }

    /**
     * [ALPS01224227]for race condition, we should make sure the processor to be
     * removed is the instance itself
     *
     * @param intent to check whether it is the same as mIntent
     * @return
     */
    public boolean identifyIntent(Intent intent) {
        return mIntent.equals(intent);
    }
}

package com.freeme.safe.utils;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

public class StaticHandler<T> extends Handler {
    private static final String TAG = "StaticHandler";
    private WeakReference<T> mRef;

    public StaticHandler(T t) {
        mRef = new WeakReference(t);
    }

    @Override
    public void handleMessage(Message msg) {
        T t = mRef.get();
        if (t == null) {
            Log.w(TAG, "ref.get is null.");
            return;
        }
        handleMessage(msg, t);
        super.handleMessage(msg);
    }

    protected void handleMessage(Message msg, T t) {
    }
}

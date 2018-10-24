package com.freeme.onehand;

import android.os.RemoteException;
import android.util.Log;

import com.freeme.internal.policy.IOneHandWatcher;

public final class OneHandServiceWatcher extends IOneHandWatcher.Stub {
    private static final String TAG = "OneHandServiceWatcher";

    @Override
    public void onInputFilterChanged() throws RemoteException {
        Log.d(TAG, "onInputFilterChanged()");
    }

    @Override
    public void onMagnificationSpecChaned() throws RemoteException {
        Log.d(TAG, "onMagnificationSpecChaned()");
    }
}

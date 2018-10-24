package com.freeme.game.receiver;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GmAppInstallReceiver extends BroadcastReceiver {

    public interface IAppChangedCallBack {
        void onAppChanged();
    }

    private static ArrayList<IAppChangedCallBack> mCallBacks = new ArrayList<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
        if (!replacing) {
            if (Intent.ACTION_PACKAGE_ADDED.equals(action)
                    || Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                for (IAppChangedCallBack callBack : mCallBacks) {
                    if (callBack != null) {
                        callBack.onAppChanged();
                    }
                }
            }
        }
    }

    public static void registerCallBack(IAppChangedCallBack callBack) {
        if (callBack != null && !mCallBacks.contains(callBack)) {
            mCallBacks.add(callBack);
        }
    }

    public static void unregisterCallBack(IAppChangedCallBack callBack) {
        if (callBack != null) {
            mCallBacks.remove(callBack);
        }
    }
}

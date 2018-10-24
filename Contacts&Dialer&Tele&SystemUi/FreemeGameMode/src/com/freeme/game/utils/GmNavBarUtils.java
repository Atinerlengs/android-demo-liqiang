package com.freeme.game.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.UserHandle;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.freeme.content.FreemeIntent;
import com.freeme.util.FreemeOption;

/**
 * To get NavigationBar height and determine the NavigationBar is displayed or not
 */
public class GmNavBarUtils {

    public interface INavBarStatusChangedCallBack {
        void onNavBarStatusChanged();
    }

    private Context mContext;
    private WindowManager mWindowManager;

    private boolean mIsRegister;
    private boolean mIsNavShow;
    private int mNavHeight;

    private List<INavBarStatusChangedCallBack> mCallBacks = Collections.synchronizedList(new ArrayList<>());

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (FreemeIntent.ACTION_NAVIGATIONBAR_STATUS_CHANGED.equals(action)) {
                mIsNavShow = !intent.getBooleanExtra(FreemeIntent.EXTRA_CHANGED_STATUS, false);
                for (INavBarStatusChangedCallBack callBack : mCallBacks)
                    if (callBack != null) {
                        callBack.onNavBarStatusChanged();
                    }
            }
        }
    };

    public GmNavBarUtils(Context context, WindowManager wm) {
        mContext = context;
        mWindowManager = wm;
        init();
    }

    private void init() {
        Resources res = mContext.getResources();
        int resourceId = res.getIdentifier("navigation_bar_height",
                "dimen", "android");
        if (resourceId > 0) {
            mNavHeight = res.getDimensionPixelSize(resourceId);
        }

        Display display = mWindowManager.getDefaultDisplay();
        Point size = new Point();
        Point realSize = new Point();
        display.getSize(size);
        display.getRealSize(realSize);
        int rotation = mWindowManager.getDefaultDisplay().getRotation();
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            mIsNavShow = realSize.x != size.x;
        } else {
            mIsNavShow = realSize.y != size.y;
        }
    }

    public void start() {
        mCallBacks.clear();
        mIsRegister = false;
        if (FreemeOption.Navigation.supports(FreemeOption.Navigation.FREEME_NAVIGATION_COLLAPSABLE)) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(FreemeIntent.ACTION_NAVIGATIONBAR_STATUS_CHANGED);
            mContext.registerReceiverAsUser(
                    mBroadcastReceiver, UserHandle.CURRENT_OR_SELF,
                    filter, null, null);
            mIsRegister = true;
        }
    }

    public void destroy() {
        if (mIsRegister && mBroadcastReceiver != null) {
            mContext.unregisterReceiver(mBroadcastReceiver);
            mIsRegister = false;
        }
        mCallBacks.clear();
    }

    public void registerNavBarCallBack(INavBarStatusChangedCallBack callBack) {
        mCallBacks.add(callBack);
    }

    public void unRegisterNavBarCallBack(INavBarStatusChangedCallBack callBack) {
        if (mCallBacks.contains(callBack)) {
            mCallBacks.remove(callBack);
        }
    }

    public boolean isNavBarShow() {
        return mIsNavShow;
    }

    public int getNavBarHeight() {
        return mNavHeight;
    }
}

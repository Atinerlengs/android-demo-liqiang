package com.freeme.game.floatingui;

import android.graphics.Point;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.freeme.game.utils.GmNavBarUtils;

abstract class GmScreenConfig {

    int mScreenW;
    int mScreenH;

    int mRotation;

    private GmNavBarUtils mNavBarUtils;
    private WindowManager mWindowManager;

    GmScreenConfig(GmFloatingUIManager manager) {
        mNavBarUtils = manager.getNavBarUtils();
        mWindowManager = manager.getWindowManager();
        mRotation = mWindowManager.getDefaultDisplay().getRotation();
    }

    boolean isLandScape() {
        return mRotation == Surface.ROTATION_90 || mRotation == Surface.ROTATION_270;
    }

    void initScreenSize() {
        Display display = mWindowManager.getDefaultDisplay();
        Point point = new Point();
        display.getRealSize(point);
        int diff = mNavBarUtils.isNavBarShow() ? mNavBarUtils.getNavBarHeight() : 0;
        if (isLandScape()) {
            mScreenH = Math.min(point.x, point.y);
            mScreenW = Math.max(point.x, point.y) - diff;
        } else {
            mScreenH = Math.max(point.x, point.y) - diff;
            mScreenW = Math.min(point.x, point.y);
        }
    }
}

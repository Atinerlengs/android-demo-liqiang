package com.freeme.game.floatingui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager.LayoutParams;

abstract class AbsGmFloatingView extends GmScreenConfig {

    Context mContext;

    View mMainView;
    LayoutParams mParams;

    GmFloatingUIManager mUIManager;

    ScreenOrientationListener mOrientationListener;

    class ScreenOrientationListener extends OrientationEventListener {

        ScreenOrientationListener(Context context) {
            super(context);
        }

        @Override
        public final void onOrientationChanged(int orientation) {
            int rotation = mUIManager.getWindowManager().getDefaultDisplay().getRotation();
            if (rotation != mRotation) {
                mRotation = rotation;
                initScreenSize();
                AbsGmFloatingView.this.onOrientationChanged();
            }
        }
    }

    AbsGmFloatingView(GmFloatingUIManager manager) {
        super(manager);
        mUIManager = manager;
        mContext = manager.getContext();
        mOrientationListener = new ScreenOrientationListener(mContext);

        initScreenSize();
    }

    public void show() {
        inflateViews();
        initParams();
        addViewToWindow();
    }

    private void inflateViews() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mMainView = inflate(inflater);
    }

    abstract View inflate(LayoutInflater inflater);

    private void initParams() {
        mParams = new LayoutParams();
        mParams.type = LayoutParams.TYPE_SYSTEM_ALERT;
        mParams.format = PixelFormat.RGBA_8888;
        mParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE |
                LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                LayoutParams.FLAG_FULLSCREEN;
        mParams.x = 0;
        mParams.y = 0;

        initParams(mParams);
    }

    abstract void initParams(LayoutParams params);

    void setLocation(int x, int y) {
        mParams.x = x;
        mParams.y = y;
    }

    void addViewToWindow() {
        registerGmFloatingUIAdapter();
        mUIManager.getViewHelper().addView(mMainView, mParams);
    }

    void removeViewFromWindow() {
        unregisterGmFloatingUIAdapter();
        mUIManager.getViewHelper().removeView(mMainView);
    }

    void updateViewLayout() {
        mUIManager.getViewHelper().updateViewLayout(mMainView, mParams);
    }

    abstract void registerGmFloatingUIAdapter();

    abstract void unregisterGmFloatingUIAdapter();

    abstract void onOrientationChanged();
}

package com.freeme.game.floatingui;

import android.graphics.Point;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.freeme.game.R;

public class GmFloatingButtonView extends AbsGmFloatingView {

    private final int MSG_START_TRANSLATE_ANIM = 10;
    private final int MSG_UPDATE_FLOATING_VIEW = 11;

    private final int TRANSLATE_STEP = 30;
    private final long ANIMATION_DELAYED = 1000;

    private final int TRANSLATE_TYPE_NONE = 0;
    private final int TRANSLATE_TYPE_TOP = 1;
    private final int TRANSLATE_TYPE_RIGHT = 2;
    private final int TRANSLATE_TYPE_LEFT = 3;
    private final int TRANSLATE_TYPE_BOTTOM = 4;

    private float mScaleX;
    private float mScaleY;

    private final int mViewSize;

    // y = kx + b
    // k = (y2-y1)/(x2-x1). slope of the line
    private float mK_OverZero; // the diagonal of the phone over the origin
    private float mK_Another;  // another diagonal of the phone
    private float mB;

    private Handler mHandler;
    private IGmFloatingUIConfig mButtonUIConfig;
    private View.OnTouchListener mTouchListener;

    GmFloatingButtonView(GmFloatingUIManager manager) {
        super(manager);
        mViewSize = mContext.getResources().getDimensionPixelSize(
                R.dimen.gm_floating_view_size);

        mScaleY = ((mScreenH - mViewSize) / 2) / (mScreenH * 1f);

        mHandler = new Handler(Looper.myLooper());
        mButtonUIConfig = new ButtonUIConfig();
        mTouchListener = new ViewTouchListener();
    }

    @Override
    final void initScreenSize() {
        Point size = new Point();
        mUIManager.getWindowManager().getDefaultDisplay().getRealSize(size);
        if (isLandScape()) {
            mScreenW = Math.max(size.x, size.y);
            mScreenH = Math.min(size.x, size.y);
        } else {
            mScreenW = Math.min(size.x, size.y);
            mScreenH = Math.max(size.x, size.y);
        }

        float x1 = 0f;
        float y1 = 0f;
        float x2 = mScreenW * 1f;
        float y2 = mScreenH * 1f;
        mK_OverZero = (y2 - y1) / (x2 - x1); // slope: k = (y2-y1)/(x2-x1)

        x1 = mScreenW * 1f;
        y1 = 0f;
        x2 = 0f;
        y2 = mScreenH * 1f;
        mK_Another = (y2 - y1) / (x2 - x1);
        mB = (mScreenH * 1f);                // y1 = kx1 + b, y2 = kx2 + b
    }

    @Override
    View inflate(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.gm_floating_button_view, null);

        view.setOnTouchListener(mTouchListener);
        view.setOnClickListener((View v) -> {
            mUIManager.showFloatingView(GmFloatingUIManager.ViewType.VIEW_TYPE_SETTING_PANEL);
            removeViewFromWindow();
        });
        return view;
    }

    @Override
    void initParams(WindowManager.LayoutParams params) {
        params.type = WindowManager.LayoutParams.TYPE_NAVIGATION_BAR_PANEL;
        params.gravity = Gravity.START | Gravity.TOP;
        params.alpha = 1f;
        params.x = (int) (mScreenW * mScaleX);
        params.y = (int) (mScreenH * mScaleY) - (mViewSize / 2);
        params.width = mViewSize;
        params.height = mViewSize;
    }

    @Override
    void addViewToWindow() {
        mHandler.removeMessages(MSG_START_TRANSLATE_ANIM);
        super.addViewToWindow();
        mHandler.sendMessageDelayed(
                mHandler.obtainMessage(MSG_START_TRANSLATE_ANIM, getTranslateType(), 0),
                ANIMATION_DELAYED);
    }

    @Override
    void removeViewFromWindow() {
        mHandler.removeMessages(MSG_START_TRANSLATE_ANIM);
        super.removeViewFromWindow();
    }


    @Override
    void registerGmFloatingUIAdapter() {
        mUIManager.registerGmFloatingUIAdapter(mButtonUIConfig);
    }

    @Override
    void unregisterGmFloatingUIAdapter() {
        mUIManager.unregisterGmFloatingUIAdapter(mButtonUIConfig);
    }

    @Override
    void onOrientationChanged() {
        initScreenSize();
        setLocation((int) (mScreenW * mScaleX), (int) (mScreenH * mScaleY));
        updateViewLayout();
    }

    void setFloatingViewVisible() {
        addViewToWindow();
        updateViewLayout();
    }

    private int getTranslateType() {
        float half = mViewSize / 2f;
        float cx = mParams.x + half;
        float cy = mParams.y + half;
        float y1 = mK_OverZero * cx;      // y = kx + b
        float y2 = mK_Another * cx + mB;
        if (cy < y1 && cy < y2) { // top
            return TRANSLATE_TYPE_TOP;
        } else if (cy < y1 && cy > y2) { // right
            return TRANSLATE_TYPE_RIGHT;
        } else if (cy > y1 && cy < y2) { // left
            return TRANSLATE_TYPE_LEFT;
        } else if (cy > y1 && cy > y2) { // bottom
            return TRANSLATE_TYPE_BOTTOM;
        } else {
            return TRANSLATE_TYPE_LEFT;
        }
    }

    private void startTranslateAnimation(int translateType) {
        mHandler.removeMessages(MSG_START_TRANSLATE_ANIM);
        int half = mViewSize / 2;
        boolean isNeedContinue = false;
        switch (translateType) {
            case TRANSLATE_TYPE_NONE: {
                int cx = mParams.x + half;
                int cy = mParams.y + half;
                if (cy == 0) {
                    setLocation(mParams.x, 0);
                } else if (cy == mScreenH) {
                    setLocation(mParams.x, mScreenH - mViewSize);
                } else if (cx == 0) {
                    setLocation(0, mParams.y);
                } else if (cx == mScreenW) {
                    setLocation(mScreenW - mViewSize, mParams.y);
                }
            }
            break;
            case TRANSLATE_TYPE_TOP: {
                int y = mParams.y - TRANSLATE_STEP;
                int cy = y - half;
                if (cy <= 0) {
                    setLocation(mParams.x, -half);
                } else {
                    setLocation(mParams.x, y);
                    isNeedContinue = true;
                }
            }
            break;
            case TRANSLATE_TYPE_RIGHT: {
                int x = mParams.x + TRANSLATE_STEP;
                int cx = x + half;
                if (cx >= mScreenW) {
                    setLocation(mScreenW - half, mParams.y);
                } else {
                    setLocation(x, mParams.y);
                    isNeedContinue = true;
                }
            }
            break;
            case TRANSLATE_TYPE_LEFT: {
                int x = mParams.x - TRANSLATE_STEP;
                int cx = x - half;
                if (cx <= 0) {
                    setLocation(-half, mParams.y);
                } else {
                    setLocation(x, mParams.y);
                    isNeedContinue = true;
                }
            }
            break;
            case TRANSLATE_TYPE_BOTTOM: {
                int y = mParams.y + TRANSLATE_STEP;
                int cy = y + half;
                if (cy >= mScreenH) {
                    setLocation(mParams.x, mScreenH - half);
                } else {
                    setLocation(mParams.x, y);
                    isNeedContinue = true;
                }
            }
            break;
            default:
                break;
        }
        if (translateType != TRANSLATE_TYPE_NONE && isNeedContinue) {
            mHandler.obtainMessage(MSG_START_TRANSLATE_ANIM, translateType, 0).sendToTarget();
        }
        updateViewLayout();
    }

    private class ViewTouchListener implements View.OnTouchListener {

        private boolean mIsFloatingBtnMoving;

        private int mDownX;
        private int mDownY;
        private int mLastX;
        private int mLastY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int x = (int) event.getRawX();
            int y = (int) event.getRawY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchDown(x, y);
                    break;

                case MotionEvent.ACTION_MOVE:
                    touchMove(x, y);
                    break;

                case MotionEvent.ACTION_UP:
                    touchUp();
                    break;
            }

            if (!mIsFloatingBtnMoving) {
                return false;
            } else {
                mMainView.setPressed(false);
                return true;
            }
        }

        private void touchDown(int x, int y) {
            mDownX = x;
            mDownY = y;
            mLastX = mDownX;
            mLastY = mDownY;
            mIsFloatingBtnMoving = false;
            mHandler.removeMessages(MSG_START_TRANSLATE_ANIM);
            mHandler.obtainMessage(MSG_START_TRANSLATE_ANIM,
                    TRANSLATE_TYPE_NONE, 0).sendToTarget();
        }

        private void touchMove(int x, int y) {
            int deltaX = x - mLastX;
            int deltaY = y - mLastY;
            mLastX = x;
            mLastY = y;
            if (Math.abs(deltaX) > 0 || Math.abs(deltaY) > 0) {
                mIsFloatingBtnMoving = true;

                int x1 = mParams.x + deltaX;
                int y1 = mParams.y + deltaY;

                setLocation(x1, y1);

                mScaleX = x1 / mScreenW;
                mScaleY = y1 / mScreenH;

                mHandler.sendEmptyMessage(MSG_UPDATE_FLOATING_VIEW);
            }
        }

        private void touchUp() {
            mHandler.removeMessages(MSG_START_TRANSLATE_ANIM);
            mHandler.sendMessageDelayed(
                    mHandler.obtainMessage(MSG_START_TRANSLATE_ANIM, getTranslateType(), 0),
                    mIsFloatingBtnMoving ? 0 : ANIMATION_DELAYED);
        }
    }

    private class Handler extends android.os.Handler {

        Handler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_TRANSLATE_ANIM:
                    startTranslateAnimation(msg.arg1);
                    break;
                case MSG_UPDATE_FLOATING_VIEW:
                    updateViewLayout();
                    break;
            }
        }
    }

    private class ButtonUIConfig implements IGmFloatingUIConfig {

        @Override
        public void start() {
            mOrientationListener.enable();
        }

        @Override
        public void stop() {
            mOrientationListener.disable();
        }
    }
}

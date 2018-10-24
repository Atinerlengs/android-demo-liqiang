package com.freeme.lockscreen;

import android.content.Context;
import android.content.res.Resources;
import android.view.MotionEvent;

import com.android.systemui.R;

public abstract class FreemeLockscreenGestureHelper {
    private float mStartX;
    private float mStartY;
    private int mHeight;
    private int mWidht;
    private int mBottomTouchAreaHeight;
    private int mBottomTouchAreaWidth;
    private int mLength;
    private int mWidthArea;
    private boolean mGestureTarget;
    private boolean mVerticalTouchUsed;
    private boolean mHorizontalTouchUsed;
    private boolean mIsLockScreenTouchStart;

    public FreemeLockscreenGestureHelper(Context context) {
        Resources res = context.getResources();
        mBottomTouchAreaHeight = res.getDimensionPixelSize(R.dimen.freeme_lockscreen_panel_bottom_height);
        mBottomTouchAreaWidth = res.getDimensionPixelSize(R.dimen.freeme_lockscreen_panel_margin_right);
        mLength = res.getDimensionPixelSize(R.dimen.freeme_lockscreen_gesture_distance);
        mWidthArea = res.getDimensionPixelSize(R.dimen.freeme_lockscreen_gesture_radius);
    }

    public void setDislpaySize(int height,int widht) {
        mWidht = widht;
        mHeight = height;
    }

    public void onTouch(MotionEvent event) {
        onHorizontalTouch(event);
        onVerticalTouch(event);
    }

    private void onHorizontalTouch(MotionEvent event) {
        final int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            mStartX = event.getX();
            mStartY = event.getY();
            mGestureTarget = false;
        } else if (action == MotionEvent.ACTION_MOVE) {
            if ((Math.abs(event.getY() - mStartY) < mWidthArea)
                    && Math.abs(event.getX() - mStartX) > mLength) {
                mGestureTarget = true;
                mHorizontalTouchUsed = true;
            }
            updateHorizontalAlpha(Math.abs(event.getX() - mStartX), Math.abs(event.getY() - mStartY));
        } else if (action == MotionEvent.ACTION_UP) {
            if (mGestureTarget) {
                mGestureTarget = false;
                isHorizontalTarget();
                mHorizontalTouchUsed = true;
            } else {
                updateHorizontalAlpha(0f, 0f);
            }
        }
    }

    private void onVerticalTouch(MotionEvent event) {
        final int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            if ((event.getY() > (mHeight - mBottomTouchAreaHeight))
                    && (event.getX() < (mWidht - mBottomTouchAreaWidth))) {
                mIsLockScreenTouchStart = true;
                mVerticalTouchUsed = true;
                return;
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (mIsLockScreenTouchStart && (event.getY() < (mHeight - mBottomTouchAreaHeight))) {
                mIsLockScreenTouchStart = false;
                mVerticalTouchUsed = true;
                return;
            }
        }
        mIsLockScreenTouchStart = false;
        mVerticalTouchUsed = false;
        return;
    }

    public boolean isVerticalTouchUsed() {
        boolean temp = mVerticalTouchUsed;
        mVerticalTouchUsed = false;
        return temp;
    }

    public boolean isHorizontalTouchUsed() {
        boolean temp = mHorizontalTouchUsed;
        mHorizontalTouchUsed = false;
        return temp;
    }

    private void updateHorizontalAlpha(float deltaX, float deltaY) {
        float alpha = (deltaX + deltaY) / mLength;
        alpha = alpha > 1.0f ? 1.0f : alpha;
        horizontalTouchAnimation(1.0f - alpha);
    }

    public abstract void isHorizontalTarget();

    public abstract void horizontalTouchAnimation(float alpha);
}

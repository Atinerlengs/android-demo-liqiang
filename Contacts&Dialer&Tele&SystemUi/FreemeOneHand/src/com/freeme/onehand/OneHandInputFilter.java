package com.freeme.onehand;

import android.content.Context;
import android.graphics.Rect;
import android.os.Debug;
import android.util.Log;
import android.view.InputEvent;
import android.view.InputFilter;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;
import android.view.ViewConfiguration;

class OneHandInputFilter extends InputFilter {
    private static final String TAG = "OneHandInputFilter";
    private static final boolean DBG = OneHandConstants.DEBUG;

    private OneHandController mController;
    private OneHandWindowInfo mWinInfo;
    private final int mTouchSlop;

    private float mDownX;
    private float mDownY;
    private boolean mInstalled;
    private boolean mIsMagnifyRectTouch;
    private boolean mIsMainWindowTouch;
    private Rect mMainRect = new Rect();
    private Rect mMagnifyRect = new Rect();
    private Rect mResizeHandleRect = new Rect();
    private int mOffsetX;
    private int mOffsetY;
    private boolean mResize;
    private boolean mResizeShown;
    private boolean mRunning;
    private float mScale;
    private int mScreenH;
    private int mScreenW;
    private PointerCoords[] mTempPointerCoords;
    private PointerProperties[] mTempPointerProperties;

    private boolean mTouchedAfterReduceScreen;
    
    public OneHandInputFilter(Context context, OneHandController controller) {
        super(context.getMainLooper());
        if (DBG) {
            Log.d(TAG, "OneHandInputFilter() constructor");
        }

        mController = controller;
        mWinInfo = OneHandWindowInfo.getInstance();

        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
    }

    @Override
    public void onInstalled() {
        if (DBG) {
            Log.d(TAG, "OneHand input filter installed.");
        }
        mInstalled = true;
        super.onInstalled();
    }

    @Override
    public void onUninstalled() {
        if (DBG) {
            Log.d(TAG, "OneHand input filter uninstalled.");
        }
        mInstalled = false;
        mRunning = false;
        mTouchedAfterReduceScreen = false;
        super.onUninstalled();
    }

    @Override
    public void onInputEvent(InputEvent event, int policyFlags) {
        if (mController.isAnimationRunning()) {
            return;
        }

        if (mInstalled && (event instanceof MotionEvent)) {
            MotionEvent mevent = (MotionEvent) event;
            int action = mevent.getAction();
            int x = (int) mevent.getRawX();
            int y = (int) mevent.getRawY();

            if (action == MotionEvent.ACTION_DOWN) {
                if (DBG) {
                    Log.d(TAG, (new StringBuilder(96))
                            .append("onInputEvent() Received. ")
                            .append("x=").append(x).append(", ")
                            .append("y=").append(y).append(", ")
                            .append("screenW=").append(mWinInfo.getScreenWidth()).append(", ")
                            .append("event=").append(event).append(", ")
                            .append("policviewyFlags=0x").append(Integer.toHexString(policyFlags))
                            .toString());
                }
                mResizeShown = false;
                mResize = isTouchOnResizeArea(x, y);
            } else if (mResize) {
                int masked = mevent.getActionMasked();
                if (masked == MotionEvent.ACTION_UP || masked == MotionEvent.ACTION_CANCEL) {
                    if (mResizeShown) {
                        mController.hideResizeGuide();
                    }
                    mResizeShown = false;
                    mResize = false;
                } else if (action == MotionEvent.ACTION_MOVE) {
                    if (mResizeShown) {
                        mController.adjustResizeGuide(x, y);
                    } else if (Math.hypot(mDownX - x, mDownY - y) > mTouchSlop) {
                        mResizeShown = true;
                        mController.showResizeGuide(x, y);
                    }
                }
                return;
            }

            boolean isInsideTouch = mMagnifyRect.contains(x, y);
            boolean isOutsideTouch = !isInsideTouch && mMainRect.contains(x, y);
            if (action == MotionEvent.ACTION_DOWN) {
                mIsMagnifyRectTouch = isInsideTouch;
                mIsMainWindowTouch = isOutsideTouch;
                mDownX = x;
                mDownY = y;
                mTouchedAfterReduceScreen = true;
            }
            if (mTouchedAfterReduceScreen) {
                if (action == MotionEvent.ACTION_MOVE) {
                    if (mIsMainWindowTouch && (!isOutsideTouch || isInsideTouch)) {
                        makeFakeTouchEvent(mevent, policyFlags, MotionEvent.ACTION_CANCEL);
                        mIsMainWindowTouch = false;
                    }
                    if (isInsideTouch != mIsMagnifyRectTouch) {
                        makeFakeTouchEvent(mevent, policyFlags,
                                isInsideTouch ? MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP);
                    }
                    mIsMagnifyRectTouch = isInsideTouch;
                }
                if (isInsideTouch) {
                    int pointerCount = mevent.getPointerCount();
                    PointerCoords[] pointerCoords
                            = getTempPointerCoordsWithMinSize(pointerCount);
                    PointerProperties[] pointerProperties
                            = getTempPointerPropertiesWithMinSize(pointerCount);
                    for (int i = 0; i < pointerCount; i++) {
                        mevent.getPointerCoords(i, pointerCoords[i]);
                        pointerCoords[i].x = (pointerCoords[i].x - mOffsetX) / mScale;
                        pointerCoords[i].y = (pointerCoords[i].y - mOffsetY) / mScale;

                        mevent.getPointerProperties(i, pointerProperties[i]);
                    }
                    super.onInputEvent(MotionEvent.obtain(
                            mevent.getDownTime(), mevent.getEventTime(), mevent.getAction(),
                            pointerCount, pointerProperties, pointerCoords,
                            0, 0, 1, 1,
                            mevent.getDeviceId(), 0, mevent.getSource(),
                            mevent.getFlags()),
                            policyFlags);
                } else if (isOutsideTouch) {
                    byPassRawEvent(mevent, policyFlags);
                }
            } else {
                byPassRawEvent(mevent, policyFlags);
            }
            return;
        }

        if (mInstalled && (event instanceof KeyEvent)) {
            KeyEvent kevent = (KeyEvent) event;
            if (DBG) {
                Log.d(TAG, (new StringBuilder(64))
                        .append("onInputEvent() Received key event. ")
                        .append("keyCode=").append(kevent.getKeyCode()).append(", ")
                        .append("scanCode=").append(kevent.getScanCode())
                        .toString());
            }
        }

        super.onInputEvent(event, policyFlags);
    }

    void registerInputFilter(boolean running) {
        if (DBG) {
            Log.d(TAG, (new StringBuilder(32))
                    .append("registerInputFilter() ")
                    .append("running=").append(running).append(", ")
                    .append("callers=").append(Debug.getCallers(5))
                    .toString());
        }
        mRunning = running;
    }

    boolean isRegistered() {
        if (DBG) {
            Log.d(TAG, (new StringBuilder())
                    .append("isRegistered() ")
                    .append("running=").append(mRunning).append(", ")
                    .append("callers=").append(Debug.getCallers(5))
                    .toString());
        }
        return mRunning;
    }

    void setWindowChanged() {
        Rect innerR = new Rect(mWinInfo.mMagnifyRect);
        if (DBG) {
            Log.d(TAG, "setWindowChanged() innerR=" + innerR);
        }
        mScale = mWinInfo.getWindowScale();
        mOffsetX = innerR.left;
        mOffsetY = innerR.top;
        mScreenW = mWinInfo.getScreenWidth();
        mScreenH = mWinInfo.getScreenHeight();
        mMagnifyRect.set(mWinInfo.mMagnifyRect);
        mMainRect.set(mWinInfo.mMainRect);
        if (DBG) {
            Log.d(TAG, "mMagnifyRect=" + mMagnifyRect + ", mMainRect=" + mMainRect);
        }
    }

    private PointerCoords[] getTempPointerCoordsWithMinSize(int size) {
        int oldSize = mTempPointerCoords != null ? mTempPointerCoords.length : 0;
        if (oldSize < size) {
            PointerCoords[] oldTempPointerCoords = mTempPointerCoords;
            mTempPointerCoords = new PointerCoords[size];
            if (oldTempPointerCoords != null) {
                System.arraycopy(oldTempPointerCoords, 0,
                        mTempPointerCoords, 0, oldSize);
            }
        }
        for (int i = oldSize; i < size; i++) {
            mTempPointerCoords[i] = new PointerCoords();
        }
        return mTempPointerCoords;
    }

    private PointerProperties[] getTempPointerPropertiesWithMinSize(int size) {
        int oldSize = mTempPointerProperties != null ? mTempPointerProperties.length : 0;
        if (oldSize < size) {
            PointerProperties[] oldTempPointerProperties = mTempPointerProperties;
            mTempPointerProperties = new PointerProperties[size];
            if (oldTempPointerProperties != null) {
                System.arraycopy(oldTempPointerProperties, 0,
                        mTempPointerProperties, 0, oldSize);
            }
        }
        for (int i = oldSize; i < size; i++) {
            mTempPointerProperties[i] = new PointerProperties();
        }
        return mTempPointerProperties;
    }

    private void makeFakeTouchEvent(MotionEvent event, int policyFlags, int fakeAction) {
        int action = fakeAction;

        final int pointerCount = event.getPointerCount();
        PointerCoords[] pointerCoords = getTempPointerCoordsWithMinSize(pointerCount);
        PointerProperties[] pointerProperties = getTempPointerPropertiesWithMinSize(pointerCount);
        for (int i = 0; i < pointerCount; i++) {
            event.getPointerCoords(i, pointerCoords[i]);
            pointerCoords[i].x = (pointerCoords[i].x - mOffsetX) / mScale;
            if (pointerCoords[i].x < 0) pointerCoords[i].x = 0;
            else if (pointerCoords[i].x > mScreenW) pointerCoords[i].x = mScreenW - 1;
            pointerCoords[i].y = (pointerCoords[i].y - mOffsetY) / mScale;
            if (pointerCoords[i].y < 0) pointerCoords[i].y = 0;
            else if (pointerCoords[i].y > mScreenH) pointerCoords[i].y = mScreenH - 1;

            event.getPointerProperties(i, pointerProperties[i]);

            if (i > 0 && fakeAction == MotionEvent.ACTION_DOWN) {
                action = (i * 0x100) + MotionEvent.ACTION_POINTER_DOWN;
            }
            if (pointerCount > 1 && (fakeAction == MotionEvent.ACTION_CANCEL
                                    || fakeAction == MotionEvent.ACTION_UP)) {
                if (i == 0) {
                    action = MotionEvent.ACTION_POINTER_UP;
                } else {
                    action = MotionEvent.ACTION_UP;
                }
            }

            super.onInputEvent(MotionEvent.obtain(
                    event.getDownTime(), event.getEventTime(),
                    action, pointerCount, pointerProperties, pointerCoords,
                    0, 0, 1, 1,
                    event.getDeviceId(), 0, event.getSource(), event.getFlags()),
                    policyFlags);
        }
    }

    private void byPassRawEvent(MotionEvent rawEvent, int policyFlags) {
        int pointerCount = rawEvent.getPointerCount();
        PointerCoords[] coords = getTempPointerCoordsWithMinSize(pointerCount);
        PointerProperties[] properties = getTempPointerPropertiesWithMinSize(pointerCount);
        for (int i = 0; i < pointerCount; i++) {
            rawEvent.getPointerCoords(i, coords[i]);
            rawEvent.getPointerProperties(i, properties[i]);
        }
        super.onInputEvent(MotionEvent.obtain(
                rawEvent.getDownTime(), rawEvent.getEventTime(), rawEvent.getAction(),
                pointerCount, properties, coords,
                0, 0, 1, 1,
                rawEvent.getDeviceId(), 0, rawEvent.getSource(),
                rawEvent.getFlags() | OneHandConstants.AMOTION_EVENT_FLAG_PREDISPATCH),
                policyFlags);
    }

    private boolean isTouchOnResizeArea(int x, int y) {
        Rect r = new Rect(mWinInfo.mMagnifyRect);
        int size = mWinInfo.getResizeHandleSide();
        mResizeHandleRect.top = r.top - size;
        mResizeHandleRect.bottom = r.top + size;
        mResizeHandleRect.left = (mWinInfo.isLeftHandMode() ? r.right : r.left) - size;
        mResizeHandleRect.right = (mWinInfo.isLeftHandMode() ? r.right : r.left) + size;

        if (!mResizeHandleRect.contains(x, y) || mWinInfo.mMagnifyRect.contains(x, y)) {
            return false;
        }
        Log.d(TAG, "Touched on Resize Handle");
        return true;
    }
}

package com.freeme.onehand;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.InternalInsetsInfo;
import android.view.Window;
import android.view.WindowManager.LayoutParams;

import com.freeme.view.FreemeWindowManager;

final class OneHandTouchWindow {
    private static final String TAG = "OneHandTouchWindow";
    private static final boolean DBG = OneHandConstants.DEBUG;

    private static int mDiagonalDist;
    private static float mInitScale;
    private static long mLastDownTime;
    private static int mScrollCount;
    private static int mTouchDownX;
    private static int mTouchDownY;
    private static int mTouchSlope;

    private Context mContext;
    private Handler mHandler;

    private final int mWidth;
    private final int mHeight;

    private OneHandController mController;
    private OneHandWindowInfo mWinInfo;

    private int mFirstTriggerDist;
    private GestureDetector mGestureDetectorSoftkey;
    private InjectorReflection mInjectorReflection;
    private final ViewTreeObserver.OnComputeInternalInsetsListener mInsetsComputer
            = new ViewTreeObserver.OnComputeInternalInsetsListener() {
        @Override
        public void onComputeInternalInsets(InternalInsetsInfo inoutInfo) {
            if (DBG) {
                Log.d(TAG, (new StringBuilder(64))
                        .append("onComputeInternalInsets() ")
                        .append("scale=").append(mWinInfo.mScale).append(", ")
                        .append("isLeft=").append(mWinInfo.isLeftHandMode())
                        .toString());
            }
            if (inoutInfo.touchableRegion.isEmpty()) {
                int screenW = mWinInfo.getScreenWidth();
                int touchableAreaBreadth = mWinInfo.getTouchableAreaBreadth();
                int b = mWinInfo.mScale == 1 ? touchableAreaBreadth : touchableAreaBreadth / 2;
                int w = mWinInfo.getTouchableAreaWidth();
                int h = mWinInfo.getTouchableAreaHeight();

                Rect vleft = new Rect(0, 0, b, h);
                Rect hleft = new Rect(0, h - b, w, h);
                Rect vright = new Rect(screenW - b, 0, screenW, h);
                Rect hright = new Rect(screenW - w, h - b, screenW, h);
                if (DBG) {
                    Log.d(TAG, (new StringBuilder(96))
                            .append("OnComputeInternalInsetsListener ")
                            .append("vLeft=").append(vleft).append(", ")
                            .append("hLeft=").append(hleft).append(", ")
                            .append("vRight=").append(vright).append(",")
                            .append("hRight=").append(hright)
                            .toString());
                }

                if (mWinInfo.mScale == 1) {
                    inoutInfo.touchableRegion.set(vleft);
                    inoutInfo.touchableRegion.union(hleft);
                    inoutInfo.touchableRegion.union(vright);
                    inoutInfo.touchableRegion.union(hright);
                } else if (mWinInfo.isLeftHandMode()) {
                    inoutInfo.touchableRegion.set(vleft);
                    inoutInfo.touchableRegion.union(hleft);
                } else {
                    inoutInfo.touchableRegion.set(vright);
                    inoutInfo.touchableRegion.union(hright);
                }

                inoutInfo.setTouchableInsets(InternalInsetsInfo.TOUCHABLE_INSETS_REGION);
            }
        }
    };

    private Dialog mMainDialog;
    private Window mMainDialogWindow;
    private LayoutParams mMainDialogWindowAttr;
    private View mMainView;

    private int mOffsetX;
    private int mOffsetY;
    private float mScale;

    private int mTriggerDist;
    private boolean mTriggered;
    private OneHandUtils mUtils;

    private static class InjectorReflection {
        private OneHandUtils mUtils = OneHandUtils.getInstance();
        private MotionEvent mDownEvent;
        private boolean mIsInjected;

        void forceInject(MotionEvent event) {
            synchronized (this) {
                mUtils.byPassRawEvent(event);
                mIsInjected = true;
            }
        }

        void inject(MotionEvent event) {
            synchronized (this) {
                if (DBG) {
                    Log.d(TAG, (new StringBuilder(48))
                            .append("inject() ")
                            .append("mIsInjected=").append(mIsInjected).append(", ")
                            .append("mInitScale=").append(mInitScale).append(", ")
                            .append("event=").append(event)
                            .toString());
                }
                if (mInitScale < 1) {
                    return;
                }
                if (!mIsInjected) {
                    forceInject(mDownEvent);
                }
                if (event != null) {
                    forceInject(event);
                }
            }
        }

        void setDownEvent(MotionEvent event) {
            if (DBG) {
                Log.d(TAG, "setDownEvent :" + event);
            }
            mIsInjected = false;
            mDownEvent = MotionEvent.obtain(event);
        }
    }

    private static class TouchFilter {
        private volatile boolean mIsValidEvent = true;
        private volatile int mMoveDistance = -1;

        void onMove(MotionEvent event) {
            float dx = Math.abs(mTouchDownX - event.getRawX());
            float dy = Math.abs(mTouchDownY - event.getRawY());
            mMoveDistance = (int) Math.hypot(dx, dy);

            long timeDiff = event.getEventTime() - event.getDownTime();
            if (mMoveDistance > mDiagonalDist / 8 && (dy > 3 * dx || dx > 2 * dy)) {
                setValidEvent(false);
            } else if (timeDiff > 1000 || (timeDiff > 100 && mMoveDistance < mTouchSlope)) {
                setValidEvent(false);
            }
        }

        void setValidEvent(boolean valid) {
            synchronized (this) {
                mIsValidEvent = mInitScale != 1 || valid;
                mMoveDistance = -1;
            }
        }

        boolean isValidEvent() {
            return mIsValidEvent;
        }

        int getMoveDistance() {
            return mMoveDistance;
        }
    }
    private TouchFilter mTouchFilter;

    private final Runnable mScaleRunnable = new Runnable() {
        @Override
        public void run() {
            if (mOffsetY > 0 && mWinInfo.mOffsetY == 0) {
                mUtils.playShortHaptic();
            }
            mController.changeScale(mScale, mOffsetX, mOffsetY);
        }
    };
    private final Runnable mCheckMovement = new Runnable() {
        @Override
        public void run() {
            if (mTouchFilter.getMoveDistance() < mTouchSlope) {
                mTouchFilter.setValidEvent(false);
                mInjectorReflection.inject(null);
            }
        }
    };
    private final View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mWinInfo.mScale < 1 && mWinInfo.isReturnToFullScreen()) {
                return false;
            }

            final int x = (int) event.getRawX();
            final int y = (int) event.getRawY();
            final int action = event.getAction();
            final int actionMasked = action & MotionEvent.ACTION_MASK;

            if (!mWinInfo.isGestureEnabledByProximitySensor()) {
                if (action == MotionEvent.ACTION_DOWN) {
                    Log.d(TAG, "Gesture disabled by Proximity Sensor");
                }
                if (action == MotionEvent.ACTION_MOVE) {
                    return false;
                }
            }

            if (actionMasked == MotionEvent.ACTION_POINTER_DOWN && !mTriggered) {
                mTouchFilter.setValidEvent(false);
                mInjectorReflection.inject(event);
                return false;
            }

            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_CANCEL
                    || mTouchFilter.isValidEvent()) {
                mGestureDetectorSoftkey.onTouchEvent(event);
                switch (action) {
                    case MotionEvent.ACTION_DOWN: {
                        long downTime = event.getEventTime();
                        if (downTime - mLastDownTime < 200 &&
                                x == mTouchDownX && y == mTouchDownY) {
                            mLastDownTime = downTime;
                            break;
                        }

                        mTouchDownX = x;
                        mTouchDownY = y;

                        mTriggered = false;
                        mScrollCount = 0;

                        mInitScale = mWinInfo.mScale;

                        mTouchFilter.setValidEvent(true);
                        mInjectorReflection.setDownEvent(event);

                        if (downTime - mLastDownTime < 50) {
                            mHandler.removeCallbacks(mCheckMovement);
                            mHandler.postDelayed(mCheckMovement, 100);
                        }
                        mLastDownTime = downTime;
                    } break;
                    case MotionEvent.ACTION_MOVE: {
                        if (!mTriggered) {
                            mTouchFilter.onMove(event);
                        }
                    } break;
                    case MotionEvent.ACTION_UP: {
                        if (DBG) {
                            Log.d(TAG, "MotionEvent.ACTION_UP");
                        }
                        if (!mTriggered) {
                            mInjectorReflection.inject(event);
                            break;
                        }
                    } // fall-through
                    case MotionEvent.ACTION_CANCEL: {
                        mHandler.removeCallbacks(mCheckMovement);
                        mHandler.removeCallbacks(mScaleRunnable);
                        if (mTriggered) {
                            mTriggered = false;

                            if (mController.isAnimationRunning() ||
                                    (mController.isInputFilterRegistered() && mWinInfo.mOffsetY == 0)) {
                                break;
                            }

                            if (mWinInfo.mScale == 1) {
                                mController.hideFgWindow();
                            } else if (mWinInfo.mScale > mWinInfo.getMaxScale()) {
                                mController.returnFullScreen(true, null);
                            } else {
                                mScale = -1;
                                mOffsetX = mOffsetY = 0;
                                mHandler.removeCallbacks(mScaleRunnable);
                                mHandler.post(mScaleRunnable);
                            }
                        }
                    } break;
                }
                return false;
            }

            mInjectorReflection.inject(event);
            return false;
        }
    };

    OneHandTouchWindow(Context context, OneHandController controller) {
        mContext = context;
        mController = controller;

        mUtils = OneHandUtils.getInstance();

        mWinInfo = OneHandWindowInfo.getInstance();
        mDiagonalDist = mWinInfo.getDiagonalDist();
        mTriggerDist = mWinInfo.getTriggerDistance();

        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlope = configuration.getScaledTouchSlop();
        mHandler = new Handler();

        mMainDialog = new Dialog(mContext);
        mMainDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.d(TAG, "Touch Dialog dismissed, stop service");
                mController.stopService();
            }
        });
        mMainDialogWindow = mMainDialog.getWindow();
        mMainDialogWindow.requestFeature(Window.FEATURE_NO_TITLE);
        mMainDialogWindow.setBackgroundDrawableResource(android.R.color.transparent);
        mMainView = View.inflate(mContext, R.layout.key_window, null);
        mMainView.getViewTreeObserver().addOnComputeInternalInsetsListener(mInsetsComputer);
        mMainDialog.setContentView(mMainView);
        View dialogDecorView = mMainDialogWindow.getDecorView();
        dialogDecorView.setOnTouchListener(mViewTouchListener);
        dialogDecorView.setElevation(0);
        dialogDecorView.setTranslationZ(0);

        mHeight = mWinInfo.getScreenHeight();
        mWidth = mWinInfo.getScreenWidth();

        createWindowLayout();
        showWindow();

        mTouchFilter = new TouchFilter();
        mInjectorReflection = new InjectorReflection();
        enableGestureDetector();
    }

    private void createWindowLayout() {
        LayoutParams lp = new LayoutParams(
                FreemeWindowManager.FreemeLayoutParams.TYPE_ONEHAND_HANDLER,
                LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | LayoutParams.FLAG_NOT_FOCUSABLE
                        | LayoutParams.FLAG_FULLSCREEN
                        | LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.alpha = DBG ? 1 : 0;
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.softInputMode = LayoutParams.SOFT_INPUT_STATE_HIDDEN;
        lp.privateFlags |= LayoutParams.PRIVATE_FLAG_NO_MOVE_ANIMATION;
        lp.privateFlags |= LayoutParams.PRIVATE_FLAG_SHOW_FOR_ALL_USERS;
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= LayoutParams.FLAG_HARDWARE_ACCELERATED;
            lp.privateFlags |= LayoutParams.PRIVATE_FLAG_FORCE_HARDWARE_ACCELERATED;
        }
        lp.setTitle("OneHandTouch");
        lp.height = mWinInfo.getTouchableAreaHeight();
        lp.y = mWinInfo.getScreenHeight() - lp.height;
        mMainDialogWindowAttr = lp;

        mMainDialogWindow.setAttributes(mMainDialogWindowAttr);
    }

    void enableGestureDetector() {
        mGestureDetectorSoftkey = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                mScrollCount++;

                int dist = (int) Math.hypot(e1.getRawX() - e2.getRawX(), e1.getRawY() - e2.getRawY());
                int dist_y = (int) Math.abs(e1.getRawY() - e2.getRawY());
                int dist_x = (int) Math.abs(e1.getRawX() - e2.getRawX());

                if ((!mTriggered) && (mScrollCount >= 3)
                        && (dist_x > dist_y * 2 || dist_y > dist_x * 3)) {
                    if (DBG) {
                        Log.d(TAG, (new StringBuilder(96))
                                .append("onScroll() invalid direction. ")
                                .append("dist_x=").append(dist_x).append(", ")
                                .append("dist_y=").append(dist_y).append(", ")
                                .append("mScrollCount=").append(mScrollCount)
                                .toString());
                    }
                    mTouchFilter.setValidEvent(false);
                    return false;
                }

                if (mTriggered || (mScrollCount >= 2 && dist > (mTriggerDist * 1.5d) / mInitScale)) {
                    if (!mTriggered && (dist_x < mTriggerDist / 2 || dist_y < mTriggerDist / 2)) {
                        return false;
                    }
                    if (mController.isAnimationRunning()) {
                        mFirstTriggerDist = dist;
                        return false;
                    }
                    if (mTriggered) {
                        float delta = (mFirstTriggerDist - dist) / (float) mDiagonalDist;
                        float scale = ((mInitScale == 1) ? mWinInfo.getMinScale() : mInitScale) + delta;
                        if (DBG) {
                            Log.d(TAG, (new StringBuilder())
                                    .append("scale=").append(scale).append(", ")
                                    .append("delta=").append(delta).append(", ")
                                    .append("dist=").append(dist).append(", ")
                                    .append("mFirstTriggerDist=").append(mFirstTriggerDist)
                                    .toString());
                        }
                        if (scale < mWinInfo.getMinScale()) {
                            scale = mWinInfo.getMinScale();
                        }
                        if (scale >= mWinInfo.getFullScale()) {
                            scale = mWinInfo.getFullScale();
                        }
                        mScale = scale;

                        int side = mWinInfo.getSideMargin();
                        int bottom = mWinInfo.getBottomMargin();

                        mOffsetX = mWinInfo.isLeftHandMode() ?
                                side : ((int) (mWidth - mWidth * mScale + 0.5f)) - side;
                        mOffsetY = ((int) (mHeight - mHeight * mScale + 0.5f)) - bottom;

                        mHandler.removeCallbacks(mScaleRunnable);
                        mHandler.post(mScaleRunnable);
                    } else {
                        mTriggered = true;
                        mFirstTriggerDist = dist;
                        mWinInfo.setLeftHandMode(mTouchDownX < mWidth / 2);
                        mHandler.removeCallbacks(mCheckMovement);
                        if (mInitScale == 1) {
                            mController.startReduceScreenAnimation();
                        } else {
                            mController.showFgWindow();
                            mUtils.playShortHaptic();
                        }
                    }
                }
                return false;
            }
        });
    }

    void cancelTouchEvent() {
        mTouchFilter.setValidEvent(false);
        mTriggered = false;
    }

    boolean gestureTriggered() {
        return mTriggered;
    }

    void updateTouchableArea() {
        if (mMainView != null) {
            mMainView.requestLayout();
        }
    }

    void showWindow() {
        if (mWinInfo.isPortraitMode() && !mWinInfo.isTripleHomeStyle()) {
            if (DBG) {
                Log.d(TAG, "showWindow()");
            }
            mMainDialog.show();
        }
    }

    void hideWindow() {
        if (DBG) {
            Log.d(TAG, "hideWindow()");
        }
        mMainDialog.hide();
    }
}

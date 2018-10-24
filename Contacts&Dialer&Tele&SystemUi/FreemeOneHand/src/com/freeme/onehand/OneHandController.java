package com.freeme.onehand;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Debug;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

public class OneHandController {
    private static final String TAG = "OneHandController";
    private static final boolean DBG = OneHandConstants.DEBUG;

    private Context mContext;
    private Handler mHandler;

    private PowerManager mPowerManager;
    private IWindowManager mWindowManagerService;
    private KeyguardManager mKeyguardManager;

    private boolean mAnimationRunning;
    private OneHandBGWindow mBGWindow;

    private final OneHandBackgroundView.ButtonClickActionCallback mButtonClickCallback
            = new OneHandBackgroundView.ButtonClickActionCallback() {
        @Override
        public void onRecentButtonClicked() {
            Log.d(TAG, "onRecentButtonClicked()");

            mUtils.sendKeyEvent(KeyEvent.KEYCODE_APP_SWITCH, KeyEvent.ACTION_DOWN, 0);
            mUtils.sendKeyEvent(KeyEvent.KEYCODE_APP_SWITCH, KeyEvent.ACTION_UP, 0);
            mUtils.playShortHaptic();
        }

        @Override
        public void onHomeButtonClicked() {
            Log.d(TAG, "onHomeButtonClicked()");
            mUtils.sendKeyEvent(KeyEvent.KEYCODE_HOME, KeyEvent.ACTION_DOWN, 0);
            mUtils.sendKeyEvent(KeyEvent.KEYCODE_HOME, KeyEvent.ACTION_UP, 0);
            mUtils.playShortHaptic();
        }

        @Override
        public void onBackButtonClicked() {
            Log.d(TAG, "onBackButtonClicked()");
            mUtils.sendKeyEvent(KeyEvent.KEYCODE_BACK, KeyEvent.ACTION_DOWN, 0);
            mUtils.sendKeyEvent(KeyEvent.KEYCODE_BACK, KeyEvent.ACTION_UP, 0);
            mUtils.playShortHaptic();
        }

        @Override
        public void onExitButtonClicked() {
            Log.d(TAG, "onExitButtonClicked()");
            mUtils.playShortHaptic();
            returnFullScreen(true, null);
        }

        @Override
        public void onSettingsButtonClicked() {
            mUtils.playShortHaptic();
            if (!mUtils.isCallRinging()) {
                if (isKeyguardLocked()) {
                    Log.d(TAG, "onSettingsButtonClicked() first unlock keyguard");
                    unlockKeyguard();
                }
                mContext.startActivityAsUser(new Intent(OneHandConstants.ACTION_ONEHAND_SETTINGS)
                        .setFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_ACTIVITY_CLEAR_TASK),
                        UserHandle.CURRENT);
            }
        }

        @Override
        public void onSwitchButtonClicked() {
            Log.d(TAG, "onSwitchButtonClicked()");
            mUtils.playShortHaptic();
            switchPositionAnimate();
        }
    };

    private final OneHandBackgroundView.ButtonLongClickActionCallback mButtonLongClickCallback
            = new OneHandBackgroundView.ButtonLongClickActionCallback() {
        @Override
        public void onRecentButtonClicked() {
            Log.d(TAG, "onRecentButtonClicked()");
            mUtils.playShortHaptic();
            mUtils.sendKeyEvent(KeyEvent.KEYCODE_APP_SWITCH, KeyEvent.ACTION_DOWN, 0);
            mUtils.sendKeyEvent(KeyEvent.KEYCODE_APP_SWITCH, KeyEvent.ACTION_DOWN, KeyEvent.FLAG_LONG_PRESS);
            mUtils.sendKeyEvent(KeyEvent.KEYCODE_APP_SWITCH, KeyEvent.ACTION_UP, 0);
        }

        @Override
        public void onHomeButtonClicked() {
            Log.d(TAG, "onHomeButtonClicked()");
            mUtils.playShortHaptic();
            mUtils.sendKeyEvent(KeyEvent.KEYCODE_HOME, KeyEvent.ACTION_DOWN, 0);
            mUtils.sendKeyEvent(KeyEvent.KEYCODE_HOME, KeyEvent.ACTION_DOWN, KeyEvent.FLAG_LONG_PRESS);
            mUtils.sendKeyEvent(KeyEvent.KEYCODE_HOME, KeyEvent.ACTION_UP, 0);
        }

        @Override
        public void onBackButtonClicked() {
            Log.d(TAG, "onHomeButtonClicked()");
            mUtils.playShortHaptic();
            mUtils.sendKeyEvent(KeyEvent.KEYCODE_MENU, KeyEvent.ACTION_DOWN, 0);
            mUtils.sendKeyEvent(KeyEvent.KEYCODE_MENU, KeyEvent.ACTION_UP, 0);
        }
    };

    private final Runnable mHideBgWindowRunnable = new Runnable() {
        @Override
        public void run() {
            if (DBG) {
                Log.d(TAG, "mHideBgWindowRunnable run");
            }
            mBGWindow.hideScreenshot();
            mBGWindow.hideFgWindow();
            mBGWindow.setForegroundTransparentRect(null);
            mUtils.trimMemory();
        }
    };
    private final Runnable mHideScreenshotRunnable = new Runnable() {
        @Override
        public void run() {
            if (DBG) {
                Log.d(TAG, "mHideScreenshotRunnable run");
            }
            mBGWindow.hideScreenshot();
            mUtils.trimMemory();
            if (mWinInfo.mScale < 1) {
                ContentResolver resolver = mContext.getContentResolver();
                Settings.System.putIntForUser(resolver,
                        OneHandConstants.ONEHAND_RUNNING, 1,
                        UserHandle.USER_CURRENT);
                Settings.System.putIntForUser(resolver,
                        OneHandConstants.ONEHAND_DIRECTION, mWinInfo.isLeftHandMode() ? 0 : 1,
                        UserHandle.USER_CURRENT);

                String reduceInfo = ""
                        + mWinInfo.mOffsetX + ";"
                        + mWinInfo.mOffsetY + ";"
                        + mWinInfo.mScale;
                if (DBG) {
                    Log.d(TAG, "reduce_screen_running_info=" + reduceInfo);
                }
                Settings.System.putStringForUser(resolver,
                        OneHandConstants.ONEHAND_RUNNING_INFO, reduceInfo,
                        UserHandle.USER_CURRENT);
            }
        }
    };

    private OneHandServiceWatcher mOneHandServiceWatcher = new OneHandServiceWatcher();
    private OneHandInputFilter mInputFilter;
    private OneHandResizeWindow mResizeWindow;
    private ValueAnimator mReturnFullScreenAnimation;
    private ValueAnimator mReduceScreenAnimation;
    private ValueAnimator mSwitchPositionAnimation;
    private ValueAnimator mChangeScaleAnimation;
    private OneHandTouchWindow mTouchWindow;
    private OneHandWindowInfo mWinInfo;
    private OneHandUtils mUtils;

    private int mOffsetX;
    private int mOffsetY;
    private float mScale = 1.0f;

    OneHandController(Context context) {
        mContext = context;
        mHandler = new Handler();
        mWindowManagerService = WindowManagerGlobal.getWindowManagerService();
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        mUtils = OneHandUtils.getInstance();
        mWinInfo = OneHandWindowInfo.getInstance();
        mInputFilter = new OneHandInputFilter(mContext, this);
        mBGWindow = new OneHandBGWindow(mContext);
        mBGWindow.setButtonClickCallback(mButtonClickCallback, mButtonLongClickCallback);
        mTouchWindow = new OneHandTouchWindow(mContext, this);
        mResizeWindow = new OneHandResizeWindow(mContext, this);

        try {
            mWindowManagerService.registerOneHandWatcher(mOneHandServiceWatcher);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void onOrientationChanged(Configuration newConfig) {
        if (DBG) {
            Log.d(TAG, "onOrientationChanged() orientation=" + newConfig.orientation);
        }
        switch (newConfig.orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                mWinInfo.updateDisplayMatrix();
                mTouchWindow.showWindow();
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                if (mWinInfo.getWindowScale() < 1) {
                    Toast.makeText(mContext, R.string.rotate_help_message, Toast.LENGTH_LONG)
                            .show();
                }
                mTouchWindow.hideWindow();
                returnFullScreen(false, null);
                break;
        }
    }

    void onFontLocaleChanged() {
        mBGWindow.onFontLocaleChanged();
    }

    boolean isInputFilterRegistered() {
        return mInputFilter.isRegistered();
    }

    void changeScale(float scale, int offsetX, int offsetY) {
        if (scale == 1 || scale < 0) {
            Log.d(TAG, "changeScale() scale=" + scale + ", mScale=" + mScale);
        }
        if (scale > 0 && offsetY == mOffsetY && offsetX == mOffsetX) {
            if (DBG) {
                Log.d(TAG, "same scale and offset. return.");
            }
            return;
        }

        mPowerManager.userActivity(SystemClock.uptimeMillis(),
                PowerManager.USER_ACTIVITY_EVENT_OTHER, 0);

        if (DBG) {
            Log.d(TAG, (new StringBuilder(96))
                    .append("changeScale() ")
                    .append("isLeft=").append(mWinInfo.isLeftHandMode()).append(", ")
                    .append("scale=").append(scale).append(", ")
                    .append("offsetX=").append(offsetX).append(", ")
                    .append("offsetY=").append(offsetY).append(", ")
                    .append("mScale=").append(mScale).append(", ")
                    .append("isBGVisible=").append(isBGVisible()).append(", ")
                    .append("callers=").append(Debug.getCallers(5))
                    .toString());
        }
        if (scale < 0) {
            mWinInfo.setWindowChanged(mScale, mOffsetX, mOffsetY);
            mBGWindow.setForegroundTransparentRect(mWinInfo.mMagnifyRect);

            changeDisplayScale(mScale, mOffsetX, mOffsetY, true, mInputFilter);

            mInputFilter.registerInputFilter(true);
            mInputFilter.setWindowChanged();
            mTouchWindow.updateTouchableArea();

            mHandler.removeCallbacks(mHideScreenshotRunnable);
            mHandler.post(mHideScreenshotRunnable);
        } else if (scale == 1) {
            mScale = 1;
            mOffsetX = 0;
            mOffsetY = 0;

            mWinInfo.setWindowChanged(mScale, mOffsetX, mOffsetY);
            mInputFilter.registerInputFilter(false);
            mInputFilter.setWindowChanged();
            mTouchWindow.updateTouchableArea();

            changeDisplayScale(mScale, mOffsetX, mOffsetY, true, null);

            mHandler.removeCallbacks(mHideBgWindowRunnable);
            mHandler.postDelayed(mHideBgWindowRunnable, 10);
            Settings.System.putIntForUser(mContext.getContentResolver(),
                    OneHandConstants.ONEHAND_RUNNING, 0,
                    UserHandle.USER_CURRENT);
        } else {
            mScale = scale;
            mOffsetX = offsetX;
            mOffsetY = offsetY;

            mWinInfo.setWindowChanged(mScale, mOffsetX, mOffsetY);
            mBGWindow.scaleScreenshot(mScale, mOffsetX, mOffsetY);
        }
    }

    void changeDisplayScale(float scale, int offsetX, int offsetY,
            boolean register, OneHandInputFilter filter) {
        try {
            mWindowManagerService.changeDisplayScale(scale, offsetX, offsetY, register, filter);
        } catch (RemoteException e) {
            Log.e(TAG, "changeDisplayScale", e);
        }
    }

    void returnFullScreen(boolean showAnimation, final Runnable runAfter) {
        if (DBG) {
            Log.d(TAG, (new StringBuilder(64))
                    .append("returnFullScreen() ")
                    .append("showAnimation=").append(showAnimation).append(", ")
                    .append("mScale=").append(mScale).append(", ")
                    .append("mAnimationRunning=").append(mAnimationRunning)
                    .toString());
        }
        mTouchWindow.cancelTouchEvent();
        mBGWindow.hideButtonViews();
        mResizeWindow.finishWindow();
        mWinInfo.setReturnToFullScreen(true);

        if (showAnimation) {
            if (mScale < 1) {
                if (!mBGWindow.captureScreenshot()) {
                    mBGWindow.setForegroundTransparentRect(null);
                }
                mReturnFullScreenAnimation = ObjectAnimator.ofFloat(mScale, 1.0f);
                mReturnFullScreenAnimation.addUpdateListener(new AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float curValue = (Float) animation.getAnimatedValue();
                        mBGWindow.scaleScreenshot(curValue,
                                (int) (mWinInfo.mOffsetX * ((1 - curValue) / (1 - mWinInfo.mScale)) + 0.5f),
                                (int) (mWinInfo.mOffsetY * ((1 - curValue) / (1 - mWinInfo.mScale)) + 0.5f));
                    }
                });
            }
            if (mScale < 1 || mOffsetY != 0) {
                mReturnFullScreenAnimation.setDuration((int) (200.0f / mScale));
                mReturnFullScreenAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
                mReturnFullScreenAnimation.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        changeScale(1.0f, 0, 0);
                        mAnimationRunning = false;
                        if (runAfter != null) {
                            mHandler.postDelayed(runAfter, 100);
                        }
                    }
                });
                mAnimationRunning = true;
                mReturnFullScreenAnimation.start();
            }
        } else {
            if (mReturnFullScreenAnimation != null && mReturnFullScreenAnimation.isRunning()) {
                mReturnFullScreenAnimation.cancel();
            } else {
                mBGWindow.scaleScreenshot(1.0f, 0, 0);
                changeScale(1.0f, 0, 0);
            }
        }
    }

    void changeScaleAnimate(final float toScale, int offsetX, int offsetY) {
        Log.d(TAG, "changeScaleAnimate() toScale=" + toScale + ", fromScale=" + mScale);
        mBGWindow.showFgWindow();
        mBGWindow.captureScreenshot();

        final Rect origRect = new Rect(mWinInfo.mMagnifyRect);
        mChangeScaleAnimation = ObjectAnimator.ofFloat(mScale, toScale);
        mChangeScaleAnimation.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float curValue = (Float) animation.getAnimatedValue();
                int xPos;
                if (mWinInfo.isLeftHandMode()) {
                    xPos = origRect.left;
                } else {
                    xPos = (int) (origRect.right - (mWinInfo.getScreenWidth() * curValue) + 0.5f);
                }
                changeScale(curValue, xPos, (int) (origRect.bottom - (mWinInfo.getScreenHeight() * curValue) + 0.5f));
            }
        });
        mChangeScaleAnimation.setDuration(200);
        mChangeScaleAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        mChangeScaleAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                changeScale(-1.0f, 0, 0);
                mBGWindow.updateOutsideViews(mWinInfo.mMagnifyRect);
                mWinInfo.changeDefaultScale(toScale);
                mAnimationRunning = false;
            }
        });
        mAnimationRunning = true;
        mChangeScaleAnimation.start();
    }

    boolean isAnimationRunning() {
        return mAnimationRunning;
    }

    boolean startReduceScreenAnimation() {
        Log.d(TAG, "startReduceScreenAnimation()");
        showFgWindow();

        mUtils.playShortHaptic();
        if (mWinInfo.isTripleHomeStyle()) {
            boolean left = Settings.System.getIntForUser(mContext.getContentResolver(),
                    OneHandConstants.ONEHAND_DIRECTION, 0,
                    UserHandle.USER_CURRENT) == 0;
            mWinInfo.setLeftHandMode(left);
        }

        float scale = mWinInfo.getDefaultScale();
        int w = mWinInfo.getScreenWidth();
        int h = mWinInfo.getScreenHeight();
        int side = mWinInfo.getSideMargin();
        int bottom = mWinInfo.getBottomMargin();
        if (mWinInfo.isLeftHandMode()) {
            animateReduceScreen(scale,
                    side, (int) (h - h * scale + 0.5f) - bottom);
        } else {
            animateReduceScreen(scale,
                    (int) (w - w * scale + 0.5f) - side,
                    (int) (h - h * scale + 0.5f) - bottom);
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mWinInfo.increaseReduceScreenLaunchCount();
            }
        });
        return true;
    }

    void updateBackgroundImage() {
        if (mBGWindow != null) {
            mBGWindow.updateBackgroundImage();
        }
    }

    private void animateReduceScreen(final float scale, final int offsetX, final int offsetY) {
        if (DBG) {
            Log.d(TAG, (new StringBuilder(64))
                    .append("animateReduceScreen() ")
                    .append("scale=").append(scale).append(", ")
                    .append("offsetX=").append(offsetX).append(", ")
                    .append("offsetY=").append(offsetY)
                    .toString());
        }
        mReduceScreenAnimation = ObjectAnimator.ofFloat(mWinInfo.mScale, scale)
                .setDuration(300);
        mReduceScreenAnimation.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float curValue = (Float) animation.getAnimatedValue();
                if (scale < 1.0f && curValue < 1.0f && isBGVisible()) {
                    changeScale(curValue,
                            (int) (offsetX * ((1 - curValue) / (1 - scale)) + 0.5f),
                            (int) (offsetY * ((1 - curValue) / (1 - scale)) + 0.5f));
                }
            }
        });
        mReduceScreenAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                boolean firstLaunch = mWinInfo.getReduceScreenLaunchButtonCount() == 1;
                if (!mTouchWindow.gestureTriggered() || firstLaunch) {
                    changeScale(scale, offsetX, offsetY);
                    changeScale(-1.0f, 0, 0);
                }
                if (firstLaunch && mWinInfo.isTripleHomeStyle()) {
                    mResizeWindow.showInitWindow();
                }
                mAnimationRunning = false;
            }
        });
        mReduceScreenAnimation.start();
        mAnimationRunning = true;
    }

    void switchPositionAnimate() {
        final boolean isLeftHand = mWinInfo.isLeftHandMode();
        final Rect r = new Rect(mWinInfo.mMagnifyRect);
        if (DBG) {
            Log.d(TAG, "switchPositionAnimate() isLeftHand =" + isLeftHand);
        }
        final int width = mWinInfo.getScreenWidth();
        final int side = mWinInfo.getSideMargin();
        mWinInfo.setSwitchPositionRunning(true);
        mBGWindow.setForegroundTransparentRect(null);
        mBGWindow.captureScreenshot();

        if (isLeftHand) {
            mSwitchPositionAnimation = ObjectAnimator.ofInt(r.left, width - r.width() - side);
        } else {
            mSwitchPositionAnimation = ObjectAnimator.ofInt(r.left, side);
        }
        mSwitchPositionAnimation.setDuration(300);
        mSwitchPositionAnimation.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mBGWindow.scaleScreenshot(mScale,
                        (Integer) animation.getAnimatedValue(), mWinInfo.mOffsetY);
            }
        });
        mSwitchPositionAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mScale = mWinInfo.getWindowScale();
                mOffsetX = isLeftHand ? (width - r.width()) - side : side;
                mOffsetY = mWinInfo.mOffsetY;
                mWinInfo.setLeftHandMode(!isLeftHand);

                changeScale(-1.0f, mOffsetX, mWinInfo.mOffsetY);

                mBGWindow.scaleScreenshot(mScale, mOffsetX, mOffsetY);
                mAnimationRunning = false;
                mWinInfo.setSwitchPositionRunning(false);
            }
        });
        mAnimationRunning = true;
        mSwitchPositionAnimation.start();
    }

    boolean isBGVisible() {
        return mBGWindow.isWindowDrawn();
    }

    void screenTurnedOn() {
        if (mWinInfo.mScale < 1.0f) {
            Log.d(TAG, "setDefault() mWinInfo.mScale =" + mWinInfo.mScale);
            changeScale(1.0f, 0, 0);
        }
        mWinInfo.screenTurnedOn();
    }

    void showFgWindow() {
        long current = 0;
        if (DBG) {
            Log.d(TAG, "showFgWindow() callers=" + Debug.getCallers(5));
            current = SystemClock.elapsedRealtime();
        }
        mWinInfo.setReturnToFullScreen(false);
        mBGWindow.showFgWindow();
        if (DBG) {
            Log.d(TAG, "showFgWindow() elapsed =" + (SystemClock.elapsedRealtime() - current));
        }
    }

    void hideFgWindow() {
        mBGWindow.hideFgWindow();
    }

    private void cleanUpWindow() {
        if (mResizeWindow != null) {
            mResizeWindow.finishWindow();
        }
        if (mTouchWindow != null) {
            mTouchWindow.hideWindow();
        }
        if (mBGWindow != null) {
            mBGWindow.hideFgWindow();
        }
        changeScale(1.0f, 0, 0);
        Settings.System.putIntForUser(mContext.getContentResolver(),
                OneHandConstants.ONEHAND_RUNNING, 0,
                UserHandle.USER_CURRENT);
    }

    public void stopService() {
        ((Service) mContext).stopSelf();
    }

    public void forceStopService() {
        Log.d(TAG, "forceStopService()");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mContext.startService(new Intent(OneHandConstants.ACTION_ONEHAND_SERVICE)
                            .setClass(mContext, OneHandService.class)
                            .putExtra(OneHandService.EXTRA_FORCE_HIDE, true));
                } catch (Exception e) {
                    Log.e(TAG, "forceStopService", e);
                }
            }
        });
    }

    void onDestroy() {
        cleanUpWindow();
        try {
            mWindowManagerService.unregisterOneHandWatcher(mOneHandServiceWatcher);
        } catch (RemoteException e) {
            Log.w(TAG, "unregisterOneHandWatcher", e);
        }
    }

    void onSoftkeyModeChanged(final int mode) {
        if (DBG) {
            Log.d(TAG, "onSoftkeyModeChanged() mode=" + mode);
        }
        if (mWinInfo.mScale == 1) {
            mWinInfo.setSoftkeyMode(mode);
        } else {
            returnFullScreen(true, new Runnable() {
                @Override
                public void run() {
                    mWinInfo.setSoftkeyMode(mode);
                    startReduceScreenAnimation();
                }
            });
        }
    }

    void onTriggerTypeChanged(int type) {
        if (DBG) {
            Log.d(TAG, "onTriggerTypeChanged() type=" + type);
        }
        switch (type) {
            case 0:
                mWinInfo.setTripleHomeStyle(false);
                mTouchWindow.showWindow();
                break;
            case 1:
                mWinInfo.setTripleHomeStyle(true);
                mTouchWindow.hideWindow();
                break;
        }
    }

    void showResizeGuide(int x, int y) {
        mUtils.playShortHaptic();
        mBGWindow.hideButtonViews();
        mResizeWindow.showWindow(false);
    }

    void hideResizeGuide() {
        mResizeWindow.hideWindow();
    }

    void adjustResizeGuide(int x, int y) {
        mResizeWindow.resizeWindow(x, y);
    }

    private boolean isKeyguardLocked() {
        return (mKeyguardManager != null) && mKeyguardManager.isKeyguardLocked();
    }

    private void unlockKeyguard() {
        try {
            mWindowManagerService.dismissKeyguard(null);
        } catch (RemoteException e) {
            Log.e(TAG, "Dismiss keyguard error", e);
        }
    }
}

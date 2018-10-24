package com.freeme.onehand;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Debug;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.InternalInsetsInfo;
import android.view.WindowManager.LayoutParams;

import com.freeme.view.FreemeWindowManager;

public class OneHandBGWindow {
    private static final String TAG = "OneHandBGWindow";
    private static final boolean DBG = OneHandConstants.DEBUG;

    private Context mContext;
    private Handler mHandler;

    private BitmapDrawable mBgBitmapImage;
    private int mBgUpdateRetryCount;
    private final Runnable mBgUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "mBgUpdateRunnable.run()");
            if (mBgView != null && mUtils.isValidBGImage()) {
                mBgBitmapImage = mUtils.getWallpaperImage();
                mBgView.setBackground(mBgBitmapImage);
                mBgView.invalidate();
            } else if (++mBgUpdateRetryCount < 20) {
                mHandler.postDelayed(mBgUpdateRunnable, 300);
            }
        }
    };
    private OneHandBackgroundView mBgView;

    private LayoutParams mFgLp;

    private final ViewTreeObserver.OnComputeInternalInsetsListener mInsetsComputer
            = new ViewTreeObserver.OnComputeInternalInsetsListener() {
        @Override
        public void onComputeInternalInsets(InternalInsetsInfo inoutInfo) {
            if (inoutInfo.touchableRegion.isEmpty() && mWinInfo.mScale != 1.0f) {
                Rect r = mWinInfo.mMagnifyRect;
                int screenW = mWinInfo.getScreenWidth();
                int screenH = mWinInfo.getScreenHeight();
                Rect topRect = new Rect(0, 0, screenW, r.top - 1);
                Rect bottomRect = new Rect(0, r.bottom + 1, screenW, screenH);
                Rect leftRect = new Rect(0, r.top, r.left - 1, r.bottom);
                Rect rightRect = new Rect(r.right + 1, r.top, screenW, r.bottom);
                inoutInfo.touchableRegion.set(topRect);
                inoutInfo.touchableRegion.union(leftRect);
                inoutInfo.touchableRegion.union(rightRect);
                inoutInfo.touchableRegion.union(bottomRect);
                inoutInfo.setTouchableInsets(InternalInsetsInfo.TOUCHABLE_INSETS_REGION);
            }
        }
    };
    private OneHandUtils mUtils;
    private OneHandWindowInfo mWinInfo;

    public OneHandBGWindow(Context context) {
        Log.d(TAG, "OneHandBGWindow() start");
        mContext = context;
        mHandler = new Handler();

        mUtils = OneHandUtils.getInstance();
        mUtils.startWallpaperImageTask();

        mBgView = (OneHandBackgroundView) View.inflate(context, R.layout.onehand_background, null);
        mBgView.setZ(0);

        mWinInfo = OneHandWindowInfo.getInstance();

        long current = 0;
        if (DBG) {
            current = SystemClock.elapsedRealtime();
        }
        mBgBitmapImage = mUtils.getWallpaperImage();
        if (DBG) {
            Log.d(TAG, "elapsed time to get wallpaer = "
                    + (SystemClock.elapsedRealtime() - current));
        }

        createWindow();
        mUtils.trimMemory();

        if (DBG) {
            Log.d(TAG, "OneHandBackgroundWindow() end. mScreenRect=" + mWinInfo);
        }
    }

    void setForegroundTransparentRect(Rect r) {
        if (DBG) {
            Log.w(TAG, "setForegroundTransparentRect() rect=" + r);
        }
        if (mBgView != null) {
            mBgView.setTransparentArea(r);
            mBgView.invalidate();
        }
    }

    void createWindow() {
        Log.d(TAG, "createWindow() callers=" + Debug.getCallers(5));
        LayoutParams lp = new LayoutParams(
                FreemeWindowManager.FreemeLayoutParams.TYPE_ONEHAND_CONTROLLER,
                LayoutParams.FLAG_FULLSCREEN
                        | LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        lp.width = mWinInfo.getScreenWidth();
        lp.height = mWinInfo.getScreenHeight();
        lp.x = 0;
        lp.y = 0;
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.softInputMode = LayoutParams.SOFT_INPUT_STATE_HIDDEN;
        lp.privateFlags |= LayoutParams.PRIVATE_FLAG_SHOW_FOR_ALL_USERS;
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= LayoutParams.FLAG_HARDWARE_ACCELERATED;
            lp.privateFlags |= LayoutParams.PRIVATE_FLAG_FORCE_HARDWARE_ACCELERATED;
        }
        lp.setTitle("OneHandBG");
        mFgLp = lp;

        if (mBgView != null) {
            mBgView.setBackground(mBgBitmapImage);
        }
    }

    void showFgWindow() {
        if (mBgView != null) {
            if (DBG) {
                Log.d(TAG, "showFgWindow() mBgView.isWindowAdded()=" + mBgView.isWindowAdded()
                        + ", mBGImageValid=" + mUtils.isValidBGImage());
            }
            if (!mUtils.isValidBGImage()) {
                updateBackgroundImage();
            } else if (mBgView.getBackground() == null) {
                Log.d(TAG, "set BG image again");
                mBgBitmapImage = mUtils.getWallpaperImage();
                mBgView.setBackground(mBgBitmapImage);
                mBgView.invalidate();
            }
            mBgView.captureScreenshot();
            mBgView.getViewTreeObserver().addOnComputeInternalInsetsListener(mInsetsComputer);
            if (!mBgView.isWindowAdded()) {
                mUtils.addWindow(mBgView, mFgLp);
                mBgView.setWindowAdded(true);
            }
            mBgView.setHasDrawn(false);
            mBgView.invalidate();
            mBgView.setTransparentArea(null);
        }
    }

    void hideFgWindow() {
        if (DBG) {
            Log.d(TAG, "hideFgWindow() callers=" + Debug.getCallers(5));
        }
        if (mBgView != null && mBgView.isWindowAdded()) {
            mBgView.setWindowAdded(false);
            mUtils.removeWindow(mBgView);
        }
    }

    boolean isWindowDrawn() {
        return mBgView != null && mBgView.isWindowAdded() && mBgView.hasDrawn();
    }

    void updateOutsideViews(Rect r) {
        mBgView.updateOutsideViews(r);
    }

    boolean captureScreenshot() {
        return mBgView.captureScreenshot();
    }

    void scaleScreenshot(float scale, int offsetX, int offsetY) {
        mBgView.scaleScreenshot(scale, offsetX, offsetY);
    }

    void hideScreenshot() {
        mBgView.hideScreenshot();
        mBgView.refreshTextView();
    }

    void hideButtonViews() {
        mBgView.hideButtonViews();
    }

    void setButtonClickCallback(OneHandBackgroundView.ButtonClickActionCallback click,
                                OneHandBackgroundView.ButtonLongClickActionCallback longClick) {
        mBgView.setButtonClickCallback(click, longClick);
    }

    void updateBackgroundImage() {
        if (DBG) {
            Log.d(TAG, "updateBackgroundImage()");
        }
        mUtils.startWallpaperImageTask();

        mBgUpdateRetryCount = 0;

        mHandler.removeCallbacks(mBgUpdateRunnable);
        mHandler.postDelayed(mBgUpdateRunnable, 300);
    }

    void onFontLocaleChanged() {
        mBgView.refreshTextView();
    }
}

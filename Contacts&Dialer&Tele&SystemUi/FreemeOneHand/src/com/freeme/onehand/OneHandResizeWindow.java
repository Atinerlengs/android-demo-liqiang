package com.freeme.onehand;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.freeme.view.FreemeWindowManager;

public class OneHandResizeWindow {
    private static final String TAG = "OneHandResizeWindow";
    private static final boolean DBG = OneHandConstants.DEBUG;

    private Context mContext;
    private Handler mHandler;

    private FrameLayout mMainView;
    private ImageView mBgView;
    private ImageView mResizeHandle;

    private OneHandController mController;
    private OneHandUtils mUtils;
    private OneHandWindowInfo mWinInfo;
    private Rect mResizeRect = new Rect();
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            finishWindow();
        }
    };

    public OneHandResizeWindow(Context context, OneHandController controller) {
        Log.d(TAG, "OneHandResizeWindow() start");
        mContext = context;
        mHandler = new Handler();

        mWinInfo = OneHandWindowInfo.getInstance();
        mUtils = OneHandUtils.getInstance();
        mController = controller;
    }

    void showInitWindow() {
        showWindow(true);
        mHandler.postDelayed(mHideRunnable, 10000);
    }

    void showWindow(boolean init) {
        if (DBG) {
            Log.d(TAG, "showWindow() init=" + init);
        }
        if (mMainView == null || mBgView == null || mResizeHandle == null) {
            LayoutParams lp = new LayoutParams(
                    FreemeWindowManager.FreemeLayoutParams.TYPE_ONEHAND_CONTROLLER,
                    LayoutParams.FLAG_NOT_FOCUSABLE
                            | LayoutParams.FLAG_FULLSCREEN
                            | LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.TOP | Gravity.LEFT;
            lp.width = LayoutParams.MATCH_PARENT;
            lp.height = LayoutParams.MATCH_PARENT;
            lp.format = PixelFormat.TRANSPARENT;
            lp.privateFlags |= LayoutParams.PRIVATE_FLAG_SHOW_FOR_ALL_USERS;
            if (ActivityManager.isHighEndGfx()) {
                lp.flags |= LayoutParams.FLAG_HARDWARE_ACCELERATED;
                lp.privateFlags |= LayoutParams.PRIVATE_FLAG_FORCE_HARDWARE_ACCELERATED;
            }
            lp.setTitle("OneHandResize");

            mMainView = (FrameLayout) View.inflate(mContext, R.layout.resize_background, null);
            mBgView = (ImageView) mMainView.findViewById(R.id.resize_img);
            mBgView.invalidate();
            if (init) {
                mMainView.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        finishWindow();
                    }
                });
                mResizeHandle = (ImageView) mMainView.findViewById(R.id.resize_handle);
                mResizeHandle.setVisibility(View.VISIBLE);
            }
            GradientDrawable drawable = (GradientDrawable) mBgView.getDrawable();
            if (!init) {
                drawable.setColor(mContext.getColor(R.color.resize_guide_bg_color));
            }
            drawable.setStroke(mWinInfo.getResizeStrokeThickness(),
                    mContext.getColor(R.color.resize_guide_border_color));
            mResizeRect.set(mWinInfo.mMagnifyRect);
            resizeViewRect();
            if (init) {
                int handleSize = mWinInfo.getResizeStrokeThickness() * 3;

                FrameLayout.LayoutParams vlp = (FrameLayout.LayoutParams) mResizeHandle
                        .getLayoutParams();
                vlp.leftMargin = (mWinInfo.isLeftHandMode() ? mResizeRect.right : mResizeRect.left)
                        - handleSize;
                vlp.topMargin = mResizeRect.top - handleSize;
                vlp.width = handleSize * 2;
                vlp.height = handleSize * 2;

                mResizeHandle.setLayoutParams(vlp);
                mResizeHandle.requestLayout();
                mResizeHandle.invalidate();
            }
            mBgView.invalidate();
            mUtils.addWindow(mMainView, lp);
        } else {
            mHandler.removeCallbacks(mHideRunnable);
            mResizeHandle.setVisibility(View.GONE);
            ((GradientDrawable) mBgView.getDrawable()).setColor(mContext
                    .getColor(R.color.resize_guide_bg_color));
            mBgView.invalidate();
        }
    }

    private void resizeViewRect() {
        int thickness = mWinInfo.getResizeStrokeThickness();
        FrameLayout.LayoutParams vlp = (FrameLayout.LayoutParams) mBgView.getLayoutParams();
        vlp.leftMargin = mResizeRect.left - thickness;
        vlp.topMargin = mResizeRect.top - thickness;
        vlp.width = mResizeRect.width() + (thickness * 2);
        int height = mResizeRect.height();
        if (!mWinInfo.isSupportNavigationBar()) {
            thickness *= 2;
        }
        vlp.height = height + thickness;
        mBgView.setLayoutParams(vlp);
        mBgView.setScaleType(ScaleType.FIT_XY);
        mBgView.requestLayout();
        mBgView.invalidate();
    }

    void finishWindow() {
        if (DBG) {
            Log.d(TAG, "finishWindow() mMainView=" + mMainView);
        }
        if (mMainView != null) {
            mHandler.removeCallbacks(mHideRunnable);
            mUtils.removeWindow(mMainView);
            OneHandViewUnbindHelper.unbindReferences(mMainView);
            mUtils.trimMemory();
            mMainView = null;
        }
    }

    void hideWindow() {
        if (DBG) {
            Log.d(TAG, "hideWindow() mMainView=" + mMainView);
        }
        if (mMainView != null) {
            float scale = mResizeRect.height() / (float) mWinInfo.getScreenHeight();
            finishWindow();
            mController.changeScaleAnimate(scale, mResizeRect.left, mResizeRect.top);
        }
    }

    void resizeWindow(int x, int y) {
        float origDiagDist = (int) Math.hypot(mWinInfo.mMagnifyRect.width(),
                mWinInfo.mMagnifyRect.height());
        float currDiagDist;
        if (mWinInfo.isLeftHandMode()) {
            currDiagDist = (int) Math.hypot(x - mWinInfo.mMagnifyRect.left,
                    y - mWinInfo.mMagnifyRect.bottom);
        } else {
            currDiagDist = (int) Math.hypot(x - mWinInfo.mMagnifyRect.right,
                    y - mWinInfo.mMagnifyRect.bottom);
        }
        float scale = currDiagDist / origDiagDist;

        mResizeRect.set(mWinInfo.mMagnifyRect);
        mResizeRect.top = (int) ((mResizeRect.bottom - (mResizeRect.height() * scale)) + 0.5f);
        if (mWinInfo.isLeftHandMode()) {
            mResizeRect.right = (int) ((mResizeRect.left + (mResizeRect.width() * scale)) + 0.5f);
        } else {
            mResizeRect.left = (int) ((mResizeRect.right - (mResizeRect.width() * scale)) + 0.5f);
        }

        int maxW = (int) ((mWinInfo.getScreenWidth() * mWinInfo.getMaxScale()) + 0.5f);
        int maxH = (int) ((mWinInfo.getScreenHeight() * mWinInfo.getMaxScale()) + 0.5f);
        int minW = (int) ((mWinInfo.getScreenWidth() * mWinInfo.getMinScale()) + 0.5f);
        int minH = (int) ((mWinInfo.getScreenHeight() * mWinInfo.getMinScale()) + 0.5f);
        if (mResizeRect.height() > maxH) {
            mResizeRect.top = mResizeRect.bottom - maxH;
            if (mWinInfo.isLeftHandMode()) {
                mResizeRect.right = mResizeRect.left + maxW;
            } else {
                mResizeRect.left = mResizeRect.right - maxW;
            }
        } else if (mResizeRect.height() < minH) {
            mResizeRect.top = mResizeRect.bottom - minH;
            if (mWinInfo.isLeftHandMode()) {
                mResizeRect.right = mResizeRect.left + minW;
            } else {
                mResizeRect.left = mResizeRect.right - minW;
            }
        }
        if (DBG) {
            Log.d(TAG, (new StringBuilder(64))
                    .append("resizeWindow() ")
                    .append("x=").append(x).append(", ")
                    .append("y=").append(y).append(", ")
                    .append("scale=").append(scale).append(", ")
                    .append("mResizeRect=").append(mResizeRect)
                    .toString());
        }
        resizeViewRect();
    }
}

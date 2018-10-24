package com.freeme.recents.presentation.view.component.overlappingstackview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.freeme.internal.app.AppLockPolicy;
import com.freeme.recents.RecentsUtils;
import com.freeme.recents.SystemServicesProxy;
import com.freeme.recents.presentation.view.component.overlappingstackview.utils.Utils;

public class ChildViewThumbnail extends View {

    // Drawing
    float mDimAlpha;
    Config mConfig;
    Matrix mScaleMatrix = new Matrix();
    Paint mDrawPaint = new Paint();
    RectF mBitmapRect = new RectF();
    RectF mLayoutRect = new RectF();
    BitmapShader mBitmapShader;
    LightingColorFilter mLightingColorFilter = new LightingColorFilter(0xffffffff, 0);

    private String mTaskPkg;

    // Thumbnail alpha
    float mThumbnailAlpha;
    ValueAnimator mThumbnailAlphaAnimator;
    ValueAnimator.AnimatorUpdateListener mThumbnailAlphaUpdateListener
            = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mThumbnailAlpha = (float) animation.getAnimatedValue();
            updateThumbnailPaintFilter();
        }
    };

    public ChildViewThumbnail(Context context) {
        this(context, null);
    }

    public ChildViewThumbnail(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChildViewThumbnail(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ChildViewThumbnail(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mConfig = Config.getInstance();
        mDrawPaint.setColorFilter(mLightingColorFilter);
        mDrawPaint.setFilterBitmap(true);
        mDrawPaint.setAntiAlias(true);
        mNavHeight = getNavgationBarHeight();
    }

    @Override
    protected void onFinishInflate() {
        mThumbnailAlpha = mConfig.childViewThumbnailAlpha;
        updateThumbnailPaintFilter();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            mLayoutRect.set(0, 0, getWidth(), getHeight());
            updateThumbnailScale();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw the thumbnail with the rounded corners
        int[] pos = new int[2];
        getLocationOnScreen(pos);
        //*/ freeme.gouzhouping. 20180604. applock
        if (AppLockPolicy.isSupportAppLock()) {
            SystemServicesProxy ssp = SystemServicesProxy.getInstance(mContext);
            if (ssp != null && !TextUtils.isEmpty(mTaskPkg) && ssp.isAppLockedPackage(mTaskPkg)) {
                int viewWidth = getWidth();
                int viewHeight = getHeight();
                Bitmap bmp = ssp.getAppLockedTaskThumbnail(viewWidth, viewHeight);
                mDrawPaint.setShader(new BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
                canvas.drawRoundRect(0, 0, viewWidth, viewHeight,
                        mConfig.childViewRoundedCornerRadiusPx,
                        mConfig.childViewRoundedCornerRadiusPx, mDrawPaint);
                bmp.recycle();
                return;
            }
        }
        //*/
        canvas.drawRoundRect(0, 0, getWidth(), getHeight(),
                mConfig.childViewRoundedCornerRadiusPx,
                mConfig.childViewRoundedCornerRadiusPx, mDrawPaint);
    }

    /**
     * Sets the thumbnail to a given bitmap.
     */
    void setThumbnail(Bitmap bm, boolean ismultiwindowmode) {
        mThumbnail = bm;
        if (bm != null) {
            int systemOrientation = getResources().getConfiguration().orientation;
            int width = mThumbnail.getWidth();
            int height = mThumbnail.getHeight();
            if (ismultiwindowmode) {
                if (mThumbnail.getHeight() / 2 < mContext.getDisplay().getHeight()) {
                    mBitmapRect.set(0, 0, mThumbnail.getWidth(), mThumbnail.getHeight());
                } else {
                    mBitmapRect.set(0, 0, mThumbnail.getWidth(), mThumbnail.getHeight() / 2);
                }
            } else {
                if (systemOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    if (width < height) {
                        mThumbnail = RecentsUtils.BitmapRotateToDegrees(bm, 90.0f);
                    }
                } else if (systemOrientation == Configuration.ORIENTATION_PORTRAIT) {
                    if (width > height) {
                        mThumbnail = RecentsUtils.BitmapRotateToDegrees(bm, 90.0f);
                    }
                }
                mBitmapRect.set(0, 0, mThumbnail.getWidth(), mThumbnail.getHeight());
            }
            mBitmapShader = new BitmapShader(mThumbnail, Shader.TileMode.CLAMP,
                    Shader.TileMode.CLAMP);
            mDrawPaint.setShader(mBitmapShader);
            updateThumbnailScale();
        } else {
            mBitmapShader = null;
            mDrawPaint.setShader(null);
        }
        updateThumbnailPaintFilter();
    }

    /**
     * Updates the paint to draw the thumbnail.
     */
    void updateThumbnailPaintFilter() {
        int mul = (int) ((1.0f - mDimAlpha) * mThumbnailAlpha * 255);
        int add = (int) ((1.0f - mDimAlpha) * (1 - mThumbnailAlpha) * 255);
        if (mBitmapShader != null) {
            mLightingColorFilter =
                    new LightingColorFilter(Color.argb(255, mul, mul, mul),
                            Color.argb(0, add, add, add));
            mDrawPaint.setColorFilter(mLightingColorFilter);
            mDrawPaint.setColor(0xffffffff);
        } else {
            int grey = mul + add;
            mDrawPaint.setColorFilter(null);
            mDrawPaint.setColor(Color.argb(255, grey, grey, grey));
        }
        invalidate();
    }

    /**
     * Updates the thumbnail shader's scale transform.
     */
    void updateThumbnailScale() {
        if (mBitmapShader != null) {
            mScaleMatrix.setRectToRect(mBitmapRect, mLayoutRect, Matrix.ScaleToFit.FILL);
            int systemOrientation = mContext.getResources().getConfiguration().orientation;
            float bitmapRectHeight = mBitmapRect.height();
            float bitmapRectWidth = mBitmapRect.width();
            int childHeaderHeight = mConfig.childHeaderHeight;
            if (systemOrientation == Configuration.ORIENTATION_PORTRAIT) {
                if (bitmapRectHeight > bitmapRectWidth) {
                    if (bitmapRectHeight ==
                            (mContext.getDisplay().getHeight() + mNavHeight) / 2) {
                        mScaleMatrix.postScale(DEFAULT_SCALE, DEFAULT_SCALE);
                    } else if (bitmapRectHeight - childHeaderHeight <
                            mContext.getDisplay().getHeight() / 2) {
                        mScaleMatrix.postScale(DEFAULT_SCALE, SPLIT_SCREEN_VALID_SCALE);
                    }
                }
            } else if (systemOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                if (bitmapRectWidth > bitmapRectHeight) {
                    mScaleMatrix.postScale(bitmapRectWidth <
                            mContext.getDisplay().getWidth() / 2
                            ? SPLIT_SCREEN_VALID_SCALE : DEFAULT_SCALE, DEFAULT_SCALE);
                }
            }
            mBitmapShader.setLocalMatrix(mScaleMatrix);
        }
    }

    /**
     * Sets the dim alpha, only used when we are not using hardware layers.
     */
    public void setDimAlpha(float dimAlpha) {
        mDimAlpha = dimAlpha;
        updateThumbnailPaintFilter();
    }

    /**
     * Binds the thumbnail view to the child
     */
    void rebindToChild(Bitmap thumbnail, boolean ismultiwindowmode, String pkgName) {
        mTaskPkg = pkgName;
        if (thumbnail != null) {
            setThumbnail(thumbnail, ismultiwindowmode);
        } else {
            setThumbnail(null, ismultiwindowmode);
        }
    }

    /**
     * Unbinds the thumbnail view from the child
     */
    void unbindFromChild() {
        setThumbnail(null, false);
    }

    Bitmap mThumbnail;

    public Bitmap getThumbnail() {
        return mThumbnail;
    }

    /**
     * Handles focus changes.
     */
    void onFocusChanged(boolean focused) {
        if (focused) {
            if (Float.compare(getAlpha(), 1f) != 0) {
                startFadeAnimation(1f, 0, 150, null);
            }
        } else {
            if (Float.compare(getAlpha(), mConfig.childViewThumbnailAlpha) != 0) {
                startFadeAnimation(mConfig.childViewThumbnailAlpha, 0, 150, null);
            }
        }
    }

    /**
     * Prepares for the enter recents animation, this gets called before the the view
     * is first visible and will be followed by a startEnterRecentsAnimation() call.
     */
    void prepareEnterRecentsAnimation(boolean isChildViewLaunchTargetChild) {
        if (isChildViewLaunchTargetChild) {
            mThumbnailAlpha = 1f;
        } else {
            mThumbnailAlpha = mConfig.childViewThumbnailAlpha;
        }
        updateThumbnailPaintFilter();
    }

    /**
     * Animates this child thumbnail as it enters Recents.
     */
    void startEnterRecentsAnimation(int delay, Runnable postAnimRunnable) {
        startFadeAnimation(mConfig.childViewThumbnailAlpha, delay,
                mConfig.childViewEnterFromAppDuration, postAnimRunnable);
    }

    /**
     * Animates this child thumbnail as it exits Recents.
     */
    void startLaunchChildAnimation(Runnable postAnimRunnable) {
        startFadeAnimation(1f, 0, mConfig.childViewExitToAppDuration, postAnimRunnable);
    }

    /**
     * Starts a new thumbnail alpha animation.
     */
    void startFadeAnimation(float finalAlpha, int delay, int duration, final Runnable postAnimRunnable) {
        Utils.cancelAnimationWithoutCallbacks(mThumbnailAlphaAnimator);
        mThumbnailAlphaAnimator = ValueAnimator.ofFloat(mThumbnailAlpha, finalAlpha);
        mThumbnailAlphaAnimator.setStartDelay(delay);
        mThumbnailAlphaAnimator.setDuration(duration);
        mThumbnailAlphaAnimator.setInterpolator(mConfig.fastOutSlowInInterpolator);
        mThumbnailAlphaAnimator.addUpdateListener(mThumbnailAlphaUpdateListener);
        if (postAnimRunnable != null) {
            mThumbnailAlphaAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    postAnimRunnable.run();
                }
            });
        }
        mThumbnailAlphaAnimator.start();
    }

    private int mNavHeight;
    private static final float SPLIT_SCREEN_VALID_SCALE = 0.6f;
    private static final float DEFAULT_SCALE = 1.0f;

    private int getNavgationBarHeight() {
        com.android.systemui.recents.misc.SystemServicesProxy sys =
                com.android.systemui.recents.misc.SystemServicesProxy.getInstance(mContext);
        int navHeight = mContext.getResources().getDimensionPixelSize(
                com.android.internal.R.dimen.navigation_bar_height);
        return sys.hasSoftNavigationBar() ? navHeight : 0;
    }
}

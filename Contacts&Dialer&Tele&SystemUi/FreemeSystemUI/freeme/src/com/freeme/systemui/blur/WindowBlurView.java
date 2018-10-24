package com.freeme.systemui.blur;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import com.freeme.systemui.blur.WindowBlur.OnBlurObserver;

public class WindowBlurView extends View implements OnBlurObserver {

    public interface BlurStateListener {
        void onBlurFailed();

        void onBlurSuccess(Bitmap bitmap);
    }

    private static final String TAG = "WindowBlurView";

    private static final int ALPHA_MAX = 255;

    private int mAlpha = ALPHA_MAX;
    private int mStartLeft = 0;
    private int mStartTop = 0;
    private int mMaxLayer = Integer.MAX_VALUE;
    private int mMinLayer = 0;
    private int mBlurRadius = 10;

    private long mRefreshDuration = -1;
    private long mSwitchTime = 0;

    private float mScale = 1.0f;

    private boolean mAnimate = false;
    private boolean mAutoStart = false;
    private boolean mReleased = false;
    private boolean mShowBlur = true;

    private BitmapDrawable mLastDrawable;
    private BitmapDrawable mBlurDrawable;
    private Drawable mMaskDrawable;
    private final Drawable mDefaultDrawable = new ColorDrawable(Color.BLACK);

    private Rect mShotRect;

    private WindowBlur mWindowBlur;

    private BlurStateListener mBlurStateListener;

    private Runnable mFrashRunnable = new Runnable() {
        @Override
        public void run() {
            refresh();
        }
    };

    public WindowBlurView(Context context) {
        super(context);
        initBlurArg();
    }

    public WindowBlurView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initBlurArg();
    }

    public WindowBlurView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initBlurArg();
    }

    public WindowBlurView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initBlurArg();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        refreshCoordinate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        refreshCoordinate();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            refresh();
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            refresh();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        checkAutoFresh();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d(TAG, "onDraw mBlurDrawable=" + mBlurDrawable
                + " mAlpha:" + mAlpha
                + " mStartLeft:" + mStartLeft
                + " mStartTop:" + mStartTop
                + " getRight:" + getRight()
                + " getBottom:" + getBottom());

        if (mBlurDrawable != null) {
            canvas.save();
            canvas.clipRect(0, 0, getRight(), getBottom());
            if (mAnimate) {
                drawLastDrawable(canvas);
                float enterAlpha = (float) (mLastDrawable == null ? ALPHA_MAX : ALPHA_MAX - mLastDrawable.getAlpha());
                if (enterAlpha < ALPHA_MAX) {
                    mBlurDrawable.setAlpha((int) ((((float) mAlpha) * enterAlpha) / ALPHA_MAX));
                    invalidate();
                } else {
                    mBlurDrawable.setAlpha(mAlpha);
                }
            }
            if (mShowBlur) {
                mBlurDrawable.draw(canvas);
                canvas.restore();
            }
            if (mMaskDrawable != null) {
                mMaskDrawable.setBounds(0, 0, getWidth(), getHeight());
                mMaskDrawable.draw(canvas);
            }
        } else if (mDefaultDrawable != null) {
            mDefaultDrawable.setBounds(0, 0, getRight(), getBottom());
            mDefaultDrawable.draw(canvas);
        }
    }

    private void initBlurArg() {
        mBlurRadius = (int) (getContext().getResources().getDisplayMetrics().density * 10.0f);
        if (mBlurRadius > 25) {
            mBlurRadius = 25;
        }
    }

    public void setProgress(float delta) {
        int alpha = (int) (ALPHA_MAX * delta);
        if (mAlpha != alpha) {
            mAlpha = alpha;
            Log.i(TAG, "setProgress: " + alpha);
            if (mAlpha > ALPHA_MAX) {
                mAlpha = ALPHA_MAX;
            } else if (mAlpha < 0) {
                mAlpha = 0;
            }
            if (mBlurDrawable != null) {
                mBlurDrawable.setAlpha(mAlpha);
            }
            if (mMaskDrawable != null) {
                mMaskDrawable.setAlpha(mAlpha);
            }
            if (mDefaultDrawable != null) {
                mDefaultDrawable.setAlpha(mAlpha);
            }
            invalidate();
        }
    }

    public void setShowBlur(boolean showBlur) {
        if (mShowBlur != showBlur) {
            mShowBlur = showBlur;
            invalidate();
        }
    }

    public void setMaskDrawable(Drawable able) {
        mMaskDrawable = able;
    }

    private void releaseBitmapDrawable(BitmapDrawable able) {
        if (able != null) {
            Bitmap bitmap = able.getBitmap();
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }

    private void drawLastDrawable(Canvas canvas) {
        if (mLastDrawable != null) {
            int alpha = (((int) (System.currentTimeMillis() - mSwitchTime)) * ALPHA_MAX) / 200;
            if (alpha > ALPHA_MAX) {
                alpha = ALPHA_MAX;
            } else if (alpha < 0) {
                alpha = 0;
            }
            if (alpha == ALPHA_MAX) {
                releaseBitmapDrawable(mLastDrawable);
                mLastDrawable = null;
            } else {
                mLastDrawable.setAlpha(ALPHA_MAX - alpha);
                mLastDrawable.draw(canvas);
            }
        }
    }

    public void refresh() {
        Log.i(TAG, "refresh");
        mReleased = false;
        if (mWindowBlur == null) {
            mWindowBlur = new WindowBlur(getContext());
            mWindowBlur.setOnBlurObserver(this);
        }
        mWindowBlur.setBlurRadius(mBlurRadius);
        mWindowBlur.start();
    }

    public void setRrefreshDuration(long mills) {
        mRefreshDuration = mills;
        if (-1 == mRefreshDuration) {
            mAutoStart = false;
            removeCallbacks(mFrashRunnable);
            return;
        }
        if (mRefreshDuration < 1000) {
            mRefreshDuration = 1000;
        }
        mAutoStart = true;
    }

    public void setScreenLayer(int minLayer, int maxLayer) {
        mMinLayer = minLayer;
        mMaxLayer = maxLayer;
    }

    public void setScale(float scale) {
        mScale = scale;
    }

    public void setBlurRadius(int blurRadius) {
        mBlurRadius = blurRadius;
    }


    private void refreshCoordinate() {
        int[] location = getLocationOnScreen();
        mStartLeft = location[0];
        mStartTop = location[1];
        setDrawableBound();
    }

    private void setDrawableBound() {
        setDrawableBound(mBlurDrawable);
    }

    protected void setDrawableBound(Drawable able) {
        if (able != null) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(displayMetrics);
            able.setBounds(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        }
    }

    private boolean checkAutoFresh() {
        if (mRefreshDuration < 0 || !mAutoStart || getWindowVisibility() != 0 || !isAttachedToWindow()) {
            return false;
        }
        postDelayed(mFrashRunnable, mRefreshDuration);
        return true;
    }

    public void onBlurFinish(Bitmap blurBitmap) {
        Log.i(TAG, "onBlurFinish");
        if (mReleased) {
            if (blurBitmap != null) {
                blurBitmap.recycle();
            }
            return;
        }
        Bitmap currBitmap;
        if (mBlurDrawable == null) {
            currBitmap = null;
        } else {
            currBitmap = mBlurDrawable.getBitmap();
        }
        if (!(currBitmap == null || currBitmap == blurBitmap)) {
            mLastDrawable = mBlurDrawable;
            mSwitchTime = System.currentTimeMillis();
            setDrawableBound(mLastDrawable);
        }
        if (blurBitmap != null) {
            mBlurDrawable = new BitmapDrawable(getContext().getResources(), blurBitmap);
            mBlurDrawable.setAlpha(mAlpha);
            setDrawableBound();
        } else {
            mBlurDrawable = null;
        }
        invalidate();
        if (mBlurStateListener != null) {
            if (mBlurDrawable == null && mDefaultDrawable == null) {
                Log.d(TAG, "blur failed");
                mBlurStateListener.onBlurFailed();
            } else {
                mBlurStateListener.onBlurSuccess(blurBitmap);
            }
        }
    }

    public Bitmap getBaseBitmap() {
        if (mIsKeyguardShow) {
            return loadBitmapFromBackDrop();
        } else {
            return FreemeScreenShot.screenShotBitmap(getContext(), mMinLayer, mMaxLayer, mScale, mShotRect);
        }
    }

    public void setBlurStateListener(BlurStateListener blurStateListener) {
        mBlurStateListener = blurStateListener;
    }

    private boolean mIsKeyguardShow;
    private View mBackDropView;

    private Bitmap loadBitmapFromBackDrop() {
        if (mBackDropView == null) {
            return null;
        }

        int w = mBackDropView.getWidth();
        int h = mBackDropView.getHeight();
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(Color.argb(0xe4, 0xe4, 0xe4, 0xe4));
        mBackDropView.layout(0, 0, w, h);
        mBackDropView.draw(c);
        Bitmap bmpScale = Bitmap.createScaledBitmap(bmp, (int)(w * mScale), (int)(h * mScale), true);
        bmp.recycle();
        return bmpScale;
    }

    public void setBackDropView(View v) {
        mBackDropView = v;
    }

    public void onKeyguardStateChanged(boolean show) {
        mIsKeyguardShow = show;
    }
}

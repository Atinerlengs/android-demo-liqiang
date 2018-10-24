package com.freeme.onehand;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Debug;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public final class OneHandBackgroundView extends FrameLayout {
    private static final String TAG = "OneHandBackgroundView";
    private static final boolean DBG = OneHandConstants.DEBUG;

    private ButtonClickActionCallback mButtonClickActionCallback;
    private ButtonLongClickActionCallback mButtonLongClickActionCallback;

    private final OnClickListener mClickListener;
    private final OnLongClickListener mLongClickListener;
    private final OnTouchListener mBGViewTouchListener;

    private TextView mTitleTextView;
    private TextView mHelpTextView;
    private FrameLayout mScreenshotView;
    private TextView mSecureViewText;
    private RelativeLayout mSideAreaContainer;
    private ImageView mSettingView;
    private ImageView mArrowView;
    private RelativeLayout mBottomAreaContainer;

    private Rect mTransparentRect;
    private Paint mTransparentPaint;
    private Paint mBorderPaint;

    boolean mWindowAdded;
    boolean mHasDrawn;
    private boolean mButtonViewHiddden;

    private OneHandUtils mUtils;
    private OneHandWindowInfo mWinInfo;

    interface ButtonClickActionCallback {
        void onBackButtonClicked();
        void onHomeButtonClicked();
        void onRecentButtonClicked();

        void onExitButtonClicked();
        void onSettingsButtonClicked();
        void onSwitchButtonClicked();
    }

    interface ButtonLongClickActionCallback {
        void onBackButtonClicked();
        void onHomeButtonClicked();
        void onRecentButtonClicked();
    }

    public OneHandBackgroundView(Context context) {
        this(context, null);
    }

    public OneHandBackgroundView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OneHandBackgroundView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mTransparentRect = new Rect();

        mClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.recent_button:
                        mButtonClickActionCallback.onRecentButtonClicked();
                        break;
                    case R.id.home_button:
                        mButtonClickActionCallback.onHomeButtonClicked();
                        break;
                    case R.id.back_button:
                        mButtonClickActionCallback.onBackButtonClicked();
                        break;
                    case R.id.arrow_button:
                        mButtonClickActionCallback.onSwitchButtonClicked();
                        break;
                    case R.id.setting_button:
                        mButtonClickActionCallback.onSettingsButtonClicked();
                        break;
                }
            }
        };
        mLongClickListener = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                switch (v.getId()) {
                    case R.id.recent_button:
                        mButtonLongClickActionCallback.onRecentButtonClicked();
                        break;
                    case R.id.home_button:
                        mButtonLongClickActionCallback.onHomeButtonClicked();
                        break;
                    case R.id.back_button:
                        mButtonLongClickActionCallback.onBackButtonClicked();
                        break;
                }
                return true;
            }
        };
        mBGViewTouchListener = new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getEventTime() - event.getDownTime() < ViewConfiguration
                            .getDoubleTapTimeout()) {
                        boolean isLeft = mWinInfo.isLeftHandMode();
                        int x = (int) event.getRawX();
                        int y = (int) event.getRawY();
                        Rect r = mWinInfo.mMagnifyRect;
                        if ((!isLeft || x >= r.left) && ((isLeft || x <= r.right) && y <= r.bottom)) {
                            mButtonClickActionCallback.onExitButtonClicked();
                        }
                    }
                }
                return false;
            }
        };

        mWinInfo = OneHandWindowInfo.getInstance();
        mUtils = OneHandUtils.getInstance();

        mTransparentPaint = new Paint();
        mTransparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        mTransparentPaint.setColor(Color.TRANSPARENT);
        mTransparentPaint.setAntiAlias(true);

        mBorderPaint = new Paint();
        mBorderPaint.setColor(getResources().getColor(R.color.border_color));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mArrowView = (ImageView) findViewById(R.id.arrow_button);
        mSettingView = (ImageView) findViewById(R.id.setting_button);
        mArrowView.setOnClickListener(mClickListener);
        mSettingView.setOnClickListener(mClickListener);

        findViewById(R.id.bottom_area_container).setOnClickListener(mClickListener);
        findViewById(R.id.recent_button).setOnClickListener(mClickListener);
        findViewById(R.id.home_button).setOnClickListener(mClickListener);
        findViewById(R.id.back_button).setOnClickListener(mClickListener);

        findViewById(R.id.recent_button).setOnLongClickListener(mLongClickListener);
        findViewById(R.id.home_button).setOnLongClickListener(mLongClickListener);
        findViewById(R.id.back_button).setOnLongClickListener(mLongClickListener);

        findViewById(R.id.onehand_background_view).setOnTouchListener(mBGViewTouchListener);

        mSideAreaContainer = (RelativeLayout) findViewById(R.id.side_area_container);
        mBottomAreaContainer = (RelativeLayout) findViewById(R.id.bottom_area_container);
        mTitleTextView = (TextView) findViewById(R.id.onehand_help_title);
        mHelpTextView = (TextView) findViewById(R.id.onehand_help_text);
        mSecureViewText = (TextView) findViewById(R.id.secure_view_text);
        mScreenshotView = (FrameLayout) findViewById(R.id.reduced_screenshot_view);
    }

    @Override
    public void onDraw(Canvas canvas) {
        Rect r = mTransparentRect;
        canvas.drawRect(r, mTransparentPaint);
        canvas.drawLine(r.left - 1, r.top - 1, r.left - 1, r.bottom + 1,
                mBorderPaint);
        canvas.drawLine(r.left - 1, r.top - 1, r.right + 1, r.top - 1,
                mBorderPaint);
        canvas.drawLine(r.right + 1, r.top - 1, r.right + 1, r.bottom + 1,
                mBorderPaint);
        canvas.drawLine(r.left - 1, r.bottom + 1, r.right + 1, r.bottom + 1,
                mBorderPaint);
        mHasDrawn = true;
    }

    boolean isWindowAdded() {
        return mWindowAdded;
    }

    void setWindowAdded(boolean added) {
        mWindowAdded = added;
    }

    boolean hasDrawn() {
        return mHasDrawn;
    }

    void setHasDrawn(boolean drawn) {
        mHasDrawn = drawn;
    }

    boolean captureScreenshot() {
        refreshTextView();

        Bitmap bmp = SurfaceControl.screenshot(
                mWinInfo.getScreenWidth(), mWinInfo.getScreenHeight());
        if (bmp == null) {
            Log.d(TAG, "Failed to get screenshot");
            mScreenshotView.setBackgroundResource(R.drawable.secure_window_bg);
            mScreenshotView.setAlpha(1.0f);
            mSecureViewText.setVisibility(VISIBLE);
            return false;
        } else {
            final long now = SystemClock.uptimeMillis();

            mSecureViewText.setVisibility(GONE);
            if (mWinInfo.mOffsetY == 0) {
                mScreenshotView.setBackground(new BitmapDrawable(bmp));
            } else {
                Rect r = new Rect(mWinInfo.mMagnifyRect);
                r.intersect(0, 0, mWinInfo.getScreenWidth(), mWinInfo.getScreenHeight());
                mScreenshotView.setBackground(new BitmapDrawable(Bitmap
                        .createBitmap(bmp, r.left, r.top, r.width(), r.height())));
            }
            mScreenshotView.setAlpha(1.0f);

            if (DBG) {
                Log.d(TAG, "captureScreenshot() elapsed=" + (SystemClock.uptimeMillis() - now)
                        + ", mWinInfo.mMagnifyRect=" + mWinInfo.mMagnifyRect
                        + ", callers=" + Debug.getCallers(5));
            }
            return true;
        }
    }

    void scaleScreenshot(final float scale, final int offsetX, final int offsetY) {
        post(new Runnable() {
            @Override
            public void run() {
                final int w = mWinInfo.getScreenWidth();
                final int h = mWinInfo.getScreenHeight();

                LayoutParams lp = (LayoutParams) mScreenshotView.getLayoutParams();
                lp.width = (int) (w * scale);
                lp.height = (int) (h * scale);
                lp.leftMargin = offsetX;
                lp.topMargin = offsetY;
                mScreenshotView.setLayoutParams(lp);

                mSecureViewText.setScaleX(scale);
                mSecureViewText.setScaleY(scale);
                if ((0 < scale && scale < 1) && mButtonViewHiddden) {
                    int oriVisibility = mSideAreaContainer.getVisibility();
                    updateOutsideViews(mWinInfo.mMagnifyRect);
                    int newVisibility = mSideAreaContainer.getVisibility();
                    if (oriVisibility == VISIBLE && newVisibility == INVISIBLE) {
                        mUtils.playShortHaptic();
                    }
                }
            }
        });
    }

    void hideScreenshot() {
        if (DBG) {
            Log.d(TAG, "hideScreenshot()");
        }
        mScreenshotView.setAlpha(0.0f);
        mScreenshotView.setBackground(null);
    }

    public void setTransparentArea(Rect r) {
        if (r == null) {
            mTransparentRect.set(0, 0, 0, 0);
        } else {
            mTransparentRect.set(r);
        }
        updateOutsideViews(r);
        if (r == null && mWinInfo.mScale < 1.0f && !mWinInfo.isSwitchAnimationRunning()) {
            mSideAreaContainer.setVisibility(VISIBLE);
            if (mWinInfo.isSoftkeyMode()) {
                mBottomAreaContainer.setVisibility(VISIBLE);
            }
        }
    }

    void hideButtonViews() {
        mSideAreaContainer.setVisibility(INVISIBLE);
        if (mWinInfo.isSoftkeyMode()) {
            mBottomAreaContainer.setVisibility(INVISIBLE);
        }
        mButtonViewHiddden = true;
    }

    void updateOutsideViews(Rect r) {
        if (DBG) {
            Log.d(TAG, "updateOutsideViews() r=" + r + ", callers=" + Debug.getCallers(5));
        }
        if (r == null || mWinInfo.isReturnToFullScreen()) {
            hideButtonViews();
        } else {
            mSideAreaContainer.setVisibility(INVISIBLE);
            mArrowView.setVisibility(VISIBLE);
            mSettingView.setVisibility(VISIBLE);

            LayoutParams sideLp = (LayoutParams) mSideAreaContainer.getLayoutParams();
            sideLp.width = mWinInfo.getIconSize();
            sideLp.topMargin = r.top;
            sideLp.height = r.height();
            if (mWinInfo.isSupportNavigationBar()) {
                sideLp.height -= mWinInfo.getSideWindowGap();
            }
            if (mWinInfo.isLeftHandMode()) {
                sideLp.leftMargin = r.right + mWinInfo.getSideWindowGap();
                mArrowView.setImageResource(R.drawable.oho_btn_ic_right);
            } else {
                sideLp.leftMargin = (r.left - mWinInfo.getIconSize()) - mWinInfo.getSideWindowGap();
                mArrowView.setImageResource(R.drawable.oho_btn_ic_left);
            }
            mSideAreaContainer.setLayoutParams(sideLp);

            if (mWinInfo.isSoftkeyMode()) {
                LayoutParams bottomLp = (LayoutParams) mBottomAreaContainer.getLayoutParams();
                bottomLp.leftMargin = r.left - 1;
                bottomLp.width = r.width() + 2;
                bottomLp.topMargin = r.bottom;
                mBottomAreaContainer.setLayoutParams(bottomLp);
                mBottomAreaContainer.setVisibility(VISIBLE);
            }
            mHelpTextView.setVisibility(r.top > mHelpTextView.getBottom() ? VISIBLE : INVISIBLE);

            mSideAreaContainer.setVisibility(VISIBLE);
            if (    (!mWinInfo.isLeftHandMode()
                            && (r.left - mSideAreaContainer.getWidth() - mWinInfo.getSideMargin() < 0))
                    || (mWinInfo.isLeftHandMode()
                            && (r.right + mSideAreaContainer.getWidth() + mWinInfo.getSideMargin() > mWinInfo.getScreenWidth()))) {
                mSideAreaContainer.setVisibility(INVISIBLE);
            }
        }
    }

    void refreshTextView() {
        if (DBG) {
            Log.d(TAG, "refreshTextView()");
        }
        mTitleTextView.setText(R.string.help_onehand_title);
        mHelpTextView.setText(R.string.help_text_to_return_full);
        mSecureViewText.setText(R.string.help_secure_app);
        mButtonViewHiddden = false;
    }

    void setButtonClickCallback(ButtonClickActionCallback clickCb,
            ButtonLongClickActionCallback longClickCb) {
        mButtonClickActionCallback = clickCb;
        mButtonLongClickActionCallback = longClickCb;
    }
}

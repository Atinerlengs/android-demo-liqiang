package com.freeme.systemui.utils;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import com.android.keyguard.R;

public class SystemUIToast {
    private static final int MSG_CANCEL_TOAST = 0;
    private static final String TAG = "KeyguardToast";
    private static final int TOAST_HIDE_ANIMATION_DRUATION = 300;
    private static final int TOAST_HIDE_DELAY = 700;
    private static final int TOAST_SHOW_ANIMATION_DRUATION = 500;
    private final Context mContext;
    private final Rect mTextPadding = new Rect();
    private ObjectAnimator mToastHideAnim = null;
    private ObjectAnimator mToastShowAnim = null;
    private TextView mToastView;
    private WindowManager mWM;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message message) {
            switch (message.what) {
                case MSG_CANCEL_TOAST:
                    cancel();
                    return;
                default:
                    return;
            }
        }
    };

    private SystemUIToast(Context context) {
        mContext = context;
        mWM = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public static SystemUIToast makeText(Context context, CharSequence charSequence) {
        SystemUIToast keyguardToast = new SystemUIToast(context);
        keyguardToast.initToast(charSequence);
        return keyguardToast;
    }

    public static SystemUIToast makeText(Context context, int resId) throws NotFoundException {
        return makeText(context, context.getResources().getText(resId));
    }

    public void show() {
        if (mToastView == null) {
            Log.d(TAG, "show. return. mToastView is null!");
        } else {
            handleShow();
        }
    }

    public void cancel() {
        if (mToastView == null || mToastView.getVisibility() != View.VISIBLE) {
            Object obj;
            StringBuilder stringBuilder = new StringBuilder("cancelToast. return. mToastView.getVisibility() =");
            if (mToastView == null) {
                obj = null;
            } else {
                obj = Integer.valueOf(mToastView.getVisibility());
            }
            Log.d(TAG, stringBuilder.append(obj).toString());
            return;
        }
        handleHide();
    }

    private void initToast(CharSequence charSequence) {
        if (mContext != null) {
            Resources resources = mContext.getResources();
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.type = WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG;
            layoutParams.windowAnimations = WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                    | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.format = PixelFormat.TRANSLUCENT;
            layoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            layoutParams.y = 40;

            mTextPadding.left = resources.getDimensionPixelSize(R.dimen.color_toast_padding_left);
            mTextPadding.top = resources.getDimensionPixelSize(R.dimen.color_toast_padding_top);
            mTextPadding.right = resources.getDimensionPixelSize(R.dimen.color_toast_padding_right);
            mTextPadding.bottom = resources.getDimensionPixelSize(R.dimen.color_toast_padding_bottom);

            mToastView = new TextView(mContext);
            mToastView.setBackground(resources.getDrawable(R.drawable.freeme_systemui_toast_bg));
            mToastView.setShadowLayer(0.0f, 0.0f, 0.0f, 0);
            mToastView.setPadding(mTextPadding.left, mTextPadding.top, mTextPadding.right, mTextPadding.bottom);
            mToastView.setTextColor(Color.BLACK);
            mToastView.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) resources.getDimensionPixelSize(R.dimen.color_toast_text_size));
            mToastView.setText(charSequence);
            mToastView.setVisibility(View.GONE);
            mWM.addView(mToastView, layoutParams);
        }
    }

    private void handleShow() {
        if (mToastShowAnim == null) {
            initToastAnimation();
        }
        if (mToastHideAnim != null) {
            mToastHideAnim.cancel();
        }
        if (mToastShowAnim == null || !mToastShowAnim.isRunning()) {
            mToastShowAnim.start();
            mHandler.removeMessages(MSG_CANCEL_TOAST);
            mHandler.sendEmptyMessageDelayed(MSG_CANCEL_TOAST, TOAST_HIDE_DELAY);
            return;
        }
        Log.d(TAG, "handleShow. mToastShowAnim.isRunning(), just return.");
    }

    private void handleHide() {
        mHandler.removeMessages(MSG_CANCEL_TOAST);
        if (mToastHideAnim == null) {
            initToastAnimation();
        }
        if (mToastShowAnim != null) {
            mToastShowAnim.cancel();
        }
        if (mToastHideAnim != null && !mToastHideAnim.isRunning()) {
            mToastHideAnim.start();
        }
    }

    private void initToastAnimation() {
        if (mToastShowAnim == null) {
            mToastShowAnim = ObjectAnimator.ofFloat(mToastView, "alpha", new float[]{0.0f, 1.0f});
            mToastShowAnim.setDuration(TOAST_SHOW_ANIMATION_DRUATION);
            mToastShowAnim.addListener(new AnimatorListener() {
                public void onAnimationStart(Animator animator) {
                    mToastView.setVisibility(View.VISIBLE);
                }

                public void onAnimationRepeat(Animator animator) {
                }

                public void onAnimationEnd(Animator animator) {
                    mToastView.setAlpha(1.0f);
                }

                public void onAnimationCancel(Animator animator) {
                }
            });
        }
        if (mToastHideAnim == null) {
            mToastHideAnim = ObjectAnimator.ofFloat(mToastView, "alpha", new float[]{1.0f, 0.0f});
            mToastHideAnim.setDuration(TOAST_HIDE_ANIMATION_DRUATION);
            mToastHideAnim.addListener(new AnimatorListener() {
                public void onAnimationStart(Animator animator) {
                }

                public void onAnimationRepeat(Animator animator) {
                }

                public void onAnimationEnd(Animator animator) {
                    mToastView.setVisibility(View.GONE);
                    mWM.removeView(mToastView);
                }

                public void onAnimationCancel(Animator animator) {
                }
            });
        }
    }
}

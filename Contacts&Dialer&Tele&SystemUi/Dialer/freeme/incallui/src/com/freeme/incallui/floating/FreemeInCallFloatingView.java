package com.freeme.incallui.floating;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.incallui.R;

public class FreemeInCallFloatingView extends LinearLayout {

    private View mFloatingView;
    private ImageView mFloatingWaveInner1, mFloatingWaveInner2, mFloatingWaveInner3;
    private ImageView mFloatingWaveOuter1, mFloatingWaveOuter2, mFloatingWaveOuter3;

    private WindowManager.LayoutParams mWindowParams;

    private float mOriginalX;
    private float mOriginalY;
    private float mTouchStartX;
    private float mTouchStartY;
    private boolean isOutSideTouch = false;

    private int mStatusBarHeight;

    private Thread mThread;
    private SharedPreferences.Editor mEditor;
    private FreemeInCallFloatingManager mFloatingManager;

    private AnimationSet mAnimationSetInner1, mAnimationSetInner2, mAnimationSetInner3;
    private AnimationSet mAnimationSetOuter1, mAnimationSetOuter2, mAnimationSetOuter3;
    private final int ANIMATION_DURATION = 500;
    private final int MSG_WHAT_WAVE2_ANIMATION = 2;
    private final int MSG_WHAT_WAVE3_ANIMATION = 3;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_WHAT_WAVE2_ANIMATION:
                    mFloatingWaveInner2.startAnimation(mAnimationSetInner2);
                    mFloatingWaveOuter2.startAnimation(mAnimationSetOuter2);
                    break;
                case MSG_WHAT_WAVE3_ANIMATION:
                    mFloatingWaveInner3.startAnimation(mAnimationSetInner3);
                    mFloatingWaveOuter3.startAnimation(mAnimationSetOuter3);
                    break;
            }
        }
    };

    private class FloatBtnTask implements Runnable {
        @Override
        public void run() {
            if (mEditor == null) {
                SharedPreferences pref = PreferenceManager
                        .getDefaultSharedPreferences(getContext());
                mEditor = pref.edit();
            }

            mEditor.putInt(FreemeInCallFloatingUtils.PRFERENCE_KEY_FLOATING_BUTTON_X_POINT,
                    mWindowParams.x);
            mEditor.putInt(FreemeInCallFloatingUtils.PRFERENCE_KEY_FLOATING_BUTTON_Y_POINT,
                    mWindowParams.y);
            mEditor.commit();

            if (mThread != null && mThread.isAlive()) {
                mThread.interrupt();
            }
        }
    }

    public FreemeInCallFloatingView(Context context) {
        super(context);
        init(context);
    }

    void setFreemeInCallFloatingManager(FreemeInCallFloatingManager manager) {
        this.mFloatingManager = manager;
    }

    void setParams(WindowManager.LayoutParams params) {
        mWindowParams = params;
    }

    void setVisibility(boolean visibility) {
        if (visibility) {
            showWaveAnimation();
            mFloatingView.setVisibility(View.VISIBLE);
        } else {
            mFloatingView.setVisibility(View.INVISIBLE);
            clearWaveAnimation();
        }
    }

    private void init(final Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        mFloatingView = inflater.inflate(R.layout.freeme_incall_floating_view, this);

        mFloatingWaveInner1 = findViewById(R.id.freeme_incall_floating_wave_inner_1);
        mFloatingWaveInner2 = findViewById(R.id.freeme_incall_floating_wave_inner_2);
        mFloatingWaveInner3 = findViewById(R.id.freeme_incall_floating_wave_inner_3);
        mFloatingWaveOuter1 = findViewById(R.id.freeme_incall_floating_wave_outer_1);
        mFloatingWaveOuter2 = findViewById(R.id.freeme_incall_floating_wave_outer_2);
        mFloatingWaveOuter3 = findViewById(R.id.freeme_incall_floating_wave_outer_3);

        mFloatingView.setOnTouchListener(mFloatBtnTouchListener);
        mFloatingView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    FreemeInCallFloatingUtils.getTelecomManager(context)
                            .showInCallScreen(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mStatusBarHeight = getResources().getDimensionPixelSize(
                com.android.internal.R.dimen.status_bar_height);

        initWaveAnimation();
        showWaveAnimation();
    }

    private void initWaveAnimation() {
        mAnimationSetInner1 = initAnimationSetInner();
        mAnimationSetOuter1 = initAnimationSetOuter();

        mAnimationSetInner2 = initAnimationSetInner();
        mAnimationSetOuter2 = initAnimationSetOuter();

        mAnimationSetInner3 = initAnimationSetInner();
        mAnimationSetOuter3 = initAnimationSetOuter();
    }

    private AnimationSet initAnimationSetInner() {
        ScaleAnimation sa = new ScaleAnimation(1f, 1.7f, 1f, 1.7f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        sa.setDuration(ANIMATION_DURATION * 3);
        sa.setRepeatCount(Animation.INFINITE);

        AlphaAnimation aa = new AlphaAnimation(1, 0.2f);
        aa.setDuration(ANIMATION_DURATION * 3);
        aa.setRepeatCount(Animation.INFINITE);

        AnimationSet set = new AnimationSet(true);
        set.addAnimation(sa);
        set.addAnimation(aa);

        return set;
    }

    private AnimationSet initAnimationSetOuter() {
        ScaleAnimation sa = new ScaleAnimation(1f, 1.7f, 1f, 1.7f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        sa.setDuration(ANIMATION_DURATION * 3);
        sa.setRepeatCount(Animation.INFINITE);
        AlphaAnimation aa = new AlphaAnimation(1, 0.1f);
        aa.setDuration(ANIMATION_DURATION * 3);
        aa.setRepeatCount(Animation.INFINITE);

        AnimationSet set = new AnimationSet(true);
        set.setInterpolator(new AccelerateInterpolator());
        set.addAnimation(sa);
        set.addAnimation(aa);

        return set;
    }

    private void showWaveAnimation() {
        mFloatingWaveInner1.startAnimation(mAnimationSetInner1);
        mFloatingWaveOuter1.startAnimation(mAnimationSetOuter1);
        mHandler.sendEmptyMessageDelayed(MSG_WHAT_WAVE2_ANIMATION, ANIMATION_DURATION);
        mHandler.sendEmptyMessageDelayed(MSG_WHAT_WAVE3_ANIMATION, ANIMATION_DURATION * 2);
    }

    private void clearWaveAnimation() {
        mFloatingWaveInner1.clearAnimation();
        mFloatingWaveOuter1.clearAnimation();

        mFloatingWaveInner2.clearAnimation();
        mFloatingWaveOuter2.clearAnimation();

        mFloatingWaveInner3.clearAnimation();
        mFloatingWaveOuter3.clearAnimation();
    }

    private View.OnTouchListener mFloatBtnTouchListener = new View.OnTouchListener() {

        private boolean mIsFloatingBtnMoving = false;

        private float mTouchEndX;
        private float mTouchEndY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mOriginalX = event.getRawX();
            mOriginalY = event.getRawY() - mStatusBarHeight;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mIsFloatingBtnMoving = false;
                    mTouchStartX = (int) event.getX();
                    mTouchStartY = (int) event.getY();
                    break;

                case MotionEvent.ACTION_MOVE:
                    mTouchEndX = (int) event.getX();
                    mTouchEndY = (int) event.getY();
                    if (Math.abs(mTouchEndX - mTouchStartX) > 10
                            || Math.abs(mTouchEndY - mTouchStartY) > 10) {
                        mIsFloatingBtnMoving = true;

                        mWindowParams.x = (int) (mOriginalX - mTouchStartX);
                        mWindowParams.y = (int) (mOriginalY - mTouchStartY);

                        if (mFloatingManager != null) {
                            mFloatingManager.updateView(mWindowParams);
                        }
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    updateBtnPosition();

                    mTouchStartX = mTouchStartY = 0;
                    mTouchEndX = mTouchEndY = 0;
                    break;
            }

            if (isOutSideTouch) {
                isOutSideTouch = false;
                return true;
            }

            if (!mIsFloatingBtnMoving) {
                return false;
            } else {
                mFloatingView.setPressed(false);
                return true;
            }
        }
    };

    private void updateBtnPosition() {
        mWindowParams.x = (int) (mOriginalX - mTouchStartX);
        mWindowParams.y = (int) (mOriginalY - mTouchStartY);

        updateBtnPos();
    }

    private void updateBtnPos() {
        mThread = new Thread(new FloatBtnTask());
        mThread.start();
    }
}

package com.freeme.systemui.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.view.View;
import android.view.animation.PathInterpolator;

public class AnimUtils {

    public static final TimeInterpolator getInterpolator() {
        return new PathInterpolator(0.3f, 0.15f, 0.1f, 0.85f);
    }

    public static boolean startEnterSecurityViewAnimation(final View target, final Runnable endRunnable) {
        ValueAnimator anim = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        anim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float param = ((Float) animation.getAnimatedValue()).floatValue();
                float scale = ((1.0f - param) * 0.1f) + 1.0f;
                target.setScaleX(scale);
                target.setScaleY(scale);
                target.setAlpha(param);
            }
        });
        anim.setInterpolator(getInterpolator());
        anim.setDuration(350);
        if (endRunnable != null) {
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    endRunnable.run();
                }
            });
        }
        anim.start();
        return true;
    }

    public static boolean startExitSecurityViewAnimation(final View target, long duration) {
        ValueAnimator anim = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        anim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float param = ((Float) animation.getAnimatedValue()).floatValue();
                float scale = 1.0f - (0.15f * param);
                target.setScaleX(scale);
                target.setScaleY(scale);
                target.setAlpha(1.0f - param);
            }
        });
        anim.setInterpolator(getInterpolator());
        anim.setDuration(duration);
        anim.start();
        return true;
    }

    public static boolean startExitSecurityViewAnimation(View target, Runnable endRunnable) {
        if (target != null) {
            target.setScaleX(0.85f);
            target.setScaleY(0.85f);
            target.setAlpha(0.0f);
        }
        if (endRunnable != null) {
            endRunnable.run();
        }
        return true;
    }

}

